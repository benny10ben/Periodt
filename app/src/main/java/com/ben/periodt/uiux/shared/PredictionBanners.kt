package com.ben.periodt.uiux.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ben.periodt.ui.theme.BricolageGrotesque
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun UpcomingBannerEnhanced(
    title: String,
    windowText: String,
    mostLikely: String,
    badge: String,
    confidence: Float,
    confidenceLabel: String,
    gradTop: Color,
    gradMid: Color,
    gradBottom: Color,
    onGradient: Color,
    onGradientMuted: Color,
    mostLikelyDate: LocalDate? = null
) {
    val isDark = isSystemInDarkTheme()

    // UPDATED: Removed alpha from dark mode background for a solid feel
    val surfaceColor = if (isDark) Color(0xFF1B1B1B) else Color.White

    // UPDATED: Accent color now uses Yellow (0xFFD89046) in both modes for consistency
    val accentColor = Color(0xFFD89046)

    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF64748B)

    val badgeAlpha = if (isDark) 0.15f else 0.1f

    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(26.dp))
                .background(surfaceColor)
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            val daysLeftLabel = remember(mostLikelyDate) {
                mostLikelyDate?.let { target ->
                    val today = LocalDate.now()
                    val diff = ChronoUnit.DAYS.between(today, target).toInt()
                    when {
                        diff < 0 -> "Overdue by ${-diff}d"
                        diff == 0 -> "Today"
                        diff == 1 -> "Tomorrow"
                        else -> "${diff}d left"
                    }
                }
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = title,
                        fontFamily = BricolageGrotesque,
                        color = textPrimary,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.weight(1f)
                    )
                    ConfidenceIndicator(
                        confidence = confidence,
                        label = confidenceLabel,
                        accentColor = accentColor,
                        textColorMuted = textSecondary
                    )
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = windowText,
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.Normal,
                    color = textPrimary.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = mostLikely,
                        fontFamily = BricolageGrotesque,
                        fontWeight = FontWeight.Normal,
                        color = textSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    if (badge.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(accentColor.copy(alpha = badgeAlpha))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = badge,
                                fontFamily = BricolageGrotesque,
                                fontWeight = FontWeight.SemiBold,
                                // Text remains Yellow in dark mode, deeper tone in light mode
                                color = if (isDark) accentColor else Color(0xFFB45309),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            if (!daysLeftLabel.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .clip(RoundedCornerShape(50))
                        // Replaced Brand Purple with the wellness Deep Green for high-contrast
                        .background(Color(0xFF2A3825))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = daysLeftLabel,
                        fontFamily = BricolageGrotesque,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
fun ConfidenceIndicator(
    confidence: Float,
    label: String,
    accentColor: Color,
    textColorMuted: Color,
) {
    val dotColor = accentColor

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(5) { index ->
                val isActive = index < (confidence * 5).toInt()
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isActive) dotColor else dotColor.copy(alpha = 0.2f))
                )
            }
        }
        Text(
            text = label,
            fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.Normal,
            color = textColorMuted,
            style = MaterialTheme.typography.labelSmall
        )
    }
}