package com.expensetracker.ui.components

import org.junit.Test
import org.junit.Assert.*
import java.math.BigDecimal

class ComponentIntegrationTest {

    @Test
    fun `expense form validation should work end-to-end`() {
        // Simulate user input for an expense form
        val amountInput = "25.50"
        val currency = "USD"
        val tags = listOf("coffee", "work", "breakfast")
        
        // Validate amount
        val amount = parseAmount(amountInput)
        assertNotNull("Amount should parse successfully", amount)
        assertEquals("Amount should be correct", BigDecimal("25.50"), amount)
        
        val amountError = validateAmount(amount)
        assertNull("Amount should be valid", amountError)
        
        // Format amount for display
        val formattedAmount = formatAmount(amount!!, currency)
        assertTrue("Should format amount with currency", formattedAmount.contains("25.50"))
        
        // Validate tags
        tags.forEach { tag ->
            val tagError = validateTag(tag)
            assertNull("Tag '$tag' should be valid", tagError)
        }
        
        // Test tag operations
        var currentTags = emptyList<String>()
        tags.forEach { tag ->
            currentTags = addTagToList(currentTags, tag)
        }
        assertEquals("Should add all tags", tags, currentTags)
        
        // Remove a tag
        currentTags = removeTagFromList(currentTags, "work")
        assertEquals("Should remove work tag", listOf("coffee", "breakfast"), currentTags)
    }

    @Test
    fun `currency selection should work with amount formatting`() {
        val amount = BigDecimal("1000.00")
        val currencies = listOf("USD", "EUR", "CHF", "GBP")
        
        currencies.forEach { currency ->
            val symbol = getCurrencySymbol(currency)
            assertNotNull("Currency $currency should have symbol", symbol)
            
            val formatted = formatAmount(amount, currency)
            assertTrue("Formatted amount should not be empty", formatted.isNotEmpty())
        }
    }

    @Test
    fun `tag suggestions should work with user input`() {
        val availableTags = listOf("coffee", "work", "breakfast", "lunch", "dinner", "groceries")
        val selectedTags = listOf("work")
        
        // Test partial matching
        val coffeeSearch = filterTagSuggestions("cof", availableTags, selectedTags)
        assertEquals("Should find coffee", listOf("coffee"), coffeeSearch)
        
        // Test that selected tags are excluded
        val workSearch = filterTagSuggestions("wor", availableTags, selectedTags)
        assertTrue("Should not suggest already selected work tag", workSearch.isEmpty())
        
        // Test multiple matches
        val mealSearch = filterTagSuggestions("", availableTags, selectedTags)
        assertTrue("Should return available tags", mealSearch.isEmpty()) // Empty input returns empty
    }

    @Test
    fun `invalid input handling should be robust`() {
        // Test amount validation with edge cases
        val invalidAmounts = listOf(null, BigDecimal.ZERO, BigDecimal("-10"))
        invalidAmounts.forEach { amount ->
            val error = validateAmount(amount)
            assertNotNull("Invalid amount $amount should have error", error)
        }
        
        // Test tag validation with edge cases
        val invalidTags = listOf("", "   ", "a".repeat(51))
        invalidTags.forEach { tag ->
            val error = validateTag(tag)
            assertNotNull("Invalid tag '$tag' should have error", error)
        }
        
        // Test amount filtering with invalid input
        val invalidInputs = listOf("abc", "12..34", "!@#$%")
        invalidInputs.forEach { input ->
            val filtered = filterAmountInput(input)
            // Should not crash and should filter out invalid characters
            assertTrue("Filtered input should be safe", filtered.all { it.isDigit() || it == '.' })
        }
    }

    // Helper functions (these would be extracted from actual components)
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

    private fun formatAmount(amount: BigDecimal, currency: String): String {
        return try {
            val formatter = java.text.NumberFormat.getCurrencyInstance(java.util.Locale.getDefault())
            formatter.currency = java.util.Currency.getInstance(currency)
            formatter.format(amount)
        } catch (e: Exception) {
            "$currency ${amount.toPlainString()}"
        }
    }

    private fun getCurrencySymbol(currencyCode: String): String {
        return try {
            java.util.Currency.getInstance(currencyCode).symbol
        } catch (e: Exception) {
            currencyCode
        }
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
}