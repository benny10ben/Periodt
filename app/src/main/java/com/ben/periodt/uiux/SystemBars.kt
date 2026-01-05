package com.ben.periodt.uiux

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun SetSystemBars(
    statusBarColor: Color,
    darkIcons: Boolean
) {
    val context = LocalContext.current
    val view = LocalView.current
    val activity = context as Activity  // MainActivity hosts Compose

    SideEffect {
        val window = activity.window
        // Draw edge-to-edge; content should handle insets if needed
        WindowCompat.setDecorFitsSystemWindows(window, false)

        window.statusBarColor = statusBarColor.toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkIcons

        // Optional: navigation bar to match
        window.navigationBarColor = statusBarColor.toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = darkIcons
    }
}
