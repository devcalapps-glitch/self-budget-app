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
    version = 14,
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
            CategoryEntity("cat_rent_mortgage", "Rent / Mortgage", "Home", "#FF9800", TransactionType.EXPENSE, true),
            CategoryEntity("cat_credit_card_loan", "Credit Card / Loan Payment", "CreditCard", "#E91E63", TransactionType.EXPENSE, true),
            CategoryEntity("cat_subscriptions", "Subscriptions", "Subscriptions", "#3F51B5", TransactionType.EXPENSE, true),
            CategoryEntity("cat_food", "Food & Dining", "Restaurant", "#FF5722", TransactionType.EXPENSE, true),
            CategoryEntity("cat_shopping", "Shopping", "ShoppingBag", "#E91E63", TransactionType.EXPENSE, true),
            CategoryEntity("cat_transport", "Transportation", "DirectionsBus", "#2196F3", TransactionType.EXPENSE, true),
            CategoryEntity("cat_bills", "Bills & Utilities", "Receipt", "#9C27B0", TransactionType.EXPENSE, true),
            CategoryEntity("cat_entertainment", "Entertainment", "Movie", "#673AB7", TransactionType.EXPENSE, true),
            CategoryEntity("cat_health", "Health & Fitness", "MedicalServices", "#009688", TransactionType.EXPENSE, true),
            CategoryEntity("cat_transfer", "Account Transfer", "CompareArrows", "#00ACC1", TransactionType.EXPENSE, true),
            CategoryEntity("cat_other", "Other", "MoreHoriz", "#607D8B", TransactionType.EXPENSE, true),

            // Tailored Income Categories
            CategoryEntity("cat_salary", "Salary", "AccountBalanceWallet", "#4CAF50", TransactionType.INCOME, true),
            CategoryEntity("cat_gifts", "Gifts", "CardGiftcard", "#E91E63", TransactionType.INCOME, true),
            CategoryEntity("cat_investment", "Investment", "TrendingUp", "#2196F3", TransactionType.INCOME, true),
            CategoryEntity("cat_side_hustle", "Side Hustle", "Work", "#FF9800", TransactionType.INCOME, true),
            CategoryEntity("cat_income_others", "Others", "MoreHoriz", "#607D8B", TransactionType.INCOME, true)
        )

        val DEFAULT_ACCOUNTS = listOf(
            AccountEntity(id = "acc_checking", userId = "system", name = "Checking Account", type = AccountType.CHECKING, initialBalance = 0.0, colorHex = "#2196F3", iconName = "AccountBalance", isDefault = true),
            AccountEntity(id = "acc_credit", userId = "system", name = "Credit Card", type = AccountType.CREDIT_CARD, initialBalance = 0.0, colorHex = "#E91E63", iconName = "CreditCard", isDefault = false),
            AccountEntity(id = "acc_cash", userId = "system", name = "Cash Wallet", type = AccountType.CASH, initialBalance = 0.0, colorHex = "#4CAF50", iconName = "Payments", isDefault = false),
            AccountEntity(id = "acc_savings", userId = "system", name = "Savings Account", type = AccountType.SAVINGS, initialBalance = 0.0, colorHex = "#9C27B0", iconName = "Savings", isDefault = false)
        )

        val MIGRATION_13_14 = object : androidx.room.migration.Migration(13, 14) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE recurring_transactions ADD COLUMN transferAccountId TEXT DEFAULT NULL")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "self_budget.db"
                )
                .addMigrations(MIGRATION_13_14)
                // PRE-LAUNCH ONLY: destructive fallback is acceptable while this app has never
                // shipped a public release (versionCode 1, no installed base yet), since there's
                // no real user data any schema bump could destroy. The moment this ships to
                // Google Play, every future version bump MUST instead ship a real
                // androidx.room.migration.Migration for that version delta - falling back to
                // destructive migration after launch means every user's transaction/budget
                // history gets silently wiped on their next app update.
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
