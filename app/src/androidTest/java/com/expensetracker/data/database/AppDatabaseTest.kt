package com.expensetracker.data.database

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.expensetracker.data.model.Category
import com.expensetracker.data.model.Expense
import com.expensetracker.data.model.RecurringExpense
import com.expensetracker.data.model.RecurrenceFrequency
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: AppDatabase

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun databaseCreationTest() {
        // Test that database is created successfully
        assertNotNull(database)
        assertNotNull(database.expenseDao())
        assertNotNull(database.categoryDao())
        assertNotNull(database.recurringExpenseDao())
    }

    @Test
    fun typeConvertersTest() = runTest {
        val categoryDao = database.categoryDao()
        val expenseDao = database.expenseDao()
        val recurringExpenseDao = database.recurringExpenseDao()

        // Test BigDecimal conversion
        val category = Category(
            name = "Test Category",
            color = "#FF5722",
            icon = "test"
        )
        val categoryId = categoryDao.insertCategory(category)

        // Test LocalDateTime conversion
        val now = LocalDateTime.now()
        val expense = Expense(
            amount = BigDecimal("123.45"),
            currency = "USD",
            description = "Test expense",
            categoryId = categoryId,
            tags = listOf("test", "converter"),
            date = now
        )
        val expenseId = expenseDao.insertExpense(expense)

        // Test LocalDate conversion and RecurrenceFrequency conversion
        val today = LocalDate.now()
        val recurringExpense = RecurringExpense(
            amount = BigDecimal("100.00"),
            currency = "EUR",
            description = "Test recurring",
            categoryId = categoryId,
            tags = listOf("recurring", "test"),
            frequency = RecurrenceFrequency.MONTHLY,
            startDate = today,
            endDate = today.plusYears(1),
            lastGenerated = null
        )
        val recurringExpenseId = recurringExpenseDao.insertRecurringExpense(recurringExpense)

        // Verify data was stored and retrieved correctly
        val retrievedExpense = expenseDao.getExpenseById(expenseId)
        assertNotNull(retrievedExpense)
        assertEquals(BigDecimal("123.45"), retrievedExpense!!.amount)
        assertEquals("USD", retrievedExpense.currency)
        assertEquals(listOf("test", "converter"), retrievedExpense.tags)
        assertEquals(now.withNano(0), retrievedExpense.date.withNano(0)) // Room truncates nanoseconds

        val retrievedRecurring = recurringExpenseDao.getRecurringExpenseById(recurringExpenseId)
        assertNotNull(retrievedRecurring)
        assertEquals(BigDecimal("100.00"), retrievedRecurring!!.amount)
        assertEquals("EUR", retrievedRecurring.currency)
        assertEquals(listOf("recurring", "test"), retrievedRecurring.tags)
        assertEquals(RecurrenceFrequency.MONTHLY, retrievedRecurring.frequency)
        assertEquals(today, retrievedRecurring.startDate)
        assertEquals(today.plusYears(1), retrievedRecurring.endDate)
    }

    @Test
    fun databaseInitializerTest() = runTest {
        val categoryDao = database.categoryDao()
        val initializer = DatabaseInitializer(categoryDao)

        // Initially should have no categories
        assertEquals(0, categoryDao.getCategoryCount())

        // Initialize database
        initializer.initializeDatabase()

        // Should now have default categories
        val categoryCount = categoryDao.getCategoryCount()
        assertTrue("Should have default categories", categoryCount > 0)
        
        val defaultCategoryCount = categoryDao.getDefaultCategoryCount()
        assertEquals(categoryCount, defaultCategoryCount)

        // Running initializer again should not add duplicate categories
        initializer.initializeDatabase()
        assertEquals(categoryCount, categoryDao.getCategoryCount())
    }

    @Test
    fun complexQueryTest() = runTest {
        val categoryDao = database.categoryDao()
        val expenseDao = database.expenseDao()

        // Create test categories
        val foodCategory = Category(name = "Food", color = "#FF5722", icon = "restaurant")
        val transportCategory = Category(name = "Transport", color = "#2196F3", icon = "directions_car")
        
        val foodCategoryId = categoryDao.insertCategory(foodCategory)
        val transportCategoryId = categoryDao.insertCategory(transportCategory)

        // Create test expenses with different dates
        val now = LocalDateTime.now()
        val expenses = listOf(
            Expense(
                amount = BigDecimal("25.50"),
                currency = "USD",
                description = "Lunch at restaurant",
                categoryId = foodCategoryId,
                tags = listOf("food", "lunch", "restaurant"),
                date = now.minusDays(1)
            ),
            Expense(
                amount = BigDecimal("15.00"),
                currency = "USD",
                description = "Bus ticket",
                categoryId = transportCategoryId,
                tags = listOf("transport", "bus"),
                date = now.minusDays(2)
            ),
            Expense(
                amount = BigDecimal("8.50"),
                currency = "USD",
                description = "Coffee",
                categoryId = foodCategoryId,
                tags = listOf("food", "coffee"),
                date = now
            )
        )
        
        expenseDao.insertExpenses(expenses)

        // Test filtered query
        val startDate = now.minusDays(3)
        val endDate = now.plusDays(1)
        
        val filteredExpenses = expenseDao.getFilteredExpenses(
            categoryId = foodCategoryId,
            startDate = startDate,
            endDate = endDate,
            searchText = null
        )
        
        // Should return 2 food expenses within date range
        // Note: We can't easily test Flow in this context, so we'll test the DAO methods that return suspend functions
        val totalFoodAmount = expenseDao.getTotalAmountByCategory(foodCategoryId)
        assertNotNull(totalFoodAmount)
        assertEquals(34.0, totalFoodAmount!!, 0.01) // 25.50 + 8.50

        val totalInDateRange = expenseDao.getTotalAmountByDateRange(startDate, endDate)
        assertNotNull(totalInDateRange)
        assertEquals(49.0, totalInDateRange!!, 0.01) // All expenses: 25.50 + 15.00 + 8.50
    }
}