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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.HorizontalDivider
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
import com.selfbudget.app.data.model.AccountEntity
import com.selfbudget.app.data.model.AccountType
import com.selfbudget.app.data.model.GoalEntity
import com.selfbudget.app.core.ui.components.DestructivePillButton
import com.selfbudget.app.core.ui.components.DoneChip
import com.selfbudget.app.core.ui.components.EntryType
import com.selfbudget.app.core.ui.components.FieldRow
import com.selfbudget.app.core.ui.components.GrayIconTile
import com.selfbudget.app.core.ui.components.PrimaryPillButton
import com.selfbudget.app.core.ui.components.QuickAmountChips
import com.selfbudget.app.core.ui.components.RampIconTile
import com.selfbudget.app.core.ui.components.SecondaryPillButton
import com.selfbudget.app.core.ui.components.SectionRowDivider
import com.selfbudget.app.core.ui.components.TransactionAmountHero
import com.selfbudget.app.feature.search.FilterChipGroup
import com.selfbudget.app.ui.theme.Ramp
import com.selfbudget.app.ui.theme.SelfBudgetType
import com.selfbudget.app.ui.theme.ShapeCard
import com.selfbudget.app.ui.theme.ShapeChip
import com.selfbudget.app.ui.theme.ShapeHero
import com.selfbudget.app.ui.theme.ShapePage
import com.selfbudget.app.ui.theme.ShapePill
import com.selfbudget.app.ui.theme.ShapeTile
import com.selfbudget.app.ui.theme.getAccentColor
import com.selfbudget.app.ui.theme.getExpenseColor
import com.selfbudget.app.ui.theme.getIncomeColor
import com.selfbudget.app.ui.theme.isAppInDarkTheme
import com.selfbudget.app.ui.theme.secondaryText
import com.selfbudget.app.ui.theme.tintFill
import com.selfbudget.app.ui.theme.titleText

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
            Text(text = "Savings Goals", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)

            if (goals.isNotEmpty()) {
                val totalGoals = goals.size
                val goalsMetCount = goals.count { goal ->
                    val linkedAccount = accounts.firstOrNull { it.id == goal.linkedAccountId }
                    val accountAmount = linkedAccount?.let { accountBalances[it.id] ?: it.initialBalance } ?: 0.0
                    val currentAmount = accountAmount + goal.savedAmount
                    currentAmount >= goal.targetAmount && goal.targetAmount > 0
                }
                val totalTargetAmount = goals.sumOf { it.targetAmount }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = ShapePage,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
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
                                    fontWeight = FontWeight.Medium,
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
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    letterSpacing = 0.8.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$totalGoals",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Medium,
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
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
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
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    letterSpacing = 0.8.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "$goalsMetCount",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = getIncomeColor()
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "/ $totalGoals",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (totalGoals > 0) "%.0f%% Completed".format((goalsMetCount.toDouble() / totalGoals) * 100) else "0% Completed",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = getIncomeColor(),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            if (goals.isEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = ShapePage,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
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
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Savings,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "No Savings Goals Set",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
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
                            shape = ShapeCard,
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = getAccentColor(),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Add Savings Goal", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        }
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "ACTIVE SAVINGS GOALS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                    )

                    Surface(
                        shape = ShapePage,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            goals.forEachIndexed { index, goal ->
                                val linkedAccount = accounts.firstOrNull { it.id == goal.linkedAccountId }
                                val accountAmount = linkedAccount?.let { accountBalances[it.id] ?: it.initialBalance } ?: 0.0
                                val currentAmount = accountAmount + goal.savedAmount
                                val progress = if (goal.targetAmount > 0) (currentAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f) else 0f

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { editingGoal = goal }
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                shape = CircleShape,
                                                color = getIncomeColor().copy(alpha = 0.15f),
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.Savings,
                                                        contentDescription = null,
                                                        tint = getIncomeColor(),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(goal.name, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyLarge)
                                                Text(
                                                    text = "$currencySymbol%.2f of $currencySymbol%.2f".format(currentAmount, goal.targetAmount) +
                                                            (linkedAccount?.let { " • ${it.name}" } ?: " • Manual goal"),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Surface(
                                            onClick = { contributingGoal = goal },
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Payments,
                                                    contentDescription = "Add or Withdraw Contribution",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(17.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = getIncomeColor(),
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )

                                    if (linkedAccount != null && goal.savedAmount > 0.0) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Includes $currencySymbol%.2f added manually".format(goal.savedAmount),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }

                                    // Goal met (spec §22): swap the milestone-pace hint for a passive
                                    // "Goal met" chip instead of an overflowing percentage.
                                    if (currentAmount >= goal.targetAmount && goal.targetAmount > 0.0) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        DoneChip(text = "Goal met")
                                    } else if (goal.targetDate != null && goal.targetDate > System.currentTimeMillis()) {
                                        val calNow = java.util.Calendar.getInstance()
                                        val calTarget = java.util.Calendar.getInstance().apply { timeInMillis = goal.targetDate }
                                        val monthsLeft = ((calTarget.get(java.util.Calendar.YEAR) - calNow.get(java.util.Calendar.YEAR)) * 12 +
                                                (calTarget.get(java.util.Calendar.MONTH) - calNow.get(java.util.Calendar.MONTH))).coerceAtLeast(1)
                                        val remaining = (goal.targetAmount - currentAmount).coerceAtLeast(0.0)
                                        val monthlyPace = remaining / monthsLeft
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Surface(
                                            shape = ShapeTile,
                                            color = getIncomeColor().copy(alpha = 0.12f)
                                        ) {
                                            Text(
                                                text = "Save $currencySymbol%.2f/mo (%d mos left)".format(monthlyPace, monthsLeft),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                style = SelfBudgetType.badge,
                                                color = getIncomeColor()
                                            )
                                        }
                                    }
                                }

                                if (index < goals.size - 1) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = 62.dp),
                                        thickness = 1.dp,
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
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
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Button(
                            onClick = { save() },
                            enabled = isValid,
                            shape = ShapeTile
                        ) {
                            Text("Save", fontWeight = FontWeight.Medium, fontSize = 14.sp)
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
                    // 1. Hero Amount Card — shared component (spec §16): every amount-entry
                    // card in the app uses this one implementation, not a per-screen copy.
                    TransactionAmountHero(
                        type = if (isAdding) EntryType.Income else EntryType.Expense,
                        amountText = amountText,
                        onAmountChange = { amountText = it },
                        currencySymbol = currencySymbol,
                        badgeText = if (isAdding) "CONTRIBUTION AMOUNT" else "WITHDRAWAL AMOUNT"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    QuickAmountChips(
                        presets = listOf(5, 10, 25, 50, 100),
                        currencySymbol = currencySymbol,
                        onPick = { preset ->
                            val currentVal = amountText.toDoubleOrNull() ?: 0.0
                            amountText = "%.2f".format(currentVal + preset)
                        }
                    )

                    // 2. Action Type & Projection in Grouped Surface
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "ACTION & PROJECTION",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                        )

                        Surface(
                            shape = ShapePage,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    FilterChip(
                                        selected = isAdding,
                                        onClick = { isAdding = true },
                                        label = { Text("➕ Add Contribution", fontWeight = FontWeight.Medium) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    FilterChip(
                                        selected = !isAdding,
                                        onClick = { isAdding = false },
                                        label = { Text("➖ Withdraw", fontWeight = FontWeight.Medium) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Savings,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "Current Saved",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "$currencySymbol%.2f".format(goal.savedAmount),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = CircleShape,
                                            color = (if (isAdding) getIncomeColor() else getExpenseColor()).copy(alpha = 0.15f),
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Payments,
                                                    contentDescription = null,
                                                    tint = if (isAdding) getIncomeColor() else getExpenseColor(),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "New Total After Action",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Text(
                                        text = "$currencySymbol%.2f".format(projectedAmount.coerceAtLeast(0.0)),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isAdding) getIncomeColor() else getExpenseColor()
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(150.dp))
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
                            shape = ShapeCard,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                        ) {
                            Text("Cancel", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                        }

                        Button(
                            onClick = { save() },
                            enabled = isValid,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isAdding) getIncomeColor() else MaterialTheme.colorScheme.primary,
                                contentColor = Color.White
                            ),
                            shape = ShapeCard,
                            modifier = Modifier
                                .weight(1.3f)
                                .height(54.dp)
                        ) {
                            Text(if (isAdding) "Add Contribution" else "Withdraw", fontWeight = FontWeight.Medium, fontSize = 15.sp)
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
                // No header Save action (spec §14/§16): the header holds only close +
                // title. Save is triggered from the footer button below.
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
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
                            fontWeight = FontWeight.Medium
                        )
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
                    val isDark = isAppInDarkTheme()

                    // 1. Target Amount Entry Hero — shared component (spec §16): every
                    // amount-entry card in the app uses this one implementation.
                    TransactionAmountHero(
                        type = EntryType.Income,
                        amountText = targetText,
                        onAmountChange = { targetText = it },
                        currencySymbol = currencySymbol,
                        badgeText = "TARGET SAVINGS AMOUNT",
                        stepAmount = 50.0
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    QuickAmountChips(
                        presets = listOf(100, 500, 1000, 5000),
                        currencySymbol = currencySymbol,
                        onPick = { preset ->
                            val currentVal = targetText.toDoubleOrNull() ?: 0.0
                            targetText = "%.2f".format(currentVal + preset)
                        }
                    )

                    // 2. Goal Details Card
                    Surface(
                        shape = ShapeCard,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                GrayIconTile(icon = Icons.Default.Savings, size = 36.dp, iconSize = 18.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it.toWordTitleCase() },
                                    placeholder = {
                                        Text(
                                            "e.g. Emergency Fund, Vacation",
                                            style = SelfBudgetType.body,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    },
                                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                                    singleLine = true,
                                    textStyle = SelfBudgetType.rowTitle.copy(color = MaterialTheme.colorScheme.onSurface),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent,
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            SectionRowDivider(modifier = Modifier.padding(start = 62.dp))

                            FieldRow(
                                icon = Icons.Default.AccountBalance,
                                label = "Track progress from",
                                value = linkedAccount?.name ?: "No account (manual goal)",
                                isPlaceholder = linkedAccount == null,
                                showChevron = true,
                                onClick = { pickingAccount = true }
                            )
                        }
                    }

                    // 3. Target Timeline (Optional Pacing)
                    FilterChipGroup(
                        title = "Target timeline (optional)",
                        options = listOf<Int?>(null, 3, 6, 12, 24),
                        optionLabel = { months ->
                            when (months) {
                                null -> "No deadline"
                                3 -> "3 mos"
                                6 -> "6 mos"
                                12 -> "1 yr"
                                24 -> "2 yrs"
                                else -> "$months mos"
                            }
                        },
                        isSelected = { months -> if (months == null) targetDate == null else selectedMonths == months },
                        onSelect = { months ->
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
                    )

                    // 4. Live Goal Preview
                    Surface(
                        shape = ShapeCard,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RampIconTile(icon = Icons.Default.Savings, ramp = Ramp.Teal, size = 36.dp, iconSize = 18.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = name.ifBlank { "Goal name" },
                                    style = SelfBudgetType.rowTitle,
                                    color = if (name.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Before a target is entered, show an empty track and placeholder
                            // text — never live numbers against a $0.00 target (spec §22).
                            LinearProgressIndicator(
                                progress = { if (target > 0.0) progress else 0f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = Ramp.Teal.c400
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = if (target > 0.0) {
                                    "$currencySymbol%.2f of $currencySymbol%.2f".format(currentAmount, target)
                                } else {
                                    "Enter a target amount to preview"
                                },
                                style = SelfBudgetType.body,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Action Buttons (spec §14: one primary + one secondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SecondaryPillButton(
                            text = "Cancel",
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .height(54.dp)
                        )

                        PrimaryPillButton(
                            text = "Add goal",
                            onClick = { save() },
                            enabled = isValid,
                            ramp = Ramp.Teal,
                            modifier = Modifier
                                .weight(1.3f)
                                .height(54.dp)
                        )
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
                // No header Edit/Save action (spec §14/§16): the header holds only close +
                // title. Edit/Save is triggered from the footer button below.
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
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
                            fontWeight = FontWeight.Medium
                        )
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
                            currentAmount = currentAmount,
                            target = target,
                            progress = progress,
                            currencySymbol = currencySymbol,
                            linkedAccountName = linkedAccount?.name,
                            onEditClick = { enterEditMode() },
                            onDeleteClick = { showDeleteConfirmationModal = true },
                            onClose = onDismiss
                        )
                    } else {
                        val isDark = isAppInDarkTheme()

                        // 1. Target Amount Entry Hero — shared component (spec §16): every
                        // amount-entry card in the app uses this one implementation.
                        TransactionAmountHero(
                            type = EntryType.Income,
                            amountText = targetText,
                            onAmountChange = { targetText = it },
                            currencySymbol = currencySymbol,
                            badgeText = "TARGET AMOUNT",
                            stepAmount = 50.0
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        QuickAmountChips(
                            presets = listOf(100, 500, 1000, 5000),
                            currencySymbol = currencySymbol,
                            onPick = { preset ->
                                val currentVal = targetText.toDoubleOrNull() ?: 0.0
                                targetText = "%.2f".format(currentVal + preset)
                            }
                        )

                        // 2. Goal Details Card
                        Surface(
                            shape = ShapeCard,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    GrayIconTile(icon = Icons.Default.Savings, size = 36.dp, iconSize = 18.dp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    OutlinedTextField(
                                        value = name,
                                        onValueChange = { name = it.toWordTitleCase() },
                                        placeholder = {
                                            Text(
                                                "e.g. Emergency Fund, Vacation",
                                                style = SelfBudgetType.body,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                        },
                                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                                        singleLine = true,
                                        textStyle = SelfBudgetType.rowTitle.copy(color = MaterialTheme.colorScheme.onSurface),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color.Transparent,
                                            unfocusedBorderColor = Color.Transparent,
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                SectionRowDivider(modifier = Modifier.padding(start = 62.dp))

                                FieldRow(
                                    icon = Icons.Default.AccountBalance,
                                    label = "Track progress from",
                                    value = linkedAccount?.name ?: "No account (manual goal)",
                                    isPlaceholder = linkedAccount == null,
                                    showChevron = true,
                                    onClick = { pickingAccount = true }
                                )
                            }
                        }

                        // 3. Live Goal Preview
                        Surface(
                            shape = ShapeCard,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RampIconTile(icon = Icons.Default.Savings, ramp = Ramp.Teal, size = 36.dp, iconSize = 18.dp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = name.ifBlank { "Goal name" },
                                        style = SelfBudgetType.rowTitle,
                                        color = if (name.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Before a target is entered, show an empty track and placeholder
                                // text — never live numbers against a $0.00 target (spec §22).
                                LinearProgressIndicator(
                                    progress = { if (target > 0.0) progress else 0f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = Ramp.Teal.c400
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = if (target > 0.0) {
                                        "$currencySymbol%.2f of $currencySymbol%.2f".format(currentAmount, target)
                                    } else {
                                        "Enter a target amount to preview"
                                    },
                                    style = SelfBudgetType.body,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Action Buttons (spec §14: Cancel + Save pair, destructive isolated below)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                SecondaryPillButton(
                                    text = "Cancel",
                                    onClick = {
                                        editBaseline?.let { baseline ->
                                            name = baseline.name
                                            targetText = baseline.targetText
                                            linkedAccount = assetAccounts.firstOrNull { it.id == baseline.linkedAccountId }
                                        }
                                        isEditMode = false
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(54.dp)
                                )

                                PrimaryPillButton(
                                    text = "Save changes",
                                    onClick = { save() },
                                    enabled = isDirty && isValid,
                                    ramp = Ramp.Teal,
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .height(54.dp)
                                )
                            }

                            DestructivePillButton(
                                text = "Delete savings goal",
                                onClick = { showDeleteConfirmationModal = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                            )
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
            Surface(
                shape = ShapeCard,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    RampIconTile(icon = Icons.Default.Delete, ramp = Ramp.Red, size = 64.dp, iconSize = 32.dp)

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Delete savings goal?",
                        style = SelfBudgetType.title,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Are you sure you want to delete \"${goal.name}\"? This cannot be undone.",
                        style = SelfBudgetType.body,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SecondaryPillButton(
                            text = "Cancel",
                            onClick = { showDeleteConfirmationModal = false },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        )

                        PrimaryPillButton(
                            text = "Delete",
                            onClick = {
                                showDeleteConfirmationModal = false
                                onDelete(goal)
                            },
                            ramp = Ramp.Red,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        )
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
    currentAmount: Double,
    target: Double,
    progress: Float,
    currencySymbol: String,
    linkedAccountName: String?,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onClose: () -> Unit
) {
    val isDark = isAppInDarkTheme()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Amount Card — neutral display number, the badge pill carries the color (spec §14)
        Surface(
            shape = ShapeHero,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(shape = ShapePill, color = Ramp.Teal.tintFill(isDark)) {
                    Text(
                        text = "TARGET AMOUNT",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = SelfBudgetType.eyebrow,
                        color = Ramp.Teal.titleText(isDark)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "$currencySymbol${targetText.ifBlank { "0.00" }}",
                    style = SelfBudgetType.display,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Progress Card
        Surface(
            shape = ShapeCard,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RampIconTile(icon = Icons.Default.Savings, ramp = Ramp.Teal, size = 36.dp, iconSize = 18.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = name.ifBlank { "Goal name" },
                        style = SelfBudgetType.rowTitle,
                        color = if (name.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                LinearProgressIndicator(
                    progress = { if (target > 0.0) progress else 0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Ramp.Teal.c400
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (target > 0.0) {
                        "$currencySymbol%.2f of $currencySymbol%.2f saved".format(currentAmount, target)
                    } else {
                        "No target amount set"
                    },
                    style = SelfBudgetType.body,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Details Card
        Surface(
            shape = ShapeCard,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                GoalInfoItem(
                    icon = Icons.Default.Savings,
                    label = "Goal name",
                    value = name
                )
                SectionRowDivider(modifier = Modifier.padding(start = 62.dp))
                GoalInfoItem(
                    icon = Icons.Default.AccountBalance,
                    label = "Track progress from",
                    value = linkedAccountName ?: "No account (manual goal)"
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Buttons (spec §14: Close + Edit pair, destructive isolated below)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SecondaryPillButton(
                    text = "Close",
                    onClick = onClose,
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                )

                PrimaryPillButton(
                    text = "Edit goal",
                    onClick = onEditClick,
                    ramp = Ramp.Teal,
                    modifier = Modifier
                        .weight(1.3f)
                        .height(54.dp)
                )
            }

            DestructivePillButton(
                text = "Delete savings goal",
                onClick = onDeleteClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            )
        }
    }
}

/** Detail-row icon tiles are Gray (spec §14): they label a field TYPE, not a category. */
@Composable
private fun GoalInfoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            GrayIconTile(icon = icon, size = 36.dp, iconSize = 18.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                style = SelfBudgetType.meta,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            style = SelfBudgetType.rowTitle,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End
        )
    }
}
