package com.selfbudget.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.selfbudget.app.core.security.BiometricSecurityManager
import com.selfbudget.app.core.util.BillReminderWorker
import com.selfbudget.app.core.util.NotificationHelper
import com.selfbudget.app.data.model.AppThemeMode
import com.selfbudget.app.feature.auth.AppLockScreen
import com.selfbudget.app.feature.auth.LoginScreen
import com.selfbudget.app.feature.dashboard.HomeScreen
import com.selfbudget.app.feature.onboarding.OnboardingQuestionnaireScreen
import com.selfbudget.app.ui.MainViewModel
import com.selfbudget.app.ui.theme.SelfBudgetTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.createNotificationChannel(this)
        BillReminderWorker.scheduleDaily8AmReminder(this)

        setContent {
            val currentUser by viewModel.currentUser.collectAsState()
            val uiState by viewModel.uiState.collectAsState()
            val authError by viewModel.authError.collectAsState()

            var isAppLocked by rememberSaveable { mutableStateOf(false) }
            var hasAuthenticatedSession by rememberSaveable { mutableStateOf(false) }
            var showBiometricSetupPrompt by remember { mutableStateOf(false) }
            var hasHandledBiometricSetupPrompt by rememberSaveable { mutableStateOf(false) }

            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                // Permission response registered
            }

            val isDarkTheme = when (uiState.themeMode) {
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            // Notification Permission Request for Android 13+
            LaunchedEffect(currentUser) {
                if (currentUser != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            // Single 1-Time Biometric Auth & Onboarding Launcher
            LaunchedEffect(currentUser, uiState.isBiometricEnabled) {
                if (currentUser != null && !hasAuthenticatedSession) {
                    if (uiState.isBiometricEnabled) {
                        showBiometricSetupPrompt = false
                        hasHandledBiometricSetupPrompt = true
                        isAppLocked = true
                        triggerBiometricAuth {
                            hasAuthenticatedSession = true
                            isAppLocked = false
                        }
                    } else if (!hasHandledBiometricSetupPrompt && BiometricSecurityManager.canAuthenticate(this@MainActivity)) {
                        hasHandledBiometricSetupPrompt = true
                        showBiometricSetupPrompt = true
                    } else {
                        hasAuthenticatedSession = true
                        isAppLocked = false
                    }
                }
            }

            SelfBudgetTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (currentUser == null) {
                        LoginScreen(
                            onGoogleSignInClick = { context, webClientId ->
                                viewModel.signInWithGoogle(context, webClientId)
                            },
                            errorMessage = authError,
                            onClearError = { viewModel.clearAuthError() }
                        )
                    } else if (uiState.user != null && !uiState.user!!.hasCompletedOnboarding) {
                        OnboardingQuestionnaireScreen(
                            onComplete = { preferredCurrency, primaryGoal, referralSource ->
                                viewModel.completeOnboarding(
                                    currency = preferredCurrency,
                                    primaryGoal = primaryGoal,
                                    referralSource = referralSource
                                )
                            }
                        )
                    } else if (isAppLocked) {
                        AppLockScreen(
                            onUnlockClick = {
                                triggerBiometricAuth {
                                    hasAuthenticatedSession = true
                                    isAppLocked = false
                                }
                            },
                            onSkipClick = {
                                hasAuthenticatedSession = true
                                isAppLocked = false
                            }
                        )
                    } else if (showBiometricSetupPrompt && !uiState.isBiometricEnabled) {
                        AppLockScreen(
                            onUnlockClick = {
                                triggerBiometricAuth {
                                    viewModel.setBiometricEnabled(true)
                                    hasAuthenticatedSession = true
                                    showBiometricSetupPrompt = false
                                    isAppLocked = false
                                }
                            },
                            onSkipClick = {
                                hasAuthenticatedSession = true
                                showBiometricSetupPrompt = false
                            },
                            isSetupPrompt = true
                        )
                    } else {
                        HomeScreen(
                            uiState = uiState,
                            onPreviousMonth = {
                                viewModel.navigateMonth(-1)
                            },
                            onNextMonth = {
                                viewModel.navigateMonth(1)
                            },
                            onSelectMonthYear = { monthYear ->
                                viewModel.setSelectedMonthYear(monthYear)
                            },
                            onAddTransaction = { title, amount, type, categoryId, accountId, note, timestamp, isRecurring, recurringFrequency, transferAccountId ->
                                viewModel.addTransaction(
                                    title = title,
                                    amount = amount,
                                    type = type,
                                    categoryId = categoryId,
                                    accountId = accountId,
                                    note = note,
                                    paymentMethod = "Cash",
                                    receiptUri = null,
                                    timestamp = timestamp,
                                    isRecurring = isRecurring,
                                    recurringFrequency = recurringFrequency,
                                    transferAccountId = transferAccountId
                                )
                            },
                            onUpdateTransaction = { updated ->
                                viewModel.updateTransaction(updated)
                            },
                            onDeleteTransaction = { transaction ->
                                viewModel.deleteTransaction(transaction)
                            },
                            onSetBudget = { categoryId, limit, rolloverEnabled ->
                                viewModel.setCategoryBudget(categoryId, limit, rolloverEnabled)
                            },
                            onDeleteBudget = { categoryId ->
                                viewModel.deleteCategoryBudget(categoryId)
                            },
                            onAddRecurring = { title, amount, type, categoryId, frequency, remainingOccurrences, nextDueDate, transferAccountId ->
                                viewModel.addRecurringTransaction(title, amount, type, categoryId, frequency, remainingOccurrences, nextDueDate, transferAccountId)
                            },
                            onDeleteRecurring = { recurring ->
                                viewModel.deleteRecurringTransaction(recurring)
                            },
                            onPostRecurring = { recurring ->
                                viewModel.postRecurringTransaction(recurring)
                            },
                            onUpdateRecurring = { recurring ->
                                viewModel.updateRecurringTransaction(recurring)
                            },
                            onAddCustomCategory = { category ->
                                viewModel.addCustomCategory(category)
                            },
                            onToggleCategoryArchive = { category ->
                                viewModel.toggleCategoryArchive(category)
                            },
                            onAddCustomAccount = { account ->
                                viewModel.addAccount(account)
                            },
                            onUpdateAccount = { account ->
                                viewModel.updateAccount(account)
                            },
                            onDeleteAccount = { account ->
                                viewModel.deleteAccount(account)
                            },
                            onAddTransfer = { fromId, toId, amount, note ->
                                viewModel.addTransfer(fromId, toId, amount, note)
                            },
                            onAddGoal = { name, target, accountId, targetDate ->
                                viewModel.addGoal(name, target, targetDate = targetDate, linkedAccountId = accountId)
                            },
                            onDeleteGoal = { goal ->
                                viewModel.deleteGoal(goal)
                            },
                            onContributeToGoal = { goal, amount ->
                                viewModel.contributeToGoal(goal, amount)
                            },
                            onUpdateGoal = { goal ->
                                viewModel.updateGoal(goal)
                            },
                            onSetExchangeRate = { from, to, rate ->
                                viewModel.setExchangeRate(from, to, rate)
                            },
                            onSetCurrency = { currency ->
                                viewModel.setPreferredCurrency(currency)
                            },
                            onSetThemeMode = { mode ->
                                viewModel.setThemeMode(mode)
                            },
                            onSetBiometricEnabled = { enabled ->
                                viewModel.setBiometricEnabled(enabled)
                            },
                            onExportBackupJson = { onSuccess, onError ->
                                viewModel.exportBackupJson(onSuccess, onError)
                            },
                            onRestoreBackupJson = { jsonString, onSuccess, onError ->
                                viewModel.restoreBackupJson(jsonString, onSuccess, onError)
                            },
                            onImportData = { data, onSuccess, onError ->
                                viewModel.importData(data, onSuccess, onError)
                            },
                            onDriveSyncClick = { account, onResult ->
                                viewModel.syncToGoogleDrive(this@MainActivity, account, onResult)
                            },
                            onDriveRestoreClick = { account, onResult ->
                                viewModel.restoreFromGoogleDrive(this@MainActivity, account, onResult)
                            },
                            onResetData = {
                                viewModel.clearAllData()
                            },
                            onResetTransactionsOnly = {
                                viewModel.clearTransactionsOnly()
                            },
                            onSignOut = {
                                viewModel.signOut()
                            }
                        )
                    }
                }
            }
        }
    }

    private fun triggerBiometricAuth(onSuccess: () -> Unit) {
        if (BiometricSecurityManager.canAuthenticate(this)) {
            BiometricSecurityManager.authenticate(
                activity = this,
                onSuccess = onSuccess,
                onError = { /* Keep locked */ }
            )
        } else {
            onSuccess()
        }
    }
}

