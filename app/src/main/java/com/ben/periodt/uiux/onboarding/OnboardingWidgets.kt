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
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ben.periodt.ui.theme.BricolageGrotesque

// --- HIGH PERFORMANCE CANVAS BACKGROUND ---
@Composable
fun AnimatedIconBackground() {
    val isDark = isSystemInDarkTheme()
    // Reduced alpha for a subtle, professional background drift
    val iconColor = if (isDark) Color.White.copy(alpha = 0.4f) else Color(0xFF1B1B1B).copy(alpha = 0.2f)

    val allRows = remember {
        listOf(
            listOf(Icons.Rounded.WaterDrop, Icons.Rounded.Eco, Icons.Rounded.Air, Icons.Rounded.WbSunny),
            listOf(Icons.Rounded.SelfImprovement, Icons.Rounded.Spa, Icons.Rounded.Bedtime, Icons.Rounded.Nightlight),
            listOf(Icons.Rounded.DirectionsRun, Icons.Rounded.DirectionsBike, Icons.Rounded.FitnessCenter, Icons.Rounded.Favorite),
            listOf(Icons.Rounded.LocalFlorist, Icons.Rounded.Pets, Icons.Rounded.Park, Icons.Rounded.NaturePeople),
            listOf(Icons.Rounded.AcUnit, Icons.Rounded.Bolt, Icons.Rounded.Tsunami, Icons.Rounded.DeviceThermostat),
            listOf(Icons.Rounded.MusicNote, Icons.Rounded.Brush, Icons.Rounded.Palette, Icons.Rounded.AutoAwesome)
        )
    }

    val uniqueIcons = remember { allRows.flatten().distinct() }
    val painterMap = uniqueIcons.associateWith { rememberVectorPainter(it) }

    val infiniteTransition = rememberInfiniteTransition(label = "bg_drift")

    // We use a 0f to 360f range for smoother float precision in long loops
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
    val iconSize = with(density) { 28.dp.toPx() } // Slightly smaller for elegance
    val spacing = with(density) { 70.dp.toPx() }
    val rowHeight = with(density) { 70.dp.toPx() }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val totalItemWidth = iconSize + spacing

        allRows.forEachIndexed { i, currentIcons ->
            val patternWidth = totalItemWidth * currentIcons.size

            // DIFFERENT SPEEDS: Gives depth (Parallax effect)
            val rowSpeed = if (i % 2 == 0) 1.2f else 0.7f
            val direction = if (i % 2 == 0) 1f else -1f

            // PHYSICS: Calculate progress based on the 360-degree loop
            val progress = (angle / 360f)
            val rawOffset = progress * patternWidth * direction * rowSpeed
            val currentOffset = rawOffset % patternWidth

            translate(top = i * rowHeight + rowHeight) {
                // Seamless Looping: Draw just enough to cover screen + 1 pattern width
                val startK = -1
                val endK = (size.width / patternWidth).toInt() + 1

                for (k in startK..endK) {
                    val loopOffset = k * patternWidth

                    currentIcons.forEachIndexed { index, icon ->
                        val xPos = currentOffset + loopOffset + (index * totalItemWidth)

                        // DRAWING: Only paint if actually within screen bounds
                        if (xPos > -iconSize && xPos < size.width) {
                            val painter = painterMap[icon]
                            painter?.let {
                                translate(left = xPos) {
                                    with(it) {
                                        draw(
                                            size = androidx.compose.ui.geometry.Size(iconSize, iconSize),
                                            colorFilter = ColorFilter.tint(iconColor)
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
            .clip(RoundedCornerShape(4.dp))
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
fun ModeCard(title: String, subtitle: String, isSelected: Boolean) {
    val cardColor = if (isSelected) Color(0xFFD89046) else Color.Gray.copy(alpha = 0.1f)
    val contentColor = Color.White

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.9f)
            .clip(RoundedCornerShape(32.dp))
            .background(cardColor)
            .padding(24.dp)
    ) {
        Column(modifier = Modifier.align(Alignment.CenterStart)) {
            Text(
                text = title,
                fontFamily = BricolageGrotesque,
                style = MaterialTheme.typography.headlineMedium,
                color = contentColor,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = subtitle,
                fontFamily = BricolageGrotesque,
                style = MaterialTheme.typography.bodyLarge,
                color = contentColor.copy(alpha = 0.9f)
            )
        }
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(44.dp)
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Color(0xFFD89046),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun PageIndicator(current: Int, total: Int, activeColor: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(total) { index ->
            val isSelected = index == current
            // Animate width for a "worm" indicator effect
            val width by animateDpAsState(if (isSelected) 24.dp else 8.dp, label = "dotWidth")

            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(width)
                    .clip(CircleShape)
                    // All indicators follow activeColor; inactive ones are faded
                    .background(if (isSelected) activeColor else activeColor.copy(alpha = 0.2f))
            )
        }
    }
}