// uiux/onboarding/OnboardingRoot.kt
package com.ben.periodt.uiux.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.ben.periodt.R

@Composable
private fun CalendarGradient(): Brush {
    val isDark = isSystemInDarkTheme()
    val gradTop = if (isDark) Color(0xFF7B8FA3) else Color(0xFF8FA0B1)
    val gradMid = if (isDark) Color(0xFF7288A0) else Color(0xFF8799B0)
    val gradBottom = if (isDark) Color(0xFF5A7396) else Color(0xFF6E87A7)
    return Brush.verticalGradient(listOf(gradTop, gradMid, gradBottom))
} // static background avoids flashes [web:92]

@Composable
fun OnboardingRoot(
    step: Int,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CalendarGradient())
    ) {
        val config = LocalConfiguration.current
        val isTablet = remember(config.screenWidthDp) { config.screenWidthDp >= 600 }
        val logoSize = if (isTablet) 220.dp else 240.dp
        val lift = if (isTablet) 36.dp else 160.dp
        val shiftRight = if (isTablet) 0.dp else 0.dp


        Image(
            painter = painterResource(R.drawable.logo_trans),
            contentDescription = "App logo",
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = shiftRight, y = -lift)
                .size(logoSize),
            contentScale = ContentScale.Fit
        ) // static chrome [web:92]

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 18.dp),
            contentAlignment = Alignment.TopCenter
        ) { PageIndicator(current = step, total = 3) } // static bar [web:92]

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content
        )
    }
}
