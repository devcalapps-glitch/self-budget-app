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
            CurrencyConverter.convert(balance, acc.currencyCode, baseCurrency, rates)
        }
        return Money.sum(perAccount)
    }
}
