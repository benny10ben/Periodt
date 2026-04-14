package com.ben.periodt.ui.overview

import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import com.ben.periodt.ui.theme.BricolageGrotesque
import com.ben.periodt.ui.theme.LocalAppIsDark
import com.ben.periodt.ui.shared.PostPillState
import com.ben.periodt.ui.shared.UpcomingBannerEnhanced
import com.ben.periodt.ui.shared.calculatePeriodLength
import com.ben.periodt.ui.shared.getConfidenceLabel
import com.ben.periodt.ui.shared.getCycleConfidence
import com.ben.periodt.ui.shared.getDisplayName
import com.ben.periodt.ui.shared.pretty
import com.ben.periodt.viewmodel.PeriodViewModel
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.unit.IntSize
import androidx.compose.animation.SizeTransform
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import com.ben.periodt.R
import kotlin.math.round

private val SIZE_XXS = 11.sp
private val SIZE_XS  = 12.sp
private val SIZE_SM  = 13.sp
private val SIZE_MD  = 14.sp
private val SIZE_LG  = 15.sp
private val SIZE_XL  = 20.sp

@Composable
fun OverviewScreen(viewModel: PeriodViewModel) {
    val cycles        by viewModel.cycles.collectAsState()
    val rawPrediction by viewModel.prediction.collectAsState()
    val prediction    = rawPrediction

    val isOnPill      by viewModel.isOnPill.collectAsState()
    val postPillState by viewModel.postPillState.collectAsState()
    val pillStopDate  by viewModel.pillStopDate.collectAsState()

    val postPillCycles = remember(cycles, pillStopDate) {
        if (pillStopDate != null) cycles.filter { !it.startDate.isBefore(pillStopDate) } else emptyList()
    }
    val discoveryCycle  = (postPillCycles.size + 1).coerceIn(1, 4)
    val isDiscoveryMode = postPillState == PostPillState.DISCOVERY
    val isLearningMode  = postPillState == PostPillState.LEARNING

    val screenScroll = rememberScrollState()
    val isDark       = LocalAppIsDark.current

    val gradTop         = if (isDark) Color(0xFF7B8FA3) else Color(0xFF8FA0B1)
    val gradMid         = if (isDark) Color(0xFF7288A0) else Color(0xFF8799B0)
    val gradBottom      = if (isDark) Color(0xFF5A7396) else Color(0xFF6E87A7)
    val onGradient      = Color.White
    val onGradientMuted = onGradient.copy(alpha = if (isDark) 0.70f else 0.55f)

    val surface = if (isDark) Color(0xFF141820) else Color(0xFFF5F7F9)
    val textCol = if (isDark) Color(0xFFF5F7FA) else Color(0xFF0F172A)
    val subCol  = if (isDark) Color(0xFFBFC6D1) else Color(0xFF1b1b1b)

    val bleedingChartColor = Color(0xFFD89046)
    val painChartColor     = if (isDark) Color(0xFF42553f) else Color(0xFFa5bda3)

    val avgPeriodLength = calculatePeriodLength(cycles).takeIf { cycles.any { c -> c.endDate != null } }
    val avgCycleLength  = calculateAvgCycleLength(cycles)

    val dailyLogs by viewModel.dailyLogs.collectAsState()

    val recentTrends = remember(cycles, dailyLogs) {
        val recentCycles = cycles.sortedBy { it.startDate }.takeLast(6)
        if (recentCycles.isEmpty()) return@remember null

        var totalPain = 0; var totalDays = 0
        val bleedingCounts = mutableMapOf<String, Int>()
        val colorCounts    = mutableMapOf<String, Int>()

        recentCycles.forEach { cycle ->
            val end = cycle.endDate ?: cycle.startDate.plusDays(4)
            var day = cycle.startDate
            while (!day.isAfter(end)) {
                val key = "${cycle.id}|$day"
                val b   = dailyLogs[key]?.bleeding   ?: cycle.bleeding
                val c   = dailyLogs[key]?.bloodColor ?: cycle.bloodColor
                val p   = dailyLogs[key]?.painLevel  ?: cycle.painLevel
                bleedingCounts[b] = (bleedingCounts[b] ?: 0) + 1
                colorCounts[c]    = (colorCounts[c]    ?: 0) + 1
                totalPain += p; totalDays++
                day = day.plusDays(1)
            }
        }
        if (totalDays == 0) return@remember null
        Triple(
            bleedingCounts.maxByOrNull { it.value }?.key ?: "Medium",
            colorCounts.maxByOrNull    { it.value }?.key ?: "Bright Red",
            round(totalPain.toFloat() / totalDays).toInt()
        )
    }
    val recentCyclesCount = minOf(cycles.size, 6)

    Box(modifier = Modifier.fillMaxSize().background(Color.Transparent)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(screenScroll)
                .padding(horizontal = 16.dp)
                .padding(bottom = 90.dp)
                .padding(top = 16.dp)
        ) {
//            if (!isOnPill && !isDiscoveryMode && !isLearningMode) {
//                RecentTrendsBanner(trends = recentTrends, cycleCount = recentCyclesCount)
//            }

            CombinedStatsCard(
                totalCycles = "${cycles.size}",
                avgPeriod   = avgPeriodLength?.let { "$it d" } ?: "0",
                avgCycle    = avgCycleLength?.let  { "$it d" } ?: "0"
            )

            Spacer(Modifier.height(14.dp))

            if (prediction != null || isDiscoveryMode) {
                UpcomingBannerEnhanced(
                    title           = if (isOnPill) "Withdrawal bleed" else "Upcoming period",
                    windowText      = prediction?.let { "${it.minPeriodStart.pretty()} – ${it.maxPeriodStart.pretty()}" } ?: "",
                    mostLikely      = prediction?.let { "Most likely: ${it.mostLikelyPeriodStart.pretty()}" } ?: "",
                    badge           = if (isOnPill) "Pill Pack" else "",
                    confidence      = prediction?.let { getCycleConfidence(it.cycleRegularity) } ?: 0f,
                    confidenceLabel = prediction?.cycleRegularity?.getDisplayName() ?: "",
                    gradTop = gradTop, gradMid = gradMid, gradBottom = gradBottom,
                    onGradient = onGradient, onGradientMuted = onGradientMuted,
                    mostLikelyDate  = prediction?.mostLikelyPeriodStart,
                    isDiscoveryMode = isDiscoveryMode,
                    isLearningMode  = isLearningMode,
                    isOnPill        = isOnPill,
                    discoveryCycle  = discoveryCycle
                )
            } else {
                UpcomingBannerEnhanced(
                    title = "Upcoming period", windowText = "Not enough data",
                    mostLikely = "Track more cycles for predictions", badge = "",
                    confidence = 0f, confidenceLabel = "No data",
                    gradTop = gradTop, gradMid = gradMid, gradBottom = gradBottom,
                    onGradient = onGradient, onGradientMuted = onGradientMuted
                )
            }

            if (prediction != null && !isOnPill && !isDiscoveryMode && !isLearningMode) {
                Spacer(Modifier.height(14.dp))
                UpcomingBannerEnhanced(
                    title           = "Fertile window",
                    windowText      = "${prediction.fertileWindow.start.pretty()} – ${prediction.fertileWindow.endInclusive.pretty()}",
                    mostLikely      = "Ovulation: ${prediction.ovulationDay.pretty()}",
                    badge           = "Confidence ${(prediction.ovulationConfidence * 100).toInt()}%",
                    confidence      = prediction.ovulationConfidence,
                    confidenceLabel = getConfidenceLabel(prediction.ovulationConfidence),
                    gradTop = gradTop, gradMid = gradMid, gradBottom = gradBottom,
                    onGradient = onGradient, onGradientMuted = onGradientMuted
                )
            }

            Spacer(Modifier.height(14.dp))

            MinimalChartCard("Bleeding intensity", surface, textCol) {
                val (bleedingPoints, bleedingDates) = remember(cycles, dailyLogs) { bleedingSeriesDailyVM(cycles, dailyLogs) }
                ScrollableLineChart(
                    points = bleedingPoints, dates = bleedingDates, lineColor = bleedingChartColor,
                    yLabels = listOf("S", "L", "M", "H"), yMax = 3f, showArea = true,
                    gridColor  = if (isDark) Color(0xFF2A2F36) else Color(0xFFEAEAEA),
                    axisColor  = if (isDark) Color(0xFF343A43) else Color(0xFFE0E0E0),
                    labelColor = subCol, surface = surface,
                    modifier = Modifier.fillMaxWidth().height(220.dp)
                )
            }

            Spacer(Modifier.height(14.dp))

            MinimalChartCard("Pain level", surface, textCol) {
                val (painPoints, painDates) = remember(cycles, dailyLogs) { painSeriesDailyVM(cycles, dailyLogs) }
                ScrollableLineChart(
                    points = painPoints, dates = painDates, lineColor = painChartColor,
                    yLabels = (0..10 step 2).map { "$it" }, yMax = 10f, showArea = true,
                    gridColor  = if (isDark) Color(0xFF2A2F36) else Color(0xFFEAEAEA),
                    axisColor  = if (isDark) Color(0xFF343A43) else Color(0xFFE0E0E0),
                    labelColor = subCol, surface = surface,
                    modifier = Modifier.fillMaxWidth().height(220.dp)
                )
            }

            Spacer(Modifier.height(14.dp))

            MinimalChartCard("Blood color", surface, textCol) {
                BloodColorPieChart(
                    data       = remember(cycles, dailyLogs) { bloodColorDistributionVM(cycles, dailyLogs) },
                    surface    = surface,
                    labelColor = subCol,
                    modifier   = Modifier.fillMaxWidth().wrapContentHeight().padding(horizontal = 8.dp)
                )
            }

            Spacer(Modifier.height(12.dp))
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.systemBars))
        }
    }
}

@Composable
private fun CombinedStatsCard(
    totalCycles: String,
    avgPeriod: String,
    avgCycle: String,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    val isDark       = LocalAppIsDark.current
    val surfaceColor = if (isDark) Color(0xFF1B1B1B).copy(alpha = 0.5f) else Color.White
    val dividerColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f)
    val valueColor   = if (isDark) Color.White else Color(0xFF0F172A)
    val titleColor   = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF1b1b1b).copy(alpha = 0.6f)

    val gentleSpring = spring<IntSize>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)

    Card(
        modifier  = modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = gentleSpring)
            .clip(RoundedCornerShape(26.dp))
            .clickable(interactionSource = interactionSource, indication = null) { isExpanded = !isExpanded },
        colors    = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape     = RoundedCornerShape(26.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier         = Modifier.fillMaxWidth().background(surfaceColor).padding(vertical = 24.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState  = isExpanded,
                transitionSpec = {
                    fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) togetherWith
                            fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) using
                            SizeTransform(clip = false)
                },
                label = "stats_expansion"
            ) { expanded ->
                if (expanded) {
                    Column(
                        modifier            = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        ExpandedStatRow("Total Cycles",   "Complete logged history",   totalCycles, valueColor, titleColor)
                        ExpandedStatRow("Average Period", "Typical bleeding duration", avgPeriod,   valueColor, titleColor)
                        ExpandedStatRow("Average Cycle",  "Start-to-start gap",        avgCycle,    valueColor, titleColor)
                    }
                } else {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem("CYCLES", totalCycles, valueColor, titleColor, Modifier.weight(1f))
                        Box(modifier = Modifier.width(1.dp).height(36.dp).background(dividerColor))
                        StatItem("PERIOD", avgPeriod,   valueColor, titleColor, Modifier.weight(1f))
                        Box(modifier = Modifier.width(1.dp).height(36.dp).background(dividerColor))
                        StatItem("CYCLE",  avgCycle,    valueColor, titleColor, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpandedStatRow(
    title: String, subtitle: String, value: String,
    valueColor: Color, titleColor: Color
) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text       = title,
                color      = valueColor,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.SemiBold,
                fontSize   = SIZE_LG
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text       = subtitle,
                color      = titleColor,
                fontFamily = BricolageGrotesque,
                fontSize   = SIZE_SM
            )
        }
        Text(
            text       = value,
            color      = valueColor,
            fontFamily = BricolageGrotesque,
            fontSize   = SIZE_XL,
            fontWeight = FontWeight.Bold,
            )
    }
}

@Composable
private fun StatItem(
    title: String, value: String,
    valueColor: Color, titleColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text       = value,
            fontFamily = BricolageGrotesque,
            fontSize   = SIZE_XL,
            fontWeight = FontWeight.Bold,
            color      = valueColor,
            maxLines   = 1,
            textAlign  = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text          = title,
            fontFamily    = BricolageGrotesque,
            fontSize      = SIZE_XXS,
            fontWeight    = FontWeight.Medium,
            letterSpacing = 1.sp,
            color         = titleColor,
            maxLines      = 1,
            textAlign     = TextAlign.Center
        )
    }
}

@Composable
private fun MinimalChartCard(
    title: String, surface: Color, titleColor: Color,
    content: @Composable () -> Unit
) {
    val isDark      = LocalAppIsDark.current
    val cardSurface = if (isDark) Color(0xFF1B1B1B).copy(alpha = 0.5f) else Color.White

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(26.dp),
        colors    = CardDefaults.cardColors(containerColor = cardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border    = null
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text       = title,
                fontFamily = BricolageGrotesque,
                fontWeight = FontWeight.SemiBold,
                fontSize   = SIZE_XL,
                color      = titleColor
            )
            Spacer(Modifier.height(24.dp))
            content()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BloodColorPieChart(
    data: List<Pair<String, Float>>,
    surface: Color, labelColor: Color,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
            Text(
                "No data yet",
                fontFamily = BricolageGrotesque,
                fontSize   = SIZE_MD,
                color      = labelColor.copy(0.5f)
            )
        }
        return
    }

    val isDark   = LocalAppIsDark.current
    val colorMap = mapOf(
        "bright red" to Color(0xFFFF8B94),
        "dark red"   to Color(0xFF4E1A1A),
        "brown"      to Color(0xFFD89046),
        "pink"       to Color(0xFFFFD3B6),
        "orange"     to Color(0xFFA8E6CF),
        "purple"     to Color(0xFF8089D2)
    )

    val total       = data.sumOf { it.second.toDouble() }.toFloat().coerceAtLeast(0.0001f)
    val legendItems = data.map { (label, value) ->
        val cleanLabel = label.lowercase().trim()
        Triple(label.replaceFirstChar { it.uppercase() }, (value / total) * 100f, colorMap[cleanLabel] ?: Color.Gray)
    }

    Column(modifier = modifier) {
        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val ringWidth  = 40.dp.toPx()
                val diameter   = minOf(size.width, size.height) - ringWidth
                val radius     = diameter / 2f
                val stroke     = Stroke(width = ringWidth, cap = StrokeCap.Round)
                var startAngle = -90f
                legendItems.forEach { (_, pct, color) ->
                    val sweep = (pct / 100f * 360f) - 4f
                    if (sweep > 0) {
                        drawArc(color = color, startAngle = startAngle, sweepAngle = sweep, useCenter = false,
                            topLeft = Offset(center.x - radius, center.y - radius),
                            size = Size(radius * 2, radius * 2), style = stroke)
                    }
                    startAngle += (sweep + 4f)
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        FlowRow(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalArrangement   = Arrangement.spacedBy(10.dp)
        ) {
            legendItems.forEach { (name, pct, color) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (isDark) color.copy(0.15f) else Color.Black.copy(0.05f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(color))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text       = "$name ${pct.toInt()}%",
                        fontFamily = BricolageGrotesque,
                        fontSize   = SIZE_XS,
                        fontWeight = FontWeight.Bold,
                        color      = if (isDark) color else Color(0xFF1B1B1B)
                    )
                }
            }
        }
    }
}

@Composable
private fun YAxisLabels(
    yLabels: List<String>, labelColor: Color, axisColor: Color,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    Canvas(modifier) {
        val bottomPadding = 40.dp.toPx()
        val topPadding    = 20.dp.toPx()
        val chartHeight   = size.height - bottomPadding - topPadding
        val chartBottom   = size.height - bottomPadding
        yLabels.forEachIndexed { index, label ->
            val y     = chartBottom - (index.toFloat() / (yLabels.size - 1)) * chartHeight
            val paint = Paint().apply {
                color     = labelColor.toArgb()
                textSize  = with(density) { SIZE_XXS.toPx() }
                textAlign = Paint.Align.RIGHT
                typeface  = Typeface.create("sans-serif", Typeface.BOLD)
            }
            val textHeight = paint.descent() - paint.ascent()
            val textOffset = (textHeight / 2) - paint.descent()
            drawContext.canvas.nativeCanvas.drawText(label, size.width - 8.dp.toPx(), y + textOffset, paint)
        }
    }
}

@Composable
fun ScrollableLineChart(
    points: List<Pair<Float, Float>>, dates: List<String>,
    lineColor: Color, yLabels: List<String>, yMax: Float,
    showArea: Boolean, gridColor: Color, axisColor: Color,
    labelColor: Color, surface: Color, modifier: Modifier = Modifier
) {
    val hScroll = rememberScrollState()
    val scope   = rememberCoroutineScope()

    if (points.isEmpty()) {
        Box(modifier.height(200.dp), contentAlignment = Alignment.Center) {
            Text(
                text       = "No data yet",
                fontFamily = BricolageGrotesque,
                fontSize   = SIZE_MD,
                color      = labelColor.copy(alpha = 0.5f)
            )
        }
        return
    }

    LaunchedEffect(points.size) {
        scope.launch { hScroll.animateScrollTo(hScroll.maxValue, animationSpec = tween(600, easing = FastOutSlowInEasing)) }
    }

    Row(modifier = modifier.height(220.dp)) {
        YAxisLabels(
            yLabels    = yLabels,
            labelColor = labelColor.copy(alpha = 0.6f),
            axisColor  = Color.Transparent,
            modifier   = Modifier.width(40.dp).fillMaxHeight()
        )
        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            val dynamicWidth = (points.size * 70.dp).coerceAtLeast(this.maxWidth)
            Box(modifier = Modifier.fillMaxSize().horizontalScroll(hScroll)) {
                LineChartContent(
                    points = points, dates = dates, lineColor = lineColor,
                    yMax = yMax, showArea = showArea,
                    gridColor  = gridColor.copy(alpha = 0.05f),
                    axisColor  = axisColor.copy(alpha = 0.1f),
                    labelColor = labelColor.copy(alpha = 0.7f),
                    surface    = surface,
                    modifier   = Modifier.width(dynamicWidth).fillMaxHeight()
                )
            }
        }
    }
}

@Composable
private fun LineChartContent(
    points: List<Pair<Float, Float>>, dates: List<String>,
    lineColor: Color, yMax: Float, showArea: Boolean,
    gridColor: Color, axisColor: Color, labelColor: Color,
    surface: Color, modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    Canvas(modifier) {
        val horizontalPadding = 32.dp.toPx()
        val bottomPadding     = 40.dp.toPx()
        val topPadding        = 20.dp.toPx()
        val chartLeft         = horizontalPadding
        val chartRight        = size.width - horizontalPadding
        val chartWidth        = chartRight - chartLeft
        val chartBottom       = size.height - bottomPadding
        val chartHeight       = chartBottom - topPadding

        val ySteps    = 4
        val gridPaint = Paint().apply {
            color       = gridColor.toArgb()
            strokeWidth = 1.dp.toPx()
            pathEffect  = DashPathEffect(floatArrayOf(10f, 10f), 0f)
        }
        repeat(ySteps + 1) { i ->
            val y = chartBottom - (i.toFloat() / ySteps) * chartHeight
            drawContext.canvas.nativeCanvas.drawLine(chartLeft, y, chartRight, y, gridPaint)
        }
        drawLine(color = axisColor, start = Offset(chartLeft, chartBottom), end = Offset(chartRight, chartBottom), strokeWidth = 1.5.dp.toPx())

        val denom    = (points.size - 1).coerceAtLeast(1).toFloat()
        val linePath = Path()
        val areaPath = Path()
        var prevXPos = 0f; var prevYPos = 0f

        points.forEachIndexed { index, (x, y) ->
            val xPos = chartLeft + (x / denom) * chartWidth
            val yPos = chartBottom - (y / yMax) * chartHeight
            if (index == 0) {
                linePath.moveTo(xPos, yPos)
                if (showArea) { areaPath.moveTo(xPos, chartBottom); areaPath.lineTo(xPos, yPos) }
            } else {
                val cpX = (prevXPos + xPos) / 2f
                linePath.cubicTo(cpX, prevYPos, cpX, yPos, xPos, yPos)
                if (showArea) areaPath.cubicTo(cpX, prevYPos, cpX, yPos, xPos, yPos)
            }
            prevXPos = xPos; prevYPos = yPos

            if (index < dates.size) {
                val paint = Paint().apply {
                    color     = labelColor.toArgb()
                    textSize  = with(density) { SIZE_XXS.toPx() }
                    textAlign = Paint.Align.CENTER
                    typeface  = Typeface.create("sans-serif", Typeface.BOLD)
                }
                drawContext.canvas.nativeCanvas.drawText(dates[index], xPos, size.height - 10.dp.toPx(), paint)
            }
        }

        if (showArea && points.isNotEmpty()) {
            val lastX = chartLeft + (points.last().first / denom) * chartWidth
            areaPath.lineTo(lastX, chartBottom); areaPath.close()
            drawPath(
                path  = areaPath,
                brush = Brush.verticalGradient(
                    listOf(lineColor.copy(alpha = 0.3f), lineColor.copy(alpha = 0.0f)),
                    startY = topPadding, endY = chartBottom
                )
            )
        }
        drawPath(path = linePath, color = lineColor, style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        points.forEach { (x, y) ->
            val xPos = chartLeft + (x / denom) * chartWidth
            val yPos = chartBottom - (y / yMax) * chartHeight
            drawCircle(lineColor.copy(alpha = 0.2f), 7.dp.toPx(), Offset(xPos, yPos))
            drawCircle(surface, 3.5.dp.toPx(), Offset(xPos, yPos))
            drawCircle(color = lineColor, radius = 3.5.dp.toPx(), center = Offset(xPos, yPos), style = Stroke(width = 2.dp.toPx()))
        }
    }
}

// ── Helpers ────────────────────────────────────────────────────────────────────
private fun bleedingSeriesDailyVM(
    cycles: List<PeriodViewModel.Cycle>,
    dailyLogs: Map<String, PeriodViewModel.DailyLog>
): Pair<List<Pair<Float, Float>>, List<String>> {
    val map       = mapOf("spotting" to 0.5f, "light" to 1f, "medium" to 2f, "heavy" to 3f)
    val formatter = DateTimeFormatter.ofPattern("MMM d")
    val points    = mutableListOf<Pair<Float, Float>>()
    val dates     = mutableListOf<String>()
    var index     = 0
    cycles.sortedBy { it.startDate }.forEach { cycle ->
        val end = cycle.endDate ?: cycle.startDate.plusDays(4); var day = cycle.startDate
        while (!day.isAfter(end)) {
            val key = "${cycle.id}|$day"
            points.add(index.toFloat() to (map[(dailyLogs[key]?.bleeding ?: cycle.bleeding).lowercase()] ?: 0f))
            dates.add(day.format(formatter)); day = day.plusDays(1); index++
        }
    }
    return points to dates
}

private fun painSeriesDailyVM(
    cycles: List<PeriodViewModel.Cycle>,
    dailyLogs: Map<String, PeriodViewModel.DailyLog>
): Pair<List<Pair<Float, Float>>, List<String>> {
    val formatter = DateTimeFormatter.ofPattern("MMM d")
    val points    = mutableListOf<Pair<Float, Float>>()
    val dates     = mutableListOf<String>()
    var index     = 0
    cycles.sortedBy { it.startDate }.forEach { cycle ->
        val end = cycle.endDate ?: cycle.startDate.plusDays(4); var day = cycle.startDate
        while (!day.isAfter(end)) {
            val key = "${cycle.id}|$day"
            points.add(index.toFloat() to (dailyLogs[key]?.painLevel ?: cycle.painLevel).toFloat())
            dates.add(day.format(formatter)); day = day.plusDays(1); index++
        }
    }
    return points to dates
}

private fun bloodColorDistributionVM(
    cycles: List<PeriodViewModel.Cycle>,
    dailyLogs: Map<String, PeriodViewModel.DailyLog>
): List<Pair<String, Float>> {
    val counts = mutableMapOf<String, Int>()
    cycles.forEach { cycle ->
        val end = cycle.endDate ?: cycle.startDate.plusDays(4); var day = cycle.startDate
        while (!day.isAfter(end)) {
            val key = "${cycle.id}|$day"
            val color = (dailyLogs[key]?.bloodColor ?: cycle.bloodColor).lowercase()
            counts[color] = (counts[color] ?: 0) + 1; day = day.plusDays(1)
        }
    }
    val total = counts.values.sum()
    if (total == 0) return emptyList()
    return counts.entries.sortedByDescending { it.value }.map { it.key to (it.value.toFloat() / total) }
}

private fun calculateAvgCycleLength(cycles: List<PeriodViewModel.Cycle>): Int? {
    val lengths = cycles.sortedBy { it.startDate }.zipWithNext { a, b ->
        ChronoUnit.DAYS.between(a.startDate, b.startDate).toInt()
    }
    return if (lengths.isNotEmpty()) lengths.average().toInt() else null
}

@Composable
fun RecentTrendsBanner(trends: Triple<String, String, Int>?, cycleCount: Int) {
    if (trends == null || cycleCount == 0) return

    val isDark      = LocalAppIsDark.current
    val textPrimary = if (isDark) Color.White else Color.Black
    val textSub     = if (isDark) Color.White else Color.Black
    val dividerColor = textPrimary.copy(alpha = 0.15f)

    // Explicitly define the background to match your app's theme
    // This prevents the "halo" effect caused by blending against transparency
    val appBackgroundColor = if (isDark) Color.Black else Color.White
    val cardShape = RoundedCornerShape(26.dp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = appBackgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        // The "Seal": A hairline border that swallows sub-pixel rendering artifacts
        border = BorderStroke(0.5.dp, appBackgroundColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    clip = true
                    shape = cardShape
                }
        ) {
            Image(
                painter = painterResource(id = R.drawable.recent),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )

            // Gradient Overlay
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                if (isDark) Color.Black.copy(alpha = 0.90f) else Color.White.copy(alpha = 1f),
                                if (isDark) Color.Black.copy(alpha = 0.80f) else Color.White.copy(alpha = 0.6f),
                                if (isDark) Color.Black.copy(alpha = 0.40f) else Color.White.copy(alpha = 0.0f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(0.85f)
            ) {
                Text(
                    text = "Recent Trends",
                    fontFamily = BricolageGrotesque,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = SIZE_XL,
                    color = textPrimary
                )

                Spacer(Modifier.height(12.dp))

                val annotatedSummary = buildAnnotatedString {
                    append("Over your last $cycleCount cycle${if (cycleCount > 1) "s" else ""}, your typical flow is ")
                    pushStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, color = textPrimary))
                    append("${trends.first.lowercase()} (${trends.second.lowercase()})")
                    pop()
                    append(", with an average pain level of ")
                    pushStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, color = textPrimary))
                    append("${trends.third}/10")
                    pop()
                    append(".")
                }

                Text(
                    text = annotatedSummary,
                    fontFamily = BricolageGrotesque,
                    fontSize = SIZE_MD,
                    color = textSub,
                    lineHeight = 20.sp
                )
            }
        }
    }
}