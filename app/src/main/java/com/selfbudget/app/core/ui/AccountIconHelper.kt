package com.selfbudget.app.core.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Elderly
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.unit.dp
import com.selfbudget.app.data.model.AccountType
import com.selfbudget.app.ui.theme.Ramp
import com.selfbudget.app.ui.theme.icon

/**
 * Custom retirement account icon combining the elderly silhouette with a distinct dollar ($) sign in the top corner.
 */
val RetirementAccountIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "RetirementAccount",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        // Base elderly figure
        for (node in Icons.Default.Elderly.root) {
            if (node is VectorPath) {
                addPath(
                    pathData = node.pathData,
                    fill = SolidColor(Color.Black)
                )
            }
        }
        // Scaled and positioned dollar ($) sign
        addGroup(
            name = "dollar_sign_badge",
            scaleX = 0.42f,
            scaleY = 0.42f,
            translationX = 0.5f,
            translationY = 0.5f
        )
        for (node in Icons.Default.AttachMoney.root) {
            if (node is VectorPath) {
                addPath(
                    pathData = node.pathData,
                    fill = SolidColor(Color.Black)
                )
            }
        }
        clearGroup()
    }.build()
}

/**
 * Returns the standard ImageVector icon for a given AccountType.
 */
fun getAccountIcon(type: AccountType): ImageVector {
    return when (type) {
        AccountType.CHECKING -> Icons.Default.AccountBalance
        AccountType.CREDIT_CARD -> Icons.Default.CreditCard
        AccountType.CASH -> Icons.Default.Payments
        AccountType.SAVINGS -> Icons.Default.Savings
        AccountType.INVESTMENT -> Icons.AutoMirrored.Filled.TrendingUp
        AccountType.LOAN -> Icons.Default.AccountBalance
        AccountType.RETIREMENT -> RetirementAccountIcon
        AccountType.MORTGAGE -> Icons.Default.Home
        AccountType.AUTO_LOAN -> Icons.Default.DirectionsCar
        AccountType.STUDENT_LOAN -> Icons.Default.School
        AccountType.REAL_ESTATE -> Icons.Default.Home
        AccountType.VEHICLE -> Icons.Default.DirectionsCar
    }
}

/**
 * Returns a human-friendly display label for each AccountType.
 */
fun getAccountTypeLabel(type: AccountType): String {
    return when (type) {
        AccountType.CHECKING -> "Checking"
        AccountType.CREDIT_CARD -> "Credit Card"
        AccountType.SAVINGS -> "Savings Account"
        AccountType.CASH -> "Cash Wallet"
        AccountType.INVESTMENT -> "Investment"
        AccountType.LOAN -> "Personal / Other Loan"
        AccountType.RETIREMENT -> "Retirement (Non-Liquid)"
        AccountType.MORTGAGE -> "Mortgage (Home Loan)"
        AccountType.AUTO_LOAN -> "Auto Loan"
        AccountType.STUDENT_LOAN -> "Student Loan"
        AccountType.REAL_ESTATE -> "Real Estate / Property"
        AccountType.VEHICLE -> "Vehicle / Automobile"
    }
}

/**
 * Returns display priority order for AccountType:
 * Checking -> Cash -> Credit Cards -> Savings -> Mortgages -> Auto Loans -> Student Loans -> Loans -> Real Estate -> Vehicles -> Investments -> Retirement
 */
fun getAccountTypePriority(type: AccountType): Int {
    return when (type) {
        AccountType.CHECKING -> 1
        AccountType.CASH -> 2
        AccountType.CREDIT_CARD -> 3
        AccountType.SAVINGS -> 4
        AccountType.MORTGAGE -> 5
        AccountType.AUTO_LOAN -> 6
        AccountType.STUDENT_LOAN -> 7
        AccountType.LOAN -> 8
        AccountType.REAL_ESTATE -> 9
        AccountType.VEHICLE -> 10
        AccountType.INVESTMENT -> 11
        AccountType.RETIREMENT -> 12
    }
}

/**
 * The design-system section-identity ramp for each AccountType: everyday
 * cash accounts read as "Accounts and wallets" (Teal), debt instruments as
 * "Debt & Financial" (Coral), real estate as the Housing identity (Blue),
 * and investment/retirement as the Custom-categories identity (Purple) —
 * the same ramps used for banded headers elsewhere.
 */
fun accountTypeRamp(type: AccountType): Ramp = when (type) {
    AccountType.CHECKING, AccountType.CASH, AccountType.SAVINGS, AccountType.VEHICLE -> Ramp.Teal
    AccountType.CREDIT_CARD, AccountType.MORTGAGE, AccountType.LOAN, AccountType.AUTO_LOAN, AccountType.STUDENT_LOAN -> Ramp.Coral
    AccountType.REAL_ESTATE -> Ramp.Blue
    AccountType.INVESTMENT, AccountType.RETIREMENT -> Ramp.Purple
}

/** Flat [Color] form of [accountTypeRamp] for call sites that need a single color rather than the ramp. */
fun getAccountTypeColor(type: AccountType, isDark: Boolean = false): Color = accountTypeRamp(type).icon(isDark)

