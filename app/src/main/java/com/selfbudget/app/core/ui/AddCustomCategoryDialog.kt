package com.selfbudget.app.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.selfbudget.app.data.model.CategoryEntity
import com.selfbudget.app.data.model.TransactionType
import com.selfbudget.app.ui.theme.ExpenseRed
import com.selfbudget.app.ui.theme.getIncomeColor
import java.util.UUID

@Composable
fun AddCustomCategoryDialog(
    initialType: TransactionType = TransactionType.EXPENSE,
    lockType: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (CategoryEntity) -> Unit
) {
    var categoryName by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(initialType) }
    var selectedColorHex by remember { mutableStateOf("#3F51B5") }
    var selectedIconName by remember { mutableStateOf("Category") }

    val presetColors = listOf(
        "#3F51B5", "#E91E63", "#9C27B0", "#009688",
        "#FF9800", "#795548", "#607D8B", "#4CAF50",
        "#15803D", "#D32F2F"
    )

    val presetIcons = listOf(
        "Restaurant" to Icons.Default.Restaurant,
        "Shopping" to Icons.Default.ShoppingBag,
        "Home" to Icons.Default.Home,
        "Transport" to Icons.Default.DirectionsBus,
        "Receipt" to Icons.AutoMirrored.Filled.ReceiptLong,
        "Subscriptions" to Icons.Default.Subscriptions,
        "Movie" to Icons.Default.Movie,
        "Medical" to Icons.Default.MedicalServices,
        "Wallet" to Icons.Default.AccountBalanceWallet,
        "CreditCard" to Icons.Default.CreditCard,
        "Trending" to Icons.AutoMirrored.Filled.TrendingUp,
        "Transfer" to Icons.AutoMirrored.Filled.CompareArrows,
        "Work" to Icons.Default.Work,
        "Gift" to Icons.Default.CardGiftcard,
        "More" to Icons.Default.MoreHoriz,
        "Category" to Icons.Default.Category
    )

    val isDark = isSystemInDarkTheme()
    val selectedAccentColor = try { Color(android.graphics.Color.parseColor(selectedColorHex)) } catch (e: Exception) { MaterialTheme.colorScheme.primary }

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
                                text = "Create Category",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                if (categoryName.isNotBlank()) {
                                    val newCat = CategoryEntity(
                                        id = "cat_custom_${UUID.randomUUID()}",
                                        name = categoryName.trim(),
                                        iconName = selectedIconName,
                                        colorHex = selectedColorHex,
                                        type = selectedType,
                                        isDefault = false
                                    )
                                    onConfirm(newCat)
                                }
                            },
                            enabled = categoryName.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
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
                    // Real-Time Category Card Preview
                    Text(
                        text = "Live Category Preview",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(selectedAccentColor.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = getCategoryIcon(selectedIconName, categoryName),
                                        contentDescription = null,
                                        tint = selectedAccentColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column {
                                    Text(
                                        text = categoryName.ifBlank { "Category Name" },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (categoryName.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (selectedType == TransactionType.INCOME) "Income Category" else "Expense Category",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (selectedType == TransactionType.INCOME) getIncomeColor() else ExpenseRed,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // Category Type Segmented Toggle
                    Text(
                        text = "Transaction Type",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (lockType) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = (if (selectedType == TransactionType.INCOME) getIncomeColor() else ExpenseRed).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, (if (selectedType == TransactionType.INCOME) getIncomeColor() else ExpenseRed).copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (selectedType == TransactionType.INCOME) "💰 Income Category" else "💸 Expense Category",
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedType == TransactionType.INCOME) getIncomeColor() else ExpenseRed,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
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
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(11.dp))
                                        .background(if (selectedType == TransactionType.EXPENSE) ExpenseRed.copy(alpha = 0.2f) else Color.Transparent)
                                        .clickable { selectedType = TransactionType.EXPENSE },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "💸 Expense Category",
                                        fontWeight = if (selectedType == TransactionType.EXPENSE) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selectedType == TransactionType.EXPENSE) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 14.sp
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(11.dp))
                                        .background(if (selectedType == TransactionType.INCOME) getIncomeColor().copy(alpha = 0.2f) else Color.Transparent)
                                        .clickable { selectedType = TransactionType.INCOME },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "💰 Income Category",
                                        fontWeight = if (selectedType == TransactionType.INCOME) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selectedType == TransactionType.INCOME) getIncomeColor() else ExpenseRed,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }

                    // Category Name Text Box
                    OutlinedTextField(
                        value = categoryName,
                        onValueChange = { input ->
                            categoryName = input.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                        },
                        label = { Text("Category Name") },
                        placeholder = { Text(if (selectedType == TransactionType.EXPENSE) "e.g. Pet Care, Subscriptions, Travel" else "e.g. Dividends, Side Hustle, Bonus") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Category Icon Picker
                    Column {
                        Text(
                            text = "Category Icon",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(presetIcons) { (iconKey, iconVector) ->
                                val isSelected = selectedIconName.equals(iconKey, ignoreCase = true)
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSelected) selectedAccentColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = BorderStroke(
                                        1.5.dp,
                                        if (isSelected) selectedAccentColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                    ),
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clickable { selectedIconName = iconKey }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = iconVector,
                                            contentDescription = iconKey,
                                            tint = if (isSelected) selectedAccentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Color Badge Accent Swatch Selection
                    Column {
                        Text(
                            text = "Category Color Theme",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            presetColors.forEach { hex ->
                                val color = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color.Gray }
                                val isSelected = selectedColorHex == hex
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .clickable { selectedColorHex = hex },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
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
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text("Cancel", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        Button(
                            onClick = {
                                if (categoryName.isNotBlank()) {
                                    val newCat = CategoryEntity(
                                        id = "cat_custom_${UUID.randomUUID()}",
                                        name = categoryName.trim(),
                                        iconName = selectedIconName,
                                        colorHex = selectedColorHex,
                                        type = selectedType,
                                        isDefault = false
                                    )
                                    onConfirm(newCat)
                                }
                            },
                            enabled = categoryName.isNotBlank(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(48.dp)
                        ) {
                            Text("Save Category", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }
}
