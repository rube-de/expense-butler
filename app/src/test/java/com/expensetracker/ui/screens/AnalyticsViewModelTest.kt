package com.expensetracker.ui.screens

import com.expensetracker.data.model.*
import com.expensetracker.data.repository.ExpenseRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsViewModelTest {

    private lateinit var mockRepository: ExpenseRepository
    private lateinit var viewModel: AnalyticsViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockRepository = mockk()
        viewModel = AnalyticsViewModel(mockRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `should calculate total spending correctly`() = runTest {
        // Arrange
        val expenses = listOf(
            createTestExpense(amount = BigDecimal("100.00")),
            createTestExpense(amount = BigDecimal("50.00")),
            createTestExpense(amount = BigDecimal("25.00"))
        )
        every { mockRepository.getAllExpenses() } returns flowOf(expenses)
        every { mockRepository.getAllCategories() } returns flowOf(emptyList())

        // Act
        viewModel.loadAnalytics(TimePeriod.MONTH)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val analyticsData = viewModel.analyticsData.value
        assertEquals(BigDecimal("175.00"), analyticsData.totalSpending)
    }

    @Test
    fun `should calculate category breakdown correctly`() = runTest {
        // Arrange
        val category1 = createTestCategory(id = 1L, name = "Food")
        val category2 = createTestCategory(id = 2L, name = "Travel")
        val expenses = listOf(
            createTestExpense(amount = BigDecimal("100.00"), categoryId = 1L),
            createTestExpense(amount = BigDecimal("50.00"), categoryId = 1L),
            createTestExpense(amount = BigDecimal("75.00"), categoryId = 2L)
        )
        every { mockRepository.getAllExpenses() } returns flowOf(expenses)
        every { mockRepository.getAllCategories() } returns flowOf(listOf(category1, category2))

        // Act
        viewModel.loadAnalytics(TimePeriod.MONTH)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val analyticsData = viewModel.analyticsData.value
        assertEquals(BigDecimal("150.00"), analyticsData.categoryBreakdown[category1])
        assertEquals(BigDecimal("75.00"), analyticsData.categoryBreakdown[category2])
    }

    @Test
    fun `should calculate monthly trends correctly`() = runTest {
        // Arrange
        val currentMonth = LocalDate.now().withDayOfMonth(1)
        val previousMonth = currentMonth.minusMonths(1)
        val expenses = listOf(
            createTestExpense(
                amount = BigDecimal("100.00"),
                date = currentMonth.atStartOfDay()
            ),
            createTestExpense(
                amount = BigDecimal("50.00"),
                date = previousMonth.atStartOfDay()
            )
        )
        every { mockRepository.getAllExpenses() } returns flowOf(expenses)
        every { mockRepository.getAllCategories() } returns flowOf(emptyList())

        // Act
        viewModel.loadAnalytics(TimePeriod.MONTH)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val analyticsData = viewModel.analyticsData.value
        assertTrue(analyticsData.monthlyTrends.isNotEmpty())
        val currentMonthTrend = analyticsData.monthlyTrends.find { it.month == currentMonth }
        assertNotNull(currentMonthTrend)
        assertEquals(BigDecimal("100.00"), currentMonthTrend!!.amount)
    }

    @Test
    fun `should calculate top tags correctly`() = runTest {
        // Arrange
        val expenses = listOf(
            createTestExpense(amount = BigDecimal("100.00"), tags = listOf("groceries", "food")),
            createTestExpense(amount = BigDecimal("50.00"), tags = listOf("groceries")),
            createTestExpense(amount = BigDecimal("75.00"), tags = listOf("travel", "vacation"))
        )
        every { mockRepository.getAllExpenses() } returns flowOf(expenses)
        every { mockRepository.getAllCategories() } returns flowOf(emptyList())

        // Act
        viewModel.loadAnalytics(TimePeriod.MONTH)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val analyticsData = viewModel.analyticsData.value
        val groceriesTag = analyticsData.topTags.find { it.tag == "groceries" }
        assertNotNull(groceriesTag)
        assertEquals(BigDecimal("150.00"), groceriesTag!!.amount)
        assertEquals(2, groceriesTag.expenseCount)
    }

    @Test
    fun `should calculate period comparison correctly`() = runTest {
        // Arrange
        val currentDate = LocalDate.now()
        val currentMonthStart = currentDate.withDayOfMonth(1)
        val previousMonthStart = currentMonthStart.minusMonths(1)
        
        val expenses = listOf(
            createTestExpense(
                amount = BigDecimal("100.00"),
                date = currentMonthStart.atStartOfDay()
            ),
            createTestExpense(
                amount = BigDecimal("50.00"),
                date = previousMonthStart.atStartOfDay()
            )
        )
        every { mockRepository.getAllExpenses() } returns flowOf(expenses)
        every { mockRepository.getAllCategories() } returns flowOf(emptyList())

        // Act
        viewModel.comparePeriodsData(TimePeriod.MONTH, TimePeriod.MONTH)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val analyticsData = viewModel.analyticsData.value
        assertNotNull(analyticsData.periodComparison)
        assertEquals(BigDecimal("100.00"), analyticsData.periodComparison!!.currentPeriod.totalAmount)
        assertEquals(BigDecimal("50.00"), analyticsData.periodComparison!!.previousPeriod.totalAmount)
        assertEquals(100.0, analyticsData.periodComparison!!.percentageChange, 0.01)
    }

    @Test
    fun `should handle empty expense list`() = runTest {
        // Arrange
        every { mockRepository.getAllExpenses() } returns flowOf(emptyList())
        every { mockRepository.getAllCategories() } returns flowOf(emptyList())

        // Act
        viewModel.loadAnalytics(TimePeriod.MONTH)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val analyticsData = viewModel.analyticsData.value
        assertEquals(BigDecimal.ZERO, analyticsData.totalSpending)
        assertTrue(analyticsData.categoryBreakdown.isEmpty())
        assertTrue(analyticsData.monthlyTrends.isEmpty())
        assertTrue(analyticsData.topTags.isEmpty())
    }

    @Test
    fun `should filter expenses by date range for quarter period`() = runTest {
        // Arrange
        val currentDate = LocalDate.now()
        val quarterStart = currentDate.withDayOfMonth(1).withMonth(((currentDate.monthValue - 1) / 3) * 3 + 1)
        val expenses = listOf(
            createTestExpense(
                amount = BigDecimal("100.00"),
                date = quarterStart.atStartOfDay()
            ),
            createTestExpense(
                amount = BigDecimal("50.00"),
                date = quarterStart.minusMonths(4).atStartOfDay() // Outside quarter
            )
        )
        every { mockRepository.getAllExpenses() } returns flowOf(expenses)
        every { mockRepository.getAllCategories() } returns flowOf(emptyList())

        // Act
        viewModel.loadAnalytics(TimePeriod.QUARTER)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val analyticsData = viewModel.analyticsData.value
        // Should only include expenses from current quarter
        assertEquals(BigDecimal("100.00"), analyticsData.totalSpending)
    }

    private fun createTestExpense(
        id: Long = 1L,
        amount: BigDecimal = BigDecimal("10.00"),
        currency: String = "USD",
        description: String = "Test expense",
        categoryId: Long = 1L,
        tags: List<String> = emptyList(),
        date: LocalDateTime = LocalDateTime.now()
    ) = Expense(
        id = id,
        amount = amount,
        currency = currency,
        description = description,
        categoryId = categoryId,
        tags = tags,
        date = date
    )

    private fun createTestCategory(
        id: Long = 1L,
        name: String = "Test Category",
        color: String = "#FF0000",
        icon: String = "category"
    ) = Category(
        id = id,
        name = name,
        color = color,
        icon = icon
    )
}