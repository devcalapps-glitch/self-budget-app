package com.selfbudget.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.selfbudget.app.ui.theme.Ramp
import com.selfbudget.app.ui.theme.ShapeTile
import com.selfbudget.app.ui.theme.icon
import com.selfbudget.app.ui.theme.isAppInDarkTheme
import com.selfbudget.app.ui.theme.tintFill

/** A colored/gray icon tile (spec §1/§14/§16): a tinted square behind a glyph. */
@Composable
fun IconTile(
    icon: ImageVector,
    tint: Color,
    background: Color,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = 20.dp,
    shape: Shape = ShapeTile,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(iconSize))
    }
}

/** An [IconTile] tinted from a single ramp per the one-ramp-rule (fill + icon share a ramp). */
@Composable
fun RampIconTile(
    icon: ImageVector,
    ramp: Ramp,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = 20.dp,
) {
    val isDark = isAppInDarkTheme()
    IconTile(
        icon = icon,
        tint = ramp.icon(isDark),
        background = ramp.tintFill(isDark),
        modifier = modifier,
        size = size,
        iconSize = iconSize,
    )
}

/**
 * The neutral gray icon tile used for field-type / detail-row icons (spec
 * §14/§16) where semantic color would be noise — e.g. amount, date, account
 * fields in a detail sheet or form. Category rows use [RampIconTile] instead.
 */
@Composable
fun GrayIconTile(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = 20.dp,
) = RampIconTile(icon = icon, ramp = Ramp.Gray, modifier = modifier, size = size, iconSize = iconSize)
