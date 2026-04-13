package com.ben.periodt.ui.theme

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color // You can remove this import if no longer used elsewhere
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
    val activity = context as Activity

    SideEffect {
        val window = activity.window

        // Tells Android to let your app draw under the system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)

        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkIcons
        WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = darkIcons
    }
}