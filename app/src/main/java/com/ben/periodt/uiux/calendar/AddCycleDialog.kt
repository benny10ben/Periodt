package com.ben.periodt.uiux.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ben.periodt.ui.theme.BricolageGrotesque
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.ceil

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddCycleDialog(
    onDismiss: () -> Unit,
    onSave: (LocalDate, LocalDate?, String, String, Int) -> Unit
) {
    // --- STATE MANAGEMENT ---
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var endDate by remember { mutableStateOf<LocalDate?>(null) }
    var bleeding by remember { mutableStateOf("Medium") }
    var bloodColor by remember { mutableStateOf("Bright Red") }

    var sliderPosition by remember { mutableStateOf(5f) }
    var painLevel by remember { mutableIntStateOf(5) }

    val bleedingOptions = listOf("Heavy", "Medium", "Light", "Spotting")
    val colorOptions = listOf("Bright Red", "Dark Red", "Brown")

    // --- THEME & COLORS ---
    val isDark = isSystemInDarkTheme()

    // Dark Mode background: Pure Black to Deep Gray
    val contentSurface = if (isDark) {
        Brush.linearGradient(
            0.0f to Color.Black,
            1.0f to Color(0xFF1B1B1B)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color(0xFFF8FAFC), Color(0xFFf2f0e3))
        )
    }

    // Yellow accent for Slider and Buttons in Dark Mode
    val accentColor = if (isDark) Color(0xFFD89046) else Color(0xFF6d9567).copy(alpha = 0.6f)
    val surfaceFallback = if (isDark) Color.Black else Color.White

    // Synchronized Pastel Palette
    val pastelGreen = Color(0xFF6d9567).copy(alpha = 0.6f)
    val pastelOrange = Color(0xFFD89046)
    val pastelMaroon = Color(0xFF4E1A1A)

    val pillBackground = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f)
    val pillTextColor = if (isDark) Color.White else Color(0xFF1B1B1B)

    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSub = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF64748B)

    val formatter = remember { DateTimeFormatter.ofPattern("MMM dd") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = surfaceFallback),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(contentSurface)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, start = 24.dp, end = 24.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Log Cycle",
                        fontFamily = BricolageGrotesque,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = textPrimary
                    )
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(textSub.copy(alpha = 0.1f))
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = textPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Date Selectors
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CleanDateCard(
                            label = "Start Date",
                            date = startDate.format(formatter),
                            icon = Icons.Rounded.CalendarToday,
                            bg = pillBackground,
                            textColor = pillTextColor,
                            onClick = { showStartPicker = true },
                            modifier = Modifier.weight(1f)
                        )
                        CleanDateCard(
                            label = "End Date",
                            date = endDate?.format(formatter) ?: "Ongoing",
                            icon = if (endDate == null) Icons.Rounded.Update else Icons.Rounded.EventAvailable,
                            bg = pillBackground,
                            textColor = pillTextColor,
                            onClick = { showEndPicker = true },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Flow Intensity
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Flow Intensity",
                            fontFamily = BricolageGrotesque,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = textPrimary
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            bleedingOptions.forEach { option ->
                                val isSelected = bleeding.equals(option, ignoreCase = true)
                                val activePillColor = when(option) {
                                    "Heavy" -> pastelMaroon
                                    "Medium" -> pastelOrange
                                    else -> pastelGreen
                                }

                                EntryStylePill(
                                    text = option,
                                    isSelected = isSelected,
                                    activeBg = activePillColor,
                                    activeText = Color.White,
                                    inactiveText = textSub,
                                    surface = surfaceFallback,
                                    onClick = { bleeding = option }
                                )
                            }
                        }
                    }

                    // Blood Color
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Blood Color",
                            fontFamily = BricolageGrotesque,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = textPrimary
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            colorOptions.forEach { option ->
                                val isSelected = bloodColor.equals(option, ignoreCase = true)

                                val activePillColor = when(option) {
                                    "Bright Red" -> pastelGreen
                                    "Dark Red"   -> Color(0xFF4E1A1A)
                                    "Brown"      -> pastelOrange
                                    else         -> accentColor
                                }

                                EntryStylePill(
                                    text = option,
                                    isSelected = isSelected,
                                    activeBg = activePillColor,
                                    activeText = Color.White,
                                    inactiveText = textSub,
                                    surface = surfaceFallback,
                                    onClick = { bloodColor = option }
                                )
                            }
                        }
                    }

                    // Pain Level Slider
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Cramps & Pain",
                                fontFamily = BricolageGrotesque,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = textPrimary
                            )
                            Text(
                                text = "${sliderPosition.toInt()} / 10",
                                fontFamily = BricolageGrotesque,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                        }
                        Slider(
                            value = sliderPosition,
                            onValueChange = {
                                sliderPosition = it
                                painLevel = it.toInt()
                            },
                            valueRange = 0f..10f,
                            colors = SliderDefaults.colors(
                                thumbColor = accentColor,
                                activeTrackColor = accentColor,
                                inactiveTrackColor = pillBackground
                            )
                        )
                    }

                    // Action Button
                    Button(
                        onClick = {
                            onSave(startDate, endDate, bleeding, bloodColor, painLevel)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(bottom = 8.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Text(
                            text = "Save Entry",
                            fontFamily = BricolageGrotesque,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // --- DATE PICKERS ---
    if (showStartPicker) {
        MinimalDatePickerDialog(
            title = "Start Date",
            brand = accentColor,
            gradTop = pastelGreen, gradMid = pastelOrange, gradBottom = pastelMaroon,
            onGradient = Color.White,
            buttonContainer = surfaceFallback,
            buttonContent = textPrimary,
            onDismiss = { showStartPicker = false },
            onConfirm = { ms ->
                millisToLocalDate(ms)?.let { startDate = it }
                showStartPicker = false
            }
        )
    }
    if (showEndPicker) {
        MinimalDatePickerDialog(
            title = "End Date",
            brand = accentColor,
            gradTop = pastelGreen, gradMid = pastelOrange, gradBottom = pastelMaroon,
            onGradient = Color.White,
            buttonContainer = surfaceFallback,
            buttonContent = textPrimary,
            onDismiss = { showEndPicker = false },
            onConfirm = { ms ->
                millisToLocalDate(ms)?.let { endDate = it }
                showEndPicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MinimalDatePickerDialog(
    title: String,
    brand: Color,
    gradTop: Color,
    gradMid: Color,
    gradBottom: Color,
    onGradient: Color,
    buttonContainer: Color,
    buttonContent: Color,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val zone = remember { ZoneId.systemDefault() }
    var selectedMillis by remember {
        mutableStateOf(Instant.now().atZone(zone).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli())
    }
    var displayedYm by remember {
        mutableStateOf(
            Instant.ofEpochMilli(selectedMillis).atZone(zone).toLocalDate().let { d ->
                YearMonth.of(d.year, d.month)
            }
        )
    }

    // --- UPDATED BACKGROUND & COLORS ---
    val contentSurface = if (isDark) {
        Brush.linearGradient(
            0.0f to Color.Black,
            1.0f to Color(0xFF1B1B1B)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color(0xFFF8FAFC), Color(0xFFf2f0e3))
        )
    }

    val surfaceFallback = if (isDark) Color.Black else Color.White
    val textPrimary = if (isDark) Color.White else Color(0xFF1B1B1B)

    // UPDATED: Yellow accent for dark mode
    val accentColor = if (isDark) Color(0xFFD89046) else Color(0xFF6d9567).copy(alpha = 0.6f)
    val buttonText = Color.White // Forced White only

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = true)
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = surfaceFallback),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(contentSurface)
                    .padding(horizontal = 16.dp, vertical = 24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontFamily = BricolageGrotesque,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        ),
                        color = textPrimary,
                        textAlign = TextAlign.Center
                    )
                }

                MinimalMonthPicker(
                    displayedYm = displayedYm,
                    selectedMillis = selectedMillis,
                    onDisplayedYmChange = { displayedYm = it },
                    onSelect = { ms -> selectedMillis = ms },
                    textColor = textPrimary,
                    accentColor = accentColor,
                    weekStartsOnMonday = true
                )

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = { onConfirm(selectedMillis) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = buttonText
                    ),
                    shape = RoundedCornerShape(50),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Text(
                        text = "Confirm Date",
                        fontFamily = BricolageGrotesque,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun MinimalMonthPicker(
    displayedYm: YearMonth,
    selectedMillis: Long?,
    onDisplayedYmChange: (YearMonth) -> Unit,
    onSelect: (Long) -> Unit,
    textColor: Color,
    accentColor: Color,
    weekStartsOnMonday: Boolean
) {
    val isDark = isSystemInDarkTheme()
    val zone = remember { ZoneId.systemDefault() }
    val today = remember { LocalDate.now(zone) }
    val selectedDate = selectedMillis?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }

    fun weekdayIndex(d: LocalDate): Int {
        val iso = d.dayOfWeek.value
        return if (weekStartsOnMonday) iso - 1 else (iso % 7)
    }

    val firstOfMonth = displayedYm.atDay(1)
    val daysInMonth = displayedYm.lengthOfMonth()
    val leadingBlanks = weekdayIndex(firstOfMonth)
    val totalCells = leadingBlanks + daysInMonth
    val rows = ceil(totalCells / 7f).toInt()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${displayedYm.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${displayedYm.year}",
            fontFamily = BricolageGrotesque,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = textColor
        )

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = { onDisplayedYmChange(displayedYm.minusMonths(1)) }, modifier = Modifier.size(32.dp)) {
                Text(
                    text = "‹",
                    fontFamily = BricolageGrotesque,
                    color = textColor.copy(alpha = 0.7f),
                    fontSize = 24.sp
                )
            }
            IconButton(onClick = { onDisplayedYmChange(displayedYm.plusMonths(1)) }, modifier = Modifier.size(32.dp)) {
                Text(
                    text = "›",
                    fontFamily = BricolageGrotesque,
                    color = textColor.copy(alpha = 0.7f),
                    fontSize = 24.sp
                )
            }
        }
    }

    Spacer(Modifier.height(12.dp))

    val labels = if (weekStartsOnMonday)
        listOf("MON","TUE","WED","THU","FRI","SAT","SUN")
    else
        listOf("SUN","MON","TUE","WED","THU","FRI","SAT")

    Row(Modifier.fillMaxWidth()) {
        labels.forEach {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    text = it,
                    fontFamily = BricolageGrotesque,
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    Spacer(Modifier.height(8.dp))

    val todayChipBg = textColor.copy(alpha = 0.1f)
    val selectedChipBg = accentColor
    // UPDATED: In dark mode, text on the yellow chip is Black for contrast
    val selectedChipText = if (isDark) Color.Black else Color.White

    var day = 1
    repeat(rows) { r ->
        Row(Modifier.fillMaxWidth()) {
            repeat(7) { c ->
                val idx = r * 7 + c
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f),
                    contentAlignment = Alignment.Center
                ) {
                    if (idx >= leadingBlanks && day <= daysInMonth) {
                        val date = displayedYm.atDay(day)
                        val isToday = date == today
                        val isSelected = date == selectedDate

                        val dayTextColor = when {
                            isSelected -> selectedChipText
                            isToday -> textColor
                            else -> textColor
                        }

                        val click = {
                            val ms = date.atStartOfDay(zone).toInstant().toEpochMilli()
                            onSelect(ms)
                        }

                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(selectedChipBg)
                                    .clickable(onClick = click),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$day",
                                    fontFamily = BricolageGrotesque,
                                    color = dayTextColor,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else if (isToday) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(todayChipBg)
                                    .clickable(onClick = click),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$day",
                                    fontFamily = BricolageGrotesque,
                                    color = dayTextColor,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .clickable(onClick = click),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$day",
                                    fontFamily = BricolageGrotesque,
                                    color = dayTextColor,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        day++
                    }
                }
            }
        }
        Spacer(Modifier.height(2.dp))
    }
}

/* ---------- Utils ---------- */

fun millisToLocalDate(millis: Long?): LocalDate? =
    millis?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }
