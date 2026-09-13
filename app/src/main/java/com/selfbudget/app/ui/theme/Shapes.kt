package com.selfbudget.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Design system radius scale (spec §4): pills, inner tiles, chips, cards and
 * sectioned containers, heroes, and the page frame. No other radius values
 * ship — screens being migrated should replace ad-hoc `RoundedCornerShape(N.dp)`
 * calls with one of these.
 */
val RadiusPill = 999.dp
val RadiusTile = 12.dp
val RadiusChip = 14.dp
val RadiusCard = 16.dp
val RadiusHero = 18.dp
val RadiusPage = 20.dp

val ShapePill = RoundedCornerShape(RadiusPill)
val ShapeTile = RoundedCornerShape(RadiusTile)
val ShapeChip = RoundedCornerShape(RadiusChip)
val ShapeCard = RoundedCornerShape(RadiusCard)
val ShapeHero = RoundedCornerShape(RadiusHero)
val ShapePage = RoundedCornerShape(RadiusPage)
