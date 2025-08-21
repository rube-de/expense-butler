package com.expensetracker.domain.recurring

import com.expensetracker.data.model.Expense
import com.expensetracker.data.model.RecurrenceFrequency
import com.expensetracker.data.model.RecurringExpense
import com.expensetracker.data.repository.ExpenseRepository
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class RecurringExpenseModificationHandlerTest {

    private lateinit var mockRepository: ExpenseRepository
    private lateinit var mockGenerator: RecurringExpenseGenerator
    private lateinit var handler: RecurringExpenseModificationHandler

    @Before
    fun setup() {
        mockRepository = mockk(relaxed = true)
        mockGenerator = mockk(relaxed = true)
        handler = RecurringExpenseModificationHandler(mockRepository, mockGenerator)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    // Update Recurring Expense Tests

    @Test
    fun `should update recurring expense without affecting past when includePast is false`() = runTest {
        val originalExpense = createTestRecurringExpense(
            id = 1L,
            amount = BigDecimal("100.00"),
            description = "Original description"
        )
        val updatedExpense = originalExpense.copy(
            amount = BigDecimal("150.00"),
            description = "Updated description"
        )

        coEvery { mockRepository.updateRecurringExpense(any()) } returns Result.success(Unit)

        val result = handler.updateRecurringExpense(updatedExpense, includePast = false)

        assertTrue("Should succeed", result.isSuccess)
        coVerify(exactly = 1) { mockRepository.updateRecurringExpense(updatedExpense) }
        // Should not modify any generated expenses when includePast is false
        coVerify(exactly = 0) { mockRepository.getExpensesByRecurringExpenseId(any()) }
    }

    @Test
    fun `should update recurring expense and past expenses when includePast is true`() = runTest {
        val originalExpense = createTestRecurringExpense(
            id = 1L,
            amount = BigDecimal("100.00"),
            description = "Original description"
        )
        val updatedExpense = originalExpense.copy(
            amount = BigDecimal("150.00"),
            description = "Updated description"
        )
        val pastExpenses = listOf(
            createTestExpense(id = 1L, amount = BigDecimal("100.00"), recurringExpenseId = 1L),
            createTestExpense(id = 2L, amount = BigDecimal("100.00"), recurringExpenseId = 1L)
        )

        coEvery { mockRepository.updateRecurringExpense(any()) } returns Result.success(Unit)
        coEvery { mockRepository.getExpensesByRecurringExpenseId(1L) } returns flowOf(pastExpenses)
        coEvery { mockRepository.updateExpense(any()) } returns Result.success(Unit)

        val result = handler.updateRecurringExpense(updatedExpense, includePast = true)

        assertTrue("Should succeed", result.isSuccess)
        coVerify(exactly = 1) { mockRepository.updateRecurringExpense(updatedExpense) }
        coVerify(exactly = 1) { mockRepository.getExpensesByRecurringExpenseId(1L) }
        coVerify(exactly = 2) { mockRepository.updateExpense(any()) }
    }

    @Test
    fun `should only update past expenses within 30 day limit when includePast is true`() = runTest {
        val originalExpense = createTestRecurringExpense(id = 1L)
        val updatedExpense = originalExpense.copy(amount = BigDecimal("150.00"))
        
        val currentDate = LocalDate.now()
        val recentExpense = createTestExpense(
            id = 1L,
            date = currentDate.minusDays(15).atTime(12, 0), // 15 days ago
            recurringExpenseId = 1L
        )
        val oldExpense = createTestExpense(
            id = 2L,
            date = currentDate.minusDays(45).atTime(12, 0), // 45 days ago
            recurringExpenseId = 1L
        )

        coEvery { mockRepository.updateRecurringExpense(any()) } returns Result.success(Unit)
        coEvery { mockRepository.getExpensesByRecurringExpenseId(1L) } returns flowOf(listOf(recentExpense, oldExpense))
        coEvery { mockRepository.updateExpense(any()) } returns Result.success(Unit)

        val result = handler.updateRecurringExpense(updatedExpense, includePast = true)

        assertTrue("Should succeed", result.isSuccess)
        // Should only update the recent expense (within 30 days)
        coVerify(exactly = 1) { mockRepository.updateExpense(match { it.id == 1L }) }
        coVerify(exactly = 0) { mockRepository.updateExpense(match { it.id == 2L }) }
    }

    @Test
    fun `should handle repository failure during recurring expense update`() = runTest {
        val updatedExpense = createTestRecurringExpense(id = 1L)
        val errorMessage = "Database error"

        coEvery { mockRepository.updateRecurringExpense(any()) } returns Result.failure(Exception(errorMessage))

        val result = handler.updateRecurringExpense(updatedExpense, includePast = false)

        assertTrue("Should fail", result.isFailure)
        assertEquals("Should preserve error message", errorMessage, result.exceptionOrNull()?.message)
    }

    @Test
    fun `should continue updating other expenses if one fails when includePast is true`() = runTest {
        val updatedExpense = createTestRecurringExpense(id = 1L)
        val pastExpenses = listOf(
            createTestExpense(id = 1L, recurringExpenseId = 1L),
            createTestExpense(id = 2L, recurringExpenseId = 1L)
        )

        coEvery { mockRepository.updateRecurringExpense(any()) } returns Result.success(Unit)
        coEvery { mockRepository.getExpensesByRecurringExpenseId(1L) } returns flowOf(pastExpenses)
        coEvery { mockRepository.updateExpense(match { it.id == 1L }) } returns Result.failure(Exception("Update failed"))
        coEvery { mockRepository.updateExpense(match { it.id == 2L }) } returns Result.success(Unit)

        val result = handler.updateRecurringExpense(updatedExpense, includePast = true)

        assertTrue("Should succeed overall", result.isSuccess)
        coVerify(exactly = 2) { mockRepository.updateExpense(any()) }
    }

    // Delete Recurring Expense Tests

    @Test
    fun `should delete recurring expense and clean up generated expenses`() = runTest {
        val recurringExpenseId = 1L
        val generatedExpenses = listOf(
            createTestExpense(id = 1L, recurringExpenseId = recurringExpenseId),
            createTestExpense(id = 2L, recurringExpenseId = recurringExpenseId)
        )

        coEvery { mockRepository.getExpensesByRecurringExpenseId(recurringExpenseId) } returns flowOf(generatedExpenses)
        coEvery { mockRepository.deleteExpense(any()) } returns Result.success(Unit)
        coEvery { mockRepository.deleteRecurringExpense(recurringExpenseId) } returns Result.success(Unit)

        val result = handler.deleteRecurringExpenseWithCleanup(recurringExpenseId)

        assertTrue("Should succeed", result.isSuccess)
        coVerify(exactly = 1) { mockRepository.getExpensesByRecurringExpenseId(recurringExpenseId) }
        coVerify(exactly = 2) { mockRepository.deleteExpense(any()) }
        coVerify(exactly = 1) { mockRepository.deleteRecurringExpense(recurringExpenseId) }
    }

    @Test
    fun `should continue deleting other expenses if one fails during cleanup`() = runTest {
        val recurringExpenseId = 1L
        val generatedExpenses = listOf(
            createTestExpense(id = 1L, recurringExpenseId = recurringExpenseId),
            createTestExpense(id = 2L, recurringExpenseId = recurringExpenseId)
        )

        coEvery { mockRepository.getExpensesByRecurringExpenseId(recurringExpenseId) } returns flowOf(generatedExpenses)
        coEvery { mockRepository.deleteExpense(1L) } returns Result.failure(Exception("Delete failed"))
        coEvery { mockRepository.deleteExpense(2L) } returns Result.success(Unit)
        coEvery { mockRepository.deleteRecurringExpense(recurringExpenseId) } returns Result.success(Unit)

        val result = handler.deleteRecurringExpenseWithCleanup(recurringExpenseId)

        assertTrue("Should succeed overall", result.isSuccess)
        coVerify(exactly = 2) { mockRepository.deleteExpense(any()) }
        coVerify(exactly = 1) { mockRepository.deleteRecurringExpense(recurringExpenseId) }
    }

    @Test
    fun `should handle repository failure during recurring expense deletion`() = runTest {
        val recurringExpenseId = 1L
        val errorMessage = "Database error"

        coEvery { mockRepository.getExpensesByRecurringExpenseId(recurringExpenseId) } returns flowOf(emptyList())
        coEvery { mockRepository.deleteRecurringExpense(recurringExpenseId) } returns Result.failure(Exception(errorMessage))

        val result = handler.deleteRecurringExpenseWithCleanup(recurringExpenseId)

        assertTrue("Should fail", result.isFailure)
        assertEquals("Should preserve error message", errorMessage, result.exceptionOrNull()?.message)
    }

    // Validation Tests

    @Test
    fun `should validate modification constraints before updating`() = runTest {
        val invalidExpense = createTestRecurringExpense(
            id = 1L,
            startDate = LocalDate.now().plusDays(1), // Future start date
            lastGenerated = LocalDate.now() // Already generated today
        )

        val result = handler.updateRecurringExpense(invalidExpense, includePast = false)

        assertTrue("Should fail validation", result.isFailure)
        assertTrue("Should contain validation error", 
            result.exceptionOrNull()?.message?.contains("validation", ignoreCase = true) == true ||
            result.exceptionOrNull()?.message?.contains("constraint", ignoreCase = true) == true)
        coVerify(exactly = 0) { mockRepository.updateRecurringExpense(any()) }
    }

    @Test
    fun `should allow updating future-only recurring expense that hasn't generated yet`() = runTest {
        val validExpense = createTestRecurringExpense(
            id = 1L,
            startDate = LocalDate.now(),
            lastGenerated = null // Never generated
        )

        coEvery { mockRepository.updateRecurringExpense(any()) } returns Result.success(Unit)

        val result = handler.updateRecurringExpense(validExpense, includePast = false)

        assertTrue("Should succeed", result.isSuccess)
        coVerify(exactly = 1) { mockRepository.updateRecurringExpense(validExpense) }
    }

    // Edge Cases

    @Test
    fun `should handle empty past expenses list when includePast is true`() = runTest {
        val updatedExpense = createTestRecurringExpense(id = 1L)

        coEvery { mockRepository.updateRecurringExpense(any()) } returns Result.success(Unit)
        coEvery { mockRepository.getExpensesByRecurringExpenseId(1L) } returns flowOf(emptyList())

        val result = handler.updateRecurringExpense(updatedExpense, includePast = true)

        assertTrue("Should succeed", result.isSuccess)
        coVerify(exactly = 1) { mockRepository.updateRecurringExpense(updatedExpense) }
        coVerify(exactly = 0) { mockRepository.updateExpense(any()) }
    }

    @Test
    fun `should handle repository exception when fetching past expenses`() = runTest {
        val updatedExpense = createTestRecurringExpense(id = 1L)

        coEvery { mockRepository.updateRecurringExpense(any()) } returns Result.success(Unit)
        coEvery { mockRepository.getExpensesByRecurringExpenseId(1L) } throws Exception("Repository error")

        val result = handler.updateRecurringExpense(updatedExpense, includePast = true)

        // Should still succeed with recurring expense update, even if past expense fetch fails
        assertTrue("Should succeed", result.isSuccess)
        coVerify(exactly = 1) { mockRepository.updateRecurringExpense(updatedExpense) }
    }

    @Test
    fun `should preserve expense creation timestamps when updating past expenses`() = runTest {
        val originalCreatedAt = LocalDateTime.now().minusDays(10)
        val updatedExpense = createTestRecurringExpense(id = 1L, amount = BigDecimal("200.00"))
        val pastExpense = createTestExpense(
            id = 1L,
            amount = BigDecimal("100.00"),
            recurringExpenseId = 1L
        ).copy(createdAt = originalCreatedAt)

        coEvery { mockRepository.updateRecurringExpense(any()) } returns Result.success(Unit)
        coEvery { mockRepository.getExpensesByRecurringExpenseId(1L) } returns flowOf(listOf(pastExpense))
        
        val capturedExpense = slot<Expense>()
        coEvery { mockRepository.updateExpense(capture(capturedExpense)) } returns Result.success(Unit)

        val result = handler.updateRecurringExpense(updatedExpense, includePast = true)

        assertTrue("Should succeed", result.isSuccess)
        assertEquals("Should preserve original creation timestamp", originalCreatedAt, capturedExpense.captured.createdAt)
        assertEquals("Should update amount", BigDecimal("200.00"), capturedExpense.captured.amount)
    }

    // Helper methods

    private fun createTestRecurringExpense(
        id: Long = 1L,
        amount: BigDecimal = BigDecimal("100.00"),
        currency: String = "USD",
        description: String = "Test recurring expense",
        categoryId: Long = 1L,
        tags: List<String> = emptyList(),
        frequency: RecurrenceFrequency = RecurrenceFrequency.MONTHLY,
        startDate: LocalDate = LocalDate.now(),
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

    private fun createTestExpense(
        id: Long = 1L,
        amount: BigDecimal = BigDecimal("100.00"),
        currency: String = "USD",
        description: String = "Test expense",
        categoryId: Long = 1L,
        tags: List<String> = emptyList(),
        date: LocalDateTime = LocalDateTime.now(),
        recurringExpenseId: Long? = null
    ): Expense {
        return Expense(
            id = id,
            amount = amount,
            currency = currency,
            description = description,
            categoryId = categoryId,
            tags = tags,
            date = date,
            recurringExpenseId = recurringExpenseId
        )
    }
}