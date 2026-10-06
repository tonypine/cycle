package com.tonypine.cycle.core.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tonypine.cycle.core.data.database.CycleDatabase
import com.tonypine.cycle.core.data.day
import com.tonypine.cycle.core.model.Breaks
import com.tonypine.cycle.core.model.ContraceptionMethod
import com.tonypine.cycle.core.model.ContraceptionStretch
import com.tonypine.cycle.core.model.StretchChange
import com.tonypine.cycle.core.model.StretchMove
import com.tonypine.cycle.core.model.StretchRefusal
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Her contraception on the database file the app opens: each write in one transaction. Synthetic dates. */
@RunWith(AndroidJUnit4::class)
class ContraceptionRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private var database = CycleDatabase.build(context)
    private var repository = ContraceptionRepository(database)

    @After
    fun deleteDatabase() {
        database.close()
        context.deleteDatabase(CycleDatabase.FILE_NAME)
    }

    private fun restart() {
        database.close()
        database = CycleDatabase.build(context)
        repository = ContraceptionRepository(database)
    }

    private suspend fun stretches() = repository.observeStretches().first().map { it.copy(id = 0) }

    @Test
    fun `stretches persist across restarts`() = runTest {
        repository.start(
            ContraceptionMethod.COMBINED_PILL,
            Breaks.MONTHLY,
            day("2027-05-03"),
            today = day("2027-05-03")
        )
        repository.start(ContraceptionMethod.HORMONAL_IUD, null, day("2027-09-13"), today = day("2027-09-17"))

        restart()

        assertEquals(
            listOf(
                ContraceptionStretch(
                    ContraceptionMethod.COMBINED_PILL,
                    day("2027-05-03"),
                    day("2027-09-12"),
                    Breaks.MONTHLY
                ),
                ContraceptionStretch(ContraceptionMethod.HORMONAL_IUD, day("2027-09-13"))
            ),
            stretches()
        )
    }

    @Test
    fun `a stretch with an unknown start sorts first`() = runTest {
        repository.start(ContraceptionMethod.IMPLANT, null, started = null, today = day("2027-03-01"))
        repository.start(ContraceptionMethod.COPPER_IUD, null, day("2027-03-01"), today = day("2027-03-01"))

        assertEquals(listOf(null, day("2027-03-01")), stretches().map { it.started })
        assertEquals(day("2027-02-28"), stretches().first().stopped)
    }

    @Test
    fun `a move she has to confirm stores nothing until she does`() = runTest {
        val today = day("2027-11-15")
        repository.start(ContraceptionMethod.IMPLANT, null, day("2026-11-09"), today = day("2026-11-09"))
        val implant = repository.observeStretches().first().single()
        assertEquals(StretchChange.Saved, repository.stop(implant.id, day("2027-11-03"), today))
        val stopped = implant.copy(stopped = day("2027-11-03"))

        val asked = repository.start(ContraceptionMethod.COMBINED_PILL, Breaks.MONTHLY, day("2027-10-20"), today)

        assertEquals(
            StretchChange.ConfirmMoves(listOf(StretchMove(stopped, stopped.copy(stopped = day("2027-10-19"))))),
            asked
        )
        assertEquals(listOf(stopped), repository.observeStretches().first())

        val saved = repository.start(
            ContraceptionMethod.COMBINED_PILL,
            Breaks.MONTHLY,
            day("2027-10-20"),
            today,
            confirmed = true
        )

        assertEquals(StretchChange.Saved, saved)
        assertEquals(
            listOf(
                stopped.copy(stopped = day("2027-10-19"), id = 0),
                ContraceptionStretch(ContraceptionMethod.COMBINED_PILL, day("2027-10-20"), breaks = Breaks.MONTHLY)
            ),
            stretches()
        )
    }

    @Test
    fun `a refused change stores nothing`() = runTest {
        repository.start(
            ContraceptionMethod.COMBINED_PILL,
            Breaks.MONTHLY,
            day("2027-05-03"),
            today = day("2027-05-03")
        )
        val before = repository.observeStretches().first()

        val refused = repository.start(ContraceptionMethod.IMPLANT, null, day("2027-05-01"), today = day("2027-09-17"))

        assertEquals(StretchChange.Refused(StretchRefusal.NotAfterCurrentStart(before.single())), refused)
        assertEquals(before, repository.observeStretches().first())
    }

    @Test
    fun `editing and deleting a stretch`() = runTest {
        val today = day("2027-09-17")
        repository.start(
            ContraceptionMethod.COMBINED_PILL,
            Breaks.MONTHLY,
            day("2027-05-03"),
            today = day("2027-05-03")
        )
        repository.start(ContraceptionMethod.HORMONAL_IUD, null, day("2027-09-13"), today)
        val (pill, iud) = repository.observeStretches().first()

        assertEquals(StretchChange.Saved, repository.edit(pill.copy(breaks = Breaks.EVERY_FEW_PACKS), today))
        val corrected = iud.copy(started = day("2027-09-06"))
        val asked = repository.edit(corrected, today)
        assertEquals(StretchChange.Saved, repository.edit(corrected, today, confirmed = true))

        assertEquals(
            StretchChange.ConfirmMoves(
                listOf(
                    StretchMove(
                        pill.copy(breaks = Breaks.EVERY_FEW_PACKS),
                        pill.copy(stopped = day("2027-09-05"), breaks = Breaks.EVERY_FEW_PACKS)
                    )
                )
            ),
            asked
        )
        assertEquals(
            listOf(pill.copy(stopped = day("2027-09-05"), breaks = Breaks.EVERY_FEW_PACKS), corrected),
            repository.observeStretches().first()
        )

        repository.delete(pill.id)

        assertEquals(listOf(corrected), repository.observeStretches().first())
    }

    @Test
    fun `stopping the injection counts 13 weeks from the last one`() = runTest {
        repository.start(ContraceptionMethod.INJECTION, null, day("2027-02-01"), today = day("2027-02-01"))
        val injection = repository.observeStretches().first().single()

        repository.stop(injection.id, day("2027-08-03"), today = day("2027-08-20"))

        assertEquals(day("2027-11-02"), repository.observeStretches().first().single().stopped)
    }
}
