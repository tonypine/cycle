package com.tonypine.cycle.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.tonypine.cycle.core.designsystem.CycleBottomSheet
import com.tonypine.cycle.core.designsystem.CycleBottomSheetState
import com.tonypine.cycle.core.designsystem.CycleTextField
import com.tonypine.cycle.core.designsystem.CycleTheme
import com.tonypine.cycle.core.designsystem.FilledButton
import com.tonypine.cycle.core.designsystem.FilterChip
import com.tonypine.cycle.core.designsystem.OutlinedButton
import com.tonypine.cycle.core.designsystem.TextButton
import com.tonypine.cycle.core.designsystem.TonalButton
import com.tonypine.cycle.core.designsystem.rememberCycleBottomSheetState
import kotlinx.coroutines.launch

private val Moods = listOf("Calm", "Happy", "Tired", "Grumpy", "Anxious")

@Composable
internal fun BottomSheetSection() {
    val sheet = rememberCycleBottomSheetState()
    val scope = rememberCoroutineScope()
    var closedCount by rememberSaveable { mutableIntStateOf(0) }

    SubsectionTitle("Try it")
    CatalogText(
        "Open the sheet: it rises half way on the slow spatial spring. Drag it up to expand it, and " +
            "type a note: it stays above the keyboard. Close it by dragging it down, tapping the dimmed " +
            "screen, going back, or with TalkBack's Dismiss action on the handle.",
        CycleTheme.typography.bodySmall,
        color = CycleTheme.colors.onSurfaceVariant
    )
    FilledButton("Open the sheet", onClick = { scope.launch { sheet.show() } }, modifier = Modifier.fillMaxWidth())
    TonalButton("Open it expanded", onClick = { scope.launch { sheet.expand() } }, modifier = Modifier.fillMaxWidth())
    CatalogText(
        "Closed $closedCount ${if (closedCount == 1) "time" else "times"}.",
        CycleTheme.typography.bodySmall,
        color = CycleTheme.colors.onSurfaceVariant
    )

    SubsectionTitle("Anatomy")
    CatalogText(
        "A surfaceContainer panel with 32dp top corners over the scrim. At the top, a 32 by 4dp " +
            "handle in a 48dp target that expands and collapses the sheet, then the title as a heading, " +
            "then the content, padded above the navigation bar and the keyboard.",
        CycleTheme.typography.body,
        color = CycleTheme.colors.onSurfaceVariant
    )

    CycleBottomSheet(sheet, title = "Log today", onDismiss = { closedCount++ }) {
        DemoSheetContent(sheet)
    }
}

/** A short log form. Its values are synthetic and never leave the sheet. */
@Composable
private fun DemoSheetContent(sheet: CycleBottomSheetState) {
    val scope = rememberCoroutineScope()
    var moods by rememberSaveable { mutableStateOf(setOf<String>()) }
    CatalogText(
        "How are you feeling? Pick anything that fits.",
        CycleTheme.typography.body,
        color = CycleTheme.colors.onSurfaceVariant
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(CycleTheme.spacing.small),
        verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.small)
    ) {
        Moods.forEach { mood ->
            FilterChip(
                mood,
                selected = mood in moods,
                onClick = { moods = if (mood in moods) moods - mood else moods + mood }
            )
        }
    }
    CycleTextField(
        rememberTextFieldState(),
        label = "Notes",
        modifier = Modifier.fillMaxWidth(),
        placeholder = "Anything else? Spill it here.",
        supportingText = "Only on this phone.",
        maxLength = 200,
        lineLimits = TextFieldLineLimits.MultiLine(minHeightInLines = 3)
    )
    FilledButton("Log it", onClick = { scope.launch { sheet.hide() } }, modifier = Modifier.fillMaxWidth())
    OutlinedButton("Expand", onClick = { scope.launch { sheet.expand() } }, modifier = Modifier.fillMaxWidth())
    TextButton("Nah", onClick = { scope.launch { sheet.hide() } }, modifier = Modifier.fillMaxWidth())
}
