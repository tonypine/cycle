package com.tonypine.cycle.feature.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tonypine.cycle.core.data.settings.SettingsRepository
import com.tonypine.cycle.core.model.CycleSettings
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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

/** Usual cycle and period on real settings in a DataStore file. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class UsualLengthsViewModelTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Before
    fun setMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun resetMain() = Dispatchers.resetMain()

    @Test
    fun `starts from the saved lengths, and Save makes them setup's`() = runTest {
        val settings = SettingsRepository(
            PreferenceDataStoreFactory.create(scope = backgroundScope) { File(folder.root, "test.preferences_pb") }
        )
        val viewModel = UsualLengthsViewModel(settings)

        assertEquals(
            UsualLengthsUiState.Editing(28, 5),
            viewModel.uiState.first { it is UsualLengthsUiState.Editing }
        )

        viewModel.onSave(cycleLength = 30, periodLength = 4)

        assertEquals(UsualLengthsUiState.Saved, viewModel.uiState.first { it == UsualLengthsUiState.Saved })
        assertEquals(CycleSettings(30, 4, setupDone = true), settings.settings.first())
    }
}
