package com.selfbudget.app.feature.profile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import com.selfbudget.app.core.ui.DataExportModal
import com.selfbudget.app.core.ui.DataImportPreviewModal
import com.selfbudget.app.core.util.CloudSyncManager
import com.selfbudget.app.core.util.CsvExporter
import com.selfbudget.app.core.util.Currencies
import com.selfbudget.app.core.util.DataImporter
import com.selfbudget.app.core.util.GoogleDriveSyncManager
import com.selfbudget.app.core.util.NotificationHelper
import com.selfbudget.app.core.util.ParsedImportData
import com.selfbudget.app.data.local.AppDatabase
import com.selfbudget.app.data.model.AccountEntity
import com.selfbudget.app.data.model.AppThemeMode
import com.selfbudget.app.data.model.BudgetEntity
import com.selfbudget.app.data.model.CategoryEntity
import com.selfbudget.app.data.model.ExchangeRateEntity
import com.selfbudget.app.data.model.GoalEntity
import com.selfbudget.app.data.model.RecurringTransactionEntity
import com.selfbudget.app.data.model.TransactionEntity
import com.selfbudget.app.data.model.UserEntity
import com.selfbudget.app.ui.theme.ExpenseRed
import kotlinx.coroutines.launch

private enum class SettingsSubScreen(val title: String) {
    MAIN("Settings"),
    PREFERENCES("General Preferences & Security"),
    BACKUP("Data & Account Management"),
    LEGAL("Legal & Privacy"),
    PRIVACY("Privacy Policy"),
    TERMS("Terms of Service"),
    ABOUT("About Developer & Support"),
    DELETE_ACCOUNT("Delete Account & Data")
}

@Composable
fun SettingsScreen(
    user: UserEntity?,
    currencySymbol: String,
    themeMode: AppThemeMode,
    isBiometricEnabled: Boolean,
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    accounts: List<AccountEntity> = emptyList(),
    recurringList: List<RecurringTransactionEntity> = emptyList(),
    budgets: List<BudgetEntity> = emptyList(),
    goals: List<GoalEntity> = emptyList(),
    accountBalances: Map<String, Double> = emptyMap(),
    exchangeRates: List<ExchangeRateEntity> = emptyList(),
    onSetCurrency: (String) -> Unit,
    onSetThemeMode: (AppThemeMode) -> Unit,
    onSetBiometricEnabled: (Boolean) -> Unit,
    onSetExchangeRate: (fromCurrency: String, toCurrency: String, rate: Double) -> Unit = { _, _, _ -> },
    onExportBackupJson: ((String) -> Unit, (String) -> Unit) -> Unit = { _, _ -> },
    onRestoreBackupJson: (jsonString: String, onSuccess: (Int) -> Unit, onError: (String) -> Unit) -> Unit = { _, _, _ -> },
    onImportData: ((ParsedImportData, (Int) -> Unit, (String) -> Unit) -> Unit)? = null,
    onDriveSyncClick: (account: GoogleSignInAccount, onResult: (String) -> Unit) -> Unit = { _, _ -> },
    onDriveRestoreClick: (account: GoogleSignInAccount, onResult: (String) -> Unit) -> Unit = { _, _ -> },
    onResetData: () -> Unit = {},
    onResetTransactionsOnly: () -> Unit = {},
    onToggleCategoryArchive: ((CategoryEntity) -> Unit)? = null,
    onSignOut: () -> Unit,
    onDismiss: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currencies = listOf("$", "€", "£", "₹", "¥", "A$")
    val scrollState = rememberScrollState()
    
    var activeSubScreen by remember { mutableStateOf(SettingsSubScreen.MAIN) }
    var showManageCategoriesModal by remember { mutableStateOf(false) }
    var showDataExportModal by remember { mutableStateOf(false) }
    var pendingImportData by remember { mutableStateOf<ParsedImportData?>(null) }
    var showResetConfirmation by remember { mutableStateOf(false) }
    var showResetActivityConfirmation by remember { mutableStateOf(false) }
    var showAccountDeletionDialog by remember { mutableStateOf(false) }
    var syncStatusMessage by remember { mutableStateOf<String?>(null) }
    var pendingDriveAction by remember { mutableStateOf<((GoogleSignInAccount) -> Unit)?>(null) }

    // Intercept back presses when on a sub-screen
    BackHandler(enabled = activeSubScreen != SettingsSubScreen.MAIN) {
        activeSubScreen = when (activeSubScreen) {
            SettingsSubScreen.PRIVACY, SettingsSubScreen.TERMS -> SettingsSubScreen.LEGAL
            SettingsSubScreen.DELETE_ACCOUNT -> SettingsSubScreen.BACKUP
            else -> SettingsSubScreen.MAIN
        }
    }

    // Scroll to top when changing sub-screens
    LaunchedEffect(activeSubScreen) {
        scrollState.animateScrollTo(0)
    }

    val googleDriveSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
            if (account != null) {
                pendingDriveAction?.invoke(account)
            } else {
                syncStatusMessage = "❌ Google Drive Sign-In cancelled."
            }
        } catch (e: com.google.android.gms.common.api.ApiException) {
            val detail = when (e.statusCode) {
                10 -> "OAuth Developer Configuration Error (Check SHA-1 in Google Cloud Console)"
                12500 -> "Sign-in cancelled or Play Services error"
                4 -> "Sign-in required"
                else -> e.localizedMessage ?: "Status Code ${e.statusCode}"
            }
            syncStatusMessage = "❌ Google Drive authorization failed: $detail"
        } catch (e: Exception) {
            syncStatusMessage = "❌ Google Drive authorization failed: ${e.localizedMessage}"
        } finally {
            pendingDriveAction = null
        }
    }

    fun requestDriveAccessAndExecute(action: (GoogleSignInAccount) -> Unit) {
        val currentAccount = GoogleDriveSyncManager.getLastSignedInAccount(context)
        val isFailedState = syncStatusMessage?.contains("expired", ignoreCase = true) == true ||
                            syncStatusMessage?.contains("failed", ignoreCase = true) == true ||
                            syncStatusMessage?.contains("Error", ignoreCase = true) == true

        val hasPermission = !isFailedState && currentAccount != null && GoogleSignIn.hasPermissions(
            currentAccount,
            Scope(DriveScopes.DRIVE_APPDATA)
        )

        if (hasPermission && currentAccount != null) {
            action(currentAccount)
        } else {
            pendingDriveAction = action
            val intent = GoogleDriveSyncManager.getGoogleDriveSignInIntent(context)
            googleDriveSignInLauncher.launch(intent)
        }
    }

    val dataImportPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val parseResult = DataImporter.parseFromUri(context, uri, user?.id ?: "local_user")
                    parseResult.onSuccess { data ->
                        if (data.totalCount > 0) {
                            pendingImportData = data
                        } else {
                            syncStatusMessage = "⚠️ No valid financial records could be parsed from ${data.fileName}."
                        }
                    }.onFailure { error ->
                        syncStatusMessage = "❌ Could not parse file: ${error.localizedMessage}"
                    }
                } catch (e: Exception) {
                    syncStatusMessage = "❌ Could not read file: ${e.localizedMessage}"
                }
            }
        }
    }

    val jsonRestorePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val jsonString = CloudSyncManager.readJsonFromUri(context, uri)
                    onRestoreBackupJson(jsonString, { count ->
                        syncStatusMessage = "✅ Restored $count items successfully!"
                    }, { error ->
                        syncStatusMessage = "❌ Restore failed: $error"
                    })
                } catch (e: Exception) {
                    syncStatusMessage = "❌ Could not read file: ${e.localizedMessage}"
                }
            }
        }
    }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Unified Top Navigation Header Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (activeSubScreen != SettingsSubScreen.MAIN) {
                        IconButton(
                            onClick = {
                                activeSubScreen = when (activeSubScreen) {
                                    SettingsSubScreen.PRIVACY, SettingsSubScreen.TERMS -> SettingsSubScreen.LEGAL
                                    SettingsSubScreen.DELETE_ACCOUNT -> SettingsSubScreen.BACKUP
                                    else -> SettingsSubScreen.MAIN
                                }
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else if (onDismiss != null) {
                        IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Settings",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = activeSubScreen.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (onDismiss != null) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "Done",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

        // --- MAIN CATEGORY MENU PAGE ---
        if (activeSubScreen == SettingsSubScreen.MAIN) {
            // 1. Centered User Profile Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val initialLetter = (user?.displayName?.firstOrNull { it.isLetterOrDigit() }
                        ?: user?.email?.firstOrNull { it.isLetterOrDigit() }
                        ?: 'U').uppercaseChar().toString()

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        modifier = Modifier.size(58.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = initialLetter,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = user?.displayName ?: "Google User",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verified Google Account",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (!user?.email.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = user?.email ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // 5 Distinct Categorized Menu Rows
            SettingsCategoryRow(
                icon = Icons.Default.Settings,
                title = "General Preferences & Security",
                subtitle = "Theme (${themeMode.name.lowercase()}), Currency ($currencySymbol), Biometrics & Alerts",
                onClick = { activeSubScreen = SettingsSubScreen.PREFERENCES }
            )

            SettingsCategoryRow(
                icon = Icons.Default.Category,
                title = "Manage Custom Categories",
                subtitle = "View, archive, or restore custom categories",
                onClick = { showManageCategoriesModal = true }
            )

            SettingsCategoryRow(
                icon = Icons.Default.CloudDone,
                title = "Data & Account Management",
                subtitle = "Google Drive sync, JSON/CSV backup, data reset & account deletion",
                onClick = { activeSubScreen = SettingsSubScreen.BACKUP }
            )

            SettingsCategoryRow(
                icon = Icons.Default.Gavel,
                title = "Legal & Privacy",
                subtitle = "Privacy Policy & Terms of Service",
                onClick = { activeSubScreen = SettingsSubScreen.LEGAL }
            )

            SettingsCategoryRow(
                icon = Icons.Default.Info,
                title = "About Developer & Support",
                subtitle = "Version 1.0.0, dev contact email",
                onClick = { activeSubScreen = SettingsSubScreen.ABOUT }
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log Out", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(150.dp))
        }

        // --- SUB-SCREEN 1: GENERAL PREFERENCES & SECURITY ---
        if (activeSubScreen == SettingsSubScreen.PREFERENCES) {
            // Theme Mode Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DarkMode,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "App Theme / Appearance",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = themeMode == AppThemeMode.SYSTEM,
                            onClick = { onSetThemeMode(AppThemeMode.SYSTEM) },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.SettingsSuggest, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("System")
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = themeMode == AppThemeMode.LIGHT,
                            onClick = { onSetThemeMode(AppThemeMode.LIGHT) },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LightMode, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Light")
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = themeMode == AppThemeMode.DARK,
                            onClick = { onSetThemeMode(AppThemeMode.DARK) },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.DarkMode, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Dark")
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Currency Symbol Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AttachMoney,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Preferred Currency Symbol",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        currencies.forEach { symbol ->
                            FilterChip(
                                selected = currencySymbol == symbol,
                                onClick = { onSetCurrency(symbol) },
                                label = { Text(symbol, fontWeight = FontWeight.Bold) }
                            )
                        }
                    }
                }
            }

            // Exchange Rates Card
            val foreignCurrencyCodes = remember(accounts, currencySymbol) {
                val base = Currencies.codeForSymbol(currencySymbol)
                accounts.map { it.currencyCode }.distinct().filter { !it.equals(base, ignoreCase = true) }
            }
            if (foreignCurrencyCodes.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AttachMoney,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Exchange Rates",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Self Budget is offline-first, so rates aren't fetched automatically — enter them yourself to include foreign-currency accounts in your net worth total.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        val baseCode = Currencies.codeForSymbol(currencySymbol)
                        foreignCurrencyCodes.forEach { foreignCode ->
                            val existingRate = exchangeRates.firstOrNull {
                                it.fromCurrency.equals(foreignCode, ignoreCase = true) && it.toCurrency.equals(baseCode, ignoreCase = true)
                            }
                            var rateText by remember(foreignCode, existingRate?.rate) {
                                mutableStateOf(existingRate?.rate?.toString() ?: "")
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "1 ${Currencies.symbolFor(foreignCode)} $foreignCode =",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = rateText,
                                    onValueChange = { input ->
                                        if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,6}$"))) {
                                            rateText = input
                                            input.toDoubleOrNull()?.let { rate ->
                                                if (rate > 0.0) onSetExchangeRate(foreignCode, baseCode, rate)
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    label = { Text(baseCode) },
                                    modifier = Modifier.width(120.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Security & App Lock Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Security & App Lock",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Biometric lock on launch",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                            )
                        }
                    }

                    Switch(
                        checked = isBiometricEnabled,
                        onCheckedChange = onSetBiometricEnabled
                    )
                }
            }

            // Push Notifications & Alerts Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Push Notifications & Alerts",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (hasNotificationPermission) "Daily bill & budget alerts active" else "Tap to enable local notifications",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                                )
                            }
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                            ) {
                                Text("Enable")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                NotificationHelper.sendNotification(
                                    context,
                                    777,
                                    "🔔 Test Notification Received",
                                    "Your local Push Notifications and Bill Reminders are working perfectly!"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send Test Notification 🔔")
                    }
                }
            }

            Spacer(modifier = Modifier.height(150.dp))
        }

        // --- SUB-SCREEN 2: DATA, BACKUP & CLOUD SYNC ---
        if (activeSubScreen == SettingsSubScreen.BACKUP) {
            // SECTION 1: Automated Google Drive Cloud Sync
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Automated Google Drive Cloud Sync",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Zero-cost background sync to your personal Google Drive appDataFolder. Data is stored privately inside your Google Account.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (syncStatusMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = syncStatusMessage!!,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                requestDriveAccessAndExecute { account ->
                                    onDriveSyncClick(account) { message ->
                                        syncStatusMessage = message
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Drive Sync", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                requestDriveAccessAndExecute { account ->
                                    onDriveRestoreClick(account) { message ->
                                        syncStatusMessage = message
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Drive Restore", fontSize = 12.sp)
                        }
                    }
                }
            }

            // SECTION 2: Manual Backup & File Export
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Manual Backup & Export",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Export or restore your full account history using JSON, or selectively export transactions, recurring bills, budget plans, savings goals, and accounts to multi-tab Excel (.xlsx) or CSV.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showDataExportModal = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Export Data", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = { dataImportPickerLauncher.launch("*/*") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(imageVector = Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Import Data...", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onExportBackupJson({ jsonString ->
                                    coroutineScope.launch {
                                        try {
                                            val backupFile = CloudSyncManager.saveBackupToLocalFile(context, jsonString)
                                            val fileUri = androidx.core.content.FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.fileprovider",
                                                backupFile
                                            )
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "application/json"
                                                putExtra(Intent.EXTRA_STREAM, fileUri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Backup Self Budget Data (Google Drive / Local Storage)"))
                                            syncStatusMessage = "✅ Exported backup JSON successfully!"
                                        } catch (e: Exception) {
                                            syncStatusMessage = "❌ Export failed: ${e.localizedMessage}"
                                        }
                                    }
                                }, { error ->
                                    syncStatusMessage = "❌ Export failed: $error"
                                })
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Backup JSON", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                dataImportPickerLauncher.launch("*/*")
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Restore JSON", fontSize = 11.sp)
                        }
                    }
                }
            }

            // SECTION 3: Clean Sweep / Reset Data Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = ExpenseRed.copy(alpha = 0.08f)
                ),
                border = BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CleaningServices,
                            contentDescription = null,
                            tint = ExpenseRed
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Clean Sweep / Reset Data",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = ExpenseRed
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Reset your transaction ledger only, or wipe all app records and start completely fresh.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Option 1: Reset Activity Only (Transactions)
                    OutlinedButton(
                        onClick = { showResetActivityConfirmation = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Reset Activity Only (Transactions)", fontWeight = FontWeight.Bold)
                    }

                    Text(
                        text = "• Wipes transaction history only\n• Keeps all Accounts, Wallets, Budgets, Recurring rules, and Goals intact",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 12.dp)
                    )

                    // Option 2: Reset All App Data
                    Button(
                        onClick = { showResetConfirmation = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ExpenseRed,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Reset All App Data", fontWeight = FontWeight.Bold)
                    }

                    Text(
                        text = "• Permanently deletes all records (transactions, accounts, budgets, goals, recurring)",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }
            }

            // SECTION 4: Delete Account & Cloud Erasure Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                border = BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PersonRemove,
                            contentDescription = null,
                            tint = ExpenseRed
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Delete Account & User Data",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = ExpenseRed
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Request cloud data deletion or permanently wipe on-device data & sign out.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { activeSubScreen = SettingsSubScreen.DELETE_ACCOUNT },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.4f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = ExpenseRed.copy(alpha = 0.06f),
                            contentColor = ExpenseRed
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete Account Options", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(150.dp))
        }

        // --- SUB-SCREEN 3: LEGAL & PRIVACY (CATEGORY PAGE WITH PRIVACY POLICY & TERMS OF SERVICE) ---
        if (activeSubScreen == SettingsSubScreen.LEGAL) {
            SettingsCategoryRow(
                icon = Icons.Default.PrivacyTip,
                title = "Privacy Policy",
                subtitle = "Read our 100% offline & zero-data collection policy",
                onClick = { activeSubScreen = SettingsSubScreen.PRIVACY }
            )

            Spacer(modifier = Modifier.height(8.dp))

            SettingsCategoryRow(
                icon = Icons.Default.Description,
                title = "Terms of Service",
                subtitle = "Read app usage terms, personal license & disclaimer",
                onClick = { activeSubScreen = SettingsSubScreen.TERMS }
            )
        }

        // --- SUB-SCREEN 4: PRIVACY POLICY PAGE ---
        if (activeSubScreen == SettingsSubScreen.PRIVACY) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PrivacyTip,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Privacy & Data Protection",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Last Updated: August 25, 2026",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "1. Zero Server Data Collection",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Self Budget operates completely offline and has no external tracking servers or third-party telemetry. We do not collect, transmit, sell, or rent your personal or financial data.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "2. On-Device Local Storage",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "All accounts, transactions, budgets, goals, and recurring items are stored locally in an encrypted Room SQLite database on your device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "3. Google Drive Private Sandbox",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Automated Cloud Sync uses the private appDataFolder scope. Backups are stored in your own Google Account. Third parties and developers have zero access to your backup files.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "4. Biometric Protection",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Fingerprint and Face Unlock authentication uses native Android BiometricPrompt hardware security (TEE). Biometric data never leaves your device's secure enclave.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://self-budget-app.netlify.app#privacy"))
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // fallback
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View Online Privacy Policy")
                    }
                }
            }
        }

        // --- SUB-SCREEN 5: TERMS OF SERVICE PAGE ---
        if (activeSubScreen == SettingsSubScreen.TERMS) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Terms of Service",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Last Updated: August 25, 2026",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "1. Acceptance of Terms",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "By installing or using Self Budget provided by DevCalApps, you agree to be bound by these Terms of Service.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "2. License & Personal Use",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "DevCalApps grants you a personal, non-exclusive license to use Self Budget strictly for personal financial and budget management.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "3. Offline Data Control & Responsibility",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Because Self Budget is an offline-first app, you are responsible for maintaining local backups. DevCalApps is not liable for data loss caused by device damage, unbacked resets, or forgotten passcodes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "4. Financial Advice Disclaimer",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Self Budget is a personal expense logging tool. The app does not provide accounting, tax, or legal financial advice.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://self-budget-app.netlify.app#terms"))
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // fallback
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View Online Terms of Service")
                    }
                }
            }
        }

        // --- SUB-SCREEN 6: ABOUT DEVELOPER & SUPPORT ---
        if (activeSubScreen == SettingsSubScreen.ABOUT) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "About Developer & Support",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Self Budget v1.0.0",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Offline-first & private local storage architecture.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                            .clickable {
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:dev.cal.apps@gmail.com")
                                    putExtra(Intent.EXTRA_SUBJECT, "Self Budget App Support & Feedback")
                                }
                                try {
                                    context.startActivity(Intent.createChooser(intent, "Contact Developer"))
                                } catch (e: Exception) {
                                    // fallback
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Developer Support & Feedback",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "dev.cal.apps@gmail.com ✉️",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // --- SUB-SCREEN 7: DELETE ACCOUNT & DATA FULL PAGE ---
        if (activeSubScreen == SettingsSubScreen.DELETE_ACCOUNT) {
            // Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PersonRemove,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Delete Account & User Data",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Data deletion options & Play Policy compliance",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Section 1: Cloud & Web Account Data Erasure Request
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Account & Cloud Data Deletion",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Pursuant to Google Play Data Safety policies, you have the right to request deletion of your account and associated data stored across Google Drive or cloud backups.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://self-budget-app.netlify.app/#delete-account"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Web Erasure Form 🌐", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }

            // Section 2: On-Device Data Wipe & Sign Out
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = ExpenseRed.copy(alpha = 0.08f)
                ),
                border = BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = null,
                            tint = ExpenseRed
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "On-Device Data Wipe & Sign Out",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = ExpenseRed
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "This action immediately performs the following:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "• Erases all local database records (transactions, income, expense categories, budgets, and recurring items)\n• Resets all app preferences and biometric security credentials\n• Revokes and signs out of your active Google session",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = { showAccountDeletionDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ExpenseRed,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Wipe Local Data & Sign Out 🗑️", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(150.dp))
        }
    }

    if (showResetActivityConfirmation) {
        Dialog(onDismissRequest = { showResetActivityConfirmation = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth(0.92f)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Reset Activity Only?",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Are you sure you want to delete ALL transaction activity (expenses, income, transfers)?\n\n✅ Kept: Accounts, Wallets, Budgets, Recurring rules, and Goals remain safe.\n\n❌ Deleted: Transaction history only.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showResetActivityConfirmation = false },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Text("Cancel", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                onResetTransactionsOnly()
                                showResetActivityConfirmation = false
                                syncStatusMessage = "✅ Transaction activity wiped. Accounts, wallets & goals preserved!"
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(46.dp)
                        ) {
                            Text("Reset Activity", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showResetConfirmation) {
        Dialog(onDismissRequest = { showResetConfirmation = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth(0.92f)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = ExpenseRed.copy(alpha = 0.15f),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = ExpenseRed,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Reset All App Data?",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Are you sure you want to delete ALL logged transactions, category budgets, recurring bills, and accounts? This action cannot be undone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showResetConfirmation = false },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Text("Cancel", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                onResetData()
                                showResetConfirmation = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(46.dp)
                        ) {
                            Text("Reset Data", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showAccountDeletionDialog) {
        Dialog(onDismissRequest = { showAccountDeletionDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                border = BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth(0.92f)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = ExpenseRed.copy(alpha = 0.15f),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.DeleteForever,
                                contentDescription = null,
                                tint = ExpenseRed,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Wipe Data & Sign Out?",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Are you sure you want to permanently erase all local budget data, transactions, and sign out of your Google session? This action cannot be undone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                showAccountDeletionDialog = false
                                onResetData()
                                onSignOut()
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ExpenseRed,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Confirm Wipe & Sign Out", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { showAccountDeletionDialog = false },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                        ) {
                            Text("Cancel", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showManageCategoriesModal) {
        com.selfbudget.app.core.ui.ManageCategoriesModal(
            categories = categories,
            onDismiss = { showManageCategoriesModal = false },
            onToggleCategoryArchive = { cat -> onToggleCategoryArchive?.invoke(cat) }
        )
    }

    if (showDataExportModal) {
        DataExportModal(
            transactions = transactions,
            categories = categories,
            accounts = accounts,
            recurringList = recurringList,
            budgets = budgets,
            goals = goals,
            accountBalances = accountBalances,
            onDismiss = { showDataExportModal = false },
            onExportComplete = { _, message ->
                syncStatusMessage = message
            }
        )
    }

    pendingImportData?.let { importData ->
        DataImportPreviewModal(
            data = importData,
            onDismiss = { pendingImportData = null },
            onConfirmImport = {
                val dataToCommit = importData
                pendingImportData = null
                if (onImportData != null) {
                    onImportData(dataToCommit, { count ->
                        syncStatusMessage = "✅ Successfully imported $count items from ${dataToCommit.fileName}!"
                    }, { error ->
                        syncStatusMessage = "❌ Import failed: $error"
                    })
                } else {
                    coroutineScope.launch {
                        val result = DataImporter.commitImportToDatabase(
                            AppDatabase.getInstance(context),
                            dataToCommit
                        )
                        result.onSuccess { count ->
                            syncStatusMessage = "✅ Successfully imported $count items from ${dataToCommit.fileName}!"
                        }.onFailure { error ->
                            syncStatusMessage = "❌ Import failed: ${error.localizedMessage}"
                        }
                    }
                }
            }
        )
    }
}
}

@Composable
private fun SettingsCategoryRow(
    icon: ImageVector,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = iconTint.copy(alpha = 0.12f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
