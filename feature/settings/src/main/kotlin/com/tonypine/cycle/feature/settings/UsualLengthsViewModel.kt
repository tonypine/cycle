package com.tonypine.cycle.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tonypine.cycle.core.data.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * "Usual cycle and period": her lengths as saved, to edit, and Save. Saving marks setup done, so the
 * estimates that use setup's lengths use these from then on.
 */
class UsualLengthsViewModel(private val settings: SettingsRepository) : ViewModel() {
    private val state = MutableStateFlow<UsualLengthsUiState>(UsualLengthsUiState.Loading)
    val uiState: StateFlow<UsualLengthsUiState> = state.asStateFlow()

    init {
        // Read once: the fields start from what is saved, then follow only what she types.
        viewModelScope.launch {
            val saved = settings.settings.first()
            state.value = UsualLengthsUiState.Editing(saved.usualCycleLength, saved.usualPeriodLength)
        }
    }

    /** Saves her lengths, already checked, then the screen closes. */
    fun onSave(cycleLength: Int, periodLength: Int) {
        viewModelScope.launch {
            settings.saveSetup(cycleLength, periodLength)
            state.value = UsualLengthsUiState.Saved
        }
    }
}

sealed interface UsualLengthsUiState {
    /** Her settings have not been read yet. */
    data object Loading : UsualLengthsUiState

    /** The lengths saved when she opened the screen, to start the fields from. */
    data class Editing(val cycleLength: Int, val periodLength: Int) : UsualLengthsUiState

    /** Her lengths are saved: the screen closes. */
    data object Saved : UsualLengthsUiState
}
