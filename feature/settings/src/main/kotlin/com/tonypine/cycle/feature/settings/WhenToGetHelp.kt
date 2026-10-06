package com.tonypine.cycle.feature.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.tonypine.cycle.core.designsystem.Card
import com.tonypine.cycle.core.designsystem.CycleTheme
import com.tonypine.cycle.core.model.ContraceptionMethod
import com.tonypine.cycle.core.model.ContraceptionMethod.COMBINED_PILL
import com.tonypine.cycle.core.model.ContraceptionMethod.COPPER_IUD
import com.tonypine.cycle.core.model.ContraceptionMethod.HORMONAL_IUD
import com.tonypine.cycle.core.model.ContraceptionMethod.IMPLANT
import com.tonypine.cycle.core.model.ContraceptionMethod.INJECTION
import com.tonypine.cycle.core.model.ContraceptionMethod.PATCH
import com.tonypine.cycle.core.model.ContraceptionMethod.PROGESTOGEN_PILL
import com.tonypine.cycle.core.model.ContraceptionMethod.RING

/**
 * "When to get help" on her method (`docs/decisions/0007-urgent-symptoms-on-a-method.md`): the signs
 * clinics give everyone on it, under the action to take. Reference text, the same for everyone on the
 * method: nothing she logs shows it, hides it or changes it. The words, each with its source, are in
 * `strings.xml`.
 *
 * @property intro who clinics give these signs to, naming the method.
 * @property actions each action line with its signs, the most urgent first.
 */
internal data class GetHelp(@StringRes val intro: Int, val actions: List<GetHelpAction>)

/** An action line, "Get urgent medical advice today if you have:", and the [signs] under it. */
internal data class GetHelpAction(@StringRes val title: Int, val signs: List<Int>)

/**
 * The section on [method]: on the combined pill, patch and ring, and on both IUDs. Null on the
 * progestogen-only pill, implant and injection, whose sources list no urgent signs, and on none. A
 * new method fails to compile here until `0007` decides whether it has one.
 */
internal fun getHelp(method: ContraceptionMethod?): GetHelp? = when (method) {
    COMBINED_PILL -> combined(R.string.get_help_intro_combined_pill)
    PATCH -> combined(R.string.get_help_intro_patch)
    RING -> combined(R.string.get_help_intro_ring)
    COPPER_IUD, HORMONAL_IUD -> GetHelp(R.string.get_help_intro_iud, listOf(IudUrgent))
    PROGESTOGEN_PILL, IMPLANT, INJECTION, null -> null
}

private fun combined(@StringRes intro: Int) = GetHelp(intro, listOf(CombinedEmergency, CombinedUrgent))

private val CombinedEmergency = GetHelpAction(
    R.string.get_help_emergency,
    listOf(R.string.get_help_combined_chest, R.string.get_help_combined_weakness)
)

private val CombinedUrgent = GetHelpAction(R.string.get_help_urgent, listOf(R.string.get_help_combined_leg))

private val IudUrgent = GetHelpAction(
    R.string.get_help_urgent,
    listOf(
        R.string.get_help_iud_painkillers,
        R.string.get_help_iud_sudden_pain,
        R.string.get_help_iud_temperature,
        R.string.get_help_iud_discharge,
        R.string.get_help_iud_bleeding
    )
)

/**
 * The section title, a heading, then one [Card] in the calm treatment of the signal cards: the intro,
 * each action line as a heading with its signs one per line after a bullet TalkBack skips, and that
 * Cycle does not check her log. No icon, no error colours, nothing to tap or dismiss.
 */
@Composable
internal fun WhenToGetHelp(help: GetHelp) {
    val colors = CycleTheme.colors
    val typography = CycleTheme.typography
    SectionTitle(stringResource(R.string.get_help_title))
    Card(Modifier.fillMaxWidth()) {
        BasicText(stringResource(help.intro), style = typography.bodySmall.copy(color = colors.onSurfaceVariant))
        help.actions.forEach { action ->
            Column(verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.extraSmall)) {
                BasicText(
                    stringResource(action.title),
                    modifier = Modifier.semantics { heading() },
                    style = typography.titleSmall.copy(color = colors.onSurface)
                )
                action.signs.forEach { Sign(stringResource(it)) }
            }
        }
        BasicText(
            stringResource(R.string.get_help_not_checked),
            style = typography.bodySmall.copy(color = colors.onSurfaceVariant)
        )
    }
}

/** One sign after a bullet, which TalkBack skips. */
@Composable
private fun Sign(text: String) {
    val style = CycleTheme.typography.body.copy(color = CycleTheme.colors.onSurface)
    Row(horizontalArrangement = Arrangement.spacedBy(CycleTheme.spacing.small)) {
        BasicText(BULLET, modifier = Modifier.clearAndSetSemantics {}, style = style)
        BasicText(text, style = style)
    }
}

private const val BULLET = "•"
