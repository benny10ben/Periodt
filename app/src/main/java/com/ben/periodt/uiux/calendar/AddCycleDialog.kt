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
    var endDate by remember { mutableStateOf(LocalDate.now()) }
    var bleeding by remember { mutableStateOf("Medium") }
    var bloodColor by remember { mutableStateOf("Bright Red") }
    var painLevel by remember { mutableIntStateOf(5) }

    val bleedingOptions = listOf("Heavy", "Medium", "Light", "Spotting")
    val colorOptions = listOf("Bright Red", "Dark Red", "Brown")

    var bleedingDropdownExpanded by remember { mutableStateOf(false) }
    var colorDropdownExpanded by remember { mutableStateOf(false) }

    val isDark = isSystemInDarkTheme()
    val cs = MaterialTheme.colorScheme

    // Gradient shell to match Settings dialog
    val gradTop = if (isDark) Color(0xFF7B8FA3) else Color(0xFF8FA0B1)
    val gradMid = if (isDark) Color(0xFF7288A0) else Color(0xFF8799B0)
    val gradBottom = if (isDark) Color(0xFF5A7396) else Color(0xFF6E87A7)
    val onGradient = Color.White

    // Themed text colors
    val textPrimary = if (isDark) Color(0xFFF5F7FA) else Color(0xFF0F172A)
    val textSub = if (isDark) Color(0xFFBFC6D1) else Color(0xFF64748B)

    // Buttons: pure black in dark theme, white in light theme
    val buttonContainer = if (isDark) Color(0xFF000000) else Color.White
    val buttonContent = if (isDark) Color(0xFFFFFFFF) else Color.Black

    // Unified container background and divider style
    val unifiedBg = if (isDark) Color.Black else Color.White
    val dividerColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.48f)

    val scroll = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
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
                            brush = Brush.verticalGradient(
                                colors = listOf(gradTop, gradMid, gradBottom)
                            )
                        )
                        .background(Color.White.copy(alpha = 0.06f))
                ) {
                    // Title (on gradient)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 18.dp, start = 22.dp, end = 22.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Add cycle",
                            color = onGradient,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                    }

                    // Scrollable content: single unified container with thin dividers
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                            .padding(top = 64.dp, bottom = 90.dp) // leave space for footer
                            .heightIn(max = 560.dp)
                            .verticalScroll(scroll),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))        // clip then background for visible corners
                                .background(unifiedBg)                   // unified white/black panel
                                .padding(horizontal = 20.dp, vertical = 14.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                                // Dates
                                Column {
                                    Text(
                                        text = "Dates",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = textPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 13.dp).padding(top = 8.dp),   // let it take full row width [web:166]
                                        textAlign = TextAlign.Start          // center the text within that width [web:158]
                                    )

                                    Spacer(Modifier.height(10.dp)) // small gap to first field [web:561]

                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { // 8dp between fields [web:559]
                                        PickerRow(
                                            label = "Start",
                                            value = startDate.toString(),
                                            onClick = { showStartPicker = true },
                                            textPrimary = textPrimary,
                                            textSub = textSub,
                                            surface = unifiedBg
                                        )
                                        PickerRow(
                                            label = "End",
                                            value = endDate.toString(),
                                            onClick = { showEndPicker = true },
                                            textPrimary = textPrimary,
                                            textSub = textSub,
                                            surface = unifiedBg
                                        )
                                    }
                                }

                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    thickness = 1.dp,
                                    color = dividerColor
                                )

                                // Cycle details
                                Column {
                                    Text(
                                        "Cycle details",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = textPrimary,
                                        fontWeight = FontWeight.SemiBold, // or FontWeight.Bold
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),   // let it take full row width [web:166]
                                        textAlign = TextAlign.Start
                                    )
                                    Spacer(Modifier.height(10.dp)) // small gap to first field [web:561]
                                    FieldSelect(
                                        label = "Bleeding",
                                        value = bleeding,
                                        expanded = bleedingDropdownExpanded,
                                        onExpandedChange = { bleedingDropdownExpanded = it },
                                        options = bleedingOptions,
                                        onSelect = {
                                            bleeding = it
                                            bleedingDropdownExpanded = false
                                        },
                                        textPrimary = textPrimary,
                                        textSub = textSub,
                                        surface = unifiedBg
                                    )
                                    FieldSelect(
                                        label = "Blood Color",
                                        value = bloodColor,
                                        expanded = colorDropdownExpanded,
                                        onExpandedChange = { colorDropdownExpanded = it },
                                        options = colorOptions,
                                        onSelect = {
                                            bloodColor = it
                                            colorDropdownExpanded = false
                                        },
                                        textPrimary = textPrimary,
                                        textSub = textSub,
                                        surface = unifiedBg
                                    )
                                }

                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    thickness = 1.dp,
                                    color = dividerColor
                                )

                                // Cramps pain
                                Column {
                                    Text(
                                        "Cramps pain",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = textPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),   // let it take full row width [web:166]
                                        textAlign = TextAlign.Start
                                    )
                                    Spacer(Modifier.height(10.dp)) // tighter label-to-text [web:561]
                                    Text(
                                        text = "Level: $painLevel / 10",
                                        color = textPrimary,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(horizontal = 19.dp)  // correct parameter
                                    )

                                    Spacer(Modifier.height(6.dp)) // small gap to slider [web:561]
                                    Slider(
                                        value = painLevel.toFloat(),
                                        onValueChange = { painLevel = it.toInt() },
                                        valueRange = 0f..10f,
                                        steps = 9,
                                        colors = SliderDefaults.colors(
                                            thumbColor = gradBottom,
                                            activeTrackColor = gradBottom,
                                            inactiveTrackColor = textSub.copy(alpha = 0.3f),
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Sticky footer Save button (separate from unified content)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color.Transparent)
                            .padding(16.dp)
                    ) {
                        Button(
                            onClick = { onSave(startDate, endDate, bleeding, bloodColor, painLevel) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = buttonContainer,
                                contentColor = buttonContent
                            ),
                            shape = RoundedCornerShape(26.dp)
                        ) { Text("Save") }
                    }
                }
            }
        }
    }

    // Date pickers with matching shell
    if (showStartPicker) {
        MinimalDatePickerDialog(
            title = "Pick start date",
            brand = cs.primary,
            gradTop = gradTop,
            gradMid = gradMid,
            gradBottom = gradBottom,
            onGradient = onGradient,
            buttonContainer = buttonContainer,
            buttonContent = buttonContent,
            onDismiss = { showStartPicker = false },
            onConfirm = { ms ->
                millisToLocalDate(ms)?.let { startDate = it }
                showStartPicker = false
            }
        )
    }
    if (showEndPicker) {
        MinimalDatePickerDialog(
            title = "Pick end date",
            brand = cs.primary,
            gradTop = gradTop,
            gradMid = gradMid,
            gradBottom = gradBottom,
            onGradient = onGradient,
            buttonContainer = buttonContainer,
            buttonContent = buttonContent,
            onDismiss = { showEndPicker = false },
            onConfirm = { ms ->
                millisToLocalDate(ms)?.let { endDate = it }
                showEndPicker = false
            }
        )
    }
}

/* ---------- Helper rows/fields (unchanged) ---------- */

@Composable
private fun PickerRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    textPrimary: Color,
    textSub: Color,
    surface: Color
) {
    val isDark = isSystemInDarkTheme()
    val bg = if (isDark) Color(0xFF000000) else Color.White
    val dotCol = textSub.copy(alpha = 0.6f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(33.dp)                 // was 52.dp; tighter row [web:561]
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp),  // was 14.dp
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Keep the tiny dot and fixed 8dp gaps
        Text(label, color = textSub, style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(4.dp)
                .clip(CircleShape)
                .background(textSub.copy(alpha = 0.6f))
        )
        Spacer(Modifier.width(8.dp))
        Text(value, color = textPrimary, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.weight(1f))
        Text("Pick", color = textPrimary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FieldSelect(
    label: String,
    value: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    options: List<String>,
    onSelect: (String) -> Unit,
    textPrimary: Color,
    textSub: Color,
    surface: Color
) {
    val cs = MaterialTheme.colorScheme
    val isDark = isSystemInDarkTheme()
    val fieldBg = if (isDark) Color(0xFF000000) else Color.White

    val gradMid = if (isDark) Color(0xFF7288A0) else Color(0xFF8799B0)


    // Themed menu backgrounds
    val entrySurface = if (isDark) Color(0xFF141820) else Color(0xFFF5F7F9)
    val entrySoft    = if (isDark) Color(0xFF1B2029) else Color(0xFFE6EAF0)

    val labelStyle = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.2.sp)
    val valueStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange
    ) {
        TextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(label, color = textSub, style = labelStyle) },
            textStyle = valueStyle,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = fieldBg,
                unfocusedContainerColor = fieldBg,
                focusedTextColor = textPrimary,
                unfocusedTextColor = textPrimary,
                cursorColor = cs.primary,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedLabelColor = textSub,
                unfocusedLabelColor = textSub
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .menuAnchor()                 // legacy overload, no params in older M3 [web:614]
                .fillMaxWidth()
                .height(52.dp)
        )

        // Fallback styling: shape + background via modifier for the popup [web:594][web:596]
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            modifier = Modifier
                .background(entrySurface)           // themed menu bg [web:594]
        ) {
            options.forEach { option ->
                val isSelected = option == value
                DropdownMenuItem(
                    text = {
                        // Per-item soft row look: wrap Text with a Box background if desired
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = option,
                                color = if (isSelected) textPrimary else gradMid,
                                style = if (isSelected)
                                    MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                else
                                    MaterialTheme.typography.bodyMedium
                            )
                        }
                    },
                    onClick = { onSelect(option) },
                    // Use default itemColors; this older API doesn’t support containerColor here [web:609]
                    colors = MenuDefaults.itemColors(
                        textColor = if (isSelected) cs.primary else textPrimary
                    )
                )
            }
        }
    }
}




@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MinimalDatePickerDialog(
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

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
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
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = onGradient
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

                        Spacer(Modifier.height(8.dp))

                        Button(
                            onClick = { onConfirm(selectedMillis) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = buttonContainer,
                                contentColor = buttonContent
                            ),
                            shape = RoundedCornerShape(20.dp)
                        ) { Text("OK") }

                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MinimalMonthPicker(
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

private fun millisToLocalDate(millis: Long?): LocalDate? =
    millis?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }
