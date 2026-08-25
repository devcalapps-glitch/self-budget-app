package com.selfbudget.app.feature.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import com.selfbudget.app.core.ui.getCategoryIcon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.selfbudget.app.core.util.Currencies
import com.selfbudget.app.data.model.AccountEntity
import com.selfbudget.app.data.model.CategoryEntity
import com.selfbudget.app.data.model.TransactionEntity
import com.selfbudget.app.data.model.TransactionType
import com.selfbudget.app.ui.theme.ExpenseRed
import com.selfbudget.app.ui.theme.IncomeGreen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class DateRangeFilter(val label: String) {
    ALL("All Time"),
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    LAST_30_DAYS("Last 30 Days"),
    THIS_YEAR("This Year")
}

enum class SortOption(val label: String) {
    NEWEST("Newest First ⬇️"),
    OLDEST("Oldest First ⬆️"),
    HIGHEST_AMOUNT("Highest Amount 💰"),
    LOWEST_AMOUNT("Lowest Amount 🏷️")
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    accounts: List<AccountEntity> = emptyList(),
    currencySymbol: String = "$",
    onDeleteTransaction: (TransactionEntity) -> Unit,
    onEditTransaction: ((TransactionEntity) -> Unit)? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf<TransactionType?>(null) } // null = All
    var selectedCategoryId by remember { mutableStateOf<String?>(null) } // null = All Categories
    var selectedAccountId by remember { mutableStateOf<String?>(null) } // null = All Accounts
    var selectedDateRange by remember { mutableStateOf(DateRangeFilter.ALL) }
    var selectedSortOption by remember { mutableStateOf(SortOption.NEWEST) }

    var showFilterModal by remember { mutableStateOf(false) }
    var pendingDeleteTx by remember { mutableStateOf<TransactionEntity?>(null) }

    val categoryMap = remember(categories) { categories.associateBy { it.id } }
    val accountMap = remember(accounts) { accounts.associateBy { it.id } }
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()) }

    val activeFilterCount = remember(
        selectedTypeFilter,
        selectedCategoryId,
        selectedAccountId,
        selectedDateRange,
        selectedSortOption
    ) {
        var count = 0
        if (selectedTypeFilter != null) count++
        if (selectedCategoryId != null) count++
        if (selectedAccountId != null) count++
        if (selectedDateRange != DateRangeFilter.ALL) count++
        if (selectedSortOption != SortOption.NEWEST) count++
        count
    }

    val filteredTransactions = remember(
        transactions,
        searchQuery,
        selectedTypeFilter,
        selectedCategoryId,
        selectedAccountId,
        selectedDateRange
    ) {
        val now = Calendar.getInstance()
        val currentYear = now.get(Calendar.YEAR)
        val currentMonth = now.get(Calendar.MONTH)

        val lastMonthCal = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
        val lastMonthYear = lastMonthCal.get(Calendar.YEAR)
        val lastMonthMonth = lastMonthCal.get(Calendar.MONTH)

        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)

        transactions.filter { tx ->
            val matchesQuery = searchQuery.isBlank() ||
                    tx.title.contains(searchQuery, ignoreCase = true) ||
                    (tx.note?.contains(searchQuery, ignoreCase = true) == true) ||
                    (accountMap[tx.accountId]?.name?.contains(searchQuery, ignoreCase = true) == true)

            val matchesType = selectedTypeFilter == null || tx.type == selectedTypeFilter
            val matchesCategory = selectedCategoryId == null || tx.categoryId == selectedCategoryId
            val matchesAccount = selectedAccountId == null || tx.accountId == selectedAccountId || tx.transferAccountId == selectedAccountId

            val txCal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
            val matchesDate = when (selectedDateRange) {
                DateRangeFilter.ALL -> true
                DateRangeFilter.THIS_MONTH -> txCal.get(Calendar.YEAR) == currentYear && txCal.get(Calendar.MONTH) == currentMonth
                DateRangeFilter.LAST_MONTH -> txCal.get(Calendar.YEAR) == lastMonthYear && txCal.get(Calendar.MONTH) == lastMonthMonth
                DateRangeFilter.LAST_30_DAYS -> tx.timestamp >= thirtyDaysAgo
                DateRangeFilter.THIS_YEAR -> txCal.get(Calendar.YEAR) == currentYear
            }

            matchesQuery && matchesType && matchesCategory && matchesAccount && matchesDate
        }
    }

    val sortedTransactions = remember(filteredTransactions, selectedSortOption) {
        when (selectedSortOption) {
            SortOption.NEWEST -> filteredTransactions.sortedByDescending { it.timestamp }
            SortOption.OLDEST -> filteredTransactions.sortedBy { it.timestamp }
            SortOption.HIGHEST_AMOUNT -> filteredTransactions.sortedByDescending { it.amount }
            SortOption.LOWEST_AMOUNT -> filteredTransactions.sortedBy { it.amount }
        }
    }

    val totalFilteredExpense = remember(filteredTransactions) {
        filteredTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    }
    val totalFilteredIncome = remember(filteredTransactions) {
        filteredTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search Input Field & Filter Button Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.weight(1f),
                label = { Text("Search Activity") },
                placeholder = { Text("Search", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            // Sleek Single Filter Button
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (activeFilterCount > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(
                    1.dp,
                    if (activeFilterCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
                modifier = Modifier
                    .height(56.dp)
                    .clickable { showFilterModal = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filter",
                        tint = if (activeFilterCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (activeFilterCount > 0) "Filter ($activeFilterCount)" else "Filter",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (activeFilterCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Active Filter Badges Bar
        if (activeFilterCount > 0 || searchQuery.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (searchQuery.isNotBlank()) {
                    item {
                        ActiveFilterPill(label = "Search: \"$searchQuery\"", onClear = { searchQuery = "" })
                    }
                }
                if (selectedDateRange != DateRangeFilter.ALL) {
                    item {
                        ActiveFilterPill(label = selectedDateRange.label, onClear = { selectedDateRange = DateRangeFilter.ALL })
                    }
                }
                if (selectedTypeFilter != null) {
                    item {
                        val typeLabel = when (selectedTypeFilter) {
                            TransactionType.EXPENSE -> "Expenses 🔴"
                            TransactionType.INCOME -> "Income 🟢"
                            TransactionType.TRANSFER -> "Transfers 🔁"
                            else -> ""
                        }
                        ActiveFilterPill(label = typeLabel, onClear = { selectedTypeFilter = null })
                    }
                }
                if (selectedAccountId != null) {
                    item {
                        val accName = accountMap[selectedAccountId]?.name ?: "Account"
                        ActiveFilterPill(label = accName, onClear = { selectedAccountId = null })
                    }
                }
                if (selectedCategoryId != null) {
                    item {
                        val catName = categoryMap[selectedCategoryId]?.name ?: "Category"
                        ActiveFilterPill(label = catName, onClear = { selectedCategoryId = null })
                    }
                }
                if (selectedSortOption != SortOption.NEWEST) {
                    item {
                        ActiveFilterPill(label = selectedSortOption.label, onClear = { selectedSortOption = SortOption.NEWEST })
                    }
                }
                item {
                    Text(
                        text = "Reset All",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                searchQuery = ""
                                selectedTypeFilter = null
                                selectedCategoryId = null
                                selectedAccountId = null
                                selectedDateRange = DateRangeFilter.ALL
                                selectedSortOption = SortOption.NEWEST
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Results Summary Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${sortedTransactions.size} Activity Record${if (sortedTransactions.size != 1) "s" else ""}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (totalFilteredIncome > 0) {
                    Text(
                        text = "+$currencySymbol%.2f".format(totalFilteredIncome),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = IncomeGreen
                    )
                }
                if (totalFilteredExpense > 0) {
                    Text(
                        text = "-$currencySymbol%.2f".format(totalFilteredExpense),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = ExpenseRed
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Results List
        if (sortedTransactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (activeFilterCount > 0 || searchQuery.isNotBlank()) "No records match your active filters." else "No activity logged yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(sortedTransactions, key = { it.id }) { tx ->
                    val category = categoryMap[tx.categoryId]
                    val account = accountMap[tx.accountId]
                    val isIncome = tx.type == TransactionType.INCOME
                    val isTransfer = tx.type == TransactionType.TRANSFER
                    val sym = if (account?.currencyCode?.isNotBlank() == true) Currencies.symbolFor(account.currencyCode) else currencySymbol

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (onEditTransaction != null) Modifier.clickable { onEditTransaction(tx) } else Modifier),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                val icon = when {
                                    isTransfer -> Icons.Default.SwapHoriz
                                    category != null -> getCategoryIcon(category)
                                    isIncome -> Icons.Default.ArrowDownward
                                    else -> Icons.Default.ArrowUpward
                                }
                                val iconColor = when {
                                    isTransfer -> MaterialTheme.colorScheme.primary
                                    isIncome -> IncomeGreen
                                    else -> ExpenseRed
                                }

                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(iconColor.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = iconColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = tx.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${category?.name ?: "General"}${if (account != null) " • ${account.name}" else ""} • ${dateFormat.format(Date(tx.timestamp))}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                                    )
                                    if (!tx.note.isNull_or_blank()) {
                                        Text(
                                            text = "Note: ${tx.note}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                        )
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val amountPrefix = when {
                                    isTransfer -> "🔁 $sym"
                                    isIncome -> "+$sym"
                                    else -> "-$sym"
                                }
                                val amountColor = when {
                                    isTransfer -> MaterialTheme.colorScheme.primary
                                    isIncome -> IncomeGreen
                                    else -> ExpenseRed
                                }

                                Text(
                                    text = "$amountPrefix%.2f".format(tx.amount),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = amountColor
                                )
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

    // Unified Full-Screen Modal: Filter Options
    if (showFilterModal) {
        Dialog(
            onDismissRequest = { showFilterModal = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false
            )
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(22.dp)
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { showFilterModal = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Filter Activity",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = {
                                    selectedTypeFilter = null
                                    selectedCategoryId = null
                                    selectedAccountId = null
                                    selectedDateRange = DateRangeFilter.ALL
                                    selectedSortOption = SortOption.NEWEST
                                }
                            ) {
                                Text("Reset All", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            Button(
                                onClick = { showFilterModal = false },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("Apply", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Section 1: Timeframe
                    Column {
                        Text("Timeframe", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DateRangeFilter.values().forEach { range ->
                                FilterChip(
                                    selected = selectedDateRange == range,
                                    onClick = { selectedDateRange = range },
                                    label = { Text(range.label, fontSize = 13.sp) },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.50f),
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }
                    }

                    // Section 2: Transaction Type
                    Column {
                        Text("Transaction Type", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = selectedTypeFilter == null,
                                onClick = { selectedTypeFilter = null },
                                label = { Text("All Types", fontSize = 13.sp) },
                                shape = RoundedCornerShape(16.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.50f),
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                            FilterChip(
                                selected = selectedTypeFilter == TransactionType.EXPENSE,
                                onClick = { selectedTypeFilter = if (selectedTypeFilter == TransactionType.EXPENSE) null else TransactionType.EXPENSE },
                                label = { Text("Expenses 🔴", fontSize = 13.sp) },
                                shape = RoundedCornerShape(16.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.50f),
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                            FilterChip(
                                selected = selectedTypeFilter == TransactionType.INCOME,
                                onClick = { selectedTypeFilter = if (selectedTypeFilter == TransactionType.INCOME) null else TransactionType.INCOME },
                                label = { Text("Income 🟢", fontSize = 13.sp) },
                                shape = RoundedCornerShape(16.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.50f),
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                            FilterChip(
                                selected = selectedTypeFilter == TransactionType.TRANSFER,
                                onClick = { selectedTypeFilter = if (selectedTypeFilter == TransactionType.TRANSFER) null else TransactionType.TRANSFER },
                                label = { Text("Transfers 🔁", fontSize = 13.sp) },
                                shape = RoundedCornerShape(16.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.50f),
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }

                    // Section 3: Bank Accounts
                    if (accounts.isNotEmpty()) {
                        Column {
                            Text("Bank Account / Wallet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = selectedAccountId == null,
                                    onClick = { selectedAccountId = null },
                                    label = { Text("All Accounts", fontSize = 13.sp) },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.50f),
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                                accounts.forEach { acc ->
                                    FilterChip(
                                        selected = selectedAccountId == acc.id,
                                        onClick = { selectedAccountId = if (selectedAccountId == acc.id) null else acc.id },
                                        label = { Text(acc.name, fontSize = 13.sp) },
                                        shape = RoundedCornerShape(16.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.50f),
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Section 4: Sort Order
                    Column {
                        Text("Sort Order", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SortOption.values().forEach { option ->
                                FilterChip(
                                    selected = selectedSortOption == option,
                                    onClick = { selectedSortOption = option },
                                    label = { Text(option.label, fontSize = 13.sp) },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.50f),
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Inline Main Action Row inside the Filter Page Body
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showFilterModal = false },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                        ) {
                            Text("Cancel", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { showFilterModal = false },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1.5f)
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                text = "Apply Filters${if (activeFilterCount > 0) " ($activeFilterCount)" else ""}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }
    }

    // Delete Transaction Confirmation Modal
    if (pendingDeleteTx != null) {
        val txToDelete = pendingDeleteTx!!
        val isIncome = txToDelete.type == TransactionType.INCOME

        Dialog(
            onDismissRequest = { pendingDeleteTx = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 12.dp,
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Delete Transaction?",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Are you sure you want to delete this transaction record? This cannot be undone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = txToDelete.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${if (isIncome) "+$currencySymbol" else "-$currencySymbol"}%.2f".format(txToDelete.amount),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isIncome) IncomeGreen else ExpenseRed
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { pendingDeleteTx = null },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("Cancel", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                onDeleteTransaction(txToDelete)
                                pendingDeleteTx = null
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("Delete", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveFilterPill(
    label: String,
    onClear: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Clear",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .size(14.dp)
                    .clickable { onClear() }
            )
        }
    }
}

private fun String?.isNull_or_blank(): Boolean {
    return this == null || this.trim().isEmpty()
}
