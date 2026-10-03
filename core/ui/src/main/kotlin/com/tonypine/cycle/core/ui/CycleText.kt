package com.tonypine.cycle.core.ui

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.tonypine.cycle.core.designsystem.CycleTheme

/** Text drawn with the theme's typography and colours. */
@Composable
fun CycleText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = CycleTheme.typography.body,
    color: Color = CycleTheme.colors.onBackground
) {
    BasicText(text = text, modifier = modifier, style = style.copy(color = color))
}
