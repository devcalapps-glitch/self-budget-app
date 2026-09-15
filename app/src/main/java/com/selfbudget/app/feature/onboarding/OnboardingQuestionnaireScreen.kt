package com.selfbudget.app.feature.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.selfbudget.app.core.ui.AppLogoBadge
import com.selfbudget.app.core.ui.components.NeutralBadge
import com.selfbudget.app.core.ui.components.PrimaryPillButton
import com.selfbudget.app.core.ui.components.SecondaryPillButton
import com.selfbudget.app.ui.theme.ProgressTrackDark
import com.selfbudget.app.ui.theme.ProgressTrackLight
import com.selfbudget.app.ui.theme.Ramp
import com.selfbudget.app.ui.theme.SelfBudgetType
import com.selfbudget.app.ui.theme.ShapeChip
import com.selfbudget.app.ui.theme.getProgressBarColor
import com.selfbudget.app.ui.theme.isAppInDarkTheme
import com.selfbudget.app.ui.theme.onSolidFill
import com.selfbudget.app.ui.theme.solidFill
import com.selfbudget.app.ui.theme.tintFill
import com.selfbudget.app.ui.theme.titleText
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

    val isDark = isAppInDarkTheme()

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
                NeutralBadge(text = "Step $currentStep of 3")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Step Progress Bar
            LinearProgressIndicator(
                progress = { currentStep / 3.0f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(ShapeChip),
                color = getProgressBarColor(),
                trackColor = if (isDark) ProgressTrackDark else ProgressTrackLight
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
                    SecondaryPillButton(
                        text = "Back",
                        onClick = { currentStep -= 1 },
                        modifier = Modifier
                            .weight(0.4f)
                            .height(52.dp)
                    )
                }

                PrimaryPillButton(
                    text = if (currentStep == 3) "Complete setup" else "Continue",
                    onClick = {
                        if (currentStep < 3) {
                            currentStep += 1
                        } else {
                            onComplete(selectedCurrency, selectedGoal, selectedReferral)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                )
            }
        }
    }
}

/** A single-select option row shared by all onboarding steps (spec §20: Teal selected state). */
@Composable
private fun SelectableOptionRow(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isDark = isAppInDarkTheme()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ShapeChip)
            .clickable { onClick() }
            .background(if (isSelected) Ramp.Teal.tintFill(isDark) else Color.Transparent)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = SelfBudgetType.rowTitle,
                color = if (isSelected) Ramp.Teal.titleText(isDark) else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = subtitle,
                style = SelfBudgetType.meta,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (isSelected) {
            Spacer(modifier = Modifier.width(12.dp))
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Selected",
                tint = Ramp.Teal.titleText(isDark),
                modifier = Modifier.size(24.dp)
            )
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
            text = "Welcome to Self Budget",
            style = SelfBudgetType.title,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "What is your primary financial focus right now?",
            style = SelfBudgetType.body,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        goals.forEachIndexed { index, (title, subtitle) ->
            SelectableOptionRow(title, subtitle, selectedGoal == title) { onSelectGoal(title) }
            if (index < goals.size - 1) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
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
    val isDark = isAppInDarkTheme()
    Column {
        Text(
            text = "Set your primary currency",
            style = SelfBudgetType.title,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Select your base currency for accounts, budgets, and net worth reports.",
            style = SelfBudgetType.body,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Auto-detected region notice (spec §12: neutral gray, not a warning tint)
        Surface(
            shape = ShapeChip,
            color = Ramp.Gray.tintFill(isDark)
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
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "System region auto-detected: $autoDetectedCurrency. Tap to change below.",
                    style = SelfBudgetType.meta,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        currencies.forEachIndexed { index, (symbol, label) ->
            val isSelected = selectedCurrency == symbol
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ShapeChip)
                    .clickable { onSelectCurrency(symbol) }
                    .background(if (isSelected) Ramp.Teal.tintFill(isDark) else Color.Transparent)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isSelected) Ramp.Teal.solidFill(isDark) else Ramp.Gray.tintFill(isDark),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = symbol,
                                style = SelfBudgetType.rowTitle,
                                color = if (isSelected) Ramp.Teal.onSolidFill(isDark) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = label,
                        style = SelfBudgetType.rowTitle,
                        color = if (isSelected) Ramp.Teal.titleText(isDark) else MaterialTheme.colorScheme.onSurface
                    )
                }

                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = Ramp.Teal.titleText(isDark),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            if (index < currencies.size - 1) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
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
            text = "One last quick question",
            style = SelfBudgetType.title,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "How did you hear about Self Budget?",
            style = SelfBudgetType.body,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        referrals.forEachIndexed { index, (title, subtitle) ->
            SelectableOptionRow(title, subtitle, selectedReferral == title) { onSelectReferral(title) }
            if (index < referrals.size - 1) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}
