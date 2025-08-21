package com.expensetracker.domain.validation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("TagValidator")
class TagValidatorTest {

    private lateinit var validator: TagValidator

    @BeforeEach
    fun setUp() {
        validator = TagValidator()
    }

    @Nested
    @DisplayName("validate() method")
    inner class ValidateMethod {

        @Test
        fun `should return success for valid tag`() {
            val result = validator.validate("coffee")
            
            assertTrue(result.isValid)
            assertTrue(result is ValidationResult.Success)
        }

        @Test
        fun `should return error for blank tag`() {
            val result = validator.validate("")
            
            assertFalse(result.isValid)
            assertTrue(result is ValidationResult.Error)
            assertEquals("Tag cannot be empty", (result as ValidationResult.Error).message)
        }

        @Test
        fun `should return error for whitespace only tag`() {
            val result = validator.validate("   ")
            
            assertFalse(result.isValid)
            assertTrue(result is ValidationResult.Error)
            assertEquals("Tag cannot be empty", (result as ValidationResult.Error).message)
        }

        @Test
        fun `should return error for tag with spaces`() {
            val result = validator.validate("coffee shop")
            
            assertFalse(result.isValid)
            assertTrue(result is ValidationResult.Error)
            assertEquals("Tag cannot contain spaces, commas, or semicolons", (result as ValidationResult.Error).message)
        }

        @Test
        fun `should return error for tag with comma`() {
            val result = validator.validate("coffee,shop")
            
            assertFalse(result.isValid)
            assertTrue(result is ValidationResult.Error)
            assertEquals("Tag cannot contain spaces, commas, or semicolons", (result as ValidationResult.Error).message)
        }

        @Test
        fun `should return error for tag with semicolon`() {
            val result = validator.validate("coffee;shop")
            
            assertFalse(result.isValid)
            assertTrue(result is ValidationResult.Error)
            assertEquals("Tag cannot contain spaces, commas, or semicolons", (result as ValidationResult.Error).message)
        }

        @Test
        fun `should return error for tag exceeding maximum length`() {
            val longTag = "a".repeat(51)
            val result = validator.validate(longTag)
            
            assertFalse(result.isValid)
            assertTrue(result is ValidationResult.Error)
            assertEquals("Tag is too long (max 50 characters)", (result as ValidationResult.Error).message)
        }

        @Test
        fun `should return success for tag at maximum length`() {
            val maxLengthTag = "a".repeat(50)
            val result = validator.validate(maxLengthTag)
            
            assertTrue(result.isValid)
            assertTrue(result is ValidationResult.Success)
        }

        @Test
        fun `should return success for tag with numbers`() {
            val result = validator.validate("coffee123")
            
            assertTrue(result.isValid)
            assertTrue(result is ValidationResult.Success)
        }

        @Test
        fun `should return success for tag with hyphens`() {
            val result = validator.validate("coffee-shop")
            
            assertTrue(result.isValid)
            assertTrue(result is ValidationResult.Success)
        }

        @Test
        fun `should return success for tag with underscores`() {
            val result = validator.validate("coffee_shop")
            
            assertTrue(result.isValid)
            assertTrue(result is ValidationResult.Success)
        }
    }

    @Nested
    @DisplayName("validateTagList() method")
    inner class ValidateTagListMethod {

        @Test
        fun `should return success for valid tag list`() {
            val tags = listOf("coffee", "work", "morning")
            val result = validator.validateTagList(tags)
            
            assertTrue(result.isValid)
            assertTrue(result is ValidationResult.Success)
        }

        @Test
        fun `should return success for empty tag list`() {
            val result = validator.validateTagList(emptyList())
            
            assertTrue(result.isValid)
            assertTrue(result is ValidationResult.Success)
        }

        @Test
        fun `should return error for too many tags`() {
            val tags = (1..11).map { "tag$it" }
            val result = validator.validateTagList(tags)
            
            assertFalse(result.isValid)
            assertTrue(result is ValidationResult.Error)
            assertEquals("Too many tags (max 10)", (result as ValidationResult.Error).message)
        }

        @Test
        fun `should return success for maximum number of tags`() {
            val tags = (1..10).map { "tag$it" }
            val result = validator.validateTagList(tags)
            
            assertTrue(result.isValid)
            assertTrue(result is ValidationResult.Success)
        }

        @Test
        fun `should return error for list with blank tag`() {
            val tags = listOf("coffee", "", "work")
            val result = validator.validateTagList(tags)
            
            assertFalse(result.isValid)
            assertTrue(result is ValidationResult.Error)
            assertEquals("Tags cannot be empty", (result as ValidationResult.Error).message)
        }

        @Test
        fun `should return error for list with duplicate tags`() {
            val tags = listOf("coffee", "work", "coffee")
            val result = validator.validateTagList(tags)
            
            assertFalse(result.isValid)
            assertTrue(result is ValidationResult.Error)
            assertEquals("Duplicate tags are not allowed", (result as ValidationResult.Error).message)
        }

        @Test
        fun `should return error for invalid tag in list`() {
            val tags = listOf("coffee", "work space", "morning")
            val result = validator.validateTagList(tags)
            
            assertFalse(result.isValid)
            assertTrue(result is ValidationResult.Error)
            assertEquals("Tag cannot contain spaces, commas, or semicolons", (result as ValidationResult.Error).message)
        }
    }

    @Nested
    @DisplayName("filterTagInput() method")
    inner class FilterTagInputMethod {

        @Test
        fun `should remove spaces from input`() {
            val result = validator.filterTagInput("coffee shop")
            
            assertEquals("coffeeshop", result)
        }

        @Test
        fun `should remove commas from input`() {
            val result = validator.filterTagInput("coffee,shop")
            
            assertEquals("coffeeshop", result)
        }

        @Test
        fun `should remove semicolons from input`() {
            val result = validator.filterTagInput("coffee;shop")
            
            assertEquals("coffeeshop", result)
        }

        @Test
        fun `should convert to lowercase`() {
            val result = validator.filterTagInput("COFFEE")
            
            assertEquals("coffee", result)
        }

        @Test
        fun `should handle mixed case with invalid characters`() {
            val result = validator.filterTagInput("Coffee, Shop; Break")
            
            assertEquals("coffeeshopbreak", result)
        }

        @Test
        fun `should preserve valid characters`() {
            val result = validator.filterTagInput("coffee-shop_123")
            
            assertEquals("coffee-shop_123", result)
        }

        @Test
        fun `should handle empty string`() {
            val result = validator.filterTagInput("")
            
            assertEquals("", result)
        }

        @Test
        fun `should handle whitespace only string`() {
            val result = validator.filterTagInput("   ")
            
            assertEquals("", result)
        }
    }

    @Nested
    @DisplayName("addTagToList() method")
    inner class AddTagToListMethod {

        @Test
        fun `should add valid tag to empty list`() {
            val (newTags, result) = validator.addTagToList("coffee", emptyList())
            
            assertTrue(result.isValid)
            assertEquals(listOf("coffee"), newTags)
        }

        @Test
        fun `should add valid tag to existing list`() {
            val currentTags = listOf("work")
            val (newTags, result) = validator.addTagToList("coffee", currentTags)
            
            assertTrue(result.isValid)
            assertEquals(listOf("work", "coffee"), newTags)
        }

        @Test
        fun `should return error for invalid tag`() {
            val currentTags = listOf("work")
            val (newTags, result) = validator.addTagToList("coffee shop", currentTags)
            
            assertFalse(result.isValid)
            assertEquals(currentTags, newTags)
            assertEquals("Tag cannot contain spaces, commas, or semicolons", (result as ValidationResult.Error).message)
        }

        @Test
        fun `should return error for duplicate tag`() {
            val currentTags = listOf("coffee", "work")
            val (newTags, result) = validator.addTagToList("coffee", currentTags)
            
            assertFalse(result.isValid)
            assertEquals(currentTags, newTags)
            assertEquals("Tag 'coffee' already exists", (result as ValidationResult.Error).message)
        }

        @Test
        fun `should return error when adding tag would exceed maximum count`() {
            val currentTags = (1..10).map { "tag$it" }
            val (newTags, result) = validator.addTagToList("newtag", currentTags)
            
            assertFalse(result.isValid)
            assertEquals(currentTags, newTags)
            assertEquals("Too many tags (max 10)", (result as ValidationResult.Error).message)
        }

        @Test
        fun `should handle whitespace in tag input`() {
            val currentTags = listOf("work")
            val (newTags, result) = validator.addTagToList("  coffee  ", currentTags)
            
            assertTrue(result.isValid)
            assertEquals(listOf("work", "coffee"), newTags)
        }

        @Test
        fun `should return error for blank tag after trimming`() {
            val currentTags = listOf("work")
            val (newTags, result) = validator.addTagToList("   ", currentTags)
            
            assertFalse(result.isValid)
            assertEquals(currentTags, newTags)
            assertEquals("Tag cannot be empty", (result as ValidationResult.Error).message)
        }
    }
}