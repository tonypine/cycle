package com.tonypine.cycle.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tonypine.cycle.core.data.settings.LanguageRepository
import com.tonypine.cycle.core.model.Language
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Cycle's language, for Settings' Language row and the Language page (`docs/design/language.md`): her
 * choice, and what the phone gives. Choosing applies it through [languages] at once: Cycle redraws in
 * it, Android recreating the activity on 13 and later and the app below, with the back stack and tab
 * kept. On Android 13 and later a choice made on Android's own page reaches her choice when Cycle
 * comes back to the front, so it shows here then.
 */
class LanguageViewModel(private val languages: LanguageRepository) : ViewModel() {
    // The phone's languages can change while Cycle is in the background: read again on each return.
    private val phone = MutableStateFlow(readPhone())

    /** Null until her choice is read. */
    val uiState: StateFlow<LanguageUiState?> =
        combine(languages.language, phone) { chosen, (phoneLanguage, phoneLocale) ->
            LanguageUiState(chosen, phoneLanguage, phoneLocale)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    /** Cycle came back to the front, or was recreated: the phone's languages may have changed. */
    fun onResume() {
        phone.value = readPhone()
    }

    /** Chooses [language], or the phone's language with null. */
    fun onSelect(language: Language?) {
        viewModelScope.launch { languages.setLanguage(language) }
    }

    private fun readPhone(): Pair<Language, Locale?> =
        languages.phoneLanguage() to languages.phoneLocales().firstOrNull()

    private companion object {
        // Keeps the choice flowing through a configuration change without a reload.
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

/**
 * Cycle's language.
 *
 * @property chosen her choice, or null while Cycle follows the phone.
 * @property phoneLanguage what "Phone's language" gives: the first of the phone's languages Cycle has,
 *   or English.
 * @property phoneLocale the phone's first language, if known, which may be one Cycle doesn't have.
 */
data class LanguageUiState(val chosen: Language?, val phoneLanguage: Language, val phoneLocale: Locale? = null) {
    /** The language Cycle is in. */
    val current: Language get() = chosen ?: phoneLanguage

    /** The phone's first language is none of Cycle's, so "Phone's language" says which it uses instead. */
    val phoneNotInCycle: Boolean
        get() = phoneLocale != null && Language.Supported.none { it.locale.language == phoneLocale.language }
}
