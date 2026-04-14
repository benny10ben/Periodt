package com.ben.periodt.ui.pill

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Medication
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
import com.ben.periodt.ui.theme.BricolageGrotesque
import com.ben.periodt.ui.theme.LocalAppIsDark
import com.ben.periodt.ui.calendar.CleanDateCard
import com.ben.periodt.ui.calendar.EntryStylePill
import com.ben.periodt.ui.calendar.MinimalDatePickerDialog
import com.ben.periodt.ui.calendar.millisToLocalDate
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val SIZE_MD = 14.sp
private val SIZE_LG = 15.sp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PillTrackingSetupDialog(
    onDismiss: () -> Unit,
    onSave: (startDate: LocalDate, pillCount: Int) -> Unit
) {
    var startDate        by remember { mutableStateOf(LocalDate.now()) }
    var selectedPillCount by remember { mutableIntStateOf(21) }
    var showDatePicker   by remember { mutableStateOf(false) }

    val pillOptions = listOf(21, 24, 28)
    val formatter   = remember { DateTimeFormatter.ofPattern("MMM dd, yyyy") }

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    // ✨ Custom Swipe Threshold Logic
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
                    } else true
                } catch (e: Exception) { true }
            } else true
        }
    )
    sheetHolder.state = sheetState

    LaunchedEffect(sheetState.currentValue) {
        if (sheetState.currentValue == SheetValue.Expanded) {
            try { expandedOffset = sheetState.requireOffset() } catch (e: Exception) {}
        }
    }

    val isDark = LocalAppIsDark.current
    val accentColor     = Color(0xFFa68e74)
    val pastelGreen     = Color(0xFF6d9567).copy(alpha = 0.4f)
    val pastelOrange    = Color(0xFFa68e74)
    val pastelMaroon    = Color(0xFF4E1A1A)
    val containerColor  = if (isDark) Color(0xFF1B1B1B) else Color.White
    val textPrimary     = if (isDark) Color.White else Color(0xFF0F172A)
    val textSub         = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF1b1b1b).copy(alpha = 0.6f)
    val surfaceFallback = if (isDark) Color.Black else Color.Black.copy(alpha = 0.05f)
    val pillBackground  = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = containerColor,
        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp)
        ) {
            // FIXED HEADER
            Row(
                modifier              = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text       = "Log Pills",
                    fontFamily = BricolageGrotesque,
                    style      = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color      = textPrimary
                )
                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(textSub.copy(alpha = 0.1f))
                        .clickable {
                            coroutineScope.launch {
                                sheetState.hide()
                                onDismiss()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, "Close", tint = textPrimary, modifier = Modifier.size(18.dp))
                }
            }

            // SCROLLABLE CONTENT
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                CleanDateCard(
                    label    = "Pack Start Date",
                    date     = startDate.format(formatter),
                    icon     = Icons.Rounded.CalendarToday,
                    bg       = pillBackground,
                    textColor = textPrimary,
                    onClick  = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                )

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text       = "Active Pills per Pack",
                        fontFamily = BricolageGrotesque,
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = SIZE_MD,
                        color      = textPrimary
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement   = Arrangement.spacedBy(8.dp)
                    ) {
                        pillOptions.forEach { count ->
                            EntryStylePill(
                                text         = "$count Pills",
                                isSelected   = selectedPillCount == count,
                                activeBg     = accentColor,
                                activeText   = Color.White,
                                inactiveText = textSub,
                                surface      = surfaceFallback,
                                onClick      = { selectedPillCount = count }
                            )
                        }
                    }
                }
            }

            // FIXED FOOTER BUTTON
            Button(
                onClick = {
                    coroutineScope.launch {
                        sheetState.hide()
                        onSave(startDate, selectedPillCount)
                    }
                },
                modifier  = Modifier.fillMaxWidth().padding(top = 24.dp).height(56.dp),
                shape     = RoundedCornerShape(18.dp),
                colors    = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Icon(Icons.Rounded.Medication, null)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Start Pill Tracking",
                    fontFamily = BricolageGrotesque,
                    fontSize   = SIZE_LG,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    if (showDatePicker) {
        MinimalDatePickerDialog(
            title           = "Pack Start Date",
            brand           = accentColor,
            gradTop         = pastelGreen,
            gradMid         = pastelOrange,
            gradBottom      = pastelMaroon,
            onGradient      = Color.White,
            buttonContainer = surfaceFallback,
            buttonContent   = textPrimary,
            onDismiss       = { showDatePicker = false },
            onConfirm       = { ms ->
                millisToLocalDate(ms)?.let { startDate = it }
                showDatePicker = false
            }
        )
    }
}