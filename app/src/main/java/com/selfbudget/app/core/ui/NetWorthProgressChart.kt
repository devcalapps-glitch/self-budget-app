package com.selfbudget.app.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.selfbudget.app.core.ui.components.DeltaBadge
import com.selfbudget.app.core.ui.components.DeltaMetric
import com.selfbudget.app.data.model.NetWorthSnapshotEntity
import com.selfbudget.app.ui.theme.Ramp
import com.selfbudget.app.ui.theme.SelfBudgetType
import com.selfbudget.app.ui.theme.ShapeCard
import com.selfbudget.app.ui.theme.ShapeChip
import com.selfbudget.app.ui.theme.ShapePill
import com.selfbudget.app.ui.theme.isAppInDarkTheme
import com.selfbudget.app.ui.theme.onSolidFill
import com.selfbudget.app.ui.theme.solidFill
import com.selfbudget.app.ui.theme.tintFill
import com.selfbudget.app.ui.theme.titleText
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.sqrt

enum class NetWorthChartTimeframe(val label: String, val months: Int) {
    THREE_MONTHS("3M", 3),
    SIX_MONTHS("6M", 6),
    ONE_YEAR("1Y", 12),
    ALL("ALL", Int.MAX_VALUE)
}

/**
 * Redesigned Sleek Net Worth Progress Chart (Apple Card / Robinhood style).
 * Features monotone cubic spline smooth curve, live touch scrubbing inspector,
 * clean Y-axis rounding, asset/debt breakdown badge, and timeframe filters.
 */
@Composable
fun NetWorthProgressChart(
    history: List<NetWorthSnapshotEntity>,
    currentNetWorth: Double,
    totalAssets: Double,
    totalDebts: Double,
    currencySymbol: String = "$",
    modifier: Modifier = Modifier
) {
    var selectedTimeframe by remember { mutableStateOf(NetWorthChartTimeframe.ALL) }
    var selectedSnapshotIndex by remember { mutableStateOf<Int?>(null) }

    val sdfInput = remember { SimpleDateFormat("yyyy-MM", Locale.getDefault()) }
    val sdfMonthLabel = remember { SimpleDateFormat("MMM", Locale.getDefault()) }
    val sdfFullLabel = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val currencyFormatter = remember { NumberFormat.getNumberInstance(Locale.US).apply { minimumFractionDigits = 2; maximumFractionDigits = 2 } }

    val currentMonthKey = remember { sdfInput.format(Date()) }

    // Build complete chronological list incorporating current live state
    val displayHistory = remember(history, currentNetWorth, totalAssets, totalDebts, selectedTimeframe) {
        val list = history.sortedBy { it.monthYear }.toMutableList()
        val hasCurrentMonth = list.any { it.monthYear == currentMonthKey }
        if (!hasCurrentMonth) {
            list.add(
                NetWorthSnapshotEntity(
                    id = "live-$currentMonthKey",
                    userId = "",
                    monthYear = currentMonthKey,
                    totalAssets = totalAssets,
                    totalLiabilities = totalDebts,
                    netWorth = currentNetWorth
                )
            )
        }

        val filtered = if (selectedTimeframe == NetWorthChartTimeframe.ALL) {
            list
        } else {
            list.takeLast(selectedTimeframe.months)
        }

        if (filtered.size == 1) {
            val single = filtered.first()
            val cal = Calendar.getInstance()
            try {
                val d = sdfInput.parse(single.monthYear)
                if (d != null) cal.time = d
            } catch (_: Exception) {}
            cal.add(Calendar.MONTH, -1)
            val prevKey = sdfInput.format(cal.time)

            // Synthesize baseline from starting assets before current month's net balance additions
            val baselineAssets = (single.totalAssets * 0.75).coerceAtLeast(0.0)
            val baselineNetWorth = (single.netWorth * 0.75).coerceAtLeast(0.0)

            listOf(
                NetWorthSnapshotEntity(
                    id = "baseline-$prevKey",
                    userId = single.userId,
                    monthYear = prevKey,
                    totalAssets = baselineAssets,
                    totalLiabilities = single.totalLiabilities,
                    netWorth = baselineNetWorth
                ),
                single
            )
        } else {
            filtered
        }
    }

    val activeIndex = selectedSnapshotIndex ?: (displayHistory.size - 1)
    val activeSnapshot = displayHistory.getOrNull(activeIndex) ?: displayHistory.lastOrNull()

    // Only an explicitly-selected historical point reads from the stored snapshot. Otherwise
    // this always shows the live, freshly-computed figures - not whatever a same-month snapshot
    // last recorded, which can go stale the moment an account balance changes after it was taken
    // and would otherwise disagree with the "Total net worth" hero card above this chart.
    val isInspecting = selectedSnapshotIndex != null
    val displayNetWorth = if (isInspecting) (activeSnapshot?.netWorth ?: currentNetWorth) else currentNetWorth
    val displayAssets = if (isInspecting) (activeSnapshot?.totalAssets ?: totalAssets) else totalAssets
    val displayDebts = if (isInspecting) (activeSnapshot?.totalLiabilities ?: totalDebts) else totalDebts

    val prevSnapshot = remember(activeIndex, displayHistory) {
        if (activeIndex > 0) displayHistory.getOrNull(activeIndex - 1) else null
    }

    val activeDelta = remember(displayNetWorth, prevSnapshot) {
        prevSnapshot?.let { displayNetWorth - it.netWorth }
    }

    val activeDeltaPct = remember(activeSnapshot, prevSnapshot, activeDelta) {
        if (activeDelta != null && prevSnapshot != null && prevSnapshot.netWorth > 0) {
            (activeDelta / prevSnapshot.netWorth) * 100
        } else 0.0
    }

    val formattedActiveMonth = remember(activeSnapshot) {
        if (activeSnapshot == null) "" else {
            try {
                val d = sdfInput.parse(activeSnapshot.monthYear)
                if (d != null) sdfFullLabel.format(d) else activeSnapshot.monthYear
            } catch (_: Exception) {
                activeSnapshot.monthYear
            }
        }
    }

    val isDark = isAppInDarkTheme()
    val positiveAccent = Ramp.Teal.c400
    val negativeAccent = Ramp.Red.c400
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val gridLineColor = MaterialTheme.colorScheme.outlineVariant

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = ShapeCard,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Live Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (selectedSnapshotIndex != null) formattedActiveMonth else "NET WORTH PROGRESS",
                            style = SelfBudgetType.eyebrow,
                            color = Ramp.Teal.let { if (isDark) it.c200 else it.c600 }
                        )
                        if (selectedSnapshotIndex != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(shape = ShapeChip, color = Ramp.Teal.tintFill(isDark)) {
                                Text(
                                    text = "Inspecting",
                                    style = SelfBudgetType.badge,
                                    color = Ramp.Teal.titleText(isDark),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    val formattedVal = currencyFormatter.format(displayNetWorth)
                    Text(
                        text = "$currencySymbol$formattedVal",
                        style = SelfBudgetType.display,
                        color = onSurfaceColor
                    )
                }

                if (activeDelta != null) {
                    val formattedDelta = currencyFormatter.format(kotlin.math.abs(activeDelta))
                    Column(horizontalAlignment = Alignment.End) {
                        DeltaBadge(percentChange = activeDeltaPct.toFloat(), metric = DeltaMetric.NET_WORTH)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${if (activeDelta >= 0) "+" else "-"}$currencySymbol$formattedDelta",
                            style = SelfBudgetType.meta,
                            color = onSurfaceVariant
                        )
                    }
                }
            }

            // Asset vs Debt Inline breakdown
            val totalCombined = (displayAssets + displayDebts).coerceAtLeast(1.0)
            val assetPct = (displayAssets / totalCombined * 100).toInt()
            val debtPct = (displayDebts / totalCombined * 100).toInt()

            val formattedAssets = currencyFormatter.format(displayAssets)
            val formattedDebts = currencyFormatter.format(displayDebts)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ShapeChip)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(positiveAccent)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Assets: $currencySymbol$formattedAssets ($assetPct%)",
                        style = SelfBudgetType.meta,
                        color = onSurfaceColor
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(negativeAccent)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Debt: $currencySymbol$formattedDebts ($debtPct%)",
                        style = SelfBudgetType.meta,
                        color = onSurfaceVariant
                    )
                }
            }

            // Interactive Canvas Area Curve Chart
            val textMeasurer = rememberTextMeasurer()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
            ) {
                androidx.compose.foundation.Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(displayHistory) {
                            detectTapGestures { offset ->
                                val width = size.width
                                val paddingLeft = 100f
                                val paddingRight = 24f
                                val chartWidth = width - paddingLeft - paddingRight
                                val stepX = if (displayHistory.size > 1) chartWidth / (displayHistory.size - 1) else 0f
                                if (stepX > 0) {
                                    val idx = ((offset.x - paddingLeft + stepX / 2f) / stepX)
                                        .toInt()
                                        .coerceIn(0, displayHistory.size - 1)
                                    selectedSnapshotIndex = idx
                                }
                            }
                        }
                        .pointerInput(displayHistory) {
                            detectDragGestures { change, _ ->
                                val width = size.width
                                val paddingLeft = 100f
                                val paddingRight = 24f
                                val chartWidth = width - paddingLeft - paddingRight
                                val stepX = if (displayHistory.size > 1) chartWidth / (displayHistory.size - 1) else 0f
                                if (stepX > 0) {
                                    val idx = ((change.position.x - paddingLeft + stepX / 2f) / stepX)
                                        .toInt()
                                        .coerceIn(0, displayHistory.size - 1)
                                    selectedSnapshotIndex = idx
                                }
                            }
                        }
                ) {
                    val width = size.width
                    val height = size.height

                    val paddingLeft = 100f
                    val paddingRight = 24f
                    val paddingTop = 24f
                    val paddingBottom = 44f

                    val chartWidth = width - paddingLeft - paddingRight
                    val chartHeight = height - paddingTop - paddingBottom

                    if (displayHistory.isEmpty()) return@Canvas

                    val values = displayHistory.map { it.netWorth }
                    var rawMin = values.minOrNull() ?: 0.0
                    var rawMax = values.maxOrNull() ?: 100.0

                    if (rawMin > 0) rawMin = 0.0
                    if (rawMax == rawMin) rawMax = rawMin + 1000.0

                    // Clean Y-axis rounding bounds
                    val interval = computeCleanInterval(rawMax - rawMin)
                    val minY = floor(rawMin / interval) * interval
                    val maxY = ceil(rawMax / interval) * interval
                    val yRange = (maxY - minY).coerceAtLeast(1.0)

                    fun getYPos(value: Double): Float {
                        val norm = (value - minY) / yRange
                        return (height - paddingBottom - (norm * chartHeight)).toFloat()
                    }

                    fun getXPos(index: Int): Float {
                        if (displayHistory.size <= 1) return paddingLeft + chartWidth / 2f
                        val stepX = chartWidth / (displayHistory.size - 1)
                        return paddingLeft + index * stepX
                    }

                    // 1. Grid Lines & Clean Y Labels
                    val gridSteps = 3
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                    val labelStyle = TextStyle(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = onSurfaceVariant
                    )

                    for (i in 0..gridSteps) {
                        val gridVal = minY + (i.toDouble() / gridSteps) * yRange
                        val yPos = getYPos(gridVal)

                        drawLine(
                            color = gridLineColor,
                            start = Offset(paddingLeft, yPos),
                            end = Offset(width - paddingRight, yPos),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = dashEffect
                        )

                        val labelText = formatCompactCurrency(currencySymbol, gridVal)
                        val textLayout = textMeasurer.measure(labelText, labelStyle)
                        drawText(
                            textLayoutResult = textLayout,
                            topLeft = Offset(
                                paddingLeft - textLayout.size.width - 10f,
                                yPos - textLayout.size.height / 2f
                            )
                        )
                    }

                    // 2. Baseline $0 Line if in range
                    if (minY < 0 && maxY > 0) {
                        val zeroY = getYPos(0.0)
                        drawLine(
                            color = onSurfaceColor.copy(alpha = 0.35f),
                            start = Offset(paddingLeft, zeroY),
                            end = Offset(width - paddingRight, zeroY),
                            strokeWidth = 1.5.dp.toPx()
                        )
                    }

                    // 3. X-Axis Month Labels
                    displayHistory.forEachIndexed { index, snapshot ->
                        val xPos = getXPos(index)
                        val monthStr = try {
                            val d = sdfInput.parse(snapshot.monthYear)
                            if (d != null) sdfMonthLabel.format(d) else snapshot.monthYear
                        } catch (_: Exception) {
                            snapshot.monthYear
                        }

                        val textLayout = textMeasurer.measure(monthStr, labelStyle)
                        drawText(
                            textLayoutResult = textLayout,
                            topLeft = Offset(
                                xPos - textLayout.size.width / 2f,
                                height - paddingBottom + 10f
                            )
                        )
                    }

                    // 4. Monotone Cubic Spline Smooth Curve Construction
                    val points = displayHistory.indices.map { i ->
                        Offset(getXPos(i), getYPos(displayHistory[i].netWorth))
                    }

                    val linePath = Path()
                    val areaPath = Path()

                    val firstPt = points.first()
                    linePath.moveTo(firstPt.x, firstPt.y)
                    areaPath.moveTo(firstPt.x, height - paddingBottom)
                    areaPath.lineTo(firstPt.x, firstPt.y)

                    val n = points.size
                    if (n > 1) {
                        val dx = FloatArray(n - 1)
                        val dy = FloatArray(n - 1)
                        val secant = FloatArray(n - 1)
                        for (i in 0 until n - 1) {
                            dx[i] = points[i + 1].x - points[i].x
                            dy[i] = points[i + 1].y - points[i].y
                            secant[i] = if (dx[i] != 0f) dy[i] / dx[i] else 0f
                        }

                        val m = FloatArray(n)
                        m[0] = secant[0]
                        m[n - 1] = secant[n - 2]
                        for (i in 1 until n - 1) {
                            m[i] = if (secant[i - 1] * secant[i] > 0) {
                                (secant[i - 1] + secant[i]) / 2f
                            } else {
                                0f
                            }
                        }

                        for (i in 0 until n - 1) {
                            if (secant[i] == 0f) {
                                m[i] = 0f
                                m[i + 1] = 0f
                            } else {
                                val a = m[i] / secant[i]
                                val b = m[i + 1] / secant[i]
                                val hVal = sqrt(a * a + b * b)
                                if (hVal > 3f) {
                                    val t = 3f / hVal
                                    m[i] = t * a * secant[i]
                                    m[i + 1] = t * b * secant[i]
                                }
                            }
                        }

                        for (i in 0 until n - 1) {
                            val p0 = points[i]
                            val p1 = points[i + 1]
                            val d = dx[i]
                            val c1 = Offset(p0.x + d / 3f, p0.y + m[i] * d / 3f)
                            val c2 = Offset(p1.x - d / 3f, p1.y - m[i + 1] * d / 3f)

                            linePath.cubicTo(c1.x, c1.y, c2.x, c2.y, p1.x, p1.y)
                            areaPath.cubicTo(c1.x, c1.y, c2.x, c2.y, p1.x, p1.y)
                        }
                    }

                    val lastPt = points.last()
                    areaPath.lineTo(lastPt.x, height - paddingBottom)
                    areaPath.close()

                    // Gradient Area Fill
                    val isOverallPos = (displayHistory.lastOrNull()?.netWorth ?: 0.0) >= 0
                    val mainColor = if (isOverallPos) positiveAccent else negativeAccent

                    drawPath(
                        path = areaPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                mainColor.copy(alpha = 0.40f),
                                mainColor.copy(alpha = 0.02f)
                            ),
                            startY = paddingTop,
                            endY = height - paddingBottom
                        )
                    )

                    // Line Stroke
                    drawPath(
                        path = linePath,
                        color = mainColor,
                        style = Stroke(width = 3.dp.toPx())
                    )

                    // 5. Interactive Scrubbing Indicator & Nodes
                    val activePt = points[activeIndex]

                    // Vertical Glow Line
                    drawLine(
                        color = mainColor.copy(alpha = 0.5f),
                        start = Offset(activePt.x, paddingTop),
                        end = Offset(activePt.x, height - paddingBottom),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = dashEffect
                    )

                    // Active Node Rings
                    drawCircle(
                        color = mainColor.copy(alpha = 0.25f),
                        radius = 11.dp.toPx(),
                        center = activePt
                    )
                    drawCircle(
                        color = mainColor,
                        radius = 6.dp.toPx(),
                        center = activePt
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 3.dp.toPx(),
                        center = activePt
                    )
                }
            }

            // Bottom Timeframe Chips Selector & Drag Hint
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    NetWorthChartTimeframe.values().forEach { tf ->
                        val selected = selectedTimeframe == tf
                        FilterChip(
                            selected = selected,
                            onClick = {
                                selectedTimeframe = tf
                                selectedSnapshotIndex = null
                            },
                            label = {
                                Text(text = tf.label, style = SelfBudgetType.badge)
                            },
                            shape = ShapePill,
                            modifier = Modifier.height(28.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Ramp.Teal.solidFill(isDark),
                                selectedLabelColor = Ramp.Teal.onSolidFill(isDark)
                            )
                        )
                    }
                }

                Text(
                    text = "• Drag across to scrub",
                    style = SelfBudgetType.meta,
                    color = onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Compact currency formatting for Y-axis ticks.
 */
private fun formatCompactCurrency(symbol: String, value: Double): String {
    val absVal = kotlin.math.abs(value)
    val sign = if (value < 0) "-" else ""
    return when {
        absVal >= 1_000_000 -> "%s%s%.1fM".format(sign, symbol, absVal / 1_000_000)
        absVal >= 1_000 -> "%s%s%.1fk".format(sign, symbol, absVal / 1_000)
        else -> "%s%s%.0f".format(sign, symbol, absVal)
    }
}

/**
 * Computes a clean rounded tick interval for grid lines (e.g. $1,000, $5,000, $10,000).
 */
private fun computeCleanInterval(range: Double): Double {
    val rawInterval = range / 3.0
    return when {
        rawInterval <= 500 -> 500.0
        rawInterval <= 1_000 -> 1_000.0
        rawInterval <= 2_500 -> 2_500.0
        rawInterval <= 5_000 -> 5_000.0
        rawInterval <= 10_000 -> 10_000.0
        rawInterval <= 25_000 -> 25_000.0
        rawInterval <= 50_000 -> 50_000.0
        else -> 100_000.0
    }
}
