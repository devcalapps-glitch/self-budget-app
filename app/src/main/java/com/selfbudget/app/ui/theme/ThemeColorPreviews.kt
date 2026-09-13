package com.selfbudget.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Visual reference for the design-system color ramps (spec §2): every ramp,
 * every stop, in both themes. Not used by the app itself — a swatch sheet for
 * checking a ramp's stops read correctly before wiring them into a screen.
 */
@Composable
fun RampPalettePreview() {
    Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Color ramps",
                style = SelfBudgetType.heading,
                color = MaterialTheme.colorScheme.onSurface
            )
            Ramp.entries.forEach { ramp -> RampRow(ramp) }
        }
    }
}

@Composable
private fun RampRow(ramp: Ramp) {
    Column {
        Text(
            text = ramp.name,
            style = SelfBudgetType.rowTitle,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                "50" to ramp.c50,
                "100" to ramp.c100,
                "200" to ramp.c200,
                "400" to ramp.c400,
                "600" to ramp.c600,
                "800" to ramp.c800,
                "900" to ramp.c900,
            ).forEach { (stop, color) ->
                Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(40.dp)
                            .clip(ShapeTile)
                            .background(color)
                    )
                    Text(stop, style = SelfBudgetType.meta, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Preview(name = "Ramp palette — light", showBackground = true, heightDp = 1400)
@Composable
private fun RampPalettePreviewLight() {
    SelfBudgetTheme(darkTheme = false) { RampPalettePreview() }
}

@Preview(name = "Ramp palette — dark", showBackground = true, heightDp = 1400)
@Composable
private fun RampPalettePreviewDark() {
    SelfBudgetTheme(darkTheme = true) { RampPalettePreview() }
}
