package com.selfbudget.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.selfbudget.app.data.model.RecurringFrequency
import com.selfbudget.app.ui.theme.Ramp
import com.selfbudget.app.ui.theme.SelfBudgetType
import com.selfbudget.app.ui.theme.ShapeChip
import com.selfbudget.app.ui.theme.isAppInDarkTheme
import com.selfbudget.app.ui.theme.onSolidFill
import com.selfbudget.app.ui.theme.solidFill

private val FrequencyOptions = listOf(
    RecurringFrequency.WEEKLY to "Weekly",
    RecurringFrequency.BI_WEEKLY to "Bi-Wk",
    RecurringFrequency.SEMI_MONTHLY to "2x/Mo",
    RecurringFrequency.MONTHLY to "Monthly",
    RecurringFrequency.YEARLY to "Yearly",
)

/**
 * The recurring-frequency picker (spec §5 segmented control): one shared
 * implementation so every screen that schedules a recurring amount — add/edit
 * transaction, add/edit recurring item — renders the identical control instead
 * of a per-screen copy. Selected segment reads from [Ramp.solidFill]/[Ramp.onSolidFill],
 * the same tokens used for any other active-tab/selected-filter control.
 */
@Composable
fun FrequencySegmentedControl(
    selected: RecurringFrequency,
    onSelect: (RecurringFrequency) -> Unit,
    modifier: Modifier = Modifier,
    ramp: Ramp = Ramp.Teal,
) {
    val isDark = isAppInDarkTheme()
    Surface(
        shape = ShapeChip,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FrequencyOptions.forEach { (freq, label) ->
                val isSelected = selected == freq
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(ShapeChip)
                        .background(if (isSelected) ramp.solidFill(isDark) else androidx.compose.ui.graphics.Color.Transparent)
                        .clickable { onSelect(freq) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = SelfBudgetType.badge,
                        color = if (isSelected) ramp.onSolidFill(isDark) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
