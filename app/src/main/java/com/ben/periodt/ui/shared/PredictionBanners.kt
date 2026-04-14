package com.ben.periodt.ui.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ben.periodt.ui.theme.BricolageGrotesque
import com.ben.periodt.ui.theme.LocalAppIsDark
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private val SIZE_XXS = 11.sp
private val SIZE_SM  = 13.sp
private val SIZE_MD  = 14.sp
private val SIZE_LG  = 15.sp
private val SIZE_XL  = 20.sp

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
    mostLikelyDate: LocalDate? = null,
    isDiscoveryMode: Boolean = false,
    isLearningMode: Boolean = false,
    isOnPill: Boolean = false,
    discoveryCycle: Int = 1
) {
    val isDark = LocalAppIsDark.current

    // --- COLOR LOGIC ---
    val starIconColor = if (isDark) Color(0xFF8089D2) else Color(0xFF2C3F70)
    val themeAccent = if (isDark) Color(0xFFD89046) else Color(0xFFa5bda3)
    val themeAccent2 = Color(0xFFa68e74)
    val activeAccent = if (isOnPill) themeAccent2 else themeAccent

    val displayTitle = when {
        isDiscoveryMode -> "Discovery Mode"
        isLearningMode  -> "Learning Mode"
        isOnPill        -> "Withdrawal Bleed"
        else            -> title
    }

    val displayBadge = when {
        isDiscoveryMode -> "Paused"
        isLearningMode  -> "Learning"
        isOnPill        -> "Pill Pack"
        else            -> badge
    }

    val displayWindowText = when {
        isDiscoveryMode -> "Predictions are paused while your body recalibrates after the pill."
        isOnPill        -> "Estimated date based on your active pill pack settings."
        else            -> windowText
    }

    val surfaceColor   = if (isDark) Color(0xFF1B1B1B).copy(alpha = 0.5f) else Color.White
    val textPrimary    = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary  = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF1b1b1b).copy(alpha = 0.6f)
    val badgeAlpha     = if (isDark) 0.15f else 0.1f

    Card(
        shape    = RoundedCornerShape(26.dp),
        colors   = CardDefaults.cardColors(containerColor = Color.Transparent),
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
                    val diff  = ChronoUnit.DAYS.between(today, target).toInt()
                    when {
                        diff < 0  -> "Overdue by ${-diff}d"
                        diff == 0 -> "Today"
                        diff == 1 -> "Tomorrow"
                        else      -> "${diff}d left"
                    }
                }
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    if (isDiscoveryMode || isLearningMode || isOnPill) {
                        Icon(
                            imageVector = when {
                                isOnPill -> Icons.Rounded.Medication
                                else     -> Icons.Rounded.AutoAwesome
                            },
                            contentDescription = null,
                            tint = if (isDiscoveryMode || isLearningMode) starIconColor else activeAccent,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(
                            text       = displayTitle,
                            fontFamily = BricolageGrotesque,
                            color      = textPrimary,
                            fontSize   = SIZE_XL,
                            fontWeight = FontWeight.SemiBold,
                            modifier   = Modifier.weight(1f)
                        )
                    }

                    if (isDiscoveryMode || isLearningMode || isOnPill) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(activeAccent.copy(alpha = badgeAlpha))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text       = displayBadge,
                                fontFamily = BricolageGrotesque,
                                fontWeight = FontWeight.SemiBold,
                                color      = activeAccent,
                                fontSize   = SIZE_XXS
                            )
                        }
                    } else {
                        ConfidenceDots(confidence = confidence, accentColor = activeAccent)
                    }
                }

                if (isDiscoveryMode || isLearningMode || isOnPill) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text       = displayTitle,
                        fontFamily = BricolageGrotesque,
                        color      = textPrimary,
                        fontSize   = SIZE_XL,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Text(
                        text       = confidenceLabel,
                        fontFamily = BricolageGrotesque,
                        fontWeight = FontWeight.Normal,
                        color      = textSecondary,
                        fontSize   = SIZE_XXS,
                        modifier   = Modifier.fillMaxWidth().wrapContentWidth(Alignment.End)
                    )
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text       = displayWindowText,
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.Normal,
                    color      = textPrimary.copy(alpha = 0.9f),
                    fontSize   = SIZE_MD
                )

                Spacer(Modifier.height(4.dp))

                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text       = mostLikely,
                        fontFamily = BricolageGrotesque,
                        fontWeight = FontWeight.Normal,
                        color      = textSecondary,
                        fontSize   = SIZE_SM
                    )
                }
            }
            Row(
                modifier = Modifier.align(Alignment.BottomEnd),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isDiscoveryMode && !isLearningMode && !isOnPill && badge.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(activeAccent)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text       = badge,
                            fontFamily = BricolageGrotesque,
                            fontWeight = FontWeight.Bold,
                            color      = Color.White,
                            fontSize   = SIZE_XXS
                        )
                    }
                }

                if (!isDiscoveryMode && !daysLeftLabel.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(activeAccent)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text       = daysLeftLabel,
                            fontFamily = BricolageGrotesque,
                            fontWeight = FontWeight.Bold,
                            color      = Color.White,
                            fontSize   = SIZE_XXS
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConfidenceDots(
    confidence: Float,
    accentColor: Color
) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(5) { index ->
            val isActive = index < (confidence * 5).toInt()
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isActive) accentColor else accentColor.copy(alpha = 0.1f))
            )
        }
    }
}