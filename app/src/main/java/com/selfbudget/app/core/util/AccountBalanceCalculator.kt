package com.selfbudget.app.core.util

import com.selfbudget.app.data.model.AccountEntity
import com.selfbudget.app.data.model.AccountType
import com.selfbudget.app.data.model.TransactionEntity
import com.selfbudget.app.data.model.TransactionType

/**
 * Computes a payment account's live balance from its starting balance plus every transaction
 * that has touched it, including transfers in/out of other accounts and liability/credit card logic.
 */
object AccountBalanceCalculator {

    /**
     * Checks if the given account type represents a liability (e.g. Credit Card or Loan).
     */
    fun isLiability(accountType: AccountType): Boolean {
        return accountType == AccountType.CREDIT_CARD || accountType == AccountType.LOAN
    }

    /**
     * Current balance of a single account, in that account's own currency.
     *
     * For all accounts, expenses charged directly to the account reduce its balance delta (-tx.amount),
     * while income, transfers into the account, and debt payoff payments (EXPENSE targeting transferAccountId)
     * increase its balance delta (+tx.amount).
     *
     * For credit cards / loans, negative balances represent debt owed (e.g. -$150 = $150 credit card balance).
     * Purchases on credit card decrease the balance (making it more negative), while payments increase the balance.
     */
    fun computeBalance(account: AccountEntity, allTransactions: List<TransactionEntity>): Double {
        val delta = Money.sum(allTransactions.mapNotNull { tx ->
            when {
                // Income into account
                tx.type == TransactionType.INCOME && tx.accountId == account.id -> tx.amount

                // Expense charged to account (e.g. buying dinner on credit card or checking)
                tx.type == TransactionType.EXPENSE && tx.accountId == account.id -> -tx.amount

                // Transfer out of account
                tx.type == TransactionType.TRANSFER && tx.accountId == account.id -> -tx.amount

                // Transfer into account
                tx.type == TransactionType.TRANSFER && tx.transferAccountId == account.id -> tx.amount

                // Expense payment targeting debt account (e.g. paying off credit card or loan debt from checking)
                tx.type == TransactionType.EXPENSE && tx.transferAccountId == account.id -> tx.amount

                else -> null
            }
        })
        return Money.add(account.initialBalance, delta)
    }

    /**
     * Helper to compute timestamp cutoff for the end of a "yyyy-MM" month (23:59:59.999).
     */
    fun getEndOfMonthTimestamp(monthYear: String): Long {
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault())
            val date = sdf.parse(monthYear) ?: return Long.MAX_VALUE
            val cal = java.util.Calendar.getInstance()
            cal.time = date
            cal.set(java.util.Calendar.DAY_OF_MONTH, cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH))
            cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
            cal.set(java.util.Calendar.MINUTE, 59)
            cal.set(java.util.Calendar.SECOND, 59)
            cal.set(java.util.Calendar.MILLISECOND, 999)
            cal.timeInMillis
        } catch (_: Exception) {
            Long.MAX_VALUE
        }
    }

    /**
     * Computes per-account balances as of the end of the given monthYear ("yyyy-MM").
     * Includes initial balances plus all transactions up to the end of that month.
     */
    fun computeBalancesAsOfMonth(
        accounts: List<AccountEntity>,
        allTransactions: List<TransactionEntity>,
        monthYear: String
    ): Map<String, Double> {
        val cutoff = getEndOfMonthTimestamp(monthYear)
        val txsUpToMonth = allTransactions.filter { it.timestamp <= cutoff }
        return accounts.associate { acc ->
            acc.id to computeBalance(acc, txsUpToMonth)
        }
    }

    /**
     * Total net worth across all accounts in `baseCurrency`.
     * Balances are converted and summed; negative balances (debts/liabilities) correctly reduce net worth.
     */
    fun computeTotalInBaseCurrency(
        accounts: List<AccountEntity>,
        allTransactions: List<TransactionEntity>,
        baseCurrency: String,
        rates: List<com.selfbudget.app.data.model.ExchangeRateEntity>
    ): Double {
        val perAccount = accounts.map { acc ->
            val balance = computeBalance(acc, allTransactions)
            val converted = CurrencyConverter.convert(balance, acc.currencyCode, baseCurrency, rates)
            if (isLiability(acc.type)) -kotlin.math.abs(converted) else converted
        }
        return Money.sum(perAccount)
    }

    /**
     * Computes accurate historical monthly net worth snapshots up to the current wall-clock month.
     * Ensures that months with no new transactions maintain a 100% stable net worth baseline.
     */
    fun computeHistoricalSnapshots(
        userId: String,
        accounts: List<AccountEntity>,
        allTransactions: List<TransactionEntity>,
        baseCurrency: String,
        rates: List<com.selfbudget.app.data.model.ExchangeRateEntity>
    ): List<com.selfbudget.app.data.model.NetWorthSnapshotEntity> {
        val sdf = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault())
        val currentMonthKey = sdf.format(java.util.Date())
        val prevCal = java.util.Calendar.getInstance().apply { add(java.util.Calendar.MONTH, -1) }
        val prevMonthKey = sdf.format(prevCal.time)

        val txMonths = allTransactions.map { sdf.format(java.util.Date(it.timestamp)) }
        val allMonths = (txMonths + listOf(prevMonthKey, currentMonthKey)).distinct().sorted()

        if (allMonths.isEmpty()) return emptyList()

        val earliestMonthStr = allMonths.first()
        val startCal = java.util.Calendar.getInstance().apply {
            val d = try { sdf.parse(earliestMonthStr) } catch (_: Exception) { null }
            if (d != null) time = d
            set(java.util.Calendar.DAY_OF_MONTH, 1)
        }
        val nowCal = java.util.Calendar.getInstance()

        val result = mutableListOf<com.selfbudget.app.data.model.NetWorthSnapshotEntity>()
        val currCal = startCal.clone() as java.util.Calendar

        while (!currCal.after(nowCal)) {
            val monthKey = sdf.format(currCal.time)

            val cutoffCal = currCal.clone() as java.util.Calendar
            cutoffCal.set(java.util.Calendar.DAY_OF_MONTH, cutoffCal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH))
            cutoffCal.set(java.util.Calendar.HOUR_OF_DAY, 23)
            cutoffCal.set(java.util.Calendar.MINUTE, 59)
            cutoffCal.set(java.util.Calendar.SECOND, 59)
            cutoffCal.set(java.util.Calendar.MILLISECOND, 999)

            val txsUpToMonth = allTransactions.filter { it.timestamp <= cutoffCal.timeInMillis }
            val netWorthAtMonth = computeTotalInBaseCurrency(
                accounts = accounts,
                allTransactions = txsUpToMonth,
                baseCurrency = baseCurrency,
                rates = rates
            )

            val monthBalances = accounts.associate { acc ->
                acc.id to computeBalance(acc, txsUpToMonth)
            }
            val assets = accounts.sumOf { acc ->
                val b = monthBalances[acc.id] ?: 0.0
                if (!isLiability(acc.type)) b else 0.0
            }
            val liabilities = accounts.sumOf { acc ->
                val b = monthBalances[acc.id] ?: 0.0
                if (isLiability(acc.type)) kotlin.math.abs(b) else 0.0
            }

            result.add(
                com.selfbudget.app.data.model.NetWorthSnapshotEntity(
                    id = "$userId-$monthKey",
                    userId = userId,
                    monthYear = monthKey,
                    totalAssets = Money.round(assets),
                    totalLiabilities = Money.round(liabilities),
                    netWorth = netWorthAtMonth
                )
            )

            currCal.add(java.util.Calendar.MONTH, 1)
        }

        return result
    }
}
