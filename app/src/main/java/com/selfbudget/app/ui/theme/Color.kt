package com.selfbudget.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val EmeraldPrimary = Color(0xFF15803D)
val EmeraldLight = Color(0xFF4ADE80)

// High-contrast, theme-adaptive expense and income colors:
val ExpenseRedLight = Color(0xFFDC2626)
val ExpenseRedDark = Color(0xFFF87171)   // Soft vivid coral/rose red (high contrast on dark surface)
val ExpenseRed = Color(0xFFDC2626)

val IncomeGreenLight = Color(0xFF16A34A)
val IncomeGreenDark = Color(0xFF4ADE80)  // Mint green (high contrast on dark surface)
val IncomeGreen = Color(0xFF16A34A)

val DarkBackground = Color(0xFF121412)
val DarkSurface = Color(0xFF1C1F1C)
val DarkSurfaceVariant = Color(0xFF272C27)

@Composable
fun getExpenseColor(): Color {
    return if (isSystemInDarkTheme()) ExpenseRedDark else ExpenseRedLight
}

@Composable
fun getIncomeColor(): Color {
    return if (isSystemInDarkTheme()) IncomeGreenDark else IncomeGreenLight
}
