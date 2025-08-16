package com.expensetracker.ui.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.expensetracker.ui.theme.ExpenseTrackerTheme
import org.junit.Rule
import org.junit.Test

class CurrencySelectorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun currencySelector_displaysSelectedCurrency() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                CurrencySelector(
                    selectedCurrency = "USD",
                    onCurrencySelected = { }
                )
            }
        }

        // Verify selected currency is displayed
        composeTestRule
            .onNodeWithText("$ USD")
            .assertIsDisplayed()

        // Verify label is displayed
        composeTestRule
            .onNodeWithText("Currency")
            .assertIsDisplayed()
    }

    @Test
    fun currencySelector_displaysDropdownIcon() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                CurrencySelector(
                    selectedCurrency = "EUR",
                    onCurrencySelected = { }
                )
            }
        }

        // Verify dropdown icon is present (by content description)
        composeTestRule
            .onNodeWithContentDescription("Select currency")
            .assertIsDisplayed()
    }

    @Test
    fun currencySelector_opensDropdownOnClick() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                CurrencySelector(
                    selectedCurrency = "USD",
                    onCurrencySelected = { }
                )
            }
        }

        // Click on the selector to open dropdown
        composeTestRule
            .onNodeWithText("$ USD")
            .performClick()

        // Verify dropdown options are displayed
        composeTestRule
            .onNodeWithText("US Dollar")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Euro")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Swiss Franc")
            .assertIsDisplayed()
    }

    @Test
    fun currencySelector_selectsCurrencyFromDropdown() {
        var selectedCurrency = "USD"

        composeTestRule.setContent {
            ExpenseTrackerTheme {
                CurrencySelector(
                    selectedCurrency = selectedCurrency,
                    onCurrencySelected = { selectedCurrency = it }
                )
            }
        }

        // Open dropdown
        composeTestRule
            .onNodeWithText("$ USD")
            .performClick()

        // Select EUR
        composeTestRule
            .onNodeWithText("Euro")
            .performClick()

        // Verify callback was triggered
        assert(selectedCurrency == "EUR")
    }

    @Test
    fun currencySelector_displaysAllSupportedCurrencies() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                CurrencySelector(
                    selectedCurrency = "USD",
                    onCurrencySelected = { }
                )
            }
        }

        // Open dropdown
        composeTestRule
            .onNodeWithText("$ USD")
            .performClick()

        // Verify major currencies are available
        val expectedCurrencies = listOf(
            "US Dollar", "Euro", "Swiss Franc", "British Pound",
            "Japanese Yen", "Canadian Dollar", "Australian Dollar"
        )

        expectedCurrencies.forEach { currencyName ->
            composeTestRule
                .onNodeWithText(currencyName)
                .assertIsDisplayed()
        }
    }

    @Test
    fun currencySelector_displaysCorrectSymbolsAndCodes() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                CurrencySelector(
                    selectedCurrency = "USD",
                    onCurrencySelected = { }
                )
            }
        }

        // Open dropdown
        composeTestRule
            .onNodeWithText("$ USD")
            .performClick()

        // Verify symbols and codes are displayed correctly
        composeTestRule
            .onNodeWithText("$")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("€")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("£")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("¥")
            .assertIsDisplayed()
    }

    @Test
    fun currencySelector_handlesUnknownCurrency() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                CurrencySelector(
                    selectedCurrency = "XYZ", // Unknown currency
                    onCurrencySelected = { }
                )
            }
        }

        // Should fallback to first supported currency (USD)
        composeTestRule
            .onNodeWithText("$ USD")
            .assertIsDisplayed()
    }

    @Test
    fun currencySelector_closesDropdownAfterSelection() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                CurrencySelector(
                    selectedCurrency = "USD",
                    onCurrencySelected = { }
                )
            }
        }

        // Open dropdown
        composeTestRule
            .onNodeWithText("$ USD")
            .performClick()

        // Select a currency
        composeTestRule
            .onNodeWithText("Euro")
            .performClick()

        // Verify dropdown is closed (Euro option should not be visible anymore)
        composeTestRule
            .onNodeWithText("Euro")
            .assertDoesNotExist()
    }
}