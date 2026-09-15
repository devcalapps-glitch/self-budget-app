package com.selfbudget.app.core.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.selfbudget.app.core.ui.components.NeutralBadge
import com.selfbudget.app.core.ui.components.PrimaryPillButton
import com.selfbudget.app.core.ui.components.RampIconTile
import com.selfbudget.app.core.ui.components.SecondaryPillButton
import com.selfbudget.app.core.ui.components.SectionHeaderBand
import com.selfbudget.app.core.ui.components.SectionRowDivider
import com.selfbudget.app.core.util.ParsedImportData
import com.selfbudget.app.ui.theme.Ramp
import com.selfbudget.app.ui.theme.SelfBudgetType
import com.selfbudget.app.ui.theme.ShapeCard
import com.selfbudget.app.ui.theme.isAppInDarkTheme
import com.selfbudget.app.ui.theme.secondaryText
import com.selfbudget.app.ui.theme.tintFill
import com.selfbudget.app.ui.theme.titleText

private data class ImportRow(val icon: ImageVector, val title: String, val count: Int, val subtitle: String)

@Composable
fun DataImportPreviewModal(
    data: ParsedImportData,
    onDismiss: () -> Unit,
    onConfirmImport: () -> Unit
) {
    val isDark = isAppInDarkTheme()
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
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
                        RampIconTile(icon = Icons.Default.FileDownload, ramp = Ramp.Teal, size = 42.dp, iconSize = 24.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Import preview",
                                style = SelfBudgetType.title,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Review data detected in file",
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
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // File Info Banner (spec §12 notice strip: neutral gray, not a warning tint)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = com.selfbudget.app.ui.theme.ShapeChip,
                    color = Ramp.Gray.tintFill(isDark)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when {
                                data.format.contains("Excel", ignoreCase = true) -> Icons.Default.TableChart
                                data.format.contains("JSON", ignoreCase = true) -> Icons.Default.Code
                                else -> Icons.Default.Description
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = data.fileName,
                                style = SelfBudgetType.rowTitle,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = data.format,
                                style = SelfBudgetType.meta,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        NeutralBadge(text = "${data.totalCount} items")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "RECORDS TO IMPORT",
                    style = SelfBudgetType.eyebrow,
                    color = Ramp.Teal.secondaryText(isDark)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Scrollable List of Parsed Entities
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val rows = buildList {
                        if (data.transactions.isNotEmpty()) add(ImportRow(Icons.AutoMirrored.Filled.ReceiptLong, "Transactions", data.transactions.size, "Financial ledger records across categories"))
                        if (data.recurring.isNotEmpty()) add(ImportRow(Icons.Default.Repeat, "Recurring transactions", data.recurring.size, "Automated bill schedules & subscriptions"))
                        if (data.budgets.isNotEmpty()) add(ImportRow(Icons.Default.PieChart, "Monthly budget plans", data.budgets.size, "Category spending limits & baselines"))
                        if (data.goals.isNotEmpty()) add(ImportRow(Icons.Default.Savings, "Savings goals", data.goals.size, "Target funds, deadlines & savings amounts"))
                        if (data.accounts.isNotEmpty()) add(ImportRow(Icons.Default.AccountBalance, "Accounts & wallets", data.accounts.size, "Payment accounts, checking, savings & cards"))
                        if (data.categories.isNotEmpty()) add(ImportRow(Icons.Default.Category, "Custom categories", data.categories.size, "Custom expense & income categories"))
                    }

                    if (rows.isNotEmpty()) {
                        SectionHeaderBand(title = "Records to import", ramp = Ramp.Teal) {
                            rows.forEachIndexed { index, row ->
                                if (index > 0) SectionRowDivider()
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RampIconTile(icon = row.icon, ramp = Ramp.Teal, size = 36.dp, iconSize = 18.dp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(row.title, style = SelfBudgetType.rowTitle, color = MaterialTheme.colorScheme.onSurface)
                                        Text(row.subtitle, style = SelfBudgetType.meta, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    NeutralBadge(text = "+${row.count}")
                                }
                            }
                        }
                    }

                    if (data.totalCount == 0) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = com.selfbudget.app.ui.theme.ShapeChip,
                            color = Ramp.Red.tintFill(isDark)
                        ) {
                            Text(
                                text = "No valid financial records could be parsed from this file. Please ensure the file has valid columns or is an exported Excel, CSV, or JSON file.",
                                style = SelfBudgetType.body,
                                color = Ramp.Red.titleText(isDark),
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Importing will safely merge these records into your account without overwriting unaffected data.",
                        style = SelfBudgetType.meta,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                        text = "Import (${data.totalCount})",
                        onClick = onConfirmImport,
                        enabled = data.totalCount > 0,
                        modifier = Modifier.weight(1.5f)
                    )
                }
            }
        }
    }
}
