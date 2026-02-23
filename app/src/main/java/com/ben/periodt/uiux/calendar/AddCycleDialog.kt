package com.ben.periodt.uiux.calendar

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.Healing
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddCycleDialog(
    onDismiss: () -> Unit,
    onSave: (LocalDate, LocalDate?, String, String, Int) -> Unit
) {
    // --- State ---
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var endDate by remember { mutableStateOf<LocalDate?>(null) }
    var bleeding by remember { mutableStateOf("Medium") }
    var bloodColor by remember { mutableStateOf("Bright Red") }
    var painLevel by remember { mutableIntStateOf(5) }

    // --- Resources ---
    val bleedingOptions = listOf("Heavy", "Medium", "Light", "Spotting")
    val colorOptions = listOf("Bright Red", "Dark Red", "Brown")

    // --- Aesthetic Palette (Matches CalendarScreen) ---
    val isDark = isSystemInDarkTheme()
    val gradTop = if (isDark) Color(0xFF7B8FA3) else Color(0xFF8FA0B1)
    val gradMid = if (isDark) Color(0xFF7288A0) else Color(0xFF8799B0)
    val gradBottom = if (isDark) Color(0xFF5A7396) else Color(0xFF6E87A7)

    val onGradient = Color.White
    // Matches the Calendar Card's content background
    val contentSurface = if (isDark) Color.Black else Color.White
    val textPrimary = if (isDark) Color(0xFFF5F7FA) else Color(0xFF0F172A)
    val textSub = if (isDark) Color(0xFFBFC6D1) else Color(0xFF64748B)

    val formatter = remember { DateTimeFormatter.ofPattern("MMM dd") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // --- MAIN CARD (Matches Calendar Card Style) ---
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(26.dp))
                    // 1. The Gradient Background
                    .background(Brush.verticalGradient(listOf(gradTop, gradMid, gradBottom)))
                    // 2. The Glass Overlay
                    .background(Color.White.copy(alpha = 0.08f))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // --- HEADER ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp, start = 24.dp, end = 24.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Log Cycle",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = onGradient
                        )
                        // Close Button (Glassy)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                                .clickable(onClick = onDismiss),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = onGradient,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // --- CONTENT AREA (White/Dark Surface) ---
                    // This mimics the 'EntryRow' look inside the dialog
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                            .background(contentSurface) // Matches Calendar List background
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {

                        // 1. Date Selectors
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            DateGlassCard(
                                label = "Started",
                                date = startDate.format(formatter),
                                icon = Icons.Rounded.CalendarToday,
                                textColor = textPrimary,
                                subColor = textSub,
                                onClick = { showStartPicker = true },
                                modifier = Modifier.weight(1f)
                            )
                            DateGlassCard(
                                label = "Ended",
                                date = endDate?.format(formatter) ?: "Ongoing",
                                icon = if (endDate == null) Icons.Rounded.Update else Icons.Rounded.EventAvailable,
                                textColor = if (endDate == null) gradBottom else textPrimary,
                                subColor = textSub,
                                onClick = { showEndPicker = true },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        HorizontalDivider(color = textSub.copy(alpha = 0.1f))

                        // 2. Bleeding (Pill Selectors)
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SectionLabel("Flow Intensity", Icons.Rounded.WaterDrop, textPrimary)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                bleedingOptions.forEach { option ->
                                    ModernSelectionPill(
                                        text = option,
                                        isSelected = bleeding.equals(option, ignoreCase = true),
                                        activeColor = gradBottom,
                                        textColor = textPrimary,
                                        onClick = { bleeding = option }
                                    )
                                }
                            }
                        }

                        // 3. Color (Pill Selectors)
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SectionLabel("Color", Icons.Rounded.Palette, textPrimary)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                colorOptions.forEach { option ->
                                    ModernSelectionPill(
                                        text = option,
                                        isSelected = bloodColor.equals(option, ignoreCase = true),
                                        activeColor = gradBottom,
                                        textColor = textPrimary,
                                        onClick = { bloodColor = option }
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = textSub.copy(alpha = 0.1f))

                        // 4. Pain Slider
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SectionLabel("Pain Level", Icons.Rounded.Healing, textPrimary)
                                Text(
                                    text = "$painLevel / 10",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = gradBottom
                                )
                            }
                            Slider(
                                value = painLevel.toFloat(),
                                onValueChange = { painLevel = it.toInt() },
                                valueRange = 0f..10f,
                                steps = 9,
                                colors = SliderDefaults.colors(
                                    thumbColor = gradBottom,
                                    activeTrackColor = gradBottom,
                                    inactiveTrackColor = textSub.copy(alpha = 0.2f)
                                )
                            )
                        }

                        Spacer(Modifier.height(4.dp))

                        // 5. Save Button (Gradient Style)
                        Button(
                            onClick = { onSave(startDate, endDate, bleeding, bloodColor, painLevel) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .shadow(8.dp, RoundedCornerShape(34.dp), ambientColor = gradBottom.copy(alpha = 0.5f), spotColor = gradBottom.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            contentPadding = PaddingValues(0.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            // Gradient Button Background
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Brush.horizontalGradient(listOf(gradTop, gradBottom))),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Save Entry",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Reuse Custom Date Pickers ---
    if (showStartPicker) {
        MinimalDatePickerDialog(
            title = "Start Date",
            brand = gradBottom, gradTop = gradTop, gradMid = gradMid, gradBottom = gradBottom,
            onGradient = Color.White,
            buttonContainer = contentSurface,
            buttonContent = textPrimary,
            onDismiss = { showStartPicker = false },
            onConfirm = { ms -> millisToLocalDate(ms)?.let { startDate = it }; showStartPicker = false }
        )
    }
    if (showEndPicker) {
        MinimalDatePickerDialog(
            title = "End Date",
            brand = gradBottom, gradTop = gradTop, gradMid = gradMid, gradBottom = gradBottom,
            onGradient = Color.White,
            buttonContainer = contentSurface,
            buttonContent = textPrimary,
            onDismiss = { showEndPicker = false },
            onConfirm = { ms -> millisToLocalDate(ms)?.let { endDate = it }; showEndPicker = false }
        )
    }
}

// --- HELPER COMPOSABLES ---

@Composable
fun SectionLabel(text: String, icon: ImageVector, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(imageVector = icon, contentDescription = null, tint = tint.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
        Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun DateGlassCard(
    label: String,
    date: String,
    icon: ImageVector,
    textColor: Color,
    subColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(subColor.copy(alpha = 0.08f)) // Subtle glass effect
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = subColor)
        Spacer(Modifier.height(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(date, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = textColor)
            Icon(imageVector = icon, contentDescription = null, tint = subColor.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun ModernSelectionPill(
    text: String,
    isSelected: Boolean,
    activeColor: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) activeColor else Color.Transparent
    val contentColor = if (isSelected) Color.White else textColor
    val borderColor = if (isSelected) Color.Transparent else textColor.copy(alpha = 0.2f)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = contentColor
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
