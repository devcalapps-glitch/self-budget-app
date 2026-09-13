package com.selfbudget.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Design system type scale (spec §1). Only two weights exist app-wide:
 * [FontWeight.Normal] (400, body/metadata) and [FontWeight.Medium] (500,
 * headings/amounts/labels/buttons) — never Bold/SemiBold/ExtraBold.
 * `tnum` is applied to every style so monetary digits line up in columns.
 */
private const val TABULAR_NUMS = "tnum"

private fun style(size: androidx.compose.ui.unit.TextUnit, weight: FontWeight, lineHeight: androidx.compose.ui.unit.TextUnit? = null, letterSpacing: androidx.compose.ui.unit.TextUnit? = null) =
    TextStyle(
        fontSize = size,
        fontWeight = weight,
        lineHeight = lineHeight ?: (size * 1.4f),
        letterSpacing = letterSpacing ?: 0.sp,
        fontFeatureSettings = TABULAR_NUMS,
    )

object SelfBudgetType {
    /** Hero number ($0/day, $26,638.36). 30-36px/500, line-height 1.1. */
    val display = style(32.sp, FontWeight.Medium, lineHeight = 35.sp)

    /** Hero headline ("You're on track"). 20px/500. */
    val title = style(20.sp, FontWeight.Medium, lineHeight = 26.sp)

    /** Page section titles ("Spending plan"). 16px/500. */
    val heading = style(16.sp, FontWeight.Medium, lineHeight = 22.sp)

    /** Header band titles. 14-15px/500. */
    val section = style(15.sp, FontWeight.Medium, lineHeight = 20.sp)

    /** Category / transaction names. 13-14px/500. */
    val rowTitle = style(14.sp, FontWeight.Medium, lineHeight = 19.sp)

    /** Amounts in rows, descriptions. 13-14px/400. */
    val body = style(13.sp, FontWeight.Normal, lineHeight = 19.sp)

    /** "safe to spend", dates, "$X spent of $Y". 11-12px/400. */
    val meta = style(12.sp, FontWeight.Normal, lineHeight = 16.sp)

    /** "DAILY CHECK-IN", "SPENDING PLAN" — apply `.uppercase()` at the call site. 10-11px/500, tracked. */
    val eyebrow = style(11.sp, FontWeight.Medium, lineHeight = 14.sp, letterSpacing = 0.07.em)

    /** Pills: "580% spent", "9 active", counts. 10-11px/500. */
    val badge = style(11.sp, FontWeight.Medium, lineHeight = 14.sp)
}

/**
 * Maps the scale above onto Material3's type slots (size/weight only — no
 * letter-spacing or case transforms here, since these slots are reused
 * broadly) so anything reading `MaterialTheme.typography.*` inherits the new
 * scale automatically. Screens that need a token with no matching slot
 * (eyebrow, badge, section) should reference [SelfBudgetType] directly.
 */
val SelfBudgetTypography = Typography(
    displayLarge = SelfBudgetType.display,
    displayMedium = style(30.sp, FontWeight.Medium, lineHeight = 34.sp),
    displaySmall = style(28.sp, FontWeight.Medium, lineHeight = 32.sp),
    headlineLarge = SelfBudgetType.title,
    headlineMedium = style(18.sp, FontWeight.Medium, lineHeight = 24.sp),
    headlineSmall = SelfBudgetType.heading,
    titleLarge = SelfBudgetType.title,
    titleMedium = SelfBudgetType.heading,
    titleSmall = SelfBudgetType.section,
    bodyLarge = style(14.sp, FontWeight.Normal, lineHeight = 20.sp),
    bodyMedium = SelfBudgetType.body,
    bodySmall = SelfBudgetType.meta,
    labelLarge = SelfBudgetType.rowTitle,
    labelMedium = SelfBudgetType.badge,
    labelSmall = style(11.sp, FontWeight.Medium, lineHeight = 14.sp),
)
