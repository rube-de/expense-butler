package com.expensetracker.ui.components

import org.junit.Test
import org.junit.Assert.*

class TagValidationTest {

    @Test
    fun `filterTagSuggestions should return matching tags`() {
        val availableTags = listOf("coffee", "work", "breakfast", "lunch", "groceries")
        val selectedTags = listOf("work")
        
        val suggestions = filterTagSuggestions("cof", availableTags, selectedTags)
        
        assertEquals("Should find coffee", listOf("coffee"), suggestions)
    }

    @Test
    fun `filterTagSuggestions should exclude already selected tags`() {
        val availableTags = listOf("coffee", "work", "breakfast")
        val selectedTags = listOf("coffee", "work")
        
        val suggestions = filterTagSuggestions("b", availableTags, selectedTags)
        
        assertEquals("Should only return unselected tags", listOf("breakfast"), suggestions)
    }

    @Test
    fun `filterTagSuggestions should be case insensitive`() {
        val availableTags = listOf("Coffee", "WORK", "breakfast")
        val selectedTags = emptyList<String>()
        
        val suggestions = filterTagSuggestions("cof", availableTags, selectedTags)
        
        assertEquals("Should find Coffee case-insensitively", listOf("Coffee"), suggestions)
    }

    @Test
    fun `filterTagSuggestions should limit results`() {
        val availableTags = (1..10).map { "tag$it" }
        val selectedTags = emptyList<String>()
        
        val suggestions = filterTagSuggestions("tag", availableTags, selectedTags, maxResults = 5)
        
        assertEquals("Should limit to 5 results", 5, suggestions.size)
    }

    @Test
    fun `filterTagSuggestions should return empty for blank input`() {
        val availableTags = listOf("coffee", "work", "breakfast")
        val selectedTags = emptyList<String>()
        
        val suggestions = filterTagSuggestions("", availableTags, selectedTags)
        
        assertTrue("Should return empty for blank input", suggestions.isEmpty())
    }

    @Test
    fun `validateTag should reject empty tags`() {
        val error = validateTag("")
        assertNotNull("Empty tag should have error", error)
    }

    @Test
    fun `validateTag should reject whitespace-only tags`() {
        val error = validateTag("   ")
        assertNotNull("Whitespace-only tag should have error", error)
    }

    @Test
    fun `validateTag should accept valid tags`() {
        val validTags = listOf("coffee", "work-related", "travel_2024", "café")
        
        validTags.forEach { tag ->
            val error = validateTag(tag)
            assertNull("Tag '$tag' should be valid", error)
        }
    }

    @Test
    fun `validateTag should reject tags that are too long`() {
        val longTag = "a".repeat(51) // Assuming 50 char limit
        val error = validateTag(longTag)
        assertNotNull("Long tag should have error", error)
    }

    @Test
    fun `addTagToList should add new tag`() {
        val currentTags = listOf("coffee", "work")
        val newTag = "breakfast"
        
        val updatedTags = addTagToList(currentTags, newTag)
        
        assertEquals("Should add new tag", listOf("coffee", "work", "breakfast"), updatedTags)
    }

    @Test
    fun `addTagToList should not add duplicate tag`() {
        val currentTags = listOf("coffee", "work")
        val duplicateTag = "coffee"
        
        val updatedTags = addTagToList(currentTags, duplicateTag)
        
        assertEquals("Should not add duplicate", currentTags, updatedTags)
    }

    @Test
    fun `removeTagFromList should remove existing tag`() {
        val currentTags = listOf("coffee", "work", "breakfast")
        val tagToRemove = "work"
        
        val updatedTags = removeTagFromList(currentTags, tagToRemove)
        
        assertEquals("Should remove tag", listOf("coffee", "breakfast"), updatedTags)
    }

    @Test
    fun `removeTagFromList should handle non-existent tag`() {
        val currentTags = listOf("coffee", "work")
        val nonExistentTag = "lunch"
        
        val updatedTags = removeTagFromList(currentTags, nonExistentTag)
        
        assertEquals("Should not change list", currentTags, updatedTags)
    }

    // Helper functions that would be extracted from the component
    private fun filterTagSuggestions(
        input: String,
        availableTags: List<String>,
        selectedTags: List<String>,
        maxResults: Int = 5
    ): List<String> {
        if (input.isBlank()) return emptyList()
        
        return availableTags
            .filter { tag ->
                tag.contains(input, ignoreCase = true) && !selectedTags.contains(tag)
            }
            .take(maxResults)
    }

    private fun validateTag(tag: String): String? {
        return when {
            tag.isBlank() -> "Tag cannot be empty"
            tag.length > 50 -> "Tag is too long"
            else -> null
        }
    }

    private fun addTagToList(currentTags: List<String>, newTag: String): List<String> {
        return if (currentTags.contains(newTag)) {
            currentTags
        } else {
            currentTags + newTag
        }
    }

    private fun removeTagFromList(currentTags: List<String>, tagToRemove: String): List<String> {
        return currentTags - tagToRemove
    }
}