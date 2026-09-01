package com.selfbudget.app.feature.transaction

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import java.util.Locale
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.focusable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.selfbudget.app.core.ui.AccountSelectionModal
import com.selfbudget.app.core.ui.AddCustomAccountDialog
import com.selfbudget.app.core.util.Currencies
import com.selfbudget.app.core.util.VoiceParser
import com.selfbudget.app.data.model.AccountEntity
import com.selfbudget.app.ui.theme.getIncomeColor

/**
 * Full-screen modal transfer form: move money between two of the user's own accounts.
 */
@OptIn(ExperimentalMaterial3Api::class)
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
                .statusBarsPadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = {
                        Text(
                            text = "Transfer Between Accounts",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    actions = {
                        Button(
                            onClick = { doTransfer() },
                            enabled = isValid,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = getIncomeColor(),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Text("Transfer", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
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

                    // 1. Top Amount Entry Stepper (- $0.00 +) without card wrapping
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "TRANSFER AMOUNT ($currencySymbol)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.2.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            // Minus Button
                            Surface(
                                onClick = {
                                    val current = amountText.toDoubleOrNull() ?: 0.0
                                    val next = maxOf(0.0, current - 1.0)
                                    amountText = if (next == 0.0) "" else "%.2f".format(next)
                                },
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "Subtract Amount",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Centered Big Amount Field (44.sp ExtraBold)
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                OutlinedTextField(
                                    value = amountText,
                                    onValueChange = { input ->
                                        if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d{0,2}$"""))) {
                                            amountText = input
                                        }
                                    },
                                    placeholder = {
                                        Text(
                                            text = "0.00",
                                            style = TextStyle(
                                                fontSize = 44.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                                textAlign = TextAlign.Center
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    },
                                    prefix = {
                                        Text(
                                            text = currencySymbol,
                                            style = TextStyle(
                                                fontSize = 32.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            ),
                                            modifier = Modifier.padding(end = 2.dp)
                                        )
                                    },
                                    textStyle = TextStyle(
                                        fontSize = 44.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        textAlign = TextAlign.Center
                                    ),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Decimal,
                                        imeAction = ImeAction.Next
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onNext = { focusManager.moveFocus(FocusDirection.Next) }
                                    ),
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

                            // Plus Button
                            Surface(
                                onClick = {
                                    val current = amountText.toDoubleOrNull() ?: 0.0
                                    val next = current + 1.0
                                    amountText = "%.2f".format(next)
                                },
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add Amount",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Quick Preset Amount Chips ($5, $10, $25, $50, $100)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(5, 10, 25, 50, 100).forEach { preset ->
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                    modifier = Modifier.clickable {
                                        val currentVal = amountText.toDoubleOrNull() ?: 0.0
                                        amountText = "%.2f".format(currentVal + preset)
                                    }
                                ) {
                                    Text(
                                        text = "+$currencySymbol$preset",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    AccountPickerField(
                        label = "From Account",
                        account = fromAccount,
                        onClick = { pickingFrom = true }
                    )

                    AccountPickerField(
                        label = "To Account",
                        account = toAccount,
                        onClick = { pickingTo = true }
                    )

                    if (fromAccount != null && fromAccount?.id == toAccount?.id) {
                        Text(
                            text = "Pick two different accounts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Note (optional)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Voice Assistant Entry Button
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shadowElevation = 6.dp,
                            modifier = Modifier
                                .size(72.dp)
                                .clickable {
                                    focusManager.clearFocus(force = true)
                                    keyboardController?.hide()
                                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                                        putExtra(
                                            RecognizerIntent.EXTRA_PROMPT,
                                            "e.g. '100 dollars'"
                                        )
                                    }
                                    voiceLauncher.launch(intent)
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Voice Entry Mic",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Voice",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                        ) {
                            Text("Cancel", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        Button(
                            onClick = { doTransfer() },
                            enabled = isValid,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = getIncomeColor(),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Transfer", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
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
private fun AccountPickerField(label: String, account: AccountEntity?, onClick: () -> Unit) {
    androidx.compose.material3.Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = Icons.Default.AccountBalance,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = account?.let { "${it.name} (${Currencies.symbolFor(it.currencyCode)})" } ?: "Select an account",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Select",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
