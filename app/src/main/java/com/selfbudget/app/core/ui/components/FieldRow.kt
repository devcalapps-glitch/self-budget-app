package com.selfbudget.app.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.selfbudget.app.ui.theme.SelfBudgetType
import com.selfbudget.app.ui.theme.TextMuted

/**
 * A form/detail-sheet field row (spec §14/§16): a gray icon tile (field types
 * are not categories — semantic color here is noise), a floating label, and
 * the value or a muted placeholder. Filled vs. placeholder is the form's only
 * "what's done vs missing" signal.
 */
@Composable
fun FieldRow(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    isPlaceholder: Boolean = false,
    showChevron: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val rowModifier = modifier
        .fillMaxWidth()
        .let { if (onClick != null) it.clickable(onClick = onClick) else it }
        .padding(horizontal = 14.dp, vertical = 12.dp)
    Row(modifier = rowModifier, verticalAlignment = Alignment.CenterVertically) {
        GrayIconTile(icon = icon, size = 36.dp, iconSize = 18.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = SelfBudgetType.meta, color = TextMuted)
            Text(
                value,
                style = SelfBudgetType.rowTitle,
                color = if (isPlaceholder) TextMuted else MaterialTheme.colorScheme.onSurface,
            )
        }
        if (showChevron) {
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = TextMuted,
            )
        }
    }
}
