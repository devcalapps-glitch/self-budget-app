package com.selfbudget.app.core.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.selfbudget.app.R

/**
 * Official Brand Logo for Self Budget App.
 * Renders the vector asset derived from selfbudget_app_logo.svg (Ascending Savings Bars & On-Track Coin Accent).
 */
@Composable
fun AppLogoBadge(
    size: Dp = 100.dp,
    showLockBadge: Boolean = false, // retained for signature compatibility
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_app_logo),
            contentDescription = "Self Budget Logo",
            modifier = Modifier.size(size)
        )
    }
}
