package com.selfbudget.app.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.selfbudget.app.ui.theme.Ramp
import com.selfbudget.app.ui.theme.SelfBudgetType
import com.selfbudget.app.ui.theme.ShapePill
import com.selfbudget.app.ui.theme.isAppInDarkTheme
import com.selfbudget.app.ui.theme.secondaryText
import com.selfbudget.app.ui.theme.titleText
import com.selfbudget.app.ui.theme.tintFill

/**
 * One solid (primary) action per card (spec §8). Coral 400 fill; text is
 * white in light mode, Coral 50 in dark mode.
 */
@Composable
fun PrimaryPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    ramp: Ramp = Ramp.Coral,
    icon: ImageVector? = null,
) {
    val isDark = isAppInDarkTheme()
    val containerColor = if (isDark && ramp == Ramp.Teal) Color(0xFF196338) else ramp.c400
    val contentColor = if (isDark) {
        if (ramp == Ramp.Teal) Color.White else ramp.c50
    } else Color.White
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = ShapePill,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
        modifier = modifier,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text, style = SelfBudgetType.rowTitle)
    }
}

/** Transparent, 0.5px [ramp]-colored border + text (spec §8) — e.g. "Post now". */
@Composable
fun SecondaryPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    ramp: Ramp = Ramp.Teal,
    icon: ImageVector? = null,
) {
    val isDark = isAppInDarkTheme()
    val color = ramp.secondaryText(isDark)
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = ShapePill,
        border = BorderStroke(0.5.dp, color),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
        modifier = modifier,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text, style = SelfBudgetType.rowTitle)
    }
}

/**
 * Isolated destructive action (spec §14): never solid, never adjacent to the
 * primary action — place it alone below the primary/secondary pair.
 */
@Composable
fun DestructivePillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val isDark = isAppInDarkTheme()
    val borderColor = if (isDark) Ramp.Red.c600 else Ramp.Red.c200
    val textColor = if (isDark) Ramp.Red.c200 else Ramp.Red.c600
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = ShapePill,
        border = BorderStroke(0.5.dp, borderColor),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = textColor),
        modifier = modifier,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text, style = SelfBudgetType.rowTitle)
    }
}

/**
 * A completed action rendered as a non-interactive chip (spec §8) so it
 * doesn't invite tapping — e.g. "Posted" instead of a "Post now" button.
 */
@Composable
fun DoneChip(text: String, modifier: Modifier = Modifier, ramp: Ramp = Ramp.Teal) {
    val isDark = isAppInDarkTheme()
    Surface(shape = ShapePill, color = ramp.tintFill(isDark), modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = ramp.titleText(isDark),
                modifier = Modifier.size(16.dp),
            )
            Text(text, style = SelfBudgetType.rowTitle, color = ramp.titleText(isDark))
        }
    }
}
