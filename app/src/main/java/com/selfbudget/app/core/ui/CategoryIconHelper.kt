package com.selfbudget.app.core.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector
import com.selfbudget.app.data.model.CategoryEntity

/**
 * Returns a specific Compose Material3 icon for a given category based on its iconName and category name.
 */
fun getCategoryIcon(iconName: String?, categoryName: String? = null): ImageVector {
    val key = iconName?.lowercase() ?: ""
    val nameKey = categoryName?.lowercase() ?: ""

    return when {
        // Rent / Mortgage / Home
        key.contains("home") || key.contains("house") || nameKey.contains("rent") || nameKey.contains("mortgage") -> Icons.Default.Home
        
        // Credit Card / Loan / Debt
        key.contains("creditcard") || key.contains("card") || nameKey.contains("credit") || nameKey.contains("loan") || nameKey.contains("debt") -> Icons.Default.CreditCard
        
        // Subscriptions
        key.contains("subscription") || nameKey.contains("subscription") || nameKey.contains("sub") -> Icons.Default.Subscriptions
        
        // Food & Dining / Restaurants
        key.contains("restaurant") || key.contains("food") || nameKey.contains("food") || nameKey.contains("dining") -> Icons.Default.Restaurant
        
        // Shopping
        key.contains("shopping") || key.contains("bag") || nameKey.contains("shopping") -> Icons.Default.ShoppingBag
        
        // Transportation / Gas / Transit
        key.contains("bus") || key.contains("car") || key.contains("transport") || nameKey.contains("transport") || nameKey.contains("gas") || nameKey.contains("auto") -> Icons.Default.DirectionsBus
        
        // Bills & Utilities
        key.contains("receipt") || key.contains("bill") || nameKey.contains("bill") || nameKey.contains("utility") -> Icons.AutoMirrored.Filled.ReceiptLong
        
        // Entertainment / Movies / Games
        key.contains("movie") || key.contains("game") || nameKey.contains("entertainment") || nameKey.contains("movie") -> Icons.Default.Movie
        
        // Health & Fitness / Medical
        key.contains("medical") || key.contains("health") || nameKey.contains("health") || nameKey.contains("fitness") -> Icons.Default.MedicalServices
        
        // Salary / Paycheck / Wallet
        key.contains("wallet") || key.contains("salary") || nameKey.contains("salary") || nameKey.contains("paycheck") || nameKey.contains("income") -> Icons.Default.AccountBalanceWallet
        
        // Gifts
        key.contains("gift") || nameKey.contains("gift") -> Icons.Default.CardGiftcard
        
        // Investment / Stocks
        key.contains("trending") || key.contains("invest") || nameKey.contains("invest") || nameKey.contains("stock") -> Icons.AutoMirrored.Filled.TrendingUp
        
        // Side Hustle / Work / Job
        key.contains("work") || nameKey.contains("hustle") || nameKey.contains("work") || nameKey.contains("job") -> Icons.Default.Work
        
        // Account Transfer / Swap
        key.contains("compare") || key.contains("transfer") || key.contains("swap") || nameKey.contains("transfer") -> Icons.AutoMirrored.Filled.CompareArrows

        // Other / Miscellaneous
        key.contains("more") || nameKey.contains("other") -> Icons.Default.MoreHoriz
        
        else -> Icons.Default.Category
    }
}

fun getCategoryIcon(category: CategoryEntity?): ImageVector {
    if (category == null) return Icons.Default.Category
    return getCategoryIcon(category.iconName, category.name)
}
