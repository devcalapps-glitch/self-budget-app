package com.selfbudget.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.selfbudget.app.data.model.AccountEntity
import com.selfbudget.app.data.model.AccountType
import com.selfbudget.app.data.model.BudgetEntity
import com.selfbudget.app.data.model.CategoryEntity
import com.selfbudget.app.data.model.ExchangeRateEntity
import com.selfbudget.app.data.model.GoalEntity
import com.selfbudget.app.data.model.NetWorthSnapshotEntity
import com.selfbudget.app.data.model.RecurringTransactionEntity
import com.selfbudget.app.data.model.TransactionEntity
import com.selfbudget.app.data.model.TransactionType
import com.selfbudget.app.data.model.UserEntity

@Database(
    entities = [
        UserEntity::class,
        TransactionEntity::class,
        CategoryEntity::class,
        BudgetEntity::class,
        RecurringTransactionEntity::class,
        AccountEntity::class,
        GoalEntity::class,
        NetWorthSnapshotEntity::class,
        ExchangeRateEntity::class
    ],
    // v8: RecurringTransactionEntity dropped autoPostEnabled/lastPostedDate (auto-post feature
    // removed - see BillReminderWorker) and gained remainingOccurrences (finite recurring items).
    // v9: GoalEntity gained savedAmount (manual contribution tracking for goals with no linked
    // account, e.g. a cash envelope).
    // v10: UserEntity gained hasCompletedOnboarding, primaryGoal, and referralSource for onboarding questionnaire.
    // v11: BudgetEntity added unique index on (userId, categoryId, monthYear).
    // v12: BudgetEntity gained isAutoSynced, so a manually-set budget ceiling stops being
    // silently overwritten by recurring-bill auto-sync (see MainViewModel.upsertRecurring).
    // v13: CategoryEntity gained isArchived to support archiving custom categories.
    // v14: RecurringTransactionEntity gained transferAccountId, so a recurring debt payment can
    // target a specific Credit Card / Loan account and actually reduce its balance when posted -
    // see MIGRATION_13_14 below, a real migration (not destructive fallback) since this app now
    // has an installed base with real transaction history.
    // v15: TransactionEntity gained linkedRecurringId/recurringCycleDueDate, so deleting a
    // mistakenly-posted recurring transaction rolls the recurring item's due date back instead of
    // leaving it advanced to the following cycle - see MainViewModel.deleteTransaction.
    // v16: AccountEntity gained loanTermMonths for fixed-term amortized mortgages and loans.
    // v17: GoalEntity gained monthlyTargetAmount for monthly savings target pacing.
    version = 17,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun recurringDao(): RecurringDao
    abstract fun accountDao(): AccountDao
    abstract fun goalDao(): GoalDao
    abstract fun netWorthDao(): NetWorthDao
    abstract fun exchangeRateDao(): ExchangeRateDao

    companion object {
        val DEFAULT_CATEGORIES = listOf(
            CategoryEntity("cat_rent_mortgage", "Rent / Mortgage", "Home", "#7C3AED", TransactionType.EXPENSE, true),
            CategoryEntity("cat_credit_card_loan", "Credit Card / Loan Payment", "CreditCard", "#DC2626", TransactionType.EXPENSE, true),
            CategoryEntity("cat_groceries", "Groceries", "ShoppingCart", "#059669", TransactionType.EXPENSE, true),
            CategoryEntity("cat_food", "Food & Dining", "Restaurant", "#EA580C", TransactionType.EXPENSE, true),
            CategoryEntity("cat_bills", "Bills & Utilities", "Receipt", "#2563EB", TransactionType.EXPENSE, true),
            CategoryEntity("cat_subscriptions", "Subscriptions", "Subscriptions", "#4F46E5", TransactionType.EXPENSE, true),
            CategoryEntity("cat_shopping", "Shopping", "ShoppingBag", "#DB2777", TransactionType.EXPENSE, true),
            CategoryEntity("cat_transport", "Transportation", "DirectionsBus", "#0891B2", TransactionType.EXPENSE, true),
            CategoryEntity("cat_travel", "Travel", "Flight", "#0284C7", TransactionType.EXPENSE, true),
            CategoryEntity("cat_fitness", "Fitness", "FitnessCenter", "#0F766E", TransactionType.EXPENSE, true),
            CategoryEntity("cat_health", "Medical & Healthcare", "MedicalServices", "#15803D", TransactionType.EXPENSE, true),
            CategoryEntity("cat_entertainment", "Entertainment", "Movie", "#9333EA", TransactionType.EXPENSE, true),
            CategoryEntity("cat_investment_expense", "Investments", "TrendingUp", "#0891B2", TransactionType.EXPENSE, true),
            CategoryEntity("cat_transfer", "Account Transfer", "CompareArrows", "#475569", TransactionType.EXPENSE, true),
            CategoryEntity("cat_other", "Other", "MoreHoriz", "#64748B", TransactionType.EXPENSE, true),

            // Tailored Income Categories
            CategoryEntity("cat_salary", "Salary", "AccountBalanceWallet", "#059669", TransactionType.INCOME, true),
            CategoryEntity("cat_gifts", "Gifts", "CardGiftcard", "#DB2777", TransactionType.INCOME, true),
            CategoryEntity("cat_investment", "Investment", "TrendingUp", "#0891B2", TransactionType.INCOME, true),
            CategoryEntity("cat_side_hustle", "Side Hustle", "Work", "#EA580C", TransactionType.INCOME, true),
            CategoryEntity("cat_income_others", "Others", "MoreHoriz", "#64748B", TransactionType.INCOME, true)
        )

        val DEFAULT_ACCOUNTS = listOf(
            AccountEntity(id = "acc_checking", userId = "system", name = "Checking Account", type = AccountType.CHECKING, initialBalance = 0.0, colorHex = "#2563EB", iconName = "AccountBalance", isDefault = true),
            AccountEntity(id = "acc_credit", userId = "system", name = "Credit Card", type = AccountType.CREDIT_CARD, initialBalance = 0.0, colorHex = "#DC2626", iconName = "CreditCard", isDefault = false),
            AccountEntity(id = "acc_cash", userId = "system", name = "Cash Wallet", type = AccountType.CASH, initialBalance = 0.0, colorHex = "#059669", iconName = "Payments", isDefault = false),
            AccountEntity(id = "acc_savings", userId = "system", name = "Savings Account", type = AccountType.SAVINGS, initialBalance = 0.0, colorHex = "#0F766E", iconName = "Savings", isDefault = false)
        )

        private fun safeAddColumn(db: androidx.sqlite.db.SupportSQLiteDatabase, table: String, columnDef: String) {
            try {
                db.execSQL("ALTER TABLE $table ADD COLUMN $columnDef")
            } catch (_: Exception) {}
        }

        private fun createCatchupMigration(fromVersion: Int, toVersion: Int): androidx.room.migration.Migration {
            return object : androidx.room.migration.Migration(fromVersion, toVersion) {
                override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    safeAddColumn(db, "recurring_transactions", "transferAccountId TEXT DEFAULT NULL")
                    safeAddColumn(db, "transactions", "linkedRecurringId TEXT DEFAULT NULL")
                    safeAddColumn(db, "transactions", "recurringCycleDueDate INTEGER DEFAULT NULL")
                    safeAddColumn(db, "accounts", "loanTermMonths INTEGER DEFAULT NULL")
                    safeAddColumn(db, "goals", "monthlyTargetAmount REAL DEFAULT NULL")
                }
            }
        }

        val MIGRATIONS_ALL = (1 until 17).map { createCatchupMigration(it, 17) }.toTypedArray()

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "self_budget.db"
                )
                .addMigrations(*MIGRATIONS_ALL)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
