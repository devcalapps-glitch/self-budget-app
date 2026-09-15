package com.selfbudget.app.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.selfbudget.app.core.ui.components.CircularBackButton
import com.selfbudget.app.core.ui.components.PrimaryPillButton
import com.selfbudget.app.core.ui.components.SecondaryPillButton
import com.selfbudget.app.core.util.toWordTitleCase
import com.selfbudget.app.data.model.CategoryEntity
import com.selfbudget.app.data.model.TransactionType
import com.selfbudget.app.ui.theme.Ramp
import com.selfbudget.app.ui.theme.SelfBudgetType
import com.selfbudget.app.ui.theme.ShapeCard
import com.selfbudget.app.ui.theme.ShapeChip
import com.selfbudget.app.ui.theme.ShapeHero
import com.selfbudget.app.ui.theme.ShapePill
import com.selfbudget.app.ui.theme.isAppInDarkTheme
import com.selfbudget.app.ui.theme.onSolidFill
import com.selfbudget.app.ui.theme.secondaryText
import com.selfbudget.app.ui.theme.solidFill
import com.selfbudget.app.ui.theme.tintFill
import com.selfbudget.app.ui.theme.titleText
import java.util.UUID

private fun Color.toHex(): String = String.format("#%06X", 0xFFFFFF and this.toArgb())

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddCustomCategoryDialog(
    initialType: TransactionType = TransactionType.EXPENSE,
    lockType: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (CategoryEntity) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val isDark = isAppInDarkTheme()

    var categoryName by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(initialType) }
    // Category color is a user personalization choice, drawn from the design
    // system's 7 vivid ramps (Gray excluded — it reads as "no color").
    val accentRamps = remember { listOf(Ramp.Blue, Ramp.Teal, Ramp.Purple, Ramp.Coral, Ramp.Amber, Ramp.Red, Ramp.Pink) }
    var selectedRamp by remember {
        mutableStateOf(if (initialType == TransactionType.INCOME) Ramp.Teal else Ramp.Blue)
    }
    var selectedIconName by remember { mutableStateOf("Category") }

    val presetIcons = listOf(
        "Groceries" to Icons.Default.ShoppingCart,
        "Restaurant" to Icons.Default.Restaurant,
        "Shopping" to Icons.Default.ShoppingBag,
        "Home" to Icons.Default.Home,
        "Bills" to Icons.AutoMirrored.Filled.ReceiptLong,
        "Transport" to Icons.Default.DirectionsBus,
        "Travel" to Icons.Default.Flight,
        "Subscriptions" to Icons.Default.Subscriptions,
        "Movie" to Icons.Default.Movie,
        "Fitness" to Icons.Default.FitnessCenter,
        "Medical" to Icons.Default.MedicalServices,
        "Salary" to Icons.Default.AccountBalanceWallet,
        "Work" to Icons.Default.Work,
        "Invest" to Icons.AutoMirrored.Filled.TrendingUp,
        "Card" to Icons.Default.CreditCard,
        "Transfer" to Icons.AutoMirrored.Filled.CompareArrows,
        "Gift" to Icons.Default.CardGiftcard,
        "More" to Icons.Default.MoreHoriz,
        "Category" to Icons.Default.Category
    )

    val selectedAccentColor = selectedRamp.c400

    fun buildCategory() = CategoryEntity(
        id = "cat_custom_${UUID.randomUUID()}",
        name = categoryName.trim(),
        iconName = selectedIconName,
        colorHex = selectedAccentColor.toHex(),
        type = selectedType,
        isDefault = false
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
            ) {
                // 1. No header Save action (spec §14/§16): the header holds only close +
                // title. Save is triggered from the footer button below.
                TopAppBar(
                    title = {
                        Text(text = "Create category", style = SelfBudgetType.title, color = MaterialTheme.colorScheme.onSurface)
                    },
                    navigationIcon = {
                        CircularBackButton(onClick = onDismiss, modifier = Modifier.padding(start = 12.dp, end = 8.dp))
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )

                // 2. Scrollable Form Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // HERO PREVIEW CARD (spec §4 hero: no border, the tint is the boundary)
                    Surface(
                        shape = ShapeHero,
                        color = selectedRamp.tintFill(isDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            RampIconTileLarge(icon = getCategoryIcon(selectedIconName, categoryName), ramp = selectedRamp)

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = categoryName.ifBlank { "New category name" },
                                style = SelfBudgetType.title,
                                color = if (categoryName.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else selectedRamp.titleText(isDark)
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            val typeRamp = if (selectedType == TransactionType.INCOME) Ramp.Teal else Ramp.Red
                            Surface(shape = ShapePill, color = typeRamp.tintFill(isDark)) {
                                Text(
                                    text = if (selectedType == TransactionType.INCOME) "Income category" else "Expense category",
                                    style = SelfBudgetType.badge,
                                    color = typeRamp.titleText(isDark),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // BASIC DETAILS CARD
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "CATEGORY DETAILS",
                            style = SelfBudgetType.eyebrow,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp)
                        )

                        Surface(
                            shape = ShapeCard,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                // Category Type Segmented Switch
                                if (lockType) {
                                    val typeRamp = if (selectedType == TransactionType.INCOME) Ramp.Teal else Ramp.Red
                                    Surface(
                                        shape = ShapeChip,
                                        color = typeRamp.tintFill(isDark),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (selectedType == TransactionType.INCOME) "Income category" else "Expense category",
                                                style = SelfBudgetType.rowTitle,
                                                color = typeRamp.titleText(isDark)
                                            )
                                        }
                                    }
                                } else {
                                    Surface(
                                        shape = ShapeChip,
                                        color = Ramp.Gray.tintFill(isDark),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            val isExpense = selectedType == TransactionType.EXPENSE
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxHeight()
                                                    .clip(ShapePill)
                                                    .background(if (isExpense) Ramp.Red.solidFill(isDark) else Color.Transparent)
                                                    .clickable {
                                                        focusManager.clearFocus(force = true)
                                                        keyboardController?.hide()
                                                        selectedType = TransactionType.EXPENSE
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "Expense",
                                                    style = SelfBudgetType.rowTitle,
                                                    color = if (isExpense) Ramp.Red.onSolidFill(isDark) else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            val isIncome = selectedType == TransactionType.INCOME
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxHeight()
                                                    .clip(ShapePill)
                                                    .background(if (isIncome) Ramp.Teal.solidFill(isDark) else Color.Transparent)
                                                    .clickable {
                                                        focusManager.clearFocus(force = true)
                                                        keyboardController?.hide()
                                                        selectedType = TransactionType.INCOME
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "Income",
                                                    style = SelfBudgetType.rowTitle,
                                                    color = if (isIncome) Ramp.Teal.onSolidFill(isDark) else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }

                                // Category Name Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RampIconTileSmall(icon = getCategoryIcon(selectedIconName, categoryName), ramp = selectedRamp)

                                    Spacer(modifier = Modifier.width(12.dp))

                                    OutlinedTextField(
                                        value = categoryName,
                                        onValueChange = { input ->
                                            categoryName = input.toWordTitleCase()
                                        },
                                        placeholder = {
                                            Text(
                                                if (selectedType == TransactionType.EXPENSE) "Pet care, Vacation" else "Side gig, Bonus",
                                                style = SelfBudgetType.body,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(
                                            capitalization = KeyboardCapitalization.Words,
                                            imeAction = ImeAction.Done
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onDone = {
                                                focusManager.clearFocus(force = true)
                                                keyboardController?.hide()
                                            }
                                        ),
                                        shape = ShapeChip,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = selectedAccentColor,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    // ICON PICKER CARD
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "CHOOSE ICON",
                            style = SelfBudgetType.eyebrow,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp)
                        )

                        Surface(
                            shape = ShapeCard,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    presetIcons.forEach { (iconKey, iconVector) ->
                                        val isSelected = selectedIconName.equals(iconKey, ignoreCase = true)
                                        Surface(
                                            shape = ShapeChip,
                                            color = if (isSelected) selectedRamp.tintFill(isDark) else Ramp.Gray.tintFill(isDark),
                                            border = if (isSelected) BorderStroke(1.5.dp, selectedRamp.secondaryText(isDark)) else null,
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clickable {
                                                    focusManager.clearFocus(force = true)
                                                    keyboardController?.hide()
                                                    selectedIconName = iconKey
                                                }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = iconVector,
                                                    contentDescription = iconKey,
                                                    tint = if (isSelected) selectedRamp.secondaryText(isDark) else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // COLOR PALETTE CARD
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "COLOR ACCENT",
                            style = SelfBudgetType.eyebrow,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp)
                        )

                        Surface(
                            shape = ShapeCard,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    accentRamps.forEach { ramp ->
                                        val isSelected = selectedRamp == ramp
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(ramp.c400)
                                                .clickable {
                                                    focusManager.clearFocus(force = true)
                                                    keyboardController?.hide()
                                                    selectedRamp = ramp
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected color",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // 3. Bottom Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SecondaryPillButton(
                            text = "Cancel",
                            onClick = onDismiss,
                            ramp = Ramp.Gray,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                        )

                        PrimaryPillButton(
                            text = "Create category",
                            onClick = { if (categoryName.isNotBlank()) onConfirm(buildCategory()) },
                            enabled = categoryName.isNotBlank(),
                            ramp = selectedRamp,
                            modifier = Modifier
                                .weight(1.4f)
                                .height(50.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(120.dp).navigationBarsPadding())
                }
            }
        }
    }
}

@Composable
private fun RampIconTileLarge(icon: androidx.compose.ui.graphics.vector.ImageVector, ramp: Ramp) {
    val isDark = isAppInDarkTheme()
    Surface(shape = CircleShape, color = ramp.tintFill(isDark, large = true), modifier = Modifier.size(68.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = null, tint = ramp.titleText(isDark), modifier = Modifier.size(34.dp))
        }
    }
}

@Composable
private fun RampIconTileSmall(icon: androidx.compose.ui.graphics.vector.ImageVector, ramp: Ramp) {
    val isDark = isAppInDarkTheme()
    Surface(shape = CircleShape, color = ramp.tintFill(isDark), modifier = Modifier.size(40.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = null, tint = ramp.secondaryText(isDark), modifier = Modifier.size(20.dp))
        }
    }
}
