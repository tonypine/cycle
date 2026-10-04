package com.tonypine.cycle.core.designsystem

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CycleMotionTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val resolver = ApplicationProvider.getApplicationContext<Context>().contentResolver

    @Test
    fun defaultSpringsAreTheZestValues() {
        // docs/design/visual-directions.md (Zest): spatial spring(0.6, 500), effects spring(1.0, 1600).
        assertEquals(CycleSpring(dampingRatio = 0.6f, stiffness = 500f), ZestMotion.defaultSpatial)
        assertEquals(CycleSpring(dampingRatio = 1f, stiffness = 1600f), ZestMotion.defaultEffects)
        assertEquals(spring<Float>(dampingRatio = 0.6f, stiffness = 500f), ZestMotion.defaultSpatialSpec<Float>())
        assertEquals(spring<Float>(dampingRatio = 1f, stiffness = 1600f), ZestMotion.defaultEffectsSpec<Float>())
    }

    @Test
    fun spatialSpringsBounceAndEffectsSpringsNever() {
        listOf(ZestMotion.fastSpatial, ZestMotion.defaultSpatial, ZestMotion.slowSpatial).forEach {
            assertEquals(0.6f, it.dampingRatio)
        }
        listOf(ZestMotion.fastEffects, ZestMotion.defaultEffects, ZestMotion.slowEffects).forEach {
            assertEquals("$it overshoots", 1f, it.dampingRatio)
        }
        assertTrue(ZestMotion.fastSpatial.stiffness > ZestMotion.defaultSpatial.stiffness)
        assertTrue(ZestMotion.defaultSpatial.stiffness > ZestMotion.slowSpatial.stiffness)
        assertTrue(ZestMotion.fastEffects.stiffness > ZestMotion.defaultEffects.stiffness)
        assertTrue(ZestMotion.defaultEffects.stiffness > ZestMotion.slowEffects.stiffness)
    }

    @Test
    fun everySpecSnapsWhenTheAnimatorDurationScaleIsZero() {
        Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        var motion: CycleMotion? = null
        composeRule.setContent { CycleTheme { motion = CycleTheme.motion } }

        assertTrue(motion!!.reduceMotion)
        motion!!.allSpecs().forEach { (name, spec) -> assertEquals(name, snap<Float>(), spec) }
    }

    @Test
    fun everySpecSpringsAtTheDefaultScale() {
        Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        var motion: CycleMotion? = null
        composeRule.setContent { CycleTheme { motion = CycleTheme.motion } }

        assertFalse(motion!!.reduceMotion)
        assertEquals(ZestMotion, motion)
        motion!!.allSpecs().forEach { (name, spec) -> assertTrue("$name is $spec", spec is SpringSpec) }
    }

    @Test
    fun followsTheSettingWhileRunning() {
        Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        var reduceMotion: Boolean? = null
        composeRule.setContent { reduceMotion = isReduceMotionEnabled() }
        composeRule.runOnIdle { assertEquals(false, reduceMotion) }

        Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        composeRule.waitForIdle()
        assertEquals(true, reduceMotion)
    }

    private fun CycleMotion.allSpecs(): List<Pair<String, FiniteAnimationSpec<Float>>> = listOf(
        "fastSpatial" to fastSpatialSpec(),
        "defaultSpatial" to defaultSpatialSpec(),
        "slowSpatial" to slowSpatialSpec(),
        "fastEffects" to fastEffectsSpec(),
        "defaultEffects" to defaultEffectsSpec(),
        "slowEffects" to slowEffectsSpec()
    )
}
