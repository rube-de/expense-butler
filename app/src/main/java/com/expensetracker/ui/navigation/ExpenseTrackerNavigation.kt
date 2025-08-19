package com.expensetracker.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.expensetracker.R
import com.expensetracker.ui.screens.AddExpenseScreen
import com.expensetracker.ui.screens.AnalyticsScreen
import com.expensetracker.ui.screens.EditExpenseScreen
import com.expensetracker.ui.screens.ExpenseListScreen
import com.expensetracker.ui.theme.spacing

data class BottomNavItem(
    val screen: Screen,
    val icon: ImageVector,
    val labelResId: Int
)

private val bottomNavItems = listOf(
    BottomNavItem(Screen.ExpenseList, Icons.Default.List, R.string.nav_expenses),
    BottomNavItem(Screen.AddExpense, Icons.Default.Add, R.string.nav_add),
    BottomNavItem(Screen.Analytics, Icons.Default.Home, R.string.nav_analytics),
    BottomNavItem(Screen.AIChat, Icons.Default.Info, R.string.nav_ai_chat),
    BottomNavItem(Screen.RecurringExpenses, Icons.Default.Refresh, R.string.nav_recurring)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseTrackerApp(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("navigation_bar")
            ) {
                bottomNavItems.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(stringResource(item.labelResId)) },
                        selected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true,
                        onClick = {
                            navController.navigate(item.screen.route) {
                                // Pop up to the start destination of the graph to
                                // avoid building up a large stack of destinations
                                // on the back stack as users select items
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                // Avoid multiple copies of the same destination when
                                // reselecting the same item
                                launchSingleTop = true
                                // Restore state when reselecting a previously selected item
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        ExpenseTrackerNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
fun ExpenseTrackerNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.ExpenseList.route,
        modifier = modifier
    ) {
        composable(Screen.ExpenseList.route) {
            ExpenseListScreen(
                onNavigateToAddExpense = { 
                    navController.navigate(Screen.AddExpense.route)
                },
                onNavigateToEditExpense = { expenseId ->
                    navController.navigate(Screen.EditExpense.createRoute(expenseId))
                }
            )
        }
        
        composable(Screen.AddExpense.route) {
            AddExpenseScreen(
                onNavigateBack = { navController.popBackStack() },
                onExpenseSaved = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Analytics.route) {
            AnalyticsScreen()
        }
        
        composable(Screen.AIChat.route) {
            // Placeholder for AIChatScreen
            PlaceholderScreen("AI Chat")
        }
        
        composable(Screen.RecurringExpenses.route) {
            // Placeholder for RecurringExpensesScreen
            PlaceholderScreen("Recurring Expenses")
        }
        
        composable(Screen.Settings.route) {
            // Placeholder for SettingsScreen
            PlaceholderScreen("Settings")
        }
        
        composable(
            route = Screen.EditExpense.route,
            arguments = listOf(navArgument("expenseId") { type = NavType.LongType })
        ) {
            EditExpenseScreen(
                onNavigateBack = { navController.popBackStack() },
                onExpenseUpdated = { navController.popBackStack() },
                onExpenseDeleted = { navController.popBackStack() }
            )
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String) {
    Surface {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(MaterialTheme.spacing.medium)
        )
    }
}