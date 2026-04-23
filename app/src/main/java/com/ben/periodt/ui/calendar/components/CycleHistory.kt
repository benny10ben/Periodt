package com.ben.periodt.ui.calendar.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ben.periodt.ui.theme.BricolageGrotesque
import com.ben.periodt.ui.theme.LocalAppIsDark
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs
import kotlin.math.absoluteValue

private val SIZE_XXS = 11.sp
private val SIZE_SM  = 13.sp
private val SIZE_LG  = 15.sp

@Composable
fun SwipeToDeleteCard(onDelete: () -> Unit, content: @Composable (Boolean) -> Unit) {
    val density     = LocalDensity.current
    val scope       = rememberCoroutineScope()
    val offsetX     = remember { Animatable(0f) }
    val revealDp    = 80.dp
    val revealPx    = with(density) { revealDp.toPx() }
    val deleteThreshold = with(density) { 180.dp.toPx() }
    val maxRevealPx = with(density) { 220.dp.toPx() }
    var widthPx by remember { mutableStateOf(0f) }
    val itemShape   = RoundedCornerShape(22.dp)
    val bounceSpring = spring<Float>(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)
    val isRevealed  by remember { derivedStateOf { offsetX.value.absoluteValue > revealPx / 2f } }
    val isSwiping   by remember { derivedStateOf { offsetX.value.absoluteValue > 2f } }

    Card(
        shape     = itemShape,
        colors    = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier  = Modifier.fillMaxWidth().onSizeChanged { widthPx = it.width.toFloat() }
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.matchParentSize().graphicsLayer { shape = itemShape; clip = true }
                    .background(Color.Red.copy(alpha = if (offsetX.value.absoluteValue > 10f) 0.8f else 0f))
                    .padding(horizontal = 24.dp),
                contentAlignment = if (offsetX.value >= 0f) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                if (isRevealed) {
                    Icon(Icons.Default.Delete, "Delete", tint = Color.White, modifier = Modifier.size(24.dp))
                    Box(
                        modifier = Modifier.fillMaxHeight().width(revealDp).clickable(
                            interactionSource = remember { MutableInteractionSource() }, indication = null
                        ) {
                            scope.launch {
                                val target = if (offsetX.value >= 0f) widthPx else -widthPx
                                offsetX.animateTo(target, bounceSpring)
                                onDelete()
                            }
                        }
                    )
                }
            }

            Box(
                modifier = Modifier.fillMaxWidth().graphicsLayer { shape = itemShape; clip = true }
                    .offset { IntOffset(offsetX.value.toInt(), 0) }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                scope.launch {
                                    val currentOff = offsetX.value
                                    val target = when {
                                        currentOff <= -deleteThreshold -> -widthPx
                                        currentOff >= deleteThreshold  ->  widthPx
                                        currentOff <= -revealPx / 2f  -> -revealPx
                                        currentOff >= revealPx / 2f   ->  revealPx
                                        else                           ->  0f
                                    }
                                    if (abs(target) == widthPx && widthPx > 0f) {
                                        offsetX.animateTo(target, bounceSpring); onDelete()
                                    } else { offsetX.animateTo(target, bounceSpring) }
                                }
                            }
                        ) { change, dragAmount ->
                            change.consume()
                            scope.launch { offsetX.snapTo((offsetX.value + dragAmount).coerceIn(-maxRevealPx, maxRevealPx)) }
                        }
                    }
            ) { content(isSwiping) }
        }
    }
}

@Composable
fun EntryRow(
    monthLabel: String, dayNumber: String, startDate: String, endDate: String,
    bleeding: String, bloodColor: String, crampsPain: Int,
    surface: Color, soft: Color, text: Color, sub: Color, accent: Color,
    isSwiping: Boolean = false, customDayCount: Int = 0, onEditClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "arrowRotation")

    val isDark             = LocalAppIsDark.current
    val cardBackground     = if (isDark) Color(0xFF1B1B1B).copy(alpha = 0.5f) else Color.White
    val pillBackground     = if (isDark) Color(0xFFE8EBED).copy(alpha = 0.1f) else Color(0xFFE8EBED).copy(alpha = 0.4f)
    val primaryTextColor   = if (isDark) Color.White else Color(0xFF1B1B1B)
    val secondaryTextColor = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF1b1b1b).copy(alpha = 0.6f)
    val pillTextColor      = if (isDark) Color.White else Color(0xFF1B1B1B)

    val capitalizedBleeding = remember(bleeding) {
        bleeding.lowercase(Locale.getDefault()).replaceFirstChar { it.titlecase(Locale.getDefault()) }
    }
    fun shortPretty(d: String): String = runCatching {
        if (d.isBlank()) return@runCatching "?"
        val date = LocalDate.parse(d)
        "${date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${date.dayOfMonth}"
    }.getOrElse { d }

    val startDt = remember(startDate) { runCatching { LocalDate.parse(startDate) }.getOrNull() }
    val endDt   = remember(endDate)   { runCatching { if (endDate.isNotBlank()) LocalDate.parse(endDate) else null }.getOrNull() }
    val today   = LocalDate.now()

    val statusText = remember(startDt, endDt) {
        if (startDt != null) {
            if (endDt == null) "Day ${ChronoUnit.DAYS.between(startDt, today) + 1} • Ongoing"
            else "${ChronoUnit.DAYS.between(startDt, endDt) + 1} Days • Completed"
        } else "Status unknown"
    }

    Card(
        colors    = CardDefaults.cardColors(containerColor = cardBackground),
        shape     = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier  = Modifier.fillMaxWidth().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = { expanded = !expanded })
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(pillBackground), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.History, null, tint = primaryTextColor, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = "$monthLabel $dayNumber",
                        fontFamily = BricolageGrotesque,
                        color      = primaryTextColor,
                        fontSize   = SIZE_LG,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text       = statusText,
                        fontFamily = BricolageGrotesque,
                        color      = secondaryTextColor,
                        fontSize   = SIZE_SM,
                        fontWeight = FontWeight.Normal
                    )
                }
                Icon(Icons.Default.KeyboardArrowDown, null, tint = primaryTextColor.copy(alpha = 0.6f), modifier = Modifier.size(24.dp).graphicsLayer { rotationZ = rotation })
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(Modifier.height(18.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(10.dp)) {
                            InfoBox("Duration", "${shortPretty(startDate)} - ${if (endDate.isNotBlank()) shortPretty(endDate) else "Ongoing"}", pillBackground, pillTextColor)
                            InfoBox(if (customDayCount > 0) "Avg Pain" else "Pain", "$crampsPain/10", pillBackground, pillTextColor)
                        }
                        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(10.dp)) {
                            InfoBox(if (customDayCount > 0) "Peak Flow" else "Flow", capitalizedBleeding, pillBackground, pillTextColor)
                            InfoBox(if (customDayCount > 0) "Main Color" else "Color", bloodColor, pillBackground, pillTextColor)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(
                        onClick  = onEditClick,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape    = RoundedCornerShape(18.dp),
                        border   = BorderStroke(1.dp, secondaryTextColor.copy(alpha = 0.3f)),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = primaryTextColor)
                    ) {
                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Edit Entry",
                            fontFamily = BricolageGrotesque,
                            fontSize   = SIZE_LG,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RowScope.InfoBox(label: String, value: String, bg: Color, textColor: Color) {
    Column(
        modifier = Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).background(bg).padding(vertical = 14.dp, horizontal = 16.dp)
    ) {
        Text(
            text       = label,
            fontFamily = BricolageGrotesque,
            color      = textColor.copy(alpha = 0.7f),
            fontSize   = SIZE_XXS,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text       = value,
            fontFamily = BricolageGrotesque,
            color      = textColor,
            fontSize   = SIZE_SM,
            fontWeight = FontWeight.Normal
        )
    }
}