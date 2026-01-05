package com.ben.periodt.uiux.shared

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    // New: pass most-likely start date for days-left calculation (ISO yyyy-MM-dd or LocalDate)
    mostLikelyDate: LocalDate? = null
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(22.dp))
                .background(Brush.verticalGradient(listOf(gradTop, gradMid, gradBottom)))
                .background(Color.White.copy(alpha = 0.06f))
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            // Compute days-left once
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
                        title,
                        color = onGradient,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.weight(1f)
                    )
                    ConfidenceIndicator(
                        confidence = confidence,
                        label = confidenceLabel,
                        onGradient = onGradient,
                        onGradientMuted = onGradientMuted
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    windowText,
                    color = onGradient.copy(alpha = 0.95f),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        mostLikely,
                        color = onGradient.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (badge.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(onGradient.copy(alpha = 0.16f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                badge,
                                color = onGradient,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            // Bottom-right days-left label
            if (!daysLeftLabel.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(top = 8.dp) // keeps off text above if it wraps
                        .clip(RoundedCornerShape(999.dp))
                        .background(onGradient.copy(alpha = 0.14f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = daysLeftLabel,
                        color = onGradient,
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
    onGradient: Color,
    onGradientMuted: Color
) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(5) { index ->
                val alpha = if (index < (confidence * 5).toInt()) 0.9f else 0.24f
                Box(
                    modifier = Modifier
                        .size(6.dp) // small dots; keep them subtle
                        .clip(CircleShape)
                        .background(onGradient.copy(alpha = alpha))
                )
            }
        }
        Text(
            text = label,
            color = onGradientMuted,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
