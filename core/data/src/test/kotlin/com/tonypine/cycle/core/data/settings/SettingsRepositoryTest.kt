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
import org.junit.Assert.assertThrows
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
}
