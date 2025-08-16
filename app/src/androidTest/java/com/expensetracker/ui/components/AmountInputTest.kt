package com.expensetracker.ui.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.expensetracker.ui.theme.ExpenseTrackerTheme
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal

class AmountInputTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun amountInput_displaysInitialAmount() {
        val initialAmount = BigDecimal("25.50")

        composeTestRule.setContent {
            ExpenseTrackerTheme {
                AmountInput(
                    amount = initialAmount,
                    currency = "USD",
                    onAmountChanged = { }
                )
            }
        }

        // Verify amount is displayed
        composeTestRule
            .onNodeWithText("25.50")
            .assertIsDisplayed()

        // Verify currency symbol is displayed
        composeTestRule
            .onNodeWithText("$")
            .assertIsDisplayed()

        // Verify label is displayed
        composeTestRule
            .onNodeWithText("Amount")
            .assertIsDisplayed()
    }

    @Test
    fun amountInput_handlesTextInput() {
        var updatedAmount: BigDecimal? = null

        composeTestRule.setContent {
            ExpenseTrackerTheme {
                AmountInput(
                    amount = null,
                    currency = "USD",
                    onAmountChanged = { updatedAmount = it }
                )
            }
        }

        // Type amount
        composeTestRule
            .onNodeWithText("Amount")
            .performTextInput("42.75")

        // Verify text was entered
        composeTestRule
            .onNodeWithText("42.75")
            .assertIsDisplayed()
    }

    @Test
    fun amountInput_filtersInvalidCharacters() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                AmountInput(
                    amount = null,
                    currency = "USD",
                    onAmountChanged = { }
                )
            }
        }

        // Try to type invalid characters
        composeTestRule
            .onNodeWithText("Amount")
            .performTextInput("abc123.45def")

        // Verify only valid characters remain
        composeTestRule
            .onNodeWithText("123.45")
            .assertIsDisplayed()
    }

    @Test
    fun amountInput_preventsMultipleDecimalPoints() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                AmountInput(
                    amount = null,
                    currency = "USD",
                    onAmountChanged = { }
                )
            }
        }

        // Try to type multiple decimal points
        composeTestRule
            .onNodeWithText("Amount")
            .performTextInput("12.34.56")

        // Verify only first decimal point is kept
        composeTestRule
            .onNodeWithText("12.34")
            .assertIsDisplayed()
    }

    @Test
    fun amountInput_displaysErrorState() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                AmountInput(
                    amount = null,
                    currency = "USD",
                    onAmountChanged = { },
                    isError = true,
                    errorMessage = "Amount is required"
                )
            }
        }

        // Verify error message is displayed
        composeTestRule
            .onNodeWithText("Amount is required")
            .assertIsDisplayed()
    }

    @Test
    fun amountInput_showsFormattedPreview() {
        val amount = BigDecimal("1234.56")

        composeTestRule.setContent {
            ExpenseTrackerTheme {
                AmountInput(
                    amount = amount,
                    currency = "USD",
                    onAmountChanged = { }
                )
            }
        }

        // Verify formatted preview is shown
        composeTestRule
            .onNodeWithText("$1,234.56")
            .assertIsDisplayed()
    }

    @Test
    fun amountInput_handlesDifferentCurrencies() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                AmountInput(
                    amount = BigDecimal("100.00"),
                    currency = "EUR",
                    onAmountChanged = { }
                )
            }
        }

        // Verify EUR symbol is displayed
        composeTestRule
            .onNodeWithText("€")
            .assertIsDisplayed()
    }

    @Test
    fun amountInputWithCurrency_displaysCorrectly() {
        var amount: BigDecimal? = BigDecimal("50.00")
        var currency = "USD"

        composeTestRule.setContent {
            ExpenseTrackerTheme {
                AmountInputWithCurrency(
                    amount = amount,
                    currency = currency,
                    onAmountChanged = { amount = it },
                    onCurrencyChanged = { currency = it }
                )
            }
        }

        // Verify amount input is displayed
        composeTestRule
            .onNodeWithText("Amount")
            .assertIsDisplayed()

        // Verify currency selector is displayed
        composeTestRule
            .onNodeWithText("Currency")
            .assertIsDisplayed()

        // Verify current values are shown
        composeTestRule
            .onNodeWithText("50.00")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("$ USD")
            .assertIsDisplayed()
    }

    @Test
    fun amountInput_handlesZeroAmount() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                AmountInput(
                    amount = BigDecimal.ZERO,
                    currency = "USD",
                    onAmountChanged = { }
                )
            }
        }

        // Verify zero is displayed
        composeTestRule
            .onNodeWithText("0")
            .assertIsDisplayed()

        // Verify no preview is shown for zero
        composeTestRule
            .onNodeWithText("$0.00")
            .assertDoesNotExist()
    }
}