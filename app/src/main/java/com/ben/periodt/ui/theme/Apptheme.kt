package com.ben.periodt.ui.theme

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