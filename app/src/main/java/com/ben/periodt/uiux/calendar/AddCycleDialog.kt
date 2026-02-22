package com.ben.periodt.uiux.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.ceil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCycleDialog(
    onDismiss: () -> Unit,
    onSave: (LocalDate, LocalDate?, String, String, Int) -> Unit
) {
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var endDate by remember { mutableStateOf<LocalDate?>(null) }
    var bleeding by remember { mutableStateOf("Medium") }
    var bloodColor by remember { mutableStateOf("Bright Red") }
    var painLevel by remember { mutableIntStateOf(5) }

    val bleedingOptions = listOf("Heavy", "Medium", "Light", "Spotting")
    val colorOptions = listOf("Bright Red", "Dark Red", "Brown")

    val isDark = isSystemInDarkTheme()
    val gradTop = if (isDark) Color(0xFF7B8FA3) else Color(0xFF8FA0B1)
    val gradMid = if (isDark) Color(0xFF7288A0) else Color(0xFF8799B0)
    val gradBottom = if (isDark) Color(0xFF5A7396) else Color(0xFF6E87A7)
    val onGradient = Color.White
    val contentBg = if (isDark) Color.Black else Color.White
    val textPrimary = if (isDark) Color(0xFFF5F7FA) else Color(0xFF0F172A)
    val textSub = if (isDark) Color(0xFFBFC6D1) else Color(0xFF64748B)

    val scroll = rememberScrollState()
    val formatter = remember { DateTimeFormatter.ofPattern("MMM dd, yyyy") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = true)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.verticalGradient(listOf(gradTop, gradMid, gradBottom)))
                    .background(Color.White.copy(alpha = 0.06f))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {

                    // --- CENTERED TITLE ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 18.dp, start = 22.dp, end = 22.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Add Cycle",
                            color = onGradient,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            ),
                            textAlign = TextAlign.Center
                        )
                    }

                    // Content Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(contentBg)
                            .padding(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(scroll),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // --- DATES SECTION ---
                            Column {
                                Text("Dates", color = textPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Start", color = textSub, fontSize = 12.sp)
                                        Spacer(Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(textPrimary.copy(alpha = 0.05f))
                                                .clickable { showStartPicker = true }
                                                .padding(12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(startDate.format(formatter), color = textPrimary, fontSize = 13.sp)
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("End", color = textSub, fontSize = 12.sp)
                                        Spacer(Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(textPrimary.copy(alpha = 0.05f))
                                                .clickable { showEndPicker = true }
                                                .padding(12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(endDate?.format(formatter) ?: "Ongoing", color = textPrimary, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }

                            // --- BLEEDING INTENSITY (Segmented Style) ---
                            Text("Bleeding", color = textPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                bleedingOptions.forEach { option ->
                                    val isSelected = bleeding.equals(option, ignoreCase = true)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) gradBottom else textPrimary.copy(alpha = 0.05f))
                                            .clickable { bleeding = option }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = option,
                                            color = if (isSelected) onGradient else textPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            // --- BLOOD COLOR (Segmented Style) ---
                            Text("Blood Color", color = textPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                colorOptions.forEach { option ->
                                    val isSelected = bloodColor.equals(option, ignoreCase = true)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) gradBottom else textPrimary.copy(alpha = 0.05f))
                                            .clickable { bloodColor = option }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = option,
                                            color = if (isSelected) onGradient else textPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }

                            // --- PAIN LEVEL ---
                            Text("Cramps / Pain", color = textPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("Level: $painLevel / 10", color = textPrimary, fontSize = 14.sp)
                            Slider(
                                value = painLevel.toFloat(),
                                onValueChange = { painLevel = it.toInt() },
                                valueRange = 0f..10f,
                                steps = 9,
                                colors = SliderDefaults.colors(
                                    thumbColor = gradBottom,
                                    activeTrackColor = gradBottom,
                                    inactiveTrackColor = textPrimary.copy(alpha = 0.1f)
                                )
                            )
                        }
                    }

                    // --- BOTTOM ACTIONS ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .clip(RoundedCornerShape(26.dp))
                                .background(Color.White.copy(alpha = 0.15f)),
                            colors = ButtonDefaults.textButtonColors(contentColor = onGradient)
                        ) {
                            Text("Close", fontWeight = FontWeight.Medium)
                        }

                        Button(
                            onClick = { onSave(startDate, endDate, bleeding, bloodColor, painLevel) },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = contentBg,
                                contentColor = if (isDark) Color.White else Color.Black
                            ),
                            shape = RoundedCornerShape(26.dp)
                        ) {
                            Text("Save", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Reuse the custom pickers
    if (showStartPicker) {
        MinimalDatePickerDialog(
            title = "Pick Start Date",
            brand = MaterialTheme.colorScheme.primary,
            gradTop = gradTop, gradMid = gradMid, gradBottom = gradBottom,
            onGradient = onGradient,
            buttonContainer = if (isDark) Color.Black else Color.White,
            buttonContent = if (isDark) Color.White else Color.Black,
            onDismiss = { showStartPicker = false },
            onConfirm = { ms ->
                millisToLocalDate(ms)?.let { startDate = it }
                showStartPicker = false
            }
        )
    }
    if (showEndPicker) {
        MinimalDatePickerDialog(
            title = "Pick End Date",
            brand = MaterialTheme.colorScheme.primary,
            gradTop = gradTop, gradMid = gradMid, gradBottom = gradBottom,
            onGradient = onGradient,
            buttonContainer = if (isDark) Color.Black else Color.White,
            buttonContent = if (isDark) Color.White else Color.Black,
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

    // CHANGED: usePlatformDefaultWidth set to true to match Edit/Add dialog widths
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = true)
    ) {
        // Removed the full-screen Box wrapper to allow the Dialog to center itself naturally
        val cardRadius = 24.dp

        Card(
            shape = RoundedCornerShape(cardRadius),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(cardRadius))
                    .background(
                        Brush.verticalGradient(
                            listOf(gradTop, gradMid, gradBottom)
                        )
                    )
                    .background(Color.White.copy(alpha = 0.06f))
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp), // Added small padding for balance
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            // CHANGED: FontSize set to 20.sp to match Edit Entry
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            ),
                            color = onGradient,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    MinimalMonthPicker(
                        displayedYm = displayedYm,
                        selectedMillis = selectedMillis,
                        onDisplayedYmChange = { displayedYm = it },
                        onSelect = { ms -> selectedMillis = ms },
                        brand = brand,
                        onGradient = onGradient,
                        weekStartsOnMonday = true
                    )

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = { onConfirm(selectedMillis) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp), // Bumbed height to 52.dp to match Save/Close buttons
                        colors = ButtonDefaults.buttonColors(
                            containerColor = buttonContainer,
                            contentColor = buttonContent
                        ),
                        shape = RoundedCornerShape(26.dp) // Updated to 26.dp for pill shape
                    ) {
                        Text("OK", fontWeight = FontWeight.Bold)
                    }

                    Spacer(Modifier.height(4.dp))
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
    brand: Color,
    onGradient: Color,
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
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = onGradient
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { onDisplayedYmChange(displayedYm.minusMonths(1)) }) {
                Text("‹", color = onGradient.copy(alpha = 0.7f))
            }
            TextButton(onClick = { onDisplayedYmChange(displayedYm.plusMonths(1)) }) {
                Text("›", color = onGradient.copy(alpha = 0.7f))
            }
        }
    }

    Spacer(Modifier.height(8.dp))

    val labels = if (weekStartsOnMonday)
        listOf("MON","TUE","WED","THU","FRI","SAT","SUN")
    else
        listOf("SUN","MON","TUE","WED","THU","FRI","SAT")

    Row(Modifier.fillMaxWidth()) {
        labels.forEach {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(it, style = MaterialTheme.typography.labelSmall, color = onGradient.copy(alpha = 0.6f))
            }
        }
    }

    Spacer(Modifier.height(6.dp))

    val todayChipBg = if (isDark) Color.White.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.25f)
    val selectedChipBg = if (isDark) Color(0xFF000000) else Color.White
    val selectedChipText = if (isDark) Color.White else Color.Black

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
                            else -> onGradient
                        }
                        val click = {
                            val ms = date.atStartOfDay(zone).toInstant().toEpochMilli()
                            onSelect(ms)
                        }

                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(selectedChipBg)
                                    .clickable(onClick = click),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("$day", color = dayTextColor, style = MaterialTheme.typography.bodySmall)
                            }
                        } else if (isToday) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(todayChipBg)
                                    .clickable(onClick = click),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("$day", color = onGradient, style = MaterialTheme.typography.bodySmall)
                            }
                        } else {
                            Text(
                                "$day",
                                color = onGradient,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable(onClick = click)
                                    .padding(6.dp)
                            )
                        }
                        day++
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

/* ---------- Utils ---------- */

fun millisToLocalDate(millis: Long?): LocalDate? =
    millis?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }
