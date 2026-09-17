package com.selfbudget.app.core.util

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import com.selfbudget.app.data.local.AppDatabase
import kotlinx.coroutines.flow.firstOrNull
import java.util.concurrent.TimeUnit

/**
 * Android WorkManager periodic worker that performs automated, non-intrusive cloud backup
 * of the user's database to their private Google Drive `appDataFolder`.
 *
 * Runs once every 24 hours under low-impact constraints:
 * - Device is connected to a network.
 * - Battery is not low.
 */
class GoogleDriveSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            // Check if user enabled auto-sync
            if (!GoogleDriveSyncManager.isAutoSyncEnabled(applicationContext)) {
                return Result.success()
            }

            // Check if a Google account is currently signed in
            val account = GoogleDriveSyncManager.getLastSignedInAccount(applicationContext)
                ?: return Result.success()

            // Verify the Drive appDataFolder OAuth scope is granted
            val hasPermission = GoogleSignIn.hasPermissions(
                account,
                Scope(DriveScopes.DRIVE_APPDATA)
            )
            if (!hasPermission) {
                return Result.success()
            }

            // Fetch current user from Room DB
            val db = AppDatabase.getInstance(applicationContext)
            val user = db.userDao().getCurrentUser().firstOrNull()
                ?: return Result.success()

            // Export local database to JSON snapshot
            val jsonPayload = CloudSyncManager.exportToJsonString(user.id, db)

            // Upload directly to user's Google Drive appDataFolder
            val uploadResult = GoogleDriveSyncManager.uploadToAppDataFolder(applicationContext, account, jsonPayload)

            if (uploadResult.isSuccess) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "GoogleDriveDailySyncWorker"

        /**
         * Enqueues or updates the 24-hour periodic backup job with non-intrusive constraints.
         */
        fun scheduleDailySync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<GoogleDriveSyncWorker>(24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }

        /**
         * Cancels the periodic background sync job.
         */
        fun cancelDailySync(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
