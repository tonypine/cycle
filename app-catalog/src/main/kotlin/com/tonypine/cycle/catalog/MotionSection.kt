package com.tonypine.cycle.catalog

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.tonypine.cycle.core.designsystem.CyclePolygons
import com.tonypine.cycle.core.designsystem.CycleSpring
import com.tonypine.cycle.core.designsystem.CycleTheme
import com.tonypine.cycle.core.designsystem.LocalCycleMotion
import com.tonypine.cycle.core.designsystem.animatedMorphShape
import java.util.Locale
import kotlin.math.roundToInt

@Composable
internal fun MotionSection() {
    val motion = CycleTheme.motion
    var forceReduceMotion by rememberSaveable { mutableStateOf(false) }

    SubsectionTitle("Springs")
    listOf(
        "fastSpatial" to motion.fastSpatial,
        "defaultSpatial" to motion.defaultSpatial,
        "slowSpatial" to motion.slowSpatial,
        "fastEffects" to motion.fastEffects,
        "defaultEffects" to motion.defaultEffects,
        "slowEffects" to motion.slowEffects
    ).forEach { (name, spring) -> SpringRow(name, spring) }
    CatalogText(
        "Spatial springs move position, size and shape, and may overshoot. Effects springs fade colour " +
            "and opacity, and never do.",
        CycleTheme.typography.bodySmall,
        color = CycleTheme.colors.onSurfaceVariant
    )

    SubsectionTitle("Reduce motion")
    CatalogText(
        "System setting: " + if (motion.reduceMotion) "on, every spec is snap()" else "off",
        CycleTheme.typography.body
    )
    CatalogText(
        "With the animator duration scale at 0, every spring becomes snap() and shapes jump to their end. " +
            "Try it here without changing the setting:",
        CycleTheme.typography.bodySmall,
        color = CycleTheme.colors.onSurfaceVariant
    )
    DemoButton(
        if (forceReduceMotion) "Animate the demos" else "Reduce motion in the demos",
        onClick = { forceReduceMotion = !forceReduceMotion }
    )

    CompositionLocalProvider(
        LocalCycleMotion provides motion.copy(reduceMotion = motion.reduceMotion || forceReduceMotion)
    ) {
        SubsectionTitle("Spring demo")
        SpringDemo()
        SubsectionTitle("Shape morph")
        MorphDemo()
    }
}

@Composable
private fun SpringRow(name: String, spring: CycleSpring) {
    Column {
        CatalogText(name, CycleTheme.typography.titleSmall)
        CatalogText(
            "damping ${spring.dampingRatio.format()} · stiffness ${spring.stiffness.toInt()}",
            CycleTheme.typography.bodySmall,
            color = CycleTheme.colors.onSurfaceVariant
        )
    }
}

@Composable
private fun SpringDemo() {
    var atEnd by rememberSaveable { mutableStateOf(false) }
    val progress by animateFloatAsState(
        if (atEnd) 1f else 0f,
        CycleTheme.motion.defaultSpatialSpec(),
        label = "spring demo"
    )
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .background(CycleTheme.colors.surfaceContainer, CycleTheme.shapes.full)
            .padding(CycleTheme.spacing.small)
    ) {
        val ball = 40.dp
        Box(
            Modifier
                .offset { IntOffset(((maxWidth - ball).roundToPx() * progress).roundToInt(), 0) }
                .size(ball)
                .background(CycleTheme.colors.accent, CycleTheme.shapes.full)
        )
    }
    DemoButton("Move with defaultSpatial", onClick = { atEnd = !atEnd })
}

@Composable
private fun MorphDemo() {
    var logged by rememberSaveable { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CycleTheme.spacing.large)
    ) {
        Box(
            Modifier
                .size(64.dp)
                .background(
                    CycleTheme.colors.ovulation,
                    animatedMorphShape(CyclePolygons.circle, CyclePolygons.sun, atEnd = logged)
                )
        )
        Column(verticalArrangement = Arrangement.spacedBy(CycleTheme.spacing.extraSmall)) {
            CatalogText(if (logged) "Sun (ovulation day)" else "Circle", CycleTheme.typography.titleSmall)
            CatalogText(
                "A graphics-shapes Morph on the spatial spring.",
                CycleTheme.typography.bodySmall,
                color = CycleTheme.colors.onSurfaceVariant
            )
        }
    }
    DemoButton(if (logged) "Back to a circle" else "Morph to the sun", onClick = { logged = !logged })
}

private fun Float.format(): String = "%.1f".format(Locale.ROOT, this)
