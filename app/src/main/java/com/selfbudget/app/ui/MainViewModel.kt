package com.selfbudget.app.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.selfbudget.app.core.auth.AuthManager
import com.selfbudget.app.core.util.AccountBalanceCalculator
import com.selfbudget.app.core.util.Currencies
import com.selfbudget.app.core.util.Money
import com.selfbudget.app.core.util.RecurringFrequencyNormalizer
import com.selfbudget.app.core.util.RecurringScheduler
import com.selfbudget.app.data.model.AccountEntity
import com.selfbudget.app.data.model.AppThemeMode
import com.selfbudget.app.data.model.BudgetEntity
import com.selfbudget.app.data.model.CategoryEntity
import com.selfbudget.app.data.model.ExchangeRateEntity
import com.selfbudget.app.data.model.GoalEntity
import com.selfbudget.app.data.model.NetWorthSnapshotEntity
import com.selfbudget.app.data.model.RecurringFrequency
import com.selfbudget.app.data.model.RecurringTransactionEntity
import com.selfbudget.app.data.model.TransactionEntity
import com.selfbudget.app.data.model.TransactionType
import com.selfbudget.app.data.model.UserEntity
import com.selfbudget.app.data.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

data class HomeUiState(
    val totalBalance: Double = 0.0,
    val totalExpense: Double = 0.0,
    val totalIncome: Double = 0.0,
    val transactions: List<TransactionEntity> = emptyList(),
    val monthTransactions: List<TransactionEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val budgets: List<BudgetEntity> = emptyList(),
    val recurringList: List<RecurringTransactionEntity> = emptyList(),
    val accounts: List<AccountEntity> = emptyList(),
    val user: UserEntity? = null,
    val selectedMonthYear: String = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date()),
    val currencySymbol: String = "$",
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val isBiometricEnabled: Boolean = false,
    val isLoading: Boolean = true,
    // Live per-account balances (initial balance + every income/expense/transfer touching it),
    // keyed by AccountEntity.id. See AccountBalanceCalculator.
    val accountBalances: Map<String, Double> = emptyMap(),
    // Total balance across all accounts converted into the user's preferred currency.
    val netWorth: Double = 0.0,
    val goals: List<GoalEntity> = emptyList(),
    val exchangeRates: List<ExchangeRateEntity> = emptyList(),
    val netWorthHistory: List<NetWorthSnapshotEntity> = emptyList(),
    // Needed for budget-rollover math on BudgetScreen.
    val previousMonthBudgets: List<BudgetEntity> = emptyList(),
    val previousMonthSpentByCategory: Map<String, Double> = emptyMap()
)

private data class BaseCombined(
    val txs: List<TransactionEntity>,
    val cats: List<CategoryEntity>,
    val bdgts: List<BudgetEntity>,
    val recurrings: List<RecurringTransactionEntity>,
    val accs: List<AccountEntity>
)

private data class ExtraCombined(
    val prevBudgets: List<BudgetEntity>,
    val goals: List<GoalEntity>,
    val rates: List<ExchangeRateEntity>,
    val netWorthHistory: List<NetWorthSnapshotEntity>
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: ExpenseRepository,
    private val authManager: AuthManager
) : ViewModel() {

    val currentUser = authManager.currentUser.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _selectedMonthYear = MutableStateFlow(
        SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
    )

    private fun shiftMonth(monthYear: String, deltaMonths: Int): String {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        return try {
            val date = sdf.parse(monthYear) ?: Date()
            val cal = Calendar.getInstance().apply { time = date }
            cal.add(Calendar.MONTH, deltaMonths)
            sdf.format(cal.time)
        } catch (e: Exception) {
            monthYear
        }
    }

    val uiState: StateFlow<HomeUiState> = currentUser.flatMapLatest { user ->
        if (user == null) {
            flowOf(HomeUiState(isLoading = false))
        } else {
            _selectedMonthYear.flatMapLatest { selectedMonth ->
                val previousMonth = shiftMonth(selectedMonth, -1)

                combine(
                    repository.getTransactions(user.id),
                    repository.getCategories(),
                    repository.getBudgets(user.id, selectedMonth),
                    repository.getRecurringTransactions(user.id),
                    repository.getAccounts(user.id)
                ) { txs, cats, bdgts, recurrings, accs ->
                    BaseCombined(txs, cats, bdgts, recurrings, accs)
                }.flatMapLatest { base ->
                    combine(
                        repository.getBudgets(user.id, previousMonth),
                        repository.getGoals(user.id),
                        repository.getExchangeRates(user.id),
                        repository.getNetWorthSnapshots(user.id)
                    ) { prevBudgets, goals, rates, netWorthHistory ->
                        ExtraCombined(prevBudgets, goals, rates, netWorthHistory)
                    }.map { extra -> Triple(base, extra, selectedMonth) }
                }
            }.map { (base, extra, selectedMonth) ->
                val formatter = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                val monthTxs = base.txs.filter {
                    formatter.format(Date(it.timestamp)) == selectedMonth
                }
                val previousMonth = shiftMonth(selectedMonth, -1)
                val previousMonthTxs = base.txs.filter {
                    formatter.format(Date(it.timestamp)) == previousMonth
                }

                val exp = Money.sum(monthTxs.filter { it.type == TransactionType.EXPENSE }.map { it.amount })
                val inc = Money.sum(monthTxs.filter { it.type == TransactionType.INCOME }.map { it.amount })
                val bal = Money.subtract(inc, exp)

                val userCurrency = user.preferredCurrency
                val userTheme = try {
                    AppThemeMode.valueOf(user.themeMode)
                } catch (e: Exception) {
                    AppThemeMode.SYSTEM
                }

                val accountBalances = base.accs.associate { acc ->
                    acc.id to AccountBalanceCalculator.computeBalance(acc, base.txs)
                }

                val baseCurrencyCode = Currencies.codeForSymbol(userCurrency)
                val netWorth = AccountBalanceCalculator.computeTotalInBaseCurrency(
                    accounts = base.accs,
                    allTransactions = base.txs,
                    baseCurrency = baseCurrencyCode,
                    rates = extra.rates
                )

                val previousMonthSpentByCategory = previousMonthTxs
                    .filter { it.type == TransactionType.EXPENSE }
                    .groupBy { it.categoryId }
                    .mapValues { entry -> Money.sum(entry.value.map { it.amount }) }

                val accurateHistory = AccountBalanceCalculator.computeHistoricalSnapshots(
                    userId = user.id,
                    accounts = base.accs,
                    allTransactions = base.txs,
                    baseCurrency = baseCurrencyCode,
                    rates = extra.rates
                )
                val netWorthHistory = if (accurateHistory.isNotEmpty()) accurateHistory else extra.netWorthHistory

                HomeUiState(
                    totalBalance = bal,
                    totalExpense = exp,
                    totalIncome = inc,
                    transactions = base.txs,
                    monthTransactions = monthTxs,
                    categories = base.cats,
                    budgets = base.bdgts,
                    recurringList = base.recurrings,
                    accounts = base.accs,
                    user = user,
                    selectedMonthYear = selectedMonth,
                    currencySymbol = userCurrency,
                    themeMode = userTheme,
                    isBiometricEnabled = user.isBiometricEnabled,
                    isLoading = false,
                    accountBalances = accountBalances,
                    netWorth = netWorth,
                    goals = extra.goals,
                    exchangeRates = extra.rates,
                    netWorthHistory = netWorthHistory,
                    previousMonthBudgets = extra.prevBudgets,
                    previousMonthSpentByCategory = previousMonthSpentByCategory
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    init {
        // Keep net-worth history for current month only, upserted as the current month's numbers move.
        // Past months remain fixed and calculated from transaction ledger timestamps.
        viewModelScope.launch {
            uiState
                .map { Triple(it.user?.id, it.netWorth, it.accountBalances) }
                .distinctUntilChanged()
                .onEach { (userId, netWorth, accountBalances) ->
                    if (userId == null) return@onEach
                    val currentRealMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
                    val state = uiState.value
                    val assets = state.accounts.sumOf { acc ->
                        val b = accountBalances[acc.id] ?: 0.0
                        if (b > 0) b else 0.0
                    }
                    val liabilities = state.accounts.sumOf { acc ->
                        val b = accountBalances[acc.id] ?: 0.0
                        if (b < 0) kotlin.math.abs(b) else 0.0
                    }
                    repository.upsertNetWorthSnapshot(
                        NetWorthSnapshotEntity(
                            id = "$userId-$currentRealMonth",
                            userId = userId,
                            monthYear = currentRealMonth,
                            totalAssets = Money.round(assets),
                            totalLiabilities = Money.round(liabilities),
                            netWorth = netWorth
                        )
                    )
                }
                .collect { }
        }
    }

    fun navigateMonth(deltaMonths: Int) {
        _selectedMonthYear.value = shiftMonth(_selectedMonthYear.value, deltaMonths)
    }

    fun setSelectedMonthYear(monthYear: String) {
        _selectedMonthYear.value = monthYear
    }

    fun signInWithGoogle(context: Context, webClientId: String) {
        viewModelScope.launch {
            val result = authManager.signInWithGoogle(context, webClientId)
            if (result is com.selfbudget.app.core.auth.AuthResult.Error) {
                _authError.value = result.message
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authManager.signOut()
        }
    }

    fun addTransaction(
        title: String,
        amount: Double,
        type: TransactionType,
        categoryId: String,
        accountId: String = "acc_checking",
        note: String? = null,
        paymentMethod: String? = "Cash",
        receiptUri: String? = null,
        timestamp: Long = System.currentTimeMillis(),
        isRecurring: Boolean = false,
        recurringFrequency: RecurringFrequency = RecurringFrequency.MONTHLY,
        transferAccountId: String? = null
    ) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val transaction = TransactionEntity(
                userId = user.id,
                title = title,
                amount = Money.round(amount),
                type = type,
                categoryId = categoryId,
                accountId = accountId,
                timestamp = timestamp,
                note = note,
                paymentMethod = paymentMethod ?: "Cash",
                receiptImageUri = receiptUri,
                transferAccountId = transferAccountId
            )
            repository.addTransaction(transaction)

            if (isRecurring) {
                upsertRecurring(
                    userId = user.id,
                    title = title,
                    amount = amount,
                    type = type,
                    categoryId = categoryId,
                    accountId = accountId,
                    frequency = recurringFrequency,
                    note = note,
                    paymentMethod = paymentMethod ?: "Credit Card",
                    nextDueDate = RecurringScheduler.computeNextDueDate(timestamp, recurringFrequency)
                )
            }
        }
    }

    /**
     * The only path allowed to create or update a recurring bill and its budget-ceiling
     * suggestion. Both "mark as recurring" on the transaction form and the Recurring tab's own
     * add/edit form call into this, so the two surfaces can never disagree about what counts as
     * a duplicate bill or how a category's suggested budget ceiling gets computed.
     */
    private suspend fun upsertRecurring(
        userId: String,
        title: String,
        amount: Double,
        type: TransactionType,
        categoryId: String,
        accountId: String,
        frequency: RecurringFrequency,
        note: String? = null,
        paymentMethod: String? = null,
        remainingOccurrences: Int? = null,
        nextDueDate: Long? = null
    ) {
        val trimmedTitle = title.trim()
        val existing = uiState.value.recurringList.firstOrNull {
            it.userId == userId &&
            it.categoryId == categoryId &&
            it.title.trim().equals(trimmedTitle, ignoreCase = true) &&
            it.frequency == frequency
        }
        val recurring = existing?.copy(
            title = trimmedTitle,
            amount = Money.round(amount),
            type = type,
            isArchived = false
        ) ?: RecurringTransactionEntity(
            userId = userId,
            title = trimmedTitle,
            amount = Money.round(amount),
            type = type,
            categoryId = categoryId,
            accountId = accountId,
            frequency = frequency,
            nextDueDate = nextDueDate ?: RecurringScheduler.computeNextDueDate(System.currentTimeMillis(), frequency),
            note = note,
            paymentMethod = paymentMethod,
            remainingOccurrences = remainingOccurrences
        )
        if (existing != null) {
            repository.updateRecurringTransaction(recurring)
        } else {
            repository.addRecurringTransaction(recurring)
        }
        if (type == TransactionType.EXPENSE) {
            syncBudgetForRecurringExpense(userId, categoryId, amount, frequency)
        }
    }

    /** Moves money between two of the user's own accounts without touching income/expense totals. */
    fun addTransfer(
        fromAccountId: String,
        toAccountId: String,
        amount: Double,
        note: String? = null,
        timestamp: Long = System.currentTimeMillis()
    ) {
        val user = currentUser.value ?: return
        if (fromAccountId == toAccountId || amount <= 0.0) return
        viewModelScope.launch {
            val transfer = TransactionEntity(
                userId = user.id,
                title = "Account Transfer",
                amount = Money.round(amount),
                type = TransactionType.TRANSFER,
                categoryId = "cat_other",
                accountId = fromAccountId,
                transferAccountId = toAccountId,
                timestamp = timestamp,
                note = note,
                paymentMethod = null
            )
            repository.addTransaction(transfer)
        }
    }

    /**
     * Keeps a category's budget ceiling in step with its recurring expense(s). Recomputes the
     * suggested ceiling from scratch every time (not just raising it) so lowering a recurring
     * bill's amount lowers the ceiling too - but only while that budget is still auto-synced. A
     * ceiling the user set by hand on the budget screen (isAutoSynced = false) is left alone;
     * delete it there to let auto-sync suggest a fresh one from the current recurring bills.
     */
    private suspend fun syncBudgetForRecurringExpense(userId: String, categoryId: String, amount: Double, frequency: RecurringFrequency) {
        val monthlyAmount = RecurringFrequencyNormalizer.toMonthlyAmount(amount, frequency)
        val suggestedLimit = Math.ceil(monthlyAmount)
        val existingBudget = uiState.value.budgets.firstOrNull { it.categoryId == categoryId }
            ?: repository.getBudgetForCategory(userId, categoryId, _selectedMonthYear.value)

        if (existingBudget == null) {
            repository.setBudget(
                BudgetEntity(
                    userId = userId,
                    categoryId = categoryId,
                    amountLimit = suggestedLimit,
                    monthYear = _selectedMonthYear.value,
                    isAutoSynced = true
                )
            )
        } else if (existingBudget.isAutoSynced) {
            repository.setBudget(existingBudget.copy(amountLimit = suggestedLimit))
        }
    }

    fun updateTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.updateTransaction(transaction.copy(amount = Money.round(transaction.amount)))
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun setCategoryBudget(categoryId: String, limit: Double, rolloverEnabled: Boolean = false) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val existing = uiState.value.budgets.firstOrNull { it.categoryId == categoryId }
                ?: repository.getBudgetForCategory(user.id, categoryId, _selectedMonthYear.value)
            val budget = BudgetEntity(
                id = existing?.id ?: UUID.randomUUID().toString(),
                userId = user.id,
                categoryId = categoryId,
                amountLimit = Money.round(limit),
                monthYear = _selectedMonthYear.value,
                rolloverEnabled = rolloverEnabled,
                isAutoSynced = false
            )
            repository.setBudget(budget)
        }
    }

    fun deleteCategoryBudget(categoryId: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.deleteBudget(user.id, categoryId, _selectedMonthYear.value)
        }
    }

    fun addRecurringTransaction(
        title: String,
        amount: Double,
        type: TransactionType,
        categoryId: String,
        frequency: RecurringFrequency,
        // Optional finite lifespan, e.g. "12 more payments on this loan and I'm done." Null means
        // it recurs indefinitely until manually archived or deleted.
        remainingOccurrences: Int? = null,
        nextDueDate: Long? = null
    ) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            upsertRecurring(
                userId = user.id,
                title = title,
                amount = amount,
                type = type,
                categoryId = categoryId,
                accountId = "acc_checking",
                frequency = frequency,
                remainingOccurrences = remainingOccurrences,
                nextDueDate = nextDueDate
            )
        }
    }

    fun updateRecurringTransaction(recurring: RecurringTransactionEntity) {
        viewModelScope.launch {
            repository.updateRecurringTransaction(recurring)
        }
    }

    fun deleteRecurringTransaction(recurring: RecurringTransactionEntity) {
        viewModelScope.launch {
            repository.deleteRecurringTransaction(recurring)
        }
    }

    fun postRecurringTransaction(recurring: RecurringTransactionEntity) {
        viewModelScope.launch {
            addTransaction(
                title = recurring.title,
                amount = recurring.amount,
                type = recurring.type,
                categoryId = recurring.categoryId,
                accountId = recurring.accountId,
                note = "Recurring (${recurring.frequency.name.lowercase().replace('_', '-')})",
                paymentMethod = recurring.paymentMethod
            )

            val postedDueDate = recurring.nextDueDate
            val nextDueDate = RecurringScheduler.computeNextDueDate(postedDueDate, recurring.frequency)
            val remaining = RecurringScheduler.decrementOccurrences(recurring.remainingOccurrences)
            val finished = RecurringScheduler.isFinished(remaining)
            val updated = recurring.copy(
                nextDueDate = nextDueDate,
                remainingOccurrences = remaining,
                // A finite recurring item (e.g. "last loan payment") auto-archives itself once
                // its remaining occurrences run out, instead of lingering forever with a stale
                // due date, generating reminders, and counting toward budget projections.
                isArchived = recurring.isArchived || finished
            )
            repository.updateRecurringTransaction(updated)
        }
    }

    fun addAccount(account: AccountEntity) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val acc = account.copy(userId = user.id)
            repository.addAccount(acc)
        }
    }

    fun updateAccount(account: AccountEntity) {
        viewModelScope.launch {
            repository.updateAccount(account)
        }
    }

    fun deleteAccount(account: AccountEntity) {
        viewModelScope.launch {
            repository.deleteAccount(account)
        }
    }

    fun addCustomCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.addCategory(category)
        }
    }

    fun setPreferredCurrency(currency: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.updateUserCurrency(user.id, currency)
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.updateThemeMode(user.id, mode.name)
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.updateBiometricEnabled(user.id, enabled)
        }
    }

    fun completeOnboarding(currency: String, primaryGoal: String, referralSource: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.updateOnboardingData(
                userId = user.id,
                currency = currency,
                primaryGoal = primaryGoal,
                referralSource = referralSource
            )
        }
    }

    // --- Goals ---

    fun addGoal(
        name: String,
        targetAmount: Double,
        targetDate: Long? = null,
        colorHex: String = "#4CAF50",
        linkedAccountId: String? = null
    ) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.addGoal(
                GoalEntity(
                    userId = user.id,
                    name = name,
                    targetAmount = Money.round(targetAmount),
                    targetDate = targetDate,
                    colorHex = colorHex,
                    linkedAccountId = linkedAccountId
                )
            )
        }
    }

    fun updateGoal(goal: GoalEntity) {
        viewModelScope.launch {
            repository.updateGoal(goal.copy(targetAmount = Money.round(goal.targetAmount)))
        }
    }

    /**
     * Manually add (positive amount) or withdraw (negative amount) progress on a goal, on top of
     * whatever its linked account contributes. This is the only way to move progress on a goal
     * with no linkedAccountId (e.g. a cash envelope). Never lets the manual total go negative.
     */
    fun contributeToGoal(goal: GoalEntity, amount: Double) {
        viewModelScope.launch {
            val updated = Money.add(goal.savedAmount, amount).coerceAtLeast(0.0)
            repository.updateGoal(goal.copy(savedAmount = Money.round(updated)))
        }
    }

    fun deleteGoal(goal: GoalEntity) {
        viewModelScope.launch {
            repository.deleteGoal(goal)
        }
    }

    // --- Exchange rates ---

    fun setExchangeRate(fromCurrency: String, toCurrency: String, rate: Double) {
        val user = currentUser.value ?: return
        if (rate <= 0.0) return
        viewModelScope.launch {
            repository.setExchangeRate(
                ExchangeRateEntity(
                    id = "$fromCurrency-$toCurrency",
                    userId = user.id,
                    fromCurrency = fromCurrency,
                    toCurrency = toCurrency,
                    rate = rate
                )
            )
        }
    }

    fun deleteExchangeRate(rate: ExchangeRateEntity) {
        viewModelScope.launch {
            repository.deleteExchangeRate(rate)
        }
    }

    fun clearAllData() {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            repository.clearAllData(user.id)
        }
    }

    // --- 100% Zero-Cost Backup & Cloud Sync ---

    fun exportBackupJson(onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            try {
                val json = com.selfbudget.app.core.util.CloudSyncManager.exportToJsonString(user.id, repository.database)
                onSuccess(json)
            } catch (e: Exception) {
                onError(e.message ?: "Failed to create backup JSON")
            }
        }
    }

    fun restoreBackupJson(jsonString: String, onSuccess: (Int) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = com.selfbudget.app.core.util.CloudSyncManager.restoreFromJsonString(jsonString, repository.database)
            result.onSuccess { count -> onSuccess(count) }
                .onFailure { error -> onError(error.message ?: "Failed to restore backup JSON") }
        }
    }

    // --- Automated Google Drive appDataFolder Background Sync ---
    val lastDriveSyncTime = MutableStateFlow<String?>(null)
    val isDriveSyncing = MutableStateFlow(false)

    fun syncToGoogleDrive(context: Context, account: com.google.android.gms.auth.api.signin.GoogleSignInAccount?, onResult: (String) -> Unit) {
        if (account == null) {
            onResult("❌ No Google Account connected for Drive Sync")
            return
        }
        val user = currentUser.value ?: return
        viewModelScope.launch {
            isDriveSyncing.value = true
            try {
                val json = com.selfbudget.app.core.util.CloudSyncManager.exportToJsonString(user.id, repository.database)
                val result = com.selfbudget.app.core.util.GoogleDriveSyncManager.uploadToAppDataFolder(context, account, json)
                result.onSuccess { meta ->
                    lastDriveSyncTime.value = meta.formattedTime
                    onResult("✅ Synced to Google Drive appDataFolder (${meta.formattedTime})")
                }.onFailure { err ->
                    val errorMsg = err.extractCleanErrorMessage()
                    onResult("❌ Drive Sync failed: $errorMsg")
                }
            } catch (e: Exception) {
                val msg = e.extractCleanErrorMessage()
                onResult("❌ Backup preparation failed: $msg")
            } finally {
                isDriveSyncing.value = false
            }
        }
    }

    fun restoreFromGoogleDrive(context: Context, account: com.google.android.gms.auth.api.signin.GoogleSignInAccount?, onResult: (String) -> Unit) {
        if (account == null) {
            onResult("❌ No Google Account connected for Drive Sync")
            return
        }
        viewModelScope.launch {
            isDriveSyncing.value = true
            try {
                val downloadResult = com.selfbudget.app.core.util.GoogleDriveSyncManager.downloadFromAppDataFolder(context, account)
                downloadResult.onSuccess { json ->
                    val restoreResult = com.selfbudget.app.core.util.CloudSyncManager.restoreFromJsonString(json, repository.database)
                    restoreResult.onSuccess { count ->
                        onResult("✅ Restored $count items from Google Drive appDataFolder!")
                    }.onFailure { err ->
                        val msg = err.extractCleanErrorMessage()
                        onResult("❌ Data restore failed: $msg")
                    }
                }.onFailure { err ->
                    val msg = err.extractCleanErrorMessage()
                    onResult("❌ Download failed: $msg")
                }
            } finally {
                isDriveSyncing.value = false
            }
        }
    }

    private fun Throwable.extractCleanErrorMessage(): String {
        return message?.takeIf { it.isNotBlank() && it != "null" }
            ?: cause?.message?.takeIf { it.isNotBlank() && it != "null" }
            ?: cause?.toString()?.takeIf { it.isNotBlank() }
            ?: javaClass.simpleName
    }

    fun clearAuthError() {
        _authError.value = null
    }
}
