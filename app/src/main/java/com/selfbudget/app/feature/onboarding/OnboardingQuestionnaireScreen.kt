package com.selfbudget.app.feature.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.selfbudget.app.core.ui.AppLogoBadge
import com.selfbudget.app.ui.theme.IncomeGreen
import java.util.Locale

/**
 * Auto-detects native region currency symbol from Android system locale.
 */
fun detectSystemCurrencySymbol(): String {
    return try {
        val currency = java.util.Currency.getInstance(Locale.getDefault())
        val symbol = currency.symbol
        val supported = listOf("$", "€", "£", "₹", "¥", "A$", "C$")
        if (supported.contains(symbol)) symbol else "$"
    } catch (e: Exception) {
        "$"
    }
}

@Composable
fun OnboardingQuestionnaireScreen(
    onComplete: (preferredCurrency: String, primaryGoal: String, referralSource: String) -> Unit
) {
    val autoDetectedCurrency = remember { detectSystemCurrencySymbol() }
    
    var currentStep by remember { mutableIntStateOf(1) }
    var selectedGoal by remember { mutableStateOf("Track daily expenses & control spending") }
    var selectedCurrency by remember { mutableStateOf(autoDetectedCurrency) }
    var selectedReferral by remember { mutableStateOf("App Store Search") }

    val goals = listOf(
        Pair("🎯 Track daily expenses & control spending", "Monitor transactions and eliminate unnecessary expenses."),
        Pair("💰 Build savings & emergency fund", "Set aside money monthly for unexpected financial needs."),
        Pair("💳 Pay off credit cards & debt", "Organize card balances and systematically reduce liabilities."),
        Pair("📊 Plan monthly budget & manage bills", "Allocate income to categories and never miss due dates.")
    )

    val currencies = listOf(
        Pair("$", "USD ($) - US Dollar"),
        Pair("€", "EUR (€) - Euro"),
        Pair("£", "GBP (£) - British Pound"),
        Pair("₹", "INR (₹) - Indian Rupee"),
        Pair("C$", "CAD ($) - Canadian Dollar"),
        Pair("A$", "AUD ($) - Australian Dollar"),
        Pair("¥", "JPY (¥) - Japanese Yen")
    )

    val referralSources = listOf(
        Pair("📱 Social Media", "Instagram, TikTok, Reddit, or X"),
        Pair("👥 Friend or Family", "Word of mouth recommendation"),
        Pair("🔍 App Store Search", "Discovered on Google Play / App Store"),
        Pair("📰 Blog / Article / YouTube", "Financial review or YouTube video"),
        Pair("📌 Other", "Other referral source")
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Top Header Progress & App Logo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppLogoBadge(size = 44.dp)

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "Step $currentStep of 3",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Step Progress Bar
            LinearProgressIndicator(
                progress = { currentStep / 3.0f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = IncomeGreen,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Animated Step Body
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) { step ->
                when (step) {
                    1 -> StepPrimaryGoal(
                        goals = goals,
                        selectedGoal = selectedGoal,
                        onSelectGoal = { selectedGoal = it }
                    )
                    2 -> StepCurrencySetup(
                        currencies = currencies,
                        selectedCurrency = selectedCurrency,
                        autoDetectedCurrency = autoDetectedCurrency,
                        onSelectCurrency = { selectedCurrency = it }
                    )
                    3 -> StepReferralSource(
                        referrals = referralSources,
                        selectedReferral = selectedReferral,
                        onSelectReferral = { selectedReferral = it }
                    )
                }
            }

            // Bottom Navigation Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStep > 1) {
                    Button(
                        onClick = { currentStep -= 1 },
                        modifier = Modifier
                            .weight(0.4f)
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text("Back", fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = {
                        if (currentStep < 3) {
                            currentStep += 1
                        } else {
                            onComplete(selectedCurrency, selectedGoal, selectedReferral)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IncomeGreen,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = if (currentStep == 3) "Complete Setup 🚀" else "Continue",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    if (currentStep < 3) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun StepPrimaryGoal(
    goals: List<Pair<String, String>>,
    selectedGoal: String,
    onSelectGoal: (String) -> Unit
) {
    Column {
        Text(
            text = "Welcome to Self Budget! 👋",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "What is your primary financial focus right now?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        goals.forEach { (title, subtitle) ->
            val isSelected = selectedGoal == title
            Card(
                onClick = { onSelectGoal(title) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) IncomeGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) IncomeGreen.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint = IncomeGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepCurrencySetup(
    currencies: List<Pair<String, String>>,
    selectedCurrency: String,
    autoDetectedCurrency: String,
    onSelectCurrency: (String) -> Unit
) {
    Column {
        Text(
            text = "Set Your Primary Currency 🌐",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Select your base currency for accounts, budgets, and net worth reports.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Auto-detected region badge
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "System region auto-detected: $autoDetectedCurrency. Tap to change below.",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        currencies.forEach { (symbol, label) ->
            val isSelected = selectedCurrency == symbol
            Card(
                onClick = { onSelectCurrency(symbol) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) IncomeGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) IncomeGreen.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) IncomeGreen else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = symbol,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint = IncomeGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepReferralSource(
    referrals: List<Pair<String, String>>,
    selectedReferral: String,
    onSelectReferral: (String) -> Unit
) {
    Column {
        Text(
            text = "One Last Quick Question 📌",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "How did you hear about Self Budget?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        referrals.forEach { (title, subtitle) ->
            val isSelected = selectedReferral == title
            Card(
                onClick = { onSelectReferral(title) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) IncomeGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) IncomeGreen.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint = IncomeGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}
