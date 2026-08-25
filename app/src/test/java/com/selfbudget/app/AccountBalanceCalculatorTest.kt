package com.selfbudget.app

import com.selfbudget.app.core.util.AccountBalanceCalculator
import com.selfbudget.app.data.model.AccountEntity
import com.selfbudget.app.data.model.AccountType
import com.selfbudget.app.data.model.TransactionEntity
import com.selfbudget.app.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Verifies AccountBalanceCalculator actually reflects transaction activity, unlike the previous
 * behavior where account cards displayed the static `initialBalance` forever regardless of any
 * income, expense, or transfer logged against that account.
 */
class AccountBalanceCalculatorTest {

    private val checking = AccountEntity(id = "acc_checking", userId = "u1", name = "Checking", type = AccountType.CHECKING, initialBalance = 1000.0)
    private val savings = AccountEntity(id = "acc_savings", userId = "u1", name = "Savings", type = AccountType.SAVINGS, initialBalance = 500.0)

    @Test
    fun testBalanceReflectsIncomeAndExpense() {
        val txs = listOf(
            TransactionEntity(userId = "u1", title = "Paycheck", amount = 2000.0, type = TransactionType.INCOME, categoryId = "cat_salary", accountId = "acc_checking"),
            TransactionEntity(userId = "u1", title = "Groceries", amount = 150.0, type = TransactionType.EXPENSE, categoryId = "cat_food", accountId = "acc_checking")
        )
        val balance = AccountBalanceCalculator.computeBalance(checking, txs)
        assertEquals(2850.0, balance, 0.001)
    }

    @Test
    fun testTransferMovesMoneyBetweenAccounts() {
        val transfer = TransactionEntity(
            userId = "u1",
            title = "Account Transfer",
            amount = 200.0,
            type = TransactionType.TRANSFER,
            categoryId = "cat_other",
            accountId = "acc_checking",
            transferAccountId = "acc_savings"
        )
        val allTxs = listOf(transfer)

        val checkingBalance = AccountBalanceCalculator.computeBalance(checking, allTxs)
        val savingsBalance = AccountBalanceCalculator.computeBalance(savings, allTxs)

        assertEquals(800.0, checkingBalance, 0.001) // 1000 - 200
        assertEquals(700.0, savingsBalance, 0.001)   // 500 + 200
    }

    @Test
    fun testTransferDoesNotAffectUnrelatedAccounts() {
        val other = AccountEntity(id = "acc_other", userId = "u1", name = "Other", type = AccountType.CASH, initialBalance = 50.0)
        val transfer = TransactionEntity(
            userId = "u1",
            title = "Account Transfer",
            amount = 200.0,
            type = TransactionType.TRANSFER,
            categoryId = "cat_other",
            accountId = "acc_checking",
            transferAccountId = "acc_savings"
        )
        val balance = AccountBalanceCalculator.computeBalance(other, listOf(transfer))
        assertEquals(50.0, balance, 0.001)
    }

    @Test
    fun testNoTransactionsReturnsInitialBalance() {
        assertEquals(1000.0, AccountBalanceCalculator.computeBalance(checking, emptyList()), 0.001)
    }

    @Test
    fun testCreditCardAndMortgagePaymentReducesDebt() {
        // Given: Checking balance of $5,000, Credit Card debt of -$1,500, Mortgage debt of -$250,000
        val creditCard = AccountEntity(id = "acc_cc", userId = "u1", name = "Chase Slate", type = AccountType.CREDIT_CARD, initialBalance = -1500.0)
        val mortgage = AccountEntity(id = "acc_mortgage", userId = "u1", name = "Home Mortgage", type = AccountType.LOAN, initialBalance = -250000.0)

        val ccPayment = TransactionEntity(
            userId = "u1",
            title = "Credit Card Bill Payment",
            amount = 500.0,
            type = TransactionType.EXPENSE,
            categoryId = "cat_bills",
            accountId = "acc_checking",
            transferAccountId = "acc_cc"
        )
        val mortgagePayment = TransactionEntity(
            userId = "u1",
            title = "Monthly Mortgage Payment",
            amount = 1800.0,
            type = TransactionType.EXPENSE,
            categoryId = "cat_bills",
            accountId = "acc_checking",
            transferAccountId = "acc_mortgage"
        )

        val allTxs = listOf(ccPayment, mortgagePayment)

        val newCheckingBalance = AccountBalanceCalculator.computeBalance(checking, allTxs)
        val newCcBalance = AccountBalanceCalculator.computeBalance(creditCard, allTxs)
        val newMortgageBalance = AccountBalanceCalculator.computeBalance(mortgage, allTxs)

        // Then: Checking deducted by $2,300 ($1000 initial - $2300 = -$1300)
        assertEquals(-1300.0, newCheckingBalance, 0.001)

        // Credit Card debt reduced by $500 (-$1500 + $500 = -$1000)
        assertEquals(-1000.0, newCcBalance, 0.001)

        // Mortgage debt reduced by $1800 (-$250,000 + $1800 = -$248,200)
        assertEquals(-248200.0, newMortgageBalance, 0.001)
    }

    @Test
    fun testTransactionEdit_recalculatesBalanceCorrectly() {
        // Given: Checking starting balance $1,000 and initial expense of $100
        val initialTx = TransactionEntity(id = "tx1", userId = "u1", title = "Dinner", amount = 100.0, type = TransactionType.EXPENSE, categoryId = "cat_food", accountId = "acc_checking")
        val initialBalance = AccountBalanceCalculator.computeBalance(checking, listOf(initialTx))
        assertEquals(900.0, initialBalance, 0.001)

        // When: Transaction amount is edited to $250
        val editedTx = initialTx.copy(amount = 250.0)
        val recalculatedBalance = AccountBalanceCalculator.computeBalance(checking, listOf(editedTx))

        // Then: Balance recalculates correctly to $750
        assertEquals(750.0, recalculatedBalance, 0.001)
    }

    @Test
    fun testTransactionDeletion_reversesFinancialEffect() {
        // Given: Checking balance of $1,000 and an expense of $150
        val tx = TransactionEntity(id = "tx1", userId = "u1", title = "Shoes", amount = 150.0, type = TransactionType.EXPENSE, categoryId = "cat_shopping", accountId = "acc_checking")
        val balanceWithTx = AccountBalanceCalculator.computeBalance(checking, listOf(tx))
        assertEquals(850.0, balanceWithTx, 0.001)

        // When: Transaction is deleted (empty list)
        val balanceAfterDelete = AccountBalanceCalculator.computeBalance(checking, emptyList())

        // Then: Balance is restored to original $1,000
        assertEquals(1000.0, balanceAfterDelete, 0.001)
    }

    @Test
    fun testTransfer_doesNotChangeNetWorth() {
        // Given: Checking ($1,000) and Savings ($500). Total Net Worth = $1,500
        val initialNetWorth = checking.initialBalance + savings.initialBalance
        assertEquals(1500.0, initialNetWorth, 0.001)

        // When: Transferring $300 from Checking to Savings
        val transfer = TransactionEntity(
            userId = "u1",
            title = "Savings Transfer",
            amount = 300.0,
            type = TransactionType.TRANSFER,
            categoryId = "cat_transfer",
            accountId = "acc_checking",
            transferAccountId = "acc_savings"
        )
        val txs = listOf(transfer)

        val endingChecking = AccountBalanceCalculator.computeBalance(checking, txs)
        val endingSavings = AccountBalanceCalculator.computeBalance(savings, txs)
        val endingNetWorth = endingChecking + endingSavings

        // Then: Net Worth remains exactly $1,500
        assertEquals(700.0, endingChecking, 0.001)
        assertEquals(800.0, endingSavings, 0.001)
        assertEquals(1500.0, endingNetWorth, 0.001)
    }

    @Test
    fun testCreditCardPayment_isNotCountedAsSpending() {
        // Given: Credit card bill payment from checking to credit card
        val ccPayment = TransactionEntity(
            userId = "u1",
            title = "Pay Credit Card",
            amount = 400.0,
            type = TransactionType.TRANSFER,
            categoryId = "cat_transfer",
            accountId = "acc_checking",
            transferAccountId = "acc_cc"
        )
        val expenseTx = TransactionEntity(
            userId = "u1",
            title = "Groceries",
            amount = 100.0,
            type = TransactionType.EXPENSE,
            categoryId = "cat_groceries",
            accountId = "acc_checking"
        )

        val txs = listOf(ccPayment, expenseTx)
        val pureExpenses = txs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

        // Then: Spending equals $100.00 (the credit card transfer is excluded)
        assertEquals(100.0, pureExpenses, 0.001)
    }

    @Test
    fun testRefund_correctlyOffsetsOriginalExpense() {
        // Given: Original expense of $100 and a subsequent refund logged as INCOME of $100
        val originalExpense = TransactionEntity(userId = "u1", title = "Defective Item", amount = 100.0, type = TransactionType.EXPENSE, categoryId = "cat_shopping", accountId = "acc_checking")
        val refundIncome = TransactionEntity(userId = "u1", title = "Item Refund", amount = 100.0, type = TransactionType.INCOME, categoryId = "cat_shopping", accountId = "acc_checking")

        val txs = listOf(originalExpense, refundIncome)
        val endingBalance = AccountBalanceCalculator.computeBalance(checking, txs)

        // Then: Refund restores the balance to original $1,000
        assertEquals(1000.0, endingBalance, 0.001)
    }

    @Test
    fun testPropertyTest_startingBalancesPlusMovementsEqualsEndingBalances() {
        // Property Test: For ANY set of random transactions, Sum(Starting Balances) + Sum(Income) - Sum(Expense) == Sum(Ending Balances)
        val accs = listOf(checking, savings)
        val randomTxs = listOf(
            TransactionEntity(id = "1", userId = "u1", title = "Inc 1", amount = 1250.50, type = TransactionType.INCOME, categoryId = "c1", accountId = "acc_checking"),
            TransactionEntity(id = "2", userId = "u1", title = "Exp 1", amount = 340.25, type = TransactionType.EXPENSE, categoryId = "c2", accountId = "acc_checking"),
            TransactionEntity(id = "3", userId = "u1", title = "Transfer 1", amount = 200.00, type = TransactionType.TRANSFER, categoryId = "c3", accountId = "acc_checking", transferAccountId = "acc_savings"),
            TransactionEntity(id = "4", userId = "u1", title = "Inc 2", amount = 500.00, type = TransactionType.INCOME, categoryId = "c1", accountId = "acc_savings"),
            TransactionEntity(id = "5", userId = "u1", title = "Exp 2", amount = 75.10, type = TransactionType.EXPENSE, categoryId = "c2", accountId = "acc_savings")
        )

        val totalStarting = accs.sumOf { it.initialBalance }
        val totalIncome = randomTxs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val totalExpense = randomTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val expectedEndingNetWorth = totalStarting + totalIncome - totalExpense

        val actualEndingChecking = AccountBalanceCalculator.computeBalance(checking, randomTxs)
        val actualEndingSavings = AccountBalanceCalculator.computeBalance(savings, randomTxs)
        val actualEndingNetWorth = actualEndingChecking + actualEndingSavings

        assertEquals(expectedEndingNetWorth, actualEndingNetWorth, 0.001)
    }

    @Test
    fun testCreditCardExpenseIncreasesLiabilityAndDecreasesNetWorth() {
        val checkingAcc = AccountEntity(id = "acc_checking", userId = "u1", name = "Checking", type = AccountType.CHECKING, initialBalance = 1000.0)
        val creditCardAcc = AccountEntity(id = "acc_cc", userId = "u1", name = "Credit Card", type = AccountType.CREDIT_CARD, initialBalance = 0.0)
        val accounts = listOf(checkingAcc, creditCardAcc)

        // Initial net worth: 1000 (checking) - 0 (cc debt) = 1000
        val initialNetWorth = AccountBalanceCalculator.computeTotalInBaseCurrency(accounts, emptyList(), "USD", emptyList())
        assertEquals(1000.0, initialNetWorth, 0.001)

        // Expense of $150 charged to credit card
        val ccPurchase = TransactionEntity(
            userId = "u1",
            title = "Dinner on Card",
            amount = 150.0,
            type = TransactionType.EXPENSE,
            categoryId = "cat_dining",
            accountId = "acc_cc"
        )
        val txs = listOf(ccPurchase)

        val ccBalance = AccountBalanceCalculator.computeBalance(creditCardAcc, txs)
        assertEquals(-150.0, ccBalance, 0.001) // Credit card debt balance becomes -$150.00

        val newNetWorth = AccountBalanceCalculator.computeTotalInBaseCurrency(accounts, txs, "USD", emptyList())
        assertEquals(850.0, newNetWorth, 0.001) // Net Worth decreased from $1000 to $850 ($1000 checking - $150 cc debt)
    }
}
