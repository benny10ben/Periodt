package com.ben.periodt.ui.pill.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ben.periodt.prediction.PostPillState
import com.ben.periodt.ui.theme.BricolageGrotesque

private val SIZE_SM = 13.sp
private val SIZE_MD = 14.sp
private val SIZE_LG = 15.sp

@Composable
fun LifeAfterPillCard(
    cardBg: Color,
    textPrimary: Color,
    textSub: Color,
    postPillState: PostPillState
) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "arrowRotation")

    Card(
        colors    = CardDefaults.cardColors(containerColor = cardBg),
        shape     = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier  = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { expanded = !expanded }
            )
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = when (postPillState) {
                        PostPillState.DISCOVERY -> "What is Discovery Mode?"
                        PostPillState.LEARNING  -> "What is Learning Mode?"
                        PostPillState.NORMAL    -> "Life after the pill"
                    },
                    fontFamily = BricolageGrotesque,
                    color      = textPrimary,
                    fontSize   = SIZE_LG,
                    fontWeight = FontWeight.SemiBold,
                    modifier   = Modifier.weight(1f)
                )
                Icon(
                    Icons.Default.KeyboardArrowDown, null,
                    tint     = textPrimary.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp).graphicsLayer { rotationZ = rotation }
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(Modifier.height(18.dp))
                    when (postPillState) {
                        PostPillState.DISCOVERY -> {
                            InfoRow("Why predictions are paused", "Hormonal birth control suppresses ovulation. After stopping, your body needs to restart signaling — this can take weeks to months.", textPrimary, textSub)
                            Spacer(Modifier.height(16.dp))
                            InfoRow("How predictions unlock", "Log your first natural period after stopping. Once you have it, the app enters Learning Mode and predictions go live.", textPrimary, textSub)
                            Spacer(Modifier.height(16.dp))
                            InfoRow("What to expect", "Your first few cycles may be irregular. Spotting is common, and your body typically settles within 3 to 6 months.", textPrimary, textSub)
                        }
                        PostPillState.LEARNING -> {
                            InfoRow("Predictions are live", "The app has enough data to make predictions, but treat them as estimates until we learn your new natural rhythm.", textPrimary, textSub)
                            Spacer(Modifier.height(16.dp))
                            InfoRow("How to reach full accuracy", "Log at least 4 natural cycles after stopping. This gives the algorithm a solid baseline to work with.", textPrimary, textSub)
                        }
                        PostPillState.NORMAL -> {
                            InfoRow("Pill Pack Tracking", "The app tracks your active pills and breaks to help you stay consistent with your specific pack settings.", textPrimary, textSub)
                            Spacer(Modifier.height(16.dp))
                            InfoRow("Stopping the Pill", "If you stop tracking, the app enters Discovery Mode to observe your body's natural rhythm as it restabilizes.", textPrimary, textSub)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(title: String, body: String, textPrimary: Color, textSub: Color) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text       = title,
            fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.SemiBold,
            fontSize   = SIZE_MD,
            color      = textPrimary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text       = body,
            fontFamily = BricolageGrotesque,
            fontSize   = SIZE_SM,
            color      = textSub,
            lineHeight = 20.sp
        )
    }
}