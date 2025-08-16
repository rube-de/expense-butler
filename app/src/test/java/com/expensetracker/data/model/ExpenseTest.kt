package com.expensetracker.data.model

import org.junit.Test
import org.junit.Assert.*
import java.math.BigDecimal
import java.time.LocalDateTime

class ExpenseTest {

    @Test
    fun `create valid expense should succeed`() {
        val expense = Expense(
            amount = BigDecimal("100.50"),
            currency = "USD",
            description = "Lunch at restaurant",
            categoryId = 1L,
            tags = listOf("food", "restaurant"),
            date = LocalDateTime.now()
        )

        assertEquals(BigDecimal("100.50"), expense.amount)
        assertEquals("USD", expense.currency)
        assertEquals("Lunch at restaurant", expense.description)
        assertEquals(1L, expense.categoryId)
        assertEquals(listOf("food", "restaurant"), expense.tags)
        assertTrue(expense.id == 0L) // Auto-generated ID starts at 0
    }

    @Test
    fun `create expense with zero amount should throw exception`() {
        assertThrows(IllegalArgumentException::class.java) {
            Expense(
                amount = BigDecimal.ZERO,
                currency = "USD",
                description = "Test",
                categoryId = 1L,
                tags = emptyList(),
                date = LocalDateTime.now()
            )
        }
    }

    @Test
    fun `create expense with negative amount should throw exception`() {
        assertThrows(IllegalArgumentException::class.java) {
            Expense(
                amount = BigDecimal("-10.00"),
                currency = "USD",
                description = "Test",
                categoryId = 1L,
                tags = emptyList(),
                date = LocalDateTime.now()
            )
        }
    }

    @Test
    fun `create expense with blank currency should throw exception`() {
        assertThrows(IllegalArgumentException::class.java) {
            Expense(
                amount = BigDecimal("100.00"),
                currency = "",
                description = "Test",
                categoryId = 1L,
                tags = emptyList(),
                date = LocalDateTime.now()
            )
        }
    }

    @Test
    fun `create expense with blank description should throw exception`() {
        assertThrows(IllegalArgumentException::class.java) {
            Expense(
                amount = BigDecimal("100.00"),
                currency = "USD",
                description = "",
                categoryId = 1L,
                tags = emptyList(),
                date = LocalDateTime.now()
            )
        }
    }

    @Test
    fun `create expense with invalid category ID should throw exception`() {
        assertThrows(IllegalArgumentException::class.java) {
            Expense(
                amount = BigDecimal("100.00"),
                currency = "USD",
                description = "Test",
                categoryId = 0L,
                tags = emptyList(),
                date = LocalDateTime.now()
            )
        }
    }

    @Test
    fun `create expense with empty tags should succeed`() {
        val expense = Expense(
            amount = BigDecimal("50.00"),
            currency = "EUR",
            description = "Coffee",
            categoryId = 2L,
            tags = emptyList(),
            date = LocalDateTime.now()
        )

        assertTrue(expense.tags.isEmpty())
    }

    @Test
    fun `create expense with multiple tags should succeed`() {
        val tags = listOf("work", "coffee", "morning")
        val expense = Expense(
            amount = BigDecimal("4.50"),
            currency = "CHF",
            description = "Morning coffee",
            categoryId = 2L,
            tags = tags,
            date = LocalDateTime.now()
        )

        assertEquals(tags, expense.tags)
        assertEquals(3, expense.tags.size)
    }

    @Test
    fun `expense should have automatic timestamps`() {
        val beforeCreation = LocalDateTime.now().minusSeconds(1)
        val expense = Expense(
            amount = BigDecimal("25.00"),
            currency = "USD",
            description = "Test expense",
            categoryId = 1L,
            tags = emptyList(),
            date = LocalDateTime.now()
        )
        val afterCreation = LocalDateTime.now().plusSeconds(1)

        assertTrue(expense.createdAt.isAfter(beforeCreation))
        assertTrue(expense.createdAt.isBefore(afterCreation))
        assertTrue(expense.updatedAt.isAfter(beforeCreation))
        assertTrue(expense.updatedAt.isBefore(afterCreation))
    }
}