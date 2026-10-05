package com.selfbudget.app.core.util

import com.selfbudget.app.data.model.AccountEntity
import com.selfbudget.app.data.model.ActivityAction
import com.selfbudget.app.data.model.ActivityEntityType
import com.selfbudget.app.data.model.ActivityLogEntity
import com.selfbudget.app.data.model.BudgetEntity
import com.selfbudget.app.data.model.CategoryEntity
import com.selfbudget.app.data.model.GoalEntity
import com.selfbudget.app.data.model.TransactionEntity
import com.selfbudget.app.data.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

enum class AnalyticsSummaryTimeframe {
    MONTHLY, ANNUAL
}

data class CategorySummaryItem(
    val category: CategoryEntity,
    val spent: Double,
    val budgeted: Double, // 0.0 if not budgeted
    val diff: Double, // spent - budgeted
    val isOverBudget: Boolean,
    val sharePercent: Float // spent / totalExpense
)

data class GoalContributionItem(
    val goalId: String,
    val goalName: String,
    val amountAdded: Double
)

data class PeriodFinancialSummary(
    val periodLabel: String,
    val isAnnual: Boolean,
    val totalExpense: Double,
    val totalBudget: Double,
    val totalIncome: Double,
    val netSavings: Double,
    val savingsRatePercent: Double,
    val prevExpense: Double,
    val diffExpense: Double,
    val diffExpensePercent: Double,
    val monthsLoggedCount: Int,
    val monthlyAverageExpense: Double,
    val monthlyAverageIncome: Double,
    val categorySummaries: List<CategorySummaryItem>,
    val topSpendingCategory: CategorySummaryItem?,
    val overBudgetCategories: List<CategorySummaryItem>,
    val underBudgetCategories: List<CategorySummaryItem>,
    val unbudgetedCategories: List<CategorySummaryItem>,
    val totalGoalsCount: Int,
    val goalsMetCount: Int,
    val totalGoalsTarget: Double,
    val totalGoalsSaved: Double,
    val goalsProgressFraction: Float,
    val totalAmountAddedToGoals: Double,
    val goalContributions: List<GoalContributionItem>,
    val headlineText: String,
    val highLevelNarrative: String,
    val spendingNarrative: String,
    val trendNarrative: String,
    val savingsNarrative: String,
    val goalsNarrative: String
)

object AnalyticsSummaryCalculator {

    fun computeSummary(
        isAnnual: Boolean,
        selectedMonthYear: String, // e.g. "2026-09"
        currencySymbol: String = "$",
        allTransactions: List<TransactionEntity>,
        categories: List<CategoryEntity>,
        budgets: List<BudgetEntity> = emptyList(),
        allBudgets: List<BudgetEntity> = emptyList(),
        accounts: List<AccountEntity> = emptyList(),
        accountBalances: Map<String, Double> = emptyMap(),
        goals: List<GoalEntity> = emptyList(),
        activityLog: List<ActivityLogEntity> = emptyList(),
        previousMonthBudgets: List<BudgetEntity> = emptyList(),
        previousMonthSpentByCategory: Map<String, Double> = emptyMap()
    ): PeriodFinancialSummary {
        val sdfMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val sdfMonthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val sdfYear = SimpleDateFormat("yyyy", Locale.getDefault())

        val catMap = categories.associateBy { it.id }

        val selectedYearStr = try {
            val date = sdfMonth.parse(selectedMonthYear)
            if (date != null) sdfYear.format(date) else sdfYear.format(Date())
        } catch (e: Exception) {
            sdfYear.format(Date())
        }

        return if (!isAnnual) {
            computeMonthlySummary(
                selectedMonthYear = selectedMonthYear,
                currencySymbol = currencySymbol,
                allTransactions = allTransactions,
                categories = categories,
                catMap = catMap,
                budgets = budgets,
                allBudgets = allBudgets,
                accounts = accounts,
                accountBalances = accountBalances,
                goals = goals,
                activityLog = activityLog,
                previousMonthBudgets = previousMonthBudgets,
                previousMonthSpentByCategory = previousMonthSpentByCategory,
                sdfMonth = sdfMonth,
                sdfMonthName = sdfMonthName
            )
        } else {
            computeAnnualSummary(
                selectedYearStr = selectedYearStr,
                currencySymbol = currencySymbol,
                allTransactions = allTransactions,
                categories = categories,
                catMap = catMap,
                budgets = budgets,
                allBudgets = allBudgets,
                accounts = accounts,
                accountBalances = accountBalances,
                goals = goals,
                activityLog = activityLog,
                sdfMonth = sdfMonth,
                sdfYear = sdfYear
            )
        }
    }

    /**
     * Resolves monthly category budgets for a target month.
     * Uses [allBudgets] with historical lookup; falls back to [budgets] baseline,
     * or the earliest/closest available configured budget per category in [allBudgets].
     */
    fun resolveMonthlyBudgets(
        targetMonthYear: String,
        budgets: List<BudgetEntity>,
        allBudgets: List<BudgetEntity>
    ): List<BudgetEntity> {
        val computed = if (allBudgets.isNotEmpty()) {
            BudgetCalculator.computeBudgetsForMonth(allBudgets, targetMonthYear)
        } else {
            emptyList()
        }
        if (computed.isNotEmpty()) {
            return computed
        }
        if (budgets.isNotEmpty()) {
            return budgets
        }
        if (allBudgets.isNotEmpty()) {
            return allBudgets
                .groupBy { it.categoryId }
                .mapNotNull { (_, list) ->
                    val closest = list.minByOrNull { it.monthYear } ?: list.firstOrNull()
                    if (closest != null && closest.amountLimit > 0.0) {
                        closest.copy(monthYear = targetMonthYear)
                    } else {
                        null
                    }
                }
        }
        return emptyList()
    }

    private fun computeMonthlySummary(
        selectedMonthYear: String,
        currencySymbol: String,
        allTransactions: List<TransactionEntity>,
        categories: List<CategoryEntity>,
        catMap: Map<String, CategoryEntity>,
        budgets: List<BudgetEntity>,
        allBudgets: List<BudgetEntity>,
        accounts: List<AccountEntity>,
        accountBalances: Map<String, Double>,
        goals: List<GoalEntity>,
        activityLog: List<ActivityLogEntity>,
        previousMonthBudgets: List<BudgetEntity>,
        previousMonthSpentByCategory: Map<String, Double>,
        sdfMonth: SimpleDateFormat,
        sdfMonthName: SimpleDateFormat
    ): PeriodFinancialSummary {
        val (startMs, endMs) = AccountBalanceCalculator.getMonthTimestampRange(selectedMonthYear)

        val cal = Calendar.getInstance()
        var currentMonthName = selectedMonthYear
        var prevMonthName = "Previous Month"
        var prevMonthYearStr = ""
        try {
            val date = sdfMonth.parse(selectedMonthYear)
            if (date != null) {
                cal.time = date
                currentMonthName = sdfMonthName.format(cal.time)
                cal.add(Calendar.MONTH, -1)
                prevMonthYearStr = sdfMonth.format(cal.time)
                prevMonthName = sdfMonthName.format(cal.time)
            }
        } catch (_: Exception) {}

        val (prevStartMs, prevEndMs) = AccountBalanceCalculator.getMonthTimestampRange(prevMonthYearStr)

        // 1. Transactions
        val currentMonthExpenses = allTransactions.filter { tx ->
            tx.timestamp in startMs..endMs &&
            tx.type == TransactionType.EXPENSE &&
            (catMap[tx.categoryId]?.type == TransactionType.EXPENSE || catMap[tx.categoryId] == null)
        }
        val currentMonthIncomes = allTransactions.filter { tx ->
            tx.timestamp in startMs..endMs &&
            tx.type == TransactionType.INCOME &&
            (catMap[tx.categoryId]?.type == TransactionType.INCOME || catMap[tx.categoryId] == null)
        }
        val prevMonthExpenses = allTransactions.filter { tx ->
            tx.timestamp in prevStartMs..prevEndMs &&
            tx.type == TransactionType.EXPENSE &&
            (catMap[tx.categoryId]?.type == TransactionType.EXPENSE || catMap[tx.categoryId] == null)
        }

        val totalExpense = Money.sum(currentMonthExpenses.map { it.amount })
        val totalIncome = Money.sum(currentMonthIncomes.map { it.amount })
        val prevExpense = Money.sum(prevMonthExpenses.map { it.amount })
        val diffExpense = Money.subtract(totalExpense, prevExpense)
        val diffExpensePercent = if (prevExpense > 0.0) {
            (diffExpense / prevExpense) * 100.0
        } else if (totalExpense > 0.0) {
            100.0
        } else {
            0.0
        }

        val netSavings = Money.subtract(totalIncome, totalExpense)
        val savingsRatePercent = if (totalIncome > 0.0) {
            ((netSavings / totalIncome) * 100.0).coerceAtLeast(0.0)
        } else {
            0.0
        }

        // 2. Category Budgets & Rollover Resolution
        val effectiveBudgets = resolveMonthlyBudgets(
            targetMonthYear = selectedMonthYear,
            budgets = budgets,
            allBudgets = allBudgets
        )
        val resolvedPrevBudgets = if (previousMonthBudgets.isNotEmpty()) {
            previousMonthBudgets
        } else if (allBudgets.isNotEmpty() && prevMonthYearStr.isNotEmpty()) {
            resolveMonthlyBudgets(prevMonthYearStr, emptyList(), allBudgets)
        } else {
            emptyList()
        }
        val prevBudgetMap = resolvedPrevBudgets.associateBy { it.categoryId }

        val categoryEffectiveBudgetMap = mutableMapOf<String, Double>()
        for (b in effectiveBudgets) {
            val prevLimit = prevBudgetMap[b.categoryId]?.amountLimit ?: 0.0
            val prevSpent = previousMonthSpentByCategory[b.categoryId] ?: 0.0
            val effectiveLimit = BudgetRollover.effectiveLimit(
                currentLimit = b.amountLimit,
                rolloverEnabled = b.rolloverEnabled,
                previousLimit = prevLimit,
                previousSpent = prevSpent
            )
            categoryEffectiveBudgetMap[b.categoryId] = effectiveLimit
        }

        val spentByCat = currentMonthExpenses.groupBy { it.categoryId }
            .mapValues { Money.sum(it.value.map { tx -> tx.amount }) }

        val allCategoryIds = (spentByCat.keys + categoryEffectiveBudgetMap.keys).distinct()
        val categorySummaries = allCategoryIds.mapNotNull { catId ->
            val spent = spentByCat[catId] ?: 0.0
            val budgeted = categoryEffectiveBudgetMap[catId] ?: 0.0
            if (spent == 0.0 && budgeted == 0.0) return@mapNotNull null

            val category = catMap[catId] ?: CategoryEntity(
                id = catId,
                name = "General / Other",
                iconName = "MoreHoriz",
                colorHex = "#64748B",
                type = TransactionType.EXPENSE
            )
            val diff = Money.subtract(spent, budgeted)
            val isOver = budgeted > 0.0 && spent > (budgeted + 0.005)
            val share = if (totalExpense > 0.0) (spent / totalExpense).toFloat() else 0f
            CategorySummaryItem(
                category = category,
                spent = spent,
                budgeted = budgeted,
                diff = diff,
                isOverBudget = isOver,
                sharePercent = share
            )
        }.sortedByDescending { it.spent }

        val totalBudget = Money.sum(categoryEffectiveBudgetMap.values.toList())
        val topSpendingCategory = categorySummaries.firstOrNull { it.spent > 0.0 }
        val overBudgetCategories = categorySummaries.filter { it.isOverBudget }
        val underBudgetCategories = categorySummaries.filter { it.budgeted > 0.0 && !it.isOverBudget }
        val unbudgetedCategories = categorySummaries.filter { it.budgeted == 0.0 && it.spent > 0.0 }

        // 3. Goals Progress & Additions
        val goalSummaries = goals.map { GoalCalculator.computeGoalProgress(it, accounts, accountBalances) }
        val totalGoalsCount = goals.size
        val goalsMetCount = goalSummaries.count { it.isCompleted }
        val totalGoalsTarget = Money.sum(goals.map { it.targetAmount })
        val totalGoalsSaved = Money.sum(goalSummaries.map { it.totalSaved })
        val goalsProgressFraction = if (totalGoalsTarget > 0.0) {
            (totalGoalsSaved / totalGoalsTarget).toFloat().coerceIn(0f, 1f)
        } else {
            0f
        }

        val (totalAddedToGoals, goalContributionItems) = computeGoalContributions(
            goals = goals,
            allTransactions = allTransactions,
            activityLog = activityLog,
            startTimestamp = startMs,
            endTimestamp = endMs
        )

        // 4. Narrative Synthesis
        val headline = buildMonthlyHeadline(
            currentMonthName = currentMonthName,
            totalExpense = totalExpense,
            totalBudget = totalBudget,
            netSavings = netSavings,
            currencySymbol = currencySymbol
        )
        val spendingText = buildMonthlySpendingNarrative(
            currentMonthName = currentMonthName,
            totalExpense = totalExpense,
            totalBudget = totalBudget,
            categorySummaries = categorySummaries,
            topSpendingCategory = topSpendingCategory,
            overBudgetCategories = overBudgetCategories,
            underBudgetCategories = underBudgetCategories,
            unbudgetedCategories = unbudgetedCategories,
            currencySymbol = currencySymbol
        )
        val trendText = buildMonthlyTrendNarrative(
            totalExpense = totalExpense,
            prevExpense = prevExpense,
            diffExpense = diffExpense,
            diffExpensePercent = diffExpensePercent,
            prevMonthName = prevMonthName,
            currencySymbol = currencySymbol
        )
        val savingsText = buildSavingsNarrative(
            periodLabel = currentMonthName,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            netSavings = netSavings,
            savingsRatePercent = savingsRatePercent,
            currencySymbol = currencySymbol
        )
        val goalsText = buildGoalsNarrative(
            isAnnual = false,
            totalGoalsCount = totalGoalsCount,
            goalsMetCount = goalsMetCount,
            totalGoalsTarget = totalGoalsTarget,
            totalGoalsSaved = totalGoalsSaved,
            goalsProgressFraction = goalsProgressFraction,
            totalAmountAddedToGoals = totalAddedToGoals,
            goalContributions = goalContributionItems,
            currencySymbol = currencySymbol
        )

        val highLevel = buildHighLevelNarrative(
            isAnnual = false,
            periodLabel = currentMonthName,
            totalExpense = totalExpense,
            totalBudget = totalBudget,
            totalIncome = totalIncome,
            netSavings = netSavings,
            savingsRatePercent = savingsRatePercent,
            prevExpense = prevExpense,
            diffExpense = diffExpense,
            diffExpensePercent = diffExpensePercent,
            prevMonthName = prevMonthName,
            monthsLoggedCount = 1,
            monthlyAverageExpense = totalExpense,
            topSpendingCategory = topSpendingCategory,
            overBudgetCategories = overBudgetCategories,
            totalAmountAddedToGoals = totalAddedToGoals,
            currencySymbol = currencySymbol
        )

        return PeriodFinancialSummary(
            periodLabel = currentMonthName,
            isAnnual = false,
            totalExpense = totalExpense,
            totalBudget = totalBudget,
            totalIncome = totalIncome,
            netSavings = netSavings,
            savingsRatePercent = savingsRatePercent,
            prevExpense = prevExpense,
            diffExpense = diffExpense,
            diffExpensePercent = diffExpensePercent,
            monthsLoggedCount = 1,
            monthlyAverageExpense = totalExpense,
            monthlyAverageIncome = totalIncome,
            categorySummaries = categorySummaries,
            topSpendingCategory = topSpendingCategory,
            overBudgetCategories = overBudgetCategories,
            underBudgetCategories = underBudgetCategories,
            unbudgetedCategories = unbudgetedCategories,
            totalGoalsCount = totalGoalsCount,
            goalsMetCount = goalsMetCount,
            totalGoalsTarget = totalGoalsTarget,
            totalGoalsSaved = totalGoalsSaved,
            goalsProgressFraction = goalsProgressFraction,
            totalAmountAddedToGoals = totalAddedToGoals,
            goalContributions = goalContributionItems,
            headlineText = headline,
            highLevelNarrative = highLevel,
            spendingNarrative = spendingText,
            trendNarrative = trendText,
            savingsNarrative = savingsText,
            goalsNarrative = goalsText
        )
    }

    private fun computeAnnualSummary(
        selectedYearStr: String,
        currencySymbol: String,
        allTransactions: List<TransactionEntity>,
        categories: List<CategoryEntity>,
        catMap: Map<String, CategoryEntity>,
        budgets: List<BudgetEntity>,
        allBudgets: List<BudgetEntity>,
        accounts: List<AccountEntity>,
        accountBalances: Map<String, Double>,
        goals: List<GoalEntity>,
        activityLog: List<ActivityLogEntity>,
        sdfMonth: SimpleDateFormat,
        sdfYear: SimpleDateFormat
    ): PeriodFinancialSummary {
        val (yearStartMs, yearEndMs) = getYearTimestampRange(selectedYearStr)

        val annualExpenses = allTransactions.filter { tx ->
            sdfYear.format(Date(tx.timestamp)) == selectedYearStr &&
            tx.type == TransactionType.EXPENSE &&
            (catMap[tx.categoryId]?.type == TransactionType.EXPENSE || catMap[tx.categoryId] == null)
        }
        val annualIncomes = allTransactions.filter { tx ->
            sdfYear.format(Date(tx.timestamp)) == selectedYearStr &&
            tx.type == TransactionType.INCOME &&
            (catMap[tx.categoryId]?.type == TransactionType.INCOME || catMap[tx.categoryId] == null)
        }

        val totalExpense = Money.sum(annualExpenses.map { it.amount })
        val totalIncome = Money.sum(annualIncomes.map { it.amount })

        val loggedMonths = (annualExpenses + annualIncomes)
            .map { sdfMonth.format(Date(it.timestamp)) }
            .distinct()
            .sorted()
        val monthsLoggedCount = loggedMonths.size.coerceAtLeast(1)

        val monthlyAverageExpense = Money.round(totalExpense / monthsLoggedCount)
        val monthlyAverageIncome = Money.round(totalIncome / monthsLoggedCount)

        val netSavings = Money.subtract(totalIncome, totalExpense)
        val savingsRatePercent = if (totalIncome > 0.0) {
            ((netSavings / totalIncome) * 100.0).coerceAtLeast(0.0)
        } else {
            0.0
        }

        // Annual Category Budgets
        // Sum the effective monthly budget across all 12 months of the year for each category
        val categoryAnnualBudget = mutableMapOf<String, Double>()
        for (monthIndex in 1..12) {
            val monthStr = "%s-%02d".format(selectedYearStr, monthIndex)
            val monthBudgets = resolveMonthlyBudgets(
                targetMonthYear = monthStr,
                budgets = budgets,
                allBudgets = allBudgets
            )
            for (b in monthBudgets) {
                categoryAnnualBudget[b.categoryId] = Money.add(categoryAnnualBudget[b.categoryId] ?: 0.0, b.amountLimit)
            }
        }

        val spentByCat = annualExpenses.groupBy { it.categoryId }
            .mapValues { Money.sum(it.value.map { tx -> tx.amount }) }

        val allCategoryIds = (spentByCat.keys + categoryAnnualBudget.keys).distinct()
        val categorySummaries = allCategoryIds.mapNotNull { catId ->
            val spent = spentByCat[catId] ?: 0.0
            val budgeted = categoryAnnualBudget[catId] ?: 0.0
            if (spent == 0.0 && budgeted == 0.0) return@mapNotNull null

            val category = catMap[catId] ?: CategoryEntity(
                id = catId,
                name = "General / Other",
                iconName = "MoreHoriz",
                colorHex = "#64748B",
                type = TransactionType.EXPENSE
            )
            val diff = Money.subtract(spent, budgeted)
            val isOver = budgeted > 0.0 && spent > (budgeted + 0.005)
            val share = if (totalExpense > 0.0) (spent / totalExpense).toFloat() else 0f
            CategorySummaryItem(
                category = category,
                spent = spent,
                budgeted = budgeted,
                diff = diff,
                isOverBudget = isOver,
                sharePercent = share
            )
        }.sortedByDescending { it.spent }

        val totalBudget = Money.sum(categoryAnnualBudget.values.toList())
        val topSpendingCategory = categorySummaries.firstOrNull { it.spent > 0.0 }
        val overBudgetCategories = categorySummaries.filter { it.isOverBudget }
        val underBudgetCategories = categorySummaries.filter { it.budgeted > 0.0 && !it.isOverBudget }
        val unbudgetedCategories = categorySummaries.filter { it.budgeted == 0.0 && it.spent > 0.0 }

        // Goals
        val goalSummaries = goals.map { GoalCalculator.computeGoalProgress(it, accounts, accountBalances) }
        val totalGoalsCount = goals.size
        val goalsMetCount = goalSummaries.count { it.isCompleted }
        val totalGoalsTarget = Money.sum(goals.map { it.targetAmount })
        val totalGoalsSaved = Money.sum(goalSummaries.map { it.totalSaved })
        val goalsProgressFraction = if (totalGoalsTarget > 0.0) {
            (totalGoalsSaved / totalGoalsTarget).toFloat().coerceIn(0f, 1f)
        } else {
            0f
        }

        val (totalAddedToGoals, goalContributionItems) = computeGoalContributions(
            goals = goals,
            allTransactions = allTransactions,
            activityLog = activityLog,
            startTimestamp = yearStartMs,
            endTimestamp = yearEndMs
        )

        val headline = buildAnnualHeadline(
            selectedYearStr = selectedYearStr,
            totalExpense = totalExpense,
            totalBudget = totalBudget,
            netSavings = netSavings,
            currencySymbol = currencySymbol
        )
        val spendingText = buildAnnualSpendingNarrative(
            selectedYearStr = selectedYearStr,
            totalExpense = totalExpense,
            totalBudget = totalBudget,
            categorySummaries = categorySummaries,
            topSpendingCategory = topSpendingCategory,
            overBudgetCategories = overBudgetCategories,
            underBudgetCategories = underBudgetCategories,
            unbudgetedCategories = unbudgetedCategories,
            currencySymbol = currencySymbol
        )
        val trendText = buildAnnualTrendNarrative(
            selectedYearStr = selectedYearStr,
            monthsLoggedCount = monthsLoggedCount,
            monthlyAverageExpense = monthlyAverageExpense,
            totalBudget = totalBudget,
            currencySymbol = currencySymbol
        )
        val savingsText = buildSavingsNarrative(
            periodLabel = "$selectedYearStr YTD",
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            netSavings = netSavings,
            savingsRatePercent = savingsRatePercent,
            currencySymbol = currencySymbol
        )
        val goalsText = buildGoalsNarrative(
            isAnnual = true,
            totalGoalsCount = totalGoalsCount,
            goalsMetCount = goalsMetCount,
            totalGoalsTarget = totalGoalsTarget,
            totalGoalsSaved = totalGoalsSaved,
            goalsProgressFraction = goalsProgressFraction,
            totalAmountAddedToGoals = totalAddedToGoals,
            goalContributions = goalContributionItems,
            currencySymbol = currencySymbol
        )

        val highLevel = buildHighLevelNarrative(
            isAnnual = true,
            periodLabel = "$selectedYearStr YTD",
            totalExpense = totalExpense,
            totalBudget = totalBudget,
            totalIncome = totalIncome,
            netSavings = netSavings,
            savingsRatePercent = savingsRatePercent,
            prevExpense = 0.0,
            diffExpense = 0.0,
            diffExpensePercent = 0.0,
            prevMonthName = "",
            monthsLoggedCount = monthsLoggedCount,
            monthlyAverageExpense = monthlyAverageExpense,
            topSpendingCategory = topSpendingCategory,
            overBudgetCategories = overBudgetCategories,
            totalAmountAddedToGoals = totalAddedToGoals,
            currencySymbol = currencySymbol
        )

        return PeriodFinancialSummary(
            periodLabel = "$selectedYearStr YTD",
            isAnnual = true,
            totalExpense = totalExpense,
            totalBudget = totalBudget,
            totalIncome = totalIncome,
            netSavings = netSavings,
            savingsRatePercent = savingsRatePercent,
            prevExpense = 0.0,
            diffExpense = 0.0,
            diffExpensePercent = 0.0,
            monthsLoggedCount = monthsLoggedCount,
            monthlyAverageExpense = monthlyAverageExpense,
            monthlyAverageIncome = monthlyAverageIncome,
            categorySummaries = categorySummaries,
            topSpendingCategory = topSpendingCategory,
            overBudgetCategories = overBudgetCategories,
            underBudgetCategories = underBudgetCategories,
            unbudgetedCategories = unbudgetedCategories,
            totalGoalsCount = totalGoalsCount,
            goalsMetCount = goalsMetCount,
            totalGoalsTarget = totalGoalsTarget,
            totalGoalsSaved = totalGoalsSaved,
            goalsProgressFraction = goalsProgressFraction,
            totalAmountAddedToGoals = totalAddedToGoals,
            goalContributions = goalContributionItems,
            headlineText = headline,
            highLevelNarrative = highLevel,
            spendingNarrative = spendingText,
            trendNarrative = trendText,
            savingsNarrative = savingsText,
            goalsNarrative = goalsText
        )
    }

    private fun computeGoalContributions(
        goals: List<GoalEntity>,
        allTransactions: List<TransactionEntity>,
        activityLog: List<ActivityLogEntity>,
        startTimestamp: Long,
        endTimestamp: Long
    ): Pair<Double, List<GoalContributionItem>> {
        val contributionsByGoal = mutableMapOf<String, Double>()
        val goalNames = goals.associate { it.id to it.name }
        val linkedAccountMap = goals.filter { it.linkedAccountId != null }
            .associateBy { it.linkedAccountId!! }

        // 1. Activity Log manual contributions
        val goalActivity = activityLog.filter { entry ->
            entry.entityType == ActivityEntityType.GOAL &&
            entry.action == ActivityAction.CONTRIBUTED &&
            entry.timestamp in startTimestamp..endTimestamp &&
            (entry.amount ?: 0.0) > 0.0
        }
        for (log in goalActivity) {
            val amount = log.amount ?: 0.0
            contributionsByGoal[log.entityId] = Money.add(contributionsByGoal[log.entityId] ?: 0.0, amount)
        }

        // 2. Transfers into linked savings accounts
        val transferTxs = allTransactions.filter { tx ->
            tx.type == TransactionType.TRANSFER &&
            tx.timestamp in startTimestamp..endTimestamp &&
            tx.transferAccountId in linkedAccountMap.keys
        }
        for (tx in transferTxs) {
            val matchingGoal = linkedAccountMap[tx.transferAccountId] ?: continue
            // Avoid double counting if manual activity was already logged in exact amount & time
            val alreadyCounted = goalActivity.any {
                it.entityId == matchingGoal.id &&
                abs(it.timestamp - tx.timestamp) < 5000 &&
                abs((it.amount ?: 0.0) - tx.amount) < 0.01
            }
            if (!alreadyCounted) {
                contributionsByGoal[matchingGoal.id] = Money.add(contributionsByGoal[matchingGoal.id] ?: 0.0, tx.amount)
            }
        }

        val items = goals.mapNotNull { goal ->
            val added = contributionsByGoal[goal.id] ?: 0.0
            if (added > 0.0) {
                GoalContributionItem(
                    goalId = goal.id,
                    goalName = goal.name,
                    amountAdded = added
                )
            } else null
        }
        val totalAdded = Money.sum(items.map { it.amountAdded })
        return Pair(totalAdded, items)
    }

    private fun getYearTimestampRange(yearStr: String): Pair<Long, Long> {
        return try {
            val cal = Calendar.getInstance()
            val year = yearStr.toIntOrNull() ?: cal.get(Calendar.YEAR)
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, Calendar.JANUARY)
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val start = cal.timeInMillis

            cal.set(Calendar.MONTH, Calendar.DECEMBER)
            cal.set(Calendar.DAY_OF_MONTH, 31)
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val end = cal.timeInMillis
            Pair(start, end)
        } catch (_: Exception) {
            Pair(0L, Long.MAX_VALUE)
        }
    }

    private fun formatMoney(amount: Double, symbol: String): String {
        return "%s%,.2f".format(symbol, abs(amount))
    }

    private fun buildHighLevelNarrative(
        isAnnual: Boolean,
        periodLabel: String,
        totalExpense: Double,
        totalBudget: Double,
        totalIncome: Double,
        netSavings: Double,
        savingsRatePercent: Double,
        prevExpense: Double,
        diffExpense: Double,
        diffExpensePercent: Double,
        prevMonthName: String,
        monthsLoggedCount: Int,
        monthlyAverageExpense: Double,
        topSpendingCategory: CategorySummaryItem?,
        overBudgetCategories: List<CategorySummaryItem>,
        totalAmountAddedToGoals: Double,
        currencySymbol: String
    ): String {
        if (totalExpense == 0.0 && totalIncome == 0.0 && totalBudget == 0.0) {
            return "No spending, income, or budget records have been logged for $periodLabel yet."
        }

        val parts = mutableListOf<String>()

        // 1. Spending & Budget sentence
        if (!isAnnual) {
            if (totalBudget > 0.0) {
                val pct = (totalExpense / totalBudget) * 100.0
                if (totalExpense <= totalBudget) {
                    parts.add("In $periodLabel, you spent ${formatMoney(totalExpense, currencySymbol)} of your ${formatMoney(totalBudget, currencySymbol)} budget (%.0f%%), remaining ${formatMoney(totalBudget - totalExpense, currencySymbol)} under budget.".format(pct))
                } else {
                    val overPart = if (overBudgetCategories.isNotEmpty()) " (${overBudgetCategories.first().category.name} was over budget)" else ""
                    parts.add("In $periodLabel, spending reached ${formatMoney(totalExpense, currencySymbol)}, exceeding your ${formatMoney(totalBudget, currencySymbol)} budget by ${formatMoney(totalExpense - totalBudget, currencySymbol)}$overPart.")
                }
            } else {
                parts.add("In $periodLabel, you spent a total of ${formatMoney(totalExpense, currencySymbol)}.")
            }
        } else {
            val budgetClause = if (totalBudget > 0.0) " against an annual budget of ${formatMoney(totalBudget, currencySymbol)}" else ""
            parts.add("In $periodLabel, total spending reached ${formatMoney(totalExpense, currencySymbol)}$budgetClause, averaging ${formatMoney(monthlyAverageExpense, currencySymbol)}/month across $monthsLoggedCount logged month(s).")
        }

        // 2. Trend & Top Category sentence
        if (!isAnnual) {
            if (prevExpense > 0.0) {
                val trendDir = if (diffExpense < -0.005) "decreased by %.1f%%".format(abs(diffExpensePercent)) else if (diffExpense > 0.005) "increased by %.1f%%".format(abs(diffExpensePercent)) else "remained flat"
                val topCatStr = topSpendingCategory?.let { ", with ${it.category.name} as your largest expense (${formatMoney(it.spent, currencySymbol)})" } ?: ""
                parts.add("Spending $trendDir compared to $prevMonthName$topCatStr.")
            } else if (topSpendingCategory != null) {
                parts.add("Your largest expense was ${topSpendingCategory.category.name} at ${formatMoney(topSpendingCategory.spent, currencySymbol)}.")
            }
        } else {
            if (topSpendingCategory != null) {
                parts.add("Your leading expense category was ${topSpendingCategory.category.name} at ${formatMoney(topSpendingCategory.spent, currencySymbol)}.")
            }
        }

        // 3. Savings & Goals sentence
        if (totalIncome > 0.0) {
            val savingsClause = if (netSavings >= 0.0) {
                "saved ${formatMoney(netSavings, currencySymbol)} (%.1f%% savings rate)".format(savingsRatePercent)
            } else {
                "concluded with a net deficit of -${formatMoney(abs(netSavings), currencySymbol)}"
            }
            val goalsClause = if (totalAmountAddedToGoals > 0.0) {
                ", and contributed ${formatMoney(totalAmountAddedToGoals, currencySymbol)} toward your goals"
            } else ""
            parts.add("You earned ${formatMoney(totalIncome, currencySymbol)}, $savingsClause$goalsClause.")
        } else if (totalAmountAddedToGoals > 0.0) {
            parts.add("You added ${formatMoney(totalAmountAddedToGoals, currencySymbol)} toward your savings goals.")
        }

        return parts.joinToString(" ")
    }

    private fun buildMonthlyHeadline(
        currentMonthName: String,
        totalExpense: Double,
        totalBudget: Double,
        netSavings: Double,
        currencySymbol: String
    ): String {
        return when {
            totalExpense == 0.0 && totalBudget == 0.0 ->
                "No spending or budget records logged for $currentMonthName yet."
            totalBudget > 0.0 && totalExpense <= totalBudget && netSavings >= 0.0 ->
                "You stayed within your budget and achieved positive savings in $currentMonthName."
            totalBudget > 0.0 && totalExpense > totalBudget && netSavings >= 0.0 ->
                "Spending exceeded your planned budget by ${formatMoney(totalExpense - totalBudget, currencySymbol)}, though you maintained net savings."
            totalBudget > 0.0 && totalExpense > totalBudget && netSavings < 0.0 ->
                "Spending exceeded your planned budget by ${formatMoney(totalExpense - totalBudget, currencySymbol)}, resulting in a monthly deficit."
            totalBudget > 0.0 && totalExpense <= totalBudget && netSavings < 0.0 ->
                "You stayed under budget, but cash flow concluded with a net deficit in $currentMonthName."
            netSavings >= 0.0 ->
                "You generated ${formatMoney(netSavings, currencySymbol)} in net savings in $currentMonthName."
            else ->
                "Expenses exceeded total income by ${formatMoney(abs(netSavings), currencySymbol)} in $currentMonthName."
        }
    }

    private fun buildAnnualHeadline(
        selectedYearStr: String,
        totalExpense: Double,
        totalBudget: Double,
        netSavings: Double,
        currencySymbol: String
    ): String {
        return when {
            totalExpense == 0.0 && totalBudget == 0.0 ->
                "No transaction records logged for $selectedYearStr yet."
            totalBudget > 0.0 && totalExpense <= totalBudget && netSavings >= 0.0 ->
                "You maintained an overall budget surplus and positive net savings in $selectedYearStr."
            totalBudget > 0.0 && totalExpense > totalBudget ->
                "Annual spending tracked ${formatMoney(totalExpense - totalBudget, currencySymbol)} above your total planned budget in $selectedYearStr."
            netSavings >= 0.0 ->
                "You achieved ${formatMoney(netSavings, currencySymbol)} in cumulative net savings throughout $selectedYearStr."
            else ->
                "Outflows exceeded total income by ${formatMoney(abs(netSavings), currencySymbol)} across $selectedYearStr."
        }
    }

    private fun buildMonthlySpendingNarrative(
        currentMonthName: String,
        totalExpense: Double,
        totalBudget: Double,
        categorySummaries: List<CategorySummaryItem>,
        topSpendingCategory: CategorySummaryItem?,
        overBudgetCategories: List<CategorySummaryItem>,
        underBudgetCategories: List<CategorySummaryItem>,
        unbudgetedCategories: List<CategorySummaryItem>,
        currencySymbol: String
    ): String {
        if (totalExpense == 0.0 && totalBudget == 0.0) {
            return "No spending activity or budgets have been recorded for $currentMonthName."
        }
        val activeCatsCount = categorySummaries.count { it.spent > 0.0 }
        val spendBase = if (totalBudget > 0.0) {
            val pctUsed = (totalExpense / totalBudget) * 100.0
            val status = if (totalExpense <= totalBudget) {
                "${formatMoney(totalBudget - totalExpense, currencySymbol)} remaining under your limit"
            } else {
                "${formatMoney(totalExpense - totalBudget, currencySymbol)} over your planned limit"
            }
            "You spent a total of ${formatMoney(totalExpense, currencySymbol)} across $activeCatsCount categories against a planned budget of ${formatMoney(totalBudget, currencySymbol)} (%.1f%% utilized, %s).".format(pctUsed, status)
        } else {
            "You spent a total of ${formatMoney(totalExpense, currencySymbol)} across $activeCatsCount categories (no planned budget was established for this month)."
        }

        val topCatText = if (topSpendingCategory != null) {
            val budgetDetail = if (topSpendingCategory.budgeted > 0.0) {
                val diff = topSpendingCategory.spent - topSpendingCategory.budgeted
                if (diff > 0.0) " (${formatMoney(topSpendingCategory.budgeted, currencySymbol)} budgeted, over by ${formatMoney(diff, currencySymbol)})"
                else " (${formatMoney(topSpendingCategory.budgeted, currencySymbol)} budgeted, ${formatMoney(abs(diff), currencySymbol)} remaining)"
            } else " (unbudgeted)"
            " Your highest expense was ${topSpendingCategory.category.name} at ${formatMoney(topSpendingCategory.spent, currencySymbol)}$budgetDetail."
        } else ""

        val budgetStatusText = when {
            overBudgetCategories.isNotEmpty() -> {
                val overList = overBudgetCategories.joinToString(", ") {
                    "${it.category.name} (+${formatMoney(it.diff, currencySymbol)})"
                }
                " Categories exceeding budget: $overList."
            }
            totalBudget > 0.0 && underBudgetCategories.isNotEmpty() -> {
                " All ${underBudgetCategories.size} budgeted categories stayed within their spending allowances."
            }
            else -> ""
        }

        val unbudgetedText = if (unbudgetedCategories.isNotEmpty()) {
            val unbudgetedTotal = Money.sum(unbudgetedCategories.map { it.spent })
            " Unbudgeted expenses totaled ${formatMoney(unbudgetedTotal, currencySymbol)} across ${unbudgetedCategories.size} categories."
        } else ""

        return "$spendBase$topCatText$budgetStatusText$unbudgetedText"
    }

    private fun buildAnnualSpendingNarrative(
        selectedYearStr: String,
        totalExpense: Double,
        totalBudget: Double,
        categorySummaries: List<CategorySummaryItem>,
        topSpendingCategory: CategorySummaryItem?,
        overBudgetCategories: List<CategorySummaryItem>,
        underBudgetCategories: List<CategorySummaryItem>,
        unbudgetedCategories: List<CategorySummaryItem>,
        currencySymbol: String
    ): String {
        if (totalExpense == 0.0 && totalBudget == 0.0) {
            return "No expenses or budgets have been logged for $selectedYearStr."
        }
        val activeCatsCount = categorySummaries.count { it.spent > 0.0 }
        val spendBase = if (totalBudget > 0.0) {
            val pctUsed = (totalExpense / totalBudget) * 100.0
            val status = if (totalExpense <= totalBudget) {
                "${formatMoney(totalBudget - totalExpense, currencySymbol)} below cumulative target"
            } else {
                "${formatMoney(totalExpense - totalBudget, currencySymbol)} above cumulative target"
            }
            "In $selectedYearStr YTD, cumulative spending totaled ${formatMoney(totalExpense, currencySymbol)} across $activeCatsCount categories against an annual budget of ${formatMoney(totalBudget, currencySymbol)} (%.1f%% utilized, %s).".format(pctUsed, status)
        } else {
            "In $selectedYearStr YTD, total spending reached ${formatMoney(totalExpense, currencySymbol)} across $activeCatsCount categories."
        }

        val topCatText = if (topSpendingCategory != null) {
            val budgetDetail = if (topSpendingCategory.budgeted > 0.0) {
                val diff = topSpendingCategory.spent - topSpendingCategory.budgeted
                if (diff > 0.0) " (${formatMoney(topSpendingCategory.budgeted, currencySymbol)} budgeted, over by ${formatMoney(diff, currencySymbol)})"
                else " (${formatMoney(topSpendingCategory.budgeted, currencySymbol)} budgeted, ${formatMoney(abs(diff), currencySymbol)} under)"
            } else " (unbudgeted)"
            " Your leading expense category for the year was ${topSpendingCategory.category.name} at ${formatMoney(topSpendingCategory.spent, currencySymbol)}$budgetDetail."
        } else ""

        val budgetStatusText = when {
            overBudgetCategories.isNotEmpty() -> {
                val overList = overBudgetCategories.joinToString(", ") {
                    "${it.category.name} (+${formatMoney(it.diff, currencySymbol)})"
                }
                " Annual overspends: $overList."
            }
            totalBudget > 0.0 && underBudgetCategories.isNotEmpty() -> {
                " All ${underBudgetCategories.size} budgeted categories remained within their cumulative allowances."
            }
            else -> ""
        }

        val unbudgetedText = if (unbudgetedCategories.isNotEmpty()) {
            val unbudgetedTotal = Money.sum(unbudgetedCategories.map { it.spent })
            " Total unbudgeted spending was ${formatMoney(unbudgetedTotal, currencySymbol)}."
        } else ""

        return "$spendBase$topCatText$budgetStatusText$unbudgetedText"
    }

    private fun buildMonthlyTrendNarrative(
        totalExpense: Double,
        prevExpense: Double,
        diffExpense: Double,
        diffExpensePercent: Double,
        prevMonthName: String,
        currencySymbol: String
    ): String {
        if (totalExpense == 0.0 && prevExpense == 0.0) {
            return "No expense transactions recorded in this month or the previous month."
        }
        if (prevExpense == 0.0) {
            return "This is the initial month of recorded expenses (${formatMoney(totalExpense, currencySymbol)}); no previous month spending baseline is available."
        }
        val pct = abs(diffExpensePercent)
        val direction = when {
            diffExpense < -0.005 -> "decreased by %.1f%% (-%s)".format(pct, formatMoney(abs(diffExpense), currencySymbol))
            diffExpense > 0.005 -> "increased by %.1f%% (+%s)".format(pct, formatMoney(diffExpense, currencySymbol))
            else -> "remained identical at ${formatMoney(totalExpense, currencySymbol)}"
        }
        return "Overall spending %s compared to %s (%s).".format(
            direction,
            prevMonthName,
            formatMoney(prevExpense, currencySymbol)
        )
    }

    private fun buildAnnualTrendNarrative(
        selectedYearStr: String,
        monthsLoggedCount: Int,
        monthlyAverageExpense: Double,
        totalBudget: Double,
        currencySymbol: String
    ): String {
        val pace = if (totalBudget > 0.0) {
            val expectedMonthlyBudget = totalBudget / 12.0
            if (monthlyAverageExpense <= expectedMonthlyBudget) {
                " maintaining a stable pace within your monthly budget average of ${formatMoney(expectedMonthlyBudget, currencySymbol)}."
            } else {
                " tracking above your monthly budget average of ${formatMoney(expectedMonthlyBudget, currencySymbol)}."
            }
        } else ""
        return "Across $monthsLoggedCount logged month(s) in $selectedYearStr, your spending averaged ${formatMoney(monthlyAverageExpense, currencySymbol)} per month,$pace".trimEnd(',')
    }

    private fun buildSavingsNarrative(
        periodLabel: String,
        totalIncome: Double,
        totalExpense: Double,
        netSavings: Double,
        savingsRatePercent: Double,
        currencySymbol: String
    ): String {
        return when {
            totalIncome == 0.0 && totalExpense == 0.0 ->
                "No income or expense records were logged for $periodLabel."
            totalIncome == 0.0 && totalExpense > 0.0 ->
                "No income was logged during $periodLabel, resulting in a net cash outflow of ${formatMoney(totalExpense, currencySymbol)}."
            netSavings >= 0.0 ->
                "You brought in ${formatMoney(totalIncome, currencySymbol)} in total income and saved ${formatMoney(netSavings, currencySymbol)}, delivering a %.1f%% net savings rate for $periodLabel.".format(savingsRatePercent)
            else ->
                "Total income was ${formatMoney(totalIncome, currencySymbol)} against ${formatMoney(totalExpense, currencySymbol)} in expenses, resulting in a net deficit of -${formatMoney(abs(netSavings), currencySymbol)}."
        }
    }

    private fun buildGoalsNarrative(
        isAnnual: Boolean,
        totalGoalsCount: Int,
        goalsMetCount: Int,
        totalGoalsTarget: Double,
        totalGoalsSaved: Double,
        goalsProgressFraction: Float,
        totalAmountAddedToGoals: Double,
        goalContributions: List<GoalContributionItem>,
        currencySymbol: String
    ): String {
        if (totalGoalsCount == 0) {
            return "No active savings goals found. Set up a savings goal from the Plan tab to track goal contributions here."
        }
        val pctFunded = (goalsProgressFraction * 100.0)
        val completedClause = if (goalsMetCount > 0) ", with $goalsMetCount goal(s) fully achieved" else ""
        val statusText = "You have $totalGoalsCount active savings goal(s) with ${formatMoney(totalGoalsSaved, currencySymbol)} saved toward a ${formatMoney(totalGoalsTarget, currencySymbol)} target (%.1f%% funded)%s.".format(
            pctFunded,
            completedClause
        )
        val timeframeWord = if (isAnnual) "year" else "month"
        val additionsText = if (totalAmountAddedToGoals > 0.0) {
            val breakdown = goalContributions.joinToString(", ") {
                "${it.goalName} (+${formatMoney(it.amountAdded, currencySymbol)})"
            }
            " A total of ${formatMoney(totalAmountAddedToGoals, currencySymbol)} was added to your goals this $timeframeWord: $breakdown."
        } else {
            " No contributions were added to your savings goals during this $timeframeWord."
        }
        return "$statusText$additionsText"
    }
}
