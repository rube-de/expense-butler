package com.expensetracker.domain.validation

import com.expensetracker.data.model.Category
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@DisplayName("CategoryValidator")
class CategoryValidatorTest {

    private lateinit var validator: CategoryValidator
    private lateinit var validCategory: Category

    @BeforeEach
    fun setUp() {
        validator = CategoryValidator()
        validCategory = Category(
            id = 1L,
            name = "Food",
            color = "#FF5722",
            icon = "restaurant",
            isDefault = true,
            createdAt = LocalDateTime.now()
        )
    }

    @Nested
    @DisplayName("validate() method")
    inner class ValidateMethod {

        @Test
        fun `should return success for valid category`() {
            val result = validator.validate(validCategory)
            
            assertTrue(result.isValid)
            assertTrue(result is ValidationResult.Success)
        }

        @Test
        fun `should return error for null category`() {
            val result = validator.validate(null)
            
            assertFalse(result.isValid)
            assertTrue(result is ValidationResult.Error)
            assertEquals("Please select a category", (result as ValidationResult.Error).message)
        }
    }

    @Nested
    @DisplayName("validateCategoryCreation() method")
    inner class ValidateCategoryCreationMethod {

        @Test
        fun `should return success for valid category creation parameters`() {
            val result = validator.validateCategoryCreation("Food", "#FF5722", "restaurant")
            
            assertTrue(result.isValid)
            assertTrue(result is ValidationResult.Success)
        }

        @Test
        fun `should return error for blank name`() {
            val result = validator.validateCategoryCreation("", "#FF5722", "restaurant")
            
            assertFalse(result.isValid)
            assertTrue(result is ValidationResult.Error)
            assertEquals("Category name is required", (result as ValidationResult.Error).message)
        }

        @Test
        fun `should return error for whitespace only name`() {
            val result = validator.validateCategoryCreation("   ", "#FF5722", "restaurant")
            
            assertFalse(result.isValid)
            assertTrue(result is ValidationResult.Error)
            assertEquals("Category name is required", (result as ValidationResult.Error).message)
        }

        @Test
        fun `should return error for name too short`() {
            val result = validator.validateCategoryCreation("A", "#FF5722", "restaurant")
            
            assertFalse(result.isValid)
            assertTrue(result is ValidationResult.Error)
            assertEquals("Category name must be at least 2 characters", (result as ValidationResult.Error).message)
        }

        @Test
        fun `should return error for name too long`() {
            val longName = "A".repeat(51)
            val result = validator.validateCategoryCreation(longName, "#FF5722", "restaurant")
            
            assertFalse(result.isValid)
            assertTrue(result is ValidationResult.Error)
            assertEquals("Category name is too long (max 50 characters)", (result as ValidationResult.Error).message)
        }

        @Test
        fun `should return success for name at minimum length`() {
            val result = validator.validateCategoryCreation("AB", "#FF5722", "restaurant")
            
            assertTrue(result.isValid)
            assertTrue(result is ValidationResult.Success)
        }

        @Test
        fun `should return success for name at maximum length`() {
            val maxName = "A".repeat(50)
            val result = validator.validateCategoryCreation(maxName, "#FF5722", "restaurant")
            
            assertTrue(result.isValid)
            assertTrue(result is ValidationResult.Success)
        }

        @Test
        fun `should return error for invalid color format - no hash`() {
            val result = validator.validateCategoryCreation("Food", "FF5722", "restaurant")
            
            assertFalse(result.isValid)
            assertTrue(result is ValidationResult.Error)
            assertEquals("Invalid color format (must be hex color)", (result as ValidationResult.Error).message)
        }

        @Test
        fun `should return error for invalid color format - too short`() {
            val result = validator.validateCategoryCreation("Food", "#FF57", "restaurant")
            
            assertFalse(result.isValid)
            assertTrue(result is ValidationResult.Error)
            assertEquals("Invalid color format (must be hex color)", (result as ValidationResult.Error).message)
        }

        @Test
        fun `should return error for invalid color format - too long`() {
            val result = validator.validateCategoryCreation("Food", "#FF572233", "restaurant")
            
            assertFalse(result.isValid)
            assertTrue(result is ValidationResult.Error)
            assertEquals("Invalid color format (must be hex color)", (result as ValidationResult.Error).message)
        }

        @Test
        fun `should return error for invalid color format - invalid characters`() {
            val result = validator.validateCategoryCreation("Food", "#GG5722", "restaurant")
            
            assertFalse(result.isValid)
            assertTrue(result is ValidationResult.Error)
            assertEquals("Invalid color format (must be hex color)", (result as ValidationResult.Error).message)
        }

        @Test
        fun `should return success for lowercase hex color`() {
            val result = validator.validateCategoryCreation("Food", "#ff5722", "restaurant")
            
            assertTrue(result.isValid)
            assertTrue(result is ValidationResult.Success)
        }

        @Test
        fun `should return success for uppercase hex color`() {
            val result = validator.validateCategoryCreation("Food", "#FF5722", "restaurant")
            
            assertTrue(result.isValid)
            assertTrue(result is ValidationResult.Success)
        }

        @Test
        fun `should return success for mixed case hex color`() {
            val result = validator.validateCategoryCreation("Food", "#Ff5722", "restaurant")
            
            assertTrue(result.isValid)
            assertTrue(result is ValidationResult.Success)
        }

        @Test
        fun `should return error for blank icon`() {
            val result = validator.validateCategoryCreation("Food", "#FF5722", "")
            
            assertFalse(result.isValid)
            assertTrue(result is ValidationResult.Error)
            assertEquals("Category icon is required", (result as ValidationResult.Error).message)
        }

        @Test
        fun `should return error for whitespace only icon`() {
            val result = validator.validateCategoryCreation("Food", "#FF5722", "   ")
            
            assertFalse(result.isValid)
            assertTrue(result is ValidationResult.Error)
            assertEquals("Category icon is required", (result as ValidationResult.Error).message)
        }

        @Test
        fun `should return success for common icon names`() {
            val icons = listOf("restaurant", "shopping_cart", "local_gas_station", "movie", "flight")
            
            icons.forEach { icon ->
                val result = validator.validateCategoryCreation("Test", "#FF5722", icon)
                assertTrue(result.isValid, "Icon '$icon' should be valid")
            }
        }
    }

    @Nested
    @DisplayName("validateCategoryName() method")
    inner class ValidateCategoryNameMethod {

        @Test
        fun `should return success for valid category name`() {
            val result = validator.validateCategoryName("Food & Drinks")
            
            assertTrue(result.isValid)
            assertTrue(result is ValidationResult.Success)
        }

        @Test
        fun `should return error for blank name`() {
            val result = validator.validateCategoryName("")
            
            assertFalse(result.isValid)
            assertTrue(result is ValidationResult.Error)
            assertEquals("Category name is required", (result as ValidationResult.Error).message)
        }

        @Test
        fun `should return error for whitespace only name`() {
            val result = validator.validateCategoryName("   ")
            
            assertFalse(result.isValid)
            assertTrue(result is ValidationResult.Error)
            assertEquals("Category name is required", (result as ValidationResult.Error).message)
        }

        @Test
        fun `should return error for name too short`() {
            val result = validator.validateCategoryName("A")
            
            assertFalse(result.isValid)
            assertTrue(result is ValidationResult.Error)
            assertEquals("Category name must be at least 2 characters", (result as ValidationResult.Error).message)
        }

        @Test
        fun `should return error for name too long`() {
            val longName = "A".repeat(51)
            val result = validator.validateCategoryName(longName)
            
            assertFalse(result.isValid)
            assertTrue(result is ValidationResult.Error)
            assertEquals("Category name is too long (max 50 characters)", (result as ValidationResult.Error).message)
        }

        @Test
        fun `should return error for name with leading spaces`() {
            val result = validator.validateCategoryName("  Food")
            
            assertFalse(result.isValid)
            assertTrue(result is ValidationResult.Error)
            assertEquals("Category name cannot start or end with spaces", (result as ValidationResult.Error).message)
        }

        @Test
        fun `should return error for name with trailing spaces`() {
            val result = validator.validateCategoryName("Food  ")
            
            assertFalse(result.isValid)
            assertTrue(result is ValidationResult.Error)
            assertEquals("Category name cannot start or end with spaces", (result as ValidationResult.Error).message)
        }

        @Test
        fun `should return error for name with leading and trailing spaces`() {
            val result = validator.validateCategoryName("  Food  ")
            
            assertFalse(result.isValid)
            assertTrue(result is ValidationResult.Error)
            assertEquals("Category name cannot start or end with spaces", (result as ValidationResult.Error).message)
        }

        @Test
        fun `should return success for name with internal spaces`() {
            val result = validator.validateCategoryName("Food and Drinks")
            
            assertTrue(result.isValid)
            assertTrue(result is ValidationResult.Success)
        }

        @Test
        fun `should return success for name with special characters`() {
            val result = validator.validateCategoryName("R&D Expenses")
            
            assertTrue(result.isValid)
            assertTrue(result is ValidationResult.Success)
        }

        @Test
        fun `should return success for name with numbers`() {
            val result = validator.validateCategoryName("2024 Travel")
            
            assertTrue(result.isValid)
            assertTrue(result is ValidationResult.Success)
        }
    }
}