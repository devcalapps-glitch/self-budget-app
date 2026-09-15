package com.selfbudget.app.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.selfbudget.app.core.ui.components.NeutralBadge
import com.selfbudget.app.core.ui.components.RampIconTile
import com.selfbudget.app.data.model.CategoryEntity
import com.selfbudget.app.data.model.TransactionEntity
import com.selfbudget.app.data.model.TransactionType
import com.selfbudget.app.feature.analytics.AnalyticsTimeframe
import com.selfbudget.app.ui.theme.Ramp
import com.selfbudget.app.ui.theme.SelfBudgetType
import com.selfbudget.app.ui.theme.ShapeCard
import com.selfbudget.app.ui.theme.ShapeChip
import com.selfbudget.app.ui.theme.ShapePill
import com.selfbudget.app.ui.theme.getProgressBarColor
import com.selfbudget.app.ui.theme.isAppInDarkTheme
import com.selfbudget.app.ui.theme.secondaryText
import com.selfbudget.app.ui.theme.sectionRamp
import com.selfbudget.app.ui.theme.titleText
import com.selfbudget.app.ui.theme.tintFill
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

data class CategoryDetailItem(
    val category: CategoryEntity,
    val totalAmount: Double,
    val percentage: Float,
    val transactionCount: Int,
    val averageAmount: Double
)

@Composable
fun CategoryAnalyticsDetailModal(
    title: String,
    subtitle: String = "",
    transactionType: TransactionType,
    timeframe: AnalyticsTimeframe,
    periodLabel: String,
    allTransactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    currencySymbol: String = "$",
    onDismiss: () -> Unit
) {
    val sdfMonth = remember { SimpleDateFormat("yyyy-MM", Locale.getDefault()) }
    val sdfYear = remember { SimpleDateFormat("yyyy", Locale.getDefault()) }
    val sdfMonthShort = remember { SimpleDateFormat("MMM", Locale.getDefault()) }

    // Filter relevant transactions for target type (and matching category type)
    val typeFilteredTxs = remember(allTransactions, categories, transactionType) {
        val catMap = categories.associateBy { it.id }
        allTransactions.filter { tx ->
            tx.type == transactionType && (catMap[tx.categoryId]?.type == transactionType || catMap[tx.categoryId] == null)
        }
    }

    // Filter by timeframe
    val activeTxs = remember(typeFilteredTxs, timeframe, periodLabel) {
        if (timeframe == AnalyticsTimeframe.MONTHLY) {
            typeFilteredTxs.filter { sdfMonth.format(Date(it.timestamp)) == periodLabel }
        } else {
            typeFilteredTxs.filter { sdfYear.format(Date(it.timestamp)) == periodLabel }
        }
    }

    val totalAmount = remember(activeTxs) { activeTxs.sumOf { it.amount } }

    val categoryDetails = remember(activeTxs, categories, totalAmount) {
        val catMap = categories.associateBy { it.id }
        activeTxs
            .groupBy { it.categoryId }
            .map { (catId, txs) ->
                val sum = txs.sumOf { it.amount }
                val cat = catMap[catId] ?: CategoryEntity(
                    catId, "Uncategorized", "MoreHoriz", "#64748B", transactionType
                )
                val pct = if (totalAmount > 0) (sum / totalAmount).toFloat() else 0f
                val avg = if (txs.isNotEmpty()) sum / txs.size else 0.0
                CategoryDetailItem(cat, sum, pct, txs.size, avg)
            }
            .sortedByDescending { it.totalAmount }
    }

    // Monthly breakdown data for annual view (Jan - Dec totals)
    val monthlyTotals = remember(typeFilteredTxs, timeframe, periodLabel) {
        val result = FloatArray(12) { 0f }
        if (timeframe == AnalyticsTimeframe.ANNUAL) {
            val cal = Calendar.getInstance()
            typeFilteredTxs.forEach { tx ->
                val d = Date(tx.timestamp)
                if (sdfYear.format(d) == periodLabel) {
                    cal.time = d
                    val monthIdx = cal.get(Calendar.MONTH) // 0-11
                    if (monthIdx in 0..11) {
                        result[monthIdx] += tx.amount.toFloat()
                    }
                }
            }
        }
        result
    }
    val maxMonthlyTotal = remember(monthlyTotals) { monthlyTotals.maxOrNull()?.coerceAtLeast(1f) ?: 1f }

    // Report identity ramp (spec §11): Spending = Coral, Income = Teal.
    val reportRamp = if (transactionType == TransactionType.INCOME) Ramp.Teal else Ramp.Coral

    // Chart items: category ramp per row, capped at 6 visible + a "Other" rollup (spec §15).
    val chartItems = remember(categoryDetails, transactionType) {
        if (categoryDetails.size <= 6) {
            categoryDetails
        } else {
            val top = categoryDetails.take(5)
            val rest = categoryDetails.drop(5)
            val other = CategoryDetailItem(
                category = CategoryEntity(
                    id = "__other__",
                    name = "Other",
                    iconName = "MoreHoriz",
                    colorHex = "#888780",
                    type = transactionType
                ),
                totalAmount = rest.sumOf { it.totalAmount },
                percentage = rest.sumOf { it.percentage.toDouble() }.toFloat(),
                transactionCount = rest.sumOf { it.transactionCount },
                averageAmount = 0.0
            )
            top + other
        }
    }
    val dominantItem = chartItems.maxByOrNull { it.percentage }
    val isDominant = (dominantItem?.percentage ?: 0f) >= 0.85f

    fun rampFor(item: CategoryDetailItem): Ramp = when {
        item.category.id == "__other__" -> Ramp.Gray
        transactionType == TransactionType.INCOME -> Ramp.Teal
        else -> sectionRamp(getExpenseCategoryGroup(item.category))
    }

    val isDark = isAppInDarkTheme()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = true)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .navigationBarsPadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Persistent Header — ✕ and title only (spec §14: one Done action, in the footer, never duplicated).
                Surface(color = MaterialTheme.colorScheme.surface) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                style = SelfBudgetType.title,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            val displaySubtitle = subtitle.ifBlank { periodLabel }
                            if (displaySubtitle.isNotBlank()) {
                                Text(
                                    text = displaySubtitle,
                                    style = SelfBudgetType.meta,
                                    color = reportRamp.secondaryText(isDark),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Hero: type badge carries color, the amount stays neutral (spec §14).
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = ShapeCard,
                            color = MaterialTheme.colorScheme.surface,
                        ) {
                            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Surface(shape = ShapePill, color = reportRamp.tintFill(isDark)) {
                                    Text(
                                        text = if (transactionType == TransactionType.INCOME) "Income" else "Expense",
                                        style = SelfBudgetType.badge,
                                        color = reportRamp.titleText(isDark),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "$currencySymbol%.2f".format(totalAmount),
                                    style = SelfBudgetType.display,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                NeutralBadge(text = "${activeTxs.size} transactions")

                                if (categoryDetails.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    val topCat = categoryDetails.first()
                                    Text(
                                        text = "Top category: ${topCat.category.name} ($currencySymbol%.2f • %.1f%%)".format(topCat.totalAmount, topCat.percentage * 100),
                                        style = SelfBudgetType.meta,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Interactive Donut Ring Breakdown Chart — or the dominance fallback (spec §15) when one
                    // category is ~85%+ of the total, since a ring at that share reads as a solid circle.
                    if (categoryDetails.isNotEmpty()) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = ShapeCard,
                                color = MaterialTheme.colorScheme.surface,
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Category share",
                                        style = SelfBudgetType.heading,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.align(Alignment.Start)
                                    )
                                    Spacer(modifier = Modifier.height(20.dp))

                                    if (isDominant && dominantItem != null) {
                                        Text(
                                            text = "${dominantItem.category.name} is ${(dominantItem.percentage * 100).roundToInt()}% of ${if (transactionType == TransactionType.INCOME) "income" else "spending"} — bar shows the full breakdown.",
                                            style = SelfBudgetType.body,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.align(Alignment.Start)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(20.dp)
                                                .clip(ShapePill)
                                        ) {
                                            chartItems.forEach { item ->
                                                Box(
                                                    modifier = Modifier
                                                        .weight(item.percentage.coerceAtLeast(0.001f))
                                                        .fillMaxHeight()
                                                        .background(rampFor(item).c400)
                                                )
                                            }
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(200.dp)
                                                .padding(10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                                var startAngle = -90f
                                                val strokeWidth = 20.dp.toPx()
                                                val halfStroke = strokeWidth / 2f
                                                val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)

                                                chartItems.forEach { item ->
                                                    val sweepAngle = item.percentage * 360f
                                                    drawArc(
                                                        color = rampFor(item).c400,
                                                        startAngle = startAngle,
                                                        sweepAngle = sweepAngle.coerceAtLeast(1f),
                                                        useCenter = false,
                                                        topLeft = Offset(halfStroke, halfStroke),
                                                        size = arcSize,
                                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                                                    )
                                                    startAngle += sweepAngle
                                                }
                                            }

                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = "${categoryDetails.size}",
                                                    style = SelfBudgetType.title,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "Categories",
                                                    style = SelfBudgetType.meta,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                }
                            }
                        }
                    }

                    // Annual Month-by-Month Trend Chart (If ANNUAL view)
                    if (timeframe == AnalyticsTimeframe.ANNUAL) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = ShapeCard,
                                color = MaterialTheme.colorScheme.surface,
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Text(
                                        text = "12-month distribution ($periodLabel)",
                                        style = SelfBudgetType.heading,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(140.dp),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        val cal = Calendar.getInstance()
                                        cal.set(Calendar.DAY_OF_MONTH, 1)
                                        for (monthIdx in 0..11) {
                                            cal.set(Calendar.MONTH, monthIdx)
                                            val mLabel = sdfMonthShort.format(cal.time)
                                            val valAmt = monthlyTotals[monthIdx]
                                            val barFraction = if (maxMonthlyTotal > 0) (valAmt / maxMonthlyTotal).coerceIn(0f, 1f) else 0f

                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Bottom,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth(0.55f)
                                                        .height((barFraction * 110).dp.coerceAtLeast(4.dp))
                                                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                                        .background(if (valAmt > 0) reportRamp.c400 else MaterialTheme.colorScheme.outlineVariant)
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = mLabel,
                                                    style = SelfBudgetType.meta,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Detailed Category Ranking List
                    item {
                        Text(
                            text = "Category ranking",
                            style = SelfBudgetType.heading,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (categoryDetails.isEmpty()) {
                        item {
                            Surface(
                                shape = ShapeCard,
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier.padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No ${transactionType.name.lowercase()} records logged for $periodLabel.",
                                        style = SelfBudgetType.body,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = ShapeCard,
                                color = MaterialTheme.colorScheme.surface,
                            ) {
                                Column {
                                    categoryDetails.forEachIndexed { index, item ->
                                        val catRamp = rampFor(item)

                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 14.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    modifier = Modifier.weight(1f),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    RampIconTile(icon = getCategoryIcon(item.category), ramp = catRamp, size = 36.dp, iconSize = 18.dp)

                                                    Spacer(modifier = Modifier.width(12.dp))

                                                    Column {
                                                        Text(
                                                            text = item.category.name,
                                                            style = SelfBudgetType.rowTitle,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Text(
                                                            text = "${item.transactionCount} ${if (item.transactionCount == 1) "entry" else "entries"} • avg $currencySymbol%.2f".format(item.averageAmount),
                                                            style = SelfBudgetType.meta,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }

                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text(
                                                        text = "$currencySymbol%.2f".format(item.totalAmount),
                                                        style = SelfBudgetType.rowTitle,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    NeutralBadge(text = formatSharePercent(item.percentage))
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))

                                            LinearProgressIndicator(
                                                progress = { item.percentage.coerceIn(0f, 1f) },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(6.dp)
                                                    .clip(ShapeChip),
                                                color = getProgressBarColor(catRamp.c400),
                                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                                            )
                                        }

                                        if (index < categoryDetails.size - 1) {
                                            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(150.dp))
                    }
                }
            }
        }
    }
}

private fun formatSharePercent(pct: Float): String {
    if (pct in 0f..0.009999f && pct > 0f) return "<1%"
    return "${(pct * 100f).roundToInt()}%"
}
