package com.expensetracker.data.dao

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.expensetracker.data.database.AppDatabase
import com.expensetracker.data.model.Category
import com.expensetracker.data.model.Expense
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import java.math.BigDecimal
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class ExpenseDaoTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: AppDatabase
    private lateinit var expenseDao: ExpenseDao
    private lateinit var categoryDao: CategoryDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        
        expenseDao = database.expenseDao()
        categoryDao = database.categoryDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndGetExpense() = runTest {
        // Create a category first
        val category = Category(
            name = "Food",
            color = "#FF5722",
            icon = "restaurant"
        )
        val categoryId = categoryDao.insertCategory(category)

        // Create and insert expense
        val expense = Expense(
            amount = BigDecimal("25.50"),
            currency = "USD",
            description = "Lunch",
            categoryId = categoryId,
            tags = listOf("food", "lunch"),
            date = LocalDateTime.now()
        )
        
        val expenseId = expenseDao.insertExpense(expense)
        assertTrue(expenseId > 0)

        // Retrieve and verify
        val retrievedExpense = expenseDao.getExpenseById(expenseId)
        assertNotNull(retrievedExpense)
        assertEquals(expense.amount, retrievedExpense!!.amount)
        assertEquals(expense.currency, retrievedExpense.currency)
        assertEquals(expense.description, retrievedExpense.description)
        assertEquals(expense.categoryId, retrievedExpense.categoryId)
        assertEquals(expense.tags, retrievedExpense.tags)
    }

    @Test
    fun getAllExpenses() = runTest {
        // Create a category first
        val category = Category(
            name = "Food",
            color = "#FF5722",
            icon = "restaurant"
        )
        val categoryId = categoryDao.insertCategory(category)

        // Insert multiple expenses
        val expenses = listOf(
            Expense(
                amount = BigDecimal("10.00"),
                currency = "USD",
                description = "Coffee",
                categoryId = categoryId,
                tags = listOf("coffee"),
                date = LocalDateTime.now().minusHours(2)
            ),
            Expense(
                amount = BigDecimal("25.50"),
                currency = "USD",
                description = "Lunch",
                categoryId = categoryId,
                tags = listOf("food", "lunch"),
                date = LocalDateTime.now().minusHours(1)
            )
        )
        
        expenseDao.insertExpenses(expenses)

        // Retrieve all expenses
        val allExpenses = expenseDao.getAllExpenses().first()
        assertEquals(2, allExpenses.size)
        
        // Should be ordered by date DESC (most recent first)
        assertEquals("Lunch", allExpenses[0].description)
        assertEquals("Coffee", allExpenses[1].description)
    }

    @Test
    fun updateExpense() = runTest {
        // Create a category first
        val category = Category(
            name = "Food",
            color = "#FF5722",
            icon = "restaurant"
        )
        val categoryId = categoryDao.insertCategory(category)

        // Insert expense
        val expense = Expense(
            amount = BigDecimal("20.00"),
            currency = "USD",
            description = "Original description",
            categoryId = categoryId,
            tags = listOf("original"),
            date = LocalDateTime.now()
        )
        
        val expenseId = expenseDao.insertExpense(expense)
        
        // Update expense
        val updatedExpense = expense.copy(
            id = expenseId,
            amount = BigDecimal("30.00"),
            description = "Updated description",
            tags = listOf("updated")
        )
        
        expenseDao.updateExpense(updatedExpense)

        // Verify update
        val retrievedExpense = expenseDao.getExpenseById(expenseId)
        assertNotNull(retrievedExpense)
        assertEquals(BigDecimal("30.00"), retrievedExpense!!.amount)
        assertEquals("Updated description", retrievedExpense.description)
        assertEquals(listOf("updated"), retrievedExpense.tags)
    }

    @Test
    fun deleteExpense() = runTest {
        // Create a category first
        val category = Category(
            name = "Food",
            color = "#FF5722",
            icon = "restaurant"
        )
        val categoryId = categoryDao.insertCategory(category)

        // Insert expense
        val expense = Expense(
            amount = BigDecimal("15.00"),
            currency = "USD",
            description = "To be deleted",
            categoryId = categoryId,
            tags = emptyList(),
            date = LocalDateTime.now()
        )
        
        val expenseId = expenseDao.insertExpense(expense)
        
        // Verify expense exists
        assertNotNull(expenseDao.getExpenseById(expenseId))
        
        // Delete expense
        expenseDao.deleteExpenseById(expenseId)
        
        // Verify expense is deleted
        assertNull(expenseDao.getExpenseById(expenseId))
    }

    @Test
    fun getExpensesByCategory() = runTest {
        // Create categories
        val foodCategory = Category(name = "Food", color = "#FF5722", icon = "restaurant")
        val transportCategory = Category(name = "Transport", color = "#2196F3", icon = "directions_car")
        
        val foodCategoryId = categoryDao.insertCategory(foodCategory)
        val transportCategoryId = categoryDao.insertCategory(transportCategory)

        // Insert expenses with different categories
        val expenses = listOf(
            Expense(
                amount = BigDecimal("10.00"),
                currency = "USD",
                description = "Coffee",
                categoryId = foodCategoryId,
                tags = listOf("coffee"),
                date = LocalDateTime.now()
            ),
            Expense(
                amount = BigDecimal("25.00"),
                currency = "USD",
                description = "Bus ticket",
                categoryId = transportCategoryId,
                tags = listOf("bus"),
                date = LocalDateTime.now()
            ),
            Expense(
                amount = BigDecimal("15.00"),
                currency = "USD",
                description = "Lunch",
                categoryId = foodCategoryId,
                tags = listOf("lunch"),
                date = LocalDateTime.now()
            )
        )
        
        expenseDao.insertExpenses(expenses)

        // Get expenses by food category
        val foodExpenses = expenseDao.getExpensesByCategory(foodCategoryId).first()
        assertEquals(2, foodExpenses.size)
        assertTrue(foodExpenses.all { it.categoryId == foodCategoryId })

        // Get expenses by transport category
        val transportExpenses = expenseDao.getExpensesByCategory(transportCategoryId).first()
        assertEquals(1, transportExpenses.size)
        assertEquals(transportCategoryId, transportExpenses[0].categoryId)
    }

    @Test
    fun getExpensesByDateRange() = runTest {
        // Create a category first
        val category = Category(
            name = "Food",
            color = "#FF5722",
            icon = "restaurant"
        )
        val categoryId = categoryDao.insertCategory(category)

        val now = LocalDateTime.now()
        val expenses = listOf(
            Expense(
                amount = BigDecimal("10.00"),
                currency = "USD",
                description = "Old expense",
                categoryId = categoryId,
                tags = emptyList(),
                date = now.minusDays(10)
            ),
            Expense(
                amount = BigDecimal("20.00"),
                currency = "USD",
                description = "Recent expense 1",
                categoryId = categoryId,
                tags = emptyList(),
                date = now.minusDays(2)
            ),
            Expense(
                amount = BigDecimal("30.00"),
                currency = "USD",
                description = "Recent expense 2",
                categoryId = categoryId,
                tags = emptyList(),
                date = now.minusDays(1)
            )
        )
        
        expenseDao.insertExpenses(expenses)

        // Get expenses from last 3 days
        val startDate = now.minusDays(3)
        val endDate = now
        val recentExpenses = expenseDao.getExpensesByDateRange(startDate, endDate).first()
        
        assertEquals(2, recentExpenses.size)
        assertTrue(recentExpenses.all { it.date.isAfter(startDate) && it.date.isBefore(endDate) })
    }

    @Test
    fun searchExpensesByDescription() = runTest {
        // Create a category first
        val category = Category(
            name = "Food",
            color = "#FF5722",
            icon = "restaurant"
        )
        val categoryId = categoryDao.insertCategory(category)

        val expenses = listOf(
            Expense(
                amount = BigDecimal("10.00"),
                currency = "USD",
                description = "Morning coffee",
                categoryId = categoryId,
                tags = emptyList(),
                date = LocalDateTime.now()
            ),
            Expense(
                amount = BigDecimal("25.00"),
                currency = "USD",
                description = "Lunch at restaurant",
                categoryId = categoryId,
                tags = emptyList(),
                date = LocalDateTime.now()
            ),
            Expense(
                amount = BigDecimal("5.00"),
                currency = "USD",
                description = "Evening coffee",
                categoryId = categoryId,
                tags = emptyList(),
                date = LocalDateTime.now()
            )
        )
        
        expenseDao.insertExpenses(expenses)

        // Search for expenses containing "coffee"
        val coffeeExpenses = expenseDao.searchExpensesByDescription("coffee").first()
        assertEquals(2, coffeeExpenses.size)
        assertTrue(coffeeExpenses.all { it.description.contains("coffee", ignoreCase = true) })

        // Search for expenses containing "lunch"
        val lunchExpenses = expenseDao.searchExpensesByDescription("lunch").first()
        assertEquals(1, lunchExpenses.size)
        assertEquals("Lunch at restaurant", lunchExpenses[0].description)
    }

    @Test
    fun getTotalAmountByCategory() = runTest {
        // Create a category first
        val category = Category(
            name = "Food",
            color = "#FF5722",
            icon = "restaurant"
        )
        val categoryId = categoryDao.insertCategory(category)

        val expenses = listOf(
            Expense(
                amount = BigDecimal("10.50"),
                currency = "USD",
                description = "Coffee",
                categoryId = categoryId,
                tags = emptyList(),
                date = LocalDateTime.now()
            ),
            Expense(
                amount = BigDecimal("25.75"),
                currency = "USD",
                description = "Lunch",
                categoryId = categoryId,
                tags = emptyList(),
                date = LocalDateTime.now()
            )
        )
        
        expenseDao.insertExpenses(expenses)

        val totalAmount = expenseDao.getTotalAmountByCategory(categoryId)
        assertNotNull(totalAmount)
        assertEquals(36.25, totalAmount!!, 0.01)
    }

    @Test
    fun getExpenseCount() = runTest {
        // Create a category first
        val category = Category(
            name = "Food",
            color = "#FF5722",
            icon = "restaurant"
        )
        val categoryId = categoryDao.insertCategory(category)

        // Initially should be 0
        assertEquals(0, expenseDao.getExpenseCount())

        // Insert expenses
        val expenses = listOf(
            Expense(
                amount = BigDecimal("10.00"),
                currency = "USD",
                description = "Expense 1",
                categoryId = categoryId,
                tags = emptyList(),
                date = LocalDateTime.now()
            ),
            Expense(
                amount = BigDecimal("20.00"),
                currency = "USD",
                description = "Expense 2",
                categoryId = categoryId,
                tags = emptyList(),
                date = LocalDateTime.now()
            )
        )
        
        expenseDao.insertExpenses(expenses)

        // Should now be 2
        assertEquals(2, expenseDao.getExpenseCount())
    }
}