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
        return accountType == AccountType.CREDIT_CARD ||
               accountType == AccountType.LOAN ||
               accountType == AccountType.MORTGAGE ||
               accountType == AccountType.AUTO_LOAN ||
               accountType == AccountType.STUDENT_LOAN
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
     * Returns the start (00:00:00.000) and end (23:59:59.999) timestamps in ms for a "yyyy-MM" month.
     */
    fun getMonthTimestampRange(monthYear: String): Pair<Long, Long> {
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault())
            val date = sdf.parse(monthYear) ?: return Pair(0L, Long.MAX_VALUE)
            val cal = java.util.Calendar.getInstance()
            cal.time = date
            cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            cal.set(java.util.Calendar.MILLISECOND, 0)
            val start = cal.timeInMillis

            cal.set(java.util.Calendar.DAY_OF_MONTH, cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH))
            cal.set(java.util.Calendar.HOUR_OF_DAY, 23)
            cal.set(java.util.Calendar.MINUTE, 59)
            cal.set(java.util.Calendar.SECOND, 59)
            cal.set(java.util.Calendar.MILLISECOND, 999)
            val end = cal.timeInMillis
            Pair(start, end)
        } catch (_: Exception) {
            Pair(0L, Long.MAX_VALUE)
        }
    }

    /**
     * Helper to compute timestamp cutoff for the end of a "yyyy-MM" month (23:59:59.999).
     */
    fun getEndOfMonthTimestamp(monthYear: String): Long {
        return getMonthTimestampRange(monthYear).second
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
        val deltas = mutableMapOf<String, Double>()
        for (tx in allTransactions) {
            if (tx.timestamp <= cutoff) {
                when {
                    tx.type == TransactionType.INCOME -> {
                        deltas[tx.accountId] = Money.add(deltas[tx.accountId] ?: 0.0, tx.amount)
                    }
                    tx.type == TransactionType.EXPENSE -> {
                        deltas[tx.accountId] = Money.subtract(deltas[tx.accountId] ?: 0.0, tx.amount)
                        if (tx.transferAccountId != null) {
                            deltas[tx.transferAccountId] = Money.add(deltas[tx.transferAccountId] ?: 0.0, tx.amount)
                        }
                    }
                    tx.type == TransactionType.TRANSFER -> {
                        deltas[tx.accountId] = Money.subtract(deltas[tx.accountId] ?: 0.0, tx.amount)
                        if (tx.transferAccountId != null) {
                            deltas[tx.transferAccountId] = Money.add(deltas[tx.transferAccountId] ?: 0.0, tx.amount)
                        }
                    }
                }
            }
        }
        return accounts.associate { acc ->
            acc.id to Money.add(acc.initialBalance, deltas[acc.id] ?: 0.0)
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
        val deltas = mutableMapOf<String, Double>()
        for (tx in allTransactions) {
            when {
                tx.type == TransactionType.INCOME -> {
                    deltas[tx.accountId] = Money.add(deltas[tx.accountId] ?: 0.0, tx.amount)
                }
                tx.type == TransactionType.EXPENSE -> {
                    deltas[tx.accountId] = Money.subtract(deltas[tx.accountId] ?: 0.0, tx.amount)
                    if (tx.transferAccountId != null) {
                        deltas[tx.transferAccountId] = Money.add(deltas[tx.transferAccountId] ?: 0.0, tx.amount)
                    }
                }
                tx.type == TransactionType.TRANSFER -> {
                    deltas[tx.accountId] = Money.subtract(deltas[tx.accountId] ?: 0.0, tx.amount)
                    if (tx.transferAccountId != null) {
                        deltas[tx.transferAccountId] = Money.add(deltas[tx.transferAccountId] ?: 0.0, tx.amount)
                    }
                }
            }
        }
        val perAccount = accounts.map { acc ->
            val balance = Money.add(acc.initialBalance, deltas[acc.id] ?: 0.0)
            val converted = CurrencyConverter.convert(balance, acc.currencyCode, baseCurrency, rates)
            if (isLiability(acc.type)) -kotlin.math.abs(converted) else converted
        }
        return Money.sum(perAccount)
    }

    /**
     * Computes accurate historical monthly net worth snapshots up to the current wall-clock month.
     * Ensures that months with no new transactions maintain a 100% stable net worth baseline.
     * Linear scan: O(N log N + M * A).
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

        val sortedTxs = allTransactions.sortedBy { it.timestamp }
        val runningDeltas = mutableMapOf<String, Double>()
        var txIndex = 0

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
            val cutoff = cutoffCal.timeInMillis

            while (txIndex < sortedTxs.size && sortedTxs[txIndex].timestamp <= cutoff) {
                val tx = sortedTxs[txIndex]
                when {
                    tx.type == TransactionType.INCOME -> {
                        runningDeltas[tx.accountId] = Money.add(runningDeltas[tx.accountId] ?: 0.0, tx.amount)
                    }
                    tx.type == TransactionType.EXPENSE -> {
                        runningDeltas[tx.accountId] = Money.subtract(runningDeltas[tx.accountId] ?: 0.0, tx.amount)
                        if (tx.transferAccountId != null) {
                            runningDeltas[tx.transferAccountId] = Money.add(runningDeltas[tx.transferAccountId] ?: 0.0, tx.amount)
                        }
                    }
                    tx.type == TransactionType.TRANSFER -> {
                        runningDeltas[tx.accountId] = Money.subtract(runningDeltas[tx.accountId] ?: 0.0, tx.amount)
                        if (tx.transferAccountId != null) {
                            runningDeltas[tx.transferAccountId] = Money.add(runningDeltas[tx.transferAccountId] ?: 0.0, tx.amount)
                        }
                    }
                }
                txIndex++
            }

            // Only accounts that existed by this month should contribute - otherwise a newly
            // added account's initialBalance would retroactively inflate every past month.
            val accountsAsOfMonth = accounts.filter { it.createdAt <= cutoff }

            var assets = 0.0
            var liabilities = 0.0
            val convertedAccountValues = mutableListOf<Double>()

            for (acc in accountsAsOfMonth) {
                val delta = runningDeltas[acc.id] ?: 0.0
                val balance = Money.add(acc.initialBalance, delta)

                if (isLiability(acc.type)) {
                    liabilities = Money.add(liabilities, kotlin.math.abs(balance))
                } else {
                    assets = Money.add(assets, balance)
                }

                val converted = CurrencyConverter.convert(balance, acc.currencyCode, baseCurrency, rates)
                val netWorthDelta = if (isLiability(acc.type)) -kotlin.math.abs(converted) else converted
                convertedAccountValues.add(netWorthDelta)
            }

            val netWorthAtMonth = Money.sum(convertedAccountValues)

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
