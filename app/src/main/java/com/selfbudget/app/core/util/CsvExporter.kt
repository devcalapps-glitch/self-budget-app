package com.selfbudget.app.core.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.selfbudget.app.data.model.CategoryEntity
import com.selfbudget.app.data.model.TransactionEntity
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {

    fun exportAndShareTransactions(
        context: Context,
        transactions: List<TransactionEntity>,
        categories: List<CategoryEntity>
    ): Boolean {
        return try {
            val categoryMap = categories.associateBy { it.id }
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val fileDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

            val fileName = "SelfBudget_Export_${fileDateFormat.format(Date())}.csv"
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }

            val file = File(exportDir, fileName)
            val writer = FileWriter(file)

            // Write CSV Header
            writer.append("Transaction ID,Date,Title,Type,Amount ($),Category,Payment Method,Note\n")

            // Write Transaction Rows
            for (tx in transactions) {
                val categoryName = categoryMap[tx.categoryId]?.name ?: "General"
                val dateStr = dateFormat.format(Date(tx.timestamp))
                val titleEscaped = escapeCsvField(tx.title)
                val noteEscaped = escapeCsvField(tx.note ?: "")
                val paymentMethodEscaped = escapeCsvField(tx.paymentMethod ?: "Cash")

                writer.append("${tx.id},")
                writer.append("$dateStr,")
                writer.append("$titleEscaped,")
                writer.append("${tx.type.name},")
                writer.append("%.2f,".format(tx.amount))
                writer.append("$categoryName,")
                writer.append("$paymentMethodEscaped,")
                writer.append("$noteEscaped\n")
            }

            writer.flush()
            writer.close()

            // Launch Share Intent via FileProvider
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "Self Budget - Transactions Export")
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

    private fun escapeCsvField(field: String): String {
        return if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            "\"" + field.replace("\"", "\"\"") + "\""
        } else {
            field
        }
    }
}
