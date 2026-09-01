package com.selfbudget.app.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.selfbudget.app.core.ui.getCategoryIcon
import com.selfbudget.app.data.model.CategoryEntity
import com.selfbudget.app.data.model.TransactionEntity
import com.selfbudget.app.data.model.TransactionType
import com.selfbudget.app.feature.analytics.AnalyticsTimeframe
import com.selfbudget.app.ui.theme.getExpenseColor
import com.selfbudget.app.ui.theme.getIncomeColor
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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
                    catId, "Uncategorized", "MoreHoriz", "#607D8B", transactionType
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

    val themeAccentColor = if (transactionType == TransactionType.INCOME) getIncomeColor() else getExpenseColor()

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
                // Persistent Header
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                    shadowElevation = 2.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val displaySubtitle = subtitle.ifBlank { periodLabel }
                                if (displaySubtitle.isNotBlank()) {
                                    Text(
                                        text = displaySubtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Done", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Hero Card: Total & Top Category
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (transactionType == TransactionType.INCOME) "Total Earned" else "Total Spent",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(themeAccentColor.copy(alpha = 0.15f))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (transactionType == TransactionType.INCOME) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                                contentDescription = null,
                                                tint = themeAccentColor,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "${activeTxs.size} Transactions",
                                                color = themeAccentColor,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "$currencySymbol%.2f".format(totalAmount),
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                if (categoryDetails.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    val topCat = categoryDetails.first()
                                    Text(
                                        text = "Top Category: ${topCat.category.name} ($currencySymbol%.2f • %.1f%%)".format(topCat.totalAmount, topCat.percentage * 100),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = themeAccentColor
                                    )
                                }
                            }
                        }
                    }

                    // Interactive Donut Ring Breakdown Chart
                    if (categoryDetails.isNotEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Category Share Ring Chart",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.align(Alignment.Start)
                                    )
                                    Spacer(modifier = Modifier.height(20.dp))

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

                                            categoryDetails.forEach { item ->
                                                val sweepAngle = item.percentage * 360f
                                                val color = try {
                                                    Color(android.graphics.Color.parseColor(item.category.colorHex))
                                                } catch (e: Exception) {
                                                    themeAccentColor
                                                }

                                                drawArc(
                                                    color = color,
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
                                                style = MaterialTheme.typography.headlineMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Categories",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
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
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Text(
                                        text = "12-Month Distribution ($periodLabel)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(130.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        val cal = Calendar.getInstance()
                                        for (monthIdx in 0..11) {
                                            cal.set(Calendar.MONTH, monthIdx)
                                            val mLabel = sdfMonthShort.format(cal.time)
                                            val valAmt = monthlyTotals[monthIdx]
                                            val barFraction = if (maxMonthlyTotal > 0) (valAmt / maxMonthlyTotal).coerceIn(0.05f, 1f) else 0.05f

                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Bottom,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .width(14.dp)
                                                        .fillMaxHeight(barFraction)
                                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                                        .background(if (valAmt > 0) themeAccentColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = mLabel.take(1),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
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
                            text = "Category Ranking Breakdown",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (categoryDetails.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                            ) {
                                Box(
                                    modifier = Modifier.padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No ${transactionType.name.lowercase()} records logged for $periodLabel.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(categoryDetails, key = { it.category.id }) { item ->
                            val catColor = try {
                                Color(android.graphics.Color.parseColor(item.category.colorHex))
                            } catch (e: Exception) {
                                themeAccentColor
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .background(catColor.copy(alpha = 0.2f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = getCategoryIcon(item.category),
                                                    contentDescription = null,
                                                    tint = catColor,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Column {
                                                Text(
                                                    text = item.category.name,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "${item.transactionCount} entries • avg $currencySymbol%.2f".format(item.averageAmount),
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "$currencySymbol%.2f".format(item.totalAmount),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "%.1f%%".format(item.percentage * 100),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = catColor
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    LinearProgressIndicator(
                                        progress = { item.percentage.coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = catColor,
                                        trackColor = catColor.copy(alpha = 0.18f)
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(120.dp))
                    }
                }
            }
        }
    }
}
