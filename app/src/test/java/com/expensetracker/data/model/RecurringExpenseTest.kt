package com.expensetracker.data.model

import org.junit.Test
import org.junit.Assert.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class RecurringExpenseTest {

    @Test
    fun `create valid recurring expense should succeed`() {
        val startDate = LocalDate.now()
        val endDate = startDate.plusMonths(12)
        
        val recurringExpense = RecurringExpense(
            amount = BigDecimal("1200.00"),
            currency = "USD",
            description = "Monthly rent",
            categoryId = 1L,
            tags = listOf("rent", "housing"),
            frequency = RecurrenceFrequency.MONTHLY,
            startDate = startDate,
            endDate = endDate,
            lastGenerated = null
        )

        assertEquals(BigDecimal("1200.00"), recurringExpense.amount)
        assertEquals("USD", recurringExpense.currency)
        assertEquals("Monthly rent", recurringExpense.description)
        assertEquals(1L, recurringExpense.categoryId)
        assertEquals(listOf("rent", "housing"), recurringExpense.tags)
        assertEquals(RecurrenceFrequency.MONTHLY, recurringExpense.frequency)
        assertEquals(startDate, recurringExpense.startDate)
        assertEquals(endDate, recurringExpense.endDate)
        assertTrue(recurringExpense.isActive)
        assertNull(recurringExpense.lastGenerated)
    }

    @Test
    fun `create recurring expense with zero amount should throw exception`() {
        assertThrows(IllegalArgumentException::class.java) {
            RecurringExpense(
                amount = BigDecimal.ZERO,
                currency = "USD",
                description = "Test",
                categoryId = 1L,
                tags = emptyList(),
                frequency = RecurrenceFrequency.MONTHLY,
                startDate = LocalDate.now(),
                endDate = null,
                lastGenerated = null
            )
        }
    }

    @Test
    fun `create recurring expense with negative amount should throw exception`() {
        assertThrows(IllegalArgumentException::class.java) {
            RecurringExpense(
                amount = BigDecimal("-100.00"),
                currency = "USD",
                description = "Test",
                categoryId = 1L,
                tags = emptyList(),
                frequency = RecurrenceFrequency.MONTHLY,
                startDate = LocalDate.now(),
                endDate = null,
                lastGenerated = null
            )
        }
    }

    @Test
    fun `create recurring expense with blank currency should throw exception`() {
        assertThrows(IllegalArgumentException::class.java) {
            RecurringExpense(
                amount = BigDecimal("100.00"),
                currency = "",
                description = "Test",
                categoryId = 1L,
                tags = emptyList(),
                frequency = RecurrenceFrequency.MONTHLY,
                startDate = LocalDate.now(),
                endDate = null,
                lastGenerated = null
            )
        }
    }

    @Test
    fun `create recurring expense with blank description should throw exception`() {
        assertThrows(IllegalArgumentException::class.java) {
            RecurringExpense(
                amount = BigDecimal("100.00"),
                currency = "USD",
                description = "",
                categoryId = 1L,
                tags = emptyList(),
                frequency = RecurrenceFrequency.MONTHLY,
                startDate = LocalDate.now(),
                endDate = null,
                lastGenerated = null
            )
        }
    }

    @Test
    fun `create recurring expense with invalid category ID should throw exception`() {
        assertThrows(IllegalArgumentException::class.java) {
            RecurringExpense(
                amount = BigDecimal("100.00"),
                currency = "USD",
                description = "Test",
                categoryId = 0L,
                tags = emptyList(),
                frequency = RecurrenceFrequency.MONTHLY,
                startDate = LocalDate.now(),
                endDate = null,
                lastGenerated = null
            )
        }
    }

    @Test
    fun `create recurring expense with end date before start date should throw exception`() {
        val startDate = LocalDate.now()
        val endDate = startDate.minusDays(1)
        
        assertThrows(IllegalArgumentException::class.java) {
            RecurringExpense(
                amount = BigDecimal("100.00"),
                currency = "USD",
                description = "Test",
                categoryId = 1L,
                tags = emptyList(),
                frequency = RecurrenceFrequency.MONTHLY,
                startDate = startDate,
                endDate = endDate,
                lastGenerated = null
            )
        }
    }

    @Test
    fun `create recurring expense with null end date should succeed`() {
        val recurringExpense = RecurringExpense(
            amount = BigDecimal("50.00"),
            currency = "EUR",
            description = "Weekly coffee subscription",
            categoryId = 2L,
            tags = listOf("coffee", "subscription"),
            frequency = RecurrenceFrequency.WEEKLY,
            startDate = LocalDate.now(),
            endDate = null,
            lastGenerated = null
        )

        assertNull(recurringExpense.endDate)
    }

    @Test
    fun `create recurring expense with all frequency types should succeed`() {
        val startDate = LocalDate.now()
        
        val daily = RecurringExpense(
            amount = BigDecimal("5.00"),
            currency = "USD",
            description = "Daily coffee",
            categoryId = 1L,
            tags = emptyList(),
            frequency = RecurrenceFrequency.DAILY,
            startDate = startDate,
            endDate = null,
            lastGenerated = null
        )
        
        val weekly = RecurringExpense(
            amount = BigDecimal("25.00"),
            currency = "USD",
            description = "Weekly groceries",
            categoryId = 1L,
            tags = emptyList(),
            frequency = RecurrenceFrequency.WEEKLY,
            startDate = startDate,
            endDate = null,
            lastGenerated = null
        )
        
        val monthly = RecurringExpense(
            amount = BigDecimal("1000.00"),
            currency = "USD",
            description = "Monthly rent",
            categoryId = 1L,
            tags = emptyList(),
            frequency = RecurrenceFrequency.MONTHLY,
            startDate = startDate,
            endDate = null,
            lastGenerated = null
        )
        
        val yearly = RecurringExpense(
            amount = BigDecimal("500.00"),
            currency = "USD",
            description = "Annual insurance",
            categoryId = 1L,
            tags = emptyList(),
            frequency = RecurrenceFrequency.YEARLY,
            startDate = startDate,
            endDate = null,
            lastGenerated = null
        )

        assertEquals(RecurrenceFrequency.DAILY, daily.frequency)
        assertEquals(RecurrenceFrequency.WEEKLY, weekly.frequency)
        assertEquals(RecurrenceFrequency.MONTHLY, monthly.frequency)
        assertEquals(RecurrenceFrequency.YEARLY, yearly.frequency)
    }

    @Test
    fun `recurring expense should have automatic timestamp`() {
        val beforeCreation = LocalDateTime.now().minusSeconds(1)
        val recurringExpense = RecurringExpense(
            amount = BigDecimal("100.00"),
            currency = "USD",
            description = "Test recurring expense",
            categoryId = 1L,
            tags = emptyList(),
            frequency = RecurrenceFrequency.MONTHLY,
            startDate = LocalDate.now(),
            endDate = null,
            lastGenerated = null
        )
        val afterCreation = LocalDateTime.now().plusSeconds(1)

        assertTrue(recurringExpense.createdAt.isAfter(beforeCreation))
        assertTrue(recurringExpense.createdAt.isBefore(afterCreation))
    }

    @Test
    fun `recurring expense should default to active status`() {
        val recurringExpense = RecurringExpense(
            amount = BigDecimal("100.00"),
            currency = "USD",
            description = "Test",
            categoryId = 1L,
            tags = emptyList(),
            frequency = RecurrenceFrequency.MONTHLY,
            startDate = LocalDate.now(),
            endDate = null,
            lastGenerated = null
        )

        assertTrue(recurringExpense.isActive)
    }

    @Test
    fun `recurring expense can be created as inactive`() {
        val recurringExpense = RecurringExpense(
            amount = BigDecimal("100.00"),
            currency = "USD",
            description = "Test",
            categoryId = 1L,
            tags = emptyList(),
            frequency = RecurrenceFrequency.MONTHLY,
            startDate = LocalDate.now(),
            endDate = null,
            lastGenerated = null,
            isActive = false
        )

        assertFalse(recurringExpense.isActive)
    }

    @Test
    fun `recurring expense with last generated date should succeed`() {
        val startDate = LocalDate.now()
        val lastGenerated = startDate.plusDays(30)
        
        val recurringExpense = RecurringExpense(
            amount = BigDecimal("100.00"),
            currency = "USD",
            description = "Test",
            categoryId = 1L,
            tags = emptyList(),
            frequency = RecurrenceFrequency.MONTHLY,
            startDate = startDate,
            endDate = null,
            lastGenerated = lastGenerated
        )

        assertEquals(lastGenerated, recurringExpense.lastGenerated)
    }
}