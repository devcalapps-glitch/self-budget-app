package com.selfbudget.app.core.util

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale

data class DriveSyncMetadata(
    val fileId: String,
    val modifiedTimeMillis: Long,
    val formattedTime: String
)

/**
 * Manages automated background sync with the user's personal Google Drive using the private appDataFolder scope.
 * 
 * Features:
 * 1. ZERO cost to app developer - uses the user's own Google account storage quota.
 * 2. High privacy: Files are saved in `appDataFolder`, a hidden private sandbox invisible to other apps and user drive view.
 * 3. Automatic upload on local Room DB mutations and on-demand cloud restore.
 */
object GoogleDriveSyncManager {

    private const val BACKUP_FILE_NAME = "self_budget_cloud_backup.json"
    private val SCOPES = Collections.singletonList(DriveScopes.DRIVE_APPDATA)

    fun getLastSignedInAccount(context: Context): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }

    fun getGoogleDriveSignInIntent(context: Context): Intent {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
            .requestEmail()
            .build()
        val client = GoogleSignIn.getClient(context, gso)
        return client.signInIntent
    }

    /**
     * Obtains a thread-safe Google Drive API service client for the current Google user account.
     */
    private fun getDriveService(context: Context, account: GoogleSignInAccount): Drive {
        val credential = GoogleAccountCredential.usingOAuth2(context, SCOPES)
            .setSelectedAccount(account.account)
        
        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("Self Budget")
            .build()
    }

    /**
     * Uploads or updates the `self_budget_cloud_backup.json` file inside the user's private Google Drive appDataFolder.
     */
    suspend fun uploadToAppDataFolder(
        context: Context,
        account: GoogleSignInAccount,
        jsonString: String
    ): Result<DriveSyncMetadata> = withContext(Dispatchers.IO) {
        try {
            val driveService = getDriveService(context, account)
            val existingFileId = findExistingBackupFileId(driveService)

            val mediaContent = ByteArrayContent("application/json", jsonString.toByteArray(Charsets.UTF_8))
            val nowMillis = System.currentTimeMillis()

            val fileId: String
            if (existingFileId != null) {
                // Update existing backup in appDataFolder
                val fileMetadata = com.google.api.services.drive.model.File()
                    .setName(BACKUP_FILE_NAME)
                val updatedFile = driveService.files().update(existingFileId, fileMetadata, mediaContent)
                    .setFields("id, modifiedTime")
                    .execute()
                fileId = updatedFile.id
            } else {
                // Create initial backup in appDataFolder
                val fileMetadata = com.google.api.services.drive.model.File()
                    .setName(BACKUP_FILE_NAME)
                    .setParents(Collections.singletonList("appDataFolder"))
                val createdFile = driveService.files().create(fileMetadata, mediaContent)
                    .setFields("id, modifiedTime")
                    .execute()
                fileId = createdFile.id
            }

            val sdf = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
            val formatted = sdf.format(Date(nowMillis))

            Result.success(DriveSyncMetadata(fileId, nowMillis, formatted))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Downloads the latest JSON backup string from the user's private Google Drive appDataFolder.
     */
    suspend fun downloadFromAppDataFolder(
        context: Context,
        account: GoogleSignInAccount
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val driveService = getDriveService(context, account)
            val fileId = findExistingBackupFileId(driveService)
                ?: return@withContext Result.failure(IllegalStateException("No cloud backup found in Google Drive appDataFolder"))

            val outputStream = ByteArrayOutputStream()
            driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream)
            val jsonString = outputStream.toString("UTF-8")

            Result.success(jsonString)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Queries Google Drive appDataFolder for existing metadata of `self_budget_cloud_backup.json`.
     */
    suspend fun getCloudMetadata(
        context: Context,
        account: GoogleSignInAccount
    ): Result<DriveSyncMetadata?> = withContext(Dispatchers.IO) {
        try {
            val driveService = getDriveService(context, account)
            val result = driveService.files().list()
                .setSpaces("appDataFolder")
                .setQ("name = '$BACKUP_FILE_NAME' and trashed = false")
                .setFields("files(id, name, modifiedTime)")
                .execute()

            val file = result.files.firstOrNull() ?: return@withContext Result.success(null)
            val modifiedTimeMillis = file.modifiedTime?.value ?: System.currentTimeMillis()
            val sdf = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
            val formatted = sdf.format(Date(modifiedTimeMillis))

            Result.success(DriveSyncMetadata(file.id, modifiedTimeMillis, formatted))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun findExistingBackupFileId(driveService: Drive): String? {
        val result = driveService.files().list()
            .setSpaces("appDataFolder")
            .setQ("name = '$BACKUP_FILE_NAME' and trashed = false")
            .setFields("files(id)")
            .execute()
        return result.files.firstOrNull()?.id
    }
}
