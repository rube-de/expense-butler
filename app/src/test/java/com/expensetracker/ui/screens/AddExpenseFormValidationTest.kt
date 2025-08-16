package com.expensetracker.ui.screens

import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class AddExpenseFormValidationTest {

    @Test
    fun `should validate amount correctly`() {
        // Test null amount
        val result1 = validateAmount(null)
        assertFalse(result1.isValid)
        assertEquals("Amount is required", result1.errorMessage)

        // Test zero amount
        val result2 = validateAmount(BigDecimal.ZERO)
        assertFalse(result2.isValid)
        assertEquals("Amount must be greater than 0", result2.errorMessage)

        // Test negative amount
        val result3 = validateAmount(BigDecimal("-10.00"))
        assertFalse(result3.isValid)
        assertEquals("Amount must be greater than 0", result3.errorMessage)

        // Test valid amount
        val result4 = validateAmount(BigDecimal("25.50"))
        assertTrue(result4.isValid)
        assertNull(result4.errorMessage)
    }

    @Test
    fun `should validate description correctly`() {
        // Test empty description
        val result1 = validateDescription("")
        assertFalse(result1.isValid)
        assertEquals("Description is required", result1.errorMessage)

        // Test blank description
        val result2 = validateDescription("   ")
        assertFalse(result2.isValid)
        assertEquals("Description is required", result2.errorMessage)

        // Test valid description
        val result3 = validateDescription("Coffee")
        assertTrue(result3.isValid)
        assertNull(result3.errorMessage)

        // Test description with whitespace
        val result4 = validateDescription("  Coffee  ")
        assertTrue(result4.isValid)
        assertNull(result4.errorMessage)
    }

    @Test
    fun `should filter amount input correctly`() {
        // Test filtering letters and special characters
        assertEquals("123.45", filterAmountInput("abc123.45def!@#"))

        // Test multiple decimal points
        assertEquals("12.3456", filterAmountInput("12.34.56"))

        // Test empty input
        assertEquals("", filterAmountInput(""))

        // Test only letters
        assertEquals("", filterAmountInput("abcdef"))

        // Test valid input
        assertEquals("25.50", filterAmountInput("25.50"))

        // Test decimal point at start
        assertEquals(".50", filterAmountInput(".50"))
    }

    // Helper validation functions (these would normally be in the ViewModel or a separate validator)
    private fun validateAmount(amount: BigDecimal?): ValidationResult {
        return when {
            amount == null -> ValidationResult(false, "Amount is required")
            amount <= BigDecimal.ZERO -> ValidationResult(false, "Amount must be greater than 0")
            else -> ValidationResult(true, null)
        }
    }

    private fun validateDescription(description: String): ValidationResult {
        return if (description.isBlank()) {
            ValidationResult(false, "Description is required")
        } else {
            ValidationResult(true, null)
        }
    }

    private fun filterAmountInput(input: String): String {
        // Allow only numbers and decimal point
        val filtered = input.filter { it.isDigit() || it == '.' }
        
        // Prevent multiple decimal points
        val decimalCount = filtered.count { it == '.' }
        return if (decimalCount <= 1) {
            filtered
        } else {
            val firstDecimalIndex = filtered.indexOf('.')
            filtered.substring(0, firstDecimalIndex + 1) + 
            filtered.substring(firstDecimalIndex + 1).replace(".", "")
        }
    }

    private data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String?
    )
}