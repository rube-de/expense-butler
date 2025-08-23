package com.expensetracker.domain.validation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("DescriptionValidator")
class DescriptionValidatorTest {

    private lateinit var validator: DescriptionValidator

    @BeforeEach
    fun setUp() {
        validator = DescriptionValidator()
    }

    @Nested
    @DisplayName("validate() method")
    inner class ValidateMethod {

        @Test
        fun `should return success for valid description`() {
            val result = validator.validate("Coffee at Starbucks")
            
            assertTrue(result.isValid)
            assertTrue(result is ValidationResult.Success)
        }

        @Test
        fun `should return error for blank description`() {
            val result = validator.validate("")
            
            assertFalse(result.isValid)
            assertTrue(result is ValidationResult.Error)
            assertEquals("Description is required", (result as ValidationResult.Error).message)
        }

        @Test
        fun `should return error for whitespace only description`() {
            val result = validator.validate("   ")
            
            assertFalse(result.isValid)
            assertTrue(result is ValidationResult.Error)
            assertEquals("Description is required", (result as ValidationResult.Error).message)
        }

        @Test
        fun `should return success for description with leading and trailing whitespace`() {
            val result = validator.validate("  Coffee  ")
            
            assertTrue(result.isValid)
            assertTrue(result is ValidationResult.Success)
        }

        @Test
        fun `should return success for single character description`() {
            val result = validator.validate("A")
            
            assertTrue(result.isValid)
            assertTrue(result is ValidationResult.Success)
        }

        @Test
        fun `should return success for description at maximum length`() {
            val maxLengthDescription = "A".repeat(500)
            val result = validator.validate(maxLengthDescription)
            
            assertTrue(result.isValid)
            assertTrue(result is ValidationResult.Success)
        }

        @Test
        fun `should return error for description exceeding maximum length`() {
            val tooLongDescription = "A".repeat(501)
            val result = validator.validate(tooLongDescription)
            
            assertFalse(result.isValid)
            assertTrue(result is ValidationResult.Error)
            assertEquals("Description is too long (max 500 characters)", (result as ValidationResult.Error).message)
        }

        @Test
        fun `should return success for description with special characters`() {
            val result = validator.validate("Coffee & pastry at café #1 - €5.50")
            
            assertTrue(result.isValid)
            assertTrue(result is ValidationResult.Success)
        }

        @Test
        fun `should return success for description with numbers`() {
            val result = validator.validate("Gas station #42 - Receipt 123456")
            
            assertTrue(result.isValid)
            assertTrue(result is ValidationResult.Success)
        }

        @Test
        fun `should return success for description with emoji`() {
            val result = validator.validate("Coffee ☕ at the local café")
            
            assertTrue(result.isValid)
            assertTrue(result is ValidationResult.Success)
        }

        @Test
        fun `should return success for description with line breaks`() {
            val result = validator.validate("First line\nSecond line")
            
            assertTrue(result.isValid)
            assertTrue(result is ValidationResult.Success)
        }
    }

    @Nested
    @DisplayName("filterDescriptionInput() method")
    inner class FilterDescriptionInputMethod {

        @Test
        fun `should replace multiple spaces with single space`() {
            val result = validator.filterDescriptionInput("Coffee    at     Starbucks")
            
            assertEquals("Coffee at Starbucks", result)
        }

        @Test
        fun `should replace tabs with single space`() {
            val result = validator.filterDescriptionInput("Coffee\t\tat\tStarbucks")
            
            assertEquals("Coffee at Starbucks", result)
        }

        @Test
        fun `should replace newlines with single space`() {
            val result = validator.filterDescriptionInput("Coffee\nat\nStarbucks")
            
            assertEquals("Coffee at Starbucks", result)
        }

        @Test
        fun `should handle mixed whitespace characters`() {
            val result = validator.filterDescriptionInput("Coffee  \t\n  at   \t\n  Starbucks")
            
            assertEquals("Coffee at Starbucks", result)
        }

        @Test
        fun `should preserve single spaces between words`() {
            val result = validator.filterDescriptionInput("Coffee at Starbucks")
            
            assertEquals("Coffee at Starbucks", result)
        }

        @Test
        fun `should handle empty string`() {
            val result = validator.filterDescriptionInput("")
            
            assertEquals("", result)
        }

        @Test
        fun `should handle whitespace only string`() {
            val result = validator.filterDescriptionInput("   \t\n   ")
            
            assertEquals(" ", result)
        }

        @Test
        fun `should preserve special characters`() {
            val result = validator.filterDescriptionInput("Coffee  &  pastry  at  café  #1")
            
            assertEquals("Coffee & pastry at café #1", result)
        }
    }
}