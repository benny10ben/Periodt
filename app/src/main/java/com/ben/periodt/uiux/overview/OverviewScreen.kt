package com.ben.periodt.uiux.overview

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
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

    // Gradient palette for Stat Cards
    val gradTop = if (isDark) Color(0xFF7B8FA3) else Color(0xFF8FA0B1)
    val gradMid = if (isDark) Color(0xFF7288A0) else Color(0xFF8799B0)
    val gradBottom = if (isDark) Color(0xFF5A7396) else Color(0xFF6E87A7)
    val onGradient = Color.White
    val onGradientMuted = onGradient.copy(alpha = if (isDark) 0.70f else 0.55f)

    // Surface and Text Colors
    val surface = if (isDark) Color(0xFF141820) else Color(0xFFF5F7F9)
    val textCol = if (isDark) Color(0xFFF5F7FA) else Color(0xFF0F172A)
    val subCol  = if (isDark) Color(0xFFBFC6D1) else Color(0xFF64748B)

    // Specific Chart Colors
    val bleedingChartColor = Color(0xFFD89046) // Darker Orange for Blood
    val painChartColor = Color(0xFF2A3825)     // Deep Green for Pain

    val prediction = remember(cycles) { predictCycle(cycles) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(screenScroll)
                .padding(horizontal = 16.dp)
                .padding(bottom = 90.dp) // Space for Bottom Bar
                .padding(top = 16.dp)
        ) {
            // --- TOP STATS ROW ---
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

            // --- UPCOMING PERIOD BANNER ---
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

            // --- FERTILE WINDOW BANNER ---
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

            // --- BLEEDING INTENSITY CHART ---
            MinimalChartCard(
                title = "Bleeding intensity",
                surface = surface,
                titleColor = textCol
            ) {
                ScrollableLineChart(
                    points = bleedingSeriesVM(cycles),
                    dates = getDateLabels(cycles),
                    lineColor = bleedingChartColor, // Darker Orange
                    yLabels = listOf("S", "L", "M", "H"),
                    yMax = 3f,
                    showArea = true,
                    gridColor = if (isDark) Color(0xFF2A2F36) else Color(0xFFEAEAEA),
                    axisColor = if (isDark) Color(0xFF343A43) else Color(0xFFE0E0E0),
                    labelColor = subCol,
                    surface = surface,
                    modifier = Modifier.fillMaxWidth().height(220.dp)
                )
            }

            Spacer(Modifier.height(14.dp))

            // --- PAIN LEVEL CHART ---
            MinimalChartCard(
                title = "Pain level",
                surface = surface,
                titleColor = textCol
            ) {
                ScrollableLineChart(
                    points = painSeriesVM(cycles),
                    dates = getDateLabels(cycles),
                    lineColor = painChartColor, // Deep Green
                    yLabels = (0..10 step 2).map { "$it" },
                    yMax = 10f,
                    showArea = true,
                    gridColor = if (isDark) Color(0xFF2A2F36) else Color(0xFFEAEAEA),
                    axisColor = if (isDark) Color(0xFF343A43) else Color(0xFFE0E0E0),
                    labelColor = subCol,
                    surface = surface,
                    modifier = Modifier.fillMaxWidth().height(220.dp)
                )
            }

            Spacer(Modifier.height(14.dp))

            // --- BLOOD COLOR PIE CHART ---
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
                        .wrapContentHeight()
                        .padding(horizontal = 8.dp)
                )
            }

            Spacer(Modifier.height(12.dp))
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.systemBars))
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
    val surfaceColor = if (isDark) Color(0xFF1B1B1B) else Color.White

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
    val cardSurface = if (isDark) Color(0xFF1B1B1B) else surface

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
        Box(Modifier.height(150.dp), contentAlignment = Alignment.Center) {
            Text("No data", fontFamily = BricolageGrotesque, color = labelColor.copy(0.5f))
        }
        return
    }

    val isDark = isSystemInDarkTheme()
    val colorMap = mapOf(
        "bright red" to Color(0xFFFF8B94),
        "dark red"   to Color(0xFF4E1A1A),
        "brown"      to Color(0xFFD89046),
        "pink"       to Color(0xFFFFD3B6),
        "orange"     to Color(0xFFA8E6CF),
        "purple"     to Color(0xFF8089D2)
    )

    val total = data.sumOf { it.second.toDouble() }.toFloat().coerceAtLeast(0.0001f)

    // Normalize keys to ensure mapping works
    val legendItems = data.map { (label, value) ->
        val cleanLabel = label.lowercase().trim()
        val pct = (value / total) * 100f
        Triple(label.replaceFirstChar { it.uppercase() }, pct, colorMap[cleanLabel] ?: Color.Gray)
    }

    Column(modifier = modifier) {
        // 1. Chart Area - Fixed height here so legend can sit below
        Box(
            modifier = Modifier.fillMaxWidth().height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val ringWidth = 40.dp.toPx()
                val diameter = minOf(size.width, size.height) - ringWidth
                val radius = diameter / 2f
                val stroke = Stroke(width = ringWidth, cap = StrokeCap.Round)
                var startAngle = -90f

                legendItems.forEach { (_, pct, color) ->
                    val sweep = (pct / 100f * 360f) - 4f
                    if (sweep > 0) {
                        drawArc(
                            color = color,
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = Offset(center.x - radius, center.y - radius),
                            size = Size(radius * 2, radius * 2),
                            style = stroke
                        )
                    }
                    startAngle += (sweep + 4f)
                }
            }
        }

        Spacer(Modifier.height(28.dp)) // Breathing room for labels

        // 2. Legend Area
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                        text = "$name ${pct.toInt()}%",
                        fontFamily = BricolageGrotesque,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) color else Color(0xFF1B1B1B)
                    )
                }
            }
        }
    }
}

private fun bloodColorDistributionVM(cycles: List<PeriodViewModel.Cycle>): List<Pair<String, Float>> {
    val counts = cycles.groupingBy { it.bloodColor.lowercase() }.eachCount()
    val total = counts.values.sum()
    if (total == 0) return emptyList()
    return counts.entries.sortedByDescending { it.value }
        .map { it.key to (it.value.toFloat() / total.toFloat()) }
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
fun ScrollableLineChart(
    points: List<Pair<Float, Float>>,
    dates: List<String>,
    lineColor: Color, // 0xFFB45309 for Blood | 0xFF2A3825 for Pain
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
            Text(
                text = "No data yet",
                fontFamily = BricolageGrotesque,
                style = MaterialTheme.typography.bodyMedium,
                color = labelColor.copy(alpha = 0.5f)
            )
        }
        return
    }

    LaunchedEffect(points.size) {
        scope.launch {
            hScroll.animateScrollTo(hScroll.maxValue, animationSpec = tween(600, easing = FastOutSlowInEasing))
        }
    }

    Row(modifier = modifier.height(220.dp)) {
        YAxisLabels(
            yLabels = yLabels,
            labelColor = labelColor.copy(alpha = 0.6f),
            axisColor = Color.Transparent,
            modifier = Modifier.width(40.dp).fillMaxHeight()
        )

        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            val dynamicWidth = (points.size * 70.dp).coerceAtLeast(this.maxWidth)

            Box(modifier = Modifier.fillMaxSize().horizontalScroll(hScroll)) {
                LineChartContent(
                    points = points,
                    dates = dates,
                    lineColor = lineColor,
                    yMax = yMax,
                    showArea = showArea,
                    gridColor = gridColor.copy(alpha = 0.05f),
                    axisColor = axisColor.copy(alpha = 0.1f),
                    labelColor = labelColor.copy(alpha = 0.7f),
                    surface = surface,
                    modifier = Modifier.width(dynamicWidth).fillMaxHeight()
                )
            }
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

        // 1. Grid
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

        // 2. Axis
        drawLine(color = axisColor, start = Offset(chartLeft, chartBottom), end = Offset(chartRight, chartBottom), strokeWidth = 1.5.dp.toPx())

        val denom = (points.size - 1).coerceAtLeast(1).toFloat()

        // 3. Paths
        val linePath = Path()
        val areaPath = Path()
        var prevXPos = 0f
        var prevYPos = 0f

        points.forEachIndexed { index, (x, y) ->
            val xPos = chartLeft + (x / denom) * chartWidth
            val yPos = chartBottom - (y / yMax) * chartHeight

            if (index == 0) {
                linePath.moveTo(xPos, yPos)
                if (showArea) { areaPath.moveTo(xPos, chartBottom); areaPath.lineTo(xPos, yPos) }
            } else {
                val controlPointX = (prevXPos + xPos) / 2f
                linePath.cubicTo(controlPointX, prevYPos, controlPointX, yPos, xPos, yPos)
                if (showArea) areaPath.cubicTo(controlPointX, prevYPos, controlPointX, yPos, xPos, yPos)
            }
            prevXPos = xPos
            prevYPos = yPos

            // Labels
            if (index < dates.size) {
                val paint = android.graphics.Paint().apply {
                    color = labelColor.toArgb()
                    textSize = with(density) { 10.sp.toPx() }
                    textAlign = android.graphics.Paint.Align.CENTER
                    typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
                }
                drawContext.canvas.nativeCanvas.drawText(dates[index], xPos, size.height - 10.dp.toPx(), paint)
            }
        }

        // 5. Area Fill
        if (showArea && points.isNotEmpty()) {
            val lastX = chartLeft + (points.last().first / denom) * chartWidth
            areaPath.lineTo(lastX, chartBottom)
            areaPath.close()
            drawPath(path = areaPath, brush = Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.3f), lineColor.copy(alpha = 0.0f)), startY = topPadding, endY = chartBottom))
        }

        // 6. Main Line
        drawPath(path = linePath, color = lineColor, style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

        // 7. Indicators
        points.forEach { (x, y) ->
            val xPos = chartLeft + (x / denom) * chartWidth
            val yPos = chartBottom - (y / yMax) * chartHeight
            drawCircle(lineColor.copy(alpha = 0.2f), 7.dp.toPx(), Offset(xPos, yPos))
            drawCircle(surface, 3.5.dp.toPx(), Offset(xPos, yPos))
            drawCircle(color = lineColor, radius = 3.5.dp.toPx(), center = Offset(xPos, yPos), style = Stroke(width = 2.dp.toPx()))
        }
    }
}

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
        c.startDate?.let { index.toFloat() to (map[c.bleeding.lowercase()] ?: 0f) }
    }
}

private fun painSeriesVM(cycles: List<PeriodViewModel.Cycle>): List<Pair<Float, Float>> {
    val sorted = cyclesSorted(cycles)
    return sorted.mapIndexedNotNull { index, c ->
        c.startDate?.let { index.toFloat() to c.painLevel.toFloat() }
    }
}