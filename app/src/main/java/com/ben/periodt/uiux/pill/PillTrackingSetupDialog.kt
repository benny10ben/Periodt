package com.ben.periodt.uiux.pill

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ben.periodt.ui.theme.BricolageGrotesque
import com.ben.periodt.uiux.calendar.CleanDateCard
import com.ben.periodt.uiux.calendar.EntryStylePill
import com.ben.periodt.uiux.calendar.MinimalDatePickerDialog
import com.ben.periodt.uiux.calendar.millisToLocalDate
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PillTrackingSetupDialog(
    onDismiss: () -> Unit,
    onSave: (startDate: LocalDate, pillCount: Int) -> Unit
) {
    // --- STATE ---
    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedPillCount by remember { mutableIntStateOf(21) }
    var showDatePicker by remember { mutableStateOf(false) }

    val pillOptions = listOf(21, 24, 28)
    val formatter = remember { DateTimeFormatter.ofPattern("MMM dd, yyyy") }

    // --- THEME & COLORS (Synced with AddCycleDialog) ---
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()

    // Dynamic accent color
    val accentColor = if (isDark) Color(0xFFa68e74) else Color(0xFF2A3825).copy(alpha = 0.5f)

    // Gradient colors for DatePicker
    val pastelGreen = Color(0xFF2A3825).copy(alpha = 0.5f)
    val pastelOrange = Color(0xFFa68e74)
    val pastelMaroon = Color(0xFF4E1A1A)

    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSub = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF64748B)
    val surfaceFallback = if (isDark) Color.Black else Color.White

    val contentSurface = if (isDark) {
        Brush.linearGradient(0.0f to Color.Black, 1.0f to Color(0xFF1B1B1B))
    } else {
        Brush.linearGradient(colors = listOf(Color(0xFFF8FAFC), Color(0xFFf2f0e3)))
    }

    val pillBackground = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.92f).padding(vertical = 24.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = surfaceFallback),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().background(contentSurface)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp, start = 24.dp, end = 24.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Log Pills",
                        fontFamily = BricolageGrotesque,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = textPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = textPrimary)
                    }
                }

                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Explanation
                    Text(
                        text = "Set your current pack details to accurately predict your next withdrawal bleed.",
                        fontFamily = BricolageGrotesque,
                        fontSize = 14.sp,
                        color = textSub
                    )

                    // Start Date Card (Matching style)
                    CleanDateCard(
                        label = "Pack Start Date",
                        date = startDate.format(formatter),
                        icon = Icons.Rounded.CalendarToday,
                        bg = pillBackground,
                        textColor = textPrimary,
                        onClick = { showDatePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Pill Count Selection
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Active Pills per Pack",
                            fontFamily = BricolageGrotesque,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = textPrimary
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pillOptions.forEach { count ->
                                EntryStylePill(
                                    text = "$count Pills",
                                    isSelected = selectedPillCount == count,
                                    activeBg = accentColor, // Switched from fixed purple to theme accent
                                    activeText = Color.White,
                                    inactiveText = textSub,
                                    surface = surfaceFallback,
                                    onClick = { selectedPillCount = count }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Save Button (Synced with Add Cycle style)
                    Button(
                        onClick = { onSave(startDate, selectedPillCount) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor, // Switched to theme accent
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(0.dp)
                    ) {
                        Icon(Icons.Rounded.Medication, null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Start Pill Tracking",
                            fontFamily = BricolageGrotesque,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        MinimalDatePickerDialog(
            title = "Pack Start Date",
            brand = accentColor,
            gradTop = pastelGreen,
            gradMid = pastelOrange,
            gradBottom = pastelMaroon,
            onGradient = Color.White,
            buttonContainer = surfaceFallback,
            buttonContent = textPrimary,
            onDismiss = { showDatePicker = false },
            onConfirm = { ms ->
                millisToLocalDate(ms)?.let { startDate = it }
                showDatePicker = false
            }
        )
    }
}