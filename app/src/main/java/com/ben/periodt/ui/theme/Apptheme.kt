package com.ben.periodt.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

/**
 * Single source of truth for the resolved dark-mode flag.
 *
 * Provided once at the root (MainScreen) from the user's saved ThemeMode
 * preference. Every composable reads this instead of calling
 * isSystemInDarkTheme() directly, so the user's appearance choice is
 * respected everywhere without any additional plumbing.
 *
 * Usage:
 *   val isDark = LocalAppIsDark.current
 */
val LocalAppIsDark = compositionLocalOf { false }

private val LightColors = lightColorScheme()
private val DarkColors  = darkColorScheme()

@Composable
fun PeriodTTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors, // choose scheme [web:321]
        content = content
    )
}
