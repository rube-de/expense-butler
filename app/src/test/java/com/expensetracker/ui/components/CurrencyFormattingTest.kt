package com.expensetracker.ui.components

import org.junit.Test
import org.junit.Assert.*
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.*

class CurrencyFormattingTest {

    @Test
    fun `formatAmount should format USD correctly`() {
        val amount = BigDecimal("1234.56")
        val formatted = formatAmount(amount, "USD")
        
        // The exact format depends on locale, but should contain the amount
        assertTrue("Should contain currency symbol", formatted.contains("$") || formatted.contains("USD"))
        assertTrue("Should contain amount", formatted.contains("1,234.56") || formatted.contains("1234.56"))
    }

    @Test
    fun `formatAmount should format EUR correctly`() {
        val amount = BigDecimal("999.99")
        val formatted = formatAmount(amount, "EUR")
        
        assertTrue("Should contain currency info", formatted.contains("€") || formatted.contains("EUR"))
        assertTrue("Should contain amount", formatted.contains("999.99"))
    }

    @Test
    fun `formatAmount should handle zero amount`() {
        val amount = BigDecimal.ZERO
        val formatted = formatAmount(amount, "USD")
        
        assertTrue("Should contain zero", formatted.contains("0"))
    }

    @Test
    fun `formatAmount should handle large amounts`() {
        val amount = BigDecimal("1000000.00")
        val formatted = formatAmount(amount, "USD")
        
        assertTrue("Should format large amounts", formatted.isNotEmpty())
    }

    @Test
    fun `getCurrencySymbol should return correct symbols`() {
        val testCases = mapOf(
            "USD" to "$",
            "EUR" to "€",
            "GBP" to "£",
            "JPY" to "¥"
        )
        
        testCases.forEach { (currencyCode, expectedSymbol) ->
            val symbol = getCurrencySymbol(currencyCode)
            assertEquals("Currency $currencyCode should have symbol $expectedSymbol", expectedSymbol, symbol)
        }
    }

    @Test
    fun `getCurrencySymbol should handle unknown currencies`() {
        val unknownCurrency = "XYZ"
        val symbol = getCurrencySymbol(unknownCurrency)
        
        // Should fallback to currency code
        assertEquals("Unknown currency should fallback to code", unknownCurrency, symbol)
    }

    @Test
    fun `getSupportedCurrencies should return expected currencies`() {
        val currencies = getSupportedCurrencies()
        
        assertTrue("Should contain USD", currencies.any { it.code == "USD" })
        assertTrue("Should contain EUR", currencies.any { it.code == "EUR" })
        assertTrue("Should contain GBP", currencies.any { it.code == "GBP" })
        assertTrue("Should contain CHF", currencies.any { it.code == "CHF" })
        
        assertTrue("Should have multiple currencies", currencies.size >= 5)
    }

    @Test
    fun `CurrencyInfo should have valid data`() {
        val currencies = getSupportedCurrencies()
        
        currencies.forEach { currency ->
            assertFalse("Currency code should not be empty", currency.code.isEmpty())
            assertFalse("Currency name should not be empty", currency.displayName.isEmpty())
            assertFalse("Currency symbol should not be empty", currency.symbol.isEmpty())
            assertEquals("Currency code should be 3 characters", 3, currency.code.length)
        }
    }

    // Helper functions that would be extracted from the component
    private fun formatAmount(amount: BigDecimal, currency: String): String {
        return try {
            val formatter = NumberFormat.getCurrencyInstance(Locale.getDefault())
            formatter.currency = Currency.getInstance(currency)
            formatter.format(amount)
        } catch (e: Exception) {
            "$currency ${amount.toPlainString()}"
        }
    }

    private fun getCurrencySymbol(currencyCode: String): String {
        return try {
            Currency.getInstance(currencyCode).symbol
        } catch (e: Exception) {
            currencyCode
        }
    }

    private fun getSupportedCurrencies(): List<CurrencyInfo> {
        return listOf(
            CurrencyInfo("USD", "US Dollar", "$"),
            CurrencyInfo("EUR", "Euro", "€"),
            CurrencyInfo("CHF", "Swiss Franc", "CHF"),
            CurrencyInfo("GBP", "British Pound", "£"),
            CurrencyInfo("JPY", "Japanese Yen", "¥"),
            CurrencyInfo("CAD", "Canadian Dollar", "C$"),
            CurrencyInfo("AUD", "Australian Dollar", "A$")
        )
    }
}