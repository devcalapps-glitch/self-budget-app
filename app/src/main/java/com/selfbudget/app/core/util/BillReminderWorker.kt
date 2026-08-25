package com.selfbudget.app.core.util

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.selfbudget.app.data.local.AppDatabase
import com.selfbudget.app.data.model.TransactionType
import java.util.Calendar
import java.util.concurrent.TimeUnit

class BillReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getInstance(applicationContext)
            val recurringDao = db.recurringDao()
            val recurringBills = recurringDao.getAllRecurringSync()

            val now = System.currentTimeMillis()
            val calendar = Calendar.getInstance()

            // Set Today boundaries (00:00:00 to 23:59:59)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val todayStart = calendar.timeInMillis

            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            val todayEnd = calendar.timeInMillis

            // Set Tomorrow boundaries (00:00:00 to 23:59:59 - 1 day early!)
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            val tomorrowStart = calendar.timeInMillis

            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            val tomorrowEnd = calendar.timeInMillis

            var notifIdCounter = 1000

            recurringBills.forEach { item ->
                // Archived/paused items (including ones that finished their remainingOccurrences)
                // should never generate reminders.
                if (item.isArchived) return@forEach

                val dueTime = item.nextDueDate

                // NOTE: this worker only ever sends reminder notifications - it never inserts a
                // transaction on its own. An earlier version auto-posted a real transaction the
                // instant an item became due, using the recurring template's fixed amount with no
                // confirmation step. That silently created false/incorrect ledger entries whenever
                // the real bill amount differed, the payment didn't actually happen on schedule, or
                // the user also logged it manually (duplicate). Posting to the ledger now always
                // requires an explicit user action ("Post Now" in RecurringScreen).
                if (item.type == TransactionType.EXPENSE) {
                    // 1 Day Early Reminder (Due Tomorrow)
                    if (dueTime in tomorrowStart..tomorrowEnd) {
                        NotificationHelper.sendNotification(
                            context = applicationContext,
                            notificationId = notifIdCounter++,
                            title = "📆 Bill Due Tomorrow",
                            message = "${item.title} ($%.2f) is due tomorrow. Log or pay to stay on budget.".format(item.amount)
                        )
                    }
                    // Due Today Reminder
                    else if (dueTime in todayStart..todayEnd) {
                        NotificationHelper.sendNotification(
                            context = applicationContext,
                            notificationId = notifIdCounter++,
                            title = "🎯 Due Today",
                            message = "${item.title} ($%.2f) is due today! Log or pay entry.".format(item.amount)
                        )
                    }
                } else if (item.type == TransactionType.INCOME) {
                    // 1 Day Early Income / Paycheck Reminder (Expected Tomorrow)
                    if (dueTime in tomorrowStart..tomorrowEnd) {
                        NotificationHelper.sendNotification(
                            context = applicationContext,
                            notificationId = notifIdCounter++,
                            title = "🚀 Paycheck Tomorrow",
                            message = "${item.title} ($%.2f) expected tomorrow! Get ready to log your earnings.".format(item.amount)
                        )
                    }
                    // Income Arriving Today
                    else if (dueTime in todayStart..todayEnd) {
                        NotificationHelper.sendNotification(
                            context = applicationContext,
                            notificationId = notifIdCounter++,
                            title = "💳 Deposit Arrived!",
                            message = "${item.title} ($%.2f) arrived today! 1-tap to post to your checking account.".format(item.amount)
                        )
                    }
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "BillReminder8AmWorker"

        fun scheduleDaily8AmReminder(context: Context) {
            val calendar = Calendar.getInstance()
            val now = calendar.timeInMillis

            calendar.set(Calendar.HOUR_OF_DAY, 8)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            if (calendar.timeInMillis <= now) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            val initialDelay = calendar.timeInMillis - now

            val workRequest = PeriodicWorkRequestBuilder<BillReminderWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        }
    }
}
