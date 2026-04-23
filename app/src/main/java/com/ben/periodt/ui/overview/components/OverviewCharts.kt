package com.ben.periodt.ui.overview.components

import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.ben.periodt.ui.theme.BricolageGrotesque
import com.ben.periodt.ui.theme.LocalAppIsDark
import kotlinx.coroutines.launch

private val SIZE_XXS = 11.sp
private val SIZE_XS  = 12.sp
private val SIZE_MD  = 14.sp
private val SIZE_XL  = 20.sp

@Composable
fun MinimalChartCard(
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
fun BloodColorPieChart(
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
            val dynamicWidth = (70.dp * points.size).coerceAtLeast(this.maxWidth)
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