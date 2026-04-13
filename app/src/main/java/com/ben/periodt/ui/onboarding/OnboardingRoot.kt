package com.ben.periodt.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
private fun AppBackgroundGradient(): Brush {
    val isDark = isSystemInDarkTheme()
    return if (isDark) {
        Brush.linearGradient(
            0.0f to Color(0xFF1b1b1b),
            0.7f to Color.Black,
            1.0f to Color.Black,
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color(0xFFf2f0e3), Color(0xFFe8ebed)),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    }
}

@Composable
fun OnboardingRoot(
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackgroundGradient())
    ) {
        content()
    }
}