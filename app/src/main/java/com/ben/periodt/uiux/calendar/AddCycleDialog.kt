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
import androidx.compose.material.icons.filled.Check
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

    val isDark = isSystemInDarkTheme()

    val contentSurface = if (isDark) Color(0xFF1B1B1B) else Color.White
    val pillBackground = if (isDark) Color(0xFFE8EBED).copy(alpha = 0.1f) else Color(0xFFE8EBED).copy(alpha = 0.4f)
    val pillTextColor = if (isDark) Color.White else Color(0xFF2C3F70)
    val accentColor = if (isDark) Color(0xFF8089D2) else Color(0xFF1B1B1B)

    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSub = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF64748B)

    val dialogBrush = if (isDark) {
        Brush.linearGradient(listOf(Color(0xFFC8D4E5), Color(0xFF8089D2)))
    } else {
        Brush.linearGradient(listOf(Color(0xFF2C3F70), Color(0xFF2C3F70)))
    }

    val formatter = remember { DateTimeFormatter.ofPattern("MMM dd") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(26.dp))
                    .background(dialogBrush)
                    .padding(0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(contentSurface)
                ) {
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
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            CleanDateCard(
                                label = "Started",
                                date = startDate.format(formatter),
                                icon = Icons.Rounded.CalendarToday,
                                bg = pillBackground,
                                textColor = pillTextColor,
                                onClick = { showStartPicker = true },
                                modifier = Modifier.weight(1f)
                            )
                            CleanDateCard(
                                label = "Ended",
                                date = endDate?.format(formatter) ?: "Ongoing",
                                icon = if (endDate == null) Icons.Rounded.Update else Icons.Rounded.EventAvailable,
                                bg = pillBackground,
                                textColor = pillTextColor,
                                onClick = { showEndPicker = true },
                                modifier = Modifier.weight(1f)
                            )
                        }

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
                                    EntryStylePill(
                                        text = option,
                                        isSelected = bleeding.equals(option, ignoreCase = true),
                                        activeBg = pillBackground,
                                        activeText = pillTextColor,
                                        inactiveText = textSub,
                                        surface = contentSurface,
                                        onClick = { bleeding = option }
                                    )
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "Color",
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
                                    EntryStylePill(
                                        text = option,
                                        isSelected = bloodColor.equals(option, ignoreCase = true),
                                        activeBg = pillBackground,
                                        activeText = pillTextColor,
                                        inactiveText = textSub,
                                        surface = contentSurface,
                                        onClick = { bloodColor = option }
                                    )
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Pain Level",
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
                                    inactiveTrackColor = pillBackground.copy(alpha = 0.3f)
                                )
                            )
                        }

                        Button(
                            onClick = { onSave(startDate, endDate, bleeding, bloodColor, painLevel) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(50),
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
    }

    if (showStartPicker) {
        MinimalDatePickerDialog(
            title = "Start Date",
            brand = accentColor, gradTop = Color.Gray, gradMid = Color.Gray, gradBottom = Color.Gray,
            onGradient = Color.White, buttonContainer = contentSurface, buttonContent = textPrimary,
            onDismiss = { showStartPicker = false },
            onConfirm = { ms -> millisToLocalDate(ms)?.let { startDate = it }; showStartPicker = false }
        )
    }
    if (showEndPicker) {
        MinimalDatePickerDialog(
            title = "End Date",
            brand = accentColor, gradTop = Color.Gray, gradMid = Color.Gray, gradBottom = Color.Gray,
            onGradient = Color.White, buttonContainer = contentSurface, buttonContent = textPrimary,
            onDismiss = { showEndPicker = false },
            onConfirm = { ms -> millisToLocalDate(ms)?.let { endDate = it }; showEndPicker = false }
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

    val dialogBrush = if (isDark) {
        Brush.linearGradient(listOf(Color(0xFFC8D4E5), Color(0xFF8089D2)))
    } else {
        Brush.linearGradient(listOf(Color(0xFF2C3F70), Color(0xFF2C3F70)))
    }

    val contentSurface = if (isDark) Color(0xFF1B1B1B) else Color.White
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val accentColor = if (isDark) Color(0xFF8089D2) else Color(0xFF1B1B1B)
    val buttonText = Color.White

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = true)
    ) {
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(26.dp))
                    .background(dialogBrush)
                    .padding(0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(contentSurface)
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            fontFamily = BricolageGrotesque,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
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

                    Spacer(Modifier.height(20.dp))

                    Button(
                        onClick = { onConfirm(selectedMillis) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = buttonText
                        ),
                        shape = RoundedCornerShape(50),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Text(
                            text = "OK",
                            fontFamily = BricolageGrotesque,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
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
    val selectedChipText = if (isDark) Color(0xFF1B1B1B) else Color.White

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
