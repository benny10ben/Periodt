package com.ben.periodt.ui.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ben.periodt.ui.theme.BricolageGrotesque

private val SIZE_XS = 12.sp
private val SIZE_SM = 13.sp
private val SIZE_MD = 14.sp

@Composable
fun AlgoStep(
    number: Int,
    title: String,
    description: String,
    textPrimary: Color,
    textSub: Color,
    accentColor: Color,
    formula: String? = null,
    extraContent: (@Composable () -> Unit)? = null
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$number",
                fontFamily = BricolageGrotesque,
                color = accentColor,
                fontSize = SIZE_XS,
                fontWeight = FontWeight.Bold,
                lineHeight = SIZE_XS
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                text = title,
                fontFamily = BricolageGrotesque,
                color = textPrimary,
                fontSize = SIZE_MD,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                fontFamily = BricolageGrotesque,
                color = textSub,
                fontSize = SIZE_SM,
                lineHeight = 19.sp
            )
            formula?.let {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(textSub.copy(alpha = 0.08f))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = it,
                        fontFamily = BricolageGrotesque,
                        color = accentColor,
                        fontSize = SIZE_XS,
                        letterSpacing = 0.2.sp
                    )
                }
            }
            extraContent?.invoke()
        }
    }
}

@Composable
fun AlgoRegularityTable(
    textPrimary: Color,
    textSub: Color,
    isDark: Boolean
) {
    data class RegRow(
        val stdDev: String,
        val label: String,
        val chipBg: Color,
        val chipFg: Color,
        val window: String
    )

    val rows = listOf(
        RegRow("≤ 2 days", "Very regular",       Color(0xFF4CAF50).copy(alpha = if (isDark) 0.25f else 0.12f), Color(if (isDark) 0xFF81C784 else 0xFF2E7D32), "± 1 day"),
        RegRow("2–4 days", "Regular",             Color(0xFF66BB6A).copy(alpha = if (isDark) 0.20f else 0.10f), Color(if (isDark) 0xFFA5D6A7 else 0xFF388E3C), "± 2–4 days"),
        RegRow("4–6 days", "Somewhat irregular",  Color(0xFFFFB74D).copy(alpha = if (isDark) 0.25f else 0.12f), Color(if (isDark) 0xFFFFCC80 else 0xFFE65100), "± 5–9 days"),
        RegRow("> 6 days",  "Irregular",           Color(0xFFEF5350).copy(alpha = if (isDark) 0.25f else 0.10f), Color(if (isDark) 0xFFEF9A9A else 0xFFC62828), "± 9+ days"),
    )

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .border(0.5.dp, textSub.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
    ) {
        // Header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(textSub.copy(alpha = 0.06f))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text("Std dev", modifier = Modifier.weight(1.1f), color = textSub, fontSize = SIZE_XS, fontFamily = BricolageGrotesque, fontWeight = FontWeight.SemiBold)
            Text("Label",   modifier = Modifier.weight(1.5f), color = textSub, fontSize = SIZE_XS, fontFamily = BricolageGrotesque, fontWeight = FontWeight.SemiBold)
            Text("Window",  modifier = Modifier.weight(1.0f), color = textSub, fontSize = SIZE_XS, fontFamily = BricolageGrotesque, fontWeight = FontWeight.SemiBold)
        }
        rows.forEach { row ->
            HorizontalDivider(color = textSub.copy(alpha = 0.08f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(row.stdDev, modifier = Modifier.weight(1.1f), color = textSub,     fontSize = SIZE_XS, fontFamily = BricolageGrotesque)
                Box(modifier = Modifier.weight(1.5f)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(99.dp))
                            .background(row.chipBg)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(row.label, color = row.chipFg, fontSize = SIZE_XS, fontFamily = BricolageGrotesque, fontWeight = FontWeight.Medium)
                    }
                }
                Text(row.window, modifier = Modifier.weight(1.0f), color = textPrimary, fontSize = SIZE_XS, fontFamily = BricolageGrotesque)
            }
        }
    }
}