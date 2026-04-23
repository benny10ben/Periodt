package com.ben.periodt.ui.profiles.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.ben.periodt.ui.theme.BricolageGrotesque
import com.ben.periodt.viewmodel.PeriodViewModel
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

private val SIZE_SM = 13.sp
private val SIZE_LG = 15.sp

@Composable
fun ProfileNameField(
    value: String,
    onValueChange: (String) -> Unit,
    isDark: Boolean,
    accentColor: Color,
    textPrimary: Color,
    textSub: Color
) {
    val isFocused = remember { mutableStateOf(false) }
    val fieldBg = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)

    val borderColor by animateColorAsState(
        targetValue = if (isFocused.value) accentColor.copy(alpha = 0.8f) else Color.Transparent,
        animationSpec = tween(200),
        label = "fieldBorder"
    )

    val labelFloat by animateFloatAsState(
        targetValue = if (isFocused.value || value.isNotEmpty()) 1f else 0f,
        animationSpec = tween(180),
        label = "labelFloat"
    )

    val labelSize = lerp(SIZE_LG.value, SIZE_SM.value, labelFloat)
    val labelColor by animateColorAsState(
        targetValue = if (isFocused.value) accentColor else textSub,
        animationSpec = tween(180),
        label = "labelColor"
    )

    CompositionLocalProvider(
        LocalTextSelectionColors provides TextSelectionColors(
            handleColor = accentColor,
            backgroundColor = accentColor.copy(alpha = 0.25f)
        )
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                fontFamily = BricolageGrotesque,
                fontSize = SIZE_LG,
                color = textPrimary
            ),
            cursorBrush = SolidColor(accentColor),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isFocused.value = it.isFocused },
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(fieldBg)
                        .border(1.5.dp, borderColor, RoundedCornerShape(18.dp))
                        .animateContentSize(animationSpec = tween(180))
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = "Profile Name",
                        fontFamily = BricolageGrotesque,
                        fontSize = labelSize.sp,
                        color = labelColor,
                        modifier = Modifier.graphicsLayer {
                            translationY = lerp(0f, -12f, labelFloat)
                        }
                    )
                    Box(modifier = Modifier.padding(top = if (labelFloat > 0.5f) 24.dp else 0.dp)) {
                        innerTextField()
                    }
                }
            }
        )
    }
}

@Composable
fun SwipeToDeleteCard(
    modifier: Modifier = Modifier,
    onDelete: () -> Unit,
    content: @Composable (Boolean) -> Unit
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val revealPx = with(density) { 80.dp.toPx() }
    val deletePx = with(density) { 150.dp.toPx() }
    var widthPx by remember { mutableStateOf(0f) }

    val itemShape = RoundedCornerShape(20.dp)

    val revealSpring = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
    val snapBackSpring = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
    val deleteExit = tween<Float>(durationMillis = 180, easing = FastOutLinearInEasing)

    val isRevealed by remember { derivedStateOf { offsetX.value.absoluteValue > revealPx * 0.55f } }
    val isSwiping by remember { derivedStateOf { offsetX.value.absoluteValue > 4f } }

    val bgAlpha by remember { derivedStateOf { (offsetX.value.absoluteValue / revealPx).coerceIn(0f, 0.85f) } }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { widthPx = it.width.toFloat() }
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(itemShape)
                .background(Color.Red.copy(alpha = bgAlpha))
                .padding(horizontal = 24.dp),
            contentAlignment = if (offsetX.value >= 0f) Alignment.CenterStart else Alignment.CenterEnd
        ) {
            AnimatedVisibility(
                visible = isRevealed,
                enter = fadeIn(tween(120)) + scaleIn(tween(120, easing = FastOutSlowInEasing)),
                exit = fadeOut(tween(80)) + scaleOut(tween(80))
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            scope.launch {
                                val target = if (offsetX.value >= 0f) widthPx else -widthPx
                                offsetX.animateTo(target, deleteExit)
                                onDelete()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(itemShape)
                .offset { IntOffset(offsetX.value.toInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                val off = offsetX.value
                                when {
                                    off <= -deletePx || off >= deletePx -> {
                                        val target = if (off < 0f) -widthPx else widthPx
                                        offsetX.animateTo(target, deleteExit)
                                        onDelete()
                                    }
                                    off <= -(revealPx * 0.5f) -> offsetX.animateTo(-revealPx, revealSpring)
                                    off >= (revealPx * 0.5f) -> offsetX.animateTo(revealPx, revealSpring)
                                    else -> offsetX.animateTo(0f, snapBackSpring)
                                }
                            }
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        scope.launch { offsetX.snapTo((offsetX.value + dragAmount).coerceIn(-widthPx, widthPx)) }
                    }
                }
        ) {
            content(isSwiping)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProfileRow(
    modifier: Modifier = Modifier,
    profile: PeriodViewModel.Profile,
    isSelected: Boolean,
    highlightColor: Color,
    rowBg: Color,
    isDark: Boolean,
    textPrimary: Color,
    onSwitch: (Int) -> Unit,
    onEdit: (PeriodViewModel.Profile) -> Unit
) {
    val sheetBg = if (isDark) Color(0xFF1B1B1B) else Color.White
    val targetBg = if (isSelected) lerp(sheetBg, highlightColor, 0.15f) else rowBg
    val animatedBg by animateColorAsState(targetValue = targetBg, animationSpec = tween(300), label = "rowBgAnim")

    Row(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(animatedBg)
            .combinedClickable(onClick = { onSwitch(profile.id) }, onLongClick = { onEdit(profile) })
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarDisplay(avatarString = profile.avatarColor, name = profile.name, modifier = Modifier.size(48.dp), fontSize = SIZE_LG)
        Spacer(Modifier.width(16.dp))
        Text(text = profile.name, modifier = Modifier.weight(1f), color = textPrimary, fontFamily = BricolageGrotesque, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, fontSize = SIZE_LG, letterSpacing = (-0.3).sp)
        AnimatedVisibility(visible = isSelected, enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) + fadeIn(), exit = scaleOut(tween(150)) + fadeOut()) {
            Icon(imageVector = Icons.Rounded.CheckCircle, contentDescription = null, tint = highlightColor, modifier = Modifier.size(22.dp))
        }
    }
}