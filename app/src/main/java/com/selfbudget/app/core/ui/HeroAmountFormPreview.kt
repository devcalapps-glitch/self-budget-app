package com.selfbudget.app.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.selfbudget.app.core.ui.components.DoneChip
import com.selfbudget.app.data.model.TransactionType
import com.selfbudget.app.ui.theme.Ramp
import com.selfbudget.app.ui.theme.SelfBudgetType
import com.selfbudget.app.ui.theme.ShapeChip
import com.selfbudget.app.ui.theme.ShapeHero
import com.selfbudget.app.ui.theme.ShapePill
import com.selfbudget.app.ui.theme.TextMuted
import com.selfbudget.app.ui.theme.isAppInDarkTheme
import com.selfbudget.app.ui.theme.tintFill
import com.selfbudget.app.ui.theme.titleText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeroAmountFormPreview(
    currencySymbol: String = "$",
    onDismiss: () -> Unit = {}
) {
    var amountText by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var selectedCategoryName by remember { mutableStateOf("Groceries & Food") }
    var selectedAccountName by remember { mutableStateOf("Main Checking") }
    val isDark = isAppInDarkTheme()

    // The type badge is the only semantic color; digits stay neutral in every variant (spec §16).
    val typeRamp = when (selectedType) {
        TransactionType.EXPENSE -> Ramp.Red
        TransactionType.INCOME -> Ramp.Teal
        TransactionType.TRANSFER -> Ramp.Gray
    }
    val typeLabel = when (selectedType) {
        TransactionType.EXPENSE -> "Expense"
        TransactionType.INCOME -> "Income"
        TransactionType.TRANSFER -> "Transfer"
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Text(
                        text = "Hero amount top preview",
                        style = SelfBudgetType.heading
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    DoneChip(text = "Done", modifier = Modifier.clickable(onClick = onDismiss).padding(end = 12.dp))
                }
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // 1. HERO AMOUNT TOP CONTAINER (spec §16): neutral card, type badge is the only color.
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = ShapeHero,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp, horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Type badge — the only semantic color in this hero. Tap to cycle (preview only).
                        Surface(
                            shape = ShapePill,
                            color = typeRamp.tintFill(isDark),
                            modifier = Modifier.clickable {
                                selectedType = when (selectedType) {
                                    TransactionType.EXPENSE -> TransactionType.INCOME
                                    TransactionType.INCOME -> TransactionType.TRANSFER
                                    TransactionType.TRANSFER -> TransactionType.EXPENSE
                                }
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(typeLabel, style = SelfBudgetType.badge, color = typeRamp.titleText(isDark))
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = "AMOUNT",
                            style = SelfBudgetType.eyebrow,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Neutral hero digits (spec §16) with the display token's tabular numerals
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = currencySymbol,
                                style = SelfBudgetType.display,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            BasicTextField(
                                value = amountText,
                                onValueChange = { amountText = it },
                                textStyle = TextStyle(
                                    fontSize = SelfBudgetType.display.fontSize,
                                    fontWeight = SelfBudgetType.display.fontWeight,
                                    fontFeatureSettings = SelfBudgetType.display.fontFeatureSettings,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Start
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                decorationBox = { innerTextField ->
                                    Box {
                                        if (amountText.isEmpty()) {
                                            Text(text = "0.00", style = SelfBudgetType.display, color = TextMuted)
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Quick preset amount chips — inactive pill style (spec §16)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(5, 10, 25, 50, 100).forEach { preset ->
                                Surface(
                                    shape = ShapePill,
                                    color = Ramp.Gray.tintFill(isDark),
                                    modifier = Modifier.clickable {
                                        val currentVal = amountText.toDoubleOrNull() ?: 0.0
                                        amountText = "%.2f".format(currentVal + preset)
                                    }
                                ) {
                                    Text(
                                        text = "+$currencySymbol$preset",
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        style = SelfBudgetType.badge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. TRANSACTION DETAILS FORM (Title, Category, Account, Date)
                Text(
                    text = "Transaction details",
                    style = SelfBudgetType.heading,
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title / Merchant") },
                    placeholder = { Text("Starbucks") },
                    shape = ShapeChip,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = selectedCategoryName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    leadingIcon = { Icon(Icons.Default.Category, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    shape = ShapeChip,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = selectedAccountName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Payment account") },
                    leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    shape = ShapeChip,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = "Today (Aug 27, 2026)",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Date") },
                    leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    shape = ShapeChip,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
