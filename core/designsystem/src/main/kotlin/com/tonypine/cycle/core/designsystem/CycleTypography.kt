package com.tonypine.cycle.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Zest type roles (`docs/design/visual-directions.md`). Display, headline and title use Bricolage
 * Grotesque; everything smaller uses DM Sans. Read them through [CycleTheme.typography].
 *
 * The emphasized variants are for key numbers, such as the cycle day or today's day number.
 */
@Immutable
data class CycleTypography(
    val display: TextStyle,
    val headline: TextStyle,
    val headlineEmphasized: TextStyle,
    val title: TextStyle,
    val titleEmphasized: TextStyle,
    val titleSmall: TextStyle,
    val body: TextStyle,
    val bodySmall: TextStyle,
    val label: TextStyle,
    val labelSmall: TextStyle,
    val dayNumber: TextStyle,
    val dayNumberEmphasized: TextStyle
)

/** Bricolage Grotesque's width axis at its narrowest, and its optical size axis at its largest. */
private const val DISPLAY_WIDTH = 75f
private const val DISPLAY_OPTICAL_SIZE = 96f

// Variable fonts: one file per family, instanced per weight with FontVariation (API 26 and up).
private fun bricolageGrotesque(weight: Int) = Font(
    R.font.bricolage_grotesque,
    FontWeight(weight),
    variationSettings = FontVariation.Settings(
        FontVariation.weight(weight),
        FontVariation.width(DISPLAY_WIDTH),
        FontVariation.Setting("opsz", DISPLAY_OPTICAL_SIZE)
    )
)

private fun dmSans(weight: Int) = Font(
    R.font.dm_sans,
    FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight))
)

/** Bricolage Grotesque at width 75 and optical size 96, for display, headline and title. */
val BricolageGrotesque = FontFamily(bricolageGrotesque(700), bricolageGrotesque(750), bricolageGrotesque(800))

/** DM Sans, for body text, labels and day numbers. */
val DmSans = FontFamily(dmSans(400), dmSans(700), dmSans(900))

private fun style(family: FontFamily, size: Int, lineHeight: Int, weight: Int, tracking: TextUnit = 0.sp) = TextStyle(
    fontFamily = family,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    fontWeight = FontWeight(weight),
    letterSpacing = tracking
)

val ZestTypography = CycleTypography(
    display = style(BricolageGrotesque, 60, 60, 800, (-1).sp),
    headline = style(BricolageGrotesque, 32, 36, 750, (-0.5).sp),
    headlineEmphasized = style(BricolageGrotesque, 32, 36, 800, (-0.5).sp),
    title = style(BricolageGrotesque, 22, 28, 700),
    titleEmphasized = style(BricolageGrotesque, 22, 28, 800),
    titleSmall = style(DmSans, 16, 22, 700),
    body = style(DmSans, 16, 24, 400),
    bodySmall = style(DmSans, 14, 20, 400),
    label = style(DmSans, 14, 20, 700, 0.1.sp),
    labelSmall = style(DmSans, 12, 16, 700, 0.4.sp),
    dayNumber = style(DmSans, 15, 20, 700),
    dayNumberEmphasized = style(DmSans, 15, 20, 900)
)

/**
 * The same roles, hyphenating long words where a line breaks: for a language whose long compounds
 * would otherwise break between any two letters on a narrow line, such as German's "Periodenlänge" at
 * 200% font size. [CycleTheme] uses it where the language's `cycle_hyphenates` says so.
 */
internal fun CycleTypography.hyphenated(): CycleTypography = CycleTypography(
    display = display.hyphenated(),
    headline = headline.hyphenated(),
    headlineEmphasized = headlineEmphasized.hyphenated(),
    title = title.hyphenated(),
    titleEmphasized = titleEmphasized.hyphenated(),
    titleSmall = titleSmall.hyphenated(),
    body = body.hyphenated(),
    bodySmall = bodySmall.hyphenated(),
    label = label.hyphenated(),
    labelSmall = labelSmall.hyphenated(),
    dayNumber = dayNumber.hyphenated(),
    dayNumberEmphasized = dayNumberEmphasized.hyphenated()
)

private fun TextStyle.hyphenated() = copy(hyphens = Hyphens.Auto)
