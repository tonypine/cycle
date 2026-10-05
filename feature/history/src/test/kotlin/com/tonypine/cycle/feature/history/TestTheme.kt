package com.tonypine.cycle.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tonypine.cycle.core.designsystem.CycleTheme

/** [content] in [CycleTheme] on the app's `surface`, with motion reduced so every frame is final. */
@Composable
internal fun Themed(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    CycleTheme(darkTheme = darkTheme, reduceMotion = true) {
        Box(Modifier.background(CycleTheme.colors.surface)) { content() }
    }
}
