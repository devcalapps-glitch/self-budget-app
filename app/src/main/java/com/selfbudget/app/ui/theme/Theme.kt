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
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Ramp.Teal.solidFill(isDark = true),
    onPrimary = Ramp.Teal.onSolidFill(isDark = true),
    primaryContainer = Ramp.Teal.tintFill(isDark = true),
    onPrimaryContainer = Ramp.Teal.titleText(isDark = true),
    secondary = Ramp.Teal.solidFill(isDark = true),
    onSecondary = Ramp.Teal.onSolidFill(isDark = true),
    secondaryContainer = Ramp.Teal.tintFill(isDark = true),
    onSecondaryContainer = Ramp.Teal.titleText(isDark = true),
    tertiary = Ramp.Amber.solidFill(isDark = true),
    onTertiary = Ramp.Amber.onSolidFill(isDark = true),
    tertiaryContainer = Ramp.Amber.tintFill(isDark = true),
    onTertiaryContainer = Ramp.Amber.titleText(isDark = true),
    background = PageBackgroundDark,
    onBackground = TextPrimaryDark,
    surface = CardSurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = Ramp.Gray.c800,
    onSurfaceVariant = TextSecondaryDark,
    outline = TextMuted,
    outlineVariant = Ramp.Gray.c800,
    error = Ramp.Red.solidFill(isDark = true),
    onError = Ramp.Red.onSolidFill(isDark = true),
    errorContainer = Ramp.Red.tintFill(isDark = true),
    onErrorContainer = Ramp.Red.titleText(isDark = true),
)

private val LightColorScheme = lightColorScheme(
    primary = Ramp.Teal.solidFill(isDark = false),
    onPrimary = Ramp.Teal.onSolidFill(isDark = false),
    primaryContainer = Ramp.Teal.tintFill(isDark = false),
    onPrimaryContainer = Ramp.Teal.titleText(isDark = false),
    secondary = Ramp.Teal.solidFill(isDark = false),
    onSecondary = Ramp.Teal.onSolidFill(isDark = false),
    secondaryContainer = Ramp.Teal.tintFill(isDark = false),
    onSecondaryContainer = Ramp.Teal.titleText(isDark = false),
    tertiary = Ramp.Amber.solidFill(isDark = false),
    onTertiary = Ramp.Amber.onSolidFill(isDark = false),
    tertiaryContainer = Ramp.Amber.tintFill(isDark = false),
    onTertiaryContainer = Ramp.Amber.titleText(isDark = false),
    background = PageBackgroundLight,
    onBackground = TextPrimaryLight,
    surface = CardSurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = Ramp.Gray.c100,
    onSurfaceVariant = TextSecondaryLight,
    outline = TextMuted,
    outlineVariant = Ramp.Gray.c200,
    error = Ramp.Red.solidFill(isDark = false),
    onError = Ramp.Red.onSolidFill(isDark = false),
    errorContainer = Ramp.Red.tintFill(isDark = false),
    onErrorContainer = Ramp.Red.titleText(isDark = false),
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
            typography = SelfBudgetTypography,
            content = content
        )
    }
}
