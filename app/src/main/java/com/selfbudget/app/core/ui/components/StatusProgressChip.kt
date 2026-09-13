package com.selfbudget.app.core.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.selfbudget.app.ui.theme.BudgetStatus
import com.selfbudget.app.ui.theme.ProgressTrackDark
import com.selfbudget.app.ui.theme.ProgressTrackLight
import com.selfbudget.app.ui.theme.Ramp
import com.selfbudget.app.ui.theme.SelfBudgetType
import com.selfbudget.app.ui.theme.ShapePill
import com.selfbudget.app.ui.theme.isAppInDarkTheme
import com.selfbudget.app.ui.theme.titleText
import com.selfbudget.app.ui.theme.tintFill
import kotlin.math.roundToInt

/** Bar fill, % badge, and "safe to spend" text all read from the same [status] (spec §9). */
@Composable
fun StatusProgressBar(
    progress: Float,
    status: BudgetStatus,
    modifier: Modifier = Modifier,
    height: Dp = 6.dp,
) {
    val isDark = isAppInDarkTheme()
    LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2)),
        color = status.ramp.c400,
        trackColor = if (isDark) ProgressTrackDark else ProgressTrackLight,
    )
}

/** A "% spent" / status pill, same-ramp fill + text (spec §9). */
@Composable
fun StatusBadge(text: String, status: BudgetStatus, modifier: Modifier = Modifier) {
    val isDark = isAppInDarkTheme()
    Surface(shape = ShapePill, color = status.ramp.tintFill(isDark), modifier = modifier) {
        Text(
            text,
            style = SelfBudgetType.badge,
            color = status.ramp.titleText(isDark),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

/** A neutral count/label pill (e.g. "13 active"), read from [Ramp.Gray] per spec §9. */
@Composable
fun NeutralBadge(text: String, modifier: Modifier = Modifier) {
    val isDark = isAppInDarkTheme()
    Surface(shape = ShapePill, color = Ramp.Gray.tintFill(isDark), modifier = modifier) {
        Text(
            text,
            style = SelfBudgetType.badge,
            color = Ramp.Gray.titleText(isDark),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

/** Formats a percentage for a badge, capping unreadable values (spec §7): >999% renders as "999%+". */
fun formatPercentBadge(fraction: Float): String {
    val pct = (fraction * 100f).roundToInt()
    return if (pct > 999) "999%+" else "$pct%"
}
