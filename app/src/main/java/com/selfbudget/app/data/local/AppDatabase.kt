package com.selfbudget.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.selfbudget.app.data.model.AccountEntity
import com.selfbudget.app.data.model.AccountType
import com.selfbudget.app.data.model.ActivityLogEntity
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
        ExchangeRateEntity::class,
        ActivityLogEntity::class
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
    // v18: AccountEntity gained createdAt, so the net worth history graph stops retroactively
    // applying a newly-added account's initialBalance to months before the account existed.
    // v19: RecurringTransactionEntity gained createdAt, so the Activity feed can show a
    // "recurring added" event without confusing it with nextDueDate (which advances every cycle).
    // v20: CategoryEntity gained createdAt, so a user-created custom category can show up as a
    // "category added" event in the Activity feed too.
    // v21: New activity_log table records edit/delete/archive/contribution events against
    // transactions, goals, recurring items, accounts, and categories — creation events don't need
    // it since they're derived live from each entity's own createdAt.
    // v22: Added high-performance SQLite indices on transactions (userId+timestamp, accountId, categoryId)
    // and activity_log (userId+timestamp) to eliminate full-table scans at scale.
    version = 22,
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
    abstract fun activityLogDao(): ActivityLogDao

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

        fun tableExists(db: androidx.sqlite.db.SupportSQLiteDatabase, table: String): Boolean {
            db.query("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?", arrayOf(table)).use { cursor ->
                return cursor.moveToFirst()
            }
        }

        fun hasColumn(db: androidx.sqlite.db.SupportSQLiteDatabase, table: String, column: String): Boolean {
            if (!tableExists(db, table)) return false
            db.query("PRAGMA table_info(`$table`)").use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                if (nameIndex != -1) {
                    while (cursor.moveToNext()) {
                        if (cursor.getString(nameIndex).equals(column, ignoreCase = true)) {
                            return true
                        }
                    }
                }
            }
            return false
        }

        fun safeAddColumn(
            db: androidx.sqlite.db.SupportSQLiteDatabase,
            table: String,
            columnName: String,
            columnDef: String
        ) {
            if (tableExists(db, table) && !hasColumn(db, table, columnName)) {
                db.execSQL("ALTER TABLE `$table` ADD COLUMN $columnDef")
            }
        }

        fun createCatchupMigration(fromVersion: Int, toVersion: Int): androidx.room.migration.Migration {
            return object : androidx.room.migration.Migration(fromVersion, toVersion) {
                override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    // 1. Ensure required tables exist across all earlier schema versions
                    db.execSQL("CREATE TABLE IF NOT EXISTS `net_worth_snapshots` (`id` TEXT NOT NULL, `userId` TEXT NOT NULL, `monthYear` TEXT NOT NULL, `totalAssets` REAL NOT NULL, `totalLiabilities` REAL NOT NULL, `netWorth` REAL NOT NULL, `capturedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `exchange_rates` (`id` TEXT NOT NULL, `userId` TEXT NOT NULL, `fromCurrency` TEXT NOT NULL, `toCurrency` TEXT NOT NULL, `rate` REAL NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `goals` (`id` TEXT NOT NULL, `userId` TEXT NOT NULL, `name` TEXT NOT NULL, `targetAmount` REAL NOT NULL, `targetDate` INTEGER, `colorHex` TEXT NOT NULL, `iconName` TEXT NOT NULL, `linkedAccountId` TEXT, `monthlyTargetAmount` REAL, `savedAmount` REAL NOT NULL DEFAULT 0.0, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `activity_log` (`id` TEXT NOT NULL, `userId` TEXT NOT NULL, `entityType` TEXT NOT NULL, `action` TEXT NOT NULL, `entityId` TEXT NOT NULL, `title` TEXT NOT NULL, `amount` REAL, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`id`))")

                    // 2. Incremental column upgrades with explicit existence checks and safe defaults
                    safeAddColumn(db, "users", "hasCompletedOnboarding", "hasCompletedOnboarding INTEGER NOT NULL DEFAULT 0")
                    safeAddColumn(db, "users", "primaryGoal", "primaryGoal TEXT DEFAULT NULL")
                    safeAddColumn(db, "users", "referralSource", "referralSource TEXT DEFAULT NULL")
                    safeAddColumn(db, "users", "preferredCurrency", "preferredCurrency TEXT NOT NULL DEFAULT '$'")
                    safeAddColumn(db, "users", "themeMode", "themeMode TEXT NOT NULL DEFAULT 'SYSTEM'")
                    safeAddColumn(db, "users", "isBiometricEnabled", "isBiometricEnabled INTEGER NOT NULL DEFAULT 0")

                    safeAddColumn(db, "recurring_transactions", "remainingOccurrences", "remainingOccurrences INTEGER DEFAULT NULL")
                    safeAddColumn(db, "recurring_transactions", "transferAccountId", "transferAccountId TEXT DEFAULT NULL")
                    safeAddColumn(db, "recurring_transactions", "isArchived", "isArchived INTEGER NOT NULL DEFAULT 0")
                    safeAddColumn(db, "recurring_transactions", "paymentMethod", "paymentMethod TEXT DEFAULT 'Credit Card'")
                    safeAddColumn(db, "recurring_transactions", "createdAt", "createdAt INTEGER NOT NULL DEFAULT 0")

                    safeAddColumn(db, "transactions", "linkedRecurringId", "linkedRecurringId TEXT DEFAULT NULL")
                    safeAddColumn(db, "transactions", "recurringCycleDueDate", "recurringCycleDueDate INTEGER DEFAULT NULL")
                    safeAddColumn(db, "transactions", "transferAccountId", "transferAccountId TEXT DEFAULT NULL")
                    safeAddColumn(db, "transactions", "paymentMethod", "paymentMethod TEXT DEFAULT 'Cash'")
                    safeAddColumn(db, "transactions", "receiptImageUri", "receiptImageUri TEXT DEFAULT NULL")

                    safeAddColumn(db, "accounts", "loanTermMonths", "loanTermMonths INTEGER DEFAULT NULL")
                    safeAddColumn(db, "accounts", "creditLimit", "creditLimit REAL DEFAULT NULL")
                    safeAddColumn(db, "accounts", "interestRateApr", "interestRateApr REAL DEFAULT NULL")
                    safeAddColumn(db, "accounts", "minimumPayment", "minimumPayment REAL DEFAULT NULL")
                    safeAddColumn(db, "accounts", "currencyCode", "currencyCode TEXT NOT NULL DEFAULT 'USD'")
                    safeAddColumn(db, "accounts", "createdAt", "createdAt INTEGER NOT NULL DEFAULT 0")

                    safeAddColumn(db, "goals", "savedAmount", "savedAmount REAL NOT NULL DEFAULT 0.0")
                    safeAddColumn(db, "goals", "monthlyTargetAmount", "monthlyTargetAmount REAL DEFAULT NULL")
                    safeAddColumn(db, "goals", "linkedAccountId", "linkedAccountId TEXT DEFAULT NULL")

                    safeAddColumn(db, "budgets", "rolloverEnabled", "rolloverEnabled INTEGER NOT NULL DEFAULT 0")
                    safeAddColumn(db, "budgets", "isAutoSynced", "isAutoSynced INTEGER NOT NULL DEFAULT 1")

                    safeAddColumn(db, "categories", "isDefault", "isDefault INTEGER NOT NULL DEFAULT 0")
                    safeAddColumn(db, "categories", "isArchived", "isArchived INTEGER NOT NULL DEFAULT 0")
                    safeAddColumn(db, "categories", "createdAt", "createdAt INTEGER NOT NULL DEFAULT 0")

                    // 3. Ensure indices and constraints are preserved
                    if (tableExists(db, "budgets")) {
                        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_budgets_userId_categoryId_monthYear` ON `budgets` (`userId`, `categoryId`, `monthYear`)")
                    }
                    if (tableExists(db, "transactions")) {
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_userId_timestamp` ON `transactions` (`userId`, `timestamp`)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_accountId` ON `transactions` (`accountId`)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_categoryId` ON `transactions` (`categoryId`)")
                    }
                    if (tableExists(db, "activity_log")) {
                        db.execSQL("CREATE INDEX IF NOT EXISTS `index_activity_log_userId_timestamp` ON `activity_log` (`userId`, `timestamp`)")
                    }
                }
            }
        }

        val MIGRATIONS_ALL = (1 until 22).map { createCatchupMigration(it, 22) }.toTypedArray()

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
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
