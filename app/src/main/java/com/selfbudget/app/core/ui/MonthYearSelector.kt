package com.selfbudget.app.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.selfbudget.app.core.ui.components.CircularBackButton
import com.selfbudget.app.core.ui.components.PrimaryPillButton
import com.selfbudget.app.core.ui.components.SecondaryPillButton
import com.selfbudget.app.ui.theme.Ramp
import com.selfbudget.app.ui.theme.SelfBudgetType
import com.selfbudget.app.ui.theme.ShapeCard
import com.selfbudget.app.ui.theme.ShapeHero
import com.selfbudget.app.ui.theme.ShapePill
import com.selfbudget.app.ui.theme.ShapeTile
import com.selfbudget.app.ui.theme.containerBorder
import com.selfbudget.app.ui.theme.getAccentColor
import com.selfbudget.app.ui.theme.isAppInDarkTheme
import com.selfbudget.app.ui.theme.onSolidFill
import com.selfbudget.app.ui.theme.secondaryText
import com.selfbudget.app.ui.theme.solidFill
import com.selfbudget.app.ui.theme.tintFill
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun CompactMonthYearHeader(
    currentMonthYear: String,
    onSelectMonthYear: (String) -> Unit,
    modifier: Modifier = Modifier,
    onPreviousMonth: (() -> Unit)? = null,
    onNextMonth: (() -> Unit)? = null
) {
    var showPickerDialog by remember { mutableStateOf(false) }
    val isDark = isAppInDarkTheme()

    val formattedDisplay = remember(currentMonthYear) {
        try {
            val date = SimpleDateFormat("yyyy-MM", Locale.getDefault()).parse(currentMonthYear)
            if (date != null) {
                SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(date)
            } else {
                currentMonthYear
            }
        } catch (e: Exception) {
            currentMonthYear
        }
    }

    Surface(
        shape = ShapePill,
        color = Ramp.Gray.tintFill(isDark),
        modifier = modifier
            .height(36.dp)
            .clip(ShapePill)
            .clickable { showPickerDialog = true }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Text(
                text = formattedDisplay,
                style = SelfBudgetType.rowTitle,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Select Month",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }

    if (showPickerDialog) {
        MonthYearPickerDialog(
            currentMonthYear = currentMonthYear,
            onDismiss = { showPickerDialog = false },
            onConfirm = { newMonthYear ->
                onSelectMonthYear(newMonthYear)
                showPickerDialog = false
            }
        )
    }
}

@Composable
fun MonthYearHeader(
    currentMonthYear: String, // e.g. "2026-08"
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectMonthYear: (String) -> Unit
) {
    var showPickerDialog by remember { mutableStateOf(false) }
    val isDark = isAppInDarkTheme()

    val formattedDisplay = remember(currentMonthYear) {
        try {
            val date = SimpleDateFormat("yyyy-MM", Locale.getDefault()).parse(currentMonthYear)
            if (date != null) {
                SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(date)
            } else {
                currentMonthYear
            }
        } catch (e: Exception) {
            currentMonthYear
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = ShapeCard,
        color = Ramp.Gray.tintFill(isDark)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Previous Month Button (Generous 52dp Touch Target)
            IconButton(
                onClick = onPreviousMonth,
                modifier = Modifier.size(52.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous Month",
                    tint = getAccentColor(),
                    modifier = Modifier.size(28.dp)
                )
            }

            // Center Month Title Dropdown Pill (Clickable)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(ShapeTile)
                    .clickable { showPickerDialog = true }
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = "Select Month",
                    tint = getAccentColor(),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formattedDisplay,
                    style = SelfBudgetType.heading,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Right Next Month Button (Generous 52dp Touch Target)
            IconButton(
                onClick = onNextMonth,
                modifier = Modifier.size(52.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next Month",
                    tint = getAccentColor(),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }

    if (showPickerDialog) {
        MonthYearPickerDialog(
            currentMonthYear = currentMonthYear,
            onDismiss = { showPickerDialog = false },
            onConfirm = { newMonthYear ->
                onSelectMonthYear(newMonthYear)
                showPickerDialog = false
            }
        )
    }
}

/**
 * Full-page interactive Month & Year selector with clean minimal layout and high-contrast dark mode support.
 */
@Composable
fun MonthYearPickerDialog(
    currentMonthYear: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val monthData = listOf(
        Pair("Jan", "January"),
        Pair("Feb", "February"),
        Pair("Mar", "March"),
        Pair("Apr", "April"),
        Pair("May", "May"),
        Pair("Jun", "June"),
        Pair("Jul", "July"),
        Pair("Aug", "August"),
        Pair("Sep", "September"),
        Pair("Oct", "October"),
        Pair("Nov", "November"),
        Pair("Dec", "December")
    )

    val actualNow = remember { Calendar.getInstance() }
    val actualYear = actualNow.get(Calendar.YEAR)
    val actualMonthIndex = actualNow.get(Calendar.MONTH)

    val initialCal = remember(currentMonthYear) {
        Calendar.getInstance().apply {
            try {
                val date = SimpleDateFormat("yyyy-MM", Locale.getDefault()).parse(currentMonthYear)
                if (date != null) {
                    time = date
                }
            } catch (_: Exception) { }
        }
    }

    var selectedYear by remember { mutableIntStateOf(initialCal.get(Calendar.YEAR)) }
    var selectedMonthIndex by remember { mutableIntStateOf(initialCal.get(Calendar.MONTH)) }

    val selectedMonthFull = monthData[selectedMonthIndex].second
    val isDark = isAppInDarkTheme()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Bar: back · title · "Current" action button — fixed, never scrolls with content.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularBackButton(onClick = onDismiss)

                    Text(
                        text = "Select month & year",
                        style = SelfBudgetType.title,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .padding(horizontal = 8.dp)
                    )

                    SecondaryPillButton(
                        text = "Current",
                        onClick = {
                            selectedYear = actualYear
                            selectedMonthIndex = actualMonthIndex
                        },
                        ramp = Ramp.Teal,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                        modifier = Modifier.height(36.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Unified Card containing Year Selector, 12-Month Grid, and Action Button
                    Surface(
                        shape = ShapeHero,
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, Ramp.Gray.containerBorder(isDark)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Year Selector Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { selectedYear-- },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(Ramp.Gray.tintFill(isDark), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                        contentDescription = "Previous Year",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$selectedYear",
                                        style = SelfBudgetType.title,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (selectedYear == actualYear) {
                                        Text(
                                            text = "Current year",
                                            style = SelfBudgetType.meta,
                                            color = Ramp.Teal.secondaryText(isDark)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { selectedYear++ },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(Ramp.Gray.tintFill(isDark), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = "Next Year",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                            Spacer(modifier = Modifier.height(14.dp))

                            // 12 Months Grid (4 rows x 3 columns) — Teal selected state (spec §20)
                            for (row in 0 until 4) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    for (col in 0 until 3) {
                                        val index = row * 3 + col
                                        val (shortName, fullName) = monthData[index]
                                        val isSelected = selectedMonthIndex == index
                                        val isCurrentActual = selectedYear == actualYear && index == actualMonthIndex

                                        Surface(
                                            shape = ShapeTile,
                                            color = if (isSelected) Ramp.Teal.solidFill(isDark) else Ramp.Gray.tintFill(isDark),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(52.dp)
                                                .clip(ShapeTile)
                                                .clickable { selectedMonthIndex = index }
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            ) {
                                                Column(
                                                    modifier = Modifier.align(Alignment.Center),
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    Text(
                                                        text = shortName,
                                                        style = SelfBudgetType.rowTitle,
                                                        color = if (isSelected) Ramp.Teal.onSolidFill(isDark) else MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = fullName,
                                                        style = SelfBudgetType.meta,
                                                        color = if (isSelected) Ramp.Teal.onSolidFill(isDark) else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }

                                                if (isCurrentActual && !isSelected) {
                                                    Box(
                                                        modifier = Modifier
                                                            .padding(4.dp)
                                                            .size(6.dp)
                                                            .background(Ramp.Teal.solidFill(isDark), CircleShape)
                                                            .align(Alignment.TopEnd)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                if (row < 3) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Confirm action — single Done control (spec §14/§19)
                            PrimaryPillButton(
                                text = "View $selectedMonthFull $selectedYear",
                                onClick = {
                                    val monthFormatted = String.format("%02d", selectedMonthIndex + 1)
                                    onConfirm("$selectedYear-$monthFormatted")
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
