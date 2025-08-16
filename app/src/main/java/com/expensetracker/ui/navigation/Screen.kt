package com.expensetracker.ui.navigation

sealed class Screen(val route: String) {
    object ExpenseList : Screen("expense_list")
    object AddExpense : Screen("add_expense")
    object Analytics : Screen("analytics")
    object AIChat : Screen("ai_chat")
    object RecurringExpenses : Screen("recurring_expenses")
    object Settings : Screen("settings")
    
    // Navigation with arguments
    object EditExpense : Screen("edit_expense/{expenseId}") {
        fun createRoute(expenseId: Long) = "edit_expense/$expenseId"
    }
}