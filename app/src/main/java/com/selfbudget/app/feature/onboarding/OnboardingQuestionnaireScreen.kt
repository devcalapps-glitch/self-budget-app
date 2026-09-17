package com.selfbudget.app.feature.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.selfbudget.app.ui.theme.ShapeCard
import com.selfbudget.app.ui.theme.ShapePill
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

/** A single onboarding choice: a Material icon paired with its label. */
private data class OnboardingOption(val icon: ImageVector, val title: String)

@Composable
fun OnboardingQuestionnaireScreen(
    onComplete: (preferredCurrency: String, primaryGoal: String, referralSource: String) -> Unit
) {
    // Currency is auto-detected from the device locale and applied silently —
    // it's still changeable anytime from Settings, so onboarding doesn't ask.
    val autoDetectedCurrency = remember { detectSystemCurrencySymbol() }

    var currentStep by remember { mutableIntStateOf(1) }
    var selectedGoal by remember { mutableStateOf<String?>(null) }
    var selectedReferral by remember { mutableStateOf<String?>(null) }

    val goals = listOf(
        OnboardingOption(Icons.Default.PieChart, "Track & control my spending"),
        OnboardingOption(Icons.Default.Savings, "Grow my savings"),
        OnboardingOption(Icons.Default.CreditCard, "Pay off my debt"),
        OnboardingOption(Icons.Default.CalendarMonth, "Master my monthly budget")
    )

    val referralSources = listOf(
        OnboardingOption(Icons.Default.Public, "Social media"),
        OnboardingOption(Icons.Default.Groups, "A friend or family member"),
        OnboardingOption(Icons.Default.Search, "Searched the app store"),
        OnboardingOption(Icons.Default.SmartDisplay, "An article or YouTube video"),
        OnboardingOption(Icons.Default.MoreHoriz, "Something else")
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
                NeutralBadge(text = "Step $currentStep of 2")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Segmented step progress: one pill per step, filled as the user advances.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(2) { index ->
                    val isFilled = index < currentStep
                    val segmentColor by animateColorAsState(
                        targetValue = if (isFilled) getProgressBarColor() else if (isDark) ProgressTrackDark else ProgressTrackLight,
                        label = "stepSegment${index + 1}"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(ShapePill)
                            .background(segmentColor)
                    )
                }
            }

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
                    2 -> StepReferralSource(
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

                val canContinue = if (currentStep == 1) selectedGoal != null else selectedReferral != null

                PrimaryPillButton(
                    text = if (currentStep == 2) "Complete setup" else "Continue",
                    enabled = canContinue,
                    onClick = {
                        if (currentStep < 2) {
                            currentStep += 1
                        } else {
                            val goal = selectedGoal
                            val referral = selectedReferral
                            if (goal != null && referral != null) {
                                onComplete(autoDetectedCurrency, goal, referral)
                            }
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

/**
 * A single-select square tile shared by all onboarding steps: centered icon
 * badge over a title, with a teal tint + border + checkmark when selected.
 */
@Composable
private fun SelectableOptionTile(
    icon: ImageVector,
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme()
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) Ramp.Teal.tintFill(isDark) else MaterialTheme.colorScheme.surface,
        label = "optionTileContainer"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) Ramp.Teal.titleText(isDark) else MaterialTheme.colorScheme.outlineVariant,
        label = "optionTileBorder"
    )

    Surface(
        onClick = onClick,
        shape = ShapeCard,
        color = containerColor,
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor),
        modifier = modifier.aspectRatio(1f)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(14.dp)) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = Ramp.Teal.titleText(isDark),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(20.dp)
                )
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isSelected) Ramp.Teal.solidFill(isDark) else Ramp.Gray.tintFill(isDark),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) Ramp.Teal.onSolidFill(isDark) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = title,
                    style = SelfBudgetType.rowTitle,
                    color = if (isSelected) Ramp.Teal.titleText(isDark) else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 3
                )
            }
        }
    }
}

/** Lays [items] out as square tiles in a 2-column grid, keeping the last odd tile half-width. */
@Composable
private fun OptionTileGrid(
    items: List<OnboardingOption>,
    selectedTitle: String?,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEach { option ->
                    SelectableOptionTile(
                        icon = option.icon,
                        title = option.title,
                        isSelected = selectedTitle == option.title,
                        onClick = { onSelect(option.title) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StepPrimaryGoal(
    goals: List<OnboardingOption>,
    selectedGoal: String?,
    onSelectGoal: (String) -> Unit
) {
    Column {
        Text(
            text = "Let's tailor Self Budget to you",
            style = SelfBudgetType.title,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Choose the focus that matters most to you right now — we'll shape your experience around it.",
            style = SelfBudgetType.body,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        OptionTileGrid(items = goals, selectedTitle = selectedGoal, onSelect = onSelectGoal)
    }
}

@Composable
private fun StepReferralSource(
    referrals: List<OnboardingOption>,
    selectedReferral: String?,
    onSelectReferral: (String) -> Unit
) {
    Column {
        Text(
            text = "Just one more thing",
            style = SelfBudgetType.title,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Tell us how you found Self Budget — it helps us keep improving.",
            style = SelfBudgetType.body,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        OptionTileGrid(items = referrals, selectedTitle = selectedReferral, onSelect = onSelectReferral)
    }
}
