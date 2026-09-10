package com.selfbudget.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Modern Fintech Palette
val IndigoPrimary = Color(0xFF4F46E5)      // Modern Indigo / Tech-Forward
val IndigoLight = Color(0xFF818CF8)        // Soft Luminous Indigo for Dark Mode

val EmeraldPrimary = Color(0xFF059669)     // Crisp Emerald
val EmeraldLight = Color(0xFF34D399)       // Crisp Mint

// High-contrast, theme-adaptive expense and income colors:
val ExpenseRedLight = Color(0xFFEF4444)    // Modern vibrant coral/rose
val ExpenseRedDark = Color(0xFFF87171)     // Soft vivid coral/rose (high contrast on dark surface)
val ExpenseRed = Color(0xFFEF4444)

val GoldPrimary = Color(0xFFD97706)
val GoldLight = Color(0xFFFBBF24)

val IncomeGreenLight = Color(0xFF059669)   // Modern crisp Emerald for Light Mode
val IncomeGreenDark = Color(0xFF34D399)    // Luminous Mint for Dark Mode
val IncomeGreen = Color(0xFF059669)

// Modern Slate / Obsidian Theme Surfaces
val DarkBackground = Color(0xFF0B0F19)     // Deep Obsidian Slate
val DarkSurface = Color(0xFF131B2E)        // Sleek elevated dark slate card
val DarkSurfaceVariant = Color(0xFF1E293B) // Slate 800

@Composable
fun isAppInDarkTheme(): Boolean {
    val fromLocal = LocalIsDarkTheme.current
    val fromMaterial = MaterialTheme.colorScheme.background == DarkBackground || MaterialTheme.colorScheme.surface == DarkSurface
    return fromLocal || fromMaterial
}

@Composable
fun getExpenseColor(): Color {
    return if (isAppInDarkTheme()) ExpenseRedDark else ExpenseRedLight
}

@Composable
fun getIncomeColor(): Color {
    return if (isAppInDarkTheme()) IncomeGreenDark else IncomeGreenLight
}

