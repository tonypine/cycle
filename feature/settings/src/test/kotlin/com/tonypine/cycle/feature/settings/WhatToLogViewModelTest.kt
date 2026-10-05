package com.tonypine.cycle.feature.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tonypine.cycle.core.data.settings.SettingsRepository
import com.tonypine.cycle.core.model.LogCategory
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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

/** What to log on real settings in a DataStore file. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class WhatToLogViewModelTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Before
    fun setMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun resetMain() = Dispatchers.resetMain()

    @Test
    fun `everything shows at first, and a switch hides a category and shows it again`() = runTest {
        val settings = SettingsRepository(
            PreferenceDataStoreFactory.create(scope = backgroundScope) { File(folder.root, "test.preferences_pb") }
        )
        val viewModel = WhatToLogViewModel(settings)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        assertEquals(WhatToLogUiState.Ready(emptySet()), viewModel.uiState.first { it is WhatToLogUiState.Ready })

        viewModel.onShownChange(LogCategory.SEX, shown = false)
        assertEquals(
            WhatToLogUiState.Ready(setOf(LogCategory.SEX)),
            viewModel.uiState.first { it == WhatToLogUiState.Ready(setOf(LogCategory.SEX)) }
        )
        assertEquals(setOf(LogCategory.SEX), settings.settings.first().hiddenCategories)

        viewModel.onShownChange(LogCategory.SEX, shown = true)
        assertEquals(
            emptySet<LogCategory>(),
            settings.settings.first { it.hiddenCategories.isEmpty() }.hiddenCategories
        )
    }
}
