package com.selfbudget.app.feature.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import com.selfbudget.app.core.ui.AppLogoBadge
import com.selfbudget.app.core.ui.getCategoryIcon
import com.selfbudget.app.core.util.BudgetRollover
import com.selfbudget.app.core.util.IncomeCalculator
import com.selfbudget.app.core.util.Money
import com.selfbudget.app.core.util.RecurringFrequencyNormalizer
import java.util.Calendar
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.selfbudget.app.core.ui.AddCustomAccountDialog
import com.selfbudget.app.core.ui.EditCustomAccountDialog
import com.selfbudget.app.core.ui.MonthYearHeader
import com.selfbudget.app.data.model.AccountEntity
import com.selfbudget.app.data.model.AccountType
import com.selfbudget.app.data.model.AppThemeMode
import com.selfbudget.app.data.model.CategoryEntity
import com.selfbudget.app.data.model.RecurringFrequency
import com.selfbudget.app.data.model.RecurringTransactionEntity
import com.selfbudget.app.data.model.TransactionEntity
import com.selfbudget.app.data.model.TransactionType
import com.selfbudget.app.data.model.UserEntity
import androidx.compose.foundation.layout.navigationBarsPadding
import com.selfbudget.app.feature.accounts.AccountsScreen
import com.selfbudget.app.feature.analytics.AnalyticsScreen
import com.selfbudget.app.feature.budget.BudgetScreen
import com.selfbudget.app.feature.profile.SettingsScreen
import com.selfbudget.app.feature.recurring.RecurringScreen
import com.selfbudget.app.feature.search.SearchScreen
import com.selfbudget.app.feature.transaction.AddExpenseDialog
import com.selfbudget.app.feature.transaction.AddIncomeDialog
import com.selfbudget.app.feature.transaction.EditTransactionDialog
import com.selfbudget.app.feature.transaction.TransferDialog
import com.selfbudget.app.ui.HomeUiState
import com.selfbudget.app.ui.theme.ExpenseRed
import com.selfbudget.app.ui.theme.IncomeGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectMonthYear: (String) -> Unit,
    onAddTransaction: (
        title: String,
        amount: Double,
        type: TransactionType,
        categoryId: String,
        accountId: String,
        note: String?,
        timestamp: Long,
        isRecurring: Boolean,
        recurringFrequency: RecurringFrequency,
        transferAccountId: String?
    ) -> Unit,
    onUpdateTransaction: (TransactionEntity) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    onSetBudget: (categoryId: String, limit: Double, rolloverEnabled: Boolean) -> Unit,
    onDeleteBudget: (categoryId: String) -> Unit = {},
    onAddRecurring: (title: String, amount: Double, type: TransactionType, categoryId: String, frequency: RecurringFrequency, remainingOccurrences: Int?) -> Unit,
    onDeleteRecurring: (RecurringTransactionEntity) -> Unit,
    onPostRecurring: (RecurringTransactionEntity) -> Unit,
    onUpdateRecurring: (RecurringTransactionEntity) -> Unit = {},
    onAddCustomCategory: (CategoryEntity) -> Unit,
    onAddCustomAccount: (AccountEntity) -> Unit,
    onUpdateAccount: (AccountEntity) -> Unit,
    onDeleteAccount: (AccountEntity) -> Unit,
    onAddTransfer: (fromAccountId: String, toAccountId: String, amount: Double, note: String?) -> Unit = { _, _, _, _ -> },
    onAddGoal: (name: String, targetAmount: Double, linkedAccountId: String?) -> Unit = { _, _, _ -> },
    onDeleteGoal: (com.selfbudget.app.data.model.GoalEntity) -> Unit = {},
    onContributeToGoal: (com.selfbudget.app.data.model.GoalEntity, Double) -> Unit = { _, _ -> },
    onUpdateGoal: (com.selfbudget.app.data.model.GoalEntity) -> Unit = {},
    onSetExchangeRate: (fromCurrency: String, toCurrency: String, rate: Double) -> Unit = { _, _, _ -> },
    onSetCurrency: (String) -> Unit,
    onSetThemeMode: (AppThemeMode) -> Unit,
    onSetBiometricEnabled: (Boolean) -> Unit,
    onExportBackupJson: ((String) -> Unit, (String) -> Unit) -> Unit = { _, _ -> },
    onRestoreBackupJson: (jsonString: String, onSuccess: (Int) -> Unit, onError: (String) -> Unit) -> Unit = { _, _, _ -> },
    onDriveSyncClick: (account: com.google.android.gms.auth.api.signin.GoogleSignInAccount, onResult: (String) -> Unit) -> Unit = { _, _ -> },
    onDriveRestoreClick: (account: com.google.android.gms.auth.api.signin.GoogleSignInAccount, onResult: (String) -> Unit) -> Unit = { _, _ -> },
    onResetData: () -> Unit = {},
    onSignOut: () -> Unit
) {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 5 })
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(pagerState.currentPage) {
        selectedTab = pagerState.currentPage
    }

    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showAddIncomeDialog by remember { mutableStateOf(false) }
    var showAddMenu by remember { mutableStateOf(false) }
    var pendingNewBudget by remember { mutableStateOf(false) }
    var pendingNewRecurring by remember { mutableStateOf(false) }
    var showProfileSettings by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var pendingDeleteTransaction by remember { mutableStateOf<TransactionEntity?>(null) }

    val userInitial = remember(uiState.user?.displayName, uiState.user?.email) {
        uiState.user?.displayName?.trim()?.firstOrNull()?.uppercase()
            ?: uiState.user?.email?.trim()?.firstOrNull()?.uppercase()
            ?: "U"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AppLogoBadge(size = 28.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Self Budget",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            val pageTitle = when (selectedTab) {
                                0 -> "Dashboard"
                                1 -> "Plan"
                                2 -> "Recurring"
                                3 -> "Analytics"
                                4 -> "Activity"
                                else -> ""
                            }
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = pageTitle,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        // Profile / Settings Top-Right Avatar Button
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .clickable { showProfileSettings = true }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = userInitial,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        coroutineScope.launch { pagerState.animateScrollToPage(0) }
                    },
                    icon = { Icon(Icons.Default.AccountBalance, contentDescription = "Home") },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        coroutineScope.launch { pagerState.animateScrollToPage(1) }
                    },
                    icon = { Icon(Icons.Default.PieChart, contentDescription = "Plan") },
                    label = { Text("Plan") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = {
                        selectedTab = 2
                        coroutineScope.launch { pagerState.animateScrollToPage(2) }
                    },
                    icon = { Icon(Icons.Default.Repeat, contentDescription = "Recurring") },
                    label = { Text("Recurring") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = {
                        selectedTab = 3
                        coroutineScope.launch { pagerState.animateScrollToPage(3) }
                    },
                    icon = { Icon(Icons.AutoMirrored.Filled.ShowChart, contentDescription = "Analytics") },
                    label = { Text("Analytics") }
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = {
                        selectedTab = 4
                        coroutineScope.launch { pagerState.animateScrollToPage(4) }
                    },
                    icon = { Icon(Icons.Default.History, contentDescription = "Activity") },
                    label = { Text("Activity") }
                )
            }
        },
        floatingActionButton = {
            // Single global entry point: every tab shares this one + button, which opens a
            // full-page chooser instead of each tab owning its own add button.
            FloatingActionButton(
                onClick = { showAddMenu = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { page ->
            when (page) {
                0 -> DashboardContent(
                    uiState = uiState,
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth,
                    onSelectMonthYear = onSelectMonthYear,
                    onEditTransaction = { tx -> editingTransaction = tx },
                    onDeleteTransaction = onDeleteTransaction,
                    onAddTransactionClick = { showAddMenu = true },
                    onAddCustomAccount = onAddCustomAccount,
                    onUpdateAccount = onUpdateAccount,
                    onDeleteAccount = onDeleteAccount,
                    onAddTransfer = onAddTransfer
                )
                1 -> BudgetScreen(
                    budgets = uiState.budgets,
                    categories = uiState.categories,
                    transactions = uiState.monthTransactions,
                    recurringList = uiState.recurringList,
                    currencySymbol = uiState.currencySymbol,
                    previousMonthBudgets = uiState.previousMonthBudgets,
                    previousMonthSpentByCategory = uiState.previousMonthSpentByCategory,
                    onSetBudget = onSetBudget,
                    onDeleteBudget = onDeleteBudget,
                    goals = uiState.goals,
                    accounts = uiState.accounts,
                    accountBalances = uiState.accountBalances,
                    onAddGoal = onAddGoal,
                    onDeleteGoal = onDeleteGoal,
                    onContributeToGoal = onContributeToGoal,
                    onUpdateGoal = onUpdateGoal,
                    onAddCustomCategory = onAddCustomCategory,
                    requestNewBudget = pendingNewBudget,
                    onNewBudgetRequestHandled = { pendingNewBudget = false }
                )
                2 -> RecurringScreen(
                    recurringList = uiState.recurringList,
                    categories = uiState.categories,
                    allTransactions = uiState.transactions,
                    currencySymbol = uiState.currencySymbol,
                    onAddRecurring = onAddRecurring,
                    onDeleteRecurring = onDeleteRecurring,
                    onPostTransaction = onPostRecurring,
                    onUpdateRecurring = onUpdateRecurring,
                    onAddCustomCategory = onAddCustomCategory,
                    requestNewRecurring = pendingNewRecurring,
                    onNewRecurringRequestHandled = { pendingNewRecurring = false }
                )
                3 -> AnalyticsScreen(
                    allTransactions = uiState.transactions,
                    categories = uiState.categories,
                    selectedMonthYear = uiState.selectedMonthYear,
                    currencySymbol = uiState.currencySymbol,
                    netWorthHistory = uiState.netWorthHistory,
                    accounts = uiState.accounts,
                    accountBalances = uiState.accountBalances
                )
                4 -> SearchScreen(
                    transactions = uiState.transactions,
                    categories = uiState.categories,
                    accounts = uiState.accounts,
                    currencySymbol = uiState.currencySymbol,
                    onDeleteTransaction = onDeleteTransaction,
                    onEditTransaction = { tx -> editingTransaction = tx }
                )
            }
        }

        if (showProfileSettings) {
            Dialog(
                onDismissRequest = { showProfileSettings = false },
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = false
                )
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp,
                            shadowElevation = 4.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Settings",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                IconButton(onClick = { showProfileSettings = false }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close Settings")
                                }
                            }
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            SettingsScreen(
                                user = uiState.user,
                                currencySymbol = uiState.currencySymbol,
                                themeMode = uiState.themeMode,
                                isBiometricEnabled = uiState.isBiometricEnabled,
                                transactions = uiState.transactions,
                                categories = uiState.categories,
                                accounts = uiState.accounts,
                                exchangeRates = uiState.exchangeRates,
                                onSetCurrency = onSetCurrency,
                                onSetThemeMode = onSetThemeMode,
                                onSetBiometricEnabled = onSetBiometricEnabled,
                                onSetExchangeRate = onSetExchangeRate,
                                onExportBackupJson = onExportBackupJson,
                                onRestoreBackupJson = onRestoreBackupJson,
                                onDriveSyncClick = onDriveSyncClick,
                                onDriveRestoreClick = onDriveRestoreClick,
                                onResetData = onResetData,
                                onSignOut = onSignOut
                            )
                        }
                    }
                }
            }
        }

        if (showAddMenu) {
            Dialog(
                onDismissRequest = { showAddMenu = false },
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = false
                )
            ) {
                AddEntryPointScreen(
                    onDismiss = { showAddMenu = false },
                    onPickIncome = {
                        showAddMenu = false
                        showAddIncomeDialog = true
                    },
                    onPickExpense = {
                        showAddMenu = false
                        showAddExpenseDialog = true
                    },
                    onPickBudget = {
                        showAddMenu = false
                        selectedTab = 1
                        coroutineScope.launch { pagerState.animateScrollToPage(1) }
                        pendingNewBudget = true
                    },
                    onPickRecurring = {
                        showAddMenu = false
                        selectedTab = 2
                        coroutineScope.launch { pagerState.animateScrollToPage(2) }
                        pendingNewRecurring = true
                    }
                )
            }
        }

        if (showAddExpenseDialog) {
            AddExpenseDialog(
                categories = uiState.categories,
                accounts = uiState.accounts,
                budgets = uiState.budgets,
                allTransactions = uiState.transactions,
                recurringList = uiState.recurringList,
                currencySymbol = uiState.currencySymbol,
                onDismiss = { showAddExpenseDialog = false },
                onConfirm = { title, amount, categoryId, accountId, note, timestamp, isRecurring, recurringFrequency, debtAccountId ->
                    onAddTransaction(title, amount, TransactionType.EXPENSE, categoryId, accountId, note, timestamp, isRecurring, recurringFrequency, debtAccountId)
                    showAddExpenseDialog = false
                },
                onSetCategoryBudget = { catId, limit ->
                    onSetBudget(catId, limit, false)
                },
                onAddCustomCategory = onAddCustomCategory,
                onAddCustomAccount = onAddCustomAccount
            )
        }

        if (showAddIncomeDialog) {
            AddIncomeDialog(
                categories = uiState.categories,
                accounts = uiState.accounts,
                allTransactions = uiState.transactions,
                recurringList = uiState.recurringList,
                currencySymbol = uiState.currencySymbol,
                onDismiss = { showAddIncomeDialog = false },
                onConfirm = { title, amount, categoryId, accountId, note, timestamp, isRecurring, recurringFrequency ->
                    onAddTransaction(title, amount, TransactionType.INCOME, categoryId, accountId, note, timestamp, isRecurring, recurringFrequency, null)
                    showAddIncomeDialog = false
                },
                onAddCustomCategory = onAddCustomCategory,
                onAddCustomAccount = onAddCustomAccount
            )
        }

        editingTransaction?.let { tx ->
            EditTransactionDialog(
                transaction = tx,
                categories = uiState.categories,
                accounts = uiState.accounts,
                budgets = uiState.budgets,
                recurringList = uiState.recurringList,
                currencySymbol = uiState.currencySymbol,
                onDismiss = { editingTransaction = null },
                onConfirmUpdate = { updatedTx ->
                    onUpdateTransaction(updatedTx)
                    editingTransaction = null
                },
                onDelete = { deletedTx ->
                    onDeleteTransaction(deletedTx)
                    editingTransaction = null
                },
                onAddRecurring = { title, amount, type, categoryId, frequency ->
                    onAddRecurring(title, amount, type, categoryId, frequency, null)
                },
                onDeleteRecurring = { rec ->
                    onDeleteRecurring(rec)
                },
                onSetCategoryBudget = { catId, limit ->
                    onSetBudget(catId, limit, false)
                },
                onDeleteCategoryBudget = { catId ->
                    onDeleteBudget(catId)
                },
                onAddCustomCategory = onAddCustomCategory,
                onAddCustomAccount = onAddCustomAccount
            )
        }

        // Delete Transaction Confirmation Modal
        if (pendingDeleteTransaction != null) {
            val txToDelete = pendingDeleteTransaction!!
            val isIncome = txToDelete.type == TransactionType.INCOME

            Dialog(
                onDismissRequest = { pendingDeleteTransaction = null },
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
                        // Glowing Red Trash Badge
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

                        // Highlighted Transaction Card
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
                                    text = "${if (isIncome) "+" else "-"}${uiState.currencySymbol}%.2f".format(txToDelete.amount),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isIncome) IncomeGreen else ExpenseRed
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Action Buttons Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { pendingDeleteTransaction = null },
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
                                    pendingDeleteTransaction = null
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
}

/**
 * Full-page chooser opened by the single global "+" (see HomeScreen's floatingActionButton).
 * Replaces separate per-tab add buttons: whichever card the user picks here is the only path
 * into that form, no matter which tab they started from.
 */
@Composable
private fun AddEntryPointScreen(
    onDismiss: () -> Unit,
    onPickIncome: () -> Unit,
    onPickExpense: () -> Unit,
    onPickBudget: () -> Unit,
    onPickRecurring: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "What do you want to add?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                AddEntryPointCard(
                    title = "Add Income",
                    subtitle = "Log a paycheck, freelance payment, or gift",
                    icon = Icons.Default.ArrowUpward,
                    iconBadgeColor = com.selfbudget.app.ui.theme.getIncomeColor().copy(alpha = 0.15f),
                    iconTint = com.selfbudget.app.ui.theme.getIncomeColor(),
                    onClick = onPickIncome
                )
                AddEntryPointCard(
                    title = "Add Expense",
                    subtitle = "Record something you spent money on",
                    icon = Icons.Default.ArrowDownward,
                    iconBadgeColor = com.selfbudget.app.ui.theme.getExpenseColor().copy(alpha = 0.15f),
                    iconTint = com.selfbudget.app.ui.theme.getExpenseColor(),
                    onClick = onPickExpense
                )
                AddEntryPointCard(
                    title = "Create a Budget",
                    subtitle = "Set a monthly spending limit for a category",
                    icon = Icons.Default.PieChart,
                    iconBadgeColor = MaterialTheme.colorScheme.secondaryContainer,
                    iconTint = MaterialTheme.colorScheme.secondary,
                    onClick = onPickBudget
                )
                AddEntryPointCard(
                    title = "Add a Recurring Bill",
                    subtitle = "Track a subscription, bill, or paycheck",
                    icon = Icons.Default.Repeat,
                    iconBadgeColor = MaterialTheme.colorScheme.tertiaryContainer,
                    iconTint = MaterialTheme.colorScheme.tertiary,
                    onClick = onPickRecurring
                )
            }
        }
    }
}

@Composable
private fun AddEntryPointCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBadgeColor: Color,
    iconTint: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = iconBadgeColor,
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun DashboardContent(
    uiState: HomeUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectMonthYear: (String) -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    onAddTransactionClick: () -> Unit = {},
    onAddCustomAccount: (AccountEntity) -> Unit = {},
    onUpdateAccount: (AccountEntity) -> Unit = {},
    onDeleteAccount: (AccountEntity) -> Unit = {},
    onAddTransfer: (fromAccountId: String, toAccountId: String, amount: Double, note: String?) -> Unit = { _, _, _, _ -> }
) {
    var isBalanceVisible by remember { mutableStateOf(true) }
    var showFullHistorySheet by remember { mutableStateOf(false) }
    var pendingDeleteTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var selectedAccountForEdit by remember { mutableStateOf<AccountEntity?>(null) }
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var showAllAccountsSheet by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }

    val categoryMap = remember(uiState.categories) {
        uiState.categories.associateBy { it.id }
    }
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()) }
    val sym = uiState.currencySymbol

    val allMonthTransactions = uiState.monthTransactions

    // Top 5 Recent Transactions Preview for Home Dashboard
    val recentPreview = remember(allMonthTransactions) {
        allMonthTransactions.take(5)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            MonthYearHeader(
                currentMonthYear = uiState.selectedMonthYear,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
                onSelectMonthYear = onSelectMonthYear
            )
        }

        // Monthly Cash Flow Overview Card (Income - Budgets - Unbudgeted Fixed Bills - Goals = Unassigned Free Cash)
        item {
            val loggedIncomeTxs = remember(allMonthTransactions) {
                allMonthTransactions.filter { it.type == TransactionType.INCOME }
            }

            val effectiveMonthlyIncome = remember(loggedIncomeTxs, uiState.recurringList) {
                IncomeCalculator.computeEffectiveMonthlyIncome(loggedIncomeTxs, uiState.recurringList)
            }

            val totalLoggedIncome = remember(loggedIncomeTxs) {
                Money.sum(loggedIncomeTxs.map { it.amount })
            }

            val budgetedCategoryIds = remember(uiState.budgets) {
                uiState.budgets.map { it.categoryId }.toSet()
            }

            val previousBudgetMap = remember(uiState.previousMonthBudgets) {
                uiState.previousMonthBudgets.associateBy { it.categoryId }
            }

            // Category Budgets: Using Effective Limit (with rollover) AND ensuring max(Effective Limit, Recurring Bill) per category (Issue 2 & Issue 5)
            val totalCategoryBudgets = remember(uiState.budgets, previousBudgetMap, uiState.previousMonthSpentByCategory, uiState.recurringList) {
                val expenseRecurring = uiState.recurringList.filter { it.type == TransactionType.EXPENSE && !it.isArchived }
                val recurringMap = expenseRecurring.groupBy { it.categoryId }.mapValues { entry ->
                    Money.sum(entry.value.map { rec ->
                        RecurringFrequencyNormalizer.toMonthlyAmount(rec.amount, rec.frequency)
                    })
                }

                Money.sum(uiState.budgets.map { budget ->
                    val ownLimit = budget.amountLimit
                    val prevLimit = previousBudgetMap[budget.categoryId]?.amountLimit ?: 0.0
                    val prevSpent = uiState.previousMonthSpentByCategory[budget.categoryId] ?: 0.0
                    val effectiveLimit = BudgetRollover.effectiveLimit(ownLimit, budget.rolloverEnabled, prevLimit, prevSpent)
                    val recBill = recurringMap[budget.categoryId] ?: 0.0
                    maxOf(effectiveLimit, recBill)
                })
            }

            // Unbudgeted Bills & Expenses (recurring bills OR actual logged expense transactions in categories without an explicit budget limit)
            val unbudgetedBillsAndExpenses = remember(uiState.recurringList, allMonthTransactions, budgetedCategoryIds) {
                val unbudgetedCategoryIds = (
                    allMonthTransactions.filter { it.type == TransactionType.EXPENSE }.map { it.categoryId } +
                    uiState.recurringList.filter { it.type == TransactionType.EXPENSE && !it.isArchived }.map { it.categoryId }
                )
                    .filter { it !in budgetedCategoryIds }
                    .toSet()

                Money.sum(unbudgetedCategoryIds.map { catId ->
                    val recurringMonthly = Money.sum(
                        uiState.recurringList
                            .filter { it.type == TransactionType.EXPENSE && !it.isArchived && it.categoryId == catId }
                            .map { rec -> RecurringFrequencyNormalizer.toMonthlyAmount(rec.amount, rec.frequency) }
                    )
                    val actualSpent = Money.sum(
                        allMonthTransactions
                            .filter { it.type == TransactionType.EXPENSE && it.categoryId == catId }
                            .map { it.amount }
                    )

                    maxOf(recurringMonthly, actualSpent)
                })
            }

            // Monthly Goal Savings Commitments (Issue 12)
            val totalGoalCommitments = remember(uiState.goals) {
                val now = System.currentTimeMillis()
                Money.sum(uiState.goals.map { goal ->
                    val targetDate = goal.targetDate
                    if (goal.targetAmount <= goal.savedAmount) 0.0
                    else if (targetDate != null && targetDate > now) {
                        val calNow = Calendar.getInstance().apply { timeInMillis = now }
                        val calTarget = Calendar.getInstance().apply { timeInMillis = targetDate }
                        val monthsLeft = ((calTarget.get(Calendar.YEAR) - calNow.get(Calendar.YEAR)) * 12 +
                                (calTarget.get(Calendar.MONTH) - calNow.get(Calendar.MONTH))).coerceAtLeast(1)
                        Money.subtract(goal.targetAmount, goal.savedAmount) / monthsLeft
                    } else {
                        0.0
                    }
                })
            }

            val totalMonthlySpent = remember(allMonthTransactions) {
                Money.sum(allMonthTransactions.filter { it.type == TransactionType.EXPENSE }.map { it.amount })
            }

            val totalCommittedAllocated = Money.sum(listOf(totalCategoryBudgets, unbudgetedBillsAndExpenses, totalGoalCommitments))
            val hasIncomeLogged = effectiveMonthlyIncome > 0.0
            val unallocatedFreeCash = Money.subtract(effectiveMonthlyIncome, totalCommittedAllocated)

            androidx.compose.material3.Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Monthly Cash Flow",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(
                            onClick = { isBalanceVisible = !isBalanceVisible },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (isBalanceVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle Balance Privacy",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Free Cash / Over Budget headline figure
                    Text(
                        text = if (!isBalanceVisible) "$sym ••••••"
                        else if (!hasIncomeLogged) "$sym%.2f Allocated".format(totalCommittedAllocated)
                        else if (unallocatedFreeCash >= 0) "$sym%.2f Free".format(unallocatedFreeCash)
                        else "$sym%.2f Over Budget".format(-unallocatedFreeCash),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (!hasIncomeLogged) MaterialTheme.colorScheme.primary
                        else if (unallocatedFreeCash >= 0) IncomeGreen
                        else ExpenseRed
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val maxCashFlowVal = maxOf(effectiveMonthlyIncome, totalCategoryBudgets, unbudgetedBillsAndExpenses, totalMonthlySpent)
                    val dynamicNumFontSize = when {
                        maxCashFlowVal >= 1_000_000.0 -> 10.5.sp
                        maxCashFlowVal >= 100_000.0 -> 12.sp
                        else -> 13.5.sp
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                            Text(
                                text = if (totalLoggedIncome < effectiveMonthlyIncome) "Income (Est.)" else "Income",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isBalanceVisible) "$sym%.2f".format(effectiveMonthlyIncome) else "$sym •••",
                                fontWeight = FontWeight.Bold,
                                fontSize = dynamicNumFontSize,
                                letterSpacing = (-0.3).sp,
                                maxLines = 1,
                                softWrap = false,
                                color = IncomeGreen
                            )
                        }
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Budgets", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isBalanceVisible) "$sym%.2f".format(totalCategoryBudgets) else "$sym •••",
                                fontWeight = FontWeight.Bold,
                                fontSize = dynamicNumFontSize,
                                letterSpacing = (-0.3).sp,
                                maxLines = 1,
                                softWrap = false,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Unbudgeted", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isBalanceVisible) "$sym%.2f".format(unbudgetedBillsAndExpenses) else "$sym •••",
                                fontWeight = FontWeight.Bold,
                                fontSize = dynamicNumFontSize,
                                letterSpacing = (-0.3).sp,
                                maxLines = 1,
                                softWrap = false,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        val expectedCommitments = totalCategoryBudgets + unbudgetedBillsAndExpenses
                        val isPaidOverSpent = (expectedCommitments > 0 && totalMonthlySpent > expectedCommitments) ||
                                (effectiveMonthlyIncome > 0 && totalMonthlySpent > effectiveMonthlyIncome)
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                            Text("Paid", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isBalanceVisible) "$sym%.2f".format(totalMonthlySpent) else "$sym •••",
                                fontWeight = FontWeight.Bold,
                                fontSize = dynamicNumFontSize,
                                letterSpacing = (-0.3).sp,
                                maxLines = 1,
                                softWrap = false,
                                color = if (isPaidOverSpent) ExpenseRed else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    if (!hasIncomeLogged && totalCommittedAllocated > 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Log monthly income to calculate free cash.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                        )
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Accounts & Wallets",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (uiState.accounts.size >= 2) {
                            TextButton(onClick = { showTransferDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.SwapHoriz,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Transfer", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        TextButton(onClick = { showAllAccountsSheet = true }) {
                            Text(
                                text = "View All (${uiState.accounts.size}) ›",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(uiState.accounts.take(3)) { acc ->
                        val accColor = try {
                            Color(android.graphics.Color.parseColor(acc.colorHex))
                        } catch (e: Exception) {
                            MaterialTheme.colorScheme.primary
                        }

                        val icon = when (acc.type) {
                            AccountType.CREDIT_CARD -> Icons.Default.CreditCard
                            AccountType.CASH -> Icons.Default.Payments
                            AccountType.SAVINGS -> Icons.Default.Savings
                            else -> Icons.Default.AccountBalance
                        }

                        androidx.compose.material3.Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .width(135.dp)
                                .clickable { selectedAccountForEdit = acc }
                        ) {
                            Column(modifier = Modifier.padding(11.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(accColor.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = accColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = acc.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                val accSym = com.selfbudget.app.core.util.Currencies.symbolFor(acc.currencyCode).ifBlank { sym }
                                val liveBalance = uiState.accountBalances[acc.id] ?: acc.initialBalance
                                Text(
                                    text = if (isBalanceVisible) "$accSym%.2f".format(liveBalance) else "$accSym ••••••",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    item {
                        androidx.compose.material3.Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .width(60.dp)
                                .height(88.dp)
                                .clickable { showAddAccountDialog = true }
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add Account",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section Header: "Recent Activity" + Clickable "View All (X) ›"
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Recent Activity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (allMonthTransactions.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "Top ${recentPreview.size} of ${allMonthTransactions.size}",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (allMonthTransactions.isNotEmpty()) {
                    TextButton(onClick = { showFullHistorySheet = true }) {
                        Text(
                            text = "View All (${allMonthTransactions.size}) ›",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        if (recentPreview.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Receipt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "No Activity Logged Yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Log your first income or expense entry to start tracking your net balance and cash flow.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = onAddTransactionClick,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = IncomeGreen,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Log First Transaction", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        } else {
            // Render Top 5 Recent Items Preview
            items(recentPreview, key = { it.id }) { transaction ->
                val category = categoryMap[transaction.categoryId]
                androidx.compose.material3.Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEditTransaction(transaction) },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            val isIncome = transaction.type == TransactionType.INCOME
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isIncome) IncomeGreen.copy(alpha = 0.15f)
                                        else ExpenseRed.copy(alpha = 0.15f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (category != null) getCategoryIcon(category)
                                                  else if (isIncome) Icons.Default.ArrowDownward
                                                  else Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = if (isIncome) IncomeGreen else ExpenseRed,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = transaction.title,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "${category?.name ?: "General"} • ${dateFormat.format(Date(transaction.timestamp))}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val isIncome = transaction.type == TransactionType.INCOME
                            val amountPrefix = if (isIncome) "+$sym" else "-$sym"
                            val amountColor = if (isIncome) (if (isSystemInDarkTheme()) Color(0xFF4ADE80) else IncomeGreen) else (if (isSystemInDarkTheme()) Color(0xFFF87171) else ExpenseRed)

                            Text(
                                text = if (isBalanceVisible) "$amountPrefix%.2f".format(transaction.amount) else "$sym ••••••",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = amountColor
                            )
                        }
                    }
                }
            }

            // Bottom "See All Transactions" Card Button if total entries > 5
            if (allMonthTransactions.size > 5) {
                item {
                    OutlinedCard(
                        onClick = { showFullHistorySheet = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "See All ${allMonthTransactions.size} Transactions",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(150.dp))
        }
    }

    // Full Transaction History Modal Dialog with Search & Category Filter Chips
    if (showFullHistorySheet) {
        FullTransactionHistoryDialog(
            transactions = allMonthTransactions,
            categories = uiState.categories,
            currencySymbol = sym,
            selectedMonthYear = uiState.selectedMonthYear,
            isBalanceVisible = isBalanceVisible,
            onDismiss = { showFullHistorySheet = false },
            onEditTransaction = { tx ->
                showFullHistorySheet = false
                onEditTransaction(tx)
            },
            onDeleteTransaction = { tx ->
                pendingDeleteTransaction = tx
            }
        )
    }

    // Delete Transaction Confirmation Modal
    if (pendingDeleteTransaction != null) {
        val txToDelete = pendingDeleteTransaction!!
        val isIncome = txToDelete.type == TransactionType.INCOME

        Dialog(
            onDismissRequest = { pendingDeleteTransaction = null },
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
                    // Glowing Red Trash Badge
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

                    // Highlighted Transaction Card
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
                                text = "${if (isIncome) "+" else "-"}$sym%.2f".format(txToDelete.amount),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isIncome) IncomeGreen else ExpenseRed
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Action Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { pendingDeleteTransaction = null },
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
                                pendingDeleteTransaction = null
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

    selectedAccountForEdit?.let { acc ->
        EditCustomAccountDialog(
            account = acc,
            currencySymbol = uiState.currencySymbol,
            onDismiss = { selectedAccountForEdit = null },
            onConfirm = { updatedAcc ->
                onUpdateAccount(updatedAcc)
                selectedAccountForEdit = null
            },
            onDelete = { deletedAcc ->
                onDeleteAccount(deletedAcc)
                selectedAccountForEdit = null
            }
        )
    }

    if (showAddAccountDialog) {
        AddCustomAccountDialog(
            currencySymbol = uiState.currencySymbol,
            onDismiss = { showAddAccountDialog = false },
            onConfirm = { newAcc ->
                onAddCustomAccount(newAcc)
                showAddAccountDialog = false
            }
        )
    }

    if (showAllAccountsSheet) {
        com.selfbudget.app.core.ui.AccountsViewAllModal(
            accounts = uiState.accounts,
            currencySymbol = uiState.currencySymbol,
            accountBalances = uiState.accountBalances,
            onDismiss = { showAllAccountsSheet = false },
            onEditAccount = { acc ->
                showAllAccountsSheet = false
                selectedAccountForEdit = acc
            },
            onAddAccount = {
                showAllAccountsSheet = false
                showAddAccountDialog = true
            }
        )
    }

    if (showTransferDialog) {
        TransferDialog(
            accounts = uiState.accounts,
            accountBalances = uiState.accountBalances,
            onDismiss = { showTransferDialog = false },
            onConfirm = { fromId, toId, amount, note ->
                onAddTransfer(fromId, toId, amount, note)
                showTransferDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullTransactionHistoryDialog(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    currencySymbol: String,
    selectedMonthYear: String,
    isBalanceVisible: Boolean,
    onDismiss: () -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf<TransactionType?>(null) }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }

    val categoryMap = remember(categories) { categories.associateBy { it.id } }
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()) }

    val filteredList = remember(transactions, searchQuery, selectedTypeFilter, selectedCategoryId) {
        transactions.filter { tx ->
            val matchesQuery = searchQuery.isBlank() ||
                    tx.title.contains(searchQuery, ignoreCase = true) ||
                    (tx.note?.contains(searchQuery, ignoreCase = true) == true)
            val matchesType = selectedTypeFilter == null || tx.type == selectedTypeFilter
            val matchesCategory = selectedCategoryId == null || tx.categoryId == selectedCategoryId

            matchesQuery && matchesType && matchesCategory
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnClickOutside = true,
            dismissOnBackPress = true,
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
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "All Transactions",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${filteredList.size} of ${transactions.size} entries ($selectedMonthYear)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                )

                // Search Bar & Filter Chips inside Full History Sheet
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search transactions...") },
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

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = selectedTypeFilter == null && selectedCategoryId == null,
                                onClick = {
                                    selectedTypeFilter = null
                                    selectedCategoryId = null
                                },
                                label = { Text("All") }
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedTypeFilter == TransactionType.EXPENSE && selectedCategoryId == null,
                                onClick = {
                                    selectedTypeFilter = if (selectedTypeFilter == TransactionType.EXPENSE) null else TransactionType.EXPENSE
                                    selectedCategoryId = null
                                },
                                label = { Text("Expenses 🔴") }
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedTypeFilter == TransactionType.INCOME && selectedCategoryId == null,
                                onClick = {
                                    selectedTypeFilter = if (selectedTypeFilter == TransactionType.INCOME) null else TransactionType.INCOME
                                    selectedCategoryId = null
                                },
                                label = { Text("Income 🟢") }
                            )
                        }
                        items(categories) { cat ->
                            FilterChip(
                                selected = selectedCategoryId == cat.id,
                                onClick = {
                                    if (selectedCategoryId == cat.id) {
                                        selectedCategoryId = null
                                    } else {
                                        selectedCategoryId = cat.id
                                        selectedTypeFilter = cat.type
                                    }
                                },
                                label = { Text(cat.name) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (filteredList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No matching transactions found.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredList, key = { it.id }) { transaction ->
                            val category = categoryMap[transaction.categoryId]
                            val isIncome = transaction.type == TransactionType.INCOME
                            val amountPrefix = if (isIncome) "+$currencySymbol" else "-$currencySymbol"
                            val amountColor = if (isIncome) (if (isSystemInDarkTheme()) Color(0xFF4ADE80) else IncomeGreen) else (if (isSystemInDarkTheme()) Color(0xFFF87171) else ExpenseRed)

                            androidx.compose.material3.Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onEditTransaction(transaction) },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isIncome) IncomeGreen.copy(alpha = 0.15f)
                                                    else ExpenseRed.copy(alpha = 0.15f)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (isIncome) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                                contentDescription = null,
                                                tint = if (isIncome) IncomeGreen else ExpenseRed,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = transaction.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                            Text(
                                                text = "${category?.name ?: "General"} • ${dateFormat.format(Date(transaction.timestamp))}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (isBalanceVisible) "$amountPrefix%.2f".format(transaction.amount) else "$currencySymbol ••••••",
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
        }
    }
}
