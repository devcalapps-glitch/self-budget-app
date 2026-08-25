package com.selfbudget.app.data.model

data class SyncDataPayload(
    val schemaVersion: Int = 7,
    val exportTimestamp: Long = System.currentTimeMillis(),
    val appVersion: String = "1.0",
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val budgets: List<BudgetEntity> = emptyList(),
    val recurringTransactions: List<RecurringTransactionEntity> = emptyList(),
    val goals: List<GoalEntity> = emptyList(),
    val netWorthSnapshots: List<NetWorthSnapshotEntity> = emptyList(),
    val exchangeRates: List<ExchangeRateEntity> = emptyList()
)
