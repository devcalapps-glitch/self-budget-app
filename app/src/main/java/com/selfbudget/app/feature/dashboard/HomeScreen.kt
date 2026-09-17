package com.selfbudget.app.feature.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import com.selfbudget.app.core.util.RecurringCycleCalculator
import com.selfbudget.app.core.util.RecurringFrequencyNormalizer
import java.util.Calendar
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.FilterChipDefaults
import com.selfbudget.app.feature.search.ActiveFilterPill
import com.selfbudget.app.feature.search.DateRangeFilter
import com.selfbudget.app.feature.search.FilterChipGroup
import com.selfbudget.app.feature.search.SortOption
import com.selfbudget.app.feature.search.TypeFilterChip
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.selfbudget.app.core.ui.AddCustomAccountDialog
import com.selfbudget.app.core.ui.components.CircularBackButton
import com.selfbudget.app.core.ui.CompactMonthYearHeader
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
import com.selfbudget.app.core.ui.components.DeltaBadge
import com.selfbudget.app.core.ui.components.DeltaMetric
import com.selfbudget.app.core.ui.components.GrayIconTile
import com.selfbudget.app.core.ui.components.IconTile
import com.selfbudget.app.core.ui.components.NeutralBadge
import com.selfbudget.app.core.ui.components.PrimaryPillButton
import com.selfbudget.app.core.ui.components.RampIconTile
import com.selfbudget.app.core.ui.components.SecondaryPillButton
import com.selfbudget.app.core.ui.components.SectionHeaderBand
import com.selfbudget.app.core.ui.components.SectionRowDivider
import com.selfbudget.app.core.ui.components.StatusBadge
import com.selfbudget.app.core.ui.components.StatusProgressBar
import com.selfbudget.app.ui.theme.BudgetStatus
import com.selfbudget.app.ui.theme.Ramp
import com.selfbudget.app.ui.theme.CardSurfaceDark
import com.selfbudget.app.ui.theme.DividerDark
import com.selfbudget.app.ui.theme.SelfBudgetType
import com.selfbudget.app.ui.theme.ShapeCard
import com.selfbudget.app.ui.theme.ShapeChip
import com.selfbudget.app.ui.theme.ShapeHero
import com.selfbudget.app.ui.theme.ShapePill
import com.selfbudget.app.ui.theme.TextPrimaryDark
import com.selfbudget.app.ui.theme.TextSecondaryDark
import com.selfbudget.app.ui.theme.WarningAmberDark
import com.selfbudget.app.ui.theme.budgetStatus
import com.selfbudget.app.ui.theme.containerBorder
import com.selfbudget.app.ui.theme.getAccentColor
import com.selfbudget.app.ui.theme.getBrandColor
import com.selfbudget.app.ui.theme.getExpenseColor
import com.selfbudget.app.ui.theme.getIncomeColor
import com.selfbudget.app.ui.theme.getWarningColor
import com.selfbudget.app.ui.theme.icon
import com.selfbudget.app.ui.theme.isAppInDarkTheme
import com.selfbudget.app.ui.theme.onSolidFill
import com.selfbudget.app.ui.theme.pillFill
import com.selfbudget.app.ui.theme.pillText
import com.selfbudget.app.ui.theme.sectionRamp
import com.selfbudget.app.ui.theme.secondaryText
import com.selfbudget.app.ui.theme.solidFill
import com.selfbudget.app.ui.theme.tintFill
import com.selfbudget.app.ui.theme.titleText
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
    onAddRecurring: (title: String, amount: Double, type: TransactionType, categoryId: String, frequency: RecurringFrequency, remainingOccurrences: Int?, nextDueDate: Long?, transferAccountId: String?) -> Unit,
    onDeleteRecurring: (RecurringTransactionEntity) -> Unit,
    onPostRecurring: (RecurringTransactionEntity, Double, String?) -> Unit,
    onUpdateRecurring: (RecurringTransactionEntity) -> Unit = {},
    onAddCustomCategory: (CategoryEntity) -> Unit,
    onToggleCategoryArchive: (CategoryEntity) -> Unit = {},
    onAddCustomAccount: (AccountEntity) -> Unit,
    onUpdateAccount: (AccountEntity) -> Unit,
    onDeleteAccount: (AccountEntity) -> Unit,
    onAddTransfer: (fromAccountId: String, toAccountId: String, amount: Double, note: String?) -> Unit = { _, _, _, _ -> },
    onAddGoal: (
        name: String,
        targetAmount: Double,
        linkedAccountId: String?,
        targetDate: Long?,
        monthlyTargetAmount: Double?,
        recurringFromAccountId: String?,
        recurringFrequency: RecurringFrequency?,
        recurringAmount: Double?
    ) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onDeleteGoal: (com.selfbudget.app.data.model.GoalEntity) -> Unit = {},
    onContributeToGoal: (com.selfbudget.app.data.model.GoalEntity, Double) -> Unit = { _, _ -> },
    onUpdateGoal: (com.selfbudget.app.data.model.GoalEntity) -> Unit = {},
    onSetExchangeRate: (fromCurrency: String, toCurrency: String, rate: Double) -> Unit = { _, _, _ -> },
    onSetCurrency: (String) -> Unit,
    onSetThemeMode: (AppThemeMode) -> Unit,
    onSetBiometricEnabled: (Boolean) -> Unit,
    onExportBackupJson: ((String) -> Unit, (String) -> Unit) -> Unit = { _, _ -> },
    onRestoreBackupJson: (jsonString: String, onSuccess: (Int) -> Unit, onError: (String) -> Unit) -> Unit = { _, _, _ -> },
    onImportData: ((com.selfbudget.app.core.util.ParsedImportData, (Int) -> Unit, (String) -> Unit) -> Unit)? = null,
    onDriveSyncClick: (account: com.google.android.gms.auth.api.signin.GoogleSignInAccount, onResult: (String) -> Unit) -> Unit = { _, _ -> },
    onDriveRestoreClick: (account: com.google.android.gms.auth.api.signin.GoogleSignInAccount, onResult: (String) -> Unit) -> Unit = { _, _ -> },
    onResetData: () -> Unit = {},
    onResetTransactionsOnly: () -> Unit = {},
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
    var showTransferDialog by remember { mutableStateOf(false) }
    var showAddRecurringDialog by remember { mutableStateOf(false) }
    var addRecurringInitialType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var showAddAccountFromMenuDialog by remember { mutableStateOf(false) }
    var pendingAccountInitialType by remember { mutableStateOf(AccountType.CHECKING) }
    var showAddMenu by remember { mutableStateOf(false) }
    var pendingNewBudget by remember { mutableStateOf(false) }
    var pendingNewRecurring by remember { mutableStateOf(false) }
    var pendingNewRecurringType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var showProfileSettings by remember { mutableStateOf(false) }
    var showPlanReviewModal by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var pendingDeleteTransaction by remember { mutableStateOf<TransactionEntity?>(null) }

    val userInitial = remember(uiState.user?.displayName, uiState.user?.email) {
        uiState.user?.displayName?.trim()?.firstOrNull()?.uppercase()
            ?: uiState.user?.email?.trim()?.firstOrNull()?.uppercase()
            ?: "U"
    }
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            AppLogoBadge(size = 36.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Self Budget",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        CompactMonthYearHeader(
                            currentMonthYear = uiState.selectedMonthYear,
                            onSelectMonthYear = onSelectMonthYear
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        // Profile / Settings Top-Right Avatar Button
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.5.dp, getAccentColor().copy(alpha = 0.4f)),
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .clickable { showProfileSettings = true }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = userInitial,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = getAccentColor()
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
            // Teal is the app's one brand/interactive color (design system §18):
            // the active tab reads the same Teal as links, active filters, and buttons.
            val isDark = isAppInDarkTheme()
            val navItemColors = NavigationBarItemDefaults.colors(
                selectedIconColor = getAccentColor(),
                selectedTextColor = getAccentColor(),
                indicatorColor = Ramp.Teal.tintFill(isDark)
            )
            Column {
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.background
                ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        coroutineScope.launch { pagerState.animateScrollToPage(0) }
                    },
                    icon = { Icon(Icons.Default.AccountBalance, contentDescription = "Home") },
                    label = { Text("Home") },
                    colors = navItemColors
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        coroutineScope.launch { pagerState.animateScrollToPage(1) }
                    },
                    icon = { Icon(Icons.Default.PieChart, contentDescription = "Plan") },
                    label = { Text("Plan") },
                    colors = navItemColors
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = {
                        selectedTab = 2
                        coroutineScope.launch { pagerState.animateScrollToPage(2) }
                    },
                    icon = { Icon(Icons.Default.Repeat, contentDescription = "Recurring") },
                    label = { Text("Recurring") },
                    colors = navItemColors
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = {
                        selectedTab = 3
                        coroutineScope.launch { pagerState.animateScrollToPage(3) }
                    },
                    icon = { Icon(Icons.AutoMirrored.Filled.ShowChart, contentDescription = "Analytics") },
                    label = { Text("Analytics") },
                    colors = navItemColors
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = {
                        selectedTab = 4
                        coroutineScope.launch { pagerState.animateScrollToPage(4) }
                    },
                    icon = { Icon(Icons.Default.History, contentDescription = "Activity") },
                    label = { Text("Activity") },
                    colors = navItemColors
                )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddMenu = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(26.dp))
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
                    onAddTransfer = onAddTransfer,
                    onReviewPlan = { showPlanReviewModal = true }
                )
                1 -> BudgetScreen(
                    budgets = uiState.budgets,
                    categories = uiState.categories,
                    transactions = uiState.monthTransactions,
                    recurringList = uiState.recurringList,
                    currencySymbol = uiState.currencySymbol,
                    previousMonthBudgets = uiState.previousMonthBudgets,
                    previousMonthSpentByCategory = uiState.previousMonthSpentByCategory,
                    selectedMonthYear = uiState.selectedMonthYear,
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth,
                    onSelectMonthYear = onSelectMonthYear,
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
                    onAddCustomAccount = onAddCustomAccount,
                    requestNewBudget = pendingNewBudget,
                    onNewBudgetRequestHandled = { pendingNewBudget = false },
                    isSelected = pagerState.currentPage == 1
                )
                2 -> RecurringScreen(
                    recurringList = uiState.recurringList,
                    categories = uiState.categories,
                    allTransactions = uiState.transactions,
                    selectedMonthYear = uiState.selectedMonthYear,
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth,
                    onSelectMonthYear = onSelectMonthYear,
                    currencySymbol = uiState.currencySymbol,
                    accounts = uiState.accounts,
                    accountBalances = uiState.accountBalances,
                    goals = uiState.goals,
                    onAddRecurring = onAddRecurring,
                    onDeleteRecurring = onDeleteRecurring,
                    onPostTransaction = onPostRecurring,
                    onUpdateRecurring = onUpdateRecurring,
                    onAddCustomCategory = onAddCustomCategory,
                    onAddCustomAccount = onAddCustomAccount,
                    requestNewRecurring = pendingNewRecurring,
                    requestNewRecurringType = pendingNewRecurringType,
                    onNewRecurringRequestHandled = { pendingNewRecurring = false }
                )
                3 -> AnalyticsScreen(
                    allTransactions = uiState.transactions,
                    categories = uiState.categories,
                    selectedMonthYear = uiState.selectedMonthYear,
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth,
                    onSelectMonthYear = onSelectMonthYear,
                    currencySymbol = uiState.currencySymbol,
                    netWorthHistory = uiState.netWorthHistory,
                    accounts = uiState.accounts,
                    accountBalances = uiState.accountBalances,
                    goals = uiState.goals
                )
                4 -> SearchScreen(
                    transactions = uiState.transactions,
                    categories = uiState.categories,
                    accounts = uiState.accounts,
                    goals = uiState.goals,
                    recurringTransactions = uiState.recurringList,
                    activityLog = uiState.activityLog,
                    currencySymbol = uiState.currencySymbol,
                    onDeleteTransaction = onDeleteTransaction,
                    onEditTransaction = { tx -> editingTransaction = tx },
                    onAddClick = { showAddMenu = true }
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
                    SettingsScreen(
                        user = uiState.user,
                        currencySymbol = uiState.currencySymbol,
                        themeMode = uiState.themeMode,
                        isBiometricEnabled = uiState.isBiometricEnabled,
                        transactions = uiState.transactions,
                        categories = uiState.categories,
                        accounts = uiState.accounts,
                        recurringList = uiState.recurringList,
                        budgets = uiState.budgets,
                        goals = uiState.goals,
                        accountBalances = uiState.accountBalances,
                        exchangeRates = uiState.exchangeRates,
                        onSetCurrency = onSetCurrency,
                        onSetThemeMode = onSetThemeMode,
                        onSetBiometricEnabled = onSetBiometricEnabled,
                        onSetExchangeRate = onSetExchangeRate,
                        onExportBackupJson = onExportBackupJson,
                        onRestoreBackupJson = onRestoreBackupJson,
                        onImportData = onImportData,
                        onDriveSyncClick = onDriveSyncClick,
                        onDriveRestoreClick = onDriveRestoreClick,
                        onResetData = onResetData,
                        onResetTransactionsOnly = onResetTransactionsOnly,
                        onToggleCategoryArchive = onToggleCategoryArchive,
                        onSignOut = {
                            showProfileSettings = false
                            onSignOut()
                        },
                        onDismiss = { showProfileSettings = false }
                    )
                }
            }
        }

        if (showAddMenu) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { showAddMenu = false },
                properties = androidx.compose.ui.window.DialogProperties(
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
                    onPickTransfer = {
                        showAddMenu = false
                        showTransferDialog = true
                    },
                    onPickBudget = {
                        showAddMenu = false
                        selectedTab = 1
                        coroutineScope.launch { pagerState.animateScrollToPage(1) }
                        pendingNewBudget = true
                    },
                    onPickRecurringIncome = {
                        showAddMenu = false
                        addRecurringInitialType = TransactionType.INCOME
                        showAddRecurringDialog = true
                    },
                    onPickRecurringExpense = {
                        showAddMenu = false
                        addRecurringInitialType = TransactionType.EXPENSE
                        showAddRecurringDialog = true
                    },
                    onPickGoal = {
                        showAddMenu = false
                        showAddGoalDialog = true
                    },
                    onPickAccount = {
                        showAddMenu = false
                        pendingAccountInitialType = AccountType.CHECKING
                        showAddAccountFromMenuDialog = true
                    },
                    onPickAsset = {
                        showAddMenu = false
                        pendingAccountInitialType = AccountType.INVESTMENT
                        showAddAccountFromMenuDialog = true
                    }
                )
            }
        }

        if (showAddRecurringDialog) {
            com.selfbudget.app.feature.recurring.SetRecurringDialog(
                categories = uiState.categories,
                currencySymbol = uiState.currencySymbol,
                accounts = uiState.accounts,
                accountBalances = uiState.accountBalances,
                initialType = addRecurringInitialType,
                onDismiss = { showAddRecurringDialog = false },
                onConfirm = { title, amount, type, categoryId, frequency, remainingOccurrences, nextDueDate, transferAccountId ->
                    onAddRecurring(title, amount, type, categoryId, frequency, remainingOccurrences, nextDueDate, transferAccountId)
                    showAddRecurringDialog = false
                },
                onAddCustomCategory = onAddCustomCategory,
                onAddCustomAccount = onAddCustomAccount
            )
        }

        if (showAddGoalDialog) {
            com.selfbudget.app.feature.dashboard.AddGoalDialog(
                accounts = uiState.accounts,
                accountBalances = uiState.accountBalances,
                currencySymbol = uiState.currencySymbol,
                onDismiss = { showAddGoalDialog = false },
                onConfirm = { name, targetAmount, linkedAccountId, targetDate, monthlyTargetAmount, recFromAcc, recFreq, recAmt ->
                    showAddGoalDialog = false
                    onAddGoal(name, targetAmount, linkedAccountId, targetDate, monthlyTargetAmount, recFromAcc, recFreq, recAmt)
                }
            )
        }

        if (showPlanReviewModal) {
            com.selfbudget.app.core.ui.PlanReviewModal(
                budgets = uiState.budgets,
                categories = uiState.categories,
                transactions = uiState.monthTransactions,
                recurringList = uiState.recurringList,
                currencySymbol = uiState.currencySymbol,
                onSetBudget = onSetBudget,
                onNavigateToFullBudget = {
                    showPlanReviewModal = false
                    selectedTab = 1
                    coroutineScope.launch { pagerState.animateScrollToPage(1) }
                },
                onDismiss = { showPlanReviewModal = false }
            )
        }

        if (showAddAccountFromMenuDialog) {
            com.selfbudget.app.core.ui.AddCustomAccountDialog(
                currencySymbol = uiState.currencySymbol,
                initialType = pendingAccountInitialType,
                onDismiss = { showAddAccountFromMenuDialog = false },
                onConfirm = { newAcc ->
                    onAddCustomAccount(newAcc)
                    showAddAccountFromMenuDialog = false
                }
            )
        }

        if (showAddExpenseDialog) {
            AddExpenseDialog(
                categories = uiState.categories,
                accounts = uiState.accounts,
                accountBalances = uiState.accountBalances,
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
                accountBalances = uiState.accountBalances,
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

        if (showTransferDialog) {
            TransferDialog(
                accounts = uiState.accounts,
                accountBalances = uiState.accountBalances,
                currencySymbol = uiState.currencySymbol,
                onDismiss = { showTransferDialog = false },
                onConfirm = { fromId, toId, amount, note ->
                    onAddTransfer(fromId, toId, amount, note)
                    showTransferDialog = false
                }
            )
        }

        editingTransaction?.let { tx ->
            EditTransactionDialog(
                transaction = tx,
                categories = uiState.categories,
                accounts = uiState.accounts,
                accountBalances = uiState.accountBalances,
                budgets = uiState.budgets,
                recurringList = uiState.recurringList,
                allTransactions = uiState.transactions,
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
                    onAddRecurring(title, amount, type, categoryId, frequency, null, null, null)
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
            TransactionDeleteConfirmDialog(
                transaction = pendingDeleteTransaction!!,
                currencySymbol = uiState.currencySymbol,
                onDismiss = { pendingDeleteTransaction = null },
                onConfirm = {
                    onDeleteTransaction(it)
                    pendingDeleteTransaction = null
                }
            )
        }
    }
}
}

/** Shared by every screen in this file that confirms deleting a transaction (spec §14). */
@Composable
private fun TransactionDeleteConfirmDialog(
    transaction: TransactionEntity,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirm: (TransactionEntity) -> Unit
) {
    val isIncome = transaction.type == TransactionType.INCOME
    val isDark = isAppInDarkTheme()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = ShapeCard,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                RampIconTile(icon = Icons.Default.Delete, ramp = Ramp.Red, size = 64.dp, iconSize = 32.dp)

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Delete transaction?",
                    style = SelfBudgetType.title,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Are you sure you want to delete this transaction record? This cannot be undone.",
                    style = SelfBudgetType.body,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Highlighted Transaction Card
                Surface(
                    shape = ShapeCard,
                    color = Ramp.Gray.tintFill(isDark),
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
                            text = transaction.title,
                            style = SelfBudgetType.rowTitle,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${if (isIncome) "+" else "-"}$currencySymbol%.2f".format(transaction.amount),
                            style = SelfBudgetType.rowTitle,
                            color = if (isIncome) getIncomeColor() else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons Row — the confirm dialog's destructive button may be
                // solid Red (spec §14's one sanctioned exception to "never solid red").
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SecondaryPillButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    )

                    PrimaryPillButton(
                        text = "Delete",
                        onClick = { onConfirm(transaction) },
                        ramp = Ramp.Red,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    )
                }
            }
        }
    }
}

/**
 * Full-page chooser opened by the shared "Add transaction" entry points (the Home tab's inline
 * pill button and the Activity tab's add action). Replaces separate per-tab add buttons:
 * whichever card the user picks here is the only path into that form, no matter which tab
 * they started from.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEntryPointScreen(
    onDismiss: () -> Unit,
    onPickIncome: () -> Unit,
    onPickExpense: () -> Unit,
    onPickTransfer: () -> Unit,
    onPickBudget: () -> Unit,
    onPickRecurringIncome: () -> Unit,
    onPickRecurringExpense: () -> Unit,
    onPickGoal: () -> Unit,
    onPickAccount: () -> Unit,
    onPickAsset: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            TopAppBar(
                title = {
                    Text(
                        text = "What do you want to add?",
                        style = SelfBudgetType.title
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Section 1: Money Movement
                val isDarkMenu = isAppInDarkTheme()
                SectionHeaderBand(title = "Money movement", ramp = Ramp.Gray) {
                    AddEntryPointRow(
                        title = "Add expense",
                        subtitle = "Something you spent money on",
                        icon = Icons.Default.ArrowDownward,
                        iconBadgeColor = Ramp.Red.tintFill(isDarkMenu),
                        iconTint = Ramp.Red.icon(isDarkMenu),
                        onClick = onPickExpense
                    )
                    SectionRowDivider(modifier = Modifier.padding(start = 70.dp))
                    AddEntryPointRow(
                        title = "Add income",
                        subtitle = "Paycheck, freelance, or gift",
                        icon = Icons.Default.ArrowUpward,
                        iconBadgeColor = Ramp.Teal.tintFill(isDarkMenu),
                        iconTint = Ramp.Teal.icon(isDarkMenu),
                        onClick = onPickIncome
                    )
                    SectionRowDivider(modifier = Modifier.padding(start = 70.dp))
                    AddEntryPointRow(
                        title = "Transfer money",
                        subtitle = "Move funds between your accounts",
                        icon = Icons.Default.SwapHoriz,
                        iconBadgeColor = Ramp.Gray.tintFill(isDarkMenu),
                        iconTint = Ramp.Gray.icon(isDarkMenu),
                        onClick = onPickTransfer
                    )
                }

                // Section 2: Planning Tools
                SectionHeaderBand(title = "Planning tools", ramp = Ramp.Gray) {
                    AddEntryPointRow(
                        title = "Recurring expense",
                        subtitle = "Subscription, bill, or rent",
                        icon = Icons.Default.Repeat,
                        iconBadgeColor = Ramp.Red.tintFill(isDarkMenu),
                        iconTint = Ramp.Red.icon(isDarkMenu),
                        onClick = onPickRecurringExpense
                    )
                    SectionRowDivider(modifier = Modifier.padding(start = 70.dp))
                    AddEntryPointRow(
                        title = "Recurring income",
                        subtitle = "Salary, freelance, or pension",
                        icon = Icons.Default.Repeat,
                        iconBadgeColor = Ramp.Teal.tintFill(isDarkMenu),
                        iconTint = Ramp.Teal.icon(isDarkMenu),
                        onClick = onPickRecurringIncome
                    )
                    SectionRowDivider(modifier = Modifier.padding(start = 70.dp))
                    AddEntryPointRow(
                        title = "Create a budget",
                        subtitle = "Monthly limit for a category",
                        icon = Icons.Default.PieChart,
                        iconBadgeColor = Ramp.Teal.tintFill(isDarkMenu),
                        iconTint = Ramp.Teal.icon(isDarkMenu),
                        onClick = onPickBudget
                    )
                    SectionRowDivider(modifier = Modifier.padding(start = 70.dp))
                    AddEntryPointRow(
                        title = "Create a savings goal",
                        subtitle = "Emergency fund, trip, purchase",
                        icon = Icons.Default.TrackChanges,
                        iconBadgeColor = Ramp.Teal.tintFill(isDarkMenu),
                        iconTint = Ramp.Teal.icon(isDarkMenu),
                        onClick = onPickGoal
                    )
                }

                // Section 3: Accounts & Assets
                SectionHeaderBand(title = "Accounts and assets", ramp = Ramp.Gray) {
                    AddEntryPointRow(
                        title = "Add an account / wallet",
                        subtitle = "Bank, card, cash, or loan",
                        icon = Icons.Default.AccountBalance,
                        iconBadgeColor = Ramp.Gray.tintFill(isDarkMenu),
                        iconTint = Ramp.Gray.icon(isDarkMenu),
                        onClick = onPickAccount
                    )
                    SectionRowDivider(modifier = Modifier.padding(start = 70.dp))
                    AddEntryPointRow(
                        title = "Add an asset",
                        subtitle = "Investment, property, or crypto",
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        iconBadgeColor = Ramp.Purple.tintFill(isDarkMenu),
                        iconTint = Ramp.Purple.icon(isDarkMenu),
                        onClick = onPickAsset
                    )
                }

                Spacer(modifier = Modifier.height(140.dp))
            }
        }
    }
}

@Composable
private fun AddEntryPointRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBadgeColor: Color,
    iconTint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = iconBadgeColor,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = SelfBudgetType.rowTitle,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = SelfBudgetType.meta,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(18.dp)
        )
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
    onAddTransfer: (fromAccountId: String, toAccountId: String, amount: Double, note: String?) -> Unit = { _, _, _, _ -> },
    onReviewPlan: () -> Unit = {}
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

    HomeDashboardMockupContent(
        uiState = uiState,
        categoryMap = categoryMap,
        recentPreview = recentPreview.take(5),
        isBalanceVisible = isBalanceVisible,
        onPreviousMonth = onPreviousMonth,
        onNextMonth = onNextMonth,
        onSelectMonthYear = onSelectMonthYear,
        onShowAllAccounts = { showAllAccountsSheet = true },
        onTransfer = { showTransferDialog = true },
        onReviewPlan = onReviewPlan,
        onAddAccount = { showAddAccountDialog = true },
        onEditAccount = { selectedAccountForEdit = it },
        onShowFullHistory = { showFullHistorySheet = true },
        onEditTransaction = onEditTransaction
    )

    // Full Transaction History Modal Dialog with Search & Category Filter Chips
    if (showFullHistorySheet) {
        FullTransactionHistoryDialog(
            transactions = uiState.transactions,
            categories = uiState.categories,
            accounts = uiState.accounts,
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
        TransactionDeleteConfirmDialog(
            transaction = pendingDeleteTransaction!!,
            currencySymbol = sym,
            onDismiss = { pendingDeleteTransaction = null },
            onConfirm = {
                onDeleteTransaction(it)
                pendingDeleteTransaction = null
            }
        )
    }

    selectedAccountForEdit?.let { acc ->
        EditCustomAccountDialog(
            account = acc,
            currentBalance = uiState.accountBalances[acc.id],
            currencySymbol = uiState.currencySymbol,
            goals = uiState.goals,
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
            goals = uiState.goals,
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
            currencySymbol = uiState.currencySymbol,
            onDismiss = { showTransferDialog = false },
            onConfirm = { fromId, toId, amount, note ->
                onAddTransfer(fromId, toId, amount, note)
                showTransferDialog = false
            },
            onAddCustomAccount = onAddCustomAccount
        )
    }
}

@Composable
private fun HomeDashboardMockupContent(
    uiState: HomeUiState,
    categoryMap: Map<String, CategoryEntity>,
    recentPreview: List<TransactionEntity>,
    isBalanceVisible: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectMonthYear: (String) -> Unit,
    onShowAllAccounts: () -> Unit,
    onTransfer: () -> Unit,
    onReviewPlan: () -> Unit,
    onAddAccount: () -> Unit,
    onEditAccount: (AccountEntity) -> Unit,
    onShowFullHistory: () -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit
) {
    var showCashBreakdown by remember { mutableStateOf(false) }
    var showUpcomingBillsModal by remember { mutableStateOf(false) }
    var showNetWorthModal by remember { mutableStateOf(false) }

    val sym = uiState.currencySymbol
    val allMonthTransactions = uiState.monthTransactions
    val expenseTransactions = remember(allMonthTransactions) {
        allMonthTransactions.filter { it.type == TransactionType.EXPENSE }
    }
    val totalSpent = remember(expenseTransactions) {
        Money.sum(expenseTransactions.map { it.amount })
    }
    val previousBudgetMap = remember(uiState.previousMonthBudgets) {
        uiState.previousMonthBudgets.associateBy { it.categoryId }
    }
    val totalBudget = remember(uiState.budgets, previousBudgetMap, uiState.previousMonthSpentByCategory) {
        Money.sum(uiState.budgets.map { budget ->
            BudgetRollover.effectiveLimit(
                currentLimit = budget.amountLimit,
                rolloverEnabled = budget.rolloverEnabled,
                previousLimit = previousBudgetMap[budget.categoryId]?.amountLimit ?: 0.0,
                previousSpent = uiState.previousMonthSpentByCategory[budget.categoryId] ?: 0.0
            )
        })
    }
    // "Still outstanding this cycle" per recurring bill (spec: posting a bill should visibly
    // shrink this figure), not the full monthly-equivalent commitment regardless of what's
    // already been posted - see RecurringCycleCalculator, shared with RecurringScreen's own
    // per-item cycle status so the two never disagree.
    val upcomingBills = remember(uiState.recurringList, uiState.transactions, uiState.selectedMonthYear) {
        Money.sum(
            uiState.recurringList
                .filter { it.type == TransactionType.EXPENSE && !it.isArchived }
                .map { item ->
                    RecurringCycleCalculator.getCyclePaymentSummary(
                        item,
                        uiState.transactions,
                        uiState.selectedMonthYear
                    ).remainingAmount
                }
        )
    }
    val remainingAfterBills = (totalBudget - totalSpent - upcomingBills).coerceAtLeast(0.0)
    val selectedMonthDaysLeft = remember(uiState.selectedMonthYear) {
        val cal = Calendar.getInstance()
        val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        try {
            val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            cal.time = sdf.parse(uiState.selectedMonthYear) ?: cal.time
        } catch (_: Exception) {
        }
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val startDay = if (uiState.selectedMonthYear == currentMonth) Calendar.getInstance().get(Calendar.DAY_OF_MONTH) else 1
        (daysInMonth - startDay + 1).coerceAtLeast(1)
    }
    val safePerDay = if (totalBudget > 0.0) remainingAfterBills / selectedMonthDaysLeft else 0.0
    val budgetPct = if (totalBudget > 0.0) (totalSpent / totalBudget).coerceIn(0.0, 1.0) else 0.0
    val liquidAccounts = remember(uiState.accounts) {
        val liquidTypes = setOf(AccountType.CHECKING, AccountType.SAVINGS, AccountType.CASH)
        uiState.accounts.filter { it.type in liquidTypes }
    }
    val cashAvailable = remember(liquidAccounts, uiState.accountBalances) {
        Money.sum(liquidAccounts.map { acc -> uiState.accountBalances[acc.id] ?: acc.initialBalance })
    }
    val sortedAccounts = remember(uiState.accounts) {
        uiState.accounts.sortedWith(
            compareBy(
                { !it.isDefault },
                { com.selfbudget.app.core.ui.getAccountTypePriority(it.type) },
                { it.name.lowercase() }
            )
        )
    }
    val categorySpend = remember(expenseTransactions) {
        expenseTransactions.groupBy { it.categoryId }
            .mapValues { (_, txs) -> Money.sum(txs.map { it.amount }) }
    }
    val budgetByCategory = remember(uiState.budgets) {
        uiState.budgets.associateBy { it.categoryId }
    }
    val watchCategory = remember(categorySpend, budgetByCategory, categoryMap) {
        categorySpend.mapNotNull { (categoryId, spent) ->
            val budget = budgetByCategory[categoryId] ?: return@mapNotNull null
            if (budget.amountLimit <= 0.0) return@mapNotNull null
            val pct = spent / budget.amountLimit
            if (pct >= 0.75) Triple(categoryMap[categoryId]?.name ?: "Category", budget.amountLimit - spent, pct) else null
        }.maxByOrNull { it.third }
    }
    val dateFormat = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Daily check-in hero (spec's canonical "$0/day" hero example): status-driven
            // color (Teal safe / Amber watch / Red over), neutral card fill.
            val heroStatus = when {
                remainingAfterBills < 0.0 -> BudgetStatus.Over
                else -> budgetStatus(budgetPct.toFloat())
            }
            val isDarkHero = isAppInDarkTheme()
            val heroHighlightColor = when (heroStatus) {
                BudgetStatus.Over -> getExpenseColor()
                BudgetStatus.Watch -> getWarningColor()
                BudgetStatus.Safe -> getIncomeColor()
            }
            Surface(
                shape = ShapeHero,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)),
                tonalElevation = 1.dp,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = heroStatus.ramp.solidFill(isDarkHero),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = heroStatus.ramp.onSolidFill(isDarkHero),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "DAILY CHECK-IN".uppercase(),
                            style = SelfBudgetType.eyebrow,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (remainingAfterBills >= 0.0) "You’re on track" else "Review your plan",
                            style = SelfBudgetType.title,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isBalanceVisible) "$sym%.0f/day".format(safePerDay) else "$sym••/day",
                            style = SelfBudgetType.display,
                            color = heroHighlightColor
                        )
                        Text(
                            text = "safe to spend",
                            style = SelfBudgetType.body,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (isBalanceVisible) "$sym%.0f left after committed bills".format(remainingAfterBills) else "$sym••• left after committed bills",
                            style = SelfBudgetType.meta,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(
                        modifier = Modifier.width(96.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        BudgetStatusBars(
                            progress = budgetPct.toFloat(),
                            barColor = heroHighlightColor
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${(budgetPct * 100).toInt()}%",
                            style = SelfBudgetType.heading,
                            color = heroHighlightColor
                        )
                        Text(
                            text = "of monthly\nbudget",
                            style = SelfBudgetType.meta,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        item {
            Surface(
                shape = ShapeHero,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val cleanCash = if (kotlin.math.abs(cashAvailable) < 0.5) 0.0 else cashAvailable
                    val cleanBills = if (kotlin.math.abs(upcomingBills) < 0.5) 0.0 else upcomingBills
                    val cleanNetWorth = if (kotlin.math.abs(uiState.netWorth) < 0.5) 0.0 else uiState.netWorth
                    SnapshotMetric(
                        Icons.Default.Payments, "Cash available",
                        if (isBalanceVisible) "$sym%,.0f".format(cleanCash) else "$sym•••",
                        getBrandColor(),
                        onClick = { showCashBreakdown = true }
                    )
                    VerticalHomeDivider()
                    SnapshotMetric(
                        Icons.Default.Receipt, "Upcoming bills",
                        if (isBalanceVisible) "$sym%,.0f".format(cleanBills) else "$sym•••",
                        getAccentColor(), "due soon",
                        onClick = { showUpcomingBillsModal = true }
                    )
                    VerticalHomeDivider()
                    SnapshotMetric(
                        Icons.AutoMirrored.Filled.TrendingUp, "Net worth",
                        if (isBalanceVisible) "$sym%,.0f".format(cleanNetWorth) else "$sym•••",
                        getBrandColor(),
                        onClick = { showNetWorthModal = true }
                    )
                }
            }
        }

        item {
            // "Next best action" banner: the Attention/Coral role (spec §5), distinct from
            // the Amber Watch-status color used for over-budget category warnings elsewhere.
            val actionTitle = watchCategory?.let { "${it.first} is trending high" } ?: "No urgent action"
            val actionSubtitle = watchCategory?.let { "$sym%.0f left this month".format(it.second.coerceAtLeast(0.0)) } ?: "Your spending plan looks steady"
            val isDarkAction = isAppInDarkTheme()
            Surface(
                shape = ShapeHero,
                color = if (isDarkAction) CardSurfaceDark else Ramp.Coral.tintFill(isDarkAction),
                border = if (isDarkAction) BorderStroke(0.5.dp, DividerDark) else null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RampIconTile(
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        ramp = Ramp.Coral,
                        size = 42.dp,
                        iconSize = 22.dp
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "NEXT BEST ACTION".uppercase(),
                            style = SelfBudgetType.eyebrow,
                            color = if (isDarkAction) Ramp.Coral.c200 else Ramp.Coral.secondaryText(isDarkAction)
                        )
                        Text(
                            actionTitle,
                            style = SelfBudgetType.heading,
                            color = if (isDarkAction) TextPrimaryDark else Ramp.Coral.titleText(isDarkAction)
                        )
                        Text(
                            actionSubtitle,
                            style = SelfBudgetType.meta,
                            color = if (isDarkAction) TextSecondaryDark else Ramp.Coral.secondaryText(isDarkAction)
                        )
                    }
                    PrimaryPillButton(text = "Review plan", onClick = onReviewPlan, ramp = Ramp.Coral)
                }
            }
        }

        item {
            SectionHeaderBand(
                title = "Accounts and wallets",
                ramp = sectionRamp("Accounts and wallets"),
                icon = Icons.Default.AccountBalance,
                trailingText = "See all",
                onTrailingClick = onShowAllAccounts
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(sortedAccounts.take(3)) { account ->
                            HomeAccountCard(
                                account = account,
                                balance = uiState.accountBalances[account.id] ?: account.initialBalance,
                                fallbackSymbol = sym,
                                isBalanceVisible = isBalanceVisible,
                                onClick = { onEditAccount(account) }
                            )
                        }
                        item { HomeAddAccountCard(onClick = onAddAccount) }
                    }
                }
            }
        }

        item {
            SectionHeaderBand(
                title = "Recent activity",
                ramp = sectionRamp("Recent activity"),
                icon = Icons.Default.History,
                trailingText = if (uiState.transactions.isNotEmpty()) "See all" else null,
                onTrailingClick = if (uiState.transactions.isNotEmpty()) onShowFullHistory else null
            ) {
                if (recentPreview.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Receipt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No activity yet", style = SelfBudgetType.rowTitle)
                    }
                } else {
                    recentPreview.forEachIndexed { index, transaction ->
                        if (index > 0) SectionRowDivider()
                        // Icon tiles are colored by category (spec §10), not by transaction
                        // sign — an all-red/all-teal icon column discards the category
                        // color language. Ordinary expenses read as neutral text; only
                        // income is Teal and transfers stay neutral Gray.
                        val isIncome = transaction.type == TransactionType.INCOME
                        val isTransfer = transaction.type == TransactionType.TRANSFER
                        val category = categoryMap[transaction.categoryId]
                        val icon = when {
                            isTransfer -> Icons.Default.SwapHoriz
                            category != null -> getCategoryIcon(category)
                            isIncome -> Icons.Default.ArrowDownward
                            else -> Icons.Default.ArrowUpward
                        }
                        val rowRamp = when {
                            isTransfer -> Ramp.Gray
                            category != null -> sectionRamp(
                                if (isIncome) com.selfbudget.app.core.ui.getIncomeCategoryGroup(category)
                                else com.selfbudget.app.core.ui.getExpenseCategoryGroup(category)
                            )
                            isIncome -> Ramp.Teal
                            else -> Ramp.Gray
                        }
                        val amountPrefix = when {
                            isTransfer -> "⇄ $sym"
                            isIncome -> "+$sym"
                            else -> "-$sym"
                        }
                        val amountColor = when {
                            isIncome -> getIncomeColor()
                            else -> MaterialTheme.colorScheme.onSurface
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onEditTransaction(transaction) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RampIconTile(icon = icon, ramp = rowRamp, size = 36.dp, iconSize = 18.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = transaction.title,
                                    style = SelfBudgetType.rowTitle,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${category?.name ?: "General"} • ${dateFormat.format(Date(transaction.timestamp))}",
                                    style = SelfBudgetType.meta,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = if (isBalanceVisible) "$amountPrefix%.2f".format(transaction.amount) else "$sym ••••••",
                                style = SelfBudgetType.rowTitle,
                                color = amountColor
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(96.dp)) }
    }

    if (showCashBreakdown) {
        com.selfbudget.app.core.ui.AccountBreakdownModal(
            title = "Cash available",
            ramp = Ramp.Teal,
            icon = Icons.Default.Payments,
            accounts = liquidAccounts,
            accountBalances = uiState.accountBalances,
            currencySymbol = sym,
            onDismiss = { showCashBreakdown = false }
        )
    }

    if (showUpcomingBillsModal) {
        UpcomingBillsModal(
            recurringList = uiState.recurringList,
            categoryMap = categoryMap,
            allTransactions = uiState.transactions,
            selectedMonthYear = uiState.selectedMonthYear,
            currencySymbol = sym,
            onDismiss = { showUpcomingBillsModal = false }
        )
    }

    if (showNetWorthModal) {
        com.selfbudget.app.core.ui.NetWorthHistoryModal(
            history = uiState.netWorthHistory,
            accounts = uiState.accounts,
            accountBalances = uiState.accountBalances,
            currencySymbol = sym,
            onDismiss = { showNetWorthModal = false }
        )
    }
}

@Composable
private fun SnapshotMetric(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color,
    caption: String? = null,
    onClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .width(88.dp)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(21.dp))
        Spacer(modifier = Modifier.height(6.dp))
        Text(label, style = SelfBudgetType.meta, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = SelfBudgetType.heading, color = MaterialTheme.colorScheme.onSurface)
        if (caption != null) {
            Text(caption, style = SelfBudgetType.meta, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * Drill-down opened by tapping the "Upcoming bills" snapshot metric (spec §14): every recurring
 * expense still outstanding this cycle, so the figure is never a dead end - mirrors
 * [com.selfbudget.app.core.ui.AccountBreakdownModal]'s pattern for the Cash/Net worth metrics.
 */
@Composable
private fun UpcomingBillsModal(
    recurringList: List<RecurringTransactionEntity>,
    categoryMap: Map<String, CategoryEntity>,
    allTransactions: List<TransactionEntity>,
    selectedMonthYear: String,
    currencySymbol: String,
    onDismiss: () -> Unit
) {
    val isDark = isAppInDarkTheme()
    val dueDateFormat = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }

    data class UpcomingBill(
        val item: RecurringTransactionEntity,
        val remaining: Double,
        val isPartiallyPaid: Boolean
    )

    val upcomingBillsList = remember(recurringList, allTransactions, selectedMonthYear) {
        recurringList
            .filter { it.type == TransactionType.EXPENSE && !it.isArchived }
            .mapNotNull { item ->
                val summary = RecurringCycleCalculator.getCyclePaymentSummary(item, allTransactions, selectedMonthYear)
                if (summary.remainingAmount > 0.005) UpcomingBill(item, summary.remainingAmount, summary.isPartiallyPaid) else null
            }
            .sortedBy { it.item.nextDueDate }
    }
    val total = remember(upcomingBillsList) { Money.sum(upcomingBillsList.map { it.remaining }) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = true)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularBackButton(onClick = onDismiss)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Upcoming bills", style = SelfBudgetType.title, color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Hero Total Card — neutral display number, the icon pill carries the color (spec §14).
                    Surface(modifier = Modifier.fillMaxWidth(), shape = ShapeCard, color = Ramp.Coral.tintFill(isDark)) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Receipt, contentDescription = null, tint = Ramp.Coral.secondaryText(isDark), modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${upcomingBillsList.size} bill${if (upcomingBillsList.size != 1) "s" else ""} still due",
                                    style = SelfBudgetType.section,
                                    color = Ramp.Coral.secondaryText(isDark)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "$currencySymbol%.2f".format(total),
                                style = SelfBudgetType.display,
                                color = Ramp.Coral.titleText(isDark)
                            )
                        }
                    }

                    if (upcomingBillsList.isEmpty()) {
                        Surface(modifier = Modifier.fillMaxWidth(), shape = ShapeCard, color = MaterialTheme.colorScheme.surface) {
                            Text(
                                text = "You're all caught up - nothing due for this cycle.",
                                style = SelfBudgetType.body,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        Surface(modifier = Modifier.fillMaxWidth(), shape = ShapeCard, color = MaterialTheme.colorScheme.surface) {
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                upcomingBillsList.forEachIndexed { index, bill ->
                                    if (index > 0) SectionRowDivider(modifier = Modifier.padding(start = 62.dp))

                                    val category = categoryMap[bill.item.categoryId]
                                    val rowRamp = if (category != null) sectionRamp(com.selfbudget.app.core.ui.getExpenseCategoryGroup(category)) else Ramp.Gray
                                    val icon = category?.let { getCategoryIcon(it) } ?: Icons.Default.Receipt

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            RampIconTile(icon = icon, ramp = rowRamp, size = 36.dp, iconSize = 18.dp)
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(text = bill.item.title, style = SelfBudgetType.rowTitle, color = MaterialTheme.colorScheme.onSurface)
                                                Text(
                                                    text = "Due ${dueDateFormat.format(Date(bill.item.nextDueDate))}" +
                                                        if (bill.isPartiallyPaid) " · partially paid" else "",
                                                    style = SelfBudgetType.meta,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Text(
                                            text = "$currencySymbol%.2f".format(bill.remaining),
                                            style = SelfBudgetType.rowTitle,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(120.dp))
                }
            }
        }
    }
}

@Composable
private fun BudgetStatusBars(
    progress: Float,
    barColor: Color = MaterialTheme.colorScheme.primary
) {
    val activeBars = (progress.coerceIn(0f, 1f) * 4f).toInt().coerceIn(1, 4)
    Row(
        modifier = Modifier.height(48.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        listOf(18.dp, 26.dp, 34.dp, 42.dp).forEachIndexed { index, height ->
            Box(
                modifier = Modifier
                    .width(12.dp)
                    .height(height)
                    .clip(RoundedCornerShape(5.dp))
                    .background(
                        if (index < activeBars) barColor
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
                    )
            )
        }
    }
}

@Composable
private fun VerticalHomeDivider() {
    Box(
        modifier = Modifier
            .height(62.dp)
            .width(1.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
    )
}

@Composable
private fun HomeAccountCard(
    account: AccountEntity,
    balance: Double,
    fallbackSymbol: String,
    isBalanceVisible: Boolean,
    onClick: () -> Unit
) {
    val color = try {
        Color(android.graphics.Color.parseColor(account.colorHex))
    } catch (_: Exception) {
        MaterialTheme.colorScheme.primary
    }
    val accountSymbol = com.selfbudget.app.core.util.Currencies.symbolFor(account.currencyCode).ifBlank { fallbackSymbol }
    val isLiability = com.selfbudget.app.core.util.AccountBalanceCalculator.isLiability(account.type)
    val rawBalance = if (isLiability) kotlin.math.abs(balance) else balance
    val cleanBalance = if (kotlin.math.abs(rawBalance) < 0.5) 0.0 else rawBalance
    val isNegative = (isLiability && cleanBalance >= 0.5) || (!isLiability && cleanBalance < 0.0)
    val formattedBalance = "$accountSymbol%,.0f".format(kotlin.math.abs(cleanBalance))

    Surface(
        shape = ShapeCard,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
        modifier = Modifier
            .width(142.dp)
            .height(112.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Icon(com.selfbudget.app.core.ui.getAccountIcon(account.type), contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column {
                Text(account.name, style = SelfBudgetType.meta, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                if (account.type == AccountType.CREDIT_CARD && account.creditLimit != null) {
                    val availableCredit = (account.creditLimit - cleanBalance).coerceAtLeast(0.0)
                    val formattedAvailable = "$accountSymbol%,.0f".format(availableCredit)
                    Text(
                        text = if (isBalanceVisible) "$formattedAvailable available" else "$accountSymbol•••",
                        style = SelfBudgetType.heading,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                } else {
                    Text(
                        text = if (isBalanceVisible) "${if (isNegative) "-" else ""}$formattedBalance" else "$accountSymbol•••",
                        style = SelfBudgetType.heading,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeAddAccountCard(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(112.dp)
            .width(58.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = ShapeChip,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
            modifier = Modifier
                .size(44.dp)
                .clickable { onClick() }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Add, contentDescription = "Add Account", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FullTransactionHistoryDialog(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    accounts: List<AccountEntity> = emptyList(),
    currencySymbol: String,
    selectedMonthYear: String,
    isBalanceVisible: Boolean,
    onDismiss: () -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf<TransactionType?>(null) } // null = All
    var selectedCategoryId by remember { mutableStateOf<String?>(null) } // null = All Categories
    var selectedAccountId by remember { mutableStateOf<String?>(null) } // null = All Accounts
    var selectedDateRange by remember { mutableStateOf(DateRangeFilter.ALL) }
    var selectedSortOption by remember { mutableStateOf(SortOption.NEWEST) }

    var showFilterModal by remember { mutableStateOf(false) }

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
            val matchesAccount = selectedAccountId == null || tx.accountId == selectedAccountId

            val matchesDateRange = when (selectedDateRange) {
                DateRangeFilter.ALL -> true
                DateRangeFilter.THIS_MONTH -> {
                    val txCal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
                    txCal.get(Calendar.YEAR) == currentYear && txCal.get(Calendar.MONTH) == currentMonth
                }
                DateRangeFilter.LAST_MONTH -> {
                    val txCal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
                    txCal.get(Calendar.YEAR) == lastMonthYear && txCal.get(Calendar.MONTH) == lastMonthMonth
                }
                DateRangeFilter.LAST_30_DAYS -> tx.timestamp >= thirtyDaysAgo
                DateRangeFilter.THIS_YEAR -> {
                    val txCal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
                    txCal.get(Calendar.YEAR) == currentYear
                }
            }

            matchesQuery && matchesType && matchesCategory && matchesAccount && matchesDateRange
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

    val totalFilteredIncome = remember(filteredTransactions) {
        filteredTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    }
    val totalFilteredExpense = remember(filteredTransactions) {
        filteredTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "All transactions",
                                style = SelfBudgetType.heading,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${sortedTransactions.size} of ${transactions.size} entries",
                                style = SelfBudgetType.meta,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        CircularBackButton(onClick = onDismiss, modifier = Modifier.padding(start = 12.dp, end = 8.dp))
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )

                // Search Input Field & Filter Button Row
                val isDarkFilterBar = isAppInDarkTheme()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    com.selfbudget.app.core.ui.AppSearchBar(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = "Search transactions...",
                        modifier = Modifier.weight(1f)
                    )

                    // Filter pill button
                    Surface(
                        shape = ShapePill,
                        color = if (activeFilterCount > 0) Ramp.Teal.tintFill(isDarkFilterBar) else MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .height(52.dp)
                            .clickable { showFilterModal = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Filter",
                                tint = if (activeFilterCount > 0) Ramp.Teal.titleText(isDarkFilterBar) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (activeFilterCount > 0) "Filter ($activeFilterCount)" else "Filter",
                                style = SelfBudgetType.rowTitle,
                                color = if (activeFilterCount > 0) Ramp.Teal.titleText(isDarkFilterBar) else MaterialTheme.colorScheme.onSurfaceVariant
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
                                    TransactionType.EXPENSE -> "Expenses"
                                    TransactionType.INCOME -> "Income"
                                    TransactionType.TRANSFER -> "Transfers"
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
                            // Quiet Teal "Reset" text action (spec §24) — never red, it isn't destructive.
                            Text(
                                text = "Reset",
                                style = SelfBudgetType.badge,
                                color = getAccentColor(),
                                modifier = Modifier
                                    .clip(ShapeChip)
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

                LazyColumn(
                    contentPadding = PaddingValues(bottom = 150.dp)
                ) {
                    item {
                        // "Recent activity" section identity is Purple (design system §"Section identity colors").
                        SectionHeaderBand(
                            title = "Transactions",
                            ramp = Ramp.Purple,
                            icon = Icons.Default.History,
                            trailingText = when {
                                totalFilteredIncome > 0 && totalFilteredExpense == 0.0 -> "+$currencySymbol%.2f".format(totalFilteredIncome)
                                totalFilteredExpense > 0 && totalFilteredIncome == 0.0 -> "-$currencySymbol%.2f".format(totalFilteredExpense)
                                totalFilteredIncome > 0 -> "+$currencySymbol%.2f / -$currencySymbol%.2f".format(totalFilteredIncome, totalFilteredExpense)
                                else -> "${sortedTransactions.size} record${if (sortedTransactions.size != 1) "s" else ""}"
                            }
                        ) {
                            if (sortedTransactions.isEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = if (activeFilterCount > 0 || searchQuery.isNotBlank()) "No records match your active filters." else "No transactions logged yet.",
                                        style = SelfBudgetType.body,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            } else {
                                sortedTransactions.forEachIndexed { index, tx ->
                                    val category = categoryMap[tx.categoryId]
                                    val account = accountMap[tx.accountId]
                                    val isIncome = tx.type == TransactionType.INCOME
                                    val isTransfer = tx.type == TransactionType.TRANSFER
                                    val sym = if (account?.currencyCode?.isNotBlank() == true) com.selfbudget.app.core.util.Currencies.symbolFor(account.currencyCode) else currencySymbol

                                    // Rows are colored by category identity, never by transaction sign (spec §10).
                                    val rowRamp = when {
                                        isTransfer -> Ramp.Gray
                                        isIncome -> Ramp.Teal
                                        category != null -> sectionRamp(com.selfbudget.app.core.ui.getExpenseCategoryGroup(category))
                                        else -> Ramp.Gray
                                    }
                                    val icon = when {
                                        isTransfer -> Icons.Default.SwapHoriz
                                        category != null -> getCategoryIcon(category)
                                        isIncome -> Icons.Default.ArrowDownward
                                        else -> Icons.Default.ArrowUpward
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onEditTransaction(tx) }
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            RampIconTile(icon = icon, ramp = rowRamp, size = 36.dp, iconSize = 18.dp)
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = tx.title,
                                                    style = SelfBudgetType.rowTitle,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "${category?.name ?: "General"}${if (account != null) " · ${account.name}" else ""} · ${dateFormat.format(Date(tx.timestamp))}",
                                                    style = SelfBudgetType.meta,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                if (!tx.note.isNullOrBlank()) {
                                                    Text(
                                                        text = "Note: ${tx.note}",
                                                        style = SelfBudgetType.meta,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }

                                        // Income reads Teal; transfers and ordinary expenses read neutral —
                                        // red is reserved for over-limit, not ordinary spending (spec §10/§13).
                                        val amountPrefix = if (isIncome) "+$sym" else if (isTransfer) sym else "-$sym"
                                        val amountColor = if (isIncome) Ramp.Teal.secondaryText(isAppInDarkTheme()) else MaterialTheme.colorScheme.onSurface
                                        Text(
                                            text = if (isBalanceVisible) "$amountPrefix%.2f".format(tx.amount) else "$sym ••••••",
                                            style = SelfBudgetType.rowTitle,
                                            color = amountColor
                                        )
                                    }

                                    if (index < sortedTransactions.lastIndex) {
                                        SectionRowDivider(modifier = Modifier.padding(horizontal = 14.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Full-Screen Filter Options Modal
        if (showFilterModal) {
            Dialog(
                onDismissRequest = { showFilterModal = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
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
                        val isDarkFilterModal = isAppInDarkTheme()

                        // Header Row: close · title · quiet reset (spec §24)
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
                                    text = "Filter transactions",
                                    style = SelfBudgetType.heading,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (activeFilterCount > 0) {
                                TextButton(
                                    onClick = {
                                        selectedTypeFilter = null
                                        selectedCategoryId = null
                                        selectedAccountId = null
                                        selectedDateRange = DateRangeFilter.ALL
                                        selectedSortOption = SortOption.NEWEST
                                    }
                                ) {
                                    Text("Reset", style = SelfBudgetType.body, color = Ramp.Teal.secondaryText(isDarkFilterModal))
                                }
                            }
                        }

                        FilterChipGroup(
                            title = "Timeframe",
                            options = DateRangeFilter.entries,
                            optionLabel = { it.label },
                            isSelected = { it == selectedDateRange },
                            onSelect = { selectedDateRange = it }
                        )

                        // Section 2: Transaction Type — 8px leading dot instead of emoji (spec §24)
                        Column {
                            Text("Transaction type", style = SelfBudgetType.heading, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TypeFilterChip(
                                    label = "All types",
                                    dotColor = null,
                                    selected = selectedTypeFilter == null,
                                    onClick = { selectedTypeFilter = null }
                                )
                                TypeFilterChip(
                                    label = "Expenses",
                                    dotColor = Ramp.Red.c400,
                                    selected = selectedTypeFilter == TransactionType.EXPENSE,
                                    onClick = { selectedTypeFilter = if (selectedTypeFilter == TransactionType.EXPENSE) null else TransactionType.EXPENSE }
                                )
                                TypeFilterChip(
                                    label = "Income",
                                    dotColor = Ramp.Teal.c400,
                                    selected = selectedTypeFilter == TransactionType.INCOME,
                                    onClick = { selectedTypeFilter = if (selectedTypeFilter == TransactionType.INCOME) null else TransactionType.INCOME }
                                )
                                TypeFilterChip(
                                    label = "Transfers",
                                    dotColor = Ramp.Gray.c400,
                                    selected = selectedTypeFilter == TransactionType.TRANSFER,
                                    onClick = { selectedTypeFilter = if (selectedTypeFilter == TransactionType.TRANSFER) null else TransactionType.TRANSFER }
                                )
                            }
                        }

                        FilterChipGroup(
                            title = "Categories",
                            options = listOf<CategoryEntity?>(null) + categories,
                            optionLabel = { it?.name ?: "All categories" },
                            isSelected = { it?.id == selectedCategoryId },
                            onSelect = { selectedCategoryId = it?.id }
                        )

                        if (accounts.isNotEmpty()) {
                            FilterChipGroup(
                                title = "Accounts & wallets",
                                options = listOf<AccountEntity?>(null) + accounts,
                                optionLabel = { it?.name ?: "All accounts" },
                                isSelected = { it?.id == selectedAccountId },
                                onSelect = { selectedAccountId = it?.id }
                            )
                        }

                        FilterChipGroup(
                            title = "Sort order",
                            options = SortOption.entries,
                            optionLabel = { it.label },
                            isSelected = { it == selectedSortOption },
                            onSelect = { selectedSortOption = it }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Footer: Cancel (secondary) + one primary stating the result (spec §24)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SecondaryPillButton(
                                text = "Cancel",
                                onClick = { showFilterModal = false },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                            )

                            PrimaryPillButton(
                                text = "Show ${sortedTransactions.size} transaction${if (sortedTransactions.size != 1) "s" else ""}",
                                onClick = { showFilterModal = false },
                                modifier = Modifier
                                    .weight(1.5f)
                                    .height(50.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(150.dp))
                    }
                }
            }
        }
    }
}
