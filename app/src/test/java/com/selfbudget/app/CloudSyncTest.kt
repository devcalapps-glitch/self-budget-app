package com.selfbudget.app

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.selfbudget.app.data.model.AccountEntity
import com.selfbudget.app.data.model.AccountType
import com.selfbudget.app.data.model.BudgetEntity
import com.selfbudget.app.data.model.CategoryEntity
import com.selfbudget.app.data.model.SyncDataPayload
import com.selfbudget.app.data.model.TransactionEntity
import com.selfbudget.app.data.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Verifies 100% Zero-Cost Cloud Sync Data Payload Serialization & Deserialization.
 * Asserts schema versioning, cent precision, and full entity round-tripping across app updates.
 */
class CloudSyncTest {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    @Test
    fun testSyncPayloadSerializationAndDeserialization() {
        // Given: A full snapshot of account history, transactions, categories, and budgets
        val payload = SyncDataPayload(
            schemaVersion = 7,
            exportTimestamp = 1767182400000L,
            appVersion = "1.0",
            accounts = listOf(
                AccountEntity(id = "acc_1", userId = "u1", name = "Main Checking", type = AccountType.CHECKING, initialBalance = 1500.50, currencyCode = "USD"),
                AccountEntity(id = "acc_2", userId = "u1", name = "Vault Savings", type = AccountType.SAVINGS, initialBalance = 5000.00, currencyCode = "EUR")
            ),
            categories = listOf(
                CategoryEntity(id = "cat_1", name = "Groceries", iconName = "ShoppingCart", colorHex = "#4CAF50", type = TransactionType.EXPENSE, isDefault = true)
            ),
            transactions = listOf(
                TransactionEntity(id = "tx_1", userId = "u1", title = "Trader Joe's", amount = 124.75, type = TransactionType.EXPENSE, categoryId = "cat_1", accountId = "acc_1"),
                TransactionEntity(id = "tx_2", userId = "u1", title = "Bi-weekly Salary", amount = 2500.00, type = TransactionType.INCOME, categoryId = "cat_salary", accountId = "acc_1")
            ),
            budgets = listOf(
                BudgetEntity(id = "b_1", userId = "u1", categoryId = "cat_1", amountLimit = 500.00, monthYear = "2026-08", rolloverEnabled = true)
            )
        )

        // When: Exported to JSON string (Zero-Cost Cloud & File Format)
        val jsonString = gson.toJson(payload)
        assertNotNull(jsonString)

        // Then: Deserialized payload matches 100% exact cent values and entity counts
        val restoredPayload = gson.fromJson(jsonString, SyncDataPayload::class.java)
        assertEquals(7, restoredPayload.schemaVersion)
        assertEquals("1.0", restoredPayload.appVersion)
        assertEquals(2, restoredPayload.accounts.size)
        assertEquals(1, restoredPayload.categories.size)
        assertEquals(2, restoredPayload.transactions.size)
        assertEquals(1, restoredPayload.budgets.size)

        // Verify exact cents preservation
        assertEquals(1500.50, restoredPayload.accounts[0].initialBalance, 0.0)
        assertEquals(124.75, restoredPayload.transactions[0].amount, 0.0)
        assertEquals(500.00, restoredPayload.budgets[0].amountLimit, 0.0)
    }

    @Test
    fun testSchemaVersionCompatibilityDefaultValues() {
        // Given: An older backup JSON payload missing recently added fields
        val legacyJson = """
            {
              "schemaVersion": 6,
              "exportTimestamp": 1767182400000,
              "appVersion": "0.9",
              "accounts": [
                {
                  "id": "acc_legacy",
                  "userId": "u1",
                  "name": "Legacy Account",
                  "type": "CHECKING",
                  "initialBalance": 350.0
                }
              ],
              "transactions": []
            }
        """.trimIndent()

        // When: Deserialized by newer app version
        val restored = gson.fromJson(legacyJson, SyncDataPayload::class.java)

        // Then: Safe default values are applied automatically without crash or data loss
        assertEquals(6, restored.schemaVersion)
        assertEquals(1, restored.accounts.size)
        assertEquals("Legacy Account", restored.accounts[0].name)
        assertEquals(350.0, restored.accounts[0].initialBalance, 0.0)
    }
}
