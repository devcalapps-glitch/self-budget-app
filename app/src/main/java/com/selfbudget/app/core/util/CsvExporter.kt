package com.selfbudget.app.core.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.selfbudget.app.data.model.AccountEntity
import com.selfbudget.app.data.model.BudgetEntity
import com.selfbudget.app.data.model.CategoryEntity
import com.selfbudget.app.data.model.GoalEntity
import com.selfbudget.app.data.model.RecurringTransactionEntity
import com.selfbudget.app.data.model.TransactionEntity
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

enum class ExportDataType(
    val title: String,
    val subtitle: String,
    val filePrefix: String
) {
    TRANSACTIONS("Transactions", "All recorded income, expense, and transfer records", "transactions"),
    RECURRING("Recurring Transactions", "Scheduled recurring bills, subscriptions, and paychecks", "recurring_transactions"),
    BUDGET("Budget Plan", "Configured monthly category budget ceilings and limits", "budget_plan"),
    GOALS("Savings Goals", "Savings targets, saved balances, and linked accounts", "savings_goals"),
    ACCOUNTS("Accounts & Wallets", "Bank accounts, credit cards, cash wallets, and balances", "accounts_and_wallets")
}

object CsvExporter {

    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val fileTimestampFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    fun generateTransactionsCsv(
        transactions: List<TransactionEntity>,
        categories: List<CategoryEntity>,
        accounts: List<AccountEntity> = emptyList()
    ): String {
        val categoryMap = categories.associateBy { it.id }
        val accountMap = accounts.associateBy { it.id }
        val sb = StringBuilder()

        sb.append("Transaction ID,Date,Title,Type,Amount ($),Category,Account / Wallet,Transfer Destination Account,Payment Method,Note\n")
        for (tx in transactions) {
            val categoryName = categoryMap[tx.categoryId]?.name ?: "General"
            val accountName = accountMap[tx.accountId]?.name ?: tx.accountId
            val transferAccountName = tx.transferAccountId?.let { accountMap[it]?.name ?: it } ?: ""
            val dateStr = dateTimeFormat.format(Date(tx.timestamp))
            val titleEscaped = escapeCsvField(tx.title)
            val noteEscaped = escapeCsvField(tx.note ?: "")
            val paymentMethodEscaped = escapeCsvField(tx.paymentMethod ?: "Cash")
            val accountEscaped = escapeCsvField(accountName)
            val transferEscaped = escapeCsvField(transferAccountName)

            sb.append("${tx.id},")
            sb.append("$dateStr,")
            sb.append("$titleEscaped,")
            sb.append("${tx.type.name},")
            sb.append("%.2f,".format(Locale.US, tx.amount))
            sb.append("${escapeCsvField(categoryName)},")
            sb.append("$accountEscaped,")
            sb.append("$transferEscaped,")
            sb.append("$paymentMethodEscaped,")
            sb.append("$noteEscaped\n")
        }
        return sb.toString()
    }

    fun generateRecurringCsv(
        recurring: List<RecurringTransactionEntity>,
        categories: List<CategoryEntity>,
        accounts: List<AccountEntity> = emptyList()
    ): String {
        val categoryMap = categories.associateBy { it.id }
        val accountMap = accounts.associateBy { it.id }
        val sb = StringBuilder()

        sb.append("Recurring ID,Title,Type,Amount ($),Category,Account / Wallet,Frequency,Next Due Date,Remaining Occurrences,Status,Payment Method,Note\n")
        for (rec in recurring) {
            val categoryName = categoryMap[rec.categoryId]?.name ?: "General"
            val accountName = accountMap[rec.accountId]?.name ?: rec.accountId
            val nextDueDateStr = dateFormat.format(Date(rec.nextDueDate))
            val remainingStr = rec.remainingOccurrences?.toString() ?: "Indefinite"
            val statusStr = if (rec.isArchived) "Paused / Completed" else "Active"
            val titleEscaped = escapeCsvField(rec.title)
            val noteEscaped = escapeCsvField(rec.note ?: "")
            val paymentMethodEscaped = escapeCsvField(rec.paymentMethod ?: "Credit Card")
            val accountEscaped = escapeCsvField(accountName)

            sb.append("${rec.id},")
            sb.append("$titleEscaped,")
            sb.append("${rec.type.name},")
            sb.append("%.2f,".format(Locale.US, rec.amount))
            sb.append("${escapeCsvField(categoryName)},")
            sb.append("$accountEscaped,")
            sb.append("${rec.frequency.name},")
            sb.append("$nextDueDateStr,")
            sb.append("$remainingStr,")
            sb.append("$statusStr,")
            sb.append("$paymentMethodEscaped,")
            sb.append("$noteEscaped\n")
        }
        return sb.toString()
    }

    fun generateBudgetCsv(
        budgets: List<BudgetEntity>,
        categories: List<CategoryEntity>
    ): String {
        val categoryMap = categories.associateBy { it.id }
        val sb = StringBuilder()

        sb.append("Budget ID,Category,Month / Period,Budget Limit ($),Rollover Enabled,Auto Synced\n")
        for (b in budgets) {
            val categoryName = categoryMap[b.categoryId]?.name ?: b.categoryId
            val rolloverStr = if (b.rolloverEnabled) "Yes" else "No"
            val autoSyncedStr = if (b.isAutoSynced) "Yes" else "No"

            sb.append("${b.id},")
            sb.append("${escapeCsvField(categoryName)},")
            sb.append("${b.monthYear},")
            sb.append("%.2f,".format(Locale.US, b.amountLimit))
            sb.append("$rolloverStr,")
            sb.append("$autoSyncedStr\n")
        }
        return sb.toString()
    }

    fun generateGoalsCsv(
        goals: List<GoalEntity>,
        accounts: List<AccountEntity> = emptyList()
    ): String {
        val accountMap = accounts.associateBy { it.id }
        val sb = StringBuilder()

        sb.append("Goal ID,Goal Name,Target Amount ($),Saved Amount ($),Linked Account / Wallet,Target Date,Created Date\n")
        for (g in goals) {
            val linkedAccountName = g.linkedAccountId?.let { accountMap[it]?.name ?: it } ?: "None (Direct Savings)"
            val targetDateStr = g.targetDate?.let { dateFormat.format(Date(it)) } ?: "No deadline"
            val createdDateStr = dateFormat.format(Date(g.createdAt))

            sb.append("${g.id},")
            sb.append("${escapeCsvField(g.name)},")
            sb.append("%.2f,".format(Locale.US, g.targetAmount))
            sb.append("%.2f,".format(Locale.US, g.savedAmount))
            sb.append("${escapeCsvField(linkedAccountName)},")
            sb.append("$targetDateStr,")
            sb.append("$createdDateStr\n")
        }
        return sb.toString()
    }

    fun generateAccountsCsv(
        accounts: List<AccountEntity>,
        accountBalances: Map<String, Double> = emptyMap()
    ): String {
        val sb = StringBuilder()

        sb.append("Account ID,Account Name,Account Type,Live Balance ($),Initial Balance ($),Currency,Credit Limit ($),Interest Rate APR (%),Minimum Payment ($),Is Default\n")
        for (acc in accounts) {
            val liveBalance = accountBalances[acc.id] ?: acc.initialBalance
            val creditLimitStr = acc.creditLimit?.let { "%.2f".format(Locale.US, it) } ?: "N/A"
            val aprStr = acc.interestRateApr?.let { "%.2f%%".format(Locale.US, it) } ?: "N/A"
            val minPayStr = acc.minimumPayment?.let { "%.2f".format(Locale.US, it) } ?: "N/A"
            val isDefaultStr = if (acc.isDefault) "Yes" else "No"

            sb.append("${acc.id},")
            sb.append("${escapeCsvField(acc.name)},")
            sb.append("${acc.type.name},")
            sb.append("%.2f,".format(Locale.US, liveBalance))
            sb.append("%.2f,".format(Locale.US, acc.initialBalance))
            sb.append("${acc.currencyCode},")
            sb.append("$creditLimitStr,")
            sb.append("$aprStr,")
            sb.append("$minPayStr,")
            sb.append("$isDefaultStr\n")
        }
        return sb.toString()
    }

    fun exportAndShareTransactions(
        context: Context,
        transactions: List<TransactionEntity>,
        categories: List<CategoryEntity>,
        accounts: List<AccountEntity> = emptyList()
    ): Boolean {
        val csvContent = generateTransactionsCsv(transactions, categories, accounts)
        val timeStamp = fileTimestampFormat.format(Date())
        val fileName = "SelfBudget_Transactions_$timeStamp.csv"
        return writeAndShareSingleCsv(context, fileName, csvContent, "Self Budget - Transactions Export")
    }

    fun exportAndShareSelected(
        context: Context,
        selectedTypes: Set<ExportDataType>,
        transactions: List<TransactionEntity>,
        categories: List<CategoryEntity>,
        accounts: List<AccountEntity>,
        recurring: List<RecurringTransactionEntity>,
        budgets: List<BudgetEntity>,
        goals: List<GoalEntity>,
        accountBalances: Map<String, Double> = emptyMap()
    ): Boolean {
        if (selectedTypes.isEmpty()) return false

        return try {
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }

            val timestamp = fileTimestampFormat.format(Date())

            if (selectedTypes.size == 1) {
                val singleType = selectedTypes.first()
                val csvContent = when (singleType) {
                    ExportDataType.TRANSACTIONS -> generateTransactionsCsv(transactions, categories, accounts)
                    ExportDataType.RECURRING -> generateRecurringCsv(recurring, categories, accounts)
                    ExportDataType.BUDGET -> generateBudgetCsv(budgets, categories)
                    ExportDataType.GOALS -> generateGoalsCsv(goals, accounts)
                    ExportDataType.ACCOUNTS -> generateAccountsCsv(accounts, accountBalances)
                }
                val fileName = "SelfBudget_${singleType.filePrefix}_$timestamp.csv"
                writeAndShareSingleCsv(context, fileName, csvContent, "Self Budget - ${singleType.title} Export")
            } else {
                // Multi-export bundle (Zip containing individual CSV files)
                val generatedFiles = mutableListOf<File>()

                for (type in selectedTypes) {
                    val (csvText, fileName) = when (type) {
                        ExportDataType.TRANSACTIONS -> Pair(
                            generateTransactionsCsv(transactions, categories, accounts),
                            "transactions.csv"
                        )
                        ExportDataType.RECURRING -> Pair(
                            generateRecurringCsv(recurring, categories, accounts),
                            "recurring_transactions.csv"
                        )
                        ExportDataType.BUDGET -> Pair(
                            generateBudgetCsv(budgets, categories),
                            "budget_plan.csv"
                        )
                        ExportDataType.GOALS -> Pair(
                            generateGoalsCsv(goals, accounts),
                            "savings_goals.csv"
                        )
                        ExportDataType.ACCOUNTS -> Pair(
                            generateAccountsCsv(accounts, accountBalances),
                            "accounts_and_wallets.csv"
                        )
                    }

                    val csvFile = File(exportDir, fileName)
                    val writer = FileWriter(csvFile)
                    writer.write(csvText)
                    writer.flush()
                    writer.close()
                    generatedFiles.add(csvFile)
                }

                val zipFileName = "SelfBudget_Export_Bundle_$timestamp.zip"
                val zipFile = File(exportDir, zipFileName)
                val zipOut = ZipOutputStream(FileOutputStream(zipFile))

                for (file in generatedFiles) {
                    val zipEntry = ZipEntry(file.name)
                    zipOut.putNextEntry(zipEntry)
                    val fis = FileInputStream(file)
                    fis.copyTo(zipOut)
                    fis.close()
                    zipOut.closeEntry()
                }
                zipOut.close()

                val contentUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    zipFile
                )

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/zip"
                    putExtra(Intent.EXTRA_SUBJECT, "Self Budget - Data Export Bundle")
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                context.startActivity(Intent.createChooser(shareIntent, "Share or Save Export Bundle (.zip)"))
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun writeAndShareSingleCsv(
        context: Context,
        fileName: String,
        csvContent: String,
        subject: String
    ): Boolean {
        return try {
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }

            val file = File(exportDir, fileName)
            val writer = FileWriter(file)
            writer.write(csvContent)
            writer.flush()
            writer.close()

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Share or Save CSV Export"))
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun escapeCsvField(field: String): String {
        return if (field.contains(",") || field.contains("\"") || field.contains("\n") || field.contains("\r")) {
            "\"" + field.replace("\"", "\"\"") + "\""
        } else {
            field
        }
    }
}
