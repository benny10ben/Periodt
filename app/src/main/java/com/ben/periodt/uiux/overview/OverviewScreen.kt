package com.ben.periodt.uiux.overview

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
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
import com.ben.periodt.ui.theme.BricolageGrotesque
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

    var isFaqExpanded by remember { mutableStateOf(false) }
    var isPrivacyExpanded by remember { mutableStateOf(false) }
    var isAboutExpanded by remember { mutableStateOf(true) }

    val isDark = isSystemInDarkTheme()

    val dialogBrush = if (isDark) {
        Brush.linearGradient(listOf(Color(0xFFC8D4E5), Color(0xFF8089D2)))
    } else {
        Brush.linearGradient(listOf(Color(0xFF2C3F70), Color(0xFF2C3F70)))
    }

    val contentSurface = if (isDark) Color(0xFF1B1B1B) else Color.White
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSub = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF64748B)
    val accentColor = if (isDark) Color(0xFF8089D2) else Color(0xFF1B1B1B)

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.85f)
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
                        .fillMaxSize()
                        .clip(RoundedCornerShape(24.dp))
                        .background(contentSurface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp, start = 24.dp, end = 24.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Settings",
                            fontFamily = BricolageGrotesque,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = textPrimary
                        )
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(textSub.copy(alpha = 0.1f))
                                .clickable(onClick = onClose),
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
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 24.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Spacer(Modifier.height(24.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SectionLabel("Data Management", Icons.Rounded.Storage, textPrimary)

                            SettingsActionCard(
                                title = "Export Data",
                                subtitle = "Save your history as CSV",
                                icon = Icons.Rounded.Upload,
                                color = textPrimary,
                                subColor = textSub,
                                iconBg = textSub.copy(alpha = 0.1f),
                                iconTint = textPrimary,
                                onClick = onExport
                            )

                            SettingsActionCard(
                                title = "Import Data",
                                subtitle = "Restore from backup",
                                icon = Icons.Rounded.Download,
                                color = textPrimary,
                                subColor = textSub,
                                iconBg = textSub.copy(alpha = 0.1f),
                                iconTint = textPrimary,
                                onClick = onImport
                            )

                            SettingsActionCard(
                                title = "Clear All Data",
                                subtitle = "Permanently delete history",
                                icon = Icons.Rounded.DeleteForever,
                                color = Color(0xFFEF5350),
                                subColor = textSub,
                                iconBg = Color(0xFFEF5350).copy(alpha = 0.1f),
                                iconTint = Color(0xFFEF5350),
                                onClick = onClearData,
                                isDestructive = true
                            )
                        }

                        Spacer(Modifier.height(24.dp))

                        ExpandableSection(
                            title = "Privacy Policy",
                            expanded = isPrivacyExpanded,
                            onToggle = { isPrivacyExpanded = !isPrivacyExpanded },
                            icon = Icons.Rounded.PrivacyTip,
                            iconColor = textPrimary,
                            textColor = textPrimary
                        ) {
                            Text(
                                text = "Your privacy is our priority. This app operates completely offline by default.\n\n" +
                                        "• No data is sent to external servers.\n" +
                                        "• All your cycle history, notes, and preferences are stored locally on this device.\n" +
                                        "• If you delete the app, your data is deleted unless you have exported a backup.\n" +
                                        "• We do not track your location or use analytics cookies.",
                                fontFamily = BricolageGrotesque,
                                fontWeight = FontWeight.Normal,
                                style = MaterialTheme.typography.bodySmall,
                                color = textSub,
                                lineHeight = 20.sp,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        ExpandableSection(
                            title = "Frequently Asked Questions",
                            expanded = isFaqExpanded,
                            onToggle = { isFaqExpanded = !isFaqExpanded },
                            icon = Icons.Rounded.HelpOutline,
                            iconColor = textPrimary,
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

                        ExpandableSection(
                            title = "About Periodt",
                            expanded = isAboutExpanded,
                            onToggle = { isAboutExpanded = !isAboutExpanded },
                            icon = Icons.Rounded.Info,
                            iconColor = textPrimary,
                            textColor = textPrimary
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 4.dp)) {
                                Text(
                                    text = "Periodt v1.0.4",
                                    fontFamily = BricolageGrotesque,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Designed to be simple, private, and aesthetic. Thank you for using our app to track your health journey.",
                                    fontFamily = BricolageGrotesque,
                                    fontWeight = FontWeight.Normal,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = textSub
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = "Developed with ❤️ by Ben",
                                    fontFamily = BricolageGrotesque,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor
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
                fontFamily = BricolageGrotesque,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = textColor.copy(alpha = 0.9f),
                modifier = Modifier.weight(1f)
            )
        }
        AnimatedVisibility(visible = expanded) {
            Text(
                text = answer,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Normal,
                style = MaterialTheme.typography.bodySmall,
                color = subColor,
                lineHeight = 18.sp,
                modifier = Modifier.padding(start = 12.dp, top = 6.dp, bottom = 4.dp)
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String, icon: ImageVector, iconTint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        Text(
            text = text,
            fontFamily = BricolageGrotesque,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = iconTint
        )
    }
}

@Composable
private fun SettingsActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    subColor: Color,
    iconBg: Color,
    iconTint: Color,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (isDestructive) color.copy(alpha = 0.05f) else subColor.copy(alpha = 0.05f))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
        }

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = BricolageGrotesque,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
            Text(
                text = subtitle,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.Normal,
                style = MaterialTheme.typography.labelSmall,
                color = subColor
            )
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
    iconColor: Color,
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
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = title,
                fontFamily = BricolageGrotesque,
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
            Column(modifier = Modifier.padding(start = 42.dp, top = 0.dp, bottom = 12.dp)) {
                content()
            }
        }
    }
}


@Composable
private fun StatCard(
    title: String,
    value: String,
    gradTop: Color, // Kept for API compatibility but used as Accent
    gradMid: Color, // Unused
    gradBottom: Color, // Unused
    onGradient: Color, // Used as Primary Text
    onGradientMuted: Color, // Used as Secondary Text
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    // AESTHETIC MAPPING
    // Surface: Dark #1B1B1B / Light White
    val surfaceColor = if (isDark) Color(0xFF1B1B1B).copy(alpha = 0.6f) else Color.White

    // Changed to Card to remove elevation/shadows
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(26.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) // Removed elevation
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(surfaceColor)
                // Border removed
                .padding(20.dp)
        ) {
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = value,
                    fontFamily = BricolageGrotesque,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = if(isDark) Color.White else Color(0xFF0F172A)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = title.uppercase(),
                    fontFamily = BricolageGrotesque,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 1.sp
                    ),
                    color = if(isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF64748B)
                )
            }
        }
    }
}

// ---------- Minimal Chart Card (Modern Update) ----------
@Composable
private fun MinimalChartCard(
    title: String,
    surface: Color,
    titleColor: Color,
    content: @Composable () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    // Explicitly override surface for dark mode if needed
    val cardSurface = if (isDark) Color(0xFF1B1B1B).copy(alpha = 0.6f) else surface

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = cardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), // Removed elevation
        border = null // Removed border
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = title,
                fontFamily = BricolageGrotesque,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = titleColor
            )
            Spacer(Modifier.height(24.dp))
            content()
        }
    }
}

// ---------- Blood Color Pie Chart ----------
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BloodColorPieChart(
    data: List<Pair<String, Float>>,
    surface: Color,
    labelColor: Color,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        Box(modifier.height(200.dp), contentAlignment = Alignment.Center) {
            Text(
                text = "No data yet",
                fontFamily = BricolageGrotesque,
                style = MaterialTheme.typography.bodyMedium,
                color = labelColor.copy(alpha = 0.5f)
            )
        }
        return
    }

    val total = data.sumOf { it.second.toDouble() }.toFloat().coerceAtLeast(0.0001f)
    val normalized = data
        .map { it.first.lowercase() to (it.second / total) }
        .filter { it.second > 0f }

    val colorMap = mapOf(
        "bright red" to Color(0xFFFF5252),
        "dark red"   to Color(0xFFD32F2F),
        "brown"      to Color(0xFFA1887F),
        "pink"       to Color(0xFFF06292),
        "orange"     to Color(0xFFFFB74D),
        "purple"     to Color(0xFFBA68C8)
    )

    val density = LocalDensity.current

    val legendItems = normalized.map { (label, frac) ->
        val pct = (frac * 100f)
        Triple(label.replaceFirstChar { it.uppercase() }, pct, colorMap[label] ?: Color(0xFF90A4AE))
    }

    Column(modifier = modifier) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {
            val padding = 10.dp.toPx()
            val diameter = minOf(size.width, size.height) - padding * 2
            val radius = diameter / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            val ringWidth = 50.dp.toPx()
            val stroke = Stroke(width = ringWidth, cap = StrokeCap.Round)

            var startAngle = -90f
            val gapAngle = 2f

            normalized.forEach { (label, frac) ->
                val sweep = (frac * 360f) - gapAngle
                val key = label.lowercase()
                val col = colorMap[key] ?: Color(0xFF90A4AE)

                if (sweep > 0) {
                    drawArc(
                        color = col,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(center.x - (radius - ringWidth/2), center.y - (radius - ringWidth/2)),
                        size = androidx.compose.ui.geometry.Size((radius - ringWidth/2) * 2, (radius - ringWidth/2) * 2),
                        style = stroke
                    )
                }

                val pct = (frac * 100f)
                if (pct >= 8f) {
                    val midAngleDeg = startAngle + sweep / 2f
                    val midAngleRad = Math.toRadians(midAngleDeg.toDouble()).toFloat()
                    val textR = radius - ringWidth / 2
                    val tx = center.x + textR * kotlin.math.cos(midAngleRad)
                    val ty = center.y + textR * kotlin.math.sin(midAngleRad)

                    val paint = android.graphics.Paint().apply {
                        color = Color.White.toArgb()
                        textSize = with(density) { 12.sp.toPx() }
                        textAlign = android.graphics.Paint.Align.CENTER
                        // Set native Typeface to match BricolageGrotesque Bold
                        typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
                        isAntiAlias = true
                    }
                    val textHeight = paint.descent() - paint.ascent()
                    val textOffset = (textHeight / 2) - paint.descent()

                    drawContext.canvas.nativeCanvas.drawText(
                        "${pct.toInt()}%",
                        tx,
                        ty + textOffset,
                        paint
                    )
                }
                startAngle += (sweep + gapAngle)
            }
        }

        Spacer(Modifier.height(24.dp))

        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            maxItemsInEachRow = 3,
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            legendItems.forEach { (name, pct, color) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(color.copy(alpha = 0.1f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = name,
                        fontFamily = BricolageGrotesque, // Applied font
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
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

// ---------- Charts (Line Chart Updated) ----------
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
        Box(modifier.height(200.dp), contentAlignment = Alignment.Center) {
            Text("No data yet", style = MaterialTheme.typography.bodyMedium, color = labelColor.copy(alpha = 0.5f))
        }
        return
    }

    LaunchedEffect(points.size) {
        scope.launch {
            hScroll.animateScrollTo(
                hScroll.maxValue,
                animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
            )
        }
    }

    // Modern layout: Y-Axis floating on left, Chart scrolling
    Row(modifier = modifier.height(220.dp)) {
        YAxisLabels(
            yLabels = yLabels,
            labelColor = labelColor.copy(alpha = 0.6f),
            axisColor = Color.Transparent, // Hidden Y-axis line for cleaner look
            modifier = Modifier
                .width(36.dp)
                .fillMaxHeight()
        )

        BoxWithConstraints(
            modifier = Modifier.weight(1f)
        ) {
            val minChartWidth = this.maxWidth
            // Wider spacing for more breathing room
            val dynamicWidth = (points.size * 70.dp).coerceAtLeast(minChartWidth)

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
                    gridColor = gridColor.copy(alpha = 0.05f), // Very subtle grid
                    axisColor = axisColor.copy(alpha = 0.1f),
                    labelColor = labelColor.copy(alpha = 0.7f),
                    surface = surface,
                    modifier = Modifier
                        .width(dynamicWidth)
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
        val bottomPadding = 40.dp.toPx()
        val topPadding = 20.dp.toPx()
        val chartHeight = size.height - bottomPadding - topPadding
        val chartBottom = size.height - bottomPadding

        yLabels.forEachIndexed { index, label ->
            val y = chartBottom - (index.toFloat() / (yLabels.size - 1)) * chartHeight
            val paint = android.graphics.Paint().apply {
                color = labelColor.toArgb()
                textSize = with(density) { 10.sp.toPx() }
                textAlign = android.graphics.Paint.Align.RIGHT
                typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
            }
            val textHeight = paint.descent() - paint.ascent()
            val textOffset = (textHeight / 2) - paint.descent()

            drawContext.canvas.nativeCanvas.drawText(
                label,
                size.width - 8.dp.toPx(),
                y + textOffset,
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
        val horizontalPadding = 32.dp.toPx()
        val bottomPadding = 40.dp.toPx()
        val topPadding = 20.dp.toPx()

        val chartLeft = horizontalPadding
        val chartRight = size.width - horizontalPadding
        val chartWidth = chartRight - chartLeft
        val chartBottom = size.height - bottomPadding
        val chartHeight = chartBottom - topPadding

        val ySteps = 4
        val gridPaint = android.graphics.Paint().apply {
            color = gridColor.toArgb()
            strokeWidth = 1.dp.toPx()
            pathEffect = android.graphics.DashPathEffect(floatArrayOf(10f, 10f), 0f)
        }

        repeat(ySteps + 1) { i ->
            val y = chartBottom - (i.toFloat() / ySteps) * chartHeight
            drawContext.canvas.nativeCanvas.drawLine(chartLeft, y, chartRight, y, gridPaint)
        }

        drawLine(axisColor, Offset(chartLeft, chartBottom), Offset(chartRight, chartBottom), 1.5.dp.toPx())

        val denom = (points.size - 1).coerceAtLeast(1).toFloat()

        points.forEachIndexed { index, (x, _) ->
            val xPos = chartLeft + (x / denom) * chartWidth
            if (index < dates.size) {
                val paint = android.graphics.Paint().apply {
                    color = labelColor.toArgb()
                    textSize = with(density) { 10.sp.toPx() }
                    textAlign = android.graphics.Paint.Align.CENTER
                    typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
                }
                drawContext.canvas.nativeCanvas.drawText(
                    dates[index],
                    xPos,
                    size.height - 10.dp.toPx(),
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
                linePath.cubicTo(controlPointX, prevYPos, controlPointX, yPos, xPos, yPos)
                if (showArea) {
                    areaPath.cubicTo(controlPointX, prevYPos, controlPointX, yPos, xPos, yPos)
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
                    listOf(lineColor.copy(0.25f), lineColor.copy(0.0f)),
                    startY = topPadding,
                    endY = chartBottom
                )
            )
        }

        drawPath(linePath, color = lineColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

        points.forEach { (x, y) ->
            val xPos = chartLeft + (x / denom) * chartWidth
            val yPos = chartBottom - (y / yMax) * chartHeight
            drawCircle(lineColor.copy(alpha = 0.2f), 6.dp.toPx(), Offset(xPos, yPos))
            drawCircle(surface, 3.dp.toPx(), Offset(xPos, yPos))
            drawCircle(lineColor, 3.dp.toPx(), Offset(xPos, yPos), style = Stroke(width = 1.5.dp.toPx()))
        }
    }
}