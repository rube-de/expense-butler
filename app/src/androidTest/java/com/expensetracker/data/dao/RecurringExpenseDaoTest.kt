package com.expensetracker.data.dao

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.expensetracker.data.database.AppDatabase
import com.expensetracker.data.model.Category
import com.expensetracker.data.model.RecurringExpense
import com.expensetracker.data.model.RecurrenceFrequency
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import java.math.BigDecimal
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class RecurringExpenseDaoTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: AppDatabase
    private lateinit var recurringExpenseDao: RecurringExpenseDao
    private lateinit var categoryDao: CategoryDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        
        recurringExpenseDao = database.recurringExpenseDao()
        categoryDao = database.categoryDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndGetRecurringExpense() = runTest {
        // Create a category first
        val category = Category(
            name = "Housing",
            color = "#FF5722",
            icon = "home"
        )
        val categoryId = categoryDao.insertCategory(category)

        val recurringExpense = RecurringExpense(
            amount = BigDecimal("1200.00"),
            currency = "USD",
            description = "Monthly rent",
            categoryId = categoryId,
            tags = listOf("rent", "housing"),
            frequency = RecurrenceFrequency.MONTHLY,
            startDate = LocalDate.now(),
            endDate = LocalDate.now().plusYears(1),
            lastGenerated = null
        )
        
        val recurringExpenseId = recurringExpenseDao.insertRecurringExpense(recurringExpense)
        assertTrue(recurringExpenseId > 0)

        val retrieved = recurringExpenseDao.getRecurringExpenseById(recurringExpenseId)
        assertNotNull(retrieved)
        assertEquals(recurringExpense.amount, retrieved!!.amount)
        assertEquals(recurringExpense.currency, retrieved.currency)
        assertEquals(recurringExpense.description, retrieved.description)
        assertEquals(recurringExpense.categoryId, retrieved.categoryId)
        assertEquals(recurringExpense.tags, retrieved.tags)
        assertEquals(recurringExpense.frequency, retrieved.frequency)
        assertEquals(recurringExpense.startDate, retrieved.startDate)
        assertEquals(recurringExpense.endDate, retrieved.endDate)
        assertTrue(retrieved.isActive)
    }

    @Test
    fun getAllRecurringExpenses() = runTest {
        // Create a category first
        val category = Category(
            name = "Subscriptions",
            color = "#2196F3",
            icon = "subscriptions"
        )
        val categoryId = categoryDao.insertCategory(category)

        val recurringExpenses = listOf(
            RecurringExpense(
                amount = BigDecimal("9.99"),
                currency = "USD",
                description = "Netflix subscription",
                categoryId = categoryId,
                tags = listOf("entertainment", "streaming"),
                frequency = RecurrenceFrequency.MONTHLY,
                startDate = LocalDate.now().minusMonths(1),
                endDate = null,
                lastGenerated = null
            ),
            RecurringExpense(
                amount = BigDecimal("5.00"),
                currency = "USD",
                description = "Coffee subscription",
                categoryId = categoryId,
                tags = listOf("coffee", "subscription"),
                frequency = RecurrenceFrequency.WEEKLY,
                startDate = LocalDate.now(),
                endDate = null,
                lastGenerated = null
            )
        )
        
        recurringExpenseDao.insertRecurringExpenses(recurringExpenses)

        val allRecurringExpenses = recurringExpenseDao.getAllRecurringExpenses().first()
        assertEquals(2, allRecurringExpenses.size)
        
        // Should be ordered by createdAt DESC (most recent first)
        assertEquals("Coffee subscription", allRecurringExpenses[0].description)
        assertEquals("Netflix subscription", allRecurringExpenses[1].description)
    }

    @Test
    fun updateRecurringExpense() = runTest {
        // Create a category first
        val category = Category(
            name = "Utilities",
            color = "#FF9800",
            icon = "flash_on"
        )
        val categoryId = categoryDao.insertCategory(category)

        val recurringExpense = RecurringExpense(
            amount = BigDecimal("100.00"),
            currency = "USD",
            description = "Original description",
            categoryId = categoryId,
            tags = listOf("original"),
            frequency = RecurrenceFrequency.MONTHLY,
            startDate = LocalDate.now(),
            endDate = null,
            lastGenerated = null
        )
        
        val recurringExpenseId = recurringExpenseDao.insertRecurringExpense(recurringExpense)
        
        val updatedRecurringExpense = recurringExpense.copy(
            id = recurringExpenseId,
            amount = BigDecimal("120.00"),
            description = "Updated description",
            tags = listOf("updated"),
            frequency = RecurrenceFrequency.WEEKLY
        )
        
        recurringExpenseDao.updateRecurringExpense(updatedRecurringExpense)

        val retrieved = recurringExpenseDao.getRecurringExpenseById(recurringExpenseId)
        assertNotNull(retrieved)
        assertEquals(BigDecimal("120.00"), retrieved!!.amount)
        assertEquals("Updated description", retrieved.description)
        assertEquals(listOf("updated"), retrieved.tags)
        assertEquals(RecurrenceFrequency.WEEKLY, retrieved.frequency)
    }

    @Test
    fun deleteRecurringExpense() = runTest {
        // Create a category first
        val category = Category(
            name = "Test",
            color = "#607D8B",
            icon = "test"
        )
        val categoryId = categoryDao.insertCategory(category)

        val recurringExpense = RecurringExpense(
            amount = BigDecimal("50.00"),
            currency = "USD",
            description = "To be deleted",
            categoryId = categoryId,
            tags = emptyList(),
            frequency = RecurrenceFrequency.MONTHLY,
            startDate = LocalDate.now(),
            endDate = null,
            lastGenerated = null
        )
        
        val recurringExpenseId = recurringExpenseDao.insertRecurringExpense(recurringExpense)
        
        // Verify exists
        assertNotNull(recurringExpenseDao.getRecurringExpenseById(recurringExpenseId))
        
        // Delete
        recurringExpenseDao.deleteRecurringExpenseById(recurringExpenseId)
        
        // Verify deleted
        assertNull(recurringExpenseDao.getRecurringExpenseById(recurringExpenseId))
    }

    @Test
    fun getActiveAndInactiveRecurringExpenses() = runTest {
        // Create a category first
        val category = Category(
            name = "Mixed",
            color = "#4CAF50",
            icon = "category"
        )
        val categoryId = categoryDao.insertCategory(category)

        val recurringExpenses = listOf(
            RecurringExpense(
                amount = BigDecimal("10.00"),
                currency = "USD",
                description = "Active expense 1",
                categoryId = categoryId,
                tags = emptyList(),
                frequency = RecurrenceFrequency.MONTHLY,
                startDate = LocalDate.now(),
                endDate = null,
                lastGenerated = null,
                isActive = true
            ),
            RecurringExpense(
                amount = BigDecimal("20.00"),
                currency = "USD",
                description = "Inactive expense",
                categoryId = categoryId,
                tags = emptyList(),
                frequency = RecurrenceFrequency.MONTHLY,
                startDate = LocalDate.now(),
                endDate = null,
                lastGenerated = null,
                isActive = false
            ),
            RecurringExpense(
                amount = BigDecimal("30.00"),
                currency = "USD",
                description = "Active expense 2",
                categoryId = categoryId,
                tags = emptyList(),
                frequency = RecurrenceFrequency.WEEKLY,
                startDate = LocalDate.now(),
                endDate = null,
                lastGenerated = null,
                isActive = true
            )
        )
        
        recurringExpenseDao.insertRecurringExpenses(recurringExpenses)

        val activeExpenses = recurringExpenseDao.getActiveRecurringExpenses().first()
        assertEquals(2, activeExpenses.size)
        assertTrue(activeExpenses.all { it.isActive })

        val inactiveExpenses = recurringExpenseDao.getInactiveRecurringExpenses().first()
        assertEquals(1, inactiveExpenses.size)
        assertFalse(inactiveExpenses[0].isActive)
        assertEquals("Inactive expense", inactiveExpenses[0].description)
    }

    @Test
    fun getRecurringExpensesByFrequency() = runTest {
        // Create a category first
        val category = Category(
            name = "Frequency Test",
            color = "#9C27B0",
            icon = "schedule"
        )
        val categoryId = categoryDao.insertCategory(category)

        val recurringExpenses = listOf(
            RecurringExpense(
                amount = BigDecimal("5.00"),
                currency = "USD",
                description = "Daily expense",
                categoryId = categoryId,
                tags = emptyList(),
                frequency = RecurrenceFrequency.DAILY,
                startDate = LocalDate.now(),
                endDate = null,
                lastGenerated = null
            ),
            RecurringExpense(
                amount = BigDecimal("25.00"),
                currency = "USD",
                description = "Weekly expense",
                categoryId = categoryId,
                tags = emptyList(),
                frequency = RecurrenceFrequency.WEEKLY,
                startDate = LocalDate.now(),
                endDate = null,
                lastGenerated = null
            ),
            RecurringExpense(
                amount = BigDecimal("100.00"),
                currency = "USD",
                description = "Monthly expense 1",
                categoryId = categoryId,
                tags = emptyList(),
                frequency = RecurrenceFrequency.MONTHLY,
                startDate = LocalDate.now(),
                endDate = null,
                lastGenerated = null
            ),
            RecurringExpense(
                amount = BigDecimal("200.00"),
                currency = "USD",
                description = "Monthly expense 2",
                categoryId = categoryId,
                tags = emptyList(),
                frequency = RecurrenceFrequency.MONTHLY,
                startDate = LocalDate.now(),
                endDate = null,
                lastGenerated = null
            )
        )
        
        recurringExpenseDao.insertRecurringExpenses(recurringExpenses)

        val dailyExpenses = recurringExpenseDao.getRecurringExpensesByFrequency(RecurrenceFrequency.DAILY).first()
        assertEquals(1, dailyExpenses.size)
        assertEquals("Daily expense", dailyExpenses[0].description)

        val weeklyExpenses = recurringExpenseDao.getRecurringExpensesByFrequency(RecurrenceFrequency.WEEKLY).first()
        assertEquals(1, weeklyExpenses.size)
        assertEquals("Weekly expense", weeklyExpenses[0].description)

        val monthlyExpenses = recurringExpenseDao.getRecurringExpensesByFrequency(RecurrenceFrequency.MONTHLY).first()
        assertEquals(2, monthlyExpenses.size)
        assertTrue(monthlyExpenses.all { it.frequency == RecurrenceFrequency.MONTHLY })
    }

    @Test
    fun getDueRecurringExpenses() = runTest {
        // Create a category first
        val category = Category(
            name = "Due Test",
            color = "#FF5722",
            icon = "alarm"
        )
        val categoryId = categoryDao.insertCategory(category)

        val today = LocalDate.now()
        val recurringExpenses = listOf(
            RecurringExpense(
                amount = BigDecimal("10.00"),
                currency = "USD",
                description = "Future expense",
                categoryId = categoryId,
                tags = emptyList(),
                frequency = RecurrenceFrequency.MONTHLY,
                startDate = today.plusDays(5), // Starts in future
                endDate = null,
                lastGenerated = null,
                isActive = true
            ),
            RecurringExpense(
                amount = BigDecimal("20.00"),
                currency = "USD",
                description = "Current due expense",
                categoryId = categoryId,
                tags = emptyList(),
                frequency = RecurrenceFrequency.MONTHLY,
                startDate = today.minusDays(1), // Started yesterday
                endDate = null,
                lastGenerated = null,
                isActive = true
            ),
            RecurringExpense(
                amount = BigDecimal("30.00"),
                currency = "USD",
                description = "Expired expense",
                categoryId = categoryId,
                tags = emptyList(),
                frequency = RecurrenceFrequency.MONTHLY,
                startDate = today.minusMonths(2),
                endDate = today.minusDays(1), // Ended yesterday
                lastGenerated = null,
                isActive = true
            ),
            RecurringExpense(
                amount = BigDecimal("40.00"),
                currency = "USD",
                description = "Inactive expense",
                categoryId = categoryId,
                tags = emptyList(),
                frequency = RecurrenceFrequency.MONTHLY,
                startDate = today.minusDays(1),
                endDate = null,
                lastGenerated = null,
                isActive = false // Inactive
            )
        )
        
        recurringExpenseDao.insertRecurringExpenses(recurringExpenses)

        val dueExpenses = recurringExpenseDao.getDueRecurringExpenses(today).first()
        assertEquals(1, dueExpenses.size)
        assertEquals("Current due expense", dueExpenses[0].description)
    }

    @Test
    fun updateLastGenerated() = runTest {
        // Create a category first
        val category = Category(
            name = "Update Test",
            color = "#2196F3",
            icon = "update"
        )
        val categoryId = categoryDao.insertCategory(category)

        val recurringExpense = RecurringExpense(
            amount = BigDecimal("50.00"),
            currency = "USD",
            description = "Test expense",
            categoryId = categoryId,
            tags = emptyList(),
            frequency = RecurrenceFrequency.MONTHLY,
            startDate = LocalDate.now(),
            endDate = null,
            lastGenerated = null
        )
        
        val recurringExpenseId = recurringExpenseDao.insertRecurringExpense(recurringExpense)
        
        // Verify initially null
        val initial = recurringExpenseDao.getRecurringExpenseById(recurringExpenseId)
        assertNull(initial!!.lastGenerated)
        
        // Update last generated
        val lastGenerated = LocalDate.now()
        recurringExpenseDao.updateLastGenerated(recurringExpenseId, lastGenerated)
        
        // Verify updated
        val updated = recurringExpenseDao.getRecurringExpenseById(recurringExpenseId)
        assertEquals(lastGenerated, updated!!.lastGenerated)
    }

    @Test
    fun updateActiveStatus() = runTest {
        // Create a category first
        val category = Category(
            name = "Status Test",
            color = "#4CAF50",
            icon = "toggle"
        )
        val categoryId = categoryDao.insertCategory(category)

        val recurringExpense = RecurringExpense(
            amount = BigDecimal("25.00"),
            currency = "USD",
            description = "Status test expense",
            categoryId = categoryId,
            tags = emptyList(),
            frequency = RecurrenceFrequency.MONTHLY,
            startDate = LocalDate.now(),
            endDate = null,
            lastGenerated = null,
            isActive = true
        )
        
        val recurringExpenseId = recurringExpenseDao.insertRecurringExpense(recurringExpense)
        
        // Verify initially active
        val initial = recurringExpenseDao.getRecurringExpenseById(recurringExpenseId)
        assertTrue(initial!!.isActive)
        
        // Update to inactive
        recurringExpenseDao.updateActiveStatus(recurringExpenseId, false)
        
        // Verify updated
        val updated = recurringExpenseDao.getRecurringExpenseById(recurringExpenseId)
        assertFalse(updated!!.isActive)
    }

    @Test
    fun getRecurringExpenseCounts() = runTest {
        // Create a category first
        val category = Category(
            name = "Count Test",
            color = "#FF9800",
            icon = "count"
        )
        val categoryId = categoryDao.insertCategory(category)

        // Initially should be 0
        assertEquals(0, recurringExpenseDao.getRecurringExpenseCount())
        assertEquals(0, recurringExpenseDao.getActiveRecurringExpenseCount())
        assertEquals(0, recurringExpenseDao.getInactiveRecurringExpenseCount())

        val recurringExpenses = listOf(
            RecurringExpense(
                amount = BigDecimal("10.00"),
                currency = "USD",
                description = "Active 1",
                categoryId = categoryId,
                tags = emptyList(),
                frequency = RecurrenceFrequency.MONTHLY,
                startDate = LocalDate.now(),
                endDate = null,
                lastGenerated = null,
                isActive = true
            ),
            RecurringExpense(
                amount = BigDecimal("20.00"),
                currency = "USD",
                description = "Active 2",
                categoryId = categoryId,
                tags = emptyList(),
                frequency = RecurrenceFrequency.MONTHLY,
                startDate = LocalDate.now(),
                endDate = null,
                lastGenerated = null,
                isActive = true
            ),
            RecurringExpense(
                amount = BigDecimal("30.00"),
                currency = "USD",
                description = "Inactive",
                categoryId = categoryId,
                tags = emptyList(),
                frequency = RecurrenceFrequency.MONTHLY,
                startDate = LocalDate.now(),
                endDate = null,
                lastGenerated = null,
                isActive = false
            )
        )
        
        recurringExpenseDao.insertRecurringExpenses(recurringExpenses)

        assertEquals(3, recurringExpenseDao.getRecurringExpenseCount())
        assertEquals(2, recurringExpenseDao.getActiveRecurringExpenseCount())
        assertEquals(1, recurringExpenseDao.getInactiveRecurringExpenseCount())
    }

    @Test
    fun getTotalActiveRecurringAmount() = runTest {
        // Create a category first
        val category = Category(
            name = "Total Test",
            color = "#9C27B0",
            icon = "calculate"
        )
        val categoryId = categoryDao.insertCategory(category)

        val recurringExpenses = listOf(
            RecurringExpense(
                amount = BigDecimal("100.50"),
                currency = "USD",
                description = "Active 1",
                categoryId = categoryId,
                tags = emptyList(),
                frequency = RecurrenceFrequency.MONTHLY,
                startDate = LocalDate.now(),
                endDate = null,
                lastGenerated = null,
                isActive = true
            ),
            RecurringExpense(
                amount = BigDecimal("200.25"),
                currency = "USD",
                description = "Active 2",
                categoryId = categoryId,
                tags = emptyList(),
                frequency = RecurrenceFrequency.MONTHLY,
                startDate = LocalDate.now(),
                endDate = null,
                lastGenerated = null,
                isActive = true
            ),
            RecurringExpense(
                amount = BigDecimal("500.00"),
                currency = "USD",
                description = "Inactive (should not count)",
                categoryId = categoryId,
                tags = emptyList(),
                frequency = RecurrenceFrequency.MONTHLY,
                startDate = LocalDate.now(),
                endDate = null,
                lastGenerated = null,
                isActive = false
            )
        )
        
        recurringExpenseDao.insertRecurringExpenses(recurringExpenses)

        val totalAmount = recurringExpenseDao.getTotalActiveRecurringAmount()
        assertNotNull(totalAmount)
        assertEquals(300.75, totalAmount!!, 0.01) // Only active expenses: 100.50 + 200.25
    }
}