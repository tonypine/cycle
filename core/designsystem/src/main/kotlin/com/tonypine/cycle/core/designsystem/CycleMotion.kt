package com.tonypine.cycle.core.designsystem

import android.content.ContentResolver
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode

/** One spring: how much it bounces ([dampingRatio], 1.0 never overshoots) and how fast ([stiffness]). */
@Immutable
data class CycleSpring(val dampingRatio: Float, val stiffness: Float)

/**
 * The spring motion scheme. Spatial springs move position, size and shape and may overshoot; effects
 * springs fade colour and opacity and never do. Each comes in a fast, default and slow speed. Read it
 * through [CycleTheme.motion] and pass a spec to `animate*AsState`, `Animatable` or `AnimatedVisibility`.
 *
 * When [reduceMotion] is true (the system animator duration scale is 0), every spec is `snap()`, so
 * animations and shape morphs jump straight to their end state.
 */
@Immutable
data class CycleMotion(
    val fastSpatial: CycleSpring,
    val defaultSpatial: CycleSpring,
    val slowSpatial: CycleSpring,
    val fastEffects: CycleSpring,
    val defaultEffects: CycleSpring,
    val slowEffects: CycleSpring,
    val reduceMotion: Boolean = false
) {
    /** Small moves: a press squash, a chip's corners. */
    fun <T> fastSpatialSpec(): FiniteAnimationSpec<T> = fastSpatial.spec()

    /** Most moves and shape changes. */
    fun <T> defaultSpatialSpec(): FiniteAnimationSpec<T> = defaultSpatial.spec()

    /** Large moves: a sheet or a whole-screen change. */
    fun <T> slowSpatialSpec(): FiniteAnimationSpec<T> = slowSpatial.spec()

    /** Quick colour and opacity changes: state layers. */
    fun <T> fastEffectsSpec(): FiniteAnimationSpec<T> = fastEffects.spec()

    /** Most colour and opacity changes. */
    fun <T> defaultEffectsSpec(): FiniteAnimationSpec<T> = defaultEffects.spec()

    /** Slow fades: content appearing or leaving. */
    fun <T> slowEffectsSpec(): FiniteAnimationSpec<T> = slowEffects.spec()

    private fun <T> CycleSpring.spec(): FiniteAnimationSpec<T> =
        if (reduceMotion) snap() else spring(dampingRatio = dampingRatio, stiffness = stiffness)
}

/**
 * Zest motion: bouncy but quick. The default springs are the Zest values in
 * `docs/design/visual-directions.md`; the fast and slow stiffnesses follow Material 3 Expressive's
 * motion scheme, keeping Zest's damping.
 */
val ZestMotion = CycleMotion(
    fastSpatial = CycleSpring(dampingRatio = 0.6f, stiffness = 800f),
    defaultSpatial = CycleSpring(dampingRatio = 0.6f, stiffness = 500f),
    slowSpatial = CycleSpring(dampingRatio = 0.6f, stiffness = 200f),
    fastEffects = CycleSpring(dampingRatio = 1f, stiffness = 3800f),
    defaultEffects = CycleSpring(dampingRatio = 1f, stiffness = 1600f),
    slowEffects = CycleSpring(dampingRatio = 1f, stiffness = 800f)
)

/**
 * True when the system animator duration scale is 0 ("Remove animations" in the accessibility
 * settings). Follows the setting while the app runs. Previews always animate.
 */
@Composable
fun isReduceMotionEnabled(): Boolean {
    if (LocalInspectionMode.current) return false
    val resolver = LocalContext.current.contentResolver
    var reduceMotion by remember(resolver) { mutableStateOf(resolver.animatorDurationScale() == 0f) }
    DisposableEffect(resolver) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                reduceMotion = resolver.animatorDurationScale() == 0f
            }
        }
        resolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
            false,
            observer
        )
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    return reduceMotion
}

private fun ContentResolver.animatorDurationScale(): Float =
    Settings.Global.getFloat(this, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
