package com.tonypine.cycle.feature.onboarding

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tonypine.cycle.core.data.database.CycleDatabase
import com.tonypine.cycle.core.data.repository.CycleRepository
import com.tonypine.cycle.core.data.repository.DayLogRepository
import com.tonypine.cycle.core.data.settings.SettingsRepository
import com.tonypine.cycle.core.model.CycleSettings
import com.tonypine.cycle.core.model.DayLog
import com.tonypine.cycle.core.model.EstimateBasis
import com.tonypine.cycle.core.model.EstimatedPeriod
import com.tonypine.cycle.core.model.Period
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
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

/**
 * The welcome's gate and what setup saves, on the real repositories: an in-memory database and a
 * DataStore file. Synthetic dates only: made-up days in 2027, never anyone's real cycle.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class OnboardingViewModelTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val today = day("2027-03-20")

    private val database = Room
        .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), CycleDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    private val dayLogs = DayLogRepository(database)

    @Before
    fun setMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `a new install shows the welcome`() = onboarding { viewModel, _ ->
        viewModel.awaitWelcome(shown = true)
    }

    @Test
    fun `setup on 20 March with 2 March, 28 and 5 gives day 19 and an estimate from her lengths`() =
        onboarding { viewModel, settings ->
            viewModel.awaitWelcome(shown = true)
            viewModel.onDone(day("2027-03-02"), cycleLength = 28, periodLength = 5)

            viewModel.awaitWelcome(shown = false)
            assertEquals(
                CycleSettings(usualCycleLength = 28, usualPeriodLength = 5, setupDone = true),
                settings.settings.first()
            )
            val overview = CycleRepository(dayLogs, settings).observeOverview(today).first()
            assertEquals(19, overview.cycleDay)
            assertEquals(listOf(Period(day("2027-03-02"), day("2027-03-06"))), overview.periods)
            val estimate = checkNotNull(overview.estimate)
            assertEquals(
                EstimatedPeriod(
                    expectedStart = day("2027-03-30"),
                    earliestStart = day("2027-03-26"),
                    latestStart = day("2027-04-03"),
                    expectedLength = 5
                ),
                estimate.next
            )
            assertEquals(EstimateBasis.Setup, estimate.cycleBasis)
            // The period she gave is over, so it already counts as one of her own.
            assertEquals(EstimateBasis.Logged(1), estimate.periodBasis)
        }

    @Test
    fun `setup uses her lengths, not the defaults`() = onboarding { viewModel, settings ->
        viewModel.awaitWelcome(shown = true)
        viewModel.onDone(day("2027-03-02"), cycleLength = 32, periodLength = 4)

        viewModel.awaitWelcome(shown = false)
        val estimate = checkNotNull(CycleRepository(dayLogs, settings).observeOverview(today).first().estimate)
        assertEquals(day("2027-04-03"), estimate.next.expectedStart)
        assertEquals(
            listOf(DayLog(day("2027-03-02"), periodStarted = true), DayLog(day("2027-03-05"), periodEnded = true)),
            dayLogs.observeDayLogs().first()
        )
    }

    @Test
    fun `I don't remember saves the lengths and logs no period`() = onboarding { viewModel, settings ->
        viewModel.awaitWelcome(shown = true)
        viewModel.onDone(lastPeriodStart = null, cycleLength = 30, periodLength = 6)

        viewModel.awaitWelcome(shown = false)
        assertEquals(CycleSettings(30, 6, setupDone = true), settings.settings.first())
        assertEquals(emptyList<DayLog>(), dayLogs.observeDayLogs().first())
    }

    @Test
    fun `skip leaves the welcome and keeps the typical lengths`() = onboarding { viewModel, settings ->
        viewModel.awaitWelcome(shown = true)
        viewModel.onSkip()

        viewModel.awaitWelcome(shown = false)
        assertEquals(CycleSettings(28, 5, setupDone = false), settings.settings.first())
        assertEquals(emptyList<DayLog>(), dayLogs.observeDayLogs().first())
    }

    @Test
    fun `after Done or Skip, the next launch opens on Today`() = onboarding { viewModel, _ ->
        viewModel.awaitWelcome(shown = true)
        viewModel.onSkip()
        viewModel.awaitWelcome(shown = false)

        nextLaunch().awaitWelcome(shown = false)
    }

    @Test
    fun `leaving setup halfway saves nothing, so the next launch shows the welcome`() = onboarding {
            viewModel,
            settings
        ->
        // She went through both steps but never tapped Done: her answers stay on screen only.
        viewModel.awaitWelcome(shown = true)

        nextLaunch().awaitWelcome(shown = true)
        assertEquals(CycleSettings(28, 5, setupDone = false), settings.settings.first())
    }

    @Test
    fun `an install with logs from before the welcome opens on Today`() = onboarding { _, _ ->
        dayLogs.setPeriodStarted(day("2027-03-02"), started = true)

        nextLaunch().awaitWelcome(shown = false)
    }

    @Test
    fun `an install with setup from before the welcome opens on Today`() = onboarding { _, settings ->
        settings.saveSetup(cycleLength = 30, periodLength = 5)

        nextLaunch().awaitWelcome(shown = false)
    }

    @Test
    fun `clearing the welcome flag with nothing logged shows the welcome again`() = onboarding { viewModel, settings ->
        viewModel.awaitWelcome(shown = true)
        viewModel.onSkip()
        viewModel.awaitWelcome(shown = false)

        // What "Delete everything" (MOT-40) leaves behind.
        settings.setWelcomeDone(false)

        viewModel.awaitWelcome(shown = true)
    }

    private lateinit var settings: SettingsRepository
    private lateinit var scope: TestScope

    /** Runs [test] with a ViewModel on the test database, its gate collected as the app would. */
    private fun onboarding(test: suspend TestScope.(OnboardingViewModel, SettingsRepository) -> Unit) = runTest {
        scope = this
        settings = SettingsRepository(
            PreferenceDataStoreFactory.create(scope = backgroundScope) { File(folder.root, "test.preferences_pb") }
        )
        test(nextLaunch(), settings)
    }

    /** A new ViewModel on the same storage, as when she opens the app again. */
    private fun nextLaunch(): OnboardingViewModel {
        val viewModel = OnboardingViewModel(settings, dayLogs) { today }
        scope.backgroundScope.launch(UnconfinedTestDispatcher(scope.testScheduler)) { viewModel.showWelcome.collect {} }
        return viewModel
    }

    // Waits for the gate to settle on [shown]; a gate that never does fails the test by timing out.
    // Done and Skip wait for the welcome first, as on screen: before that, the gate is still reading.
    private suspend fun OnboardingViewModel.awaitWelcome(shown: Boolean) {
        showWelcome.first { it == shown }
    }

    private fun day(iso: String): LocalDate = LocalDate.parse(iso)
}
