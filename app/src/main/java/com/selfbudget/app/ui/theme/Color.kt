package com.selfbudget.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// =============================================================================
// Color ramps — every color in the app comes from one of these 8 ramps.
// Stops: 50, 100, 200, 400, 600, 800, 900. No off-ramp hex values elsewhere.
// =============================================================================
enum class Ramp(
    val c50: Color,
    val c100: Color,
    val c200: Color,
    val c400: Color,
    val c600: Color,
    val c800: Color,
    val c900: Color,
) {
    Teal(Color(0xFFE1F5EE), Color(0xFF9FE1CB), Color(0xFF5DCAA5), Color(0xFF1D9E75), Color(0xFF0F6E56), Color(0xFF085041), Color(0xFF04342C)),
    Blue(Color(0xFFE6F1FB), Color(0xFFB5D4F4), Color(0xFF85B7EB), Color(0xFF378ADD), Color(0xFF185FA5), Color(0xFF0C447C), Color(0xFF042C53)),
    Purple(Color(0xFFEEEDFE), Color(0xFFCECBF6), Color(0xFFAFA9EC), Color(0xFF7F77DD), Color(0xFF534AB7), Color(0xFF3C3489), Color(0xFF26215C)),
    Coral(Color(0xFFFAECE7), Color(0xFFF5C4B3), Color(0xFFF0997B), Color(0xFFD85A30), Color(0xFF993C1D), Color(0xFF712B13), Color(0xFF4A1B0C)),
    Amber(Color(0xFFFAEEDA), Color(0xFFFAC775), Color(0xFFEF9F27), Color(0xFFBA7517), Color(0xFF854F0B), Color(0xFF633806), Color(0xFF412402)),
    Red(Color(0xFFFCEBEB), Color(0xFFF7C1C1), Color(0xFFF09595), Color(0xFFE24B4A), Color(0xFFA32D2D), Color(0xFF791F1F), Color(0xFF501313)),
    Pink(Color(0xFFFBEAF0), Color(0xFFF4C0D1), Color(0xFFED93B1), Color(0xFFD4537E), Color(0xFF993556), Color(0xFF72243E), Color(0xFF4B1528)),
    Gray(Color(0xFFF1EFE8), Color(0xFFD3D1C7), Color(0xFFB4B2A9), Color(0xFF888780), Color(0xFF5F5E5A), Color(0xFF444441), Color(0xFF2C2C2A)),
}

// -----------------------------------------------------------------------------
// One-ramp pairing rules (design system §2): a colored element's fill, title
// text, secondary text/icon and border all come from the SAME ramp, flipping
// stops between light and dark instead of switching hue.
// -----------------------------------------------------------------------------

/** Tinted fill for a band/chip/hero. [large] selects the deeper dark stop used for big surfaces (e.g. alert hero). */
fun Ramp.tintFill(isDark: Boolean, large: Boolean = false): Color =
    if (isDark) (if (large) c900 else c800) else (if (large) c100 else c50)

/** Title text/icon on a tint — the darkest/lightest stop for maximum contrast. */
fun Ramp.titleText(isDark: Boolean): Color = if (isDark) c50 else c900

/** Secondary text/icon on a tint, or a band's icon + meta text. */
fun Ramp.secondaryText(isDark: Boolean): Color = if (isDark) c200 else c600

/** Icon tint on a plain (non-filled) surface — same stops as secondary text. */
fun Ramp.icon(isDark: Boolean): Color = secondaryText(isDark)

/** Tinted 0.5px border for a sectioned container. */
fun Ramp.containerBorder(isDark: Boolean): Color = if (isDark) c600 else c100

/** Count-pill fill on a header band. */
fun Ramp.pillFill(isDark: Boolean): Color = if (isDark) c600 else c100

/** Count-pill text on a header band. */
fun Ramp.pillText(isDark: Boolean): Color = if (isDark) c50 else c800

/** Solid-fill selected/active control (active tab, primary pill background). */
fun Ramp.solidFill(isDark: Boolean): Color = if (isDark) c200 else c800

/** Text/icon that sits on top of [solidFill]. */
fun Ramp.onSolidFill(isDark: Boolean): Color = if (isDark) c900 else c50

// -----------------------------------------------------------------------------
// Section identities (design system §"Section identity colors"). Each content
// section keeps one hue across every page.
// -----------------------------------------------------------------------------
fun sectionRamp(sectionKey: String): Ramp = when (sectionKey) {
    "Housing & Essentials" -> Ramp.Blue
    "Food & Daily Living" -> Ramp.Teal
    "Lifestyle & Entertainment" -> Ramp.Pink
    "Debt & Financial" -> Ramp.Coral
    "Custom Categories" -> Ramp.Purple
    "Accounts and wallets" -> Ramp.Teal
    "Recent activity" -> Ramp.Purple
    "Earned Income" -> Ramp.Teal
    "Investments & Passive" -> Ramp.Purple
    "Gifts & Other" -> Ramp.Pink
    else -> Ramp.Gray // "Other" and anything unmapped
}

// -----------------------------------------------------------------------------
// Status thresholds (design system §"Status thresholds"): <80% Safe (Teal),
// 80-100% Watch (Amber), >100% Over (Red).
// -----------------------------------------------------------------------------
enum class BudgetStatus(val ramp: Ramp) {
    Safe(Ramp.Teal),
    Watch(Ramp.Amber),
    Over(Ramp.Red),
}

fun budgetStatus(spentRatio: Float): BudgetStatus = when {
    spentRatio > 1f -> BudgetStatus.Over
    spentRatio >= 0.8f -> BudgetStatus.Watch
    else -> BudgetStatus.Safe
}

fun budgetStatus(spent: Double, limit: Double): BudgetStatus =
    if (limit <= 0.0) (if (spent > 0.0) BudgetStatus.Over else BudgetStatus.Safe)
    else budgetStatus((spent / limit).toFloat())

// =============================================================================
// Neutral surfaces & text (design system §"Surfaces and text (neutral)")
// =============================================================================
val PageBackgroundLight = Color(0xFFFFFFFF)
val PageBackgroundDark = Color(0xFF1C1D1C)
val CardSurfaceLight = Ramp.Gray.c50
val CardSurfaceDark = Ramp.Gray.c900
val DividerLight = Color(0x14000000) // rgba(0,0,0,0.08)
val DividerDark = Ramp.Gray.c800
val TextPrimaryLight = Ramp.Gray.c900
val TextPrimaryDark = Ramp.Gray.c50
val TextSecondaryLight = Ramp.Gray.c600
val TextSecondaryDark = Ramp.Gray.c200
val TextMuted = Ramp.Gray.c400
val ProgressTrackLight = Ramp.Gray.c50
val ProgressTrackDark = Ramp.Gray.c800

// Backward-compatible aliases kept because other files still reference these
// exact names directly (HomeScreen.kt, ThemeColorPreviews.kt). Values now come
// from the ramp system above rather than the old ad-hoc palette.
val DarkBackground = PageBackgroundDark
val DarkSurface = CardSurfaceDark
val WarningAmberDark = Ramp.Amber.c100

@Composable
fun isAppInDarkTheme(): Boolean {
    val fromLocal = LocalIsDarkTheme.current
    val fromMaterial = MaterialTheme.colorScheme.background == DarkBackground || MaterialTheme.colorScheme.surface == DarkSurface
    return fromLocal || fromMaterial
}

// =============================================================================
// Money colors (design system §"Money colors")
// =============================================================================

/** Income / positive amounts: Teal 600 light, Teal 100 dark. */
@Composable
fun getIncomeColor(): Color = if (isAppInDarkTheme()) Ramp.Teal.c100 else Ramp.Teal.c600

/**
 * Red is reserved for over-limit / negative-balance amounts, not ordinary
 * spending — ordinary expense amounts in lists/rows should use the neutral
 * text-primary color (`MaterialTheme.colorScheme.onSurface`) instead of this.
 * Kept for "this represents an expense" semantics: type badges, outflow dots,
 * over-limit and negative-balance text. Red 600 light, Red 200 dark.
 */
@Composable
fun getExpenseColor(): Color = if (isAppInDarkTheme()) Ramp.Red.c200 else Ramp.Red.c600

/** Watch-status / "safe to spend" at 80-100% of budget: Amber 800 light, Amber 100 dark. */
@Composable
fun getWarningColor(): Color = if (isAppInDarkTheme()) Ramp.Amber.c100 else Ramp.Amber.c800

@Composable
fun getOnWarningColor(): Color = if (isAppInDarkTheme()) Ramp.Amber.c900 else Color.White

/**
 * Brand/interactive color — Teal is the app's only brand color (design system
 * §18): active tab, selected filter, links, and primary actions all read from
 * it. `getAccentColor()` used to mean "blue interactive accent"; it now
 * resolves to the same Teal value as [getBrandColor] so existing call sites
 * (links, active states, selected pickers) fall in line with the one-brand
 * rule without needing to touch every call site individually.
 */
@Composable
fun getAccentColor(): Color = getBrandColor()

@Composable
fun getBrandColor(): Color = if (isAppInDarkTheme()) Ramp.Teal.c200 else Ramp.Teal.c600
