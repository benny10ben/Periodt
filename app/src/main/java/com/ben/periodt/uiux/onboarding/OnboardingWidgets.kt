package com.ben.periodt.uiux.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ben.periodt.ui.theme.BricolageGrotesque

// --- HIGH PERFORMANCE CANVAS BACKGROUND ---
@Composable
fun AnimatedIconBackground() {
    val isDark = isSystemInDarkTheme()
    val iconColor = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF1B1B1B).copy(alpha = 0.6f)

    // --- 1. ICON TO TEXT MAPPING ---
    val iconLabels = remember {
        mapOf(
            Icons.Rounded.WaterDrop to "Hydration", Icons.Rounded.Eco to "Eco", Icons.Rounded.Air to "Breath", Icons.Rounded.WbSunny to "Sun",
            Icons.Rounded.SelfImprovement to "Mind", Icons.Rounded.Spa to "Relax", Icons.Rounded.Bedtime to "Sleep", Icons.Rounded.Nightlight to "Rest",
            Icons.Rounded.DirectionsRun to "Run", Icons.Rounded.DirectionsBike to "Cycle", Icons.Rounded.FitnessCenter to "Gym", Icons.Rounded.Favorite to "Heart",
            Icons.Rounded.LocalFlorist to "Flora", Icons.Rounded.Pets to "Pets", Icons.Rounded.Park to "Nature", Icons.Rounded.NaturePeople to "Active",
            Icons.Rounded.AcUnit to "Cold", Icons.Rounded.Bolt to "Power", Icons.Rounded.Tsunami to "Waves", Icons.Rounded.DeviceThermostat to "Temp"
        )
    }

    val allRows = remember {
        listOf(
            listOf(Icons.Rounded.WaterDrop, Icons.Rounded.Eco, Icons.Rounded.Air, Icons.Rounded.WbSunny),
            listOf(Icons.Rounded.SelfImprovement, Icons.Rounded.Spa, Icons.Rounded.Bedtime, Icons.Rounded.Nightlight),
            listOf(Icons.Rounded.DirectionsRun, Icons.Rounded.DirectionsBike, Icons.Rounded.FitnessCenter, Icons.Rounded.Favorite),
            listOf(Icons.Rounded.LocalFlorist, Icons.Rounded.Pets, Icons.Rounded.Park, Icons.Rounded.NaturePeople),
            listOf(Icons.Rounded.AcUnit, Icons.Rounded.Bolt, Icons.Rounded.Tsunami, Icons.Rounded.DeviceThermostat),
        )
    }

    val uniqueIcons = remember { allRows.flatten().distinct() }
    val painterMap = uniqueIcons.associateWith { rememberVectorPainter(it) }

    // --- 2. TEXT MEASUREMENT ---
    val textMeasurer = rememberTextMeasurer()
    val textStyle = MaterialTheme.typography.labelSmall.copy(
        fontFamily = BricolageGrotesque,
        fontSize = 10.sp,
        color = iconColor
    )

    val infiniteTransition = rememberInfiniteTransition(label = "bg_drift")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(40000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )

    val density = LocalDensity.current
    val iconSize = with(density) { 24.dp.toPx() }
    val spacing = with(density) { 70.dp.toPx() }
    val rowHeight = with(density) { 80.dp.toPx() }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val totalItemWidth = iconSize + spacing

        allRows.forEachIndexed { i, currentIcons ->
            val patternWidth = totalItemWidth * currentIcons.size
            val rowSpeed = if (i % 2 == 0) 1.2f else 0.7f
            val direction = if (i % 2 == 0) 1f else -1f
            val progress = (angle / 360f)
            val rawOffset = progress * patternWidth * direction * rowSpeed
            val currentOffset = rawOffset % patternWidth

            translate(top = i * rowHeight + rowHeight) {
                val startK = -1
                val endK = (size.width / patternWidth).toInt() + 1

                for (k in startK..endK) {
                    val loopOffset = k * patternWidth

                    currentIcons.forEachIndexed { index, icon ->
                        val xPos = currentOffset + loopOffset + (index * totalItemWidth)

                        if (xPos > -iconSize && xPos < size.width) {
                            val painter = painterMap[icon]
                            painter?.let {
                                translate(left = xPos) {
                                    // Draw Icon
                                    with(it) {
                                        draw(
                                            size = androidx.compose.ui.geometry.Size(iconSize, iconSize),
                                            colorFilter = ColorFilter.tint(iconColor)
                                        )
                                    }

                                    // --- 3. DRAW TEXT BELOW ICON ---
                                    val label = iconLabels[icon] ?: ""
                                    val measuredText = textMeasurer.measure(label, textStyle)

                                    drawText(
                                        textLayoutResult = measuredText,
                                        topLeft = Offset(
                                            x = (iconSize / 2) - (measuredText.size.width / 2),
                                            y = iconSize + 4.dp.toPx()
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- BUTTONS & CARDS ---

@Composable
fun OnboardingButton(text: String, onClick: () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val btnColor = if (isDark) Color.White else Color(0xFF1B1B1B)
    val txtColor = if (isDark) Color(0xFF1B1B1B) else Color.White

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(btnColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontFamily = BricolageGrotesque,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = txtColor
        )
    }
}

@Composable
fun PageIndicator(current: Int, total: Int, activeColor: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(total) { index ->
            val isSelected = index == current
            val width by animateDpAsState(if (isSelected) 24.dp else 8.dp, label = "dotWidth")

            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(width)
                    .clip(CircleShape)
                    .background(if (isSelected) activeColor else activeColor.copy(alpha = 0.2f))
            )
        }
    }
}