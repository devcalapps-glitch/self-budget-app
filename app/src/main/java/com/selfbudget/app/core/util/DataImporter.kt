package com.selfbudget.app.core.util

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.selfbudget.app.data.local.AppDatabase
import com.selfbudget.app.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

data class ParsedImportData(
    val format: String,
    val fileName: String,
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val budgets: List<BudgetEntity> = emptyList(),
    val recurring: List<RecurringTransactionEntity> = emptyList(),
    val goals: List<GoalEntity> = emptyList(),
    val snapshots: List<NetWorthSnapshotEntity> = emptyList(),
    val exchangeRates: List<ExchangeRateEntity> = emptyList()
) {
    val totalCount: Int
        get() = accounts.size + categories.size + transactions.size + budgets.size + recurring.size + goals.size
}

object DataImporter {

    private val gson = Gson()
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    suspend fun parseFromUri(
        context: Context,
        uri: Uri,
        userId: String
    ): Result<ParsedImportData> = withContext(Dispatchers.IO) {
        try {
            val fileName = getFileName(context, uri) ?: "imported_file"
            val extension = fileName.substringAfterLast('.', "").lowercase()

            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@withContext Result.failure(IOException("Could not open file input stream"))

            parseFromBytes(bytes, fileName, extension, userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun parseFromBytes(
        bytes: ByteArray,
        fileName: String,
        extension: String,
        userId: String
    ): Result<ParsedImportData> {
        return try {
            when (extension) {
                "json" -> parseJsonBytes(bytes, fileName, userId)
                "csv" -> parseCsvBytes(bytes, fileName, userId)
                "xlsx" -> parseXlsxBytes(bytes, fileName, userId)
                else -> {
                    // Try auto-detecting format from content
                    if (isZipBytes(bytes)) {
                        parseXlsxBytes(bytes, fileName, userId)
                    } else {
                        val text = String(bytes, Charsets.UTF_8).trim()
                        if (text.startsWith("{") || text.startsWith("[")) {
                            parseJsonBytes(bytes, fileName, userId)
                        } else {
                            parseCsvBytes(bytes, fileName, userId)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun isZipBytes(bytes: ByteArray): Boolean {
        return bytes.size >= 4 &&
                bytes[0] == 0x50.toByte() &&
                bytes[1] == 0x4B.toByte() &&
                bytes[2] == 0x03.toByte() &&
                bytes[3] == 0x04.toByte()
    }

    // -------------------------------------------------------------
    // JSON PARSER
    // -------------------------------------------------------------
    fun parseJsonBytes(bytes: ByteArray, fileName: String, userId: String): Result<ParsedImportData> {
        return try {
            val jsonString = String(bytes, Charsets.UTF_8)
            val payload = gson.fromJson(jsonString, SyncDataPayload::class.java)
                ?: return Result.failure(IllegalArgumentException("Empty or invalid JSON backup"))

            val accounts = payload.accounts.map { it.copy(userId = userId) }
            val transactions = payload.transactions.map { it.copy(userId = userId) }
            val budgets = payload.budgets.map { it.copy(userId = userId) }
            val goals = payload.goals.map { it.copy(userId = userId) }
            val recurring = payload.recurringTransactions.map { it.copy(userId = userId) }
            val snapshots = payload.netWorthSnapshots.map { it.copy(userId = userId) }
            val rates = payload.exchangeRates.map { it.copy(userId = userId) }

            Result.success(
                ParsedImportData(
                    format = "JSON Backup (.json)",
                    fileName = fileName,
                    accounts = accounts,
                    categories = payload.categories,
                    transactions = transactions,
                    budgets = budgets,
                    recurring = recurring,
                    goals = goals,
                    snapshots = snapshots,
                    exchangeRates = rates
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------
    // CSV PARSER
    // -------------------------------------------------------------
    fun parseCsvBytes(bytes: ByteArray, fileName: String, userId: String): Result<ParsedImportData> {
        return try {
            val content = String(bytes, Charsets.UTF_8)
            val rows = parseCsvRows(content)
            if (rows.isEmpty()) {
                return Result.failure(IllegalArgumentException("CSV file is empty"))
            }

            val header = rows.first().map { it.trim().lowercase() }
            val dataRows = rows.drop(1)

            val parsed = if (header.any { it.contains("recurring id") || it.contains("remaining occurrences") || it.contains("frequency") }) {
                // Recurring CSV
                val recList = dataRows.mapNotNull { row -> parseRecurringRow(header, row, userId) }
                ParsedImportData(format = "CSV (Recurring Bills)", fileName = fileName, recurring = recList)
            } else if (header.any { it.contains("budget limit") || it.contains("month / period") || it.contains("rollover") }) {
                // Budget CSV
                val budgets = dataRows.mapNotNull { row -> parseBudgetRow(header, row, userId) }
                ParsedImportData(format = "CSV (Budget Plan)", fileName = fileName, budgets = budgets)
            } else if (header.any { it.contains("goal name") || it.contains("target amount") || it.contains("saved amount") }) {
                // Goals CSV
                val goals = dataRows.mapNotNull { row -> parseGoalRow(header, row, userId) }
                ParsedImportData(format = "CSV (Savings Goals)", fileName = fileName, goals = goals)
            } else if (header.any { it.contains("account type") || it.contains("live balance") || it.contains("credit limit") || it.contains("apr") }) {
                // Accounts CSV
                val accounts = dataRows.mapNotNull { row -> parseAccountRow(header, row, userId) }
                ParsedImportData(format = "CSV (Accounts & Wallets)", fileName = fileName, accounts = accounts)
            } else {
                // Transactions CSV (Default)
                val txs = dataRows.mapNotNull { row -> parseTransactionRow(header, row, userId) }
                ParsedImportData(format = "CSV (Transactions)", fileName = fileName, transactions = txs)
            }

            Result.success(parsed)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------
    // EXCEL (.XLSX) PARSER
    // -------------------------------------------------------------
    fun parseXlsxBytes(bytes: ByteArray, fileName: String, userId: String): Result<ParsedImportData> {
        return try {
            val entries = mutableMapOf<String, ByteArray>()
            val zipIn = ZipInputStream(ByteArrayInputStream(bytes))
            var entry = zipIn.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val buffer = ByteArrayOutputStream()
                    val data = ByteArray(4096)
                    var count: Int
                    while (zipIn.read(data).also { count = it } != -1) {
                        buffer.write(data, 0, count)
                    }
                    entries[entry.name] = buffer.toByteArray()
                }
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
            zipIn.close()

            // 1. Parse Shared Strings if available
            val sharedStrings = mutableListOf<String>()
            val sstBytes = entries["xl/sharedStrings.xml"]
            if (sstBytes != null) {
                sharedStrings.addAll(parseSharedStringsXml(sstBytes))
            }

            // 2. Parse Workbook sheet names
            val sheetNames = mutableListOf<String>()
            val wbBytes = entries["xl/workbook.xml"]
            if (wbBytes != null) {
                sheetNames.addAll(parseWorkbookSheetNames(wbBytes))
            }

            val accounts = mutableListOf<AccountEntity>()
            val transactions = mutableListOf<TransactionEntity>()
            val budgets = mutableListOf<BudgetEntity>()
            val recurring = mutableListOf<RecurringTransactionEntity>()
            val goals = mutableListOf<GoalEntity>()

            // 3. Process each sheet
            var sheetIndex = 1
            while (true) {
                val sheetPath = "xl/worksheets/sheet$sheetIndex.xml"
                val sheetBytes = entries[sheetPath] ?: break
                val sheetName = sheetNames.getOrNull(sheetIndex - 1) ?: "Sheet $sheetIndex"
                val rows = parseSheetXml(sheetBytes, sharedStrings)

                if (rows.isNotEmpty()) {
                    val header = rows.first().map { it.trim().lowercase() }
                    val dataRows = rows.drop(1)

                    val normalizedName = sheetName.lowercase()
                    if (normalizedName.contains("transaction") && !normalizedName.contains("recurring")) {
                        transactions.addAll(dataRows.mapNotNull { parseTransactionRow(header, it, userId) })
                    } else if (normalizedName.contains("recurring")) {
                        recurring.addAll(dataRows.mapNotNull { parseRecurringRow(header, it, userId) })
                    } else if (normalizedName.contains("budget")) {
                        budgets.addAll(dataRows.mapNotNull { parseBudgetRow(header, it, userId) })
                    } else if (normalizedName.contains("goal")) {
                        goals.addAll(dataRows.mapNotNull { parseGoalRow(header, it, userId) })
                    } else if (normalizedName.contains("account") || normalizedName.contains("wallet")) {
                        accounts.addAll(dataRows.mapNotNull { parseAccountRow(header, it, userId) })
                    } else {
                        // Fallback detection via header columns
                        if (header.any { it.contains("remaining occurrences") || it.contains("frequency") }) {
                            recurring.addAll(dataRows.mapNotNull { parseRecurringRow(header, it, userId) })
                        } else if (header.any { it.contains("budget limit") || it.contains("month / period") }) {
                            budgets.addAll(dataRows.mapNotNull { parseBudgetRow(header, it, userId) })
                        } else if (header.any { it.contains("goal name") || it.contains("target amount") }) {
                            goals.addAll(dataRows.mapNotNull { parseGoalRow(header, it, userId) })
                        } else if (header.any { it.contains("account type") || it.contains("live balance") }) {
                            accounts.addAll(dataRows.mapNotNull { parseAccountRow(header, it, userId) })
                        } else {
                            transactions.addAll(dataRows.mapNotNull { parseTransactionRow(header, it, userId) })
                        }
                    }
                }

                sheetIndex++
            }

            Result.success(
                ParsedImportData(
                    format = "Excel Workbook (.xlsx)",
                    fileName = fileName,
                    accounts = accounts,
                    transactions = transactions,
                    budgets = budgets,
                    recurring = recurring,
                    goals = goals
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------
    // ATOMIC ROOM DB COMMIT
    // -------------------------------------------------------------
    suspend fun commitImportToDatabase(
        db: AppDatabase,
        data: ParsedImportData
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            db.runInTransaction {
                kotlinx.coroutines.runBlocking {
                    if (data.categories.isNotEmpty()) {
                        db.categoryDao().insertCategories(data.categories)
                    }
                    if (data.accounts.isNotEmpty()) {
                        db.accountDao().insertAccounts(data.accounts)
                    }
                    if (data.transactions.isNotEmpty()) {
                        db.transactionDao().insertTransactions(data.transactions)
                    }
                    if (data.budgets.isNotEmpty()) {
                        db.budgetDao().insertBudgets(data.budgets)
                    }
                    if (data.recurring.isNotEmpty()) {
                        db.recurringDao().insertRecurringList(data.recurring)
                    }
                    if (data.goals.isNotEmpty()) {
                        db.goalDao().insertGoals(data.goals)
                    }
                    if (data.snapshots.isNotEmpty()) {
                        db.netWorthDao().insertSnapshots(data.snapshots)
                    }
                    if (data.exchangeRates.isNotEmpty()) {
                        db.exchangeRateDao().insertRates(data.exchangeRates)
                    }
                }
            }
            Result.success(data.totalCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------
    // ROW PARSING HELPERS
    // -------------------------------------------------------------
    private fun parseTransactionRow(header: List<String>, row: List<String>, userId: String): TransactionEntity? {
        if (row.isEmpty()) return null
        val idIdx = header.indexOfFirst { it == "transaction id" || it == "id" || it.startsWith("transaction id") }
        val dateIdx = header.indexOfFirst { it.contains("date") || it.contains("time") }
        val titleIdx = header.indexOfFirst { it == "title" || it == "description" || it == "merchant" || it.contains("title") || it.contains("description") }
        val typeIdx = header.indexOfFirst { it == "type" || it == "transaction type" }
        val amountIdx = header.indexOfFirst { it == "amount" || it.contains("amount") }
        val catIdx = header.indexOfFirst { it == "category" || it.contains("category") }
        val accIdx = header.indexOfFirst { it == "account / wallet" || it == "account" || (it.contains("account") && !it.contains("transfer") && !it.contains("id") && !it.contains("name")) }
        val transferIdx = header.indexOfFirst { it.contains("transfer") }
        val methodIdx = header.indexOfFirst { it.contains("method") || it.contains("payment") }
        val noteIdx = header.indexOfFirst { it.contains("note") || it.contains("memo") }

        val id = if (idIdx >= 0 && idIdx < row.size && row[idIdx].isNotBlank()) row[idIdx] else UUID.randomUUID().toString()
        val title = if (titleIdx >= 0 && titleIdx < row.size) row[titleIdx].trim() else "Transaction"
        val rawAmount = if (amountIdx >= 0 && amountIdx < row.size) row[amountIdx].replace("$", "").replace(",", "").trim().toDoubleOrNull() ?: 0.0 else 0.0
        val amount = Math.abs(rawAmount)

        val rawType = if (typeIdx >= 0 && typeIdx < row.size) row[typeIdx].trim().uppercase() else ""
        val type = when {
            rawType.contains("INCOME") -> TransactionType.INCOME
            rawType.contains("TRANSFER") -> TransactionType.TRANSFER
            rawAmount < 0 -> TransactionType.EXPENSE
            else -> TransactionType.EXPENSE
        }

        val rawDate = if (dateIdx >= 0 && dateIdx < row.size) row[dateIdx].trim() else ""
        val timestamp = parseDateStringToMillis(rawDate)

        val categoryId = if (catIdx >= 0 && catIdx < row.size) row[catIdx].trim().ifBlank { "cat_other" } else "cat_other"
        val accountId = if (accIdx >= 0 && accIdx < row.size) row[accIdx].trim().ifBlank { "acc_main" } else "acc_main"
        val transferAccountId = if (transferIdx >= 0 && transferIdx < row.size) row[transferIdx].trim().ifBlank { null } else null
        val paymentMethod = if (methodIdx >= 0 && methodIdx < row.size) row[methodIdx].trim().ifBlank { "Cash" } else "Cash"
        val note = if (noteIdx >= 0 && noteIdx < row.size) row[noteIdx].trim().ifBlank { null } else null

        if (title.isBlank() && amount == 0.0) return null

        return TransactionEntity(
            id = id,
            userId = userId,
            title = title.ifBlank { "Untitled Transaction" },
            amount = amount,
            type = type,
            categoryId = categoryId,
            accountId = accountId,
            transferAccountId = transferAccountId,
            timestamp = timestamp,
            note = note,
            paymentMethod = paymentMethod
        )
    }

    private fun parseRecurringRow(header: List<String>, row: List<String>, userId: String): RecurringTransactionEntity? {
        if (row.isEmpty()) return null
        val idIdx = header.indexOfFirst { it == "recurring id" || it == "id" || it.startsWith("recurring id") }
        val titleIdx = header.indexOfFirst { it == "title" || it.contains("title") || it.contains("merchant") }
        val typeIdx = header.indexOfFirst { it == "type" }
        val amountIdx = header.indexOfFirst { it == "amount" || it.contains("amount") }
        val catIdx = header.indexOfFirst { it == "category" || it.contains("category") }
        val accIdx = header.indexOfFirst { it == "account" || it.contains("account") }
        val freqIdx = header.indexOfFirst { it == "frequency" || it.contains("frequency") }
        val dueIdx = header.indexOfFirst { it.contains("due") || it.contains("date") }
        val remainIdx = header.indexOfFirst { it.contains("remaining") || it.contains("occurrences") }
        val statusIdx = header.indexOfFirst { it == "status" || it.contains("status") }
        val noteIdx = header.indexOfFirst { it.contains("note") }

        val id = if (idIdx >= 0 && idIdx < row.size && row[idIdx].isNotBlank()) row[idIdx] else UUID.randomUUID().toString()
        val title = if (titleIdx >= 0 && titleIdx < row.size) row[titleIdx].trim() else "Recurring Bill"
        val amount = if (amountIdx >= 0 && amountIdx < row.size) Math.abs(row[amountIdx].replace("$", "").replace(",", "").trim().toDoubleOrNull() ?: 0.0) else 0.0
        val type = if (typeIdx >= 0 && typeIdx < row.size && row[typeIdx].uppercase().contains("INCOME")) TransactionType.INCOME else TransactionType.EXPENSE

        val rawFreq = if (freqIdx >= 0 && freqIdx < row.size) row[freqIdx].trim().uppercase() else "MONTHLY"
        val frequency = try {
            RecurringFrequency.valueOf(rawFreq)
        } catch (_: Exception) {
            RecurringFrequency.MONTHLY
        }

        val rawDate = if (dueIdx >= 0 && dueIdx < row.size) row[dueIdx].trim() else ""
        val nextDueDate = parseDateStringToMillis(rawDate)
        val remainingOccurrences = if (remainIdx >= 0 && remainIdx < row.size) row[remainIdx].trim().toIntOrNull() else null
        val isArchived = if (statusIdx >= 0 && statusIdx < row.size) row[statusIdx].lowercase().contains("paused") || row[statusIdx].lowercase().contains("archive") else false
        val note = if (noteIdx >= 0 && noteIdx < row.size) row[noteIdx].trim().ifBlank { null } else null

        if (title.isBlank() && amount == 0.0) return null

        return RecurringTransactionEntity(
            id = id,
            userId = userId,
            title = title,
            amount = amount,
            type = type,
            categoryId = if (catIdx >= 0 && catIdx < row.size) row[catIdx].trim().ifBlank { "cat_other" } else "cat_other",
            accountId = if (accIdx >= 0 && accIdx < row.size) row[accIdx].trim().ifBlank { "acc_main" } else "acc_main",
            frequency = frequency,
            nextDueDate = nextDueDate,
            remainingOccurrences = remainingOccurrences,
            isArchived = isArchived,
            note = note
        )
    }

    private fun parseBudgetRow(header: List<String>, row: List<String>, userId: String): BudgetEntity? {
        if (row.isEmpty()) return null
        val idIdx = header.indexOfFirst { it == "budget id" || it == "id" || it.startsWith("budget id") }
        val catIdx = header.indexOfFirst { it == "category" || it.contains("category") }
        val monthIdx = header.indexOfFirst { it.contains("month") || it.contains("period") }
        val amountIdx = header.indexOfFirst { it.contains("limit") || it.contains("amount") }
        val rolloverIdx = header.indexOfFirst { it.contains("rollover") }
        val syncIdx = header.indexOfFirst { it.contains("synced") || it.contains("auto") }

        val id = if (idIdx >= 0 && idIdx < row.size && row[idIdx].isNotBlank()) row[idIdx] else UUID.randomUUID().toString()
        val categoryId = if (catIdx >= 0 && catIdx < row.size) row[catIdx].trim().ifBlank { "cat_other" } else "cat_other"
        val monthYear = if (monthIdx >= 0 && monthIdx < row.size && row[monthIdx].isNotBlank()) row[monthIdx].trim() else SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        val amountLimit = if (amountIdx >= 0 && amountIdx < row.size) Math.abs(row[amountIdx].replace("$", "").replace(",", "").trim().toDoubleOrNull() ?: 0.0) else 0.0
        val rolloverEnabled = if (rolloverIdx >= 0 && rolloverIdx < row.size) row[rolloverIdx].lowercase().contains("yes") || row[rolloverIdx].lowercase().contains("true") else false
        val isAutoSynced = if (syncIdx >= 0 && syncIdx < row.size) row[syncIdx].lowercase().contains("yes") || row[syncIdx].lowercase().contains("true") else false

        return BudgetEntity(
            id = id,
            userId = userId,
            categoryId = categoryId,
            amountLimit = amountLimit,
            monthYear = monthYear,
            rolloverEnabled = rolloverEnabled,
            isAutoSynced = isAutoSynced
        )
    }

    private fun parseGoalRow(header: List<String>, row: List<String>, userId: String): GoalEntity? {
        if (row.isEmpty()) return null
        val idIdx = header.indexOfFirst { it == "goal id" || it == "id" || it.startsWith("goal id") }
        val nameIdx = header.indexOfFirst { it == "goal name" || it == "name" || it.contains("name") || it.contains("title") }
        val targetIdx = header.indexOfFirst { it.contains("target") && it.contains("amount") }
        val savedIdx = header.indexOfFirst { it.contains("saved") || it.contains("current") }
        val accIdx = header.indexOfFirst { it.contains("account") }
        val targetDateIdx = header.indexOfFirst { it.contains("target date") || it.contains("deadline") }
        val createdDateIdx = header.indexOfFirst { it.contains("created") }

        val id = if (idIdx >= 0 && idIdx < row.size && row[idIdx].isNotBlank()) row[idIdx] else UUID.randomUUID().toString()
        val name = if (nameIdx >= 0 && nameIdx < row.size) row[nameIdx].trim() else "Savings Goal"
        val targetAmount = if (targetIdx >= 0 && targetIdx < row.size) Math.abs(row[targetIdx].replace("$", "").replace(",", "").trim().toDoubleOrNull() ?: 0.0) else 0.0
        val savedAmount = if (savedIdx >= 0 && savedIdx < row.size) Math.abs(row[savedIdx].replace("$", "").replace(",", "").trim().toDoubleOrNull() ?: 0.0) else 0.0
        val linkedAccountId = if (accIdx >= 0 && accIdx < row.size && !row[accIdx].lowercase().contains("none")) row[accIdx].trim().ifBlank { null } else null

        val targetDate = if (targetDateIdx >= 0 && targetDateIdx < row.size && !row[targetDateIdx].lowercase().contains("no deadline")) parseDateStringToMillis(row[targetDateIdx].trim()) else null
        val createdAt = if (createdDateIdx >= 0 && createdDateIdx < row.size) parseDateStringToMillis(row[createdDateIdx].trim()) else System.currentTimeMillis()

        if (name.isBlank() && targetAmount == 0.0) return null

        return GoalEntity(
            id = id,
            userId = userId,
            name = name,
            targetAmount = targetAmount,
            savedAmount = savedAmount,
            targetDate = targetDate,
            linkedAccountId = linkedAccountId,
            createdAt = createdAt
        )
    }

    private fun parseAccountRow(header: List<String>, row: List<String>, userId: String): AccountEntity? {
        if (row.isEmpty()) return null
        val idIdx = header.indexOfFirst { it == "account id" || it == "id" || it.startsWith("account id") }
        val nameIdx = header.indexOfFirst { it == "account name" || it == "name" || (it.contains("name") && !it.contains("user")) }
        val typeIdx = header.indexOfFirst { it == "account type" || it == "type" || (it.contains("type") && !it.contains("transaction")) }
        val initBalIdx = header.indexOfFirst { it.contains("initial") }
        val currIdx = header.indexOfFirst { it.contains("currency") }
        val limitIdx = header.indexOfFirst { it.contains("credit limit") }
        val aprIdx = header.indexOfFirst { it.contains("apr") || it.contains("interest") }
        val minPayIdx = header.indexOfFirst { it.contains("minimum payment") }
        val defIdx = header.indexOfFirst { it.contains("default") }

        val id = if (idIdx >= 0 && idIdx < row.size && row[idIdx].isNotBlank()) row[idIdx] else UUID.randomUUID().toString()
        val name = if (nameIdx >= 0 && nameIdx < row.size) row[nameIdx].trim() else "Account"

        val rawType = if (typeIdx >= 0 && typeIdx < row.size) row[typeIdx].trim().uppercase() else "CHECKING"
        val type = try {
            AccountType.valueOf(rawType)
        } catch (_: Exception) {
            when {
                rawType.contains("SAVING") -> AccountType.SAVINGS
                rawType.contains("CREDIT") -> AccountType.CREDIT_CARD
                rawType.contains("CASH") -> AccountType.CASH
                rawType.contains("INVEST") -> AccountType.INVESTMENT
                rawType.contains("RETIRE") -> AccountType.RETIREMENT
                rawType.contains("LOAN") -> AccountType.LOAN
                else -> AccountType.CHECKING
            }
        }

        val initialBalance = if (initBalIdx >= 0 && initBalIdx < row.size) row[initBalIdx].replace("$", "").replace(",", "").trim().toDoubleOrNull() ?: 0.0 else 0.0
        val currencyCode = if (currIdx >= 0 && currIdx < row.size) row[currIdx].trim().ifBlank { "USD" } else "USD"
        val creditLimit = if (limitIdx >= 0 && limitIdx < row.size && !row[limitIdx].lowercase().contains("n/a")) row[limitIdx].replace("$", "").replace(",", "").trim().toDoubleOrNull() else null
        val interestRateApr = if (aprIdx >= 0 && aprIdx < row.size && !row[aprIdx].lowercase().contains("n/a")) row[aprIdx].replace("%", "").replace(",", "").trim().toDoubleOrNull() else null
        val minimumPayment = if (minPayIdx >= 0 && minPayIdx < row.size && !row[minPayIdx].lowercase().contains("n/a")) row[minPayIdx].replace("$", "").replace(",", "").trim().toDoubleOrNull() else null
        val isDefault = if (defIdx >= 0 && defIdx < row.size) row[defIdx].lowercase().contains("yes") || row[defIdx].lowercase().contains("true") else false

        if (name.isBlank()) return null

        return AccountEntity(
            id = id,
            userId = userId,
            name = name,
            type = type,
            initialBalance = initialBalance,
            currencyCode = currencyCode,
            isDefault = isDefault,
            creditLimit = creditLimit,
            interestRateApr = interestRateApr,
            minimumPayment = minimumPayment
        )
    }

    private fun parseDateStringToMillis(dateStr: String): Long {
        if (dateStr.isBlank()) return System.currentTimeMillis()
        try {
            return dateTimeFormat.parse(dateStr)?.time ?: System.currentTimeMillis()
        } catch (_: Exception) {}
        try {
            return dateFormat.parse(dateStr)?.time ?: System.currentTimeMillis()
        } catch (_: Exception) {}
        try {
            val longVal = dateStr.toLongOrNull()
            if (longVal != null && longVal > 0) return longVal
        } catch (_: Exception) {}
        return System.currentTimeMillis()
    }

    // -------------------------------------------------------------
    // RAW CSV PARSER (RFC 4180 compliant)
    // -------------------------------------------------------------
    fun parseCsvRows(csvContent: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val currentRow = mutableListOf<String>()
        val currentField = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < csvContent.length) {
            val c = csvContent[i]
            when {
                c == '\"' -> {
                    if (inQuotes && i + 1 < csvContent.length && csvContent[i + 1] == '\"') {
                        currentField.append('\"')
                        i++ // Skip escaped quote
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == ',' && !inQuotes -> {
                    currentRow.add(currentField.toString().trim())
                    currentField.setLength(0)
                }
                (c == '\r' || c == '\n') && !inQuotes -> {
                    if (c == '\r' && i + 1 < csvContent.length && csvContent[i + 1] == '\n') {
                        i++ // CRLF
                    }
                    currentRow.add(currentField.toString().trim())
                    currentField.setLength(0)
                    if (currentRow.any { it.isNotBlank() }) {
                        rows.add(ArrayList(currentRow))
                    }
                    currentRow.clear()
                }
                else -> {
                    currentField.append(c)
                }
            }
            i++
        }

        if (currentField.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(currentField.toString().trim())
            if (currentRow.any { it.isNotBlank() }) {
                rows.add(currentRow)
            }
        }

        return rows
    }

    // -------------------------------------------------------------
    // DOM XML PARSING FOR XLSX (Works on both JVM and Android)
    // -------------------------------------------------------------
    private fun parseSharedStringsXml(bytes: ByteArray): List<String> {
        val strings = mutableListOf<String>()
        try {
            val dbFactory = DocumentBuilderFactory.newInstance()
            val dBuilder = dbFactory.newDocumentBuilder()
            val doc = dBuilder.parse(ByteArrayInputStream(bytes))
            doc.documentElement.normalize()

            val siList = doc.getElementsByTagName("si")
            for (i in 0 until siList.length) {
                val siNode = siList.item(i)
                if (siNode.nodeType == Node.ELEMENT_NODE) {
                    val siElem = siNode as Element
                    val tList = siElem.getElementsByTagName("t")
                    val sb = StringBuilder()
                    for (j in 0 until tList.length) {
                        sb.append(tList.item(j).textContent)
                    }
                    strings.add(sb.toString())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return strings
    }

    private fun parseWorkbookSheetNames(bytes: ByteArray): List<String> {
        val sheetNames = mutableListOf<String>()
        try {
            val dbFactory = DocumentBuilderFactory.newInstance()
            val dBuilder = dbFactory.newDocumentBuilder()
            val doc = dBuilder.parse(ByteArrayInputStream(bytes))
            doc.documentElement.normalize()

            val sheetList = doc.getElementsByTagName("sheet")
            for (i in 0 until sheetList.length) {
                val sheetNode = sheetList.item(i)
                if (sheetNode.nodeType == Node.ELEMENT_NODE) {
                    val sheetElem = sheetNode as Element
                    val name = sheetElem.getAttribute("name")
                    if (name.isNotBlank()) {
                        sheetNames.add(name)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return sheetNames
    }

    private fun parseSheetXml(bytes: ByteArray, sharedStrings: List<String>): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        try {
            val dbFactory = DocumentBuilderFactory.newInstance()
            val dBuilder = dbFactory.newDocumentBuilder()
            val doc = dBuilder.parse(ByteArrayInputStream(bytes))
            doc.documentElement.normalize()

            val rowList = doc.getElementsByTagName("row")
            for (i in 0 until rowList.length) {
                val rowNode = rowList.item(i)
                if (rowNode.nodeType == Node.ELEMENT_NODE) {
                    val rowElem = rowNode as Element
                    val cList = rowElem.getElementsByTagName("c")
                    val rowData = mutableListOf<String>()

                    for (j in 0 until cList.length) {
                        val cNode = cList.item(j)
                        if (cNode.nodeType == Node.ELEMENT_NODE) {
                            val cElem = cNode as Element
                            val tAttr = cElem.getAttribute("t")

                            var cellValue = ""
                            if (tAttr == "inlineStr") {
                                val isList = cElem.getElementsByTagName("is")
                                if (isList.length > 0) {
                                    val isElem = isList.item(0) as Element
                                    cellValue = isElem.textContent ?: ""
                                }
                            } else if (tAttr == "s") {
                                val vList = cElem.getElementsByTagName("v")
                                if (vList.length > 0) {
                                    val idx = vList.item(0).textContent?.toIntOrNull()
                                    if (idx != null && idx >= 0 && idx < sharedStrings.size) {
                                        cellValue = sharedStrings[idx]
                                    }
                                }
                            } else {
                                val vList = cElem.getElementsByTagName("v")
                                if (vList.length > 0) {
                                    cellValue = vList.item(0).textContent ?: ""
                                } else {
                                    cellValue = cElem.textContent ?: ""
                                }
                            }
                            rowData.add(cellValue.trim())
                        }
                    }

                    if (rowData.any { it.isNotBlank() }) {
                        rows.add(rowData)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return rows
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val idx = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) name = it.getString(idx)
                }
            }
        }
        return name ?: uri.lastPathSegment
    }
}
