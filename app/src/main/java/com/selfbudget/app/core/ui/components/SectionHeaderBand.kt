package com.selfbudget.app.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.selfbudget.app.ui.theme.CardSurfaceDark
import com.selfbudget.app.ui.theme.DividerDark
import com.selfbudget.app.ui.theme.PageBackgroundDark
import com.selfbudget.app.ui.theme.PageBackgroundLight
import com.selfbudget.app.ui.theme.Ramp
import com.selfbudget.app.ui.theme.SelfBudgetType
import com.selfbudget.app.ui.theme.ShapeCard
import com.selfbudget.app.ui.theme.ShapePill
import com.selfbudget.app.ui.theme.TextPrimaryDark
import com.selfbudget.app.ui.theme.TextSecondaryDark
import com.selfbudget.app.ui.theme.containerBorder
import com.selfbudget.app.ui.theme.isAppInDarkTheme
import com.selfbudget.app.ui.theme.pillFill
import com.selfbudget.app.ui.theme.pillText
import com.selfbudget.app.ui.theme.secondaryText
import com.selfbudget.app.ui.theme.tintFill
import com.selfbudget.app.ui.theme.titleText

/**
 * The sectioned container from spec §3: a tinted header band and its content
 * are one bordered container, never a floating colored bar above separate
 * cards. Callers supply hairline-divided rows via [content] (see
 * [SectionRowDivider]).
 */
@Composable
fun SectionHeaderBand(
    title: String,
    ramp: Ramp,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    countPill: String? = null,
    trailingText: String? = null,
    onTrailingClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val isDark = isAppInDarkTheme()
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = ShapeCard,
        color = if (isDark) PageBackgroundDark else PageBackgroundLight,
        border = BorderStroke(0.5.dp, if (isDark) DividerDark else ramp.containerBorder(isDark)),
    ) {
        Column(modifier = Modifier.clip(ShapeCard)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isDark) CardSurfaceDark else ramp.tintFill(isDark))
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (icon != null) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = if (isDark) Ramp.Gray.c400 else ramp.secondaryText(isDark),
                        modifier = Modifier.size(18.dp),
                    )
                }
                Text(
                    title,
                    style = SelfBudgetType.section,
                    color = if (isDark) TextPrimaryDark else ramp.titleText(isDark),
                    modifier = Modifier.weight(1f),
                )
                if (countPill != null) {
                    Surface(
                        shape = ShapePill,
                        color = if (isDark) DividerDark else ramp.pillFill(isDark),
                    ) {
                        Text(
                            countPill,
                            style = SelfBudgetType.badge,
                            color = if (isDark) TextSecondaryDark else ramp.pillText(isDark),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
                if (trailingText != null) {
                    val textModifier = if (onTrailingClick != null) {
                        Modifier.clickable(onClick = onTrailingClick)
                    } else Modifier
                    Text(
                        trailingText,
                        style = SelfBudgetType.meta,
                        color = if (isDark) TextSecondaryDark else ramp.secondaryText(isDark),
                        modifier = textModifier,
                    )
                }
            }
            if (isDark) {
                HorizontalDivider(thickness = 0.5.dp, color = DividerDark)
            }
            Column {
                content()
            }
        }
    }
}

/** Hairline divider between rows inside a [SectionHeaderBand]'s content area. */
@Composable
fun SectionRowDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier,
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}
