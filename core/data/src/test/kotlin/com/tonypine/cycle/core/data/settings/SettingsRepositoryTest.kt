package com.tonypine.cycle.core.data.settings

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tonypine.cycle.core.data.day
import com.tonypine.cycle.core.data.settingsRepository
import com.tonypine.cycle.core.model.CyclePrompt
import com.tonypine.cycle.core.model.CycleSettings
import com.tonypine.cycle.core.model.LogCategory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsRepositoryTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun `before setup, the typical 28-day cycle and 5-day period`() = runTest {
        val settings = settingsRepository(folder.root, backgroundScope)

        assertEquals(
            CycleSettings(usualCycleLength = 28, usualPeriodLength = 5, setupDone = false),
            settings.settings.first()
        )
    }

    @Test
    fun `stores her usual lengths and setup`() = runTest {
        val settings = settingsRepository(folder.root, backgroundScope)

        settings.setUsualCycleLength(31)
        settings.setUsualPeriodLength(4)
        settings.setSetupDone(true)

        assertEquals(
            CycleSettings(usualCycleLength = 31, usualPeriodLength = 4, setupDone = true),
            settings.settings.first()
        )
    }

    @Test
    fun `setup saves both lengths and marks setup done`() = runTest {
        val settings = settingsRepository(folder.root, backgroundScope)

        settings.saveSetup(cycleLength = 33, periodLength = 6)

        assertEquals(
            CycleSettings(usualCycleLength = 33, usualPeriodLength = 6, setupDone = true),
            settings.settings.first()
        )
        // Setup alone does not pass the welcome: the app does that once everything is saved.
        assertFalse(settings.welcomeDone.first())
    }

    @Test
    fun `the welcome is done once set, apart from setup`() = runTest {
        val settings = settingsRepository(folder.root, backgroundScope)
        assertFalse(settings.welcomeDone.first())

        settings.setWelcomeDone(true)

        assertTrue(settings.welcomeDone.first())
        assertFalse(settings.settings.first().setupDone)
    }

    @Test
    fun `remembers each dismissed prompt under its own cycle`() = runTest {
        val settings = settingsRepository(folder.root, backgroundScope)

        settings.dismiss(CyclePrompt.StillGoing(day("2027-03-01"), periodDay = 7))
        settings.dismiss(CyclePrompt.MissedPeriod(day("2027-01-04"), cycleDay = 51))
        settings.dismiss(CyclePrompt.MissedPeriod(day("2027-03-01"), cycleDay = 60))

        val stored = settings.settings.first()
        assertEquals(setOf(day("2027-03-01")), stored.dismissedStillGoing)
        assertEquals(setOf(day("2027-01-04"), day("2027-03-01")), stored.dismissedMissedPeriod)
    }

    @Test
    fun `refuses a length under a day`() = runTest {
        val settings = settingsRepository(folder.root, backgroundScope)

        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { settings.setUsualCycleLength(0) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { settings.setUsualPeriodLength(-1) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { settings.saveSetup(cycleLength = 28, periodLength = 0) }
        }
    }

    @Test
    fun `every category shows until she hides one, and showing it again undoes that`() = runTest {
        val settings = settingsRepository(folder.root, backgroundScope)
        assertEquals(emptySet<LogCategory>(), settings.settings.first().hiddenCategories)

        settings.setCategoryShown(LogCategory.SEX, shown = false)
        settings.setCategoryShown(LogCategory.NOTES, shown = false)
        assertEquals(setOf(LogCategory.SEX, LogCategory.NOTES), settings.settings.first().hiddenCategories)

        settings.setCategoryShown(LogCategory.SEX, shown = true)
        settings.setCategoryShown(LogCategory.MOOD, shown = true)
        assertEquals(setOf(LogCategory.NOTES), settings.settings.first().hiddenCategories)
    }

    @Test
    fun `remembers the day she last exported, and clearing forgets every setting`() = runTest {
        val settings = settingsRepository(folder.root, backgroundScope)
        assertEquals(null, settings.lastExported.first())

        settings.setLastExported(day("2027-03-20"))
        settings.saveSetup(cycleLength = 31, periodLength = 4)
        settings.setWelcomeDone(true)
        assertEquals(day("2027-03-20"), settings.lastExported.first())

        settings.clear()

        assertEquals(null, settings.lastExported.first())
        assertFalse(settings.welcomeDone.first())
        assertEquals(
            CycleSettings(usualCycleLength = 28, usualPeriodLength = 5, setupDone = false),
            settings.settings.first()
        )
    }

    @Test
    fun `the app lock is off until she turns it on`() = runTest {
        val settings = settingsRepository(folder.root, backgroundScope)
        assertFalse(settings.appLock.first())

        settings.setAppLock(true)
        assertTrue(settings.appLock.first())

        settings.setAppLock(false)
        assertFalse(settings.appLock.first())
        assertFalse(settings.appLockTurnedOff.first())
    }

    @Test
    fun `a lock turned off for want of a screen lock leaves a note until she dismisses it`() = runTest {
        val settings = settingsRepository(folder.root, backgroundScope)
        // Nothing to turn off: no note.
        settings.turnOffAppLockWithoutScreenLock()
        assertFalse(settings.appLockTurnedOff.first())

        settings.setAppLock(true)
        settings.turnOffAppLockWithoutScreenLock()

        assertFalse(settings.appLock.first())
        assertTrue(settings.appLockTurnedOff.first())
        settings.dismissAppLockTurnedOff()
        assertFalse(settings.appLockTurnedOff.first())
    }

    @Test
    fun `turning the lock back on drops the note`() = runTest {
        val settings = settingsRepository(folder.root, backgroundScope)
        settings.setAppLock(true)
        settings.turnOffAppLockWithoutScreenLock()

        settings.setAppLock(true)

        assertTrue(settings.appLock.first())
        assertFalse(settings.appLockTurnedOff.first())
    }

    @Test
    fun `delete everything turns the lock off`() = runTest {
        val settings = settingsRepository(folder.root, backgroundScope)
        settings.setAppLock(true)

        settings.clear()

        assertFalse(settings.appLock.first())
    }
}
