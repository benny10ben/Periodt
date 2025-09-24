// uiux/onboarding/OnboardingWidgets.kt
package com.ben.periodt.uiux.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun PageIndicator(current: Int, total: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(total) { idx ->
            val active = idx == current

            val isDark = isSystemInDarkTheme()
            val indicontainer = if (isDark) Color(0xFF000000) else Color.White

            Box(
                modifier = Modifier
                    .height(6.dp)
                    .width(if (active) 22.dp else 6.dp)
                    .background(
                        if (active) indicontainer else indicontainer.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(999.dp)
                    )
            )
        }
    }
} // static capsule indicator [web:92]

enum class ButtonEmphasis { Primary, Secondary, Tertiary }

@Composable
fun PillButton(
    label: String,
    emphasis: ButtonEmphasis,
    onClick: () -> Unit
) {

    val isDark = isSystemInDarkTheme()
    val buttonContainer = if (isDark) when (emphasis) {
        ButtonEmphasis.Primary -> Color.Black
        ButtonEmphasis.Secondary -> Color(0x14000000)
        ButtonEmphasis.Tertiary -> Color.Transparent
    } else when (emphasis) {
        ButtonEmphasis.Primary -> Color.White
        ButtonEmphasis.Secondary -> Color(0x14FFFFFF)
        ButtonEmphasis.Tertiary -> Color.Transparent
    }
    val buttonContent = if (isDark) when (emphasis) {
        ButtonEmphasis.Primary -> Color.White
        ButtonEmphasis.Secondary -> Color.White
        ButtonEmphasis.Tertiary -> Color.White.copy(alpha = 0.75f)
    } else when (emphasis) {
        ButtonEmphasis.Primary -> Color.Black
        ButtonEmphasis.Secondary -> Color.White
        ButtonEmphasis.Tertiary -> Color.White.copy(alpha = 0.75f)
    }

    val padding = if (emphasis == ButtonEmphasis.Tertiary) PaddingValues(10.dp) else PaddingValues(14.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (emphasis == ButtonEmphasis.Tertiary) 44.dp else 52.dp)
            .background(buttonContainer, shape = RoundedCornerShape(26.dp))
            .clickable(onClick = onClick)
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = buttonContent, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun ButtonRow(primary: Pair<String, () -> Unit>, secondary: Pair<String, () -> Unit>? = null) {
    Column(Modifier.fillMaxWidth()) {
        PillButton(primary.first, ButtonEmphasis.Primary, onClick = primary.second)
        if (secondary != null) {
            Spacer(Modifier.height(16.dp))
            PillButton(secondary.first, ButtonEmphasis.Secondary, onClick = secondary.second)
        }
    }
}
