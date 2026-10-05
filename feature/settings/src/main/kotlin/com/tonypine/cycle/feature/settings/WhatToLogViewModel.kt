package com.tonypine.cycle.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tonypine.cycle.core.data.settings.SettingsRepository
import com.tonypine.cycle.core.model.LogCategory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** What to log: the categories she hid, from her settings, and the switch that shows or hides one. */
class WhatToLogViewModel(private val settings: SettingsRepository) : ViewModel() {
    val uiState: StateFlow<WhatToLogUiState> = settings.settings
        .map { WhatToLogUiState.Ready(it.hiddenCategories) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), WhatToLogUiState.Loading)

    /** Shows or hides [category]. Hiding keeps what she logged in it. */
    fun onShownChange(category: LogCategory, shown: Boolean) {
        viewModelScope.launch { settings.setCategoryShown(category, shown) }
    }

    private companion object {
        // Keeps the settings flowing through a configuration change without a reload.
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

sealed interface WhatToLogUiState {
    /** Her settings have not been read yet. */
    data object Loading : WhatToLogUiState

    /** [hidden] are the categories she turned off. */
    data class Ready(val hidden: Set<LogCategory>) : WhatToLogUiState {
        fun isShown(category: LogCategory): Boolean = category !in hidden
    }
}
