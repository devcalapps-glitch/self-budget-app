package com.selfbudget.app.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.selfbudget.app.core.ui.ManageCategoriesModal
import com.selfbudget.app.core.ui.components.DestructivePillButton
import com.selfbudget.app.core.ui.components.PrimaryPillButton
import com.selfbudget.app.core.util.CsvExporter
import com.selfbudget.app.data.model.CategoryEntity
import com.selfbudget.app.data.model.TransactionEntity
import com.selfbudget.app.data.model.UserEntity
import com.selfbudget.app.ui.theme.Ramp
import com.selfbudget.app.ui.theme.SelfBudgetType
import com.selfbudget.app.ui.theme.ShapeCard
import com.selfbudget.app.ui.theme.isAppInDarkTheme
import com.selfbudget.app.ui.theme.onSolidFill
import com.selfbudget.app.ui.theme.solidFill
import com.selfbudget.app.ui.theme.tintFill

@Composable
fun ProfileScreen(
    user: UserEntity?,
    currencySymbol: String,
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    onSetCurrency: (String) -> Unit,
    onToggleCategoryArchive: (CategoryEntity) -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val currencies = listOf("$", "€", "£", "₹", "¥", "A$")
    var showManageCategoriesModal by remember { mutableStateOf(false) }
    val isDark = isAppInDarkTheme()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Settings", style = SelfBudgetType.title, color = MaterialTheme.colorScheme.onSurface)

        // User Account Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = ShapeCard,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = user?.displayName ?: "User",
                        style = SelfBudgetType.heading,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = user?.email ?: "",
                        style = SelfBudgetType.meta,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Manage Custom Categories Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showManageCategoriesModal = true },
            shape = ShapeCard,
            colors = CardDefaults.cardColors(
                containerColor = Ramp.Gray.tintFill(isDark)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = "Manage custom categories", style = SelfBudgetType.rowTitle, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            text = "View, archive, or restore custom categories",
                            style = SelfBudgetType.meta,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Currency Preference Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = ShapeCard,
            colors = CardDefaults.cardColors(
                containerColor = Ramp.Gray.tintFill(isDark)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AttachMoney,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Preferred currency symbol", style = SelfBudgetType.rowTitle, color = MaterialTheme.colorScheme.onSurface)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    currencies.forEach { symbol ->
                        FilterChip(
                            selected = currencySymbol == symbol,
                            onClick = { onSetCurrency(symbol) },
                            label = { Text(symbol, style = SelfBudgetType.rowTitle) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Ramp.Teal.solidFill(isDark),
                                selectedLabelColor = Ramp.Teal.onSolidFill(isDark)
                            )
                        )
                    }
                }
            }
        }

        // CSV Export & Backup Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = ShapeCard,
            colors = CardDefaults.cardColors(
                containerColor = Ramp.Gray.tintFill(isDark)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Data export & backup", style = SelfBudgetType.rowTitle, color = MaterialTheme.colorScheme.onSurface)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Export your financial records into standard CSV format (${transactions.size} records available).",
                    style = SelfBudgetType.meta,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                PrimaryPillButton(
                    text = "Export to CSV file",
                    onClick = { CsvExporter.exportAndShareTransactions(context, transactions, categories) },
                    enabled = transactions.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Sign Out Button
        DestructivePillButton(
            text = "Sign out",
            onClick = onSignOut,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        )
    }

    if (showManageCategoriesModal) {
        ManageCategoriesModal(
            categories = categories,
            onDismiss = { showManageCategoriesModal = false },
            onToggleCategoryArchive = onToggleCategoryArchive
        )
    }
}
