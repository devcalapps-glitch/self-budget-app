package com.selfbudget.app.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.selfbudget.app.core.ui.components.PrimaryPillButton
import com.selfbudget.app.core.ui.components.SecondaryPillButton
import com.selfbudget.app.data.model.CategoryEntity
import com.selfbudget.app.data.model.TransactionType
import com.selfbudget.app.ui.theme.Ramp
import com.selfbudget.app.ui.theme.SelfBudgetType
import com.selfbudget.app.ui.theme.ShapeCard
import com.selfbudget.app.ui.theme.containerBorder
import com.selfbudget.app.ui.theme.isAppInDarkTheme
import com.selfbudget.app.ui.theme.sectionRamp
import com.selfbudget.app.ui.theme.secondaryText
import com.selfbudget.app.ui.theme.tintFill
import com.selfbudget.app.ui.theme.titleText

@Composable
fun CategorySelectionModal(
    categories: List<CategoryEntity>,
    selectedCategory: CategoryEntity?,
    transactionType: TransactionType = TransactionType.EXPENSE,
    onDismiss: () -> Unit,
    onSelectCategory: (CategoryEntity) -> Unit,
    onAddCustomCategory: () -> Unit,
    onArchiveCategory: ((CategoryEntity) -> Unit)? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    var pendingArchiveCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val effectiveType = if (transactionType == TransactionType.TRANSFER) TransactionType.EXPENSE else transactionType

    val filteredCategories = remember(categories, effectiveType, searchQuery) {
        categories
            .filter { !it.isArchived }
            .filter { it.type == effectiveType }
            .filter { searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) }
    }

    val groupedCategories = remember(filteredCategories, effectiveType) {
        val groupOrder = if (effectiveType == TransactionType.EXPENSE) {
            listOf("Housing & Essentials", "Food & Daily Living", "Lifestyle & Entertainment", "Debt & Financial", "Custom Categories", "Other")
        } else {
            listOf("Earned Income", "Investments & Passive", "Gifts & Other", "Custom Categories", "Other")
        }

        val map = filteredCategories.groupBy {
            if (effectiveType == TransactionType.EXPENSE) getExpenseCategoryGroup(it)
            else getIncomeCategoryGroup(it)
        }

        groupOrder.mapNotNull { groupName ->
            map[groupName]?.let { items -> groupName to items }
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
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .navigationBarsPadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Persistent Top App Bar
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
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (effectiveType == TransactionType.INCOME) "Select income type" else "Select expense type",
                            style = SelfBudgetType.title,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    PrimaryPillButton(
                        text = "New",
                        onClick = {
                            onDismiss()
                            onAddCustomCategory()
                        }
                    )
                }
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    // Search Box
                    AppSearchBar(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = "Search categories...",
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (filteredCategories.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No categories found.",
                                style = SelfBudgetType.body,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        groupedCategories.forEachIndexed { groupIndex, (groupName, items) ->
                            if (groupIndex > 0) {
                                Spacer(modifier = Modifier.height(20.dp))
                            }
                            CategorySectionLabel(text = groupName)
                            CategoryGroup(
                                ramp = sectionRamp(groupName),
                                categories = items,
                                selectedCategory = selectedCategory,
                                onSelectCategory = { category ->
                                    focusManager.clearFocus(force = true)
                                    keyboardController?.hide()
                                    onSelectCategory(category)
                                    onDismiss()
                                },
                                onArchiveCategory = { cat -> pendingArchiveCategory = cat }
                            )
                        }
                    }

                    // Generous bottom spacing for comfortable scrolling
                    Spacer(modifier = Modifier.height(140.dp))
                }
            }
        }
    }

    // Archive Confirmation Dialog
    if (pendingArchiveCategory != null) {
        val catToArchive = pendingArchiveCategory!!
        AlertDialog(
            onDismissRequest = { pendingArchiveCategory = null },
            title = {
                Text(text = "Archive category?", style = SelfBudgetType.heading)
            },
            text = {
                Text(
                    text = "This will hide '${catToArchive.name}' from future picklists. Your past spending history for this category remains safe and intact.",
                    style = SelfBudgetType.body
                )
            },
            confirmButton = {
                PrimaryPillButton(
                    text = "Archive category",
                    onClick = {
                        onArchiveCategory?.invoke(catToArchive)
                        pendingArchiveCategory = null
                    }
                )
            },
            dismissButton = {
                SecondaryPillButton(
                    text = "Cancel",
                    onClick = { pendingArchiveCategory = null },
                    ramp = Ramp.Gray
                )
            }
        )
    }
}

@Composable
private fun CategorySectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = SelfBudgetType.eyebrow,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp, start = 2.dp)
    )
}

@Composable
private fun CategoryGroup(
    ramp: Ramp,
    categories: List<CategoryEntity>,
    selectedCategory: CategoryEntity?,
    onSelectCategory: (CategoryEntity) -> Unit,
    onArchiveCategory: (CategoryEntity) -> Unit
) {
    val isDark = isAppInDarkTheme()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ShapeCard)
            .background(MaterialTheme.colorScheme.surface)
            .border(BorderStroke(0.5.dp, ramp.containerBorder(isDark)), ShapeCard)
    ) {
        categories.forEachIndexed { index, category ->
            if (index > 0) {
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
            CategoryRow(
                category = category,
                isSelected = selectedCategory?.id == category.id,
                onClick = { onSelectCategory(category) },
                onArchive = if (!category.isDefault) { { onArchiveCategory(category) } } else null
            )
        }
    }
}

@Composable
private fun CategoryRow(
    category: CategoryEntity,
    isSelected: Boolean,
    onClick: () -> Unit,
    onArchive: (() -> Unit)?
) {
    val isDark = isAppInDarkTheme()
    val catColor = try {
        Color(android.graphics.Color.parseColor(category.colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    val iconTint = if (isSelected) Ramp.Teal.titleText(isDark) else catColor

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) Ramp.Teal.tintFill(isDark) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = getCategoryIcon(category),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.height(20.dp).width(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = category.name,
            style = SelfBudgetType.rowTitle,
            color = if (isSelected) Ramp.Teal.titleText(isDark) else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        if (onArchive != null) {
            IconButton(
                onClick = onArchive,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Archive,
                    contentDescription = "Archive category",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
        }

        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = Ramp.Teal.secondaryText(isDark),
                modifier = Modifier.height(18.dp).width(18.dp)
            )
        }
    }
}
