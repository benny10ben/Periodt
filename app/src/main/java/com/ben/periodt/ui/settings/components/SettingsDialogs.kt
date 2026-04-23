package com.ben.periodt.ui.settings.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ben.periodt.ui.theme.BricolageGrotesque
import com.ben.periodt.ui.theme.LocalAppIsDark
import kotlinx.coroutines.launch

private val SIZE_MD = 14.sp
private val SIZE_LG = 15.sp

enum class ThemeMode { SYSTEM, LIGHT, DARK }
val THEME_MODE_KEY = stringPreferencesKey("theme_mode")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentDialog(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val isDark = LocalAppIsDark.current
    val containerColor = if (isDark) Color(0xFF1B1B1B) else Color.White
    val textPrimary    = if (isDark) Color.White else Color(0xFF1B1B1B)
    val textSub        = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF1b1b1b).copy(alpha = 0.6f)

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val thresholdPx = remember(configuration.screenHeightDp) {
        with(density) { (configuration.screenHeightDp.dp * 0.20f).toPx() }
    }

    var expandedOffset by remember { mutableFloatStateOf(0f) }

    class SheetStateHolder { var state: SheetState? = null }
    val sheetHolder = remember { SheetStateHolder() }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { targetValue ->
            if (targetValue == SheetValue.Hidden) {
                try {
                    val currentState = sheetHolder.state
                    if (currentState != null) {
                        val currentOffset = currentState.requireOffset()
                        val dragDistance = currentOffset - expandedOffset
                        dragDistance <= 10f || dragDistance >= thresholdPx
                    } else {
                        true
                    }
                } catch (e: Exception) {
                    true
                }
            } else {
                true
            }
        }
    )

    sheetHolder.state = sheetState

    LaunchedEffect(sheetState.currentValue) {
        if (sheetState.currentValue == SheetValue.Expanded) {
            try {
                expandedOffset = sheetState.requireOffset()
            } catch (e: Exception) {}
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = containerColor,
        sheetState = sheetState,
        scrimColor = Color.Black.copy(alpha = 0.32f),
        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
        dragHandle = { BottomSheetDefaults.DragHandle(color = textSub.copy(alpha = 0.2f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp)
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness    = Spring.StiffnessMediumLow
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    fontFamily = BricolageGrotesque,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(textSub.copy(alpha = 0.1f))
                        .clickable {
                            coroutineScope.launch {
                                sheetState.hide()
                                onDismiss()
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Close, null, tint = textPrimary, modifier = Modifier.size(18.dp))
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {
                content()
            }
        }
    }
}

@Composable
fun FaqItem(question: String, answer: String, primary: Color, sub: Color) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(question, fontFamily = BricolageGrotesque, fontWeight = FontWeight.Bold, color = primary, fontSize = SIZE_LG)
        Spacer(Modifier.height(4.dp))
        Text(answer, fontFamily = BricolageGrotesque, color = sub, fontSize = SIZE_MD, lineHeight = 20.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceDialog(current: ThemeMode, onSelect: (ThemeMode) -> Unit, onDismiss: () -> Unit) {
    val isDark         = LocalAppIsDark.current
    val containerColor = if (isDark) Color(0xFF1B1B1B) else Color.White
    val textPrimary    = if (isDark) Color.White else Color(0xFF1B1B1B)
    val textSub        = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF1b1b1b).copy(alpha = 0.6f)
    val accentColor    = if (isDark) Color(0xFFD89046) else Color(0xFFa5bda3)
    val rowBg          = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f)

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val thresholdPx = remember(configuration.screenHeightDp) {
        with(density) { (configuration.screenHeightDp.dp * 0.20f).toPx() }
    }

    var expandedOffset by remember { mutableFloatStateOf(0f) }

    class SheetStateHolder { var state: SheetState? = null }
    val sheetHolder = remember { SheetStateHolder() }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { targetValue ->
            if (targetValue == SheetValue.Hidden) {
                try {
                    val currentState = sheetHolder.state
                    if (currentState != null) {
                        val currentOffset = currentState.requireOffset()
                        val dragDistance = currentOffset - expandedOffset
                        dragDistance <= 10f || dragDistance >= thresholdPx
                    } else {
                        true
                    }
                } catch (e: Exception) {
                    true
                }
            } else {
                true
            }
        }
    )

    sheetHolder.state = sheetState

    LaunchedEffect(sheetState.currentValue) {
        if (sheetState.currentValue == SheetValue.Expanded) {
            try {
                expandedOffset = sheetState.requireOffset()
            } catch (e: Exception) {}
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = containerColor,
        sheetState = sheetState,
        scrimColor = Color.Black.copy(alpha = 0.32f),
        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
        dragHandle = { BottomSheetDefaults.DragHandle(color = textSub.copy(alpha = 0.2f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp)
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness    = Spring.StiffnessMediumLow
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Appearance",
                    fontFamily = BricolageGrotesque,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(textSub.copy(alpha = 0.1f))
                        .clickable {
                            coroutineScope.launch {
                                sheetState.hide()
                                onDismiss()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, null, tint = textPrimary, modifier = Modifier.size(18.dp))
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ThemeOptionRow("System Default", current == ThemeMode.SYSTEM, textPrimary, rowBg, accentColor) {
                    coroutineScope.launch {
                        sheetState.hide()
                        onSelect(ThemeMode.SYSTEM)
                    }
                }
                ThemeOptionRow("Light Mode",     current == ThemeMode.LIGHT,   textPrimary, rowBg, accentColor) {
                    coroutineScope.launch {
                        sheetState.hide()
                        onSelect(ThemeMode.LIGHT)
                    }
                }
                ThemeOptionRow("Dark Mode",      current == ThemeMode.DARK,    textPrimary, rowBg, accentColor) {
                    coroutineScope.launch {
                        sheetState.hide()
                        onSelect(ThemeMode.DARK)
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeOptionRow(
    label: String,
    isSelected: Boolean,
    textPrimary: Color,
    rowBg: Color,
    accentColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (isSelected) accentColor.copy(alpha = 0.15f) else rowBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontFamily = BricolageGrotesque,
            color = if (isSelected) accentColor else textPrimary,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = SIZE_LG
        )
        Icon(
            imageVector = if (isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (isSelected) accentColor else textPrimary.copy(alpha = 0.2f),
            modifier = Modifier.size(22.dp)
        )
    }
}