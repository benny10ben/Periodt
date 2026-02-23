package com.ben.periodt.uiux.overview

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ben.periodt.uiux.shared.UpcomingBannerEnhanced
import com.ben.periodt.uiux.shared.getConfidenceLabel
import com.ben.periodt.uiux.shared.getCycleConfidence
import com.ben.periodt.uiux.shared.getDisplayName
import com.ben.periodt.uiux.shared.predictCycle
import com.ben.periodt.uiux.shared.pretty
import com.ben.periodt.viewmodel.PeriodViewModel
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

@Composable
fun OverviewScreen(
    viewModel: PeriodViewModel,
    isDarkTheme: Boolean = isSystemInDarkTheme()
) {
    val cycles by viewModel.cycles.collectAsState()
    val screenScroll = rememberScrollState()
    val isDark = isDarkTheme

    // Gradient palette
    val gradTop = if (isDark) Color(0xFF7B8FA3) else Color(0xFF8FA0B1)
    val gradMid = if (isDark) Color(0xFF7288A0) else Color(0xFF8799B0)
    val gradBottom = if (isDark) Color(0xFF5A7396) else Color(0xFF6E87A7)
    val onGradient = Color.White
    val onGradientMuted = onGradient.copy(alpha = if (isDark) 0.70f else 0.55f)

    val surface = if (isDark) Color(0xFF141820) else Color(0xFFF5F7F9)
    val textCol = if (isDark) Color(0xFFF5F7FA) else Color(0xFF0F172A)
    val subCol  = if (isDark) Color(0xFFBFC6D1) else Color(0xFF64748B)

    val prediction = remember(cycles) { predictCycle(cycles) }

    var showSettings by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        // Scrollable content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(screenScroll)
                .padding(horizontal = 16.dp)
                .padding(bottom = 90.dp)
                .padding(top = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Total Cycles",
                    value = "${cycles.size}",
                    gradTop = gradTop, gradMid = gradMid, gradBottom = gradBottom,
                    onGradient = onGradient, onGradientMuted = onGradientMuted,
                    modifier = Modifier.weight(1f)
                )

                val completed = cycles.filter { it.endDate != null }
                val avgLength = completed.mapNotNull { c ->
                    val s = c.startDate; val e = c.endDate
                    if (s != null && e != null) (e.toEpochDay() - s.toEpochDay()).toInt() else null
                }.takeIf { it.isNotEmpty() }?.average()?.toInt()

                StatCard(
                    title = "Avg Length",
                    value = avgLength?.let { "$it days" } ?: "N/A",
                    gradTop = gradTop, gradMid = gradMid, gradBottom = gradBottom,
                    onGradient = onGradient, onGradientMuted = onGradientMuted,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(14.dp))

            if (prediction != null) {
                UpcomingBannerEnhanced(
                    title = "Upcoming period",
                    windowText = "${prediction.minPeriodStart.pretty()} – ${prediction.maxPeriodStart.pretty()}",
                    mostLikely = "Most likely: ${prediction.mostLikelyPeriodStart.pretty()}",
                    badge = "${prediction.periodLength} days",
                    confidence = getCycleConfidence(prediction.cycleRegularity),
                    confidenceLabel = prediction.cycleRegularity.getDisplayName(),
                    gradTop = gradTop, gradMid = gradMid, gradBottom = gradBottom,
                    onGradient = onGradient, onGradientMuted = onGradientMuted,
                    mostLikelyDate = prediction.mostLikelyPeriodStart
                )
            } else {
                UpcomingBannerEnhanced(
                    title = "Upcoming period",
                    windowText = "Not enough data",
                    mostLikely = "Track more cycles for predictions",
                    badge = "",
                    confidence = 0f,
                    confidenceLabel = "No data",
                    gradTop = gradTop, gradMid = gradMid, gradBottom = gradBottom,
                    onGradient = onGradient, onGradientMuted = onGradientMuted
                )
            }

            Spacer(Modifier.height(14.dp))

            if (prediction != null) {
                UpcomingBannerEnhanced(
                    title = "Fertile window",
                    windowText = "${prediction.fertileWindow.start.pretty()} – ${prediction.fertileWindow.endInclusive.pretty()}",
                    mostLikely = "Ovulation: ${prediction.ovulationDay.pretty()}",
                    badge = "Confidence ${(prediction.ovulationConfidence * 100).toInt()}%",
                    confidence = prediction.ovulationConfidence,
                    confidenceLabel = getConfidenceLabel(prediction.ovulationConfidence),
                    gradTop = gradTop, gradMid = gradMid, gradBottom = gradBottom,
                    onGradient = onGradient, onGradientMuted = onGradientMuted
                )
            }

            Spacer(Modifier.height(14.dp))

            MinimalChartCard(
                title = "Bleeding intensity",
                surface = surface,
                titleColor = textCol
            ) {
                ScrollableLineChart(
                    points = bleedingSeriesVM(cycles),
                    dates = getDateLabels(cycles),
                    lineColor = if (isDark) Color(0xFFFF6B6B) else Color(0xFFD32F2F),
                    yLabels = listOf("S", "L", "M", "H"),
                    yMax = 3f,
                    showArea = true,
                    gridColor = if (isDark) Color(0xFF2A2F36) else Color(0xFFEAEAEA),
                    axisColor = if (isDark) Color(0xFF343A43) else Color(0xFFE0E0E0),
                    labelColor = subCol,
                    surface = surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                )
            }

            Spacer(Modifier.height(14.dp))

            MinimalChartCard(
                title = "Pain level",
                surface = surface,
                titleColor = textCol
            ) {
                ScrollableLineChart(
                    points = painSeriesVM(cycles),
                    dates = getDateLabels(cycles),
                    lineColor = if (isDark) Color(0xFF64B5F6) else Color(0xFF1976D2),
                    yLabels = (0..10 step 2).map { "$it" },
                    yMax = 10f,
                    showArea = true,
                    gridColor = if (isDark) Color(0xFF2A2F36) else Color(0xFFEAEAEA),
                    axisColor = if (isDark) Color(0xFF343A43) else Color(0xFFE0E0E0),
                    labelColor = subCol,
                    surface = surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                )
            }

            Spacer(Modifier.height(14.dp))

            MinimalChartCard(
                title = "Blood color",
                surface = surface,
                titleColor = textCol
            ) {
                BloodColorPieChart(
                    data = bloodColorDistributionVM(cycles),
                    surface = surface,
                    labelColor = subCol,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)   // a bit taller to fit bottom label
                        .padding(horizontal = 8.dp)
                )
            }



            Spacer(Modifier.height(12.dp))
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.systemBars))
        }
    }

    // Full settings dialog with sections
    SettingsDialog(
        show = showSettings,
        onClose = { showSettings = false },
    )
}

// ---------- Settings dialog and helpers ----------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    show: Boolean,
    onClose: () -> Unit,
    onExport: () -> Unit = {},
    onImport: () -> Unit = {},
    onClearData: () -> Unit = {}
) {
    if (!show) return

    // --- State ---
    var isFaqExpanded by remember { mutableStateOf(false) }
    var isPrivacyExpanded by remember { mutableStateOf(false) }
    var isAboutExpanded by remember { mutableStateOf(true) } // Open by default

    // --- Aesthetic Palette ---
    val isDark = isSystemInDarkTheme()
    val gradTop = if (isDark) Color(0xFF7B8FA3) else Color(0xFF8FA0B1)
    val gradMid = if (isDark) Color(0xFF7288A0) else Color(0xFF8799B0)
    val gradBottom = if (isDark) Color(0xFF5A7396) else Color(0xFF6E87A7)

    val onGradient = Color.White
    val contentSurface = if (isDark) Color.Black else Color.White
    val textPrimary = if (isDark) Color(0xFFF5F7FA) else Color(0xFF0F172A)
    val textSub = if (isDark) Color(0xFFBFC6D1) else Color(0xFF64748B)

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.85f) // Taller dialog to fit content
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(26.dp))
                    .background(Brush.verticalGradient(listOf(gradTop, gradMid, gradBottom)))
                    .background(Color.White.copy(alpha = 0.08f))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {

                    // --- HEADER ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp, start = 24.dp, end = 24.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = onGradient
                        )
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                                .clickable(onClick = onClose),
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

                    // --- CONTENT AREA ---
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                            .background(contentSurface)
                            .padding(horizontal = 24.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Spacer(Modifier.height(24.dp))

                        // 1. DATA MANAGEMENT
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SectionLabel("Data Management", Icons.Rounded.Storage, textPrimary)

                            SettingsActionCard(
                                title = "Export Data",
                                subtitle = "Save your history as CSV",
                                icon = Icons.Rounded.Upload,
                                color = textPrimary,
                                subColor = textSub,
                                onClick = onExport
                            )

                            SettingsActionCard(
                                title = "Import Data",
                                subtitle = "Restore from backup",
                                icon = Icons.Rounded.Download,
                                color = textPrimary,
                                subColor = textSub,
                                onClick = onImport
                            )

                            SettingsActionCard(
                                title = "Clear All Data",
                                subtitle = "Permanently delete history",
                                icon = Icons.Rounded.DeleteForever,
                                color = Color(0xFFEF5350),
                                subColor = textSub,
                                onClick = onClearData,
                                isDestructive = true
                            )
                        }

                        Spacer(Modifier.height(24.dp))

                        // 2. PRIVACY POLICY
                        ExpandableSection(
                            title = "Privacy Policy",
                            expanded = isPrivacyExpanded,
                            onToggle = { isPrivacyExpanded = !isPrivacyExpanded },
                            icon = Icons.Rounded.PrivacyTip,
                            accentColor = textPrimary,
                            textColor = textPrimary
                        ) {
                            Text(
                                text = "Your privacy is our priority. This app operates completely offline by default.\n\n" +
                                        "• No data is sent to external servers.\n" +
                                        "• All your cycle history, notes, and preferences are stored locally on this device.\n" +
                                        "• If you delete the app, your data is deleted unless you have exported a backup.\n" +
                                        "• We do not track your location or use analytics cookies.",
                                style = MaterialTheme.typography.bodySmall,
                                color = textSub,
                                lineHeight = 20.sp,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        // 3. FAQs (Restored)
                        ExpandableSection(
                            title = "Frequently Asked Questions",
                            expanded = isFaqExpanded,
                            onToggle = { isFaqExpanded = !isFaqExpanded },
                            icon = Icons.Rounded.HelpOutline,
                            accentColor = textPrimary,
                            textColor = textPrimary
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                ExpandableQA(
                                    "How is the prediction calculated?",
                                    "We use a standard 28-day cycle model that adjusts dynamically based on the average length of your last 3 logged periods.",
                                    textPrimary, textSub
                                )
                                ExpandableQA(
                                    "How do I edit a past cycle?",
                                    "Go to the calendar screen, find the cycle in the list below the calendar, expand the card, and tap the 'Edit Entry' button.",
                                    textPrimary, textSub
                                )
                                ExpandableQA(
                                    "What does the fertile window mean?",
                                    "This is the estimated time during your cycle when you are most likely to conceive. It is usually 5 days before ovulation and the day of ovulation.",
                                    textPrimary, textSub
                                )
                                ExpandableQA(
                                    "Can I sync across devices?",
                                    "Currently, sync is not supported to ensure maximum privacy. Please use the Export feature to transfer data manually.",
                                    textPrimary, textSub
                                )
                                ExpandableQA(
                                    "What happens if I forget to log?",
                                    "You can log past dates at any time. Just tap the '+' button and select the dates from the past.",
                                    textPrimary, textSub
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // 4. ABOUT
                        ExpandableSection(
                            title = "About Periodt",
                            expanded = isAboutExpanded,
                            onToggle = { isAboutExpanded = !isAboutExpanded },
                            icon = Icons.Rounded.Info,
                            accentColor = textPrimary,
                            textColor = textPrimary
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 4.dp)) {
                                Text(
                                    "Periodt v1.0.4",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Designed to be simple, private, and aesthetic. Thank you for using our app to track your health journey.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = textSub
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    "Developed with ❤️ by Ben",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = gradBottom
                                )
                            }
                        }

                        Spacer(Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

// --- HELPER COMPOSABLES ---

@Composable
private fun SectionLabel(text: String, icon: ImageVector, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(imageVector = icon, contentDescription = null, tint = tint.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
        Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = tint)
    }
}

@Composable
private fun SettingsActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    subColor: Color,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isDestructive) color.copy(alpha = 0.08f) else subColor.copy(alpha = 0.08f))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        }

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = color)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = subColor)
        }

        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = subColor.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ExpandableSection(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    icon: ImageVector,
    accentColor: Color,
    textColor: Color,
    content: @Composable () -> Unit
) {
    val rotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "arrowRotation")

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onToggle)
                .padding(vertical = 12.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor.copy(alpha = 0.8f),
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = textColor,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
                modifier = Modifier.graphicsLayer { rotationZ = rotation },
                tint = textColor.copy(alpha = 0.5f)
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(modifier = Modifier.padding(start = 36.dp, top = 0.dp, bottom = 12.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun ExpandableQA(question: String, answer: String, textColor: Color, subColor: Color) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { expanded = !expanded },
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = "• $question",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = textColor.copy(alpha = 0.9f),
                modifier = Modifier.weight(1f)
            )
        }
        AnimatedVisibility(visible = expanded) {
            Text(
                text = answer,
                style = MaterialTheme.typography.bodySmall,
                color = subColor,
                lineHeight = 18.sp,
                modifier = Modifier.padding(start = 12.dp, top = 6.dp, bottom = 4.dp)
            )
        }
    }
}

// ---------- Gradient Stat card ----------
@Composable
private fun StatCard(
    title: String,
    value: String,
    gradTop: Color,
    gradMid: Color,
    gradBottom: Color,
    onGradient: Color,
    onGradientMuted: Color,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(gradTop, gradMid, gradBottom)))
                .background(Color.White.copy(alpha = 0.06f))
                .padding(16.dp)
        ) {
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = onGradient
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = onGradientMuted
                )
            }
        }
    }
}

// ---------- Minimal chart card ----------
@Composable
private fun MinimalChartCard(
    title: String,
    surface: Color,
    titleColor: Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = titleColor
            )
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BloodColorPieChart(
    data: List<Pair<String, Float>>,  // (label, fraction)
    surface: Color,
    labelColor: Color,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text("No data yet", style = MaterialTheme.typography.bodyMedium, color = labelColor)
        }
        return
    }

    // Normalize
    val total = data.sumOf { it.second.toDouble() }.toFloat().coerceAtLeast(0.0001f)
    val normalized = data
        .map { it.first.lowercase() to (it.second / total) }
        .filter { it.second > 0f }

    // Palette
    val colorMap = mapOf(
        "bright red" to Color(0xFFE53935),
        "dark red"   to Color(0xFFC62828),
        "brown"      to Color(0xFF8D6E63),
        "pink"       to Color(0xFFF48FB1),
        "orange"     to Color(0xFFFFA726),
        "purple"     to Color(0xFF8E24AA)
    )

    val density = LocalDensity.current

    // Build a human legend list for the bottom row(s)
    val legendItems = normalized.map { (label, frac) ->
        val pct = (frac * 100f)
        Triple(label.replaceFirstChar { it.uppercase() }, pct, colorMap[label] ?: Color(0xFF90A4AE))
    }

    Column(modifier = modifier) {
        // Donut with percent labels inside
        Canvas(
            Modifier
                .fillMaxWidth()
                .weight(1f, fill = true)
        ) {
            drawRect(color = surface, topLeft = Offset.Zero, size = size)

            val padding = 20.dp.toPx()
            val diameter = minOf(size.width, size.height) - padding * 2
            val radius = diameter / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            val ring = radius * 0.2f
            val stroke = Stroke(width = radius - ring)

            var startAngle = -90f

            normalized.forEach { (label, frac) ->
                val sweep = frac * 360f
                val key = label.lowercase()
                val col = colorMap[key] ?: Color(0xFF90A4AE)

                // Slice
                drawArc(
                    color = col,
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                    style = stroke
                )

                // Percentage label inside slice (skip tiny slices)
                val pct = (frac * 100f)
                if (pct >= 4f) { // avoid clutter under 4%
                    val midAngleDeg = startAngle + sweep / 2f
                    val midAngleRad = Math.toRadians(midAngleDeg.toDouble()).toFloat()
                    // Place text between ring and outer radius
                    val textR = ring + (radius - ring) * 0.9f
                    val tx = center.x + textR * kotlin.math.cos(midAngleRad)
                    val ty = center.y + textR * kotlin.math.sin(midAngleRad)

                    val paint = android.graphics.Paint().apply {
                        color = Color.White.toArgb()
                        textSize = with(density) { 11.sp.toPx() }
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    drawContext.canvas.nativeCanvas.drawText(
                        "${pct.toInt()}%",
                        tx,
                        ty + 8.dp.toPx(),
                        paint
                    )
                }

                startAngle += sweep
            }

            // Donut center
            drawCircle(color = surface, radius = ring - 2.dp.toPx(), center = center)
        }

        Spacer(Modifier.height(30.dp))

        // Bottom legend: wrap items centered
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            maxItemsInEachRow = Int.MAX_VALUE,
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            legendItems.forEach { (name, pct, color) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(color)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "$name",
                        style = MaterialTheme.typography.labelMedium,
                        color = labelColor
                    )
                }
            }
        }
    }
}



// ---------- Sorting helpers for charts ----------
private fun cyclesSorted(cycles: List<PeriodViewModel.Cycle>): List<PeriodViewModel.Cycle> =
    cycles.sortedWith(compareBy(nullsLast()) { it.startDate })

private fun getDateLabels(cycles: List<PeriodViewModel.Cycle>): List<String> {
    val formatter = DateTimeFormatter.ofPattern("MMM dd")
    return cyclesSorted(cycles).mapNotNull { it.startDate?.format(formatter) }
}

private fun bleedingSeriesVM(cycles: List<PeriodViewModel.Cycle>): List<Pair<Float, Float>> {
    val map = mapOf("none" to 0f, "light" to 1f, "medium" to 2f, "heavy" to 3f)
    val sorted = cyclesSorted(cycles)
    return sorted.mapIndexedNotNull { index, c ->
        c.startDate?.let {
            index.toFloat() to (map[c.bleeding.lowercase()] ?: c.bleeding.toFloatOrNull() ?: 0f)
        }
    }
}

private fun painSeriesVM(cycles: List<PeriodViewModel.Cycle>): List<Pair<Float, Float>> {
    val sorted = cyclesSorted(cycles)
    return sorted.mapIndexedNotNull { index, c ->
        c.startDate?.let { index.toFloat() to c.painLevel.coerceIn(0, 10).toFloat() }
    }
}

private fun bloodColorDistributionVM(cycles: List<PeriodViewModel.Cycle>): List<Pair<String, Float>> {
    val counts = cycles.groupingBy { it.bloodColor.lowercase() }.eachCount()
    val total = counts.values.sum()
    if (total == 0) return emptyList()
    return counts.entries.sortedByDescending { it.value }
        .map { it.key to (it.value.toFloat() / total.toFloat()) }
}

// ---------- Charts ----------
@Composable
private fun ScrollableLineChart(
    points: List<Pair<Float, Float>>,
    dates: List<String>,
    lineColor: Color,
    yLabels: List<String>,
    yMax: Float,
    showArea: Boolean,
    gridColor: Color,
    axisColor: Color,
    labelColor: Color,
    surface: Color,
    modifier: Modifier = Modifier
) {
    val hScroll = rememberScrollState()
    val scope = rememberCoroutineScope()

    if (points.isEmpty()) {
        Box(modifier, contentAlignment = Alignment.Center) {
            Text("No data yet", style = MaterialTheme.typography.bodyMedium, color = labelColor)
        }
        return
    }

    LaunchedEffect(points.size) {
        scope.launch {
            hScroll.animateScrollTo(
                hScroll.maxValue,
                animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing)
            )
        }
    }

    Row(modifier = modifier) {
        YAxisLabels(
            yLabels = yLabels,
            labelColor = labelColor,
            axisColor = axisColor,
            modifier = Modifier
                .width(32.dp)
                .fillMaxHeight()
        )

        // 1. Measure the exact available screen space FIRST (no scroll modifier here)
        BoxWithConstraints(
            modifier = Modifier.weight(1f)
        ) {
            // This 'maxWidth' is explicitly pulling from the BoxWithConstraints scope
            val minChartWidth = this.maxWidth
            val dynamicWidth = (points.size * 64.dp)

            // 2. Apply the scrolling INSIDE the bounded area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(hScroll)
            ) {
                LineChartContent(
                    points = points,
                    dates = dates,
                    lineColor = lineColor,
                    yMax = yMax,
                    showArea = showArea,
                    gridColor = gridColor,
                    axisColor = axisColor,
                    labelColor = labelColor,
                    surface = surface,
                    modifier = Modifier
                        .width(maxOf(minChartWidth, dynamicWidth))
                        .fillMaxHeight()
                )
            }
        }
    }
}

@Composable
private fun YAxisLabels(
    yLabels: List<String>,
    labelColor: Color,
    axisColor: Color,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    Canvas(modifier) {
        val padding = 8.dp.toPx()
        val bottomPadding = 48.dp.toPx()
        val chartHeight = size.height - padding - bottomPadding
        val chartTop = padding
        val chartBottom = chartTop + chartHeight

        drawLine(
            color = axisColor,
            start = Offset(size.width - 1.dp.toPx(), chartTop),
            end = Offset(size.width - 1.dp.toPx(), chartBottom),
            strokeWidth = 1.dp.toPx()
        )

        yLabels.forEachIndexed { index, label ->
            val y = chartBottom - (index.toFloat() / (yLabels.size - 1)) * chartHeight
            val paint = android.graphics.Paint().apply {
                color = labelColor.toArgb()
                textSize = with(density) { 11.sp.toPx() }
                textAlign = android.graphics.Paint.Align.RIGHT
            }
            drawContext.canvas.nativeCanvas.drawText(
                label,
                size.width - 6.dp.toPx(),
                y + 4.dp.toPx(),
                paint
            )
        }
    }
}

@Composable
private fun LineChartContent(
    points: List<Pair<Float, Float>>,
    dates: List<String>,
    lineColor: Color,
    yMax: Float,
    showArea: Boolean,
    gridColor: Color,
    axisColor: Color,
    labelColor: Color,
    surface: Color,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    Canvas(modifier) {
        // --- UPDATED PADDING MATH ---
        val horizontalPadding = 32.dp.toPx() // Pulls chart inwards so text doesn't clip
        val bottomPadding = 48.dp.toPx()
        val topPadding = 16.dp.toPx()

        val chartLeft = horizontalPadding
        val chartRight = size.width - horizontalPadding
        val chartWidth = chartRight - chartLeft
        val chartTop = topPadding
        val chartBottom = size.height - bottomPadding
        val chartHeight = chartBottom - chartTop

        drawRect(color = surface, topLeft = Offset.Zero, size = size)

        // Horizontal Grid Lines
        val ySteps = 4
        repeat(ySteps + 1) { i ->
            val y = chartBottom - (i.toFloat() / ySteps) * chartHeight
            drawLine(gridColor, Offset(chartLeft, y), Offset(chartRight, y), 1.dp.toPx())
        }
        drawLine(axisColor, Offset(chartLeft, chartBottom), Offset(chartRight, chartBottom), 1.dp.toPx())

        val denom = (points.size - 1).coerceAtLeast(1).toFloat()

        // Vertical guides + X labels
        points.forEachIndexed { index, (x, _) ->
            val xPos = chartLeft + (x / denom) * chartWidth
            drawLine(gridColor.copy(alpha = 0.7f), Offset(xPos, chartTop), Offset(xPos, chartBottom), 1.dp.toPx())

            if (index < dates.size) {
                val paint = android.graphics.Paint().apply {
                    color = labelColor.toArgb()
                    textSize = with(density) { 10.sp.toPx() }
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                // No more clamping needed because of horizontalPadding
                drawContext.canvas.nativeCanvas.drawText(
                    dates[index],
                    xPos,
                    chartBottom + 22.dp.toPx(),
                    paint
                )
            }
        }

        val linePath = Path()
        val areaPath = Path()

        var prevXPos = 0f
        var prevYPos = 0f

        points.forEachIndexed { index, (x, y) ->
            val xPos = chartLeft + (x / denom) * chartWidth
            val yPos = chartBottom - (y / yMax) * chartHeight

            if (index == 0) {
                linePath.moveTo(xPos, yPos)
                if (showArea) {
                    areaPath.moveTo(xPos, chartBottom)
                    areaPath.lineTo(xPos, yPos)
                }
            } else {
                val controlPointX = (prevXPos + xPos) / 2f

                linePath.cubicTo(
                    x1 = controlPointX, y1 = prevYPos,
                    x2 = controlPointX, y2 = yPos,
                    x3 = xPos, y3 = yPos
                )

                if (showArea) {
                    areaPath.cubicTo(
                        x1 = controlPointX, y1 = prevYPos,
                        x2 = controlPointX, y2 = yPos,
                        x3 = xPos, y3 = yPos
                    )
                }
            }
            prevXPos = xPos
            prevYPos = yPos
        }

        if (showArea && points.isNotEmpty()) {
            val lastX = chartLeft + (points.last().first / denom) * chartWidth
            areaPath.lineTo(lastX, chartBottom)
            areaPath.close()

            drawPath(
                areaPath,
                brush = Brush.verticalGradient(
                    listOf(lineColor.copy(0.18f), lineColor.copy(0.04f)),
                    chartTop,
                    chartBottom
                )
            )
        }

        drawPath(linePath, color = lineColor, style = Stroke(width = 2.5.dp.toPx()))

        points.forEach { (x, y) ->
            val xPos = chartLeft + (x / denom) * chartWidth
            val yPos = chartBottom - (y / yMax) * chartHeight

            drawCircle(lineColor.copy(alpha = 0.18f), 8.dp.toPx(), Offset(xPos, yPos))
            drawCircle(lineColor, 3.5.dp.toPx(), Offset(xPos, yPos))
            drawCircle(Color.White, 2.dp.toPx(), Offset(xPos, yPos))
        }
    }
}
