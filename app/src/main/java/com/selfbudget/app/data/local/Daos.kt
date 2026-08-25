package com.selfbudget.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.selfbudget.app.data.model.AccountEntity
import com.selfbudget.app.data.model.BudgetEntity
import com.selfbudget.app.data.model.CategoryEntity
import com.selfbudget.app.data.model.ExchangeRateEntity
import com.selfbudget.app.data.model.GoalEntity
import com.selfbudget.app.data.model.NetWorthSnapshotEntity
import com.selfbudget.app.data.model.RecurringTransactionEntity
import com.selfbudget.app.data.model.TransactionEntity
import com.selfbudget.app.data.model.TransactionType
import com.selfbudget.app.data.model.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY timestamp DESC")
    fun getTransactionsByUser(userId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE userId = :userId OR userId = 'system' ORDER BY timestamp DESC")
    suspend fun getAllTransactionsSync(userId: String): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE userId = :userId AND type = :type ORDER BY timestamp DESC")
    fun getTransactionsByType(userId: String, type: TransactionType): Flow<List<TransactionEntity>>

    @Query("SELECT SUM(amount) FROM transactions WHERE userId = :userId AND type = :type")
    fun getTotalByType(userId: String, type: TransactionType): Flow<Double?>

    @Query("SELECT categoryId FROM transactions WHERE userId = :userId AND LOWER(title) = LOWER(:title) ORDER BY timestamp DESC LIMIT 1")
    suspend fun getMostRecentCategoryIdForMerchant(userId: String, title: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE userId = :userId OR userId = 'system'")
    suspend fun deleteAllTransactions(userId: String)
}

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts WHERE userId = :userId")
    fun getAccountsByUser(userId: String): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE userId = :userId")
    suspend fun getAllAccountsSync(userId: String): List<AccountEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<AccountEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity)

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Delete
    suspend fun deleteAccount(account: AccountEntity)

    @Query("DELETE FROM accounts WHERE userId = :userId OR userId = 'system'")
    suspend fun deleteAllAccounts(userId: String)
}

@Dao
interface RecurringDao {
    @Query("SELECT * FROM recurring_transactions WHERE userId = :userId ORDER BY nextDueDate ASC")
    fun getRecurringByUser(userId: String): Flow<List<RecurringTransactionEntity>>

    @Query("SELECT * FROM recurring_transactions")
    suspend fun getAllRecurringSync(): List<RecurringTransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurring(recurring: RecurringTransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringList(recurring: List<RecurringTransactionEntity>)

    @Update
    suspend fun updateRecurring(recurring: RecurringTransactionEntity)

    @Delete
    suspend fun deleteRecurring(recurring: RecurringTransactionEntity)

    @Query("DELETE FROM recurring_transactions WHERE userId = :userId OR userId = 'system'")
    suspend fun deleteAllRecurring(userId: String)
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals WHERE userId = :userId ORDER BY createdAt DESC")
    fun getGoalsByUser(userId: String): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE userId = :userId")
    suspend fun getAllGoalsSync(userId: String): List<GoalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: GoalEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoals(goals: List<GoalEntity>)

    @Update
    suspend fun updateGoal(goal: GoalEntity)

    @Delete
    suspend fun deleteGoal(goal: GoalEntity)

    @Query("DELETE FROM goals WHERE userId = :userId")
    suspend fun deleteAllGoals(userId: String)
}

@Dao
interface NetWorthDao {
    @Query("SELECT * FROM net_worth_snapshots WHERE userId = :userId ORDER BY monthYear ASC")
    fun getSnapshotsByUser(userId: String): Flow<List<NetWorthSnapshotEntity>>

    @Query("SELECT * FROM net_worth_snapshots WHERE userId = :userId")
    suspend fun getAllSnapshotsSync(userId: String): List<NetWorthSnapshotEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSnapshot(snapshot: NetWorthSnapshotEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshots(snapshots: List<NetWorthSnapshotEntity>)

    @Query("DELETE FROM net_worth_snapshots WHERE userId = :userId")
    suspend fun deleteAllSnapshots(userId: String)
}

@Dao
interface ExchangeRateDao {
    @Query("SELECT * FROM exchange_rates WHERE userId = :userId")
    fun getRatesByUser(userId: String): Flow<List<ExchangeRateEntity>>

    @Query("SELECT * FROM exchange_rates WHERE userId = :userId")
    suspend fun getAllRatesSync(userId: String): List<ExchangeRateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRate(rate: ExchangeRateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRates(rates: List<ExchangeRateEntity>)

    @Delete
    suspend fun deleteRate(rate: ExchangeRateEntity)

    @Query("DELETE FROM exchange_rates WHERE userId = :userId")
    suspend fun deleteAllRates(userId: String)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories")
    suspend fun getAllCategoriesSync(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE type = :type")
    fun getCategoriesByType(type: TransactionType): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE userId = :userId AND monthYear = :monthYear")
    fun getBudgetsForMonth(userId: String, monthYear: String): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE userId = :userId AND categoryId = :categoryId AND monthYear = :monthYear LIMIT 1")
    suspend fun getBudgetForCategory(userId: String, categoryId: String, monthYear: String): BudgetEntity?

    @Query("SELECT * FROM budgets WHERE userId = :userId OR userId = 'system'")
    suspend fun getAllBudgetsSync(userId: String): List<BudgetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgets(budgets: List<BudgetEntity>)

    @Query("DELETE FROM budgets WHERE userId = :userId AND categoryId = :categoryId AND monthYear = :monthYear")
    suspend fun deleteBudget(userId: String, categoryId: String, monthYear: String)

    @Query("DELETE FROM budgets WHERE userId = :userId OR userId = 'system'")
    suspend fun deleteAllBudgets(userId: String)
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserById(id: String): UserEntity?

    @Query("SELECT * FROM users LIMIT 1")
    fun getCurrentUser(): Flow<UserEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("UPDATE users SET preferredCurrency = :currency WHERE id = :userId")
    suspend fun updatePreferredCurrency(userId: String, currency: String)

    @Query("UPDATE users SET themeMode = :mode WHERE id = :userId")
    suspend fun updateThemeMode(userId: String, mode: String)

    @Query("UPDATE users SET isBiometricEnabled = :enabled WHERE id = :userId")
    suspend fun updateBiometricEnabled(userId: String, enabled: Boolean)

    @Query("UPDATE users SET hasCompletedOnboarding = 1, preferredCurrency = :currency, primaryGoal = :primaryGoal, referralSource = :referralSource WHERE id = :userId")
    suspend fun updateOnboardingData(userId: String, currency: String, primaryGoal: String, referralSource: String)

    @Query("DELETE FROM users")
    suspend fun clearUsers()
}
