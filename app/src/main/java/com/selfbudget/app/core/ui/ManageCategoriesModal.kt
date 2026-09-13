package com.selfbudget.app.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.selfbudget.app.core.ui.components.PrimaryPillButton
import com.selfbudget.app.core.ui.components.SecondaryPillButton
import com.selfbudget.app.data.model.CategoryEntity
import com.selfbudget.app.ui.theme.Ramp
import com.selfbudget.app.ui.theme.SelfBudgetType
import com.selfbudget.app.ui.theme.ShapeCard
import com.selfbudget.app.ui.theme.ShapePill
import com.selfbudget.app.ui.theme.ShapeTile
import com.selfbudget.app.ui.theme.containerBorder
import com.selfbudget.app.ui.theme.isAppInDarkTheme
import com.selfbudget.app.ui.theme.onSolidFill
import com.selfbudget.app.ui.theme.solidFill
import com.selfbudget.app.ui.theme.tintFill

@Composable
fun ManageCategoriesModal(
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onToggleCategoryArchive: (CategoryEntity) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Active, 1 = Archived
    val customCategories = remember(categories) { categories.filter { !it.isDefault } }
    val activeCategories = remember(customCategories) { customCategories.filter { !it.isArchived } }
    val archivedCategories = remember(customCategories) { customCategories.filter { it.isArchived } }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = true)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .navigationBarsPadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            val isDark = isAppInDarkTheme()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                // Top App Bar
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
                            text = "Manage categories",
                            style = SelfBudgetType.heading,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

                // Subtitle Info Banner — neutral, no warning tint (spec §12)
                Surface(
                    color = Ramp.Gray.tintFill(isDark),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = ShapeCard
                ) {
                    Text(
                        text = "Archiving custom categories hides them from transaction & budget picklists while safely protecting all your past spending reports.",
                        style = SelfBudgetType.meta,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Tab Switcher (Active vs Archived) — standard FilterChips matching Search and Settings
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val chipColors = FilterChipDefaults.filterChipColors(
                        containerColor = Ramp.Gray.tintFill(isDark),
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedContainerColor = Ramp.Teal.solidFill(isDark),
                        selectedLabelColor = Ramp.Teal.onSolidFill(isDark)
                    )

                    FilterChip(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        shape = ShapePill,
                        colors = chipColors,
                        label = { Text("Active (${activeCategories.size})", style = SelfBudgetType.badge) },
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        shape = ShapePill,
                        colors = chipColors,
                        label = { Text("Archived (${archivedCategories.size})", style = SelfBudgetType.badge) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                val displayList = if (selectedTab == 0) activeCategories else archivedCategories

                if (displayList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (selectedTab == 0) "No active custom categories" else "No archived categories",
                            style = SelfBudgetType.body,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = ShapeCard,
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(0.5.dp, Ramp.Purple.containerBorder(isDark))
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    displayList.forEachIndexed { index, category ->
                                        val catColor = try {
                                            Color(android.graphics.Color.parseColor(category.colorHex))
                                        } catch (e: Exception) {
                                            MaterialTheme.colorScheme.primary
                                        }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(1f),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Surface(
                                                    shape = ShapeTile,
                                                    color = catColor.copy(alpha = 0.15f),
                                                    modifier = Modifier.size(38.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = getCategoryIcon(category),
                                                            contentDescription = null,
                                                            tint = catColor,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.width(14.dp))

                                                Column {
                                                    Text(
                                                        text = category.name,
                                                        style = SelfBudgetType.rowTitle,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = category.type.name.lowercase().replaceFirstChar { it.uppercase() },
                                                        style = SelfBudgetType.meta,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            if (selectedTab == 0) {
                                                SecondaryPillButton(
                                                    text = "Archive",
                                                    onClick = { onToggleCategoryArchive(category) },
                                                    ramp = Ramp.Gray
                                                )
                                            } else {
                                                PrimaryPillButton(
                                                    text = "Restore",
                                                    onClick = { onToggleCategoryArchive(category) },
                                                    ramp = Ramp.Teal
                                                )
                                            }
                                        }

                                        if (index < displayList.size - 1) {
                                            HorizontalDivider(
                                                color = MaterialTheme.colorScheme.outlineVariant,
                                                thickness = 0.5.dp,
                                                modifier = Modifier.padding(start = 68.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
