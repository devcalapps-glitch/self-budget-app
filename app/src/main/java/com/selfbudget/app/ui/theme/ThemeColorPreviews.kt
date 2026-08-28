package com.selfbudget.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ColorFamilySample(
    val familyName: String,
    val aesthetic: String,
    val lightColor: Color,
    val darkColor: Color,
    val lightHex: String,
    val darkHex: String
)

val NonGreenColorFamilies = listOf(
    ColorFamilySample(
        familyName = "1. Modern Emerald (Green)",
        aesthetic = "Classic Growth & Wealth (Robinhood style)",
        lightColor = Color(0xFF059669),
        darkColor = Color(0xFF34D399),
        lightHex = "#059669",
        darkHex = "#34D399"
    ),
    ColorFamilySample(
        familyName = "2. Electric Blue & Indigo",
        aesthetic = "Security, Trust & Clarity (Revolut/Coinbase style)",
        lightColor = Color(0xFF2563EB),
        darkColor = Color(0xFF60A5FA),
        lightHex = "#2563EB",
        darkHex = "#60A5FA"
    ),
    ColorFamilySample(
        familyName = "3. Royal Violet & Purple",
        aesthetic = "Modern Premium Wealth (Monarch/Nubank style)",
        lightColor = Color(0xFF7C3AED),
        darkColor = Color(0xFFC084FC),
        lightHex = "#7C3AED",
        darkHex = "#C084FC"
    ),
    ColorFamilySample(
        familyName = "4. Ocean Teal",
        aesthetic = "Precision & Balance (YNAB/Betterment style)",
        lightColor = Color(0xFF0D9488),
        darkColor = Color(0xFF2DD4BF),
        lightHex = "#0D9488",
        darkHex = "#2DD4BF"
    ),
    ColorFamilySample(
        familyName = "5. Warm Gold & Amber",
        aesthetic = "Luxury Prosperity (Apple Savings/Gold Tier)",
        lightColor = Color(0xFFD97706),
        darkColor = Color(0xFFFBBF24),
        lightHex = "#D97706",
        darkHex = "#FBBF24"
    )
)

@Composable
fun ColorPalettePreviewScreen(isDark: Boolean = false) {
    val bg = if (isDark) DarkBackground else Color(0xFFF1F5F9)
    val cardBg = if (isDark) DarkSurface else Color.White
    val textPrimary = if (isDark) Color.White else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = bg
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (isDark) "Financial Color Palettes (DARK MODE)" else "Financial Color Palettes (LIGHT MODE)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )

            NonGreenColorFamilies.forEach { sample ->
                val activeColor = if (isDark) sample.darkColor else sample.lightColor
                val activeHex = if (isDark) sample.darkHex else sample.lightHex

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, activeColor)
                ) {
                    Column {
                        // Header Banner
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(activeColor)
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = sample.familyName,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = activeHex,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = sample.aesthetic,
                                style = MaterialTheme.typography.bodySmall,
                                color = textSecondary,
                                fontWeight = FontWeight.Medium
                            )

                            // Action Button
                            Button(
                                onClick = {},
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = activeColor,
                                    contentColor = Color.White
                                )
                            ) {
                                Text(
                                    text = "+ Add Income / Net Worth",
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Net Worth & Percentage Pill
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "NET WORTH",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textSecondary
                                    )
                                    Text(
                                        text = "$12,450.00",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = activeColor
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(activeColor.copy(alpha = 0.2f))
                                        .border(1.dp, activeColor, RoundedCornerShape(14.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "+$1,200.00 (+10.6%)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = activeColor
                                    )
                                }
                            }

                            // Area Chart Sparkline
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(activeColor.copy(alpha = 0.08f))
                            ) {
                                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxWidth()) {
                                    val w = size.width
                                    val h = size.height
                                    val points = listOf(8000.0, 8500.0, 9200.0, 10100.0, 11200.0, 12450.0)
                                    val minV = points.min()
                                    val maxV = points.max()
                                    val rangeV = (maxV - minV).coerceAtLeast(1.0)
                                    val stepX = w / (points.size - 1)

                                    val path = Path()
                                    val areaPath = Path()

                                    points.forEachIndexed { i, valPt ->
                                        val x = i * stepX
                                        val normY = (valPt - minV) / rangeV
                                        val y = h - (normY * (h - 10f) + 5f).toFloat()
                                        if (i == 0) {
                                            path.moveTo(x, y)
                                            areaPath.moveTo(x, h)
                                            areaPath.lineTo(x, y)
                                        } else {
                                            path.lineTo(x, y)
                                            areaPath.lineTo(x, y)
                                        }
                                    }
                                    areaPath.lineTo(w, h)
                                    areaPath.close()

                                    drawPath(
                                        path = areaPath,
                                        brush = Brush.verticalGradient(
                                            colors = listOf(activeColor.copy(alpha = 0.5f), activeColor.copy(alpha = 0.05f))
                                        )
                                    )
                                    drawPath(
                                        path = path,
                                        color = activeColor,
                                        style = Stroke(width = 3.5.dp.toPx())
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
