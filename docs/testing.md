# Automated Testing Suite & Strategy

This document describes the automated testing suite built for **Self Budget** to ensure business logic, cash flow calculations, budget safeguards, recurring bill normalizations, debt payoff reductions, idempotency, rollover across Dec → Jan, end-to-end multi-step monthly lifecycles, zero-cost cloud sync payload serialization, and multi-account aggregations work correctly.

---

## 1. Test Architecture

The testing suite consists of unit tests written with JUnit 4 and Kotlin Coroutines Test framework (`kotlinx-coroutines-test`).

- **Location**: `app/src/test/java/com/selfbudget/app/`
- **Execution Command**: `./gradlew test`

---

## 2. Test Suites Overview

### ☁️ `CloudSyncTest.kt` (100% Zero-Cost Cloud Sync & Serialization)
- **Sync Payload Serialization & Deserialization**: Verifies that full database snapshots (`SyncDataPayload`) containing accounts, categories, transactions, budgets, goals, net worth snapshots, and exchange rates serialize to JSON and deserialize back with 100% exact cent precision.
- **Schema Version Compatibility**: Verifies that opening older cloud backups (e.g. schema v6 in app v7+) applies safe defaults automatically without crashing or losing data.

### 🚀 `FullLifecycleIntegrationTest.kt` (End-to-End Workflows)
- **Full Monthly Lifecycle**: Simulates `Paycheck → allocate budgets → recurring bill posts → grocery expense → credit-card purchase → credit-card payment → refund → month rollover → next month's budget`. Verifies that checking balances, net grocery spending, credit card balances, and January rollover limits ($600.00 effective limit) match exact cent expectations across month boundaries.
- **Debt Payoff & Multi-Account Lifecycle**: Simulates initial deposit, auto loan payment, credit card purchase, and credit card payoff, verifying loan debt balance reduction (-$9,600.00) and zero credit card balance ($0.00).
- **Multi-Currency & Net Worth Snapshot Lifecycle**: Simulates multi-currency account balances (USD Checking + EUR Savings Vault), verifies initial net worth conversion ($2,100.00 USD), updates historical exchange rates (1.10 $\rightarrow$ 1.15), and verifies dynamic net worth recalculation ($2,150.00 USD).

### 🧪 `BudgetCalculationTest.kt`
- **Zero-State Settlement**: Verifies that when no explicit budgets are defined by the user (or when all budgets/transactions are deleted), `totalBudget`, `remainingBudget`, and `safeToSpend` cleanly settle to `$0.00`.
- **Safe-to-Spend Formula**: Validates `Safe-to-Spend = Budget Limit - Posted Spent - Pending Upcoming Bills`.
- **Over-Budget & Warning Triggers**: Ensures that spending over the budget limit sets `isOverBudget = true`, and spending at $\ge 75\%$ triggers `isWarning = true`.
- **Recurring Bill Floor Calculation**: Validates conversion of weekly recurring bills ($50/wk $\rightarrow$ $216.65/mo) into monthly equivalent floors.
- **Transaction Edits & Deletes Handling**: Verifies budget spent and remaining amounts recalculate cleanly when transactions are edited or deleted.

### 💳 `AccountBalanceTest.kt`
- **Multi-Account Balance Aggregation**: Tests net worth summation across Checking, High-Yield Savings, Cash Wallets, and Credit Cards (negative liabilities).
- **Zero Account Balance**: Verifies zero balance handling when no accounts are present.
- **Account Deletion**: Verifies balance removal upon account deletion.

### 📊 `CashFlowCalculationTest.kt`
- **Master Cash Flow Equation**: Validates `Income - Category Budgets - Unbudgeted Bills = Unassigned Free Cash`.
- **4-Column Breakdown Validation**: Verifies that `Income` (🟢), `Budgets` (🔵), `Unbudgeted` (Neutral Slate/Dark), and `Paid` (Conditional Red) compute 4 distinct metrics accurately without double counting.
- **Income Exclusion Safeguard**: Verifies that Income transactions & recurring paychecks are 100% excluded from `Unbudgeted` and `Paid` calculations.
- **Million-Dollar Large Amount Scaling**: Verifies numbers up to millions ($1,250,000.00) compute and format safely on a single line.
- **Surplus vs. Deficit**: Tests positive unallocated free cash (surplus) and negative free cash (deficit) states.
- **Empty State**: Verifies `$0.00` cash flow when all values are zero.

### 🔁 `RecurringTransactionTest.kt`
- **Frequency Normalization**: Tests weekly ($\times 4.333$), bi-weekly ($\times 2.166$), monthly ($\times 1.0$), and yearly ($\div 12.0$) conversion ratios.
- **Duplicate Posting Safeguard**: Validates 24-hour duplicate transaction detection algorithm preventing accidental double postings.

### 💰 `MoneyTest.kt`
- **Floating-Point Drift Elimination**: Verifies the cent-safe `Money` utility eliminates binary floating-point drift (e.g. summing `0.1` ten times equals exactly `1.0`).
- **Negative & Zero Money Behavior**: Verifies cent precision for negative debt balances (-$1,500.25) and $0.00 amounts without producing `-0.0` or precision errors.
- **Persistence Round-Trip Exact Cents**: Verifies string serialization and deserialization (`Double` $\rightarrow$ `"1234.56"` $\rightarrow$ `Double`) preserves exact cents.

### 🏦 `AccountBalanceCalculatorTest.kt`
- **Live Transaction Balance Reflection**: Verifies account balances reflect live income, expense, and transfer activity rather than the static starting balance.
- **Transaction Edit Recalculation**: Verifies balance recalculates correctly when transaction amounts are edited.
- **Transaction Deletion Reversal**: Verifies deleting a transaction reverses its financial effect and restores original balance.
- **Transfer Net Worth Invariance**: Verifies transferring funds between accounts leaves total net worth unchanged.
- **Credit Card Payment Excluded from Spending**: Verifies credit card transfers/payoffs are not counted as general spending.
- **Refund Offsetting**: Verifies refunds logged as Income correctly offset original expenses.
- **Property Test (Financial Movement Invariant)**: Property test asserting that `Starting Balances + Total Income - Total Expense == Ending Balances` holds invariant across random financial movements.
- **Debt Payoff Balance Reduction**: Verifies that logging an expense payment toward a Credit Card, Loan, or Mortgage account simultaneously debits the funding checking account and reduces the debt balance on the target liability account.

### 🌍 `CurrencyConverterTest.kt`
- **Currency Conversion Rounding & Historical Rate Behavior**: Verifies direct/inverse rate conversion with cent rounding, and dynamic recalculation when historical exchange rates are updated.
- **Same Currency & Missing Rate Fallbacks**: Verifies pass-through for matching currencies and safe face-value fallbacks.

### 💳 `DebtPayoffCalculatorTest.kt`
- **Amortization & Interest Estimates**: Verifies credit-card/loan payoff estimates, 0% APR simple division, and flags monthly payments too low to cover interest.
- **Final Debt Payment Rounding**: Verifies final debt payoff payments settle remaining balance to $0.00 with cent-safe interest rounding.

### 🔄 `BudgetRolloverTest.kt`
- **Underspend Bonus & Overspend Reduction**: Verifies underspending carries forward as a bonus into next month, while overspending reduces next month's limit.
- **Dec → Jan Rollover Continuity**: Verifies rollover works continuously across year boundaries from December into January.

### 📅 `RecurringSchedulerTest.kt`
- **Feb 28/29/30/31 Clamping**: Verifies monthly recurrence handling for February leap years (Feb 29) and non-leap years (Feb 28).
- **Crash & Restart Idempotency**: Verifies recurring scheduler checks `lastPostedDate` to remain 100% idempotent after background worker or app crashes.

---

## 3. Running the Tests

To run the full unit test suite:

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ANDROID_HOME="/Users/bbhanda1/Library/Android/sdk" ./gradlew test
```
