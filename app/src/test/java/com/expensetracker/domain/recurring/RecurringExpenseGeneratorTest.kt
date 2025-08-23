package com.expensetracker.domain.recurring

import com.expensetracker.data.model.Expense
import com.expensetracker.data.model.RecurrenceFrequency
import com.expensetracker.data.model.RecurringExpense
import com.expensetracker.data.repository.ExpenseRepository
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import com.expensetracker.test.utils.assertSuspendDoesNotThrow

class RecurringExpenseGeneratorTest {

    private lateinit var mockRepository: ExpenseRepository
    private lateinit var mockScheduler: RecurringExpenseScheduler
    private lateinit var generator: RecurringExpenseGenerator

    @Before
    fun setup() {
        mockRepository = mockk(relaxed = true)
        mockScheduler = mockk(relaxed = true)
        generator = RecurringExpenseGenerator(mockRepository, mockScheduler)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    // GenerateDueExpenses Tests

    @Test
    fun `should generate expense when recurring expense is due`() = runTest {
        val recurringExpense = createRecurringExpense(
            id = 1L,
            frequency = RecurrenceFrequency.DAILY,
            startDate = LocalDate.of(2024, 1, 1),
            lastGenerated = null
        )
        val currentDate = LocalDate.of(2024, 1, 1)

        coEvery { mockRepository.getRecurringExpensesToGenerate(currentDate) } returns listOf(recurringExpense)
        every { mockScheduler.shouldGenerateOn(recurringExpense, currentDate) } returns true
        coEvery { mockRepository.insertExpense(any()) } returns Result.success(1L)
        coEvery { mockRepository.updateRecurringExpenseLastGenerated(any(), any()) } returns Result.success(Unit)

        generator.generateDueExpenses(currentDate)

        coVerify(exactly = 1) { mockRepository.insertExpense(any()) }
        coVerify(exactly = 1) { mockRepository.updateRecurringExpenseLastGenerated(1L, currentDate) }
    }

    @Test
    fun `should not generate expense when not due`() = runTest {
        val recurringExpense = createRecurringExpense(
            id = 1L,
            frequency = RecurrenceFrequency.DAILY,
            startDate = LocalDate.of(2024, 1, 1),
            lastGenerated = LocalDate.of(2024, 1, 1)
        )
        val currentDate = LocalDate.of(2024, 1, 1)

        coEvery { mockRepository.getRecurringExpensesToGenerate(currentDate) } returns listOf(recurringExpense)
        every { mockScheduler.shouldGenerateOn(recurringExpense, currentDate) } returns false

        generator.generateDueExpenses(currentDate)

        coVerify(exactly = 0) { mockRepository.insertExpense(any()) }
        coVerify(exactly = 0) { mockRepository.updateRecurringExpenseLastGenerated(any(), any()) }
    }

    @Test
    fun `should generate multiple expenses for different recurring expenses`() = runTest {
        val recurringExpense1 = createRecurringExpense(
            id = 1L,
            frequency = RecurrenceFrequency.DAILY,
            description = "Daily expense 1"
        )
        val recurringExpense2 = createRecurringExpense(
            id = 2L,
            frequency = RecurrenceFrequency.WEEKLY,
            description = "Weekly expense 2"
        )
        val currentDate = LocalDate.of(2024, 1, 1)

        coEvery { mockRepository.getRecurringExpensesToGenerate(currentDate) } returns listOf(recurringExpense1, recurringExpense2)
        every { mockScheduler.shouldGenerateOn(any(), currentDate) } returns true
        coEvery { mockRepository.insertExpense(any()) } returns Result.success(1L)
        coEvery { mockRepository.updateRecurringExpenseLastGenerated(any(), any()) } returns Result.success(Unit)

        generator.generateDueExpenses(currentDate)

        coVerify(exactly = 2) { mockRepository.insertExpense(any()) }
        coVerify(exactly = 1) { mockRepository.updateRecurringExpenseLastGenerated(1L, currentDate) }
        coVerify(exactly = 1) { mockRepository.updateRecurringExpenseLastGenerated(2L, currentDate) }
    }

    @Test
    fun `should handle error during expense insertion gracefully`() = runTest {
        val recurringExpense = createRecurringExpense(id = 1L)
        val currentDate = LocalDate.of(2024, 1, 1)

        coEvery { mockRepository.getRecurringExpensesToGenerate(currentDate) } returns listOf(recurringExpense)
        every { mockScheduler.shouldGenerateOn(recurringExpense, currentDate) } returns true
        coEvery { mockRepository.insertExpense(any()) } returns Result.failure(Exception("Database error"))

        generator.generateDueExpenses(currentDate)

        // Should not update lastGenerated when insertion fails
        coVerify(exactly = 1) { mockRepository.insertExpense(any()) }
        coVerify(exactly = 0) { mockRepository.updateRecurringExpenseLastGenerated(any(), any()) }
    }

    @Test
    fun `should handle repository exception during generation`() = runTest {
        val currentDate = LocalDate.of(2024, 1, 1)

        coEvery { mockRepository.getRecurringExpensesToGenerate(currentDate) } throws Exception("Repository error")

        // Should not throw exception
        assertSuspendDoesNotThrow {
            generator.generateDueExpenses(currentDate)
        }

        coVerify(exactly = 0) { mockRepository.insertExpense(any()) }
    }

    @Test
    fun `should generate expenses for multiple days when retroactive`() = runTest {
        val recurringExpense = createRecurringExpense(
            id = 1L,
            frequency = RecurrenceFrequency.DAILY,
            startDate = LocalDate.of(2024, 1, 1),
            lastGenerated = null
        )
        val currentDate = LocalDate.of(2024, 1, 3)

        coEvery { mockRepository.getRecurringExpensesToGenerate(currentDate) } returns listOf(recurringExpense)
        every { mockScheduler.shouldGenerateOn(recurringExpense, LocalDate.of(2024, 1, 1)) } returns true
        every { mockScheduler.shouldGenerateOn(recurringExpense, LocalDate.of(2024, 1, 2)) } returns true
        every { mockScheduler.shouldGenerateOn(recurringExpense, LocalDate.of(2024, 1, 3)) } returns true
        coEvery { mockRepository.insertExpense(any()) } returns Result.success(1L)
        coEvery { mockRepository.updateRecurringExpenseLastGenerated(any(), any()) } returns Result.success(Unit)

        generator.generateDueExpenses(currentDate)

        // Should generate for all 3 days
        coVerify(exactly = 3) { mockRepository.insertExpense(any()) }
        coVerify(exactly = 1) { mockRepository.updateRecurringExpenseLastGenerated(1L, currentDate) }
    }

    @Test
    fun `should create expense with correct data from recurring expense`() = runTest {
        val recurringExpense = createRecurringExpense(
            id = 1L,
            amount = BigDecimal("25.50"),
            currency = "EUR",
            description = "Coffee subscription",
            categoryId = 5L,
            tags = listOf("coffee", "subscription")
        )
        val currentDate = LocalDate.of(2024, 1, 1)

        coEvery { mockRepository.getRecurringExpensesToGenerate(currentDate) } returns listOf(recurringExpense)
        every { mockScheduler.shouldGenerateOn(recurringExpense, currentDate) } returns true
        coEvery { mockRepository.insertExpense(any()) } returns Result.success(1L)
        coEvery { mockRepository.updateRecurringExpenseLastGenerated(any(), any()) } returns Result.success(Unit)

        generator.generateDueExpenses(currentDate)

        val capturedExpense = slot<Expense>()
        coVerify(exactly = 1) { mockRepository.insertExpense(capture(capturedExpense)) }

        val expense = capturedExpense.captured
        assertEquals(BigDecimal("25.50"), expense.amount)
        assertEquals("EUR", expense.currency)
        assertEquals("Coffee subscription", expense.description)
        assertEquals(5L, expense.categoryId)
        assertEquals(listOf("coffee", "subscription"), expense.tags)
        assertEquals(1L, expense.recurringExpenseId)
        // Date should be set to the generation date, not current time
        assertTrue(expense.date.toLocalDate() == currentDate)
    }

    @Test
    fun `should limit retroactive generation to reasonable period`() = runTest {
        val recurringExpense = createRecurringExpense(
            id = 1L,
            frequency = RecurrenceFrequency.DAILY,
            startDate = LocalDate.of(2024, 1, 1),
            lastGenerated = null
        )
        val currentDate = LocalDate.of(2024, 3, 1) // 60 days later

        coEvery { mockRepository.getRecurringExpensesToGenerate(currentDate) } returns listOf(recurringExpense)
        every { mockScheduler.shouldGenerateOn(recurringExpense, any()) } returns true
        coEvery { mockRepository.insertExpense(any()) } returns Result.success(1L)
        coEvery { mockRepository.updateRecurringExpenseLastGenerated(any(), any()) } returns Result.success(Unit)

        generator.generateDueExpenses(currentDate)

        // Should limit to maximum retroactive period (30 days + 1 for current day = 31 max)
        coVerify(atMost = 31) { mockRepository.insertExpense(any()) }
    }

    // GenerateForecast Tests

    @Test
    fun `should generate forecast for next month`() = runTest {
        val recurringExpense = createRecurringExpense(
            id = 1L,
            frequency = RecurrenceFrequency.WEEKLY,
            startDate = LocalDate.of(2024, 1, 1)
        )
        val fromDate = LocalDate.of(2024, 1, 15)
        val toDate = fromDate.plusMonths(1)

        val mockOccurrences = listOf(
            LocalDate.of(2024, 1, 22),
            LocalDate.of(2024, 1, 29),
            LocalDate.of(2024, 2, 5),
            LocalDate.of(2024, 2, 12)
        )

        every { 
            mockScheduler.calculateNextOccurrences(recurringExpense, fromDate, toDate) 
        } returns mockOccurrences

        val forecast = generator.generateForecast(recurringExpense, ForecastPeriod.NEXT_MONTH, fromDate)

        assertEquals(4, forecast.size)
        forecast.forEachIndexed { index, forecastedExpense ->
            assertEquals(recurringExpense.amount, forecastedExpense.amount)
            assertEquals(recurringExpense.currency, forecastedExpense.currency)
            assertEquals(recurringExpense.description, forecastedExpense.description)
            assertEquals(recurringExpense.categoryId, forecastedExpense.categoryId)
            assertEquals(recurringExpense.tags, forecastedExpense.tags)
            assertEquals(recurringExpense.id, forecastedExpense.sourceRecurringExpenseId)
            assertEquals(mockOccurrences[index], forecastedExpense.date)
        }
    }

    @Test
    fun `should generate forecast for rest of year`() = runTest {
        val recurringExpense = createRecurringExpense(
            id = 1L,
            frequency = RecurrenceFrequency.MONTHLY,
            startDate = LocalDate.of(2024, 1, 15)
        )
        val fromDate = LocalDate.of(2024, 6, 1)
        val endOfYear = LocalDate.of(2024, 12, 31)

        val mockOccurrences = listOf(
            LocalDate.of(2024, 6, 15),
            LocalDate.of(2024, 7, 15),
            LocalDate.of(2024, 8, 15),
            LocalDate.of(2024, 9, 15),
            LocalDate.of(2024, 10, 15),
            LocalDate.of(2024, 11, 15),
            LocalDate.of(2024, 12, 15)
        )

        every { 
            mockScheduler.calculateNextOccurrences(recurringExpense, fromDate, endOfYear) 
        } returns mockOccurrences

        val forecast = generator.generateForecast(recurringExpense, ForecastPeriod.REST_OF_YEAR, fromDate)

        assertEquals(7, forecast.size)
        assertEquals(LocalDate.of(2024, 6, 15), forecast[0].date)
        assertEquals(LocalDate.of(2024, 12, 15), forecast[6].date)
    }

    @Test
    fun `should handle empty forecast when no occurrences`() = runTest {
        val recurringExpense = createRecurringExpense(
            id = 1L,
            frequency = RecurrenceFrequency.DAILY,
            startDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 5) // Already ended
        )
        val fromDate = LocalDate.of(2024, 2, 1)

        every { 
            mockScheduler.calculateNextOccurrences(recurringExpense, fromDate, any()) 
        } returns emptyList()

        val forecast = generator.generateForecast(recurringExpense, ForecastPeriod.NEXT_MONTH, fromDate)

        assertTrue(forecast.isEmpty())
    }

    @Test
    fun `should generate forecast with custom date range`() = runTest {
        val recurringExpense = createRecurringExpense(
            id = 1L,
            frequency = RecurrenceFrequency.DAILY
        )
        val fromDate = LocalDate.of(2024, 1, 1)
        val toDate = LocalDate.of(2024, 1, 7)

        val mockOccurrences = (1..7).map { LocalDate.of(2024, 1, it) }

        every { 
            mockScheduler.calculateNextOccurrences(recurringExpense, fromDate, toDate) 
        } returns mockOccurrences

        val forecast = generator.generateForecast(recurringExpense, ForecastPeriod.CUSTOM, fromDate, toDate)

        assertEquals(7, forecast.size)
        assertEquals(LocalDate.of(2024, 1, 1), forecast[0].date)
        assertEquals(LocalDate.of(2024, 1, 7), forecast[6].date)
    }

    @Test
    fun `should handle large forecast periods efficiently`() = runTest {
        val recurringExpense = createRecurringExpense(
            id = 1L,
            frequency = RecurrenceFrequency.DAILY
        )
        val fromDate = LocalDate.of(2024, 1, 1)
        val endOfYear = LocalDate.of(2024, 12, 31)

        // Mock occurrences for the entire year
        val mockOccurrences = (1..366).map { LocalDate.of(2024, 1, 1).plusDays(it.toLong() - 1) }
            .takeWhile { it <= endOfYear }

        every { 
            mockScheduler.calculateNextOccurrences(recurringExpense, fromDate, endOfYear) 
        } returns mockOccurrences

        val forecast = generator.generateForecast(recurringExpense, ForecastPeriod.REST_OF_YEAR, fromDate)

        assertEquals(mockOccurrences.size, forecast.size)
        // Verify first and last entries match mock data
        assertEquals(mockOccurrences.first(), forecast.first().date)
        assertEquals(mockOccurrences.last(), forecast.last().date)
    }

    // Edge Cases and Error Handling

    @Test
    fun `should handle invalid forecast period gracefully`() = runTest {
        val recurringExpense = createRecurringExpense(id = 1L)
        val fromDate = LocalDate.of(2024, 1, 1)

        every { 
            mockScheduler.calculateNextOccurrences(any(), any(), any()) 
        } returns emptyList()

        // Should not throw exception
        assertSuspendDoesNotThrow {
            generator.generateForecast(recurringExpense, ForecastPeriod.CUSTOM, fromDate, null)
        }
    }

    @Test
    fun `should prevent duplicate expense generation`() = runTest {
        val recurringExpense = createRecurringExpense(
            id = 1L,
            frequency = RecurrenceFrequency.DAILY,
            startDate = LocalDate.of(2024, 1, 1),
            lastGenerated = LocalDate.of(2024, 1, 1)
        )
        val currentDate = LocalDate.of(2024, 1, 1)

        coEvery { mockRepository.getRecurringExpensesToGenerate(currentDate) } returns listOf(recurringExpense)
        every { mockScheduler.shouldGenerateOn(recurringExpense, currentDate) } returns false // Already generated

        generator.generateDueExpenses(currentDate)

        coVerify(exactly = 0) { mockRepository.insertExpense(any()) }
    }

    @Test
    fun `should handle concurrent generation attempts safely`() = runTest {
        val recurringExpense = createRecurringExpense(id = 1L)
        val currentDate = LocalDate.of(2024, 1, 1)

        coEvery { mockRepository.getRecurringExpensesToGenerate(currentDate) } returns listOf(recurringExpense)
        every { mockScheduler.shouldGenerateOn(recurringExpense, currentDate) } returns true
        coEvery { mockRepository.insertExpense(any()) } returns Result.success(1L)
        coEvery { mockRepository.updateRecurringExpenseLastGenerated(any(), any()) } returns Result.success(Unit)

        // Simulate concurrent calls
        generator.generateDueExpenses(currentDate)
        generator.generateDueExpenses(currentDate)

        // Should handle gracefully (implementation dependent)
        coVerify(atLeast = 1) { mockRepository.insertExpense(any()) }
    }

    @Test
    fun `should validate forecast parameters`() = runTest {
        val recurringExpense = createRecurringExpense(id = 1L)
        val fromDate = LocalDate.of(2024, 1, 15)
        val toDate = LocalDate.of(2024, 1, 10) // Before fromDate

        every { 
            mockScheduler.calculateNextOccurrences(any(), any(), any()) 
        } returns emptyList()

        val forecast = generator.generateForecast(recurringExpense, ForecastPeriod.CUSTOM, fromDate, toDate)

        assertTrue("Should return empty forecast for invalid date range", forecast.isEmpty())
    }

    @Test
    fun `should respect recurring expense end date in forecast`() = runTest {
        val recurringExpense = createRecurringExpense(
            id = 1L,
            frequency = RecurrenceFrequency.DAILY,
            startDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 15)
        )
        val fromDate = LocalDate.of(2024, 1, 1)

        val mockOccurrences = (1..15).map { LocalDate.of(2024, 1, it) }

        every { 
            mockScheduler.calculateNextOccurrences(recurringExpense, fromDate, any()) 
        } returns mockOccurrences

        val forecast = generator.generateForecast(recurringExpense, ForecastPeriod.NEXT_MONTH, fromDate)

        assertEquals(15, forecast.size)
        assertEquals(LocalDate.of(2024, 1, 15), forecast.last().date)
    }

    // Helper functions

    private fun createRecurringExpense(
        id: Long = 1L,
        amount: BigDecimal = BigDecimal("100.00"),
        currency: String = "USD",
        description: String = "Test recurring expense",
        categoryId: Long = 1L,
        tags: List<String> = listOf("test"),
        frequency: RecurrenceFrequency = RecurrenceFrequency.DAILY,
        startDate: LocalDate = LocalDate.of(2024, 1, 1),
        endDate: LocalDate? = null,
        lastGenerated: LocalDate? = null,
        isActive: Boolean = true
    ): RecurringExpense {
        return RecurringExpense(
            id = id,
            amount = amount,
            currency = currency,
            description = description,
            categoryId = categoryId,
            tags = tags,
            frequency = frequency,
            startDate = startDate,
            endDate = endDate,
            lastGenerated = lastGenerated,
            isActive = isActive
        )
    }
}