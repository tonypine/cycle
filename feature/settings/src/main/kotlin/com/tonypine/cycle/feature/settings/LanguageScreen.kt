package com.tonypine.cycle.feature.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tonypine.cycle.core.designsystem.AppBarAction
import com.tonypine.cycle.core.designsystem.CycleIcons
import com.tonypine.cycle.core.designsystem.CycleTheme
import com.tonypine.cycle.core.designsystem.LoadingState
import com.tonypine.cycle.core.designsystem.RadioGroup
import com.tonypine.cycle.core.designsystem.TopAppBar
import com.tonypine.cycle.core.designsystem.cycleLocale
import com.tonypine.cycle.core.designsystem.startingLine
import com.tonypine.cycle.core.model.Language
import java.util.Locale

/** Settings › App › Language, wired to its [viewModel]. [onBack] returns to Settings. */
@Composable
fun LanguageRoute(viewModel: LanguageViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    LanguageScreen(
        rememberLanguageState(viewModel),
        onSelect = viewModel::onSelect,
        onBack = onBack,
        modifier = modifier
    )
}

/** [viewModel]'s state, with the phone's languages read again each time Cycle comes back to the front. */
@Composable
internal fun rememberLanguageState(viewModel: LanguageViewModel): LanguageUiState? {
    LifecycleResumeEffect(viewModel) {
        viewModel.onResume()
        onPauseOrDispose {}
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    return state
}

/**
 * The Language page (`docs/design/language.md`): a top bar with Back, the intro, then five options in
 * one group: Phone's language, then the four languages under their own names, in the same order in
 * every language. Each language's title is marked with its own language, so TalkBack reads "Deutsch"
 * in a German voice, and its second line names it in Cycle's language, except the one Cycle is in.
 * Phone's language says what the phone gives, or that its first language isn't in Cycle and which
 * this option uses instead. Choosing one calls [onSelect] with it, null for the phone's language.
 * Scrolls when the text is large, and keeps its place when Cycle redraws in a new language.
 */
@Composable
fun LanguageScreen(
    state: LanguageUiState?,
    onSelect: (Language?) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = CycleTheme.spacing
    Column(modifier.fillMaxSize()) {
        TopAppBar(
            title = stringResource(R.string.language_title),
            navigation = AppBarAction(CycleIcons.Back, stringResource(R.string.settings_back), onBack)
        )
        if (state == null) {
            LoadingState(Modifier.fillMaxSize())
            return@Column
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                )
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.large)
                .padding(top = spacing.small, bottom = spacing.extraLarge),
            verticalArrangement = Arrangement.spacedBy(spacing.large)
        ) {
            BasicText(
                stringResource(R.string.language_intro),
                style = CycleTheme.typography.body.copy(color = CycleTheme.colors.onSurfaceVariant)
            )
            RadioGroup(
                options = LanguageOptions,
                selected = LanguageOption(state.chosen?.supported),
                onSelect = { onSelect(it.language) },
                title = { option ->
                    option.language?.let { stringResource(it.ownName) } ?: stringResource(R.string.language_phone)
                },
                body = { option ->
                    when (option.language) {
                        null -> phoneLanguageBody(state)
                        state.current -> null
                        else -> stringResource(option.language.translatedName)
                    }
                },
                titleLocale = { option -> option.language?.let { LocaleList(it.tag) } }
            )
        }
    }
}

/**
 * The Settings row's second line: "Phone's language, English" while Cycle follows the phone, or the
 * language she chose, under its own name ("Deutsch"). The language named is the one Cycle is in.
 */
@Composable
internal fun languageSummary(state: LanguageUiState): String {
    val name = stringResource(state.current.ownName)
    return if (state.chosen == null) stringResource(R.string.settings_language_phone, name) else name
}

/**
 * What "Phone's language" gives: that language's name, or, when the phone's first language is none of
 * Cycle's, "French isn't in Cycle yet, so this option uses English." Both names are Android's, in
 * Cycle's language, the first capitalized to start the sentence.
 */
@Composable
private fun phoneLanguageBody(state: LanguageUiState): String {
    val phoneLocale = state.phoneLocale
    if (!state.phoneNotInCycle || phoneLocale == null) return stringResource(state.phoneLanguage.translatedName)
    val locale = cycleLocale()
    return stringResource(
        R.string.language_phone_not_in_cycle,
        startingLine(phoneLocale.getDisplayLanguage(locale), locale),
        state.phoneLanguage.locale.getDisplayName(locale)
    )
}

/** One option of the Language page: a language, or the phone's with null. */
private data class LanguageOption(val language: Language?)

// Phone's language, then the languages in the alphabetical order of their own names, as Android lists
// them: Deutsch, English, Español, Português (Brasil).
private val LanguageOptions: List<LanguageOption> = listOf(LanguageOption(null)) +
    Language.Supported
        .sortedBy { it.locale.getDisplayName(it.locale).lowercase(Locale.ROOT) }
        .map(::LanguageOption)

// The one of Cycle's languages this is, in any region (es-ES is Español), so a tag from Android's
// per-app page never leaves Settings without a name. Cycle has no other: those are in English.
private val Language.supported: Language get() = Language.ofTag(tag) ?: Language.English

/** The language's name in itself, the same in every language: "Deutsch". */
@get:StringRes
internal val Language.ownName: Int
    get() = when (supported.tag) {
        "en" -> R.string.language_own_name_en
        "pt-BR" -> R.string.language_own_name_pt_br
        "es" -> R.string.language_own_name_es
        "de" -> R.string.language_own_name_de
        else -> R.string.language_own_name_en
    }

/** The language's name in Cycle's language: "German". */
@get:StringRes
internal val Language.translatedName: Int
    get() = when (supported.tag) {
        "en" -> R.string.language_name_en
        "pt-BR" -> R.string.language_name_pt_br
        "es" -> R.string.language_name_es
        "de" -> R.string.language_name_de
        else -> R.string.language_name_en
    }

@Preview
@Composable
private fun LanguagePreview() {
    CycleTheme {
        LanguageScreen(
            LanguageUiState(chosen = null, phoneLanguage = Language.English, phoneLocale = Locale.FRANCE),
            onSelect = {},
            onBack = {}
        )
    }
}
