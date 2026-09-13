package com.selfbudget.app.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.selfbudget.app.core.ui.components.GrayIconTile
import com.selfbudget.app.core.ui.components.NeutralBadge
import com.selfbudget.app.core.ui.components.PrimaryPillButton
import com.selfbudget.app.core.ui.components.RampIconTile
import com.selfbudget.app.core.ui.components.SecondaryPillButton
import com.selfbudget.app.core.ui.components.SectionHeaderBand
import com.selfbudget.app.core.ui.components.SectionRowDivider
import com.selfbudget.app.core.util.CsvExporter
import com.selfbudget.app.core.util.ExcelExporter
import com.selfbudget.app.core.util.ExportDataType
import com.selfbudget.app.data.model.*
import com.selfbudget.app.ui.theme.Ramp
import com.selfbudget.app.ui.theme.SelfBudgetType
import com.selfbudget.app.ui.theme.ShapeCard
import com.selfbudget.app.ui.theme.ShapePill
import com.selfbudget.app.ui.theme.isAppInDarkTheme
import com.selfbudget.app.ui.theme.onSolidFill
import com.selfbudget.app.ui.theme.secondaryText
import com.selfbudget.app.ui.theme.solidFill
import com.selfbudget.app.ui.theme.tintFill
import com.selfbudget.app.ui.theme.titleText

enum class ExportFileFormat(
    val label: String,
    val description: String,
    val icon: ImageVector
) {
    EXCEL("Excel (.xlsx)", "Single spreadsheet workbook for all selected data (recommended)", Icons.Default.TableChart),
    CSV("CSV (.csv)", "Plain text format (.csv / .zip bundle)", Icons.Default.Description)
}

@Composable
fun DataExportModal(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    accounts: List<AccountEntity>,
    recurringList: List<RecurringTransactionEntity>,
    budgets: List<BudgetEntity>,
    goals: List<GoalEntity>,
    accountBalances: Map<String, Double> = emptyMap(),
    onDismiss: () -> Unit,
    onExportComplete: (Boolean, String) -> Unit
) {
    val context = LocalContext.current
    val isDark = isAppInDarkTheme()
    var selectedFormat by remember { mutableStateOf(ExportFileFormat.EXCEL) }
    var selectedTypes by remember {
        mutableStateOf(
            setOf(
                ExportDataType.TRANSACTIONS,
                ExportDataType.RECURRING,
                ExportDataType.BUDGET,
                ExportDataType.GOALS,
                ExportDataType.ACCOUNTS,
                ExportDataType.CATEGORIES
            )
        )
    }

    val allTypes = ExportDataType.entries

    fun toggleType(type: ExportDataType) {
        selectedTypes = if (selectedTypes.contains(type)) {
            selectedTypes - type
        } else {
            selectedTypes + type
        }
    }

    val isAllSelected = selectedTypes.size == allTypes.size

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.88f)
                .clip(ShapeCard),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header: close + title only (spec §14/§19)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RampIconTile(icon = Icons.Default.FileDownload, ramp = Ramp.Teal, size = 40.dp, iconSize = 22.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Export financial data",
                                style = SelfBudgetType.heading,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Choose file format & data sets to include",
                                style = SelfBudgetType.meta,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // FORMAT SELECTOR
                    Text(
                        text = "EXPORT FORMAT",
                        style = SelfBudgetType.eyebrow,
                        color = Ramp.Teal.secondaryText(isDark)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExportFileFormat.entries.forEach { format ->
                            val isFormatSelected = selectedFormat == format
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedFormat = format },
                                shape = com.selfbudget.app.ui.theme.ShapeChip,
                                color = if (isFormatSelected) Ramp.Teal.solidFill(isDark) else Ramp.Gray.tintFill(isDark)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = format.icon,
                                        contentDescription = null,
                                        tint = if (isFormatSelected) Ramp.Teal.onSolidFill(isDark) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = format.label,
                                        style = SelfBudgetType.badge,
                                        color = if (isFormatSelected) Ramp.Teal.onSolidFill(isDark) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Format hint subtext
                    Text(
                        text = if (selectedFormat == ExportFileFormat.EXCEL) {
                            "Single spreadsheet file (.xlsx) with all selected data sets."
                        } else {
                            "Plain-text CSV format (single .csv or .zip bundle if multiple)."
                        },
                        style = SelfBudgetType.meta,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // DATA SETS SELECTOR HEADER & SELECT ALL
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${selectedTypes.size} of ${allTypes.size} data sets selected",
                            style = SelfBudgetType.meta,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = if (isAllSelected) "Deselect all" else "Select all",
                            style = SelfBudgetType.body,
                            color = Ramp.Teal.secondaryText(isDark),
                            modifier = Modifier
                                .clip(ShapePill)
                                .clickable {
                                    selectedTypes = if (isAllSelected) emptySet() else allTypes.toSet()
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Data Options — one sectioned container, hairline-divided rows (no card-in-card).
                    SectionHeaderBand(title = "Data sets", ramp = Ramp.Teal) {
                        allTypes.forEachIndexed { index, type ->
                            if (index > 0) SectionRowDivider()
                            val count = when (type) {
                                ExportDataType.TRANSACTIONS -> transactions.size
                                ExportDataType.RECURRING -> recurringList.size
                                ExportDataType.BUDGET -> budgets.size
                                ExportDataType.GOALS -> goals.size
                                ExportDataType.ACCOUNTS -> accounts.size
                                ExportDataType.CATEGORIES -> categories.size
                            }
                            val countLabel = when (type) {
                                ExportDataType.TRANSACTIONS -> "$count records"
                                ExportDataType.RECURRING -> "$count bills & rules"
                                ExportDataType.BUDGET -> "$count limits"
                                ExportDataType.GOALS -> "$count goals"
                                ExportDataType.ACCOUNTS -> "$count accounts"
                                ExportDataType.CATEGORIES -> "$count categories"
                            }
                            val icon: ImageVector = when (type) {
                                ExportDataType.TRANSACTIONS -> Icons.AutoMirrored.Filled.ReceiptLong
                                ExportDataType.RECURRING -> Icons.Default.Repeat
                                ExportDataType.BUDGET -> Icons.Default.PieChart
                                ExportDataType.GOALS -> Icons.Default.Savings
                                ExportDataType.ACCOUNTS -> Icons.Default.AccountBalance
                                ExportDataType.CATEGORIES -> Icons.Default.Category
                            }
                            val isChecked = selectedTypes.contains(type)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { toggleType(type) }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                GrayIconTile(icon = icon, size = 36.dp, iconSize = 18.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = type.title,
                                            style = SelfBudgetType.rowTitle,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        NeutralBadge(text = countLabel)
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = type.subtitle,
                                        style = SelfBudgetType.meta,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { toggleType(type) },
                                    colors = CheckboxDefaults.colors(checkedColor = Ramp.Teal.solidFill(isDark))
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SecondaryPillButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )

                    PrimaryPillButton(
                        text = if (selectedFormat == ExportFileFormat.EXCEL) {
                            "Export Excel (${selectedTypes.size})"
                        } else {
                            "Export CSV (${selectedTypes.size})"
                        },
                        onClick = {
                            val success = if (selectedFormat == ExportFileFormat.EXCEL) {
                                ExcelExporter.exportAndShareExcel(
                                    context = context,
                                    selectedTypes = selectedTypes,
                                    transactions = transactions,
                                    categories = categories,
                                    accounts = accounts,
                                    recurring = recurringList,
                                    budgets = budgets,
                                    goals = goals,
                                    accountBalances = accountBalances
                                )
                            } else {
                                CsvExporter.exportAndShareSelected(
                                    context = context,
                                    selectedTypes = selectedTypes,
                                    transactions = transactions,
                                    categories = categories,
                                    accounts = accounts,
                                    recurring = recurringList,
                                    budgets = budgets,
                                    goals = goals,
                                    accountBalances = accountBalances
                                )
                            }

                            if (success) {
                                val formatStr = if (selectedFormat == ExportFileFormat.EXCEL) "Excel (.xlsx)" else "CSV"
                                onExportComplete(true, "Exported $formatStr successfully.")
                            } else {
                                onExportComplete(false, "Export failed. Please try again.")
                            }
                            onDismiss()
                        },
                        enabled = selectedTypes.isNotEmpty(),
                        modifier = Modifier.weight(1.5f)
                    )
                }
            }
        }
    }
}
