package com.expensetracker.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.expensetracker.data.database.AppDatabase
import com.expensetracker.data.model.Category
import com.expensetracker.data.model.Expense
import com.expensetracker.data.model.RecurringExpense
import com.expensetracker.data.model.RecurrenceFrequency
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Integration tests for ExpenseRepository using in-memory Room database.
 * These tests verify the complete data flow from repository to database.
 */
@RunWith(AndroidJUnit4::class)
class ExpenseRepositoryIntegrationTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: ExpenseRepositoryImpl

    @Before
    fun setup() {
        // Create in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        // Create repository with real DAOs
        repository = ExpenseRepositoryImpl(
            database.expenseDao(),
            database.categoryDao(),
            database.recurringExpenseDao()
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `should perform complete expense lifecycle with database`() = runTest {
        // Arrange - Create and insert category first
        val category = Category(
            name = "Food",
            color = "#FF5722",
            icon = "restaurant",
            isDefault = false
        )
        val categoryResult = repository.insertCategory(category)
        assertTrue("Category insertion should succeed", categoryResult.isSuccess)
        val categoryId = categoryResult.getOrNull()!!

        // Act 1 - Insert expense
        val expense = Expense(
            amount = BigDecimal("25.50"),
            currency = "USD",
            description = "Lunch at restaurant",
            categoryId = categoryId,
            tags = listOf("lunch", "restaurant"),
            date = LocalDateTime.now(),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        val insertResult = repository.insertExpense(expense)
        assertTrue("Expense insertion should succeed", insertResult.isSuccess)
        val expenseId = insertResult.getOrNull()!!

        // Assert 1 - Verify expense was inserted
        repository.getAllExpenses().test {
            val expenses = awaitItem()
            assertEquals("Should have 1 expense", 1, expenses.size)
            assertEquals("Should have correct description", "Lunch at restaurant", expenses[0].description)
            assertEquals("Should have correct amount", BigDecimal("25.50"), expenses[0].amount)
        }

        // Act 2 - Update expense
        val updatedExpense = expense.copy(
            id = expenseId,
            description = "Updated lunch",
            amount = BigDecimal("30.00")
        )
        val updateResult = repository.updateExpense(updatedExpense)
        assertTrue("Expense update should succeed", updateResult.isSuccess)

        // Assert 2 - Verify expense was updated
        repository.getAllExpenses().test {
            val expenses = awaitItem()
            assertEquals("Should still have 1 expense", 1, expenses.size)
            assertEquals("Should have updated description", "Updated lunch", expenses[0].description)
            assertEquals("Should have updated amount", BigDecimal("30.00"), expenses[0].amount)
        }

        // Act 3 - Delete expense
        val deleteResult = repository.deleteExpense(expenseId)
        assertTrue("Expense deletion should succeed", deleteResult.isSuccess)

        // Assert 3 - Verify expense was deleted
        repository.getAllExpenses().test {
            val expenses = awaitItem()
            assertEquals("Should have 0 expenses", 0, expenses.size)
        }
    }

    @Test
    fun `should filter expenses by multiple criteria with database`() = runTest {
        // Arrange - Create categories
        val foodCategory = Category(name = "Food", color = "#FF5722", icon = "restaurant", isDefault = false)
        val transportCategory = Category(name = "Transport", color = "#2196F3", icon = "car", isDefault = false)
        
        val foodCategoryId = repository.insertCategory(foodCategory).getOrNull()!!
        val transportCategoryId = repository.insertCategory(transportCategory).getOrNull()!!

        // Create expenses with different dates, categories, and tags
        val expenses = listOf(
            Expense(
                amount = BigDecimal("25.50"),
                currency = "USD",
                description = "Lunch",
                categoryId = foodCategoryId,
                tags = listOf("lunch", "restaurant"),
                date = LocalDateTime.of(2024, 1, 15, 12, 0),
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            ),
            Expense(
                amount = BigDecimal("50.00"),
                currency = "USD",
                description = "Gas",
                categoryId = transportCategoryId,
                tags = listOf("fuel", "car"),
                date = LocalDateTime.of(2024, 1, 20, 10, 0),
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            ),
            Expense(
                amount = BigDecimal("15.00"),
                currency = "USD",
                description = "Coffee",
                categoryId = foodCategoryId,
                tags = listOf("coffee", "morning"),
                date = LocalDateTime.of(2024, 2, 5, 8, 0),
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        )

        // Insert all expenses
        expenses.forEach { expense ->
            val result = repository.insertExpense(expense)
            assertTrue("All expenses should be inserted successfully", result.isSuccess)
        }

        // Act & Assert - Filter by category
        repository.getExpensesByCategory(foodCategoryId).test {
            val foodExpenses = awaitItem()
            assertEquals("Should have 2 food expenses", 2, foodExpenses.size)
            assertTrue("Should contain lunch", foodExpenses.any { it.description == "Lunch" })
            assertTrue("Should contain coffee", foodExpenses.any { it.description == "Coffee" })
        }

        // Act & Assert - Filter by date range
        val startDate = LocalDate.of(2024, 1, 1)
        val endDate = LocalDate.of(2024, 1, 31)
        
        repository.getExpensesByDateRange(startDate, endDate).test {
            val januaryExpenses = awaitItem()
            assertEquals("Should have 2 January expenses", 2, januaryExpenses.size)
            assertTrue("Should contain lunch", januaryExpenses.any { it.description == "Lunch" })
            assertTrue("Should contain gas", januaryExpenses.any { it.description == "Gas" })
        }

        // Act & Assert - Filter by tags
        repository.getExpensesByTags(listOf("restaurant")).test {
            val restaurantExpenses = awaitItem()
            assertEquals("Should have 1 restaurant expense", 1, restaurantExpenses.size)
            assertEquals("Should be lunch expense", "Lunch", restaurantExpenses[0].description)
        }

        // Act & Assert - Complex filtering
        repository.getFilteredExpenses(
            categoryId = foodCategoryId,
            startDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 31),
            searchText = null,
            tags = null
        ).test {
            val filteredExpenses = awaitItem()
            assertEquals("Should have 1 filtered expense", 1, filteredExpenses.size)
            assertEquals("Should be lunch expense", "Lunch", filteredExpenses[0].description)
        }
    }

    @Test
    fun `should handle category operations with database constraints`() = runTest {
        // Act 1 - Initialize default categories
        val initResult = repository.initializeDefaultCategories()
        assertTrue("Default categories initialization should succeed", initResult.isSuccess)

        // Assert 1 - Verify default categories were created
        repository.getDefaultCategories().test {
            val defaultCategories = awaitItem()
            assertEquals("Should have 10 default categories", 10, defaultCategories.size)
            
            val categoryNames = defaultCategories.map { it.name }
            assertTrue("Should include Food & Dining", categoryNames.contains("Food & Dining"))
            assertTrue("Should include Transportation", categoryNames.contains("Transportation"))
            assertTrue("Should include Travel", categoryNames.contains("Travel"))
        }

        // Act 2 - Try to initialize again (should not duplicate)
        val secondInitResult = repository.initializeDefaultCategories()
        assertTrue("Second initialization should succeed", secondInitResult.isSuccess)

        // Assert 2 - Should still have same number of categories
        repository.getDefaultCategories().test {
            val defaultCategories = awaitItem()
            assertEquals("Should still have 10 default categories", 10, defaultCategories.size)
        }

        // Act 3 - Add custom category
        val customCategory = Category(
            name = "Custom Category",
            color = "#FF5722",
            icon = "custom",
            isDefault = false
        )
        val customResult = repository.insertCategory(customCategory)
        assertTrue("Custom category insertion should succeed", customResult.isSuccess)

        // Assert 3 - Verify custom and default categories
        repository.getAllCategories().test {
            val allCategories = awaitItem()
            assertEquals("Should have 11 total categories", 11, allCategories.size)
        }

        repository.getCustomCategories().test {
            val customCategories = awaitItem()
            assertEquals("Should have 1 custom category", 1, customCategories.size)
            assertEquals("Should be custom category", "Custom Category", customCategories[0].name)
        }
    }

    @Test
    fun `should handle recurring expenses with database persistence`() = runTest {
        // Arrange - Create category
        val category = Category(
            name = "Subscription",
            color = "#9C27B0",
            icon = "subscription",
            isDefault = false
        )
        val categoryId = repository.insertCategory(category).getOrNull()!!

        // Act 1 - Insert recurring expense
        val recurringExpense = RecurringExpense(
            amount = BigDecimal("9.99"),
            currency = "USD",
            description = "Netflix Subscription",
            categoryId = categoryId,
            tags = listOf("subscription", "entertainment"),
            frequency = RecurrenceFrequency.MONTHLY,
            startDate = LocalDate.of(2024, 1, 1),
            endDate = null,
            lastGenerated = null,
            isActive = true,
            createdAt = LocalDateTime.now()
        )

        val insertResult = repository.insertRecurringExpense(recurringExpense)
        assertTrue("Recurring expense insertion should succeed", insertResult.isSuccess)
        val recurringExpenseId = insertResult.getOrNull()!!

        // Assert 1 - Verify recurring expense was inserted
        repository.getAllRecurringExpenses().test {
            val recurringExpenses = awaitItem()
            assertEquals("Should have 1 recurring expense", 1, recurringExpenses.size)
            assertEquals("Should have correct description", "Netflix Subscription", recurringExpenses[0].description)
        }

        // Act 2 - Update active status
        val updateStatusResult = repository.updateRecurringExpenseActiveStatus(recurringExpenseId, false)
        assertTrue("Status update should succeed", updateStatusResult.isSuccess)

        // Assert 2 - Verify active status was updated
        repository.getActiveRecurringExpenses().test {
            val activeRecurringExpenses = awaitItem()
            assertEquals("Should have 0 active recurring expenses", 0, activeRecurringExpenses.size)
        }

        // Act 3 - Update last generated date
        val lastGenerated = LocalDate.of(2024, 1, 15)
        val updateLastGeneratedResult = repository.updateRecurringExpenseLastGenerated(recurringExpenseId, lastGenerated)
        assertTrue("Last generated update should succeed", updateLastGeneratedResult.isSuccess)

        // Assert 3 - Verify last generated was updated
        repository.getAllRecurringExpenses().test {
            val recurringExpenses = awaitItem()
            assertEquals("Should have updated last generated", lastGenerated, recurringExpenses[0].lastGenerated)
        }
    }

    @Test
    fun `should calculate analytics with real database data`() = runTest {
        // Arrange - Create category and expenses
        val category = Category(
            name = "Food",
            color = "#FF5722",
            icon = "restaurant",
            isDefault = false
        )
        val categoryId = repository.insertCategory(category).getOrNull()!!

        val expenses = listOf(
            Expense(
                amount = BigDecimal("25.50"),
                currency = "USD",
                description = "Lunch",
                categoryId = categoryId,
                tags = listOf("lunch"),
                date = LocalDateTime.of(2024, 1, 15, 12, 0),
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            ),
            Expense(
                amount = BigDecimal("30.00"),
                currency = "USD",
                description = "Dinner",
                categoryId = categoryId,
                tags = listOf("dinner"),
                date = LocalDateTime.of(2024, 1, 20, 19, 0),
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            ),
            Expense(
                amount = BigDecimal("15.00"),
                currency = "USD",
                description = "Coffee",
                categoryId = categoryId,
                tags = listOf("coffee"),
                date = LocalDateTime.of(2024, 2, 5, 8, 0),
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        )

        // Insert expenses
        expenses.forEach { expense ->
            repository.insertExpense(expense)
        }

        // Act & Assert - Total by category
        val totalByCategory = repository.getTotalAmountByCategory(categoryId)
        assertEquals("Should calculate correct total by category", BigDecimal("70.50"), totalByCategory)

        // Act & Assert - Total by date range
        val startDate = LocalDate.of(2024, 1, 1)
        val endDate = LocalDate.of(2024, 1, 31)
        val totalByDateRange = repository.getTotalAmountByDateRange(startDate, endDate)
        assertEquals("Should calculate correct total by date range", BigDecimal("55.50"), totalByDateRange)

        // Act & Assert - Expense count
        val expenseCount = repository.getExpenseCount()
        assertEquals("Should have correct expense count", 3, expenseCount)

        // Act & Assert - Category count
        val categoryCount = repository.getCategoryCount()
        assertEquals("Should have correct category count", 1, categoryCount)
    }

    @Test
    fun `should handle tag operations with real database data`() = runTest {
        // Arrange - Create category and expenses with various tags
        val category = Category(
            name = "Food",
            color = "#FF5722",
            icon = "restaurant",
            isDefault = false
        )
        val categoryId = repository.insertCategory(category).getOrNull()!!

        val expenses = listOf(
            Expense(
                amount = BigDecimal("25.50"),
                currency = "USD",
                description = "Lunch",
                categoryId = categoryId,
                tags = listOf("food", "restaurant", "lunch"),
                date = LocalDateTime.now(),
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            ),
            Expense(
                amount = BigDecimal("30.00"),
                currency = "USD",
                description = "Fuel",
                categoryId = categoryId,
                tags = listOf("fuel", "car", "transport"),
                date = LocalDateTime.now(),
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            ),
            Expense(
                amount = BigDecimal("15.00"),
                currency = "USD",
                description = "Coffee",
                categoryId = categoryId,
                tags = listOf("food", "coffee", "morning"),
                date = LocalDateTime.now(),
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        )

        // Insert expenses
        expenses.forEach { expense ->
            repository.insertExpense(expense)
        }

        // Act & Assert - Get all tags
        val allTags = repository.getAllTags()
        assertTrue("Should have tags", allTags.isNotEmpty())
        assertTrue("Should contain 'food'", allTags.contains("food"))
        assertTrue("Should contain 'restaurant'", allTags.contains("restaurant"))
        assertTrue("Should contain 'fuel'", allTags.contains("fuel"))

        // Act & Assert - Get tag suggestions
        val foodSuggestions = repository.getTagSuggestions("f")
        assertTrue("Should have suggestions for 'f'", foodSuggestions.isNotEmpty())
        assertTrue("Should suggest tags starting with 'f'", 
            foodSuggestions.any { it.startsWith("f", ignoreCase = true) })

        // Act & Assert - Get popular tags
        val popularTags = repository.getPopularTags(5)
        assertTrue("Should have popular tags", popularTags.isNotEmpty())
        assertTrue("Should limit to 5 tags", popularTags.size <= 5)
    }
}