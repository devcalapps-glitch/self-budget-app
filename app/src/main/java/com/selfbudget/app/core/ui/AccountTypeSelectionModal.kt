package com.selfbudget.app.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.selfbudget.app.core.ui.components.CircularBackButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.selfbudget.app.data.model.AccountType
import com.selfbudget.app.ui.theme.Ramp
import com.selfbudget.app.ui.theme.SelfBudgetType
import com.selfbudget.app.ui.theme.ShapeCard
import com.selfbudget.app.ui.theme.containerBorder
import com.selfbudget.app.ui.theme.icon
import com.selfbudget.app.ui.theme.isAppInDarkTheme
import com.selfbudget.app.ui.theme.titleText
import com.selfbudget.app.ui.theme.tintFill

data class AccountTypeItem(
    val type: AccountType,
    val label: String,
    val description: String,
    val isLiability: Boolean,
    val colorHex: String
)

val ALL_ACCOUNT_TYPES = listOf(
    AccountTypeItem(AccountType.CHECKING, "Checking", "Everyday spending and cash flow", false, "#2563EB"),
    AccountTypeItem(AccountType.SAVINGS, "Savings Account", "Emergency funds & liquid savings", false, "#0F766E"),
    AccountTypeItem(AccountType.CASH, "Cash Wallet", "Physical cash on hand", false, "#059669"),
    AccountTypeItem(AccountType.CREDIT_CARD, "Credit Card", "Revolving credit line & rewards", true, "#DC2626"),
    AccountTypeItem(AccountType.MORTGAGE, "Mortgage", "Home loan & property financing", true, "#7C3AED"),
    AccountTypeItem(AccountType.AUTO_LOAN, "Auto Loan", "Vehicle financing & debt", true, "#EA580C"),
    AccountTypeItem(AccountType.STUDENT_LOAN, "Student Loan", "Education loan debt", true, "#9333EA"),
    AccountTypeItem(AccountType.LOAN, "Personal / Other Loan", "Fixed loan or debt balance", true, "#B45309"),
    AccountTypeItem(AccountType.INVESTMENT, "Investment", "Taxable brokerage & stocks", false, "#0891B2"),
    AccountTypeItem(AccountType.RETIREMENT, "Retirement", "401(k), IRA, non-liquid retirement", false, "#475569"),
    AccountTypeItem(AccountType.REAL_ESTATE, "Real Estate / Property", "Primary residence or rental property value", false, "#15803D"),
    AccountTypeItem(AccountType.VEHICLE, "Vehicle / Auto", "Estimated car or automobile value", false, "#4F46E5")
)

/** Cash-like, everyday/liquid account types (non-liability). */
private val LIQUID_ACCOUNT_TYPES = setOf(AccountType.CHECKING, AccountType.SAVINGS, AccountType.CASH)

@Composable
fun AccountTypeSelectionModal(
    selectedType: AccountType,
    onDismiss: () -> Unit,
    onSelectType: (AccountType) -> Unit
) {
    val liquidItems = ALL_ACCOUNT_TYPES.filter { !it.isLiability && it.type in LIQUID_ACCOUNT_TYPES }
    val debtItems = ALL_ACCOUNT_TYPES.filter { it.isLiability }
    val assetItems = ALL_ACCOUNT_TYPES.filter { !it.isLiability && it.type !in LIQUID_ACCOUNT_TYPES }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar
                Surface(
                    color = MaterialTheme.colorScheme.background
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularBackButton(onClick = onDismiss)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Select account type",
                                style = SelfBudgetType.title,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    AccountTypeSectionLabel(text = "Liquid")
                    AccountTypeGroup(
                        items = liquidItems,
                        selectedType = selectedType,
                        isDebtSection = false,
                        onSelectType = onSelectType
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    AccountTypeSectionLabel(text = "Debt")
                    AccountTypeGroup(
                        items = debtItems,
                        selectedType = selectedType,
                        isDebtSection = true,
                        onSelectType = onSelectType
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    AccountTypeSectionLabel(text = "Assets")
                    AccountTypeGroup(
                        items = assetItems,
                        selectedType = selectedType,
                        isDebtSection = false,
                        onSelectType = onSelectType
                    )

                    Spacer(modifier = Modifier.height(140.dp))
                }
            }
        }
    }
}

@Composable
private fun AccountTypeSectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = SelfBudgetType.eyebrow,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp, start = 2.dp)
    )
}

@Composable
private fun AccountTypeGroup(
    items: List<AccountTypeItem>,
    selectedType: AccountType,
    isDebtSection: Boolean,
    onSelectType: (AccountType) -> Unit
) {
    val isDark = isAppInDarkTheme()
    val borderColor = if (isDebtSection) {
        Ramp.Coral.containerBorder(isDark)
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ShapeCard)
            .background(MaterialTheme.colorScheme.surface)
            .border(BorderStroke(0.5.dp, borderColor), ShapeCard)
    ) {
        items.forEachIndexed { index, item ->
            if (index > 0) {
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            }
            AccountTypeRow(
                item = item,
                isSelected = item.type == selectedType,
                isDebtSection = isDebtSection,
                onClick = { onSelectType(item.type) }
            )
        }
    }
}

@Composable
private fun AccountTypeRow(
    item: AccountTypeItem,
    isSelected: Boolean,
    isDebtSection: Boolean,
    onClick: () -> Unit
) {
    val isDark = isAppInDarkTheme()
    val iconTint = when {
        isDebtSection -> Ramp.Coral.icon(isDark)
        isSelected -> Ramp.Teal.titleText(isDark)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) Ramp.Teal.tintFill(isDark) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = getAccountIcon(item.type),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.height(20.dp).width(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = item.label,
            style = SelfBudgetType.body,
            color = if (isSelected) Ramp.Teal.titleText(isDark) else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = Ramp.Teal.titleText(isDark),
                modifier = Modifier.height(18.dp).width(18.dp)
            )
        }
    }
}
