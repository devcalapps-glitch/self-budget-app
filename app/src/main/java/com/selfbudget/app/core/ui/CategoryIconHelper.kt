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
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.selfbudget.app.data.model.CategoryEntity
import com.selfbudget.app.ui.theme.icon
import com.selfbudget.app.ui.theme.sectionRamp

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
        
        // Groceries / Supermarket
        key.contains("cart") || key.contains("grocer") || nameKey.contains("grocer") || nameKey.contains("supermarket") -> Icons.Default.ShoppingCart

        // Food & Dining / Restaurants
        key.contains("restaurant") || key.contains("food") || key.contains("dining") || nameKey.contains("food") || nameKey.contains("dining") || nameKey.contains("cafe") || nameKey.contains("coffee") || nameKey.contains("restaurant") || nameKey.contains("takeout") -> Icons.Default.Restaurant
        
        // Shopping
        key.contains("shopping") || key.contains("bag") || nameKey.contains("shopping") -> Icons.Default.ShoppingBag
        
        // Transportation / Gas / Transit
        key.contains("bus") || key.contains("car") || key.contains("transport") || nameKey.contains("transport") || nameKey.contains("gas") || nameKey.contains("auto") -> Icons.Default.DirectionsBus
        
        // Travel / Flight / Vacation
        key.contains("flight") || key.contains("travel") || key.contains("plane") || key.contains("trip") || nameKey.contains("travel") || nameKey.contains("flight") || nameKey.contains("vacation") || nameKey.contains("trip") || nameKey.contains("hotel") || nameKey.contains("airline") -> Icons.Default.Flight

        // Bills & Utilities
        key.contains("receipt") || key.contains("bill") || nameKey.contains("bill") || nameKey.contains("utility") -> Icons.AutoMirrored.Filled.ReceiptLong
        
        // Entertainment / Movies / Games
        key.contains("movie") || key.contains("game") || nameKey.contains("entertainment") || nameKey.contains("movie") -> Icons.Default.Movie
        
        // Fitness / Gym / Workout
        key.contains("fitness") || key.contains("gym") || key.contains("workout") || key.contains("sport") || nameKey.contains("fitness") || nameKey.contains("gym") || nameKey.contains("workout") || nameKey.contains("exercise") || nameKey.contains("sport") -> Icons.Default.FitnessCenter

        // Health & Medical / Healthcare
        key.contains("medical") || key.contains("health") || key.contains("doctor") || key.contains("hospital") || key.contains("pharmacy") || nameKey.contains("health") || nameKey.contains("medical") || nameKey.contains("doctor") || nameKey.contains("pharmacy") || nameKey.contains("hospital") -> Icons.Default.MedicalServices
        
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

fun getExpenseCategoryGroup(category: CategoryEntity): String {
    if (!category.isDefault) return "Custom Categories"
    return when (category.id) {
        "cat_rent_mortgage", "cat_bills", "cat_health", "cat_transport" -> "Housing & Essentials"
        "cat_groceries", "cat_food", "cat_shopping" -> "Food & Daily Living"
        "cat_subscriptions", "cat_entertainment", "cat_travel", "cat_fitness" -> "Lifestyle & Entertainment"
        "cat_credit_card_loan", "cat_investment_expense", "cat_transfer", "cat_other" -> "Debt & Financial"
        else -> {
            val name = category.name.lowercase()
            when {
                name.contains("rent") || name.contains("mortgage") || name.contains("bill") || name.contains("utilit") || name.contains("health") || name.contains("transport") || name.contains("gas") || name.contains("car") -> "Housing & Essentials"
                name.contains("grocer") || name.contains("food") || name.contains("dining") || name.contains("shop") || name.contains("restaurant") -> "Food & Daily Living"
                name.contains("sub") || name.contains("entertain") || name.contains("movie") || name.contains("travel") || name.contains("fitness") || name.contains("gym") -> "Lifestyle & Entertainment"
                name.contains("debt") || name.contains("card") || name.contains("credit") || name.contains("loan") || name.contains("invest") -> "Debt & Financial"
                else -> "Other"
            }
        }
    }
}

fun getIncomeCategoryGroup(category: CategoryEntity): String {
    if (!category.isDefault) return "Custom Categories"
    return when (category.id) {
        "cat_salary", "cat_side_hustle" -> "Earned Income"
        "cat_investment" -> "Investments & Passive"
        "cat_gifts", "cat_income_others" -> "Gifts & Other"
        else -> "Other"
    }
}

/**
 * Returns the design-system section-identity color for each expense category
 * group (spec "Section identity colors": Housing=Blue, Food=Teal,
 * Lifestyle=Pink, Debt=Coral, Custom=Purple, Other=Gray), read from the same
 * ramp used for that group's header band, border, and icon tiles elsewhere.
 */
fun getExpenseCategoryGroupColor(group: String, isDark: Boolean = false): Color {
    return sectionRamp(group).icon(isDark)
}

/**
 * Returns a distinct ImageVector icon for each expense category group.
 */
fun getExpenseCategoryGroupIcon(group: String): ImageVector {
    return when (group) {
        "Housing & Essentials" -> Icons.Default.Home
        "Food & Daily Living" -> Icons.Default.Restaurant
        "Lifestyle & Entertainment" -> Icons.Default.Movie
        "Debt & Financial" -> Icons.Default.CreditCard
        "Custom Categories" -> Icons.Default.Category
        else -> Icons.Default.MoreHoriz
    }
}

