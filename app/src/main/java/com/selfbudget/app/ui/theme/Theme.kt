package com.selfbudget.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF818CF8),          // Luminous Indigo 400
    onPrimary = Color(0xFF1E1B4B),
    primaryContainer = Color(0xFF312E81), // Deep Indigo
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = Color(0xFF38BDF8),        // Sky 400
    onSecondary = Color(0xFF082F49),
    secondaryContainer = Color(0xFF0369A1),
    onSecondaryContainer = Color(0xFFE0F2FE),
    tertiary = Color(0xFFA78BFA),         // Violet 400
    onTertiary = Color(0xFF2E1065),
    tertiaryContainer = Color(0xFF5B21B6),
    onTertiaryContainer = Color(0xFFEDE9FE),
    background = DarkBackground,          // Deep Obsidian Slate #0B0F19
    onBackground = Color(0xFFF8FAFC),
    surface = DarkSurface,                // Elevated Dark Slate Card #131B2E
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = DarkSurfaceVariant,  // Slate 800 #1E293B
    onSurfaceVariant = Color(0xFF94A3B8), // Slate 400
    outline = Color(0xFF334155),          // Slate 700
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF4F46E5),          // Modern Indigo 600
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEEF2FF), // Indigo 50
    onPrimaryContainer = Color(0xFF312E81),
    secondary = Color(0xFF0284C7),        // Ocean / Sky 600
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF075985),
    tertiary = Color(0xFF7C3AED),         // Violet 600
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF5F3FF),
    onTertiaryContainer = Color(0xFF581C87),
    background = Color(0xFFF8FAFC),       // Clean, modern slate-50 background
    onBackground = Color(0xFF0F172A),     // Crisp Slate 900
    surface = Color.White,                // Crisp pure white card surface
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),   // Slate 100
    onSurfaceVariant = Color(0xFF64748B), // Slate 500
    outline = Color(0xFFE2E8F0),          // Subtle, clean slate-200 card border
    error = Color(0xFFEF4444),            // Modern Rose / Coral
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF991B1B)
)

val LocalIsDarkTheme = staticCompositionLocalOf { false }

@Composable
fun SelfBudgetTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
