package com.selfbudget.app.core.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.selfbudget.app.data.model.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class ExcelSheetData(
    val name: String,
    val headers: List<String>,
    val rows: List<List<Any?>>
)

object ExcelExporter {

    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val fileTimestampFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    fun buildSheets(
        selectedTypes: Set<ExportDataType>,
        transactions: List<TransactionEntity>,
        categories: List<CategoryEntity>,
        accounts: List<AccountEntity>,
        recurring: List<RecurringTransactionEntity>,
        budgets: List<BudgetEntity>,
        goals: List<GoalEntity>,
        accountBalances: Map<String, Double> = emptyMap()
    ): List<ExcelSheetData> {
        val sheets = mutableListOf<ExcelSheetData>()
        val categoryMap = categories.associateBy { it.id }
        val accountMap = accounts.associateBy { it.id }

        if (selectedTypes.contains(ExportDataType.TRANSACTIONS)) {
            val headers = listOf(
                "Transaction ID", "Date & Time", "Title", "Type", "Amount ($)",
                "Category", "Account / Wallet", "Transfer Destination", "Payment Method", "Note"
            )
            val rows = transactions.map { tx ->
                val categoryName = categoryMap[tx.categoryId]?.name ?: "General"
                val accountName = accountMap[tx.accountId]?.name ?: tx.accountId
                val transferAccountName = tx.transferAccountId?.let { accountMap[it]?.name ?: it } ?: ""
                val dateStr = dateTimeFormat.format(Date(tx.timestamp))
                listOf(
                    tx.id,
                    dateStr,
                    tx.title,
                    tx.type.name,
                    tx.amount,
                    categoryName,
                    accountName,
                    transferAccountName,
                    tx.paymentMethod ?: "Cash",
                    tx.note ?: ""
                )
            }
            sheets.add(ExcelSheetData("Transactions", headers, rows))
        }

        if (selectedTypes.contains(ExportDataType.RECURRING)) {
            val headers = listOf(
                "Recurring ID", "Title", "Type", "Amount ($)", "Category",
                "Account / Wallet", "Frequency", "Next Due Date", "Remaining Occurrences",
                "Status", "Payment Method", "Note"
            )
            val rows = recurring.map { rec ->
                val categoryName = categoryMap[rec.categoryId]?.name ?: "General"
                val accountName = accountMap[rec.accountId]?.name ?: rec.accountId
                val nextDueDateStr = dateFormat.format(Date(rec.nextDueDate))
                val remainingStr = rec.remainingOccurrences?.toString() ?: "Indefinite"
                val statusStr = if (rec.isArchived) "Paused / Completed" else "Active"
                listOf(
                    rec.id,
                    rec.title,
                    rec.type.name,
                    rec.amount,
                    categoryName,
                    accountName,
                    rec.frequency.name,
                    nextDueDateStr,
                    remainingStr,
                    statusStr,
                    rec.paymentMethod ?: "Credit Card",
                    rec.note ?: ""
                )
            }
            sheets.add(ExcelSheetData("Recurring Transactions", headers, rows))
        }

        if (selectedTypes.contains(ExportDataType.BUDGET)) {
            val headers = listOf(
                "Budget ID", "Category", "Month / Period", "Budget Limit ($)",
                "Rollover Enabled", "Auto Synced"
            )
            val rows = budgets.map { b ->
                val categoryName = categoryMap[b.categoryId]?.name ?: b.categoryId
                val rolloverStr = if (b.rolloverEnabled) "Yes" else "No"
                val autoSyncedStr = if (b.isAutoSynced) "Yes" else "No"
                listOf(
                    b.id,
                    categoryName,
                    b.monthYear,
                    b.amountLimit,
                    rolloverStr,
                    autoSyncedStr
                )
            }
            sheets.add(ExcelSheetData("Budget Plan", headers, rows))
        }

        if (selectedTypes.contains(ExportDataType.GOALS)) {
            val headers = listOf(
                "Goal ID", "Goal Name", "Target Amount ($)", "Current Saved Amount ($)",
                "Linked Account / Wallet", "Target Date", "Created Date"
            )
            val rows = goals.map { g ->
                val linkedAccountName = g.linkedAccountId?.let { accountMap[it]?.name ?: it } ?: "None (Direct Savings)"
                val targetDateStr = g.targetDate?.let { dateFormat.format(Date(it)) } ?: "No deadline"
                val createdDateStr = dateFormat.format(Date(g.createdAt))
                listOf(
                    g.id,
                    g.name,
                    g.targetAmount,
                    g.savedAmount,
                    linkedAccountName,
                    targetDateStr,
                    createdDateStr
                )
            }
            sheets.add(ExcelSheetData("Savings Goals", headers, rows))
        }

        if (selectedTypes.contains(ExportDataType.ACCOUNTS)) {
            val headers = listOf(
                "Account ID", "Account Name", "Account Type", "Live Balance ($)",
                "Initial Balance ($)", "Currency", "Credit Limit ($)", "Interest Rate APR (%)",
                "Minimum Payment ($)", "Is Default"
            )
            val rows = accounts.map { acc ->
                val liveBalance = accountBalances[acc.id] ?: acc.initialBalance
                val creditLimitStr = acc.creditLimit?.let { "%.2f".format(Locale.US, it) } ?: "N/A"
                val aprStr = acc.interestRateApr?.let { "%.2f%%".format(Locale.US, it) } ?: "N/A"
                val minPayStr = acc.minimumPayment?.let { "%.2f".format(Locale.US, it) } ?: "N/A"
                val isDefaultStr = if (acc.isDefault) "Yes" else "No"
                listOf(
                    acc.id,
                    acc.name,
                    acc.type.name,
                    liveBalance,
                    acc.initialBalance,
                    acc.currencyCode,
                    creditLimitStr,
                    aprStr,
                    minPayStr,
                    isDefaultStr
                )
            }
            sheets.add(ExcelSheetData("Accounts & Wallets", headers, rows))
        }

        return sheets
    }

    /**
     * Generates a standard OpenXML SpreadsheetML (.xlsx) binary byte array containing
     * the provided sheets with separate tabs and formatted headers.
     */
    fun generateWorkbookBytes(sheets: List<ExcelSheetData>): ByteArray {
        val out = ByteArrayOutputStream()
        val zip = ZipOutputStream(out)

        // 1. [Content_Types].xml
        zip.putNextEntry(ZipEntry("[Content_Types].xml"))
        val contentTypes = buildString {
            append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
            append("""<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""")
            append("""<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""")
            append("""<Default Extension="xml" ContentType="application/xml"/>""")
            append("""<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>""")
            append("""<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>""")
            sheets.indices.forEach { index ->
                append("""<Override PartName="/xl/worksheets/sheet${index + 1}.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>""")
            }
            append("""</Types>""")
        }
        zip.write(contentTypes.toByteArray(Charsets.UTF_8))
        zip.closeEntry()

        // 2. _rels/.rels
        zip.putNextEntry(ZipEntry("_rels/.rels"))
        val rootRels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""
        zip.write(rootRels.toByteArray(Charsets.UTF_8))
        zip.closeEntry()

        // 3. xl/_rels/workbook.xml.rels
        zip.putNextEntry(ZipEntry("xl/_rels/workbook.xml.rels"))
        val wbRels = buildString {
            append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
            append("""<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""")
            sheets.indices.forEach { index ->
                append("""<Relationship Id="rId${index + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet${index + 1}.xml"/>""")
            }
            append("""<Relationship Id="rIdStyles" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>""")
            append("""</Relationships>""")
        }
        zip.write(wbRels.toByteArray(Charsets.UTF_8))
        zip.closeEntry()

        // 4. xl/styles.xml (Header font + normal font)
        zip.putNextEntry(ZipEntry("xl/styles.xml"))
        val stylesXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="2">
    <font><name val="Calibri"/><sz val="11"/></font>
    <font><b/><name val="Calibri"/><sz val="11"/></font>
  </fonts>
  <fills count="2">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
  </fills>
  <borders count="1">
    <border><left/><right/><top/><bottom/><diagonal/></border>
  </borders>
  <cellStyleXfs count="1">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
  </cellStyleXfs>
  <cellXfs count="2">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
    <xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/>
  </cellXfs>
</styleSheet>"""
        zip.write(stylesXml.toByteArray(Charsets.UTF_8))
        zip.closeEntry()

        // 5. xl/workbook.xml
        zip.putNextEntry(ZipEntry("xl/workbook.xml"))
        val workbookXml = buildString {
            append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
            append("""<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">""")
            append("""<sheets>""")
            sheets.forEachIndexed { index, sheet ->
                val sheetNameEscaped = escapeXml(sheet.name)
                append("""<sheet name="$sheetNameEscaped" sheetId="${index + 1}" r:id="rId${index + 1}"/>""")
            }
            append("""</sheets>""")
            append("""</workbook>""")
        }
        zip.write(workbookXml.toByteArray(Charsets.UTF_8))
        zip.closeEntry()

        // 6. xl/worksheets/sheetN.xml for each sheet
        sheets.forEachIndexed { sheetIndex, sheet ->
            zip.putNextEntry(ZipEntry("xl/worksheets/sheet${sheetIndex + 1}.xml"))
            val sheetXml = buildSheetXml(sheet)
            zip.write(sheetXml.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }

        zip.finish()
        zip.close()
        return out.toByteArray()
    }

    private fun buildSheetXml(sheet: ExcelSheetData): String {
        return buildString {
            append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
            append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
            append("""<sheetData>""")

            // Header Row (r="1", style s="1" for Bold)
            append("""<row r="1">""")
            sheet.headers.forEachIndexed { colIndex, header ->
                val cellRef = "${toColumnName(colIndex)}1"
                append("""<c r="$cellRef" t="inlineStr" s="1"><is><t>${escapeXml(header)}</t></is></c>""")
            }
            append("""</row>""")

            // Data Rows (r="2", "3", ...)
            sheet.rows.forEachIndexed { rowIndex, row ->
                val r = rowIndex + 2
                append("""<row r="$r">""")
                row.forEachIndexed { colIndex, value ->
                    val cellRef = "${toColumnName(colIndex)}$r"
                    if (value == null) {
                        // Empty cell
                    } else if (value is Number) {
                        append("""<c r="$cellRef"><v>$value</v></c>""")
                    } else {
                        val text = value.toString()
                        append("""<c r="$cellRef" t="inlineStr"><is><t>${escapeXml(text)}</t></is></c>""")
                    }
                }
                append("""</row>""")
            }

            append("""</sheetData>""")
            append("""</worksheet>""")
        }
    }

    fun exportAndShareExcel(
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
            val sheets = buildSheets(
                selectedTypes = selectedTypes,
                transactions = transactions,
                categories = categories,
                accounts = accounts,
                recurring = recurring,
                budgets = budgets,
                goals = goals,
                accountBalances = accountBalances
            )

            if (sheets.isEmpty()) return false

            val xlsxBytes = generateWorkbookBytes(sheets)
            val timestamp = fileTimestampFormat.format(Date())
            val fileName = "SelfBudget_Export_$timestamp.xlsx"

            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }

            val file = File(exportDir, fileName)
            val fos = FileOutputStream(file)
            fos.write(xlsxBytes)
            fos.flush()
            fos.close()

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                putExtra(Intent.EXTRA_SUBJECT, "Self Budget - Multi-Tab Excel Export")
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Share or Save Excel Spreadsheet (.xlsx)"))
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun toColumnName(index: Int): String {
        var n = index
        val sb = StringBuilder()
        while (n >= 0) {
            sb.append(('A'.code + (n % 26)).toChar())
            n = (n / 26) - 1
        }
        return sb.reverse().toString()
    }

    fun escapeXml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
