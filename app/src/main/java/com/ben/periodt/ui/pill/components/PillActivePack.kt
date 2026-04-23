package com.ben.periodt.ui.pill.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ben.periodt.prediction.pretty
import com.ben.periodt.ui.theme.BricolageGrotesque
import com.ben.periodt.ui.theme.LocalAppIsDark
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private val SIZE_XXS = 11.sp
private val SIZE_SM  = 13.sp
private val SIZE_MD  = 14.sp
private val SIZE_LG  = 15.sp

@Composable
fun ActivePackSection(
    startDate: LocalDate,
    pillCount: Int,
    cardBg: Color,
    pillBackground: Color,
    textPrimary: Color,
    textSub: Color,
    accentColor: Color,
    onStop: () -> Unit
) {
    val isDark       = LocalAppIsDark.current
    val daysElapsed  = ChronoUnit.DAYS.between(startDate, LocalDate.now()).toInt().coerceAtLeast(0)
    val daysTaken    = minOf(daysElapsed + 1, pillCount)
    val progressTarget = (daysTaken.toFloat() / pillCount.toFloat()).coerceIn(0f, 1f)

    val animatedProgress by animateFloatAsState(
        targetValue   = progressTarget,
        animationSpec = tween(1200),
        label         = "PillProgress"
    )

    val progressBrush = remember(isDark, accentColor) {
        Brush.linearGradient(colors = listOf(accentColor, accentColor))
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Card(
            colors    = CardDefaults.cardColors(containerColor = cardBg),
            shape     = RoundedCornerShape(22.dp),
            elevation = CardDefaults.cardElevation(0.dp),
            modifier  = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier         = Modifier.size(42.dp).clip(CircleShape).background(pillBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Medication, null, tint = textPrimary, modifier = Modifier.size(24.dp))
                    }

                    Spacer(Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Active Pill Pack",
                            fontFamily = BricolageGrotesque,
                            color      = textPrimary,
                            fontSize   = SIZE_LG,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Started ${startDate.pretty()}",
                            fontFamily = BricolageGrotesque,
                            color      = textSub,
                            fontSize   = SIZE_SM
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(pillBackground)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            "$pillCount Pills",
                            fontFamily = BricolageGrotesque,
                            fontWeight = FontWeight.SemiBold,
                            color      = textPrimary,
                            fontSize   = SIZE_XXS
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        "Pack Progress",
                        fontFamily = BricolageGrotesque,
                        fontWeight = FontWeight.SemiBold,
                        color      = textPrimary,
                        fontSize   = SIZE_MD
                    )
                    Text(
                        "$daysTaken / $pillCount",
                        fontFamily = BricolageGrotesque,
                        color      = textSub,
                        fontSize   = SIZE_SM
                    )
                }

                Spacer(Modifier.height(12.dp))

                Canvas(
                    Modifier
                        .fillMaxWidth(0.95f)
                        .height(6.dp)
                        .clip(CircleShape)
                        .align(Alignment.CenterHorizontally)
                ) {
                    drawRoundRect(color = pillBackground.copy(alpha = 0.4f), size = size, cornerRadius = CornerRadius(50f))
                    drawRoundRect(brush = progressBrush, size = Size(animatedProgress * size.width, size.height), cornerRadius = CornerRadius(50f))
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick   = onStop,
            modifier  = Modifier.fillMaxWidth().height(56.dp),
            shape     = RoundedCornerShape(100.dp),
            colors    = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350).copy(alpha = 0.1f)),
            elevation = ButtonDefaults.buttonElevation(0.dp)
        ) {
            Icon(Icons.Rounded.StopCircle, null, tint = Color(0xFFEF5350))
            Spacer(Modifier.width(8.dp))
            Text(
                "Stop Taking Pills",
                fontSize   = SIZE_LG,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Bold,
                color      = Color(0xFFEF5350)
            )
        }
    }
}