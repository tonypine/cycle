package com.tonypine.cycle.feature.settings

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tonypine.cycle.core.data.database.CycleDatabase
import com.tonypine.cycle.core.data.repository.ContraceptionRepository
import com.tonypine.cycle.core.data.repository.CycleRepository
import com.tonypine.cycle.core.data.repository.DayLogRepository
import com.tonypine.cycle.core.data.settings.SettingsRepository
import com.tonypine.cycle.core.model.Breaks
import com.tonypine.cycle.core.model.ContraceptionMethod
import com.tonypine.cycle.core.model.ContraceptionStretch
import com.tonypine.cycle.core.model.EstimateKind
import com.tonypine.cycle.core.model.StretchRefusal
import com.tonypine.cycle.core.ui.MethodChoice
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

/**
 * Settings › Contraception on the real repositories, an in-memory database: journeys B, D and E of
 * `docs/design/contraception.md`, with their synthetic dates.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class ContraceptionViewModelTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val database = Room
        .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), CycleDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val contraception = ContraceptionRepository(database)
    private var today = LocalDate.of(2027, 5, 10)

    @Before
    fun setMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        database.close()
    }

    private fun TestScope.page() = ContraceptionViewModel(contraception) { today }.also { viewModel ->
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    }

    private fun TestScope.add() = AddMethodViewModel(contraception) { today }.also { viewModel ->
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    }

    private fun TestScope.stretch(id: Long) = StretchViewModel(contraception, id) { today }.also { viewModel ->
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    }

    private suspend fun stored() = contraception.observeStretches().first()

    private suspend fun storedPill(started: LocalDate, stopped: LocalDate? = null) {
        contraception.start(ContraceptionMethod.COMBINED_PILL, Breaks.MONTHLY, started, today)
        if (stopped != null) contraception.stop(stored().single().id, stopped, today)
    }

    private suspend fun ContraceptionViewModel.await(until: (ContraceptionUiState) -> Boolean = { true }) =
        uiState.first { it != null && until(it) }!!

    private suspend fun AddMethodViewModel.await(until: (AddMethodUiState) -> Boolean = { true }) =
        uiState.first { it != null && until(it) }!!

    private suspend fun StretchViewModel.await(until: (StretchUiState) -> Boolean = { it is StretchUiState.Ready }) =
        uiState.first(until)

    private suspend fun StretchViewModel.ready(until: (StretchUiState.Ready) -> Boolean = { true }) =
        await { it is StretchUiState.Ready && until(it) } as StretchUiState.Ready

    /** Chooses [method] and picks [started], then Next or Save, once the stretches are read. */
    private suspend fun AddMethodViewModel.choose(method: ContraceptionMethod, started: LocalDate) {
        await()
        onChoose(MethodChoice.Method(method))
        onMethodNext()
        onPickStart(started)
        onSinceNext()
    }

    @Test
    fun `on none the page shows none, and the pill started in Settings becomes her method now`() = runTest {
        val page = page()
        assertEquals(ContraceptionUiState(today, current = null, stretches = emptyList()), page.await())

        // B3 to B5: Combined pill, since 3 May, a break every month.
        val add = add()
        add.await()
        add.onChoose(MethodChoice.Method(ContraceptionMethod.COMBINED_PILL))
        add.onMethodNext()
        assertEquals(AddMethodStep.Since, add.await().step)
        add.onPickStart(LocalDate.of(2027, 5, 3))
        add.onSinceNext()
        val breaks = add.await { it.step == AddMethodStep.Breaks }
        assertEquals(Breaks.MONTHLY, breaks.breaks)
        add.onSave()

        add.await { it.saved }
        val pill = ContraceptionStretch(
            ContraceptionMethod.COMBINED_PILL,
            LocalDate.of(2027, 5, 3),
            breaks = Breaks.MONTHLY,
            id = 1
        )
        assertEquals(
            ContraceptionUiState(today, current = pill, stretches = listOf(pill)),
            page.await { it.current != null }
        )
    }

    @Test
    fun `back goes back a step, and from the first leaves`() = runTest {
        val add = add()
        add.await()
        add.onChoose(MethodChoice.Method(ContraceptionMethod.RING))
        add.onMethodNext()
        add.onPickStart(LocalDate.of(2027, 5, 3))
        add.onSinceNext()
        add.await { it.step == AddMethodStep.Breaks }

        assertTrue(add.onBack())
        assertEquals(AddMethodStep.Since, add.await().step)
        assertTrue(add.onBack())
        assertEquals(AddMethodStep.Method, add.await().step)
        assertEquals(false, add.onBack())
        assertTrue(stored().isEmpty())
    }

    @Test
    fun `a method with no breaks saves on its date, and None has no next step`() = runTest {
        val add = add()
        add.await()
        add.onChoose(MethodChoice.None)
        add.onMethodNext()
        assertEquals(AddMethodStep.Method, add.await().step)

        add.choose(ContraceptionMethod.IMPLANT, LocalDate.of(2027, 4, 1))

        add.await { it.saved }
        assertEquals(
            listOf(ContraceptionStretch(ContraceptionMethod.IMPLANT, LocalDate.of(2027, 4, 1), id = 1)),
            stored()
        )
    }

    @Test
    fun `changing to the hormonal IUD ends the pill the day before, and an earlier day is refused`() = runTest {
        // E2 and E3: today is 17 September, pill since 3 May.
        today = LocalDate.of(2027, 9, 17)
        storedPill(LocalDate.of(2027, 5, 3))
        val page = page()

        val tooEarly = add()
        tooEarly.choose(ContraceptionMethod.HORMONAL_IUD, LocalDate.of(2027, 5, 3))
        val refused = tooEarly.await { it.dialog != null }.dialog as StretchDialog.Refused
        assertEquals(
            ContraceptionMethod.COMBINED_PILL,
            (refused.reason as StretchRefusal.NotAfterCurrentStart).current.method
        )
        tooEarly.onDismissDialog()
        assertNull(tooEarly.await().dialog)

        val add = add()
        add.choose(ContraceptionMethod.HORMONAL_IUD, LocalDate.of(2027, 9, 13))

        add.await { it.saved }
        val state = page.await { it.current?.method == ContraceptionMethod.HORMONAL_IUD }
        assertEquals(
            listOf(
                ContraceptionMethod.HORMONAL_IUD to (LocalDate.of(2027, 9, 13) to null),
                ContraceptionMethod.COMBINED_PILL to (LocalDate.of(2027, 5, 3) to LocalDate.of(2027, 9, 12))
            ),
            state.stretches.map { it.method to (it.started to it.stopped) }
        )
    }

    @Test
    fun `a pill started inside a stopped implant asks to move its end before its breaks`() = runTest {
        // D, adding a method after it: implant 9 November 2026 to 3 November 2027, today 15 November.
        today = LocalDate.of(2027, 11, 15)
        contraception.start(ContraceptionMethod.IMPLANT, null, LocalDate.of(2026, 11, 9), today)
        contraception.stop(stored().single().id, LocalDate.of(2027, 11, 3), today)

        val add = add()
        add.choose(ContraceptionMethod.COMBINED_PILL, LocalDate.of(2027, 10, 20))
        val asking = add.await { it.dialog != null }
        assertEquals(
            LocalDate.of(2027, 10, 19),
            (asking.dialog as StretchDialog.ConfirmMoves).moves.single().to.stopped
        )
        assertEquals(AddMethodStep.Since, asking.step)

        add.onMove()
        add.await { it.step == AddMethodStep.Breaks }
        add.onPickBreaks(Breaks.NONE)
        add.onSave()

        add.await { it.saved }
        assertEquals(
            listOf(
                ContraceptionMethod.IMPLANT to (LocalDate.of(2026, 11, 9) to LocalDate.of(2027, 10, 19)),
                ContraceptionMethod.COMBINED_PILL to (LocalDate.of(2027, 10, 20) to null)
            ),
            stored().map { it.method to (it.started to it.stopped) }
        )
        assertEquals(Breaks.NONE, stored().last().breaks)
    }

    @Test
    fun `a start that would cover a stopped method whole is refused, and cancel saves nothing`() = runTest {
        today = LocalDate.of(2027, 11, 15)
        contraception.start(ContraceptionMethod.IMPLANT, null, LocalDate.of(2026, 11, 9), today)
        contraception.stop(stored().single().id, LocalDate.of(2027, 11, 3), today)
        val before = stored()

        val covering = add()
        covering.choose(ContraceptionMethod.INJECTION, LocalDate.of(2026, 11, 9))
        val refused = covering.await { it.dialog != null }.dialog as StretchDialog.Refused
        assertEquals(ContraceptionMethod.IMPLANT, (refused.reason as StretchRefusal.CoversWhole).stretch.method)

        val cancelled = add()
        cancelled.choose(ContraceptionMethod.INJECTION, LocalDate.of(2027, 10, 20))
        cancelled.await { it.dialog is StretchDialog.ConfirmMoves }
        cancelled.onDismissDialog()

        assertEquals(false, cancelled.await { it.dialog == null }.saved)
        assertEquals(before, stored())
    }

    @Test
    fun `correcting the IUD's start asks to move the end of the pill`() = runTest {
        // E5 to E7: IUD since 13 September, the pill 3 May to 12 September.
        today = LocalDate.of(2027, 9, 17)
        storedPill(LocalDate.of(2027, 5, 3))
        contraception.start(ContraceptionMethod.HORMONAL_IUD, null, LocalDate.of(2027, 9, 13), today)
        val iud = stored().last()
        val page = stretch(iud.id)
        page.ready()

        page.onEdit(iud.copy(started = LocalDate.of(2027, 9, 6)))
        val asking = page.ready { it.dialog != null }
        val move = (asking.dialog as StretchDialog.ConfirmMoves).moves.single()
        assertEquals(LocalDate.of(2027, 9, 12), move.from.stopped)
        assertEquals(LocalDate.of(2027, 9, 5), move.to.stopped)
        assertEquals(LocalDate.of(2027, 9, 13), stored().last().started)

        page.onMove()

        val moved = page.ready { it.stretch.started == LocalDate.of(2027, 9, 6) }
        assertEquals(iud.copy(started = LocalDate.of(2027, 9, 6)), moved.stretch)
        assertEquals(
            listOf(LocalDate.of(2027, 5, 3) to LocalDate.of(2027, 9, 5), LocalDate.of(2027, 9, 6) to null),
            stored().map { it.started to it.stopped }
        )
    }

    @Test
    fun `editing the pill's start and breaks saves them, and a stop before its start is refused`() = runTest {
        today = LocalDate.of(2027, 9, 17)
        storedPill(LocalDate.of(2027, 5, 3), stopped = LocalDate.of(2027, 9, 12))
        val pill = stored().single()
        val page = stretch(pill.id)
        page.ready()

        page.onEdit(pill.copy(started = LocalDate.of(2027, 4, 26)))
        val earlier = page.ready { it.stretch.started == LocalDate.of(2027, 4, 26) }.stretch
        page.onEdit(earlier.copy(breaks = Breaks.EVERY_FEW_PACKS))
        val edited = page.ready { it.stretch.breaks == Breaks.EVERY_FEW_PACKS }.stretch
        assertEquals(pill.copy(started = LocalDate.of(2027, 4, 26), breaks = Breaks.EVERY_FEW_PACKS), edited)

        page.onEdit(edited.copy(stopped = LocalDate.of(2027, 4, 1)))
        assertEquals(StretchDialog.Refused(StretchRefusal.StopBeforeStart), page.ready { it.dialog != null }.dialog)
        assertEquals(LocalDate.of(2027, 9, 12), stored().single().stopped)
    }

    @Test
    fun `marking a method as stopped saves its last day and closes, and the injection counts 13 weeks`() = runTest {
        // D3: the implant came out on 3 November.
        today = LocalDate.of(2027, 11, 15)
        contraception.start(ContraceptionMethod.IMPLANT, null, LocalDate.of(2026, 11, 9), today)
        val implant = stretch(stored().single().id)
        implant.ready()
        implant.onStop(LocalDate.of(2027, 11, 3))

        implant.await { it == StretchUiState.Closed }
        assertEquals(LocalDate.of(2027, 11, 3), stored().single().stopped)
        assertNull(page().await().current)

        // After the injection: the last one on 3 August, marked on 20 August.
        today = LocalDate.of(2027, 8, 20)
        database.clearAllTables()
        contraception.start(ContraceptionMethod.INJECTION, null, LocalDate.of(2026, 11, 9), today)
        val injection = stretch(stored().single().id)
        assertEquals(LocalDate.of(2027, 11, 19), injection.ready().latestStop)
        injection.onStop(LocalDate.of(2027, 8, 3))

        injection.await { it == StretchUiState.Closed }
        assertEquals(LocalDate.of(2027, 11, 2), stored().single().stopped)
        assertEquals(stored().single(), page().await().current)
    }

    @Test
    fun `deleting a stretch forgets it and closes its page, and the others stay`() = runTest {
        today = LocalDate.of(2027, 9, 17)
        storedPill(LocalDate.of(2027, 5, 3))
        contraception.start(ContraceptionMethod.HORMONAL_IUD, null, LocalDate.of(2027, 9, 13), today)
        val iud = stretch(stored().last().id)
        iud.ready()

        iud.onDelete()

        iud.await { it == StretchUiState.Closed }
        assertEquals(listOf(ContraceptionMethod.COMBINED_PILL), stored().map { it.method })
        assertNull(page().await().current)
    }

    @Test
    fun `the estimates follow what she saves here`() = runTest {
        val settings = SettingsRepository(
            PreferenceDataStoreFactory.create(scope = backgroundScope) { File(folder.root, "test.preferences_pb") }
        )
        val cycle = CycleRepository(DayLogRepository(database), settings, contraception)
        assertEquals(EstimateKind.PERIOD, cycle.observeOverview(today).first().contraception.estimates)

        val add = add()
        add.choose(ContraceptionMethod.IMPLANT, LocalDate.of(2027, 4, 1))
        add.await { it.saved }
        assertEquals(EstimateKind.NONE, cycle.observeOverview(today).first().contraception.estimates)

        val implant = stretch(stored().single().id)
        implant.ready()
        implant.onDelete()
        implant.await { it == StretchUiState.Closed }
        assertEquals(EstimateKind.PERIOD, cycle.observeOverview(today).first().contraception.estimates)
    }
}
