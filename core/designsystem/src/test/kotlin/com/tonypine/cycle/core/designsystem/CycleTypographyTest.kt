package com.tonypine.cycle.core.designsystem

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Test

class CycleTypographyTest {
    @Test
    fun everyRoleMatchesTheZestTable() {
        val typography = ZestTypography
        // Font, size / line height, weight and tracking, as in docs/design/visual-directions.md (Zest).
        assertRole(typography.display, BricolageGrotesque, 60, 60, 800, -1f)
        assertRole(typography.headline, BricolageGrotesque, 32, 36, 750, -0.5f)
        assertRole(typography.title, BricolageGrotesque, 22, 28, 700, 0f)
        assertRole(typography.titleSmall, DmSans, 16, 22, 700, 0f)
        assertRole(typography.body, DmSans, 16, 24, 400, 0f)
        assertRole(typography.bodySmall, DmSans, 14, 20, 400, 0f)
        assertRole(typography.label, DmSans, 14, 20, 700, 0.1f)
        assertRole(typography.labelSmall, DmSans, 12, 16, 700, 0.4f)
        assertRole(typography.dayNumber, DmSans, 15, 20, 700, 0f)
    }

    @Test
    fun emphasizedVariantsKeepTheMetricsAndAddWeight() {
        val typography = ZestTypography
        listOf(
            typography.headline to typography.headlineEmphasized,
            typography.title to typography.titleEmphasized,
            typography.dayNumber to typography.dayNumberEmphasized
        ).forEach { (role, emphasized) ->
            assertEquals(role.copy(fontWeight = emphasized.fontWeight), emphasized)
            assert(emphasized.fontWeight!! > role.fontWeight!!) { "$emphasized is not heavier than $role" }
        }
    }

    private fun assertRole(
        style: TextStyle,
        family: FontFamily,
        size: Int,
        lineHeight: Int,
        weight: Int,
        tracking: Float
    ) {
        assertEquals(family, style.fontFamily)
        assertEquals(size.sp, style.fontSize)
        assertEquals(lineHeight.sp, style.lineHeight)
        assertEquals(FontWeight(weight), style.fontWeight)
        assertEquals(tracking.sp, style.letterSpacing)
    }
}
