package com.selfbudget.app.core.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.selfbudget.app.ui.theme.Ramp
import com.selfbudget.app.ui.theme.SelfBudgetType
import com.selfbudget.app.ui.theme.ShapePill
import com.selfbudget.app.ui.theme.isAppInDarkTheme
import com.selfbudget.app.ui.theme.titleText
import com.selfbudget.app.ui.theme.tintFill
import kotlin.math.abs

/** Which direction of change is "good" for a given metric (spec §7 "Delta badges"). */
enum class DeltaMetric { SPENDING, INCOME, DEBT_PAYOFF, NET_WORTH }

/**
 * A "vs last month" pill. Color encodes whether the change is good, not its
 * arithmetic sign — an increase in spending is Red, an increase in income is
 * Teal. Exactly zero is always neutral gray, never colored.
 */
@Composable
fun DeltaBadge(percentChange: Float, metric: DeltaMetric, modifier: Modifier = Modifier) {
    val isDark = isAppInDarkTheme()
    val ramp = when {
        percentChange == 0f -> Ramp.Gray
        else -> {
            val isIncrease = percentChange > 0f
            val increaseIsGood = metric != DeltaMetric.SPENDING
            val isGood = if (isIncrease) increaseIsGood else !increaseIsGood
            if (isGood) Ramp.Teal else Ramp.Red
        }
    }
    val arrow = when {
        percentChange > 0f -> "↑ "
        percentChange < 0f -> "↓ "
        else -> ""
    }
    Surface(shape = ShapePill, color = ramp.tintFill(isDark), modifier = modifier) {
        Text(
            "$arrow${formatSignedPercent(percentChange)}",
            style = SelfBudgetType.badge,
            color = ramp.titleText(isDark),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

private fun formatSignedPercent(percentChange: Float): String {
    val sign = if (percentChange > 0f) "+" else if (percentChange < 0f) "−" else "+"
    return "$sign${"%.1f".format(abs(percentChange))}%"
}
