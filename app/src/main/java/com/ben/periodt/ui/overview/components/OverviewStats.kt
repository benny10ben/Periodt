package com.ben.periodt.ui.overview.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ben.periodt.ui.theme.BricolageGrotesque
import com.ben.periodt.ui.theme.LocalAppIsDark

private val SIZE_XXS = 11.sp
private val SIZE_SM  = 13.sp
private val SIZE_LG  = 15.sp
private val SIZE_XL  = 20.sp

@Composable
fun CombinedStatsCard(
    totalCycles: String,
    avgPeriod: String,
    avgCycle: String,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    val isDark       = LocalAppIsDark.current
    val surfaceColor = if (isDark) Color(0xFF1B1B1B).copy(alpha = 0.5f) else Color.White
    val dividerColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f)
    val valueColor   = if (isDark) Color.White else Color(0xFF0F172A)
    val titleColor   = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF1b1b1b).copy(alpha = 0.6f)

    val gentleSpring = spring<IntSize>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)

    Card(
        modifier  = modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = gentleSpring)
            .clip(RoundedCornerShape(26.dp))
            .clickable(interactionSource = interactionSource, indication = null) { isExpanded = !isExpanded },
        colors    = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape     = RoundedCornerShape(26.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier         = Modifier.fillMaxWidth().background(surfaceColor).padding(vertical = 24.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState  = isExpanded,
                transitionSpec = {
                    fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) togetherWith
                            fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) using
                            SizeTransform(clip = false)
                },
                label = "stats_expansion"
            ) { expanded ->
                if (expanded) {
                    Column(
                        modifier            = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        ExpandedStatRow("Total Cycles",   "Complete logged history",   totalCycles, valueColor, titleColor)
                        ExpandedStatRow("Average Period", "Typical bleeding duration", avgPeriod,   valueColor, titleColor)
                        ExpandedStatRow("Average Cycle",  "Start-to-start gap",        avgCycle,    valueColor, titleColor)
                    }
                } else {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem("CYCLES", totalCycles, valueColor, titleColor, Modifier.weight(1f))
                        Box(modifier = Modifier.width(1.dp).height(36.dp).background(dividerColor))
                        StatItem("PERIOD", avgPeriod,   valueColor, titleColor, Modifier.weight(1f))
                        Box(modifier = Modifier.width(1.dp).height(36.dp).background(dividerColor))
                        StatItem("CYCLE",  avgCycle,    valueColor, titleColor, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpandedStatRow(
    title: String, subtitle: String, value: String,
    valueColor: Color, titleColor: Color
) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text       = title,
                color      = valueColor,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.SemiBold,
                fontSize   = SIZE_LG
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text       = subtitle,
                color      = titleColor,
                fontFamily = BricolageGrotesque,
                fontSize   = SIZE_SM
            )
        }
        Text(
            text       = value,
            color      = valueColor,
            fontFamily = BricolageGrotesque,
            fontSize   = SIZE_XL,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun StatItem(
    title: String, value: String,
    valueColor: Color, titleColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text       = value,
            fontFamily = BricolageGrotesque,
            fontSize   = SIZE_XL,
            fontWeight = FontWeight.Bold,
            color      = valueColor,
            maxLines   = 1,
            textAlign  = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text          = title,
            fontFamily    = BricolageGrotesque,
            fontSize      = SIZE_XXS,
            fontWeight    = FontWeight.Medium,
            letterSpacing = 1.sp,
            color         = titleColor,
            maxLines      = 1,
            textAlign     = TextAlign.Center
        )
    }
}