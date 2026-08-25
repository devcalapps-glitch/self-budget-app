package com.selfbudget.app.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class TransactionType {
    EXPENSE,
    INCOME,
    TRANSFER
}

enum class RecurringFrequency {
    WEEKLY,
    BI_WEEKLY,
    MONTHLY,
    YEARLY
}

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class AccountType {
    CHECKING,
    CREDIT_CARD,
    SAVINGS,
    CASH,
    INVESTMENT,
    LOAN
}

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val name: String,
    val type: AccountType = AccountType.CHECKING,
    val initialBalance: Double = 0.0,
    val colorHex: String = "#2196F3",
    val iconName: String = "AccountBalance",
    val isDefault: Boolean = false,
    // Real multi-currency: each account is tracked in its own currency.
    val currencyCode: String = "USD",
    // Debt / liability tracking for CREDIT_CARD and LOAN accounts.
    val creditLimit: Double? = null,
    val interestRateApr: Double? = null,
    val minimumPayment: Double? = null
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val categoryId: String,
    val accountId: String = "acc_checking",
    val timestamp: Long = System.currentTimeMillis(),
    val note: String? = null,
    val paymentMethod: String? = "Cash",
    val receiptImageUri: String? = null,
    // Only set when type == TRANSFER: the destination account. `accountId` is the source.
    val transferAccountId: String? = null
)

@Entity(tableName = "recurring_transactions")
data class RecurringTransactionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val categoryId: String,
    val accountId: String = "acc_checking",
    val frequency: RecurringFrequency = RecurringFrequency.MONTHLY,
    val nextDueDate: Long = System.currentTimeMillis(),
    val note: String? = null,
    val paymentMethod: String? = "Credit Card",
    // When true, item is paused/archived (also set automatically once remainingOccurrences hits 0)
    val isArchived: Boolean = false,
    // Optional finite lifespan: e.g. "12 more payments on this loan and I'm done." Null means the
    // item recurs indefinitely. Decremented by 1 each time it's posted (MainViewModel.postRecurringTransaction);
    // the item is auto-archived once this reaches 0, so a finished bill/loan doesn't linger forever
    // and doesn't keep generating reminders or counting toward budget/cash-flow projections.
    val remainingOccurrences: Int? = null
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val iconName: String,
    val colorHex: String,
    val type: TransactionType,
    val isDefault: Boolean = false
)

@Entity(
    tableName = "budgets",
    indices = [Index(value = ["userId", "categoryId", "monthYear"], unique = true)]
)
data class BudgetEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val categoryId: String,
    val amountLimit: Double,
    val monthYear: String, // e.g., "2026-08"
    // When true, unspent (or overspent) amount from the previous month's budget for this
    // category carries into this month's effective limit. See BudgetRollover.kt.
    val rolloverEnabled: Boolean = false,
    // True while this category's limit is still the auto-suggested one derived from its
    // recurring bills (see MainViewModel.syncBudgetForRecurringExpense). Set to false the
    // moment the user sets a limit by hand on the budget screen, so a hand-picked number is
    // never silently overwritten the next time a recurring bill in that category changes.
    val isAutoSynced: Boolean = true
)

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: String,
    val email: String,
    val displayName: String?,
    val photoUrl: String?,
    // NOTE: The Google ID token is intentionally NOT persisted here. It was previously stored
    // in plaintext in the local Room database despite never being read back or sent to any
    // backend for verification, so it was a pure liability. Sign-in identity is established
    // for the duration of the Credential Manager call only.
    val preferredCurrency: String = "$",
    val themeMode: String = "SYSTEM",
    val isBiometricEnabled: Boolean = false,
    val hasCompletedOnboarding: Boolean = false,
    val primaryGoal: String? = null,
    val referralSource: String? = null
)

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val name: String,
    val targetAmount: Double,
    val targetDate: Long? = null,
    val colorHex: String = "#4CAF50",
    val iconName: String = "Savings",
    // Progress is derived from this account's computed balance (see AccountBalanceCalculator),
    // so a goal simply points at the savings account/wallet the user is funding it from.
    val linkedAccountId: String? = null,
    // Manual contributions (e.g. a cash envelope with no bank account behind it), added on top of
    // the linked account's balance if any. Adjusted by hand via the "+ Add Contribution" action -
    // this is the only progress source for a goal with no linkedAccountId.
    val savedAmount: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "net_worth_snapshots")
data class NetWorthSnapshotEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val monthYear: String, // e.g., "2026-08" — one row per user per month, upserted as the month progresses
    val totalAssets: Double,
    val totalLiabilities: Double,
    val netWorth: Double,
    val capturedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "exchange_rates")
data class ExchangeRateEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val fromCurrency: String,
    val toCurrency: String,
    // Units of `toCurrency` per 1 unit of `fromCurrency`. User-editable in Settings since this
    // app is offline-first and does not call out to a live FX rate service.
    val rate: Double,
    val updatedAt: Long = System.currentTimeMillis()
)
