package com.selfbudget.app.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.selfbudget.app.core.util.CsvExporter
import com.selfbudget.app.core.util.ExcelExporter
import com.selfbudget.app.core.util.ExportDataType
import com.selfbudget.app.data.model.*

enum class ExportFileFormat(
    val label: String,
    val description: String,
    val icon: ImageVector
) {
    EXCEL("Excel (.xlsx)", "Single spreadsheet workbook for all selected data (Recommended)", Icons.Default.TableChart),
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
    var selectedFormat by remember { mutableStateOf(ExportFileFormat.EXCEL) }
    var selectedTypes by remember {
        mutableStateOf(
            setOf(
                ExportDataType.TRANSACTIONS,
                ExportDataType.RECURRING,
                ExportDataType.BUDGET,
                ExportDataType.GOALS,
                ExportDataType.ACCOUNTS
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
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Export Financial Data",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Choose file format & data sets to include",
                                style = MaterialTheme.typography.bodySmall,
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
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExportFileFormat.entries.forEach { format ->
                            val isFormatSelected = selectedFormat == format
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedFormat = format },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isFormatSelected) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                    }
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (isFormatSelected) {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                    }
                                )
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
                                        tint = if (isFormatSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = format.label,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (isFormatSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isFormatSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    // Format hint subtext
                    Text(
                        text = if (selectedFormat == ExportFileFormat.EXCEL) {
                            "✨ Single spreadsheet file (.xlsx) with all selected data sets."
                        } else {
                            "📄 Plain-text CSV format (single .csv or .zip bundle if multiple)."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // DATA SETS SELECTOR HEADER & SELECT ALL
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${selectedTypes.size} of ${allTypes.size} data sets selected",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        TextButton(
                            onClick = {
                                selectedTypes = if (isAllSelected) {
                                    emptySet()
                                } else {
                                    allTypes.toSet()
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isAllSelected) "Deselect All" else "Select All",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Data Options List
                    allTypes.forEach { type ->
                        val count = when (type) {
                            ExportDataType.TRANSACTIONS -> transactions.size
                            ExportDataType.RECURRING -> recurringList.size
                            ExportDataType.BUDGET -> budgets.size
                            ExportDataType.GOALS -> goals.size
                            ExportDataType.ACCOUNTS -> accounts.size
                        }

                        val countLabel = when (type) {
                            ExportDataType.TRANSACTIONS -> "$count records"
                            ExportDataType.RECURRING -> "$count bills & rules"
                            ExportDataType.BUDGET -> "$count limits"
                            ExportDataType.GOALS -> "$count goals"
                            ExportDataType.ACCOUNTS -> "$count accounts"
                        }

                        val icon: ImageVector = when (type) {
                            ExportDataType.TRANSACTIONS -> Icons.AutoMirrored.Filled.ReceiptLong
                            ExportDataType.RECURRING -> Icons.Default.Repeat
                            ExportDataType.BUDGET -> Icons.Default.PieChart
                            ExportDataType.GOALS -> Icons.Default.Savings
                            ExportDataType.ACCOUNTS -> Icons.Default.AccountBalance
                        }

                        val isChecked = selectedTypes.contains(type)

                        ExportOptionCard(
                            title = type.title,
                            subtitle = type.subtitle,
                            countLabel = countLabel,
                            icon = icon,
                            isChecked = isChecked,
                            onToggle = { toggleType(type) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
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
                                val message = "✅ Exported $formatStr successfully!"
                                onExportComplete(true, message)
                            } else {
                                onExportComplete(false, "❌ Export failed. Please try again.")
                            }
                            onDismiss()
                        },
                        enabled = selectedTypes.isNotEmpty(),
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (selectedFormat == ExportFileFormat.EXCEL) {
                                "Export Excel (${selectedTypes.size})"
                            } else {
                                "Export CSV (${selectedTypes.size})"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExportOptionCard(
    title: String,
    subtitle: String,
    countLabel: String,
    icon: ImageVector,
    isChecked: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isChecked) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
            }
        ),
        border = BorderStroke(
            1.dp,
            if (isChecked) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        if (isChecked) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isChecked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isChecked) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    ) {
                        Text(
                            text = countLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }

            Checkbox(
                checked = isChecked,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}
