package com.tonypine.cycle.catalog

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tonypine.cycle.core.designsystem.CycleTheme
import com.tonypine.cycle.core.designsystem.FilledButton
import com.tonypine.cycle.core.designsystem.LoadingIndicator
import com.tonypine.cycle.core.designsystem.LoadingState
import com.tonypine.cycle.core.designsystem.LocalCycleMotion

@Composable
internal fun LoadingSection() {
    val motion = CycleTheme.motion
    var forceReduceMotion by rememberSaveable { mutableStateOf(false) }
    FilledButton(
        if (forceReduceMotion) "Animate" else "Reduce motion",
        onClick = { forceReduceMotion = !forceReduceMotion }
    )
    CompositionLocalProvider(
        LocalCycleMotion provides motion.copy(reduceMotion = motion.reduceMotion || forceReduceMotion)
    ) {
        SubsectionTitle("LoadingIndicator")
        LoadingIndicator()
        CatalogText(
            "48dp, in line with content. Morphs from the sun to a clover, a pentagon and a cookie, a quarter " +
                "turn each step, on the slow spatial spring. Still under reduce motion.",
            CycleTheme.typography.bodySmall,
            color = CycleTheme.colors.onSurfaceVariant
        )

        SubsectionTitle("LoadingState")
        LoadingState(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .border(1.dp, CycleTheme.colors.outlineVariant, CycleTheme.shapes.large),
            message = "Loading your cycle"
        )
        CatalogText(
            "Fills the screen, or the part of it that is waiting. TalkBack reads one indeterminate progress bar " +
                "described by the message, or \"Loading\".",
            CycleTheme.typography.bodySmall,
            color = CycleTheme.colors.onSurfaceVariant
        )
    }
}
