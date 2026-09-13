package com.selfbudget.app.feature.transaction

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import java.util.Locale
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.focusable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.selfbudget.app.core.ui.AccountSelectionModal
import com.selfbudget.app.core.ui.AddCustomAccountDialog
import com.selfbudget.app.core.util.toWordTitleCase
import com.selfbudget.app.core.ui.components.EntryType
import com.selfbudget.app.core.ui.components.PrimaryPillButton
import com.selfbudget.app.core.ui.components.QuickAmountChips
import com.selfbudget.app.core.ui.components.SecondaryPillButton
import com.selfbudget.app.core.ui.components.TransactionAmountHero
import com.selfbudget.app.core.util.Currencies
import com.selfbudget.app.core.util.VoiceParser
import com.selfbudget.app.data.model.AccountEntity
import com.selfbudget.app.ui.theme.Ramp
import com.selfbudget.app.ui.theme.ShapeCard
import com.selfbudget.app.ui.theme.ShapeChip
import com.selfbudget.app.ui.theme.getIncomeColor

/**
 * Full-screen modal transfer form: move money between two of the user's own accounts.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransferDialog(
    accounts: List<AccountEntity>,
    accountBalances: Map<String, Double>,
    currencySymbol: String = "$",
    onDismiss: () -> Unit,
    onConfirm: (fromAccountId: String, toAccountId: String, amount: Double, note: String?) -> Unit,
    onAddCustomAccount: ((AccountEntity) -> Unit)? = null
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var fromAccount by remember { mutableStateOf(accounts.firstOrNull()) }
    var toAccount by remember { mutableStateOf(accounts.getOrNull(1) ?: accounts.firstOrNull()) }
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var pickingFrom by remember { mutableStateOf(false) }
    var pickingTo by remember { mutableStateOf(false) }
    var showNewAccountDialogForFrom by remember { mutableStateOf(false) }
    var showNewAccountDialogForTo by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val focusAnchor = remember { FocusRequester() }

    LaunchedEffect(pickingFrom, pickingTo) {
        runCatching { focusAnchor.requestFocus() }
        keyboardController?.hide()
    }

    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenMatches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = spokenMatches?.firstOrNull()
            if (spokenText != null && spokenText.isNotBlank()) {
                val parsed = VoiceParser.parseSpokenText(spokenText)
                if (parsed != null) {
                    amountText = "%.2f".format(parsed.amount)
                } else {
                    val numberMatch = Regex("""\d+(\.\d+)?""").find(spokenText)?.value
                    if (numberMatch != null) {
                        amountText = numberMatch
                    }
                }
            }
        }
    }

    val amount = amountText.toDoubleOrNull()
    val isValid = fromAccount != null && toAccount != null && fromAccount?.id != toAccount?.id && amount != null && amount > 0.0

    fun doTransfer() {
        if (isValid) {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
            onConfirm(fromAccount!!.id, toAccount!!.id, amount!!, note.ifBlank { null })
        }
    }

    fun swapAccounts() {
        val temp = fromAccount
        fromAccount = toAccount
        toAccount = temp
    }

    val isDark = isSystemInDarkTheme()
    val primaryColor = MaterialTheme.colorScheme.primary

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnClickOutside = true,
            dismissOnBackPress = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .drawWithContent {
                    drawContent()
                    if (!isDark) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = 0.035f),
                                    Color.Transparent
                                ),
                                center = Offset(size.width * 0.5f, 160f),
                                radius = size.width * 0.75f
                            )
                        )
                    }
                },
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = {
                        Text(
                            text = "Transfer Funds",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    // No header Transfer action (spec §14/§16): the header holds only close +
                    // title; the single primary action lives in the footer button below.
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Inert focus target used to steal focus away from real fields when a selection modal opens/closes
                    Box(
                        modifier = Modifier
                            .size(0.dp)
                            .focusRequester(focusAnchor)
                            .focusable()
                    )

                    // 1. Top Amount Hero Card
                    TransactionAmountHero(
                        type = EntryType.Transfer,
                        amountText = amountText,
                        onAmountChange = { amountText = it },
                        currencySymbol = currencySymbol,
                        badgeText = "TRANSFER AMOUNT",
                        onNext = { focusManager.moveFocus(FocusDirection.Next) }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    QuickAmountChips(
                        presets = listOf(10, 25, 50, 100, 250),
                        currencySymbol = currencySymbol,
                        onPick = { preset ->
                            val currentVal = amountText.toDoubleOrNull() ?: 0.0
                            amountText = "%.2f".format(currentVal + preset)
                        }
                    )

                    // 2. Grouped Transfer Account Details Card
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "TRANSFER ACCOUNTS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )

                        Surface(
                            shape = ShapeCard,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // From Account Row
                                TransferAccountRow(
                                    title = "From (Source Account)",
                                    account = fromAccount,
                                    currencySymbol = currencySymbol,
                                    accountBalances = accountBalances,
                                    isSource = true,
                                    onClick = {
                                        focusManager.clearFocus(force = true)
                                        keyboardController?.hide()
                                        pickingFrom = true
                                    }
                                )

                                // Divider with central Swap button
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    HorizontalDivider(
                                        modifier = Modifier.fillMaxWidth(),
                                        thickness = 1.dp,
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                                    )

                                    Surface(
                                        onClick = { swapAccounts() },
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                                        shadowElevation = 2.dp,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.SwapVert,
                                                contentDescription = "Swap Source and Destination",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }

                                // To Account Row
                                TransferAccountRow(
                                    title = "To (Destination Account)",
                                    account = toAccount,
                                    currencySymbol = currencySymbol,
                                    accountBalances = accountBalances,
                                    isSource = false,
                                    onClick = {
                                        focusManager.clearFocus(force = true)
                                        keyboardController?.hide()
                                        pickingTo = true
                                    }
                                )
                            }
                        }
                    }

                    if (fromAccount != null && fromAccount?.id == toAccount?.id) {
                        Surface(
                            shape = ShapeChip,
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Source and destination accounts must be different.",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }
                    }

                    // 3. Grouped Note & Memo Section
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "NOTE & MEMO",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )

                        Surface(
                            shape = ShapeCard,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.EditNote,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                OutlinedTextField(
                                    value = note,
                                    onValueChange = { note = it.toWordTitleCase() },
                                    label = { Text("Transfer Note (optional)") },
                                    placeholder = { Text("e.g. Monthly savings transfer") },
                                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent,
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Action Buttons (spec §8: one primary + one secondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SecondaryPillButton(
                            text = "Cancel",
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                        )

                        PrimaryPillButton(
                            text = "Save transfer",
                            onClick = { doTransfer() },
                            enabled = isValid,
                            ramp = Ramp.Teal,
                            modifier = Modifier
                                .weight(1.3f)
                                .height(50.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(150.dp))
                }
            }
        }
    }

    if (pickingFrom) {
        AccountSelectionModal(
            accounts = accounts,
            selectedAccount = fromAccount,
            currencySymbol = currencySymbol,
            accountBalances = accountBalances,
            onDismiss = { pickingFrom = false },
            onSelectAccount = { acc -> fromAccount = acc; pickingFrom = false },
            onAddCustomAccount = {
                pickingFrom = false
                showNewAccountDialogForFrom = true
            }
        )
    }

    if (pickingTo) {
        AccountSelectionModal(
            accounts = accounts,
            selectedAccount = toAccount,
            currencySymbol = currencySymbol,
            accountBalances = accountBalances,
            onDismiss = { pickingTo = false },
            onSelectAccount = { acc -> toAccount = acc; pickingTo = false },
            onAddCustomAccount = {
                pickingTo = false
                showNewAccountDialogForTo = true
            }
        )
    }

    if (showNewAccountDialogForFrom) {
        AddCustomAccountDialog(
            currencySymbol = currencySymbol,
            onDismiss = { showNewAccountDialogForFrom = false },
            onConfirm = { newAcc ->
                onAddCustomAccount?.invoke(newAcc)
                fromAccount = newAcc
                showNewAccountDialogForFrom = false
            }
        )
    }

    if (showNewAccountDialogForTo) {
        AddCustomAccountDialog(
            currencySymbol = currencySymbol,
            onDismiss = { showNewAccountDialogForTo = false },
            onConfirm = { newAcc ->
                onAddCustomAccount?.invoke(newAcc)
                toAccount = newAcc
                showNewAccountDialogForTo = false
            }
        )
    }
}

@Composable
private fun TransferAccountRow(
    title: String,
    account: AccountEntity?,
    currencySymbol: String,
    accountBalances: Map<String, Double>,
    isSource: Boolean,
    onClick: () -> Unit
) {
    val accColor = remember(account) {
        if (account == null) Color.Gray
        else {
            try {
                Color(android.graphics.Color.parseColor(account.colorHex))
            } catch (e: Exception) {
                Color(0xFF2196F3)
            }
        }
    }

    val rawBalance = account?.let { accountBalances[it.id] ?: it.initialBalance } ?: 0.0
    val sym = account?.let { if (it.currencyCode.isNotBlank()) Currencies.symbolFor(it.currencyCode) else currencySymbol } ?: currencySymbol

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Surface(
                shape = CircleShape,
                color = accColor.copy(alpha = 0.15f),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = accColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = account?.name ?: "Select account",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (account != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                if (account != null) {
                    Text(
                        text = "Balance: $sym%.2f".format(rawBalance),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }

        Icon(
            imageVector = Icons.Default.ArrowDropDown,
            contentDescription = "Select",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

