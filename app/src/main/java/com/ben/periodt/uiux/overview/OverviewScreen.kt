package com.ben.periodt.uiux.overview

import android.R
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
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
    onClose: () -> Unit
) {
    if (!show) return

    val isDark = isSystemInDarkTheme()
    val cs = MaterialTheme.colorScheme

    // Gradient and theme colors matching AddCycle
    val gradTop = if (isDark) Color(0xFF7B8FA3) else Color(0xFF8FA0B1)
    val gradMid = if (isDark) Color(0xFF7288A0) else Color(0xFF8799B0)
    val gradBottom = if (isDark) Color(0xFF5A7396) else Color(0xFF6E87A7)
    val onGradient = Color.White
    val surfaceSoft = if (isDark) Color(0xFF1B2029) else Color(0xFFE6EAF0)
    val textPrimary = if (isDark) Color(0xFFF5F7FA) else Color(0xFF0F172A)
    val textSub = if (isDark) Color(0xFFBFC6D1) else Color(0xFF64748B)
    val buttonContainer = if (isDark) Color(0xFF000000) else Color.White
    val buttonContent = if (isDark) Color(0xFFFFFFFF) else Color.Black

    val scroll = rememberScrollState()

    androidx.compose.material3.BasicAlertDialog(onDismissRequest = onClose) {
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
                // Title
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp, start = 22.dp, end = 22.dp, bottom = 6.dp)
                        .clip(RoundedCornerShape(20.dp)),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Settings",
                        color = onGradient,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                // Scrollable content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .padding(top = 64.dp, bottom = 90.dp)   // leaves space for footer
                        .heightIn(max = 560.dp)
                        .verticalScroll(scroll)
                ) {
                    // One unified container for all sections (white in light, black in dark)
                    val isDark = isSystemInDarkTheme()
                    val unifiedBg = if (isDark) Color.Black else Color.White

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(unifiedBg)
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {

                            // FAQ
                            var faqExpanded by remember { mutableStateOf(false) }
                            ExpandableSection(
                                title = "FAQ",
                                expanded = faqExpanded,
                                onToggle = { faqExpanded = !faqExpanded }
                            ) {
                                val faqItems = remember {
                                    listOf(
                                "What data does the app store?" to "All cycle logs, symptoms, and preferences are stored locally on device; nothing is sent to a server.",
                                "Is internet required?" to "No — predictions, reminders, and charts work offline; internet is not needed for core features.",
                                "How are period dates predicted?" to "The app analyzes previously logged cycles to estimate the next period window and fertile days; predictions are estimates, not guarantees.",
                                "What if cycles are irregular?" to "The app still learns from entries and widens prediction windows to reflect variability; logging consistently improves accuracy.",
                                "Can fertility and ovulation be tracked?" to "Yes — the app projects a fertile window and likely ovulation day based on recent cycles; these are informational only.",
                                "Does the app have ads or trackers?" to "No ads and no third‑party analytics; the app is designed to be privacy‑first.",
                                "Will reminders work without opening the app?" to "Yes — once enabled, local notifications fire on schedule (e.g., a few days before a predicted period).",
                                "Can past cycles be edited or deleted?" to "Yes — previous entries can be updated or removed, and charts refresh automatically.",
                                "What symptoms can be tracked?" to "Common options include flow intensity, pain level, mood, and discharge color; additional notes can capture anything unique.",
                                "How accurate are predictions?" to "Accuracy depends on consistent logging and cycle regularity; the app shows confidence/uncertainty so expectations stay realistic.",
                                "Does the app support dark mode?" to "Yes — dark and light themes are supported; the setting can be changed in preferences.",
                                "Can data be backed up?" to "Data is on device by default; backup/restore can be added via an export/import option if enabled in settings.",
                                "Do notifications respect quiet hours?" to "System Do Not Disturb rules are respected; reminder timing can be adjusted in settings.",
                                "Will the app share data with other apps?" to "No — sharing only occurs if an explicit export is performed.",
                                "How does the fertile window help with planning?" to "It highlights higher‑probability days for conception and helps plan activities around symptoms throughout the cycle.",
                                "Is there a way to track PMS patterns?" to "Yes — symptom logs across cycles reveal recurring pre‑period patterns to prepare proactively.",
                                "Can multiple profiles be managed?" to "Not currently; one profile per device is supported, though multi‑profile may be considered later.",
                                "How are averages (cycle length, period length) computed?" to "Averages use completed cycles; including more recent cycles yields more current estimates.",
                                "What happens if a cycle is missed?" to "Logging can resume anytime; the model adjusts once new data is added.",
                                "Does the app support exporting to share with a clinician?" to "An export summary (dates, symptoms, averages) can be provided if enabled; otherwise screenshots of charts can be used."
                                    )
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    faqItems.forEach { (q, a) -> ExpandableQA(question = q, answer = a) }
                                }
                            }

                            HorizontalDivider(
                                thickness = 0.5.dp,                          // thinner line
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.18f)
                            )


                            // Privacy Policy
                            var privacyExpanded by remember { mutableStateOf(false) }
                            ExpandableSection(
                                title = "Privacy Policy",
                                expanded = privacyExpanded,
                                onToggle = { privacyExpanded = !privacyExpanded }
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("No data is collected.", style = MaterialTheme.typography.bodyMedium)
                                    Text("All cycle logs, symptoms, and preferences are stored locally on device. No analytics, no ads, no third‑party SDKs.", style = MaterialTheme.typography.bodyMedium)
                                    Text("Export and sharing are fully user‑initiated. Without export, data stays on the device.", style = MaterialTheme.typography.bodyMedium)
                                }
                            }

                            HorizontalDivider(
                                thickness = 0.5.dp,                          // thinner line
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.18f)
                            )


                            // Backup (disabled + badge)
                            DisabledRowWithBadge(
                                title = "Backup",
                                badge = "Coming soon"
                            )

                            HorizontalDivider(
                                thickness = 0.5.dp,                          // thinner line
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.18f)
                            )

                            // Backup (disabled + badge)
                            DisabledRowWithBadge(
                                title = "Widgets",
                                badge = "Coming soon"
                            )

                            HorizontalDivider(
                                thickness = 0.5.dp,                          // thinner line
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.18f)
                            )

                            // About
                            var aboutExpanded by remember { mutableStateOf(false) }
                            ExpandableSection(
                                title = "About",
                                expanded = aboutExpanded,
                                onToggle = { aboutExpanded = !aboutExpanded }
                            ) {
                                val context = LocalContext.current
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Ben", style = MaterialTheme.typography.bodyMedium)
                                    Text("Open source, designed for on‑device use.", style = MaterialTheme.typography.bodyMedium)

                                    // Clickable GitHub link (label only)
                                    val githubUrl = "https://github.com/benny10ben/Periodt-Track-Predict-Cycles"
                                    Text(
                                        text = "GitHub",
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.clickable {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(githubUrl))
                                            context.startActivity(intent)
                                        }
                                    )


                                    // License line
                                    Text(
                                        "License: GNU General Public License (GPL)",
                                        style = MaterialTheme.typography.bodyMedium
                                    )

                                    // “Want to connect?” acts as a button (opens email)
                                    Text(
                                        text = "Want to connect?",
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.clickable {
                                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                                data = Uri.parse("mailto:developer.ben10@gmail.com")
                                                putExtra(Intent.EXTRA_SUBJECT, "PeriodT: Feedback / Inquiry")
                                            }
                                            try {
                                                context.startActivity(intent)
                                            } catch (_: ActivityNotFoundException) {
                                                // Optionally show a toast/snackbar if no email app is present
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxSize()
                            .padding(top = 20.dp), // or .fillMaxWidth().height(200.dp)
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Dedicated to my love.", color = onGradient, style = MaterialTheme.typography.labelLarge)
                    }
                }

                    // Bottom-centered Close button (sticky footer)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .align(Alignment.Center)
                    ) {
                        TextButton(
                            onClick = onClose,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clip(RoundedCornerShape(26.dp))
                                .background(buttonContainer),
                            colors = ButtonDefaults.textButtonColors(contentColor = buttonContent)
                        ) {
                            Text("Close")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionContainer(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val container = if (isDark) Color.Black else Color.White   // black in dark, white in light [web:321]
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))                   // round corners first [web:491]
            .background(container)                              // then apply background [web:491]
            .padding(14.dp)
    ) {
        Column(content = content)
    }
}



@Composable
private fun DisabledRowWithBadge(
    title: String,
    badge: String
) {
    // Use onSurface at disabled alpha for accessibility-aware “greyed out” content [web:452][web:288]
    val base = MaterialTheme.colorScheme.onSurface
    val disabledColor = base.copy(alpha = 0.38f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = disabledColor,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(disabledColor.copy(alpha = 0.16f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = badge,
                style = MaterialTheme.typography.labelMedium,
                color = disabledColor
            )
        }
    }
}


@Composable
private fun ExpandableQA(question: String, answer: String) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "qaArrowRotation"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = { expanded = !expanded })
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = question,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer { rotationZ = rotation },
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(
                animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
                expandFrom = Alignment.Top
            ) + fadeIn(
                animationSpec = tween(durationMillis = 150, delayMillis = 50)
            ),
            exit = shrinkVertically(
                animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
                shrinkTowards = Alignment.Top
            ) + fadeOut(
                animationSpec = tween(durationMillis = 100)
            )
        ) {
            Text(
                text = answer,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
            )
        }
    }
}

@Composable
private fun ExpandableSection(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "arrowRotation"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .padding(vertical = 4.dp)
                .clickable(onClick = onToggle),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onToggle) {
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    modifier = Modifier.graphicsLayer { rotationZ = rotation }
                )
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                expandFrom = Alignment.Top
            ) + fadeIn(animationSpec = tween(durationMillis = 150, delayMillis = 75)),
            exit = shrinkVertically(
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                shrinkTowards = Alignment.Top
            ) + fadeOut(animationSpec = tween(durationMillis = 150))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 8.dp, start = 4.dp, end = 4.dp)
            ) { content() }
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

        Box(
            modifier = Modifier
                .weight(1f)
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
                    .width(maxOf(280.dp, points.size * 48.dp))
                    .fillMaxHeight()
            )
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
        val padding = 8.dp.toPx()
        val bottomPadding = 48.dp.toPx()
        val xLabelPadding = 16.dp.toPx()
        val chartWidth = size.width - padding * 2 - xLabelPadding
        val chartHeight = size.height - padding - bottomPadding
        val chartLeft = padding
        val chartTop = padding
        val chartRight = chartLeft + chartWidth
        val chartBottom = chartTop + chartHeight

        drawRect(color = surface, topLeft = Offset.Zero, size = size)

        val ySteps = 4
        repeat(ySteps + 1) { i ->
            val y = chartBottom - (i.toFloat() / ySteps) * chartHeight
            drawLine(gridColor, Offset(chartLeft, y), Offset(chartRight, y), 1.dp.toPx())
        }
        drawLine(axisColor, Offset(chartLeft, chartBottom), Offset(chartRight, chartBottom), 1.dp.toPx())

        val denom = (points.size - 1).coerceAtLeast(1).toFloat()

        // Vertical guides + X labels (now in sorted chronological order because data was sorted up front)
        points.forEachIndexed { index, (x, _) ->
            val xPos = chartLeft + (x / denom) * chartWidth
            drawLine(gridColor.copy(alpha = 0.7f), Offset(xPos, chartTop), Offset(xPos, chartBottom), 1.dp.toPx())

            if (index < dates.size) {
                val paint = android.graphics.Paint().apply {
                    color = labelColor.toArgb()
                    textSize = with(density) { 10.sp.toPx() }
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                val clampedX = xPos.coerceIn(chartLeft, chartRight + xLabelPadding * 0.5f)
                drawContext.canvas.nativeCanvas.drawText(
                    dates[index],
                    clampedX,
                    chartBottom + 18.dp.toPx(),
                    paint
                )
            }
        }

        val linePath = Path()
        val areaPath = Path()
        points.forEachIndexed { index, (x, y) ->
            val xPos = chartLeft + ((denom - x) / denom) * chartWidth
            val yPos = chartBottom - (y / yMax) * chartHeight
            if (index == 0) {
                linePath.moveTo(xPos, yPos)
                if (showArea) { areaPath.moveTo(xPos, chartBottom); areaPath.lineTo(xPos, yPos) }
            } else {
                linePath.lineTo(xPos, yPos)
                if (showArea) areaPath.lineTo(xPos, yPos)
            }
        }
        if (showArea) {
            val lastX = chartLeft + ((denom - points.last().first) / denom) * chartWidth
            areaPath.lineTo(lastX, chartBottom); areaPath.close()
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
            val xPos = chartLeft + ((denom - x) / denom) * chartWidth
            val yPos = chartBottom - (y / yMax) * chartHeight
            drawCircle(lineColor.copy(alpha = 0.18f), 8.dp.toPx(), Offset(xPos, yPos))
            drawCircle(lineColor, 3.5.dp.toPx(), Offset(xPos, yPos))
            drawCircle(Color.White, 2.dp.toPx(), Offset(xPos, yPos))
        }
    }
}
