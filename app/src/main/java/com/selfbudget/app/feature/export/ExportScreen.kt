package com.selfbudget.app.feature.export

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.selfbudget.app.core.ui.components.PrimaryPillButton
import com.selfbudget.app.core.ui.components.RampIconTile
import com.selfbudget.app.core.util.CsvExporter
import com.selfbudget.app.data.model.CategoryEntity
import com.selfbudget.app.data.model.TransactionEntity
import com.selfbudget.app.ui.theme.Ramp
import com.selfbudget.app.ui.theme.SelfBudgetType
import com.selfbudget.app.ui.theme.ShapeHero
import com.selfbudget.app.ui.theme.isAppInDarkTheme
import com.selfbudget.app.ui.theme.secondaryText
import com.selfbudget.app.ui.theme.tintFill
import com.selfbudget.app.ui.theme.titleText

@Composable
fun ExportScreen(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>
) {
    val context = LocalContext.current
    val isDark = isAppInDarkTheme()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Data export & backup",
            style = SelfBudgetType.heading,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Explainer card (spec §21): icon tile + headline + body + one primary CTA.
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = ShapeHero,
            color = Ramp.Teal.tintFill(isDark)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RampIconTile(icon = Icons.Default.FileDownload, ramp = Ramp.Teal, size = 44.dp, iconSize = 24.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Export to CSV",
                            style = SelfBudgetType.heading,
                            color = Ramp.Teal.titleText(isDark)
                        )
                        Text(
                            text = "Export your financial records into standard CSV format for Excel, Google Sheets, or local backups.",
                            style = SelfBudgetType.body,
                            color = Ramp.Teal.secondaryText(isDark)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Total records",
                        style = SelfBudgetType.body,
                        color = Ramp.Teal.secondaryText(isDark)
                    )
                    Text(
                        text = "${transactions.size} entries",
                        style = SelfBudgetType.rowTitle,
                        color = Ramp.Teal.titleText(isDark)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                PrimaryPillButton(
                    text = "Share / save CSV file",
                    onClick = { CsvExporter.exportAndShareTransactions(context, transactions, categories) },
                    enabled = transactions.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
