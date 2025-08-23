package com.expensetracker.domain.recurring

import com.expensetracker.data.model.RecurrenceFrequency
import com.expensetracker.data.model.RecurringExpense
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class RecurringExpenseSchedulerTest {

    private val scheduler = RecurringExpenseScheduler()

    // CalculateNextOccurrences Tests

    @Test
    fun `should calculate daily occurrences correctly`() {
        val recurringExpense = createRecurringExpense(
            frequency = RecurrenceFrequency.DAILY,
            startDate = LocalDate.of(2024, 1, 1)
        )

        val occurrences = scheduler.calculateNextOccurrences(
            recurring = recurringExpense,
            from = LocalDate.of(2024, 1, 1),
            until = LocalDate.of(2024, 1, 5)
        )

        assertEquals(5, occurrences.size)
        assertEquals(LocalDate.of(2024, 1, 1), occurrences[0])
        assertEquals(LocalDate.of(2024, 1, 2), occurrences[1])
        assertEquals(LocalDate.of(2024, 1, 3), occurrences[2])
        assertEquals(LocalDate.of(2024, 1, 4), occurrences[3])
        assertEquals(LocalDate.of(2024, 1, 5), occurrences[4])
    }

    @Test
    fun `should calculate weekly occurrences correctly`() {
        val recurringExpense = createRecurringExpense(
            frequency = RecurrenceFrequency.WEEKLY,
            startDate = LocalDate.of(2024, 1, 1) // Monday
        )

        val occurrences = scheduler.calculateNextOccurrences(
            recurring = recurringExpense,
            from = LocalDate.of(2024, 1, 1),
            until = LocalDate.of(2024, 1, 29)
        )

        assertEquals(5, occurrences.size)
        assertEquals(LocalDate.of(2024, 1, 1), occurrences[0])
        assertEquals(LocalDate.of(2024, 1, 8), occurrences[1])
        assertEquals(LocalDate.of(2024, 1, 15), occurrences[2])
        assertEquals(LocalDate.of(2024, 1, 22), occurrences[3])
        assertEquals(LocalDate.of(2024, 1, 29), occurrences[4])
    }

    @Test
    fun `should calculate monthly occurrences correctly`() {
        val recurringExpense = createRecurringExpense(
            frequency = RecurrenceFrequency.MONTHLY,
            startDate = LocalDate.of(2024, 1, 15)
        )

        val occurrences = scheduler.calculateNextOccurrences(
            recurring = recurringExpense,
            from = LocalDate.of(2024, 1, 15),
            until = LocalDate.of(2024, 4, 15)
        )

        assertEquals(4, occurrences.size)
        assertEquals(LocalDate.of(2024, 1, 15), occurrences[0])
        assertEquals(LocalDate.of(2024, 2, 15), occurrences[1])
        assertEquals(LocalDate.of(2024, 3, 15), occurrences[2])
        assertEquals(LocalDate.of(2024, 4, 15), occurrences[3])
    }

    @Test
    fun `should handle monthly occurrences on month end dates`() {
        val recurringExpense = createRecurringExpense(
            frequency = RecurrenceFrequency.MONTHLY,
            startDate = LocalDate.of(2024, 1, 31)
        )

        val occurrences = scheduler.calculateNextOccurrences(
            recurring = recurringExpense,
            from = LocalDate.of(2024, 1, 31),
            until = LocalDate.of(2024, 4, 30)
        )

        assertEquals(4, occurrences.size)
        assertEquals(LocalDate.of(2024, 1, 31), occurrences[0])
        assertEquals(LocalDate.of(2024, 2, 29), occurrences[1]) // Leap year
        assertEquals(LocalDate.of(2024, 3, 31), occurrences[2])
        assertEquals(LocalDate.of(2024, 4, 30), occurrences[3])
    }

    @Test
    fun `should handle monthly occurrences on 31st for February in non-leap year`() {
        val recurringExpense = createRecurringExpense(
            frequency = RecurrenceFrequency.MONTHLY,
            startDate = LocalDate.of(2023, 1, 31)
        )

        val occurrences = scheduler.calculateNextOccurrences(
            recurring = recurringExpense,
            from = LocalDate.of(2023, 1, 31),
            until = LocalDate.of(2023, 3, 31)
        )

        assertEquals(3, occurrences.size)
        assertEquals(LocalDate.of(2023, 1, 31), occurrences[0])
        assertEquals(LocalDate.of(2023, 2, 28), occurrences[1]) // Non-leap year
        assertEquals(LocalDate.of(2023, 3, 31), occurrences[2])
    }

    @Test
    fun `should calculate yearly occurrences correctly`() {
        val recurringExpense = createRecurringExpense(
            frequency = RecurrenceFrequency.YEARLY,
            startDate = LocalDate.of(2024, 6, 15)
        )

        val occurrences = scheduler.calculateNextOccurrences(
            recurring = recurringExpense,
            from = LocalDate.of(2024, 6, 15),
            until = LocalDate.of(2027, 6, 15)
        )

        assertEquals(4, occurrences.size)
        assertEquals(LocalDate.of(2024, 6, 15), occurrences[0])
        assertEquals(LocalDate.of(2025, 6, 15), occurrences[1])
        assertEquals(LocalDate.of(2026, 6, 15), occurrences[2])
        assertEquals(LocalDate.of(2027, 6, 15), occurrences[3])
    }

    @Test
    fun `should handle yearly occurrence on February 29th`() {
        val recurringExpense = createRecurringExpense(
            frequency = RecurrenceFrequency.YEARLY,
            startDate = LocalDate.of(2024, 2, 29) // Leap year
        )

        val occurrences = scheduler.calculateNextOccurrences(
            recurring = recurringExpense,
            from = LocalDate.of(2024, 2, 29),
            until = LocalDate.of(2027, 3, 1)
        )

        assertEquals(4, occurrences.size)
        assertEquals(LocalDate.of(2024, 2, 29), occurrences[0])
        assertEquals(LocalDate.of(2025, 2, 28), occurrences[1]) // Non-leap year
        assertEquals(LocalDate.of(2026, 2, 28), occurrences[2]) // Non-leap year
        assertEquals(LocalDate.of(2027, 2, 28), occurrences[3]) // Non-leap year
    }

    @Test
    fun `should return empty list when from date is after until date`() {
        val recurringExpense = createRecurringExpense(
            frequency = RecurrenceFrequency.DAILY,
            startDate = LocalDate.of(2024, 1, 1)
        )

        val occurrences = scheduler.calculateNextOccurrences(
            recurring = recurringExpense,
            from = LocalDate.of(2024, 1, 10),
            until = LocalDate.of(2024, 1, 5)
        )

        assertTrue(occurrences.isEmpty())
    }

    @Test
    fun `should respect recurring expense end date`() {
        val recurringExpense = createRecurringExpense(
            frequency = RecurrenceFrequency.DAILY,
            startDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 3)
        )

        val occurrences = scheduler.calculateNextOccurrences(
            recurring = recurringExpense,
            from = LocalDate.of(2024, 1, 1),
            until = LocalDate.of(2024, 1, 10)
        )

        assertEquals(3, occurrences.size)
        assertEquals(LocalDate.of(2024, 1, 1), occurrences[0])
        assertEquals(LocalDate.of(2024, 1, 2), occurrences[1])
        assertEquals(LocalDate.of(2024, 1, 3), occurrences[2])
    }

    // ShouldGenerateOn Tests

    @Test
    fun `should return true when date matches daily frequency`() {
        val recurringExpense = createRecurringExpense(
            frequency = RecurrenceFrequency.DAILY,
            startDate = LocalDate.of(2024, 1, 1),
            lastGenerated = null
        )

        assertTrue(scheduler.shouldGenerateOn(recurringExpense, LocalDate.of(2024, 1, 1)))
        assertTrue(scheduler.shouldGenerateOn(recurringExpense, LocalDate.of(2024, 1, 2)))
    }

    @Test
    fun `should return false when date already generated`() {
        val recurringExpense = createRecurringExpense(
            frequency = RecurrenceFrequency.DAILY,
            startDate = LocalDate.of(2024, 1, 1),
            lastGenerated = LocalDate.of(2024, 1, 2)
        )

        assertFalse(scheduler.shouldGenerateOn(recurringExpense, LocalDate.of(2024, 1, 1)))
        assertFalse(scheduler.shouldGenerateOn(recurringExpense, LocalDate.of(2024, 1, 2)))
        assertTrue(scheduler.shouldGenerateOn(recurringExpense, LocalDate.of(2024, 1, 3)))
    }

    @Test
    fun `should return false when date is before start date`() {
        val recurringExpense = createRecurringExpense(
            frequency = RecurrenceFrequency.DAILY,
            startDate = LocalDate.of(2024, 1, 5),
            lastGenerated = null
        )

        assertFalse(scheduler.shouldGenerateOn(recurringExpense, LocalDate.of(2024, 1, 4)))
        assertTrue(scheduler.shouldGenerateOn(recurringExpense, LocalDate.of(2024, 1, 5)))
    }

    @Test
    fun `should return false when date is after end date`() {
        val recurringExpense = createRecurringExpense(
            frequency = RecurrenceFrequency.DAILY,
            startDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 5),
            lastGenerated = null
        )

        assertTrue(scheduler.shouldGenerateOn(recurringExpense, LocalDate.of(2024, 1, 5)))
        assertFalse(scheduler.shouldGenerateOn(recurringExpense, LocalDate.of(2024, 1, 6)))
    }

    @Test
    fun `should return false when recurring expense is inactive`() {
        val recurringExpense = createRecurringExpense(
            frequency = RecurrenceFrequency.DAILY,
            startDate = LocalDate.of(2024, 1, 1),
            isActive = false,
            lastGenerated = null
        )

        assertFalse(scheduler.shouldGenerateOn(recurringExpense, LocalDate.of(2024, 1, 1)))
    }

    @Test
    fun `should handle weekly frequency correctly for Monday start`() {
        val recurringExpense = createRecurringExpense(
            frequency = RecurrenceFrequency.WEEKLY,
            startDate = LocalDate.of(2024, 1, 1), // Monday
            lastGenerated = null
        )

        assertTrue(scheduler.shouldGenerateOn(recurringExpense, LocalDate.of(2024, 1, 1))) // Monday start
        assertFalse(scheduler.shouldGenerateOn(recurringExpense, LocalDate.of(2024, 1, 2))) // Tuesday
        assertTrue(scheduler.shouldGenerateOn(recurringExpense, LocalDate.of(2024, 1, 8))) // Next Monday
    }

    @Test
    fun `should handle monthly frequency correctly for 15th`() {
        val recurringExpense = createRecurringExpense(
            frequency = RecurrenceFrequency.MONTHLY,
            startDate = LocalDate.of(2024, 1, 15),
            lastGenerated = null
        )

        assertTrue(scheduler.shouldGenerateOn(recurringExpense, LocalDate.of(2024, 1, 15))) // 15th start
        assertFalse(scheduler.shouldGenerateOn(recurringExpense, LocalDate.of(2024, 1, 16))) // 16th
        assertTrue(scheduler.shouldGenerateOn(recurringExpense, LocalDate.of(2024, 2, 15))) // Next month 15th
    }

    @Test
    fun `should handle monthly frequency for month end dates`() {
        val recurringExpense = createRecurringExpense(
            frequency = RecurrenceFrequency.MONTHLY,
            startDate = LocalDate.of(2024, 1, 31),
            lastGenerated = null
        )

        assertTrue(scheduler.shouldGenerateOn(recurringExpense, LocalDate.of(2024, 1, 31))) // 31st start
        assertTrue(scheduler.shouldGenerateOn(recurringExpense, LocalDate.of(2024, 2, 29))) // Feb 29th (leap year)
        assertTrue(scheduler.shouldGenerateOn(recurringExpense, LocalDate.of(2024, 4, 30))) // April 30th
    }

    @Test
    fun `should handle yearly frequency correctly`() {
        val recurringExpense = createRecurringExpense(
            frequency = RecurrenceFrequency.YEARLY,
            startDate = LocalDate.of(2024, 6, 15),
            lastGenerated = null
        )

        assertTrue(scheduler.shouldGenerateOn(recurringExpense, LocalDate.of(2024, 6, 15))) // Same year
        assertFalse(scheduler.shouldGenerateOn(recurringExpense, LocalDate.of(2024, 6, 16))) // Different day
        assertTrue(scheduler.shouldGenerateOn(recurringExpense, LocalDate.of(2025, 6, 15))) // Next year
    }

    @Test
    fun `should handle leap year correctly for yearly frequency`() {
        val recurringExpense = createRecurringExpense(
            frequency = RecurrenceFrequency.YEARLY,
            startDate = LocalDate.of(2024, 2, 29), // Leap year
            lastGenerated = null
        )

        assertTrue(scheduler.shouldGenerateOn(recurringExpense, LocalDate.of(2024, 2, 29))) // Leap year
        assertTrue(scheduler.shouldGenerateOn(recurringExpense, LocalDate.of(2025, 2, 28))) // Non-leap year -> Feb 28
        assertTrue(scheduler.shouldGenerateOn(recurringExpense, LocalDate.of(2028, 2, 29))) // Next leap year
    }

    // GetNextOccurrence Tests

    @Test
    fun `should return next daily occurrence`() {
        val recurringExpense = createRecurringExpense(
            frequency = RecurrenceFrequency.DAILY,
            startDate = LocalDate.of(2024, 1, 1)
        )

        val nextOccurrence = scheduler.getNextOccurrence(recurringExpense, LocalDate.of(2024, 1, 5))
        assertEquals(LocalDate.of(2024, 1, 6), nextOccurrence)
    }

    @Test
    fun `should return next weekly occurrence`() {
        val recurringExpense = createRecurringExpense(
            frequency = RecurrenceFrequency.WEEKLY,
            startDate = LocalDate.of(2024, 1, 1) // Monday
        )

        val nextOccurrence = scheduler.getNextOccurrence(recurringExpense, LocalDate.of(2024, 1, 5)) // Friday
        assertEquals(LocalDate.of(2024, 1, 8), nextOccurrence) // Next Monday
    }

    @Test
    fun `should return next monthly occurrence`() {
        val recurringExpense = createRecurringExpense(
            frequency = RecurrenceFrequency.MONTHLY,
            startDate = LocalDate.of(2024, 1, 15)
        )

        val nextOccurrence = scheduler.getNextOccurrence(recurringExpense, LocalDate.of(2024, 1, 20))
        assertEquals(LocalDate.of(2024, 2, 15), nextOccurrence)
    }

    @Test
    fun `should return next yearly occurrence`() {
        val recurringExpense = createRecurringExpense(
            frequency = RecurrenceFrequency.YEARLY,
            startDate = LocalDate.of(2024, 6, 15)
        )

        val nextOccurrence = scheduler.getNextOccurrence(recurringExpense, LocalDate.of(2024, 8, 1))
        assertEquals(LocalDate.of(2025, 6, 15), nextOccurrence)
    }

    @Test
    fun `should return null when no next occurrence due to end date`() {
        val recurringExpense = createRecurringExpense(
            frequency = RecurrenceFrequency.DAILY,
            startDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 1, 5)
        )

        val nextOccurrence = scheduler.getNextOccurrence(recurringExpense, LocalDate.of(2024, 1, 5))
        assertNull(nextOccurrence)
    }

    @Test
    fun `should return null when recurring expense is inactive`() {
        val recurringExpense = createRecurringExpense(
            frequency = RecurrenceFrequency.DAILY,
            startDate = LocalDate.of(2024, 1, 1),
            isActive = false
        )

        val nextOccurrence = scheduler.getNextOccurrence(recurringExpense, LocalDate.of(2024, 1, 1))
        assertNull(nextOccurrence)
    }

    // Edge Cases and Validation Tests

    @Test
    fun `should handle year transitions correctly`() {
        val recurringExpense = createRecurringExpense(
            frequency = RecurrenceFrequency.DAILY,
            startDate = LocalDate.of(2023, 12, 30)
        )

        val occurrences = scheduler.calculateNextOccurrences(
            recurring = recurringExpense,
            from = LocalDate.of(2023, 12, 30),
            until = LocalDate.of(2024, 1, 2)
        )

        assertEquals(4, occurrences.size)
        assertEquals(LocalDate.of(2023, 12, 30), occurrences[0])
        assertEquals(LocalDate.of(2023, 12, 31), occurrences[1])
        assertEquals(LocalDate.of(2024, 1, 1), occurrences[2])
        assertEquals(LocalDate.of(2024, 1, 2), occurrences[3])
    }

    @Test
    fun `should limit maximum occurrences to prevent memory issues`() {
        val recurringExpense = createRecurringExpense(
            frequency = RecurrenceFrequency.DAILY,
            startDate = LocalDate.of(2024, 1, 1)
        )

        val occurrences = scheduler.calculateNextOccurrences(
            recurring = recurringExpense,
            from = LocalDate.of(2024, 1, 1),
            until = LocalDate.of(2030, 1, 1) // ~6 years = ~2190 days
        )

        // Should limit to reasonable number to prevent memory issues
        assertTrue("Should limit occurrences to prevent memory issues", occurrences.size <= 1000)
    }

    // Helper function to create test recurring expenses
    private fun createRecurringExpense(
        frequency: RecurrenceFrequency,
        startDate: LocalDate,
        endDate: LocalDate? = null,
        lastGenerated: LocalDate? = null,
        isActive: Boolean = true
    ): RecurringExpense {
        return RecurringExpense(
            id = 1L,
            amount = BigDecimal("100.00"),
            currency = "USD",
            description = "Test recurring expense",
            categoryId = 1L,
            tags = listOf("test"),
            frequency = frequency,
            startDate = startDate,
            endDate = endDate,
            lastGenerated = lastGenerated,
            isActive = isActive
        )
    }
}