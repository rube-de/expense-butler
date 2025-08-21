package com.expensetracker.domain.validation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@DisplayName("AmountValidator")
class AmountValidatorTest {

    private lateinit var validator: AmountValidator

    @BeforeEach
    fun setUp() {
        validator = AmountValidator()
    }

    @Nested
    @DisplayName("validate() method")
    inner class ValidateMethod {

        @Test
        fun `should return success for valid positive amount`() {
            val result = validator.validate(BigDecimal("25.50"))
            
            assertTrue(result.isValid)
            assertTrue(result is ValidationResult.Success)
        }

        @Test
        fun `should return error for null amount`() {
            val result = validator.validate(null)
            
            assertFalse(result.isValid)
            assertTrue(result is ValidationResult.Error)
            assertEquals("Amount is required", (result as ValidationResult.Error).message)
        }

        @Test
        fun `should return error for zero amount`() {
            val result = validator.validate(BigDecimal.ZERO)
            
            assertFalse(result.isValid)
            assertTrue(result is ValidationResult.Error)
            assertEquals("Amount must be greater than 0", (result as ValidationResult.Error).message)
        }

        @Test
        fun `should return error for negative amount`() {
            val result = validator.validate(BigDecimal("-10.00"))
            
            assertFalse(result.isValid)
            assertTrue(result is ValidationResult.Error)
            assertEquals("Amount must be greater than 0", (result as ValidationResult.Error).message)
        }

        @Test
        fun `should return error for amount exceeding maximum`() {
            val result = validator.validate(BigDecimal("1000000000.00"))
            
            assertFalse(result.isValid)
            assertTrue(result is ValidationResult.Error)
            assertEquals("Amount is too large", (result as ValidationResult.Error).message)
        }

        @Test
        fun `should return success for maximum allowed amount`() {
            val result = validator.validate(BigDecimal("999999999.99"))
            
            assertTrue(result.isValid)
            assertTrue(result is ValidationResult.Success)
        }

        @Test
        fun `should return success for minimum valid amount`() {
            val result = validator.validate(BigDecimal("0.01"))
            
            assertTrue(result.isValid)
            assertTrue(result is ValidationResult.Success)
        }
    }

    @Nested
    @DisplayName("validateAmountString() method")
    inner class ValidateAmountStringMethod {

        @Test
        fun `should return success for valid amount string`() {
            val result = validator.validateAmountString("25.50")
            
            assertTrue(result.isValid)
            assertTrue(result is ValidationResult.Success)
        }

        @Test
        fun `should return error for blank string`() {
            val result = validator.validateAmountString("")
            
            assertFalse(result.isValid)
            assertTrue(result is ValidationResult.Error)
            assertEquals("Amount is required", (result as ValidationResult.Error).message)
        }

        @Test
        fun `should return error for whitespace only string`() {
            val result = validator.validateAmountString("   ")
            
            assertFalse(result.isValid)
            assertTrue(result is ValidationResult.Error)
            assertEquals("Amount is required", (result as ValidationResult.Error).message)
        }

        @Test
        fun `should return error for invalid number format`() {
            val result = validator.validateAmountString("abc")
            
            assertFalse(result.isValid)
            assertTrue(result is ValidationResult.Error)
            assertEquals("Invalid amount format", (result as ValidationResult.Error).message)
        }

        @Test
        fun `should return error for string with multiple decimal points`() {
            val result = validator.validateAmountString("25.50.00")
            
            assertFalse(result.isValid)
            assertTrue(result is ValidationResult.Error)
            assertEquals("Invalid amount format", (result as ValidationResult.Error).message)
        }

        @Test
        fun `should return success for integer string`() {
            val result = validator.validateAmountString("100")
            
            assertTrue(result.isValid)
            assertTrue(result is ValidationResult.Success)
        }

        @Test
        fun `should return success for decimal string`() {
            val result = validator.validateAmountString("100.99")
            
            assertTrue(result.isValid)
            assertTrue(result is ValidationResult.Success)
        }
    }

    @Nested
    @DisplayName("filterAmountInput() method")
    inner class FilterAmountInputMethod {

        @Test
        fun `should filter out non-numeric characters except decimal point`() {
            val result = validator.filterAmountInput("abc123.45def")
            
            assertEquals("123.45", result)
        }

        @Test
        fun `should allow only one decimal point`() {
            val result = validator.filterAmountInput("12.34.56")
            
            assertEquals("12.3456", result)
        }

        @Test
        fun `should preserve first decimal point and remove subsequent ones`() {
            val result = validator.filterAmountInput("12.34.56.78")
            
            assertEquals("12.345678", result)
        }

        @Test
        fun `should return empty string for non-numeric input`() {
            val result = validator.filterAmountInput("abc")
            
            assertEquals("", result)
        }

        @Test
        fun `should preserve numeric input with single decimal`() {
            val result = validator.filterAmountInput("123.45")
            
            assertEquals("123.45", result)
        }

        @Test
        fun `should preserve integer input`() {
            val result = validator.filterAmountInput("12345")
            
            assertEquals("12345", result)
        }

        @Test
        fun `should handle decimal point at start`() {
            val result = validator.filterAmountInput(".50")
            
            assertEquals(".50", result)
        }

        @Test
        fun `should handle decimal point at end`() {
            val result = validator.filterAmountInput("50.")
            
            assertEquals("50.", result)
        }

        @Test
        fun `should handle only decimal point`() {
            val result = validator.filterAmountInput(".")
            
            assertEquals(".", result)
        }

        @Test
        fun `should handle mixed input with spaces and symbols`() {
            val result = validator.filterAmountInput("$ 1,234.56 USD")
            
            assertEquals("1234.56", result)
        }
    }
}