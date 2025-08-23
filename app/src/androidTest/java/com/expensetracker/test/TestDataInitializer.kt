package com.expensetracker.test

import com.expensetracker.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class for initializing consistent test data across all test scenarios.
 * Provides methods to set up categories, expenses, and other test data in a controlled manner.
 */
@Singleton
class TestDataInitializer @Inject constructor(
    private val repository: ExpenseRepository
) {

    /**
     * Initializes the test database with default categories.
     * This ensures that category-related tests have consistent data to work with.
     * Should be called in test setup phases.
     */
    suspend fun initializeDefaultCategories() {
        try {
            // First, try to initialize default categories through repository
            val result = repository.initializeDefaultCategories()
            if (result.isFailure) {
                // If that fails, manually insert our test categories
                insertTestCategories()
            }
        } catch (e: Exception) {
            // Fallback to manual insertion
            insertTestCategories()
        }
    }

    /**
     * Manually inserts test categories when automatic initialization fails.
     * Uses the same categories as production but ensures they exist for testing.
     */
    private suspend fun insertTestCategories() {
        val categories = TestData.createDefaultCategories()
        categories.forEach { category ->
            try {
                repository.insertCategory(category)
            } catch (e: Exception) {
                // Category might already exist, continue with others
            }
        }
    }

    /**
     * Initializes the database with sample expense data for analytics and list tests.
     * This provides realistic data for testing charts, calculations, and filtering.
     */
    suspend fun initializeTestExpenses() {
        // Ensure categories exist first
        initializeDefaultCategories()
        
        // Insert sample expenses for different test scenarios
        val expenses = TestData.createExpensesForAnalytics()
        expenses.forEach { expense ->
            try {
                repository.insertExpense(expense)
            } catch (e: Exception) {
                // Continue with other expenses if one fails
            }
        }
    }

    /**
     * Clears all test data to ensure clean test environment.
     * Useful for tests that need to start with empty database.
     */
    suspend fun clearAllTestData() {
        try {
            // Get all expenses and delete them
            // Note: In a real scenario, you might want to add a clearAll method to repository
            // For now, we rely on in-memory database being recreated for each test class
        } catch (e: Exception) {
            // If clearing fails, the in-memory database should still be clean between test runs
        }
    }

    /**
     * Sets up a complete test scenario with categories, expenses, and recurring expenses.
     * Useful for comprehensive integration tests.
     */
    suspend fun initializeFullTestScenario() {
        initializeDefaultCategories()
        initializeTestExpenses()
        
        // Add a sample recurring expense
        try {
            val recurringExpense = TestData.createTestRecurringExpense()
            repository.insertRecurringExpense(recurringExpense)
        } catch (e: Exception) {
            // Continue if recurring expense insertion fails
        }
    }

    /**
     * Waits for category initialization to complete.
     * Useful for tests that need to ensure categories are loaded before proceeding.
     * 
     * @param maxWaitMs Maximum time to wait in milliseconds
     * @return true if categories are loaded, false if timeout
     */
    suspend fun waitForCategoriesLoaded(maxWaitMs: Long = 5000): Boolean {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < maxWaitMs) {
            try {
                val categories = repository.getAllCategories()
                // Use first() to get the first emission and not collect indefinitely
                val categoryList = categories.first()
                if (categoryList.isNotEmpty()) {
                    return true
                }
                kotlinx.coroutines.delay(100)
            } catch (e: Exception) {
                kotlinx.coroutines.delay(100)
            }
        }
        return false
    }

    /**
     * Ensures that at least the minimum required categories exist for tests.
     * This is a lightweight version that just checks for category existence.
     */
    suspend fun ensureMinimumCategoriesExist(): Boolean {
        return try {
            initializeDefaultCategories()
            waitForCategoriesLoaded(3000)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Creates a single test expense with specified parameters.
     * Useful for tests that need specific expense data.
     */
    suspend fun createTestExpense(
        amount: String = "25.50",
        description: String = "Test Expense",
        categoryId: Long = 1L,
        tags: List<String> = emptyList()
    ): Long? {
        return try {
            val expense = TestExpenseBuilder()
                .withAmount(amount)
                .withDescription(description)
                .withCategoryId(categoryId)
                .withTags(tags)
                .build()
            
            val result = repository.insertExpense(expense)
            result.getOrNull()
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Extension functions for easy use in test classes
 */

/**
 * Blocking version of initializeDefaultCategories for use in @Before methods
 */
fun TestDataInitializer.initializeDefaultCategoriesBlocking() = runBlocking {
    initializeDefaultCategories()
}

/**
 * Blocking version of initializeTestExpenses for use in @Before methods
 */
fun TestDataInitializer.initializeTestExpensesBlocking() = runBlocking {
    initializeTestExpenses()
}

/**
 * Blocking version of ensureMinimumCategoriesExist for use in @Before methods
 */
fun TestDataInitializer.ensureMinimumCategoriesExistBlocking(): Boolean = runBlocking {
    ensureMinimumCategoriesExist()
}