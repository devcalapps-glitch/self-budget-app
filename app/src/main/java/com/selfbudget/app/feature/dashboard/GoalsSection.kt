package com.selfbudget.app.feature.dashboard

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import java.util.Locale
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Savings
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.selfbudget.app.core.util.VoiceParser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.selfbudget.app.data.model.AccountEntity
import com.selfbudget.app.data.model.AccountType
import com.selfbudget.app.data.model.GoalEntity
import com.selfbudget.app.ui.theme.ExpenseRed
import com.selfbudget.app.ui.theme.getIncomeColor

/**
 * Savings goals: a goal is tied to an account (usually a savings account/wallet) and its
 * progress is that account's live balance, PLUS goal.savedAmount - manual contributions added or
 * withdrawn by hand via the "+" button on each card. Manual contributions are the only progress
 * source for a goal with no linked account (e.g. a cash envelope).
 */
@Composable
fun GoalsSection(
    goals: List<GoalEntity>,
    accounts: List<AccountEntity>,
    accountBalances: Map<String, Double>,
    currencySymbol: String,
    onAddGoal: (name: String, targetAmount: Double, linkedAccountId: String?, targetDate: Long?) -> Unit,
    onDeleteGoal: (GoalEntity) -> Unit,
    onContributeToGoal: (GoalEntity, Double) -> Unit = { _, _ -> },
    onUpdateGoal: (GoalEntity) -> Unit = {},
    onAddCustomAccount: (AccountEntity) -> Unit = {}
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf<GoalEntity?>(null) }
    var contributingGoal by remember { mutableStateOf<GoalEntity?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = "Savings Goals", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            if (goals.isNotEmpty()) {
                val totalGoals = goals.size
                val goalsMetCount = goals.count { goal ->
                    val linkedAccount = accounts.firstOrNull { it.id == goal.linkedAccountId }
                    val accountAmount = linkedAccount?.let { accountBalances[it.id] ?: it.initialBalance } ?: 0.0
                    val currentAmount = accountAmount + goal.savedAmount
                    currentAmount >= goal.targetAmount && goal.targetAmount > 0
                }
                val totalTargetAmount = goals.sumOf { it.targetAmount }

                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Savings,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Goals Overview",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Column 1: Number of Goals
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = "Active Goals",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$totalGoals",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$currencySymbol%.2f Target".format(totalTargetAmount),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // Vertical Divider
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(48.dp)
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                            )

                            // Column 2: Goals Met
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 16.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    text = "Goals Met",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "$goalsMetCount",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = getIncomeColor()
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "/ $totalGoals",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (totalGoals > 0) "%.0f%% Completed".format((goalsMetCount.toDouble() / totalGoals) * 100) else "0% Completed",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = getIncomeColor(),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            if (goals.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Savings,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "No Savings Goals Set",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Set targets for Emergency Funds, Vacations, or Down Payments and track progress automatically.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { showAddDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Savings, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Add Savings Goal", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            } else {
                goals.forEach { goal ->
                    val linkedAccount = accounts.firstOrNull { it.id == goal.linkedAccountId }
                    val accountAmount = linkedAccount?.let { accountBalances[it.id] ?: it.initialBalance } ?: 0.0
                    val currentAmount = accountAmount + goal.savedAmount
                    val progress = if (goal.targetAmount > 0) (currentAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f) else 0f

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { editingGoal = goal },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Savings, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(goal.name, fontWeight = FontWeight.Bold)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { contributingGoal = goal }, modifier = Modifier.size(28.dp)) {
                                        Icon(
                                            imageVector = Icons.Default.Payments,
                                            contentDescription = "Add or Withdraw Contribution",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = getIncomeColor()
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$currencySymbol%.2f of $currencySymbol%.2f".format(currentAmount, goal.targetAmount) +
                                    (linkedAccount?.let { " • ${it.name}" } ?: " • Manual goal"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (linkedAccount != null && goal.savedAmount > 0.0) {
                                Text(
                                    text = "Includes $currencySymbol%.2f added manually".format(goal.savedAmount),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                            if (goal.targetDate != null && goal.targetDate > System.currentTimeMillis() && currentAmount < goal.targetAmount) {
                                val calNow = java.util.Calendar.getInstance()
                                val calTarget = java.util.Calendar.getInstance().apply { timeInMillis = goal.targetDate }
                                val monthsLeft = ((calTarget.get(java.util.Calendar.YEAR) - calNow.get(java.util.Calendar.YEAR)) * 12 +
                                        (calTarget.get(java.util.Calendar.MONTH) - calNow.get(java.util.Calendar.MONTH))).coerceAtLeast(1)
                                val remaining = (goal.targetAmount - currentAmount).coerceAtLeast(0.0)
                                val monthlyPace = remaining / monthsLeft
                                Spacer(modifier = Modifier.height(4.dp))
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = getIncomeColor().copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "🎯 Save $currencySymbol%.2f/mo (%d mos left)".format(monthlyPace, monthsLeft),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = getIncomeColor()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddGoalDialog(
            accounts = accounts,
            accountBalances = accountBalances,
            currencySymbol = currencySymbol,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, target, accountId, targetDate ->
                onAddGoal(name, target, accountId, targetDate)
                showAddDialog = false
            },
            onAddCustomAccount = onAddCustomAccount
        )
    }

    editingGoal?.let { goalToEdit ->
        EditGoalDialog(
            goal = goalToEdit,
            accounts = accounts,
            accountBalances = accountBalances,
            currencySymbol = currencySymbol,
            onDismiss = { editingGoal = null },
            onSave = { updatedGoal ->
                onUpdateGoal(updatedGoal)
                editingGoal = null
            },
            onDelete = { goalToDelete ->
                onDeleteGoal(goalToDelete)
                editingGoal = null
            },
            onAddCustomAccount = onAddCustomAccount
        )
    }

    contributingGoal?.let { goal ->
        ContributeDialog(
            goal = goal,
            currencySymbol = currencySymbol,
            onDismiss = { contributingGoal = null },
            onConfirm = { delta ->
                onContributeToGoal(goal, delta)
                contributingGoal = null
            }
        )
    }
}

// Same full-screen modal pattern as AddGoalDialog/AddCustomAccountDialog: persistent top bar
// (Close + inline Save), scrollable content, sticky bottom action bar - kept consistent with
// every other "modify something" flow in the app rather than a one-off compact dialog.
@Composable
private fun ContributeDialog(
    goal: GoalEntity,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirm: (delta: Double) -> Unit
) {
    var isAdding by remember { mutableStateOf(true) }
    var amountText by remember { mutableStateOf("") }
    val amount = amountText.toDoubleOrNull() ?: 0.0
    val isValid = amount > 0.0
    val projectedAmount = goal.savedAmount + (if (isAdding) amount else -amount)

    fun save() {
        if (isValid) onConfirm(if (isAdding) amount else -amount)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Persistent Top App Bar
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                    shadowElevation = 2.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = goal.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = { save() },
                            enabled = isValid,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Save", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }

                // Scrollable Form Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // 1. Top Amount Entry Stepper (- $0.00 +) without card wrapping
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isAdding) "CONTRIBUTION AMOUNT ($currencySymbol)" else "WITHDRAWAL AMOUNT ($currencySymbol)",
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
                                                color = (if (isAdding) getIncomeColor() else ExpenseRed).copy(alpha = 0.35f),
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
                                                color = if (isAdding) getIncomeColor() else ExpenseRed
                                            ),
                                            modifier = Modifier.padding(end = 2.dp)
                                        )
                                    },
                                    textStyle = TextStyle(
                                        fontSize = 44.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isAdding) getIncomeColor() else ExpenseRed,
                                        textAlign = TextAlign.Center
                                    ),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Decimal,
                                        imeAction = ImeAction.Next
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
                                color = (if (isAdding) getIncomeColor() else ExpenseRed).copy(alpha = 0.15f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add Amount",
                                        tint = if (isAdding) getIncomeColor() else ExpenseRed
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

                    // 2. Action Segmented Chips (Add vs Withdraw) below Amount
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "ACTION TYPE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilterChip(
                                selected = isAdding,
                                onClick = { isAdding = true },
                                label = { Text("➕ Add Contribution", fontWeight = FontWeight.Bold) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = !isAdding,
                                onClick = { isAdding = false },
                                label = { Text("➖ Withdraw", fontWeight = FontWeight.Bold) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // 3. Live Balance Progression Preview Card
                    Text(
                        text = "Projected Saved Total",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Current Saved",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$currencySymbol%.2f".format(goal.savedAmount),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "New Total After Action",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "$currencySymbol%.2f".format(projectedAmount.coerceAtLeast(0.0)),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isAdding) getIncomeColor() else com.selfbudget.app.ui.theme.ExpenseRed
                                )
                            }
                        }
                    }
                }

                // Sticky Bottom Action Bar
                Surface(
                    shadowElevation = 12.dp,
                    tonalElevation = 6.dp,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("Cancel", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        Button(
                            onClick = { save() },
                            enabled = isValid,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(48.dp)
                        ) {
                            Text(if (isAdding) "Add Contribution" else "Withdraw", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }
}

// Same full-screen modal pattern as AddCustomAccountDialog: persistent top bar (Close + Save),
// scrollable form with a live preview card, and a sticky bottom action bar - kept consistent so
// every "add X" flow in the app looks and behaves the same way.
@Composable
internal fun AddGoalDialog(
    accounts: List<AccountEntity>,
    accountBalances: Map<String, Double>,
    currencySymbol: String = "$",
    onDismiss: () -> Unit,
    onConfirm: (name: String, targetAmount: Double, linkedAccountId: String?, targetDate: Long?) -> Unit,
    onAddCustomAccount: (AccountEntity) -> Unit = {}
) {
    val assetAccounts = remember(accounts) {
        accounts.filter {
            it.type == AccountType.CHECKING ||
            it.type == AccountType.SAVINGS ||
            it.type == AccountType.CASH
        }
    }
    var name by remember { mutableStateOf("") }
    var targetText by remember { mutableStateOf("") }
    var linkedAccount by remember(assetAccounts) { mutableStateOf<AccountEntity?>(assetAccounts.firstOrNull()) }
    var selectedMonths by remember { mutableStateOf<Int?>(null) }
    var targetDate by remember { mutableStateOf<Long?>(null) }
    var pickingAccount by remember { mutableStateOf(false) }
    var showAddAccountDialog by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenMatches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = spokenMatches?.firstOrNull()
            if (spokenText != null && spokenText.isNotBlank()) {
                val parsed = VoiceParser.parseSpokenText(spokenText)
                if (parsed != null) {
                    name = parsed.title
                    targetText = "%.2f".format(parsed.amount)
                } else {
                    val numberMatch = Regex("""\d+(\.\d+)?""").find(spokenText)?.value
                    if (numberMatch != null) {
                        targetText = numberMatch
                        name = spokenText.replace(numberMatch, "").replace("dollars", "").trim()
                    } else {
                        name = spokenText
                    }
                }
            }
        }
    }

    val target = targetText.toDoubleOrNull() ?: 0.0
    val isValid = name.isNotBlank() && target > 0.0
    val currentAmount = linkedAccount?.let { accountBalances[it.id] ?: it.initialBalance } ?: 0.0
    val progress = if (target > 0.0) (currentAmount / target).toFloat().coerceIn(0f, 1f) else 0f

    fun save() {
        if (isValid) onConfirm(name.trim(), target, linkedAccount?.id, targetDate)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Persistent Top App Bar
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                    shadowElevation = 2.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Add Savings Goal",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = { save() },
                            enabled = isValid,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Save", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }

                // Scrollable Form Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // 1. Top Amount Entry Stepper (- $0.00 +) without card wrapping
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "TARGET SAVINGS AMOUNT ($currencySymbol)",
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
                                    val current = targetText.toDoubleOrNull() ?: 0.0
                                    val next = maxOf(0.0, current - 50.0)
                                    targetText = if (next == 0.0) "" else "%.2f".format(next)
                                },
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "Subtract Target",
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
                                    value = targetText,
                                    onValueChange = { input ->
                                        if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d{0,2}$"""))) {
                                            targetText = input
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
                                    val current = targetText.toDoubleOrNull() ?: 0.0
                                    val next = current + 50.0
                                    targetText = "%.2f".format(next)
                                },
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add Target",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Quick Preset Amount Chips ($100, $500, $1000, $5000)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(100, 500, 1000, 5000).forEach { preset ->
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                    modifier = Modifier.clickable {
                                        val currentVal = targetText.toDoubleOrNull() ?: 0.0
                                        targetText = "%.2f".format(currentVal + preset)
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

                    // Goal Name Field
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Goal Name") },
                        placeholder = { Text("e.g. Emergency Fund, Vacation") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Target Timeline (Optional Pacing)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "TARGET TIMELINE (OPTIONAL)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(
                                "No Deadline" to null,
                                "3 Mos" to 3,
                                "6 Mos" to 6,
                                "1 Yr" to 12,
                                "2 Yrs" to 24
                            ).forEach { (label, months) ->
                                val isSelected = if (months == null) targetDate == null else selectedMonths == months
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                    modifier = Modifier.clickable {
                                        if (months == null) {
                                            targetDate = null
                                            selectedMonths = null
                                        } else {
                                            selectedMonths = months
                                            val cal = java.util.Calendar.getInstance()
                                            cal.add(java.util.Calendar.MONTH, months)
                                            targetDate = cal.timeInMillis
                                        }
                                    }
                                ) {
                                    Text(
                                        text = label,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // Live Goal Card Preview
                    Text(
                        text = "Live Goal Preview",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Savings,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = name.ifBlank { "Goal Name" },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (name.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = getIncomeColor()
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "$currencySymbol%.2f of $currencySymbol%.2f".format(currentAmount, target),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Linked Account Field (Taps to open AccountSelectionModal)
                    Text(
                        text = "Track Progress From (Checking / Savings / Cash)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { pickingAccount = true }
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
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = linkedAccount?.name ?: "No account (manual goal)",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Select",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

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
                                            "e.g. 'Emergency Fund 5000 dollars'"
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
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                        ) {
                            Text("Cancel", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        Button(
                            onClick = { save() },
                            enabled = isValid,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Add Goal", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(150.dp))
                }
            }
        }
    }

    if (pickingAccount) {
        AccountSelectionModal(
            accounts = assetAccounts,
            selectedAccount = linkedAccount,
            accountBalances = accountBalances,
            onDismiss = { pickingAccount = false },
            onSelectAccount = { acc -> linkedAccount = acc; pickingAccount = false },
            onAddCustomAccount = { pickingAccount = false; showAddAccountDialog = true }
        )
    }

    if (showAddAccountDialog) {
        AddCustomAccountDialog(
            onDismiss = { showAddAccountDialog = false },
            onConfirm = { newAcc ->
                showAddAccountDialog = false
                onAddCustomAccount(newAcc)
                linkedAccount = newAcc
            }
        )
    }
}

private data class GoalEditSnapshot(
    val name: String,
    val targetText: String,
    val linkedAccountId: String?
)

@Composable
private fun EditGoalDialog(
    goal: GoalEntity,
    accounts: List<AccountEntity>,
    accountBalances: Map<String, Double>,
    currencySymbol: String = "$",
    onDismiss: () -> Unit,
    onSave: (GoalEntity) -> Unit,
    onDelete: (GoalEntity) -> Unit,
    onAddCustomAccount: (AccountEntity) -> Unit = {}
) {
    val assetAccounts = remember(accounts) {
        accounts.filter {
            it.type == AccountType.CHECKING ||
            it.type == AccountType.SAVINGS ||
            it.type == AccountType.CASH
        }
    }
    var name by remember(goal.id) { mutableStateOf(goal.name) }
    var targetText by remember(goal.id) { mutableStateOf("%.2f".format(goal.targetAmount)) }
    var linkedAccount by remember(goal.id, assetAccounts) {
        mutableStateOf(assetAccounts.firstOrNull { it.id == goal.linkedAccountId })
    }
    var pickingAccount by remember { mutableStateOf(false) }
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmationModal by remember { mutableStateOf(false) }

    val target = targetText.toDoubleOrNull() ?: 0.0
    val isValid = name.isNotBlank() && target > 0.0
    val currentAmount = (linkedAccount?.let { accountBalances[it.id] ?: it.initialBalance } ?: 0.0) + goal.savedAmount
    val progress = if (target > 0.0) (currentAmount / target).toFloat().coerceIn(0f, 1f) else 0f

    // Dialog opens read-only; tapping "Edit" is the deliberate action that unlocks the form.
    var isEditMode by remember { mutableStateOf(false) }
    var editBaseline by remember { mutableStateOf<GoalEditSnapshot?>(null) }

    fun captureEditSnapshot() = GoalEditSnapshot(
        name = name,
        targetText = targetText,
        linkedAccountId = linkedAccount?.id
    )

    val isDirty = editBaseline != null && editBaseline != captureEditSnapshot()

    fun enterEditMode() {
        editBaseline = captureEditSnapshot()
        isEditMode = true
    }

    fun save() {
        if (isValid) {
            onSave(
                goal.copy(
                    name = name.trim(),
                    targetAmount = target,
                    linkedAccountId = linkedAccount?.id
                )
            )
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Persistent Top App Bar
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                    shadowElevation = 2.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isEditMode) "Edit Savings Goal" else "Savings Goal Details",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                if (isEditMode) {
                                    save()
                                } else {
                                    enterEditMode()
                                }
                            },
                            enabled = !isEditMode || (isDirty && isValid),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (isEditMode) "Save" else "Edit", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }

                // Scrollable Form Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    if (!isEditMode) {
                        GoalViewModeSummary(
                            name = name,
                            targetText = targetText,
                            currencySymbol = currencySymbol,
                            linkedAccountName = linkedAccount?.name
                        )
                    } else {

                    // 1. Top Amount Entry Stepper (- $0.00 +) in 44.sp ExtraBold font
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "TARGET AMOUNT ($currencySymbol)",
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
                                    val current = targetText.toDoubleOrNull() ?: 0.0
                                    val next = maxOf(0.0, current - 50.0)
                                    targetText = if (next == 0.0) "" else "%.2f".format(next)
                                },
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "Subtract Target",
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
                                    value = targetText,
                                    onValueChange = { input ->
                                        if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d{0,2}$"""))) {
                                            targetText = input
                                        }
                                    },
                                    placeholder = {
                                        Text(
                                            text = "0.00",
                                            style = TextStyle(
                                                fontSize = 44.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = getIncomeColor().copy(alpha = 0.35f),
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
                                                color = getIncomeColor()
                                            ),
                                            modifier = Modifier.padding(end = 2.dp)
                                        )
                                    },
                                    textStyle = TextStyle(
                                        fontSize = 44.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = getIncomeColor(),
                                        textAlign = TextAlign.Center
                                    ),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Decimal,
                                        imeAction = ImeAction.Next
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
                                    val current = targetText.toDoubleOrNull() ?: 0.0
                                    val next = current + 50.0
                                    targetText = "%.2f".format(next)
                                },
                                shape = CircleShape,
                                color = getIncomeColor().copy(alpha = 0.15f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add Target",
                                        tint = getIncomeColor()
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Quick Preset Amount Chips
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(100, 500, 1000, 5000).forEach { preset ->
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                    modifier = Modifier.clickable {
                                        val currentVal = targetText.toDoubleOrNull() ?: 0.0
                                        targetText = "%.2f".format(currentVal + preset)
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

                    // 2. Goal Name Field
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Goal Name") },
                        placeholder = { Text("e.g. Emergency Fund, Vacation") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 3. Linked Account Field
                    Text(
                        text = "Track Progress From (Checking / Savings / Cash)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { pickingAccount = true }
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
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = linkedAccount?.name ?: "No account (manual goal)",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Select",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    } // end isEditMode form fields

                    // 4. Live Goal Preview (Hero Card)
                    Text(
                        text = "Live Goal Preview",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Savings,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = name.ifBlank { "Goal Name" },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (name.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = getIncomeColor()
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "$currencySymbol%.2f of $currencySymbol%.2f".format(currentAmount, target),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Action Buttons Layout:
                    // Row 1: [ Cancel ] | [ Save Goal Changes ]
                    // Row 2: [ 🗑️ Delete Savings Goal ]
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                            ) {
                                Text(if (isEditMode) "Cancel" else "Close", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }

                            Button(
                                onClick = {
                                    if (isEditMode) {
                                        save()
                                    } else {
                                        enterEditMode()
                                    }
                                },
                                enabled = !isEditMode || (isDirty && isValid),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(if (isEditMode) "Save Changes" else "Edit Goal", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }

                        OutlinedButton(
                            onClick = { showDeleteConfirmationModal = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.5.dp, ExpenseRed.copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseRed)
                        ) {
                            Text("Delete Savings Goal", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }

    if (pickingAccount) {
        AccountSelectionModal(
            accounts = assetAccounts,
            selectedAccount = linkedAccount,
            accountBalances = accountBalances,
            onDismiss = { pickingAccount = false },
            onSelectAccount = { acc -> linkedAccount = acc; pickingAccount = false },
            onAddCustomAccount = { pickingAccount = false; showAddAccountDialog = true }
        )
    }

    if (showAddAccountDialog) {
        AddCustomAccountDialog(
            onDismiss = { showAddAccountDialog = false },
            onConfirm = { newAcc ->
                showAddAccountDialog = false
                onAddCustomAccount(newAcc)
                linkedAccount = newAcc
            }
        )
    }

    // Delete Confirmation Modal
    if (showDeleteConfirmationModal) {
        Dialog(
            onDismissRequest = { showDeleteConfirmationModal = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Text(
                        text = "Delete Savings Goal?",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Are you sure you want to delete \"${goal.name}\"? This action cannot be undone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showDeleteConfirmationModal = false },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancel", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                showDeleteConfirmationModal = false
                                onDelete(goal)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Delete", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalViewModeSummary(
    name: String,
    targetText: String,
    currencySymbol: String,
    linkedAccountName: String?
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "TARGET AMOUNT",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$currencySymbol${targetText.ifBlank { "0.00" }}",
                style = TextStyle(fontSize = 44.sp, fontWeight = FontWeight.ExtraBold, color = getIncomeColor())
            )
        }

        GoalInfoRow(icon = Icons.Default.Savings, label = "Goal Name", value = name)
        GoalInfoRow(
            icon = Icons.Default.AccountBalance,
            label = "Track Progress From",
            value = linkedAccountName ?: "No account (manual goal)"
        )
    }
}

@Composable
private fun GoalInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = valueColor
                )
            }
        }
    }
}
