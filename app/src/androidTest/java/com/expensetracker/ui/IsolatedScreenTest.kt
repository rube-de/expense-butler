package com.expensetracker.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import com.expensetracker.data.repository.ExpenseRepository
import com.expensetracker.test.TestDataInitializer
import com.expensetracker.ui.screens.AddExpenseViewModel
import com.expensetracker.ui.screens.AnalyticsViewModel
import com.expensetracker.ui.theme.ExpenseTrackerTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import javax.inject.Inject

/**
 * Base test class for isolated screen testing using direct composable content.
 * This approach tests screens in isolation without navigation overhead,
 * providing faster and more focused testing of UI behavior and logic.
 * 
 * Unlike BaseScreenTest which uses MainActivity with navigation,
 * this class directly instantiates screen composables for testing.
 */
@HiltAndroidTest
abstract class IsolatedScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createComposeRule()

    @Inject
    lateinit var repository: ExpenseRepository

    @Inject
    lateinit var testDataInitializer: TestDataInitializer

    // ViewModels for direct injection in isolated tests
    protected lateinit var addExpenseViewModel: AddExpenseViewModel
    protected lateinit var analyticsViewModel: AnalyticsViewModel

    /**
     * Base setup method that should be called by all subclasses.
     * Handles Hilt injection and test data initialization.
     */
    @Before
    open fun setup() {
        hiltRule.inject()
        
        // Initialize ViewModels manually for isolated testing
        addExpenseViewModel = AddExpenseViewModel(repository)
        analyticsViewModel = AnalyticsViewModel(repository)
        
        // Initialize default categories for all tests
        runBlocking {
            testDataInitializer.initializeDefaultCategories()
        }
    }

    /**
     * Sets the content for the isolated screen test.
     * Wraps the provided composable in the app theme.
     * 
     * @param content The composable content to test
     */
    protected fun setScreenContent(content: @Composable () -> Unit) {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                content()
            }
        }
    }

    /**
     * Initializes test expenses for tests that need sample data.
     * Call this in setup methods for tests that require populated data.
     */
    protected fun initializeTestExpenses() {
        runBlocking {
            testDataInitializer.initializeTestExpenses()
        }
        // Wait for UI to reflect the new data
        composeTestRule.waitForIdle()
    }

    /**
     * Creates a single test expense and returns its ID.
     * Useful for tests that need specific expense data.
     */
    protected fun createTestExpense(
        amount: String = "25.50",
        description: String = "Test Expense",
        categoryId: Long = 1L,
        tags: List<String> = emptyList()
    ): Long? {
        return runBlocking {
            testDataInitializer.createTestExpense(amount, description, categoryId, tags)
        }
    }

    /**
     * Waits for categories to be loaded before proceeding with tests.
     * Essential for tests that depend on category data.
     */
    protected fun waitForCategoriesToLoad(timeoutMs: Long = 10000) {
        runBlocking {
            testDataInitializer.waitForCategoriesLoaded(timeoutMs)
        }
        // Additional UI wait for categories to appear in components
        composeTestRule.waitForIdle()
    }
}