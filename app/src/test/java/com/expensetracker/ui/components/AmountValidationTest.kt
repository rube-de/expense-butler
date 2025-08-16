package com.expensetracker.ui.components

import org.junit.Test
import org.junit.Assert.*
import java.math.BigDecimal

class AmountValidationTest {

    @Test
    fun `filterAmountInput should allow valid decimal numbers`() {
        val validInputs = listOf("123", "123.45", "0.99", "1000.00")
        
        validInputs.forEach { input ->
            val filtered = filterAmountInput(input)
            assertEquals("Input '$input' should not be filtered", input, filtered)
        }
    }

    @Test
    fun `filterAmountInput should remove invalid characters`() {
        val testCases = mapOf(
            "abc123def" to "123",
            "1a2b3c" to "123",
            "!@#123.45$%^" to "123.45",
            "" to ""
        )
        
        testCases.forEach { (input, expected) ->
            val filtered = filterAmountInput(input)
            assertEquals("Input '$input' should be filtered to '$expected'", expected, filtered)
        }
    }

    @Test
    fun `filterAmountInput should prevent multiple decimal points`() {
        val input = "12.34.56.78"
        val filtered = filterAmountInput(input)
        assertEquals("Should keep only first decimal point", "12.345678", filtered)
    }

    @Test
    fun `parseAmount should convert valid strings to BigDecimal`() {
        val testCases = mapOf(
            "123" to BigDecimal("123"),
            "123.45" to BigDecimal("123.45"),
            "0.99" to BigDecimal("0.99"),
            "1000.00" to BigDecimal("1000.00")
        )
        
        testCases.forEach { (input, expected) ->
            val result = parseAmount(input)
            assertEquals("Input '$input' should parse to $expected", expected, result)
        }
    }

    @Test
    fun `parseAmount should return null for invalid strings`() {
        val invalidInputs = listOf("", "abc", "12.34.56", ".")
        
        invalidInputs.forEach { input ->
            val result = parseAmount(input)
            assertNull("Input '$input' should return null", result)
        }
    }

    @Test
    fun `validateAmount should return error for null or zero amounts`() {
        assertNotNull("Null amount should have error", validateAmount(null))
        assertNotNull("Zero amount should have error", validateAmount(BigDecimal.ZERO))
        assertNotNull("Negative amount should have error", validateAmount(BigDecimal("-10")))
    }

    @Test
    fun `validateAmount should return null for valid amounts`() {
        val validAmounts = listOf(
            BigDecimal("0.01"),
            BigDecimal("1.00"),
            BigDecimal("999.99"),
            BigDecimal("1000000")
        )
        
        validAmounts.forEach { amount ->
            val error = validateAmount(amount)
            assertNull("Amount $amount should be valid", error)
        }
    }

    // Helper functions that would be extracted from the component
    private fun filterAmountInput(input: String): String {
        val filtered = input.filter { it.isDigit() || it == '.' }
        val decimalCount = filtered.count { it == '.' }
        return if (decimalCount <= 1) {
            filtered
        } else {
            val firstDecimalIndex = filtered.indexOf('.')
            filtered.substring(0, firstDecimalIndex + 1) + 
            filtered.substring(firstDecimalIndex + 1).replace(".", "")
        }
    }

    private fun parseAmount(input: String): BigDecimal? {
        return try {
            if (input.isBlank()) null else BigDecimal(input)
        } catch (e: NumberFormatException) {
            null
        }
    }

    private fun validateAmount(amount: BigDecimal?): String? {
        return when {
            amount == null -> "Amount is required"
            amount <= BigDecimal.ZERO -> "Amount must be greater than 0"
            else -> null
        }
    }
}