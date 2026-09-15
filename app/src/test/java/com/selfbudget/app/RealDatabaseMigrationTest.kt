package com.selfbudget.app

import com.selfbudget.app.data.local.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet

class RealDatabaseMigrationTest {

    private fun createJdbcSupportDatabase(conn: Connection): androidx.sqlite.db.SupportSQLiteDatabase {
        val handler = object : InvocationHandler {
            override fun invoke(proxy: Any?, method: Method, args: Array<out Any?>?): Any? {
                when (method.name) {
                    "execSQL" -> {
                        val sql = args?.get(0) as String
                        conn.createStatement().use { it.execute(sql) }
                        return null
                    }
                    "query" -> {
                        val sql = args?.get(0) as String
                        val bindArgs = if (args != null && args.size > 1) args[1] as? Array<*> else null

                        val stmt = conn.prepareStatement(sql)
                        if (bindArgs != null) {
                            for (i in bindArgs.indices) {
                                stmt.setObject(i + 1, bindArgs[i])
                            }
                        }
                        val rs = stmt.executeQuery()
                        return createJdbcCursorProxy(rs, stmt)
                    }
                    "isOpen" -> return !conn.isClosed
                    "close" -> {
                        conn.close()
                        return null
                    }
                    else -> throw UnsupportedOperationException("Method not mocked: ${method.name}")
                }
            }
        }

        return Proxy.newProxyInstance(
            androidx.sqlite.db.SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(androidx.sqlite.db.SupportSQLiteDatabase::class.java),
            handler
        ) as androidx.sqlite.db.SupportSQLiteDatabase
    }

    private fun createJdbcCursorProxy(rs: ResultSet, stmt: java.sql.Statement): android.database.Cursor {
        val md = rs.metaData
        val colCount = md.columnCount
        val colNames = (1..colCount).map { md.getColumnLabel(it) }

        val handler = object : InvocationHandler {
            override fun invoke(proxy: Any?, method: Method, args: Array<out Any?>?): Any? {
                when (method.name) {
                    "moveToFirst" -> return rs.next()
                    "moveToNext" -> return rs.next()
                    "getColumnIndex" -> {
                        val colName = args?.get(0) as String
                        val idx = colNames.indexOfFirst { it.equals(colName, ignoreCase = true) }
                        return idx
                    }
                    "getColumnIndexOrThrow" -> {
                        val colName = args?.get(0) as String
                        val idx = colNames.indexOfFirst { it.equals(colName, ignoreCase = true) }
                        if (idx == -1) throw IllegalArgumentException("Column $colName not found")
                        return idx
                    }
                    "getString" -> {
                        val colIndex = (args?.get(0) as Int) + 1
                        return rs.getString(colIndex)
                    }
                    "getInt" -> {
                        val colIndex = (args?.get(0) as Int) + 1
                        return rs.getInt(colIndex)
                    }
                    "getLong" -> {
                        val colIndex = (args?.get(0) as Int) + 1
                        return rs.getLong(colIndex)
                    }
                    "getDouble" -> {
                        val colIndex = (args?.get(0) as Int) + 1
                        return rs.getDouble(colIndex)
                    }
                    "isNull" -> {
                        val colIndex = (args?.get(0) as Int) + 1
                        rs.getObject(colIndex)
                        return rs.wasNull()
                    }
                    "close" -> {
                        rs.close()
                        stmt.close()
                        return null
                    }
                    else -> throw UnsupportedOperationException("Cursor method not mocked: ${method.name}")
                }
            }
        }

        return Proxy.newProxyInstance(
            android.database.Cursor::class.java.classLoader,
            arrayOf(android.database.Cursor::class.java),
            handler
        ) as android.database.Cursor
    }

    @Test
    fun testRealDatabaseMigration_fromVersion1To17() {
        val dbFile = File.createTempFile("test_real_db_v1_", ".db")
        dbFile.deleteOnExit()

        DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePath}").use { conn ->
            conn.createStatement().use { stmt ->
                // Create Schema v1 baseline
                stmt.execute("CREATE TABLE `users` (`id` TEXT NOT NULL PRIMARY KEY, `email` TEXT NOT NULL, `displayName` TEXT, `photoUrl` TEXT)")
                stmt.execute("CREATE TABLE `accounts` (`id` TEXT NOT NULL PRIMARY KEY, `userId` TEXT NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL, `initialBalance` REAL NOT NULL, `colorHex` TEXT NOT NULL, `iconName` TEXT NOT NULL, `isDefault` INTEGER NOT NULL)")
                stmt.execute("CREATE TABLE `categories` (`id` TEXT NOT NULL PRIMARY KEY, `name` TEXT NOT NULL, `iconName` TEXT NOT NULL, `colorHex` TEXT NOT NULL, `type` TEXT NOT NULL)")
                stmt.execute("CREATE TABLE `budgets` (`id` TEXT NOT NULL PRIMARY KEY, `userId` TEXT NOT NULL, `categoryId` TEXT NOT NULL, `amountLimit` REAL NOT NULL, `monthYear` TEXT NOT NULL)")
                stmt.execute("CREATE TABLE `recurring_transactions` (`id` TEXT NOT NULL PRIMARY KEY, `userId` TEXT NOT NULL, `title` TEXT NOT NULL, `amount` REAL NOT NULL, `type` TEXT NOT NULL, `categoryId` TEXT NOT NULL, `accountId` TEXT NOT NULL, `frequency` TEXT NOT NULL, `nextDueDate` INTEGER NOT NULL, `note` TEXT)")
                stmt.execute("CREATE TABLE `transactions` (`id` TEXT NOT NULL PRIMARY KEY, `userId` TEXT NOT NULL, `title` TEXT NOT NULL, `amount` REAL NOT NULL, `type` TEXT NOT NULL, `categoryId` TEXT NOT NULL, `accountId` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `note` TEXT)")

                // Insert representative financial data into v1 database
                stmt.execute("INSERT INTO `users` VALUES ('u1', 'alpha@selfbudget.app', 'Alpha User', 'https://avatar.png')")
                stmt.execute("INSERT INTO `accounts` VALUES ('acc_chk', 'u1', 'Main Checking', 'CHECKING', 1000.0, '#2563EB', 'AccountBalance', 1)")
                stmt.execute("INSERT INTO `accounts` VALUES ('acc_sav', 'u1', 'High Yield Savings', 'SAVINGS', 2000.0, '#0F766E', 'Savings', 0)")
                stmt.execute("INSERT INTO `accounts` VALUES ('acc_loan', 'u1', 'Auto Loan', 'LOAN', -15000.0, '#DC2626', 'DirectionsCar', 0)")
                stmt.execute("INSERT INTO `categories` VALUES ('cat_groceries', 'Groceries', 'ShoppingCart', '#059669', 'EXPENSE')")
                stmt.execute("INSERT INTO `budgets` VALUES ('b_groceries', 'u1', 'cat_groceries', 400.0, '2026-09')")
                stmt.execute("INSERT INTO `recurring_transactions` VALUES ('rec_rent', 'u1', 'Apartment Rent', 1200.0, 'EXPENSE', 'cat_groceries', 'acc_chk', 'MONTHLY', 1767182400000, 'Monthly rent')")
                stmt.execute("INSERT INTO `transactions` VALUES ('tx_trader_joes', 'u1', 'Trader Joes Groceries', 85.50, 'EXPENSE', 'cat_groceries', 'acc_chk', 1767182400000, 'Weekly essentials')")
            }

            val supportDb = createJdbcSupportDatabase(conn)

            // Run migration from v1 to v17
            val migration1To17 = AppDatabase.createCatchupMigration(1, 17)
            migration1To17.migrate(supportDb)

            // Verify table and column additions
            assertTrue(AppDatabase.tableExists(supportDb, "net_worth_snapshots"))
            assertTrue(AppDatabase.tableExists(supportDb, "exchange_rates"))
            assertTrue(AppDatabase.tableExists(supportDb, "goals"))

            assertTrue(AppDatabase.hasColumn(supportDb, "users", "hasCompletedOnboarding"))
            assertTrue(AppDatabase.hasColumn(supportDb, "users", "primaryGoal"))
            assertTrue(AppDatabase.hasColumn(supportDb, "users", "referralSource"))
            assertTrue(AppDatabase.hasColumn(supportDb, "users", "preferredCurrency"))
            assertTrue(AppDatabase.hasColumn(supportDb, "recurring_transactions", "remainingOccurrences"))
            assertTrue(AppDatabase.hasColumn(supportDb, "recurring_transactions", "transferAccountId"))
            assertTrue(AppDatabase.hasColumn(supportDb, "recurring_transactions", "isArchived"))
            assertTrue(AppDatabase.hasColumn(supportDb, "transactions", "linkedRecurringId"))
            assertTrue(AppDatabase.hasColumn(supportDb, "transactions", "recurringCycleDueDate"))
            assertTrue(AppDatabase.hasColumn(supportDb, "accounts", "loanTermMonths"))
            assertTrue(AppDatabase.hasColumn(supportDb, "accounts", "currencyCode"))
            assertTrue(AppDatabase.hasColumn(supportDb, "budgets", "isAutoSynced"))
            assertTrue(AppDatabase.hasColumn(supportDb, "budgets", "rolloverEnabled"))
            assertTrue(AppDatabase.hasColumn(supportDb, "categories", "isArchived"))
            assertTrue(AppDatabase.hasColumn(supportDb, "categories", "isDefault"))

            // Verify original financial records are 100% intact with exact precision
            conn.createStatement().use { stmt ->
                val rsUser = stmt.executeQuery("SELECT * FROM users WHERE id = 'u1'")
                assertTrue(rsUser.next())
                assertEquals("alpha@selfbudget.app", rsUser.getString("email"))
                assertEquals("Alpha User", rsUser.getString("displayName"))
                assertEquals(0, rsUser.getInt("hasCompletedOnboarding"))
                assertEquals("$", rsUser.getString("preferredCurrency"))
                assertEquals("SYSTEM", rsUser.getString("themeMode"))

                val rsAcc = stmt.executeQuery("SELECT * FROM accounts WHERE id = 'acc_loan'")
                assertTrue(rsAcc.next())
                assertEquals("Auto Loan", rsAcc.getString("name"))
                assertEquals(-15000.0, rsAcc.getDouble("initialBalance"), 0.001)
                assertEquals("USD", rsAcc.getString("currencyCode"))

                val rsTx = stmt.executeQuery("SELECT * FROM transactions WHERE id = 'tx_trader_joes'")
                assertTrue(rsTx.next())
                assertEquals(85.50, rsTx.getDouble("amount"), 0.001)
                assertEquals("Cash", rsTx.getString("paymentMethod"))

                val rsRec = stmt.executeQuery("SELECT * FROM recurring_transactions WHERE id = 'rec_rent'")
                assertTrue(rsRec.next())
                assertEquals(1200.0, rsRec.getDouble("amount"), 0.001)
                assertEquals(0, rsRec.getInt("isArchived"))
                assertEquals("Credit Card", rsRec.getString("paymentMethod"))

                val rsBudget = stmt.executeQuery("SELECT * FROM budgets WHERE id = 'b_groceries'")
                assertTrue(rsBudget.next())
                assertEquals(400.0, rsBudget.getDouble("amountLimit"), 0.001)
                assertEquals(1, rsBudget.getInt("isAutoSynced"))
                assertEquals(0, rsBudget.getInt("rolloverEnabled"))
            }

            // Test IDEMPOTENCY: Run migration again on the migrated database.
            // Confirm it does NOT fail because columns/tables/indices already exist.
            migration1To17.migrate(supportDb)
        }
    }

    @Test
    fun testRealDatabaseMigration_fromVersion12To17() {
        val dbFile = File.createTempFile("test_real_db_v12_", ".db")
        dbFile.deleteOnExit()

        DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePath}").use { conn ->
            conn.createStatement().use { stmt ->
                // Schema v12 has isAutoSynced, unique index on budgets, goals, snapshots, exchange_rates, but lacks v13 (categories.isArchived), v14 (recurring.transferAccountId), v15 (tx.linkedRecurringId), v16 (accounts.loanTermMonths), v17 (goals.monthlyTargetAmount)
                stmt.execute("CREATE TABLE `users` (`id` TEXT NOT NULL PRIMARY KEY, `email` TEXT NOT NULL, `displayName` TEXT, `photoUrl` TEXT, `preferredCurrency` TEXT NOT NULL, `themeMode` TEXT NOT NULL, `isBiometricEnabled` INTEGER NOT NULL, `hasCompletedOnboarding` INTEGER NOT NULL, `primaryGoal` TEXT, `referralSource` TEXT)")
                stmt.execute("CREATE TABLE `accounts` (`id` TEXT NOT NULL PRIMARY KEY, `userId` TEXT NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL, `initialBalance` REAL NOT NULL, `colorHex` TEXT NOT NULL, `iconName` TEXT NOT NULL, `isDefault` INTEGER NOT NULL, `currencyCode` TEXT NOT NULL, `creditLimit` REAL, `interestRateApr` REAL, `minimumPayment` REAL)")
                stmt.execute("CREATE TABLE `categories` (`id` TEXT NOT NULL PRIMARY KEY, `name` TEXT NOT NULL, `iconName` TEXT NOT NULL, `colorHex` TEXT NOT NULL, `type` TEXT NOT NULL, `isDefault` INTEGER NOT NULL)")
                stmt.execute("CREATE TABLE `budgets` (`id` TEXT NOT NULL PRIMARY KEY, `userId` TEXT NOT NULL, `categoryId` TEXT NOT NULL, `amountLimit` REAL NOT NULL, `monthYear` TEXT NOT NULL, `rolloverEnabled` INTEGER NOT NULL, `isAutoSynced` INTEGER NOT NULL)")
                stmt.execute("CREATE UNIQUE INDEX `index_budgets_userId_categoryId_monthYear` ON `budgets` (`userId`, `categoryId`, `monthYear`)")
                stmt.execute("CREATE TABLE `recurring_transactions` (`id` TEXT NOT NULL PRIMARY KEY, `userId` TEXT NOT NULL, `title` TEXT NOT NULL, `amount` REAL NOT NULL, `type` TEXT NOT NULL, `categoryId` TEXT NOT NULL, `accountId` TEXT NOT NULL, `frequency` TEXT NOT NULL, `nextDueDate` INTEGER NOT NULL, `note` TEXT, `paymentMethod` TEXT, `isArchived` INTEGER NOT NULL, `remainingOccurrences` INTEGER)")
                stmt.execute("CREATE TABLE `transactions` (`id` TEXT NOT NULL PRIMARY KEY, `userId` TEXT NOT NULL, `title` TEXT NOT NULL, `amount` REAL NOT NULL, `type` TEXT NOT NULL, `categoryId` TEXT NOT NULL, `accountId` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `note` TEXT, `paymentMethod` TEXT, `receiptImageUri` TEXT, `transferAccountId` TEXT)")
                stmt.execute("CREATE TABLE `goals` (`id` TEXT NOT NULL PRIMARY KEY, `userId` TEXT NOT NULL, `name` TEXT NOT NULL, `targetAmount` REAL NOT NULL, `targetDate` INTEGER, `colorHex` TEXT NOT NULL, `iconName` TEXT NOT NULL, `linkedAccountId` TEXT, `savedAmount` REAL NOT NULL, `createdAt` INTEGER NOT NULL)")
                stmt.execute("CREATE TABLE `net_worth_snapshots` (`id` TEXT NOT NULL PRIMARY KEY, `userId` TEXT NOT NULL, `monthYear` TEXT NOT NULL, `totalAssets` REAL NOT NULL, `totalLiabilities` REAL NOT NULL, `netWorth` REAL NOT NULL, `capturedAt` INTEGER NOT NULL)")
                stmt.execute("CREATE TABLE `exchange_rates` (`id` TEXT NOT NULL PRIMARY KEY, `userId` TEXT NOT NULL, `fromCurrency` TEXT NOT NULL, `toCurrency` TEXT NOT NULL, `rate` REAL NOT NULL, `updatedAt` INTEGER NOT NULL)")

                // Insert v12 data
                stmt.execute("INSERT INTO `goals` VALUES ('g_emergency', 'u1', 'Emergency Fund', 10000.0, 1800000000000, '#059669', 'Savings', 'acc_sav', 3500.0, 1767182400000)")
                stmt.execute("INSERT INTO `accounts` VALUES ('acc_mortgage', 'u1', 'Home Mortgage', 'MORTGAGE', -350000.0, '#DC2626', 'Home', 0, 'USD', NULL, 3.75, 1650.0)")
            }

            val supportDb = createJdbcSupportDatabase(conn)

            // Run migration from v12 to v17
            val migration12To17 = AppDatabase.createCatchupMigration(12, 17)
            migration12To17.migrate(supportDb)

            // Verify newly added columns
            assertTrue(AppDatabase.hasColumn(supportDb, "goals", "monthlyTargetAmount"))
            assertTrue(AppDatabase.hasColumn(supportDb, "accounts", "loanTermMonths"))
            assertTrue(AppDatabase.hasColumn(supportDb, "categories", "isArchived"))
            assertTrue(AppDatabase.hasColumn(supportDb, "recurring_transactions", "transferAccountId"))
            assertTrue(AppDatabase.hasColumn(supportDb, "transactions", "linkedRecurringId"))
            assertTrue(AppDatabase.hasColumn(supportDb, "transactions", "recurringCycleDueDate"))

            // Verify existing goal and account values were preserved
            conn.createStatement().use { stmt ->
                val rsGoal = stmt.executeQuery("SELECT * FROM goals WHERE id = 'g_emergency'")
                assertTrue(rsGoal.next())
                assertEquals(10000.0, rsGoal.getDouble("targetAmount"), 0.001)
                assertEquals(3500.0, rsGoal.getDouble("savedAmount"), 0.001)

                val rsAcc = stmt.executeQuery("SELECT * FROM accounts WHERE id = 'acc_mortgage'")
                assertTrue(rsAcc.next())
                assertEquals(-350000.0, rsAcc.getDouble("initialBalance"), 0.001)
                assertEquals(3.75, rsAcc.getDouble("interestRateApr"), 0.001)
            }
        }
    }

    @Test
    fun testRealDatabaseMigration_fromVersion16To17() {
        val dbFile = File.createTempFile("test_real_db_v16_", ".db")
        dbFile.deleteOnExit()

        DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePath}").use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute("CREATE TABLE `goals` (`id` TEXT NOT NULL PRIMARY KEY, `userId` TEXT NOT NULL, `name` TEXT NOT NULL, `targetAmount` REAL NOT NULL, `targetDate` INTEGER, `colorHex` TEXT NOT NULL, `iconName` TEXT NOT NULL, `linkedAccountId` TEXT, `savedAmount` REAL NOT NULL, `createdAt` INTEGER NOT NULL)")
                stmt.execute("INSERT INTO `goals` VALUES ('g_vacation', 'u1', 'Japan Trip', 5000.0, NULL, '#059669', 'Flight', NULL, 1200.0, 1767182400000)")
            }

            val supportDb = createJdbcSupportDatabase(conn)

            // Run migration 16 to 17
            val migration16To17 = AppDatabase.createCatchupMigration(16, 17)
            migration16To17.migrate(supportDb)

            assertTrue(AppDatabase.hasColumn(supportDb, "goals", "monthlyTargetAmount"))

            conn.createStatement().use { stmt ->
                val rs = stmt.executeQuery("SELECT * FROM goals WHERE id = 'g_vacation'")
                assertTrue(rs.next())
                assertEquals(5000.0, rs.getDouble("targetAmount"), 0.001)
                assertEquals(1200.0, rs.getDouble("savedAmount"), 0.001)
                rs.getObject("monthlyTargetAmount")
                assertTrue(rs.wasNull())
            }
        }
    }

    @Test
    fun testAllSupportedMigrationVersions_fromEachVersionTo17() {
        // Iterate through all supported versions (1..16) and verify migration to 17 on real SQLite databases
        for (v in 1..16) {
            val dbFile = File.createTempFile("test_real_db_v${v}_", ".db")
            dbFile.deleteOnExit()

            DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePath}").use { conn ->
                conn.createStatement().use { stmt ->
                    stmt.execute("CREATE TABLE `users` (`id` TEXT NOT NULL PRIMARY KEY, `email` TEXT NOT NULL, `displayName` TEXT, `photoUrl` TEXT)")
                    stmt.execute("CREATE TABLE `accounts` (`id` TEXT NOT NULL PRIMARY KEY, `userId` TEXT NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL, `initialBalance` REAL NOT NULL, `colorHex` TEXT NOT NULL, `iconName` TEXT NOT NULL, `isDefault` INTEGER NOT NULL)")
                    stmt.execute("INSERT INTO `users` VALUES ('u_${v}', 'user${v}@selfbudget.app', 'User $v', NULL)")
                    stmt.execute("INSERT INTO `accounts` VALUES ('acc_${v}', 'u_${v}', 'Account $v', 'CHECKING', ${v * 100.0}, '#2563EB', 'AccountBalance', 1)")
                }

                val supportDb = createJdbcSupportDatabase(conn)
                val migration = AppDatabase.createCatchupMigration(v, 17)
                assertEquals(v, migration.startVersion)
                assertEquals(17, migration.endVersion)

                // Execute migration
                migration.migrate(supportDb)

                // Assert migration outcome
                assertTrue(AppDatabase.hasColumn(supportDb, "users", "hasCompletedOnboarding"))
                assertTrue(AppDatabase.hasColumn(supportDb, "accounts", "currencyCode"))
                assertTrue(AppDatabase.tableExists(supportDb, "goals"))
                assertTrue(AppDatabase.tableExists(supportDb, "net_worth_snapshots"))
                assertTrue(AppDatabase.tableExists(supportDb, "exchange_rates"))

                conn.createStatement().use { stmt ->
                    val rs = stmt.executeQuery("SELECT * FROM accounts WHERE id = 'acc_${v}'")
                    assertTrue(rs.next())
                    assertEquals(v * 100.0, rs.getDouble("initialBalance"), 0.001)
                    assertEquals("USD", rs.getString("currencyCode"))
                }
            }
        }
    }

    @Test
    fun testFinancialCalculationsOnMigratedData() {
        val dbFile = File.createTempFile("test_calc_migrated_", ".db")
        dbFile.deleteOnExit()

        DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePath}").use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute("CREATE TABLE `users` (`id` TEXT NOT NULL PRIMARY KEY, `email` TEXT NOT NULL, `displayName` TEXT, `photoUrl` TEXT)")
                stmt.execute("CREATE TABLE `accounts` (`id` TEXT NOT NULL PRIMARY KEY, `userId` TEXT NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL, `initialBalance` REAL NOT NULL, `colorHex` TEXT NOT NULL, `iconName` TEXT NOT NULL, `isDefault` INTEGER NOT NULL)")

                // Insert Workflow 8 accounts into legacy schema
                stmt.execute("INSERT INTO `accounts` VALUES ('acc_chk', 'u1', 'Checking', 'CHECKING', 1000.0, '#2563EB', 'AccountBalance', 1)")
                stmt.execute("INSERT INTO `accounts` VALUES ('acc_sav', 'u1', 'Savings', 'SAVINGS', 2000.0, '#0F766E', 'Savings', 0)")
                stmt.execute("INSERT INTO `accounts` VALUES ('acc_cc', 'u1', 'Credit Card', 'CREDIT_CARD', 0.0, '#DC2626', 'CreditCard', 0)")
                stmt.execute("INSERT INTO `accounts` VALUES ('acc_loan', 'u1', 'Auto Loan', 'LOAN', -15000.0, '#DC2626', 'DirectionsCar', 0)")
                stmt.execute("INSERT INTO `accounts` VALUES ('acc_inv', 'u1', 'Brokerage', 'INVESTMENT', 5000.0, '#0891B2', 'TrendingUp', 0)")
            }

            val supportDb = createJdbcSupportDatabase(conn)
            AppDatabase.createCatchupMigration(1, 17).migrate(supportDb)

            // Read accounts back from the migrated real SQLite database
            val accountsList = mutableListOf<com.selfbudget.app.data.model.AccountEntity>()
            conn.createStatement().use { stmt ->
                val rs = stmt.executeQuery("SELECT * FROM accounts")
                while (rs.next()) {
                    accountsList.add(
                        com.selfbudget.app.data.model.AccountEntity(
                            id = rs.getString("id"),
                            userId = rs.getString("userId"),
                            name = rs.getString("name"),
                            type = com.selfbudget.app.data.model.AccountType.valueOf(rs.getString("type")),
                            initialBalance = rs.getDouble("initialBalance"),
                            colorHex = rs.getString("colorHex"),
                            iconName = rs.getString("iconName"),
                            isDefault = rs.getInt("isDefault") == 1,
                            currencyCode = rs.getString("currencyCode")
                        )
                    )
                }
            }

            assertEquals(5, accountsList.size)

            // Confirm Net Worth calculation on migrated accounts:
            // $1,000 + $2,000 + $0 - $15,000 + $5,000 = -$7,000.00
            val totalNetWorth = com.selfbudget.app.core.util.AccountBalanceCalculator.computeTotalInBaseCurrency(
                accounts = accountsList,
                allTransactions = emptyList(),
                baseCurrency = "USD",
                rates = emptyList()
            )
            assertEquals(-7000.0, totalNetWorth, 0.001)
        }
    }

    @Test
    fun testMigrationFailureRollbackPreservesDataWithoutLoss() {
        val dbFile = File.createTempFile("test_rollback_", ".db")
        dbFile.deleteOnExit()

        DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePath}").use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute("CREATE TABLE `accounts` (`id` TEXT NOT NULL PRIMARY KEY, `name` TEXT NOT NULL, `initialBalance` REAL NOT NULL)")
                stmt.execute("INSERT INTO `accounts` VALUES ('acc1', 'Untouched Checking', 5000.0)")
            }

            // Simulate transactional execution where a subsequent statement fails
            conn.autoCommit = false
            try {
                conn.createStatement().use { stmt ->
                    stmt.execute("ALTER TABLE `accounts` ADD COLUMN `newCol` TEXT")
                    // Force an intentional failure (syntax error)
                    stmt.execute("INVALID SQL STATEMENT THAT FAILS")
                }
                conn.commit()
            } catch (_: Exception) {
                conn.rollback()
            } finally {
                conn.autoCommit = true
            }

            // Verify original data is 100% intact after rollback
            conn.createStatement().use { stmt ->
                val rs = stmt.executeQuery("SELECT * FROM accounts WHERE id = 'acc1'")
                assertTrue(rs.next())
                assertEquals(5000.0, rs.getDouble("initialBalance"), 0.001)
                assertEquals("Untouched Checking", rs.getString("name"))
            }
        }
    }
}

