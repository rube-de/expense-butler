package com.expensetracker.ui.screens

import app.cash.turbine.test
import com.expensetracker.data.model.*
import com.expensetracker.data.repository.ExpenseRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Test suite for analytics filtering functionality.
 * Tests filtering by category, tags, and date range with drill-down capabilities.
 */
@ExperimentalCoroutinesApi
class AnalyticsFilteringTest {

    private lateinit var repository: ExpenseRepository
    private lateinit var viewModel: AnalyticsViewModel
    private val testDispatcher = StandardTestDispatcher()

    private val testCategories = listOf(
        Category(id = 1, name = "Food", color = "#FF0000", icon = "restaurant"),
        Category(id = 2, name = "Transport", color = "#00FF00", icon = "directions_car"),
        Category(id = 3, name = "Entertainment", color = "#0000FF", icon = "movie")
    )

    private val testExpenses = listOf(
        Expense(
            id = 1,
            amount = BigDecimal("50.00"),
            currency = "USD",
            description = "Lunch",
            categoryId = 1,
            tags = listOf("restaurant", "business"),
            date = LocalDateTime.now().minusDays(5)
        ),
        Expense(
            id = 2,
            amount = BigDecimal("30.00"),
            currency = "USD",
            description = "Taxi",
            categoryId = 2,
            tags = listOf("uber", "business"),
            date = LocalDateTime.now().minusDays(10)
        ),
        Expense(
            id = 3,
            amount = BigDecimal("100.00"),
            currency = "USD",
            description = "Movie tickets",
            categoryId = 3,
            tags = listOf("cinema", "weekend"),
            date = LocalDateTime.now().minusDays(15)
        ),
        Expense(
            id = 4,
            amount = BigDecimal("75.00"),
            currency = "USD",
            description = "Groceries",
            categoryId = 1,
            tags = listOf("shopping", "weekly"),
            date = LocalDateTime.now().minusDays(2)
        )
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        
        // Setup default mock responses
        every { repository.getAllExpenses() } returns flowOf(testExpenses)
        every { repository.getAllCategories() } returns flowOf(testCategories)
        every { repository.getExpensesByCategory(any()) } answers {
            val categoryId = firstArg<Long>()
            flowOf(testExpenses.filter { it.categoryId == categoryId })
        }
        every { repository.getExpensesByTags(any()) } answers {
            val tags = firstArg<List<String>>()
            flowOf(testExpenses.filter { expense ->
                expense.tags.any { it in tags }
            })
        }
        
        viewModel = AnalyticsViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `should filter analytics by single category`() = runTest {
        // Given
        val foodCategoryId = 1L
        
        // When
        viewModel.applyFilters(
            selectedCategories = listOf(foodCategoryId),
            selectedTags = emptyList(),
            customDateRange = null
        )
        
        // Then
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.analyticsData.test {
            val data = awaitItem()
            
            // Should only include Food category expenses
            assertEquals(BigDecimal("125.00"), data.totalSpending) // 50 + 75
            assertTrue(data.categoryBreakdown.keys.all { it.id == foodCategoryId })
            assertEquals(1, data.categoryBreakdown.size)
        }
    }

    @Test
    fun `should filter analytics by multiple categories`() = runTest {
        // Given
        val selectedCategories = listOf(1L, 2L) // Food and Transport
        
        // When
        viewModel.applyFilters(
            selectedCategories = selectedCategories,
            selectedTags = emptyList(),
            customDateRange = null
        )
        
        // Then
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.analyticsData.test {
            val data = awaitItem()
            
            // Should include Food and Transport expenses
            assertEquals(BigDecimal("155.00"), data.totalSpending) // 50 + 30 + 75
            assertTrue(data.categoryBreakdown.keys.all { it.id in selectedCategories })
            assertEquals(2, data.categoryBreakdown.size)
        }
    }

    @Test
    fun `should filter analytics by tags`() = runTest {
        // Given
        val selectedTags = listOf("business")
        
        // When
        viewModel.applyFilters(
            selectedCategories = emptyList(),
            selectedTags = selectedTags,
            customDateRange = null
        )
        
        // Then
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.analyticsData.test {
            val data = awaitItem()
            
            // Should only include expenses with "business" tag
            assertEquals(BigDecimal("80.00"), data.totalSpending) // 50 + 30
            assertEquals(2, data.categoryBreakdown.size)
        }
    }

    @Test
    fun `should filter analytics by custom date range`() = runTest {
        // Given
        val startDate = LocalDate.now().minusDays(7)
        val endDate = LocalDate.now()
        val dateRange = DateRange(startDate, endDate)
        
        // When
        viewModel.applyFilters(
            selectedCategories = emptyList(),
            selectedTags = emptyList(),
            customDateRange = dateRange
        )
        
        // Then
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.analyticsData.test {
            val data = awaitItem()
            
            // Should only include expenses within last 7 days
            assertEquals(BigDecimal("125.00"), data.totalSpending) // 50 + 75
            // Monthly trends should have data for current month
            assertTrue(data.monthlyTrends.isNotEmpty())
        }
    }

    @Test
    fun `should combine multiple filters`() = runTest {
        // Given
        val selectedCategories = listOf(1L) // Food
        val selectedTags = listOf("business")
        val dateRange = DateRange(
            LocalDate.now().minusDays(10),
            LocalDate.now()
        )
        
        // When
        viewModel.applyFilters(
            selectedCategories = selectedCategories,
            selectedTags = selectedTags,
            customDateRange = dateRange
        )
        
        // Then
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.analyticsData.test {
            val data = awaitItem()
            
            // Should only include Food expenses with "business" tag in last 10 days
            assertEquals(BigDecimal("50.00"), data.totalSpending) // Only lunch expense
            assertEquals(1, data.categoryBreakdown.size)
        }
    }

    @Test
    fun `should clear filters and show all data`() = runTest {
        // Given - filters are applied
        viewModel.applyFilters(
            selectedCategories = listOf(1L),
            selectedTags = listOf("business"),
            customDateRange = null
        )
        
        // When - filters are cleared
        viewModel.clearFilters()
        
        // Then
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.analyticsData.test {
            val data = awaitItem()
            
            // Should show all expenses
            assertEquals(BigDecimal("255.00"), data.totalSpending) // All expenses
            assertEquals(3, data.categoryBreakdown.size) // All categories
        }
    }

    @Test
    fun `should export filtered analytics data`() = runTest {
        // Given
        val selectedCategories = listOf(1L, 2L)
        viewModel.applyFilters(
            selectedCategories = selectedCategories,
            selectedTags = emptyList(),
            customDateRange = null
        )
        
        // When
        testDispatcher.scheduler.advanceUntilIdle()
        val exportData = viewModel.exportAnalyticsData()
        
        // Then
        assertNotNull(exportData)
        assertTrue(exportData.contains("Total Spending"))
        assertTrue(exportData.contains("Category Breakdown"))
        assertTrue(exportData.contains("Food"))
        assertTrue(exportData.contains("Transport"))
    }

    @Test
    fun `should handle drill-down into category details`() = runTest {
        // Given
        val foodCategoryId = 1L
        
        // When
        viewModel.drillDownCategory(foodCategoryId)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then
        val drillDownData = viewModel.categoryDrillDownData.value
        assertNotNull(drillDownData)
        assertEquals(foodCategoryId, drillDownData!!.categoryId)
        assertEquals(2, drillDownData.expenses.size) // 2 food expenses
        assertEquals(BigDecimal("125.00"), drillDownData.totalAmount)
        assertTrue(drillDownData.tagBreakdown.isNotEmpty())
    }

    @Test
    fun `should maintain filter state across period changes`() = runTest {
        // Given
        val selectedCategories = listOf(1L)
        viewModel.applyFilters(
            selectedCategories = selectedCategories,
            selectedTags = emptyList(),
            customDateRange = null
        )
        
        // When - change period
        viewModel.loadAnalytics(TimePeriod.QUARTER)
        
        // Then - filters should still be applied
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.analyticsData.test {
            val data = awaitItem()
            
            assertTrue(data.categoryBreakdown.keys.all { it.id == 1L })
        }
    }
}