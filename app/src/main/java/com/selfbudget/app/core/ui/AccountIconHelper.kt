package com.selfbudget.app.core.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Elderly
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.unit.dp
import com.selfbudget.app.data.model.AccountType

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
        AccountType.INVESTMENT -> Icons.Default.Wallet
        AccountType.LOAN -> Icons.Default.AccountBalance
        AccountType.RETIREMENT -> RetirementAccountIcon
    }
}

/**
 * Returns display priority order for AccountType:
 * Checking -> Cash -> Credit Cards -> Savings -> Loans -> Investments -> Retirement
 */
fun getAccountTypePriority(type: AccountType): Int {
    return when (type) {
        AccountType.CHECKING -> 1
        AccountType.CASH -> 2
        AccountType.CREDIT_CARD -> 3
        AccountType.SAVINGS -> 4
        AccountType.LOAN -> 5
        AccountType.INVESTMENT -> 6
        AccountType.RETIREMENT -> 7
    }
}
