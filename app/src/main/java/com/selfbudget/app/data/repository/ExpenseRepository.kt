package com.selfbudget.app.data.repository

import com.selfbudget.app.data.local.AccountDao
import com.selfbudget.app.data.local.AppDatabase
import com.selfbudget.app.data.local.BudgetDao
import com.selfbudget.app.data.local.CategoryDao
import com.selfbudget.app.data.local.ExchangeRateDao
import com.selfbudget.app.data.local.GoalDao
import com.selfbudget.app.data.local.NetWorthDao
import com.selfbudget.app.data.local.RecurringDao
import com.selfbudget.app.data.local.TransactionDao
import com.selfbudget.app.data.local.UserDao
import com.selfbudget.app.data.model.AccountEntity
import com.selfbudget.app.data.model.BudgetEntity
import com.selfbudget.app.data.model.CategoryEntity
import com.selfbudget.app.data.model.ExchangeRateEntity
import com.selfbudget.app.data.model.GoalEntity
import com.selfbudget.app.data.model.NetWorthSnapshotEntity
import com.selfbudget.app.data.model.RecurringTransactionEntity
import com.selfbudget.app.data.model.TransactionEntity
import com.selfbudget.app.data.model.TransactionType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpenseRepository @Inject constructor(
    val database: AppDatabase,
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val budgetDao: BudgetDao,
    private val recurringDao: RecurringDao,
    private val userDao: UserDao,
    private val accountDao: AccountDao,
    private val goalDao: GoalDao,
    private val netWorthDao: NetWorthDao,
    private val exchangeRateDao: ExchangeRateDao
) {
    fun getTransactions(userId: String): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsByUser(userId)

    fun getTotalIncome(userId: String): Flow<Double?> =
        transactionDao.getTotalByType(userId, TransactionType.INCOME)

    fun getTotalExpense(userId: String): Flow<Double?> =
        transactionDao.getTotalByType(userId, TransactionType.EXPENSE)

    suspend fun getMerchantCategoryId(userId: String, title: String): String? =
        transactionDao.getMostRecentCategoryIdForMerchant(userId, title)

    fun getCategories(): Flow<List<CategoryEntity>> =
        categoryDao.getAllCategories()

    suspend fun addCategory(category: CategoryEntity) {
        categoryDao.insertCategory(category)
    }

    fun getAccounts(userId: String): Flow<List<AccountEntity>> =
        accountDao.getAccountsByUser(userId)

    suspend fun addAccount(account: AccountEntity) {
        accountDao.insertAccount(account)
    }

    suspend fun updateAccount(account: AccountEntity) {
        accountDao.updateAccount(account)
    }

    suspend fun deleteAccount(account: AccountEntity) {
        accountDao.deleteAccount(account)
    }

    suspend fun addTransaction(transaction: TransactionEntity) {
        transactionDao.insertTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: TransactionEntity) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        transactionDao.deleteTransaction(transaction)
    }

    fun getBudgets(userId: String, monthYear: String): Flow<List<BudgetEntity>> =
        budgetDao.getBudgetsForMonth(userId, monthYear)

    suspend fun getBudgetForCategory(userId: String, categoryId: String, monthYear: String): BudgetEntity? =
        budgetDao.getBudgetForCategory(userId, categoryId, monthYear)

    suspend fun setBudget(budget: BudgetEntity) {
        budgetDao.insertBudget(budget)
    }

    suspend fun deleteBudget(userId: String, categoryId: String, monthYear: String) {
        budgetDao.deleteBudget(userId, categoryId, monthYear)
    }

    fun getRecurringTransactions(userId: String): Flow<List<RecurringTransactionEntity>> =
        recurringDao.getRecurringByUser(userId)

    suspend fun addRecurringTransaction(recurring: RecurringTransactionEntity) {
        recurringDao.insertRecurring(recurring)
    }

    suspend fun updateRecurringTransaction(recurring: RecurringTransactionEntity) {
        recurringDao.updateRecurring(recurring)
    }

    suspend fun deleteRecurringTransaction(recurring: RecurringTransactionEntity) {
        recurringDao.deleteRecurring(recurring)
    }

    suspend fun updateUserCurrency(userId: String, currency: String) {
        userDao.updatePreferredCurrency(userId, currency)
    }

    suspend fun updateThemeMode(userId: String, mode: String) {
        userDao.updateThemeMode(userId, mode)
    }

    suspend fun updateBiometricEnabled(userId: String, enabled: Boolean) {
        userDao.updateBiometricEnabled(userId, enabled)
    }

    suspend fun updateOnboardingData(userId: String, currency: String, primaryGoal: String, referralSource: String) {
        userDao.updateOnboardingData(userId, currency, primaryGoal, referralSource)
    }

    // --- Goals ---

    fun getGoals(userId: String): Flow<List<GoalEntity>> =
        goalDao.getGoalsByUser(userId)

    suspend fun addGoal(goal: GoalEntity) {
        goalDao.insertGoal(goal)
    }

    suspend fun updateGoal(goal: GoalEntity) {
        goalDao.updateGoal(goal)
    }

    suspend fun deleteGoal(goal: GoalEntity) {
        goalDao.deleteGoal(goal)
    }

    // --- Net worth history ---

    fun getNetWorthSnapshots(userId: String): Flow<List<NetWorthSnapshotEntity>> =
        netWorthDao.getSnapshotsByUser(userId)

    suspend fun upsertNetWorthSnapshot(snapshot: NetWorthSnapshotEntity) {
        netWorthDao.upsertSnapshot(snapshot)
    }

    // --- Exchange rates (manual, offline multi-currency conversion) ---

    fun getExchangeRates(userId: String): Flow<List<ExchangeRateEntity>> =
        exchangeRateDao.getRatesByUser(userId)

    suspend fun setExchangeRate(rate: ExchangeRateEntity) {
        exchangeRateDao.upsertRate(rate)
    }

    suspend fun deleteExchangeRate(rate: ExchangeRateEntity) {
        exchangeRateDao.deleteRate(rate)
    }

    suspend fun clearAllData(userId: String) {
        transactionDao.deleteAllTransactions(userId)
        budgetDao.deleteAllBudgets(userId)
        recurringDao.deleteAllRecurring(userId)
        accountDao.deleteAllAccounts(userId)
        goalDao.deleteAllGoals(userId)
        netWorthDao.deleteAllSnapshots(userId)
        exchangeRateDao.deleteAllRates(userId)
    }
}
