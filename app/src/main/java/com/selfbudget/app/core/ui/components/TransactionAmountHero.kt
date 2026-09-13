package com.selfbudget.app.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.selfbudget.app.ui.theme.Ramp
import com.selfbudget.app.ui.theme.SelfBudgetType
import com.selfbudget.app.ui.theme.ShapeHero
import com.selfbudget.app.ui.theme.ShapePill
import com.selfbudget.app.ui.theme.isAppInDarkTheme
import com.selfbudget.app.ui.theme.tintFill
import com.selfbudget.app.ui.theme.titleText
import java.util.Locale

/** Entry-form transaction type, kept local so this component has no feature-module dependency. */
enum class EntryType(val label: String, val ramp: Ramp) {
    Expense("Expense", Ramp.Red),
    Income("Income", Ramp.Teal),
    Transfer("Transfer", Ramp.Gray)
}

/**
 * Amount-entry hero (spec §16): a type badge is the ONLY element carrying
 * money-direction color — the digits stay neutral text primary in every
 * variant, and both +/- steppers are identical neutral gray (never tint one
 * red). Used by every transaction entry/edit form so they share one visual
 * system instead of each re-implementing it.
 */
@Composable
fun TransactionAmountHero(
    type: EntryType,
    amountText: String,
    onAmountChange: (String) -> Unit,
    currencySymbol: String,
    modifier: Modifier = Modifier,
    badgeText: String = "${type.label.uppercase()} AMOUNT",
    ramp: Ramp = type.ramp,
    stepAmount: Double = 1.0,
    onNext: (() -> Unit)? = null,
) {
    val isDark = isAppInDarkTheme()
    Surface(
        shape = ShapeHero,
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(shape = ShapePill, color = ramp.tintFill(isDark)) {
                Text(
                    text = badgeText,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = SelfBudgetType.eyebrow,
                    color = ramp.titleText(isDark)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                AmountStepperButton(
                    icon = Icons.Default.Remove,
                    contentDescription = "Decrease amount",
                    onClick = {
                        val current = amountText.toDoubleOrNull() ?: 0.0
                        val next = maxOf(0.0, current - stepAmount)
                        onAmountChange(if (next == 0.0) "" else "%.2f".format(next))
                    }
                )

                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d{0,2}$"""))) {
                                onAmountChange(input)
                            }
                        },
                        placeholder = {
                            Text(
                                text = "0.00",
                                style = SelfBudgetType.display.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    textAlign = TextAlign.Center
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        prefix = {
                            Text(
                                text = currencySymbol,
                                style = SelfBudgetType.display.copy(fontSize = 24.sp),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(end = 2.dp)
                            )
                        },
                        textStyle = SelfBudgetType.display.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = if (onNext != null) ImeAction.Next else ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onNext = { onNext?.invoke() }),
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

                AmountStepperButton(
                    icon = Icons.Default.Add,
                    contentDescription = "Increase amount",
                    onClick = {
                        val current = amountText.toDoubleOrNull() ?: 0.0
                        onAmountChange("%.2f".format(current + stepAmount))
                    }
                )
            }
        }
    }
}

@Composable
private fun AmountStepperButton(icon: androidx.compose.ui.graphics.vector.ImageVector, contentDescription: String, onClick: () -> Unit) {
    val isDark = isAppInDarkTheme()
    Surface(
        onClick = onClick,
        shape = androidx.compose.foundation.shape.CircleShape,
        color = Ramp.Gray.tintFill(isDark),
        modifier = Modifier.size(44.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Ramp.Gray.titleText(isDark),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/** Inactive-pill quick-amount chips (spec §16), wrapping, 44dp min tap height. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun QuickAmountChips(
    presets: List<Int>,
    currencySymbol: String,
    onPick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = isAppInDarkTheme()
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, alignment = Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        presets.forEach { preset ->
            Surface(
                shape = ShapePill,
                color = Ramp.Gray.tintFill(isDark),
                modifier = Modifier
                    .heightIn(min = 44.dp)
                    .clickable { onPick(preset) }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 14.dp)) {
                    Text(
                        text = "+$currencySymbol${String.format(Locale.US, "%,d", preset)}",
                        style = SelfBudgetType.rowTitle,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

