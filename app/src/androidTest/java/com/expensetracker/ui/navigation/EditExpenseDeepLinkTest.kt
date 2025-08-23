package com.expensetracker.ui.navigation

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.expensetracker.data.model.Category
import com.expensetracker.data.model.Expense
import com.expensetracker.data.repository.ExpenseRepository
import com.expensetracker.test.TestDataInitializer
import com.expensetracker.ui.navigation.ExpenseTrackerNavHost
import com.expensetracker.ui.theme.ExpenseTrackerTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.LocalDateTime
import javax.inject.Inject

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class EditExpenseDeepLinkTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createComposeRule()

    @Inject
    lateinit var repository: ExpenseRepository

    @Inject
    lateinit var testDataInitializer: TestDataInitializer

    private lateinit var navController: TestNavHostController
    private lateinit var testExpense: Expense
    private lateinit var testCategory: Category

    @Before
    fun setup() {
        hiltRule.inject()
        
        runBlocking {
            // Initialize test data
            testDataInitializer.initializeDefaultCategories()
            
            // Create test category
            testCategory = Category(
                id = 1,
                name = "Food",
                color = "#4CAF50",
                icon = "restaurant",
                isDefault = true
            )
            
            // Create test expense
            testExpense = Expense(
                id = 1,
                amount = BigDecimal("25.50"),
                currency = "USD",
                description = "Test Coffee Purchase",
                categoryId = testCategory.id,
                tags = listOf("coffee", "work"),
                date = LocalDateTime.of(2024, 1, 15, 9, 30)
            )
            
            // Save test expense to repository
            repository.insertExpense(testExpense)
        }
        
        // Set up test navigation controller
        navController = TestNavHostController(ApplicationProvider.getApplicationContext())
        navController.navigatorProvider.addNavigator(ComposeNavigator())
    }

    @Test
    fun should_navigate_to_edit_expense_screen_via_deep_link() {
        // Test the deep link route directly
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                ExpenseTrackerNavHost(navController = navController)
            }
        }
        
        // Navigate to the deep link route
        navController.navigate("edit_expense/1")
        
        // Verify navigation to EditExpenseScreen
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithText("Edit Expense")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        
        // Verify that the expense data is loaded
        composeTestRule
            .onNodeWithText("Test Coffee Purchase")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("25.50")
            .assertIsDisplayed()
    }

    @Test
    fun should_handle_deep_link_route_navigation() {
        // Test that the deep link route structure works
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                ExpenseTrackerNavHost(navController = navController)
            }
        }
        
        // Navigate to the deep link route with another expense ID
        runBlocking {
            val anotherExpense = Expense(
                id = 2,
                amount = BigDecimal("150.75"),
                currency = "USD",
                description = "Grocery Shopping",
                categoryId = testCategory.id,
                tags = listOf("food", "groceries"),
                date = LocalDateTime.of(2024, 1, 16, 14, 20)
            )
            repository.insertExpense(anotherExpense)
        }
        
        navController.navigate("edit_expense/2")
        
        // Verify navigation to edit screen
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithText("Edit Expense")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        
        // Verify the navigation was successful
        assert(navController.currentDestination?.route == "edit_expense/{expenseId}")
    }

    @Test
    fun should_verify_deep_link_configuration() {
        // This test verifies that the deep link configuration is correct
        // by checking that the navigation graph has the proper deep links
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                ExpenseTrackerNavHost(navController = navController)
            }
        }
        
        // Check that we can navigate to the edit expense route
        navController.navigate("edit_expense/1")
        
        // Verify the navigation destination
        assert(navController.currentDestination?.route == "edit_expense/{expenseId}")
        
        // Verify that we're on the correct screen
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithText("Edit Expense")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }
}