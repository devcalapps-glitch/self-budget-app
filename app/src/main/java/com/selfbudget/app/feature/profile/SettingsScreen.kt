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
import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
import com.selfbudget.app.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CleaningServices
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
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.text.style.TextOverflow
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
import com.selfbudget.app.core.ui.components.DestructivePillButton
import com.selfbudget.app.core.ui.components.GrayIconTile
import com.selfbudget.app.core.ui.components.PrimaryPillButton
import com.selfbudget.app.core.ui.components.RampIconTile
import com.selfbudget.app.core.ui.components.SecondaryPillButton
import com.selfbudget.app.core.ui.components.SectionRowDivider
import com.selfbudget.app.core.ui.components.ToggleRow
import com.selfbudget.app.ui.theme.Ramp
import com.selfbudget.app.ui.theme.SelfBudgetType
import com.selfbudget.app.ui.theme.ShapeCard
import com.selfbudget.app.ui.theme.ShapeChip
import com.selfbudget.app.ui.theme.ShapeHero
import com.selfbudget.app.ui.theme.ShapePage
import com.selfbudget.app.ui.theme.ShapePill
import com.selfbudget.app.ui.theme.ShapeTile
import com.selfbudget.app.ui.theme.getExpenseColor
import com.selfbudget.app.ui.theme.isAppInDarkTheme
import com.selfbudget.app.ui.theme.tintFill
import com.selfbudget.app.ui.theme.titleText
import com.selfbudget.app.ui.theme.secondaryText
import com.selfbudget.app.ui.theme.solidFill
import com.selfbudget.app.core.ui.ManageCategoriesContent
import com.selfbudget.app.ui.theme.onSolidFill
import com.selfbudget.app.ui.theme.containerBorder
import kotlinx.coroutines.launch

private enum class SettingsSubScreen(val title: String) {
    MAIN("Settings"),
    PREFERENCES("General Preferences & Security"),
    CATEGORIES("Manage Custom Categories"),
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
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (activeSubScreen != SettingsSubScreen.MAIN || onDismiss != null) {
                        IconButton(
                            onClick = {
                                if (activeSubScreen != SettingsSubScreen.MAIN) {
                                    activeSubScreen = when (activeSubScreen) {
                                        SettingsSubScreen.PRIVACY, SettingsSubScreen.TERMS -> SettingsSubScreen.LEGAL
                                        SettingsSubScreen.DELETE_ACCOUNT -> SettingsSubScreen.BACKUP
                                        else -> SettingsSubScreen.MAIN
                                    }
                                } else {
                                    onDismiss?.invoke()
                                }
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = activeSubScreen.title,
                        style = SelfBudgetType.title,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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
            val isDarkMain = isAppInDarkTheme()

            // 1. Centered User Profile Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = ShapeCard,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
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
                        color = Ramp.Teal.tintFill(isDarkMain),
                        modifier = Modifier.size(60.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = initialLetter,
                                style = SelfBudgetType.title,
                                color = Ramp.Teal.titleText(isDarkMain)
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
                            style = SelfBudgetType.heading,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verified Google Account",
                            tint = Ramp.Teal.secondaryText(isDarkMain),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (!user?.email.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = user?.email ?: "",
                            style = SelfBudgetType.meta,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Section Label
            Text(
                text = "PREFERENCES & SETTINGS",
                style = SelfBudgetType.eyebrow,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )

            // Grouped Navigation Menu in a Single Seamless Surface
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = ShapeCard,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsGroupedItem(
                        icon = Icons.Default.Settings,
                        title = "General Preferences & Security",
                        subtitle = "Theme (${themeMode.name.lowercase()}), Currency ($currencySymbol), Biometrics",
                        onClick = { activeSubScreen = SettingsSubScreen.PREFERENCES }
                    )

                    SectionRowDivider(modifier = Modifier.padding(start = 68.dp))

                    SettingsGroupedItem(
                        icon = Icons.Default.Category,
                        title = "Manage Custom Categories",
                        subtitle = "View, archive, or restore custom categories",
                        onClick = { activeSubScreen = SettingsSubScreen.CATEGORIES }
                    )

                    SectionRowDivider(modifier = Modifier.padding(start = 68.dp))

                    SettingsGroupedItem(
                        icon = Icons.Default.CloudDone,
                        title = "Data & Account Management",
                        subtitle = "Google Drive sync, JSON/CSV backup & reset",
                        onClick = { activeSubScreen = SettingsSubScreen.BACKUP }
                    )

                    SectionRowDivider(modifier = Modifier.padding(start = 68.dp))

                    SettingsGroupedItem(
                        icon = Icons.Default.Gavel,
                        title = "Legal & Privacy",
                        subtitle = "Privacy Policy & Terms of Service",
                        onClick = { activeSubScreen = SettingsSubScreen.LEGAL }
                    )

                    SectionRowDivider(modifier = Modifier.padding(start = 68.dp))

                    SettingsGroupedItem(
                        icon = Icons.Default.Info,
                        title = "About Developer & Support",
                        subtitle = "Version 1.0.0, dev contact email",
                        onClick = { activeSubScreen = SettingsSubScreen.ABOUT }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Sign-out is a plain, reversible action - not destructive - so it reads as a
            // neutral secondary pill, not the Red reserved for irreversible data loss (spec §14).
            SecondaryPillButton(
                text = "Log out",
                onClick = onSignOut,
                ramp = Ramp.Gray,
                icon = Icons.AutoMirrored.Filled.Logout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            )

            Spacer(modifier = Modifier.height(150.dp))
        }

        // --- SUB-SCREEN 1: GENERAL PREFERENCES & SECURITY ---
        if (activeSubScreen == SettingsSubScreen.PREFERENCES) {
            val isDarkPrefs = isAppInDarkTheme()
            val chipColors = FilterChipDefaults.filterChipColors(
                containerColor = Ramp.Gray.tintFill(isDarkPrefs),
                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                selectedContainerColor = Ramp.Teal.solidFill(isDarkPrefs),
                selectedLabelColor = Ramp.Teal.onSolidFill(isDarkPrefs),
                selectedLeadingIconColor = Ramp.Teal.onSolidFill(isDarkPrefs)
            )

            // Theme Mode Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = ShapeCard,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GrayIconTile(icon = Icons.Default.DarkMode, size = 36.dp, iconSize = 18.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = "App theme / appearance", style = SelfBudgetType.rowTitle, color = MaterialTheme.colorScheme.onSurface)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = themeMode == AppThemeMode.SYSTEM,
                            onClick = { onSetThemeMode(AppThemeMode.SYSTEM) },
                            shape = ShapePill,
                            colors = chipColors,
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.SettingsSuggest, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("System", style = SelfBudgetType.badge)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = themeMode == AppThemeMode.LIGHT,
                            onClick = { onSetThemeMode(AppThemeMode.LIGHT) },
                            shape = ShapePill,
                            colors = chipColors,
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LightMode, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Light", style = SelfBudgetType.badge)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = themeMode == AppThemeMode.DARK,
                            onClick = { onSetThemeMode(AppThemeMode.DARK) },
                            shape = ShapePill,
                            colors = chipColors,
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.DarkMode, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Dark", style = SelfBudgetType.badge)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Currency Symbol Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = ShapeCard,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GrayIconTile(icon = Icons.Default.AttachMoney, size = 36.dp, iconSize = 18.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = "Preferred currency symbol", style = SelfBudgetType.rowTitle, color = MaterialTheme.colorScheme.onSurface)
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
                                shape = ShapePill,
                                colors = chipColors,
                                label = { Text(symbol, style = SelfBudgetType.badge) }
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
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = ShapeCard,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            GrayIconTile(icon = Icons.Default.AttachMoney, size = 36.dp, iconSize = 18.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = "Exchange rates", style = SelfBudgetType.rowTitle, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Self Budget is offline-first, so rates aren't fetched automatically — enter them yourself to include foreign-currency accounts in your net worth total.",
                            style = SelfBudgetType.body,
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
                                    style = SelfBudgetType.body,
                                    color = MaterialTheme.colorScheme.onSurface,
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
                                    shape = ShapeChip,
                                    modifier = Modifier.width(120.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Security & App Lock Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = ShapeCard,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                ToggleRow(
                    icon = Icons.Default.Fingerprint,
                    title = "Biometric app lock",
                    description = "Require Face/Touch ID on launch",
                    checked = isBiometricEnabled,
                    onCheckedChange = onSetBiometricEnabled
                )
            }

            // Push Notifications & Alerts Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = ShapeCard,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
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
                            GrayIconTile(icon = Icons.Default.Notifications, size = 40.dp, iconSize = 22.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = "Push notifications & alerts", style = SelfBudgetType.rowTitle, color = MaterialTheme.colorScheme.onSurface)
                                Text(
                                    text = if (hasNotificationPermission) "Daily bill & budget alerts active" else "Tap to enable local notifications",
                                    style = SelfBudgetType.meta,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                            PrimaryPillButton(
                                text = "Enable",
                                onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                                ramp = Ramp.Teal
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    SecondaryPillButton(
                        text = "Send test notification",
                        icon = Icons.Default.NotificationsActive,
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                NotificationHelper.sendNotification(
                                    context,
                                    777,
                                    "Test notification received",
                                    "Your local push notifications and bill reminders are working perfectly!"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(150.dp))
        }

        // --- SUB-SCREEN: MANAGE CUSTOM CATEGORIES ---
        if (activeSubScreen == SettingsSubScreen.CATEGORIES) {
            ManageCategoriesContent(
                categories = categories,
                onToggleCategoryArchive = { cat -> onToggleCategoryArchive?.invoke(cat) }
            )

            Spacer(modifier = Modifier.height(150.dp))
        }

        // --- SUB-SCREEN 2: DATA, BACKUP & CLOUD SYNC ---
        if (activeSubScreen == SettingsSubScreen.BACKUP) {
            val isDarkBackup = isAppInDarkTheme()
            val isSyncError = syncStatusMessage?.let {
                it.contains("❌") || it.contains("⚠️") || it.contains("fail", ignoreCase = true)
            } ?: false

            // SECTION 1: Automated Google Drive Cloud Sync
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = ShapeCard,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GrayIconTile(icon = Icons.Default.CloudDone, size = 36.dp, iconSize = 18.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Automated Google Drive cloud sync",
                            style = SelfBudgetType.rowTitle,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Zero-cost background sync to your personal Google Drive appDataFolder. Data is stored privately inside your Google Account.",
                        style = SelfBudgetType.body,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (syncStatusMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val statusRamp = if (isSyncError) Ramp.Red else Ramp.Teal
                        Text(
                            text = syncStatusMessage!!,
                            style = SelfBudgetType.meta,
                            color = statusRamp.secondaryText(isDarkBackup)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PrimaryPillButton(
                            text = "Drive sync",
                            icon = Icons.Default.CloudUpload,
                            ramp = Ramp.Teal,
                            onClick = {
                                requestDriveAccessAndExecute { account ->
                                    onDriveSyncClick(account) { message ->
                                        syncStatusMessage = message
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )

                        SecondaryPillButton(
                            text = "Drive restore",
                            icon = Icons.Default.CloudDownload,
                            ramp = Ramp.Teal,
                            onClick = {
                                requestDriveAccessAndExecute { account ->
                                    onDriveRestoreClick(account) { message ->
                                        syncStatusMessage = message
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // SECTION 2: Manual Backup & File Export
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = ShapeCard,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GrayIconTile(icon = Icons.Default.FileDownload, size = 36.dp, iconSize = 18.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = "Manual backup & export", style = SelfBudgetType.rowTitle, color = MaterialTheme.colorScheme.onSurface)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Export or restore your full account history using JSON, or selectively export transactions, recurring bills, budget plans, savings goals, and accounts to multi-tab Excel (.xlsx) or CSV.",
                        style = SelfBudgetType.body,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SecondaryPillButton(
                            text = "Export data",
                            icon = Icons.Default.FileDownload,
                            ramp = Ramp.Teal,
                            onClick = { showDataExportModal = true },
                            modifier = Modifier.weight(1f)
                        )

                        PrimaryPillButton(
                            text = "Import data",
                            icon = Icons.Default.FileUpload,
                            ramp = Ramp.Teal,
                            onClick = { dataImportPickerLauncher.launch("*/*") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SecondaryPillButton(
                            text = "Backup JSON",
                            icon = Icons.Default.CloudUpload,
                            ramp = Ramp.Gray,
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
                            modifier = Modifier.weight(1f)
                        )

                        SecondaryPillButton(
                            text = "Restore JSON",
                            icon = Icons.Default.CloudDownload,
                            ramp = Ramp.Gray,
                            onClick = { jsonRestorePickerLauncher.launch("*/*") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Section Label: DANGER ZONE
            Text(
                text = "DANGER ZONE",
                style = SelfBudgetType.eyebrow,
                color = Ramp.Red.titleText(isDarkBackup),
                modifier = Modifier.padding(start = 4.dp, top = 6.dp)
            )

            // SECTION 3: Clean Sweep / Reset Data Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = ShapeCard,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, Ramp.Red.containerBorder(isDarkBackup))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RampIconTile(icon = Icons.Default.CleaningServices, ramp = Ramp.Red, size = 36.dp, iconSize = 18.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Reset ledger & app data",
                                style = SelfBudgetType.rowTitle,
                                color = Ramp.Red.titleText(isDarkBackup)
                            )
                            Text(
                                text = "Irreversible data wipe",
                                style = SelfBudgetType.meta,
                                color = Ramp.Red.secondaryText(isDarkBackup)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Reset your transaction ledger only, or permanently wipe all app records and start completely fresh.",
                        style = SelfBudgetType.body,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Option 1: Reset Activity Only (Transactions)
                    DestructivePillButton(
                        text = "Reset activity only (transactions)",
                        icon = Icons.AutoMirrored.Filled.ReceiptLong,
                        onClick = { showResetActivityConfirmation = true },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "• Wipes transaction history only\n• Keeps all Accounts, Wallets, Budgets, Recurring rules, and Goals intact",
                        style = SelfBudgetType.meta,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 12.dp)
                    )

                    // Option 2: Reset All App Data
                    DestructivePillButton(
                        text = "Reset all app data",
                        icon = Icons.Default.DeleteForever,
                        onClick = { showResetConfirmation = true },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "• Permanently deletes all records (transactions, accounts, budgets, goals, recurring)",
                        style = SelfBudgetType.meta,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }
            }

            // SECTION 4: Delete Account & Cloud Erasure Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = ShapeCard,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, Ramp.Red.containerBorder(isDarkBackup))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RampIconTile(icon = Icons.Default.PersonRemove, ramp = Ramp.Red, size = 36.dp, iconSize = 18.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Delete account & cloud data",
                                style = SelfBudgetType.rowTitle,
                                color = Ramp.Red.titleText(isDarkBackup)
                            )
                            Text(
                                text = "Account deletion and cloud erasure options",
                                style = SelfBudgetType.meta,
                                color = Ramp.Red.secondaryText(isDarkBackup)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Submit a Google Play data safety cloud erasure request, or execute a permanent on-device wipe and sign out.",
                        style = SelfBudgetType.body,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    DestructivePillButton(
                        text = "Delete account options",
                        icon = Icons.Default.DeleteForever,
                        onClick = { activeSubScreen = SettingsSubScreen.DELETE_ACCOUNT },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(150.dp))
        }

        // --- SUB-SCREEN 3: LEGAL & PRIVACY (CATEGORY PAGE WITH PRIVACY POLICY & TERMS OF SERVICE) ---
        if (activeSubScreen == SettingsSubScreen.LEGAL) {
            Text(
                text = "POLICIES & AGREEMENTS",
                style = SelfBudgetType.eyebrow,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = ShapeCard,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsGroupedItem(
                        icon = Icons.Default.PrivacyTip,
                        title = "Privacy Policy",
                        subtitle = "100% offline & zero-data collection architecture",
                        onClick = { activeSubScreen = SettingsSubScreen.PRIVACY }
                    )

                    SectionRowDivider(modifier = Modifier.padding(start = 68.dp))

                    SettingsGroupedItem(
                        icon = Icons.Default.Description,
                        title = "Terms of Service",
                        subtitle = "App usage terms, personal license & disclaimer",
                        onClick = { activeSubScreen = SettingsSubScreen.TERMS }
                    )
                }
            }

            Spacer(modifier = Modifier.height(150.dp))
        }

        // --- SUB-SCREEN 4: PRIVACY POLICY PAGE ---
        if (activeSubScreen == SettingsSubScreen.PRIVACY) {
            // No repeated "Privacy Policy" title here (spec §14): the page's own top bar
            // already reads that - same duplicate-headline pattern fixed elsewhere.
            val isDarkPrivacy = isAppInDarkTheme()
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = ShapeCard,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Last updated: August 25, 2026",
                        style = SelfBudgetType.meta,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "1. Zero server data collection",
                        style = SelfBudgetType.rowTitle,
                        color = Ramp.Teal.titleText(isDarkPrivacy)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Self Budget operates completely offline and has no external tracking servers or third-party telemetry. We do not collect, transmit, sell, or rent your personal or financial data.",
                        style = SelfBudgetType.body,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "2. On-device local storage",
                        style = SelfBudgetType.rowTitle,
                        color = Ramp.Teal.titleText(isDarkPrivacy)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "All accounts, transactions, budgets, goals, and recurring items are stored locally in an encrypted Room SQLite database on your device.",
                        style = SelfBudgetType.body,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "3. Google Drive private sandbox",
                        style = SelfBudgetType.rowTitle,
                        color = Ramp.Teal.titleText(isDarkPrivacy)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Automated cloud sync uses the private appDataFolder scope. Backups are stored in your own Google Account. Third parties and developers have zero access to your backup files.",
                        style = SelfBudgetType.body,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "4. Biometric protection",
                        style = SelfBudgetType.rowTitle,
                        color = Ramp.Teal.titleText(isDarkPrivacy)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Fingerprint and Face Unlock authentication uses native Android BiometricPrompt hardware security (TEE). Biometric data never leaves your device's secure enclave.",
                        style = SelfBudgetType.body,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    SecondaryPillButton(
                        text = "View online privacy policy",
                        icon = Icons.AutoMirrored.Filled.OpenInNew,
                        ramp = Ramp.Teal,
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://self-budget-app.netlify.app#privacy"))
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // fallback
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(150.dp))
        }

        // --- SUB-SCREEN 5: TERMS OF SERVICE PAGE ---
        if (activeSubScreen == SettingsSubScreen.TERMS) {
            // No repeated "Terms of Service" title here (spec §14): the page's own top bar
            // already reads that - same duplicate-headline pattern fixed elsewhere.
            val isDarkTerms = isAppInDarkTheme()
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = ShapeCard,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Last updated: August 25, 2026",
                        style = SelfBudgetType.meta,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "1. Acceptance of terms",
                        style = SelfBudgetType.rowTitle,
                        color = Ramp.Teal.titleText(isDarkTerms)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "By installing or using Self Budget provided by DevCalApps, you agree to be bound by these Terms of Service.",
                        style = SelfBudgetType.body,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "2. License & personal use",
                        style = SelfBudgetType.rowTitle,
                        color = Ramp.Teal.titleText(isDarkTerms)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "DevCalApps grants you a personal, non-exclusive license to use Self Budget strictly for personal financial and budget management.",
                        style = SelfBudgetType.body,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "3. Offline data control & responsibility",
                        style = SelfBudgetType.rowTitle,
                        color = Ramp.Teal.titleText(isDarkTerms)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Because Self Budget is an offline-first app, you are responsible for maintaining local backups. DevCalApps is not liable for data loss caused by device damage, unbacked resets, or forgotten passcodes.",
                        style = SelfBudgetType.body,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "4. Financial advice disclaimer",
                        style = SelfBudgetType.rowTitle,
                        color = Ramp.Teal.titleText(isDarkTerms)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Self Budget is a personal expense logging tool. The app does not provide accounting, tax, or legal financial advice.",
                        style = SelfBudgetType.body,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    SecondaryPillButton(
                        text = "View online terms of service",
                        icon = Icons.AutoMirrored.Filled.OpenInNew,
                        ramp = Ramp.Teal,
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://self-budget-app.netlify.app#terms"))
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // fallback
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(150.dp))
        }

        // --- SUB-SCREEN 6: ABOUT DEVELOPER & SUPPORT ---
        if (activeSubScreen == SettingsSubScreen.ABOUT) {
            val isDarkAbout = isAppInDarkTheme()

            // 1. App Identity Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = ShapeCard,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_app),
                        contentDescription = "Self Budget App Icon",
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Self Budget",
                        style = SelfBudgetType.heading,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Version 1.0.0",
                        style = SelfBudgetType.meta,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Smart, privacy-first personal finance tracking built with pure Jetpack Compose and offline Room persistence.",
                        style = SelfBudgetType.body,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            // Section Label: ABOUT DEVELOPER
            Text(
                text = "ABOUT THE DEVELOPER",
                style = SelfBudgetType.eyebrow,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )

            // 2. Developer Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = ShapeCard,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GrayIconTile(icon = Icons.Default.Person, size = 36.dp, iconSize = 18.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "DevCalApps",
                                style = SelfBudgetType.rowTitle,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Independent Android Engineering & Product Design",
                                style = SelfBudgetType.meta,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Self Budget was built with a clear philosophy: personal finance software should be transparent, lightning fast, and entirely respectful of user privacy. There are no tracking analytics, no third-party data broker SDKs, and zero paywalls for core budgeting features.",
                        style = SelfBudgetType.body,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Section Label: CORE PRINCIPLES
            Text(
                text = "CORE PRINCIPLES & PRIVACY",
                style = SelfBudgetType.eyebrow,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )

            // 3. Core Principles Surface
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = ShapeCard,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        RampIconTile(icon = Icons.Default.PrivacyTip, ramp = Ramp.Teal, size = 32.dp, iconSize = 16.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "100% Offline-First Architecture",
                                style = SelfBudgetType.rowTitle,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "All transactions, income, and account balances reside solely in your on-device SQLite/Room database.",
                                style = SelfBudgetType.body,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.Top) {
                        RampIconTile(icon = Icons.Default.CloudDone, ramp = Ramp.Blue, size = 32.dp, iconSize = 16.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Private Google Drive Sync",
                                style = SelfBudgetType.rowTitle,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Cloud backups are stored strictly in your own private Google Drive AppData folder, inaccessible to third parties.",
                                style = SelfBudgetType.body,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.Top) {
                        RampIconTile(icon = Icons.Default.Fingerprint, ramp = Ramp.Purple, size = 32.dp, iconSize = 16.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Hardware-Backed Security",
                                style = SelfBudgetType.rowTitle,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Biometric authentication utilizes the Android Keystore / BiometricPrompt framework for robust device-level protection.",
                                style = SelfBudgetType.body,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Section Label: SUPPORT & FEEDBACK
            Text(
                text = "SUPPORT & FEEDBACK",
                style = SelfBudgetType.eyebrow,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )

            // 4. Support Actions Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = ShapeCard,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsGroupedItem(
                        icon = Icons.Default.Email,
                        title = "Direct Developer Email",
                        subtitle = "dev.cal.apps@gmail.com",
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:dev.cal.apps@gmail.com")
                                putExtra(Intent.EXTRA_SUBJECT, "Self Budget - Support & Feedback [v1.0.0]")
                            }
                            try {
                                context.startActivity(Intent.createChooser(intent, "Contact Developer"))
                            } catch (e: Exception) {
                                // fallback
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(150.dp))
        }

        // --- SUB-SCREEN 7: DELETE ACCOUNT & DATA FULL PAGE ---
        if (activeSubScreen == SettingsSubScreen.DELETE_ACCOUNT) {
            val isDarkDelete = isAppInDarkTheme()

            // Section 1: Cloud & Web Account Data Erasure Request. No separate header card
            // here (spec §14): the page's own top bar already reads "Delete Account & Data" -
            // restating it in a card below would just be the same duplicate-headline pattern
            // fixed on the Net Worth page.
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = ShapeCard,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GrayIconTile(icon = Icons.Default.Public, size = 36.dp, iconSize = 18.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Account & cloud data deletion",
                            style = SelfBudgetType.rowTitle,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Pursuant to Google Play Data Safety policies, you have the right to request deletion of your account and associated data stored across Google Drive or cloud backups.",
                        style = SelfBudgetType.body,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    SecondaryPillButton(
                        text = "Open web erasure form",
                        icon = Icons.AutoMirrored.Filled.OpenInNew,
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://self-budget-app.netlify.app/#delete-account"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    )
                }
            }

            // Section 2: On-Device Data Wipe & Sign Out
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = ShapeCard,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, Ramp.Red.containerBorder(isDarkDelete))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RampIconTile(icon = Icons.Default.DeleteForever, ramp = Ramp.Red, size = 36.dp, iconSize = 18.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "On-device data wipe & sign out",
                            style = SelfBudgetType.rowTitle,
                            color = Ramp.Red.titleText(isDarkDelete)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "This action immediately performs the following:",
                        style = SelfBudgetType.body,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "• Erases all local database records (transactions, income, expense categories, budgets, and recurring items)\n• Resets all app preferences and biometric security credentials\n• Revokes and signs out of your active Google session",
                        style = SelfBudgetType.body,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    DestructivePillButton(
                        text = "Wipe local data & sign out",
                        onClick = { showAccountDeletionDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(150.dp))
        }
    }

    if (showResetActivityConfirmation) {
        Dialog(onDismissRequest = { showResetActivityConfirmation = false }) {
            Surface(
                shape = ShapeHero,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth(0.92f)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    RampIconTile(icon = Icons.AutoMirrored.Filled.ReceiptLong, ramp = Ramp.Red, size = 56.dp, iconSize = 28.dp)

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Reset activity only?",
                        style = SelfBudgetType.title,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Are you sure you want to delete ALL transaction activity (expenses, income, transfers)?\n\nKept: Accounts, wallets, budgets, recurring rules, and goals remain safe.\n\nDeleted: Transaction history only.",
                        style = SelfBudgetType.body,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SecondaryPillButton(
                            text = "Cancel",
                            onClick = { showResetActivityConfirmation = false },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        )

                        PrimaryPillButton(
                            text = "Reset activity",
                            onClick = {
                                onResetTransactionsOnly()
                                showResetActivityConfirmation = false
                                syncStatusMessage = "Transaction activity wiped. Accounts, wallets & goals preserved!"
                            },
                            ramp = Ramp.Red,
                            modifier = Modifier
                                .weight(1.3f)
                                .height(46.dp)
                        )
                    }
                }
            }
        }
    }

    if (showResetConfirmation) {
        Dialog(onDismissRequest = { showResetConfirmation = false }) {
            Surface(
                shape = ShapeHero,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth(0.92f)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    RampIconTile(icon = Icons.Default.Delete, ramp = Ramp.Red, size = 56.dp, iconSize = 28.dp)

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Reset all app data?",
                        style = SelfBudgetType.title,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Are you sure you want to delete ALL logged transactions, category budgets, recurring bills, and accounts? This action cannot be undone.",
                        style = SelfBudgetType.body,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SecondaryPillButton(
                            text = "Cancel",
                            onClick = { showResetConfirmation = false },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        )

                        PrimaryPillButton(
                            text = "Reset data",
                            onClick = {
                                onResetData()
                                showResetConfirmation = false
                            },
                            ramp = Ramp.Red,
                            modifier = Modifier
                                .weight(1.3f)
                                .height(46.dp)
                        )
                    }
                }
            }
        }
    }

    if (showAccountDeletionDialog) {
        Dialog(onDismissRequest = { showAccountDeletionDialog = false }) {
            Surface(
                shape = ShapeHero,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth(0.92f)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    RampIconTile(icon = Icons.Default.DeleteForever, ramp = Ramp.Red, size = 56.dp, iconSize = 28.dp)

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Wipe data & sign out?",
                        style = SelfBudgetType.title,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Are you sure you want to permanently erase all local budget data, transactions, and sign out of your Google session? This action cannot be undone.",
                        style = SelfBudgetType.body,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // The confirm step's destructive action may be solid red (spec §14's
                        // one sanctioned exception to "never solid red").
                        PrimaryPillButton(
                            text = "Confirm wipe & sign out",
                            onClick = {
                                showAccountDeletionDialog = false
                                onResetData()
                                onSignOut()
                            },
                            ramp = Ramp.Red,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        )

                        SecondaryPillButton(
                            text = "Cancel",
                            onClick = { showAccountDeletionDialog = false },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                        )
                    }
                }
            }
        }
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
private fun SettingsGroupedItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GrayIconTile(icon = icon, size = 38.dp, iconSize = 20.dp)

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = SelfBudgetType.rowTitle, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, style = SelfBudgetType.meta, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }

        Spacer(modifier = Modifier.width(8.dp))

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
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
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = ShapeCard,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = ShapeTile,
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
                Text(text = title, style = SelfBudgetType.heading, color = MaterialTheme.colorScheme.onSurface)
                Text(text = subtitle, style = SelfBudgetType.meta, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
