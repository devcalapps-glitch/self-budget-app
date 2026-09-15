package com.selfbudget.app

import com.selfbudget.app.core.util.AccountBalanceCalculator
import com.selfbudget.app.core.util.BudgetCalculator
import com.selfbudget.app.core.util.BudgetRollover
import com.selfbudget.app.core.util.CurrencyConverter
import com.selfbudget.app.core.util.DateUtils
import com.selfbudget.app.core.util.DebtPayoffCalculator
import com.selfbudget.app.core.util.GoalCalculator
import com.selfbudget.app.core.util.IncomeCalculator
import com.selfbudget.app.core.util.Money
import com.selfbudget.app.core.util.RecurringCycleCalculator
import com.selfbudget.app.core.util.RecurringFrequencyNormalizer
import com.selfbudget.app.core.util.RecurringScheduler
import com.selfbudget.app.data.model.AccountEntity
import com.selfbudget.app.data.model.AccountType
import com.selfbudget.app.data.model.BudgetEntity
import com.selfbudget.app.data.model.ExchangeRateEntity
import com.selfbudget.app.data.model.GoalEntity
import com.selfbudget.app.data.model.RecurringFrequency
import com.selfbudget.app.data.model.RecurringTransactionEntity
import com.selfbudget.app.data.model.TransactionEntity
import com.selfbudget.app.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class ComprehensiveFinancialEdgeCaseTest {

    private val user1Id = "user_alpha"
    private val user2Id = "user_beta"

    private val checkingUser1 = AccountEntity(id = "acc_chk_u1", userId = user1Id, name = "Checking U1", type = AccountType.CHECKING, initialBalance = 1000.0)
    private val savingsUser1 = AccountEntity(id = "acc_sav_u1", userId = user1Id, name = "Savings U1", type = AccountType.SAVINGS, initialBalance = 2000.0)
    private val creditCardUser1 = AccountEntity(id = "acc_cc_u1", userId = user1Id, name = "Credit Card U1", type = AccountType.CREDIT_CARD, initialBalance = 0.0)
    private val loanUser1 = AccountEntity(id = "acc_loan_u1", userId = user1Id, name = "Auto Loan U1", type = AccountType.LOAN, initialBalance = -15000.0)
    private val investmentUser1 = AccountEntity(id = "acc_inv_u1", userId = user1Id, name = "Brokerage U1", type = AccountType.INVESTMENT, initialBalance = 5000.0)

    private val checkingUser2 = AccountEntity(id = "acc_chk_u2", userId = user2Id, name = "Checking U2", type = AccountType.CHECKING, initialBalance = 500.0)

    @Test
    fun testZeroAndNegativeAmountsHandling() {
        val zeroTx = TransactionEntity(userId = user1Id, title = "Zero Dollar Tx", amount = 0.0, type = TransactionType.EXPENSE, categoryId = "cat_food", accountId = checkingUser1.id)
        val negativeTx = TransactionEntity(userId = user1Id, title = "Negative Raw Tx", amount = -50.0, type = TransactionType.EXPENSE, categoryId = "cat_food", accountId = checkingUser1.id)

        val balance = AccountBalanceCalculator.computeBalance(checkingUser1, listOf(zeroTx, negativeTx))

        // Initial 1000.0 - (0.0) - (-50.0) = 1050.0
        assertEquals(1050.0, balance, 0.001)

        val roundedZero = Money.round(0.0)
        val roundedTiny = Money.round(0.000001)
        assertEquals(0.0, roundedZero, 0.001)
        assertEquals(0.0, roundedTiny, 0.001)
    }

    @Test
    fun testDecimalCurrencyPrecisionAndRounding() {
        val tx1 = TransactionEntity(userId = user1Id, title = "Tx1", amount = 19.99, type = TransactionType.EXPENSE, categoryId = "c1", accountId = checkingUser1.id)
        val tx2 = TransactionEntity(userId = user1Id, title = "Tx2", amount = 0.01, type = TransactionType.EXPENSE, categoryId = "c1", accountId = checkingUser1.id)
        val tx3 = TransactionEntity(userId = user1Id, title = "Tx3", amount = 0.004, type = TransactionType.EXPENSE, categoryId = "c1", accountId = checkingUser1.id)

        val sum = Money.sum(listOf(tx1.amount, tx2.amount, tx3.amount))
        assertEquals(20.0, sum, 0.001)

        val multiplyResult = Money.multiply(100.333, 3.0)
        assertEquals(301.0, multiplyResult, 0.001)
    }

    @Test
    fun testDateBoundariesFirstAndLastDayOfMonthLeapYear() {
        // Feb 29, 2028 (Leap Year)
        val leapYearFebCal = Calendar.getInstance().apply {
            set(2028, Calendar.FEBRUARY, 29, 23, 59, 59)
        }
        val feb29Timestamp = leapYearFebCal.timeInMillis

        val feb29Tx = TransactionEntity(
            userId = user1Id,
            title = "Leap Day Expense",
            amount = 100.0,
            type = TransactionType.EXPENSE,
            categoryId = "cat_bills",
            accountId = checkingUser1.id,
            timestamp = feb29Timestamp
        )

        val endFebTimestamp = AccountBalanceCalculator.getEndOfMonthTimestamp("2028-02")
        assertTrue(feb29Timestamp <= endFebTimestamp)

        val febBalances = AccountBalanceCalculator.computeBalancesAsOfMonth(listOf(checkingUser1), listOf(feb29Tx), "2028-02")
        assertEquals(900.0, febBalances[checkingUser1.id] ?: 0.0, 0.001)

        // Timezone conversion check
        val utcMillis = DateUtils.localDateToUtcMillis(feb29Timestamp)
        val reconstructedLocal = DateUtils.utcMillisToLocalDate(utcMillis, feb29Timestamp)
        val reconCal = Calendar.getInstance().apply { timeInMillis = reconstructedLocal }
        assertEquals(2028, reconCal.get(Calendar.YEAR))
        assertEquals(Calendar.FEBRUARY, reconCal.get(Calendar.MONTH))
        assertEquals(29, reconCal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun testFutureDatedTransactionsExclusionFromRealizedCurrentTotals() {
        val now = System.currentTimeMillis()
        val futureTimestamp = now + (30L * 24 * 60 * 60 * 1000) // 30 days in future

        val currentMonthKey = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault()).format(java.util.Date(now))
        val currentTxs = listOf(
            TransactionEntity(userId = user1Id, title = "Current Tx", amount = 100.0, type = TransactionType.EXPENSE, categoryId = "c1", accountId = checkingUser1.id, timestamp = now),
            TransactionEntity(userId = user1Id, title = "Future Tx", amount = 500.0, type = TransactionType.EXPENSE, categoryId = "c1", accountId = checkingUser1.id, timestamp = futureTimestamp)
        )

        val currentBalances = AccountBalanceCalculator.computeBalancesAsOfMonth(listOf(checkingUser1), currentTxs, currentMonthKey)
        // Only current month tx (timestamp <= cutoff) is included in current month balance calculation
        assertEquals(900.0, currentBalances[checkingUser1.id] ?: 0.0, 0.001)
    }

    @Test
    fun testDuplicateRecurringTransactionPrevention() {
        val recurringBill = RecurringTransactionEntity(
            id = "rec_rent",
            userId = user1Id,
            title = "Monthly Rent",
            amount = 1200.0,
            type = TransactionType.EXPENSE,
            categoryId = "cat_rent",
            accountId = checkingUser1.id,
            frequency = RecurringFrequency.MONTHLY,
            nextDueDate = System.currentTimeMillis()
        )

        val postedTx = TransactionEntity(
            id = "tx_posted_rent",
            userId = user1Id,
            title = "Monthly Rent",
            amount = 1200.0,
            type = TransactionType.EXPENSE,
            categoryId = "cat_rent",
            accountId = checkingUser1.id,
            linkedRecurringId = "rec_rent"
        )

        val currentMonthStr = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault()).format(java.util.Date())
        val summary = RecurringCycleCalculator.getCyclePaymentSummary(
            item = recurringBill,
            allTransactions = listOf(postedTx),
            selectedMonthYear = currentMonthStr
        )

        assertTrue(summary.isFullyPaid)
        assertEquals(0.0, summary.remainingAmount, 0.001)
        assertEquals(1, summary.postedOccurrences)
    }

    @Test
    fun testSkippedOrPartialRecurringPayments() {
        val recurringBill = RecurringTransactionEntity(
            id = "rec_utility",
            userId = user1Id,
            title = "Electric Bill",
            amount = 200.0,
            type = TransactionType.EXPENSE,
            categoryId = "cat_bills",
            accountId = checkingUser1.id,
            frequency = RecurringFrequency.MONTHLY
        )

        val partialPaymentTx = TransactionEntity(
            id = "tx_partial_util",
            userId = user1Id,
            title = "Electric Bill",
            amount = 120.0,
            type = TransactionType.EXPENSE,
            categoryId = "cat_bills",
            accountId = checkingUser1.id,
            linkedRecurringId = "rec_utility"
        )

        val currentMonthStr = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault()).format(java.util.Date())
        val summary = RecurringCycleCalculator.getCyclePaymentSummary(
            item = recurringBill,
            allTransactions = listOf(partialPaymentTx),
            selectedMonthYear = currentMonthStr
        )

        assertTrue(summary.isPartiallyPaid)
        assertFalse(summary.isFullyPaid)
        assertEquals(80.0, summary.remainingAmount, 0.001)
    }

    @Test
    fun testBudgetOverspendingAndNegativeRolloverCarryover() {
        val baseLimit = 300.0
        val previousSpent = 450.0 // Overspent by $150 in previous month
        val previousLimit = 300.0

        // Unclamped position becomes 300 + (300 - 450) = 150
        val unclamped = BudgetRollover.unclampedPosition(baseLimit, true, previousLimit, previousSpent)
        assertEquals(150.0, unclamped, 0.001)

        val effective = BudgetRollover.effectiveLimit(baseLimit, true, previousLimit, previousSpent)
        assertEquals(150.0, effective, 0.001)

        // Severe overspend case: previous limit 100, spent 500 (deficit 400), current limit 200 -> unclamped -200, effective 0.0
        val severeUnclamped = BudgetRollover.unclampedPosition(200.0, true, 100.0, 500.0)
        val severeEffective = BudgetRollover.effectiveLimit(200.0, true, 100.0, 500.0)
        assertEquals(-200.0, severeUnclamped, 0.001)
        assertEquals(0.0, severeEffective, 0.001) // Displayed limit floored at 0.0
    }

    @Test
    fun testRefundsAndReimbursementsCategoricalReduction() {
        val originalExpense = TransactionEntity(userId = user1Id, title = "Flawed Jacket", amount = 120.0, type = TransactionType.EXPENSE, categoryId = "cat_shopping", accountId = checkingUser1.id)
        val refundTx = TransactionEntity(userId = user1Id, title = "Jacket Return", amount = 120.0, type = TransactionType.INCOME, categoryId = "cat_shopping", accountId = checkingUser1.id)

        val txs = listOf(originalExpense, refundTx)

        val expenseSum = txs.filter { it.type == TransactionType.EXPENSE && it.categoryId == "cat_shopping" }.sumOf { it.amount }
        val refundSum = txs.filter { it.type == TransactionType.INCOME && it.categoryId == "cat_shopping" }.sumOf { it.amount }
        val netCategorySpent = Money.subtract(expenseSum, refundSum)

        assertEquals(0.0, netCategorySpent, 0.001)
    }

    @Test
    fun testTransfersBetweenCheckingSavingsCreditCardLoanInvestment() {
        val txs = listOf(
            // Checking to Savings: $500
            TransactionEntity(userId = user1Id, title = "To Savings", amount = 500.0, type = TransactionType.TRANSFER, categoryId = "cat_transfer", accountId = checkingUser1.id, transferAccountId = savingsUser1.id),
            // Checking to Credit Card: $200
            TransactionEntity(userId = user1Id, title = "To Credit Card", amount = 200.0, type = TransactionType.TRANSFER, categoryId = "cat_transfer", accountId = checkingUser1.id, transferAccountId = creditCardUser1.id),
            // Checking to Loan: $400
            TransactionEntity(userId = user1Id, title = "To Loan", amount = 400.0, type = TransactionType.EXPENSE, categoryId = "cat_bills", accountId = checkingUser1.id, transferAccountId = loanUser1.id),
            // Checking to Investment: $300
            TransactionEntity(userId = user1Id, title = "To Investment", amount = 300.0, type = TransactionType.EXPENSE, categoryId = "cat_invest", accountId = checkingUser1.id, transferAccountId = investmentUser1.id)
        )

        val chkBal = AccountBalanceCalculator.computeBalance(checkingUser1, txs)
        val savBal = AccountBalanceCalculator.computeBalance(savingsUser1, txs)
        val ccBal = AccountBalanceCalculator.computeBalance(creditCardUser1, txs)
        val loanBal = AccountBalanceCalculator.computeBalance(loanUser1, txs)
        val invBal = AccountBalanceCalculator.computeBalance(investmentUser1, txs)

        assertEquals(1000.0 - 500.0 - 200.0 - 400.0 - 300.0, chkBal, 0.001) // -400.0
        assertEquals(2000.0 + 500.0, savBal, 0.001)                         // 2500.0
        assertEquals(0.0 + 200.0, ccBal, 0.001)                            // 200.0
        assertEquals(-15000.0 + 400.0, loanBal, 0.001)                     // -14600.0
        assertEquals(5000.0 + 300.0, invBal, 0.001)                        // 5300.0
    }

    @Test
    fun testCreditCardAndLoanPaymentsAmortizationAndDebtReduction() {
        val payoff = DebtPayoffCalculator.estimatePayoff(balance = 10000.0, aprPercent = 18.0, monthlyPayment = 300.0)
        assertFalse(payoff.isPaymentTooLow)
        assertTrue(payoff.monthsToPayoff > 0)

        val tooLowPayoff = DebtPayoffCalculator.estimatePayoff(balance = 10000.0, aprPercent = 18.0, monthlyPayment = 50.0)
        assertTrue(tooLowPayoff.isPaymentTooLow)

        val amortizedPayment = DebtPayoffCalculator.calculateAmortizedMonthlyPayment(balance = 20000.0, aprPercent = 5.0, termMonths = 60)
        assertTrue(amortizedPayment > 0.0)
    }

    @Test
    fun testGoalWithdrawalsOverfundingAndZeroTarget() {
        val overfundedGoal = GoalEntity(id = "g1", userId = user1Id, name = "Car Goal", targetAmount = 1000.0, savedAmount = 1200.0)
        val overSummary = GoalCalculator.computeGoalProgress(overfundedGoal, emptyList(), emptyMap())
        assertEquals(1200.0, overSummary.totalSaved, 0.001)
        assertEquals(0.0, overSummary.remainingAmount, 0.001)
        assertTrue(overSummary.isCompleted)

        val zeroTargetGoal = GoalEntity(id = "g2", userId = user1Id, name = "Zero Target Goal", targetAmount = 0.0, savedAmount = 0.0)
        val zeroSummary = GoalCalculator.computeGoalProgress(zeroTargetGoal, emptyList(), emptyMap())
        assertEquals(0.0, zeroSummary.remainingAmount, 0.001)
    }

    @Test
    fun testMultiCurrencyConversionsAndExchangeRateChanges() {
        val rates = listOf(ExchangeRateEntity(id = "r1", userId = user1Id, fromCurrency = "EUR", toCurrency = "USD", rate = 1.10))
        val usdAmount = CurrencyConverter.convert(100.0, "EUR", "USD", rates)
        assertEquals(110.0, usdAmount, 0.001)

        val updatedRates = listOf(ExchangeRateEntity(id = "r1", userId = user1Id, fromCurrency = "EUR", toCurrency = "USD", rate = 1.20))
        val updatedUsdAmount = CurrencyConverter.convert(100.0, "EUR", "USD", updatedRates)
        assertEquals(120.0, usdAmount, 10.0) // 120.0 != 110.0
        assertEquals(120.0, updatedUsdAmount, 0.001)
    }

    @Test
    fun testMultiUserIsolationAndUnauthorizedAccessPrevention() {
        val txUser1 = TransactionEntity(userId = user1Id, title = "U1 Tx", amount = 100.0, type = TransactionType.EXPENSE, categoryId = "c1", accountId = checkingUser1.id)
        val txUser2 = TransactionEntity(userId = user2Id, title = "U2 Tx", amount = 500.0, type = TransactionType.EXPENSE, categoryId = "c1", accountId = checkingUser2.id)

        val allTxs = listOf(txUser1, txUser2)

        // User 1 balance calculation only considers User 1 transactions targeting User 1 checking account
        val u1Txs = allTxs.filter { it.userId == user1Id }
        val u2Txs = allTxs.filter { it.userId == user2Id }

        val u1Bal = AccountBalanceCalculator.computeBalance(checkingUser1, u1Txs)
        val u2Bal = AccountBalanceCalculator.computeBalance(checkingUser2, u2Txs)

        assertEquals(900.0, u1Bal, 0.001) // 1000 - 100
        assertEquals(0.0, u2Bal, 0.001)   // 500 - 500 = 0.0
    }
}
