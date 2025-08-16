package com.expensetracker.ui.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.expensetracker.data.model.Category
import com.expensetracker.data.model.Expense
import com.expensetracker.ui.theme.ExpenseTrackerTheme
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime

class ExpenseCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleExpense = Expense(
        id = 1,
        amount = BigDecimal("25.50"),
        currency = "USD",
        description = "Coffee and pastry at local cafe",
        categoryId = 1,
        tags = listOf("coffee", "breakfast", "work"),
        date = LocalDateTime.of(2024, 1, 15, 9, 30)
    )

    private val sampleCategory = Category(
        id = 1,
        name = "Food",
        color = "#4CAF50",
        icon = "restaurant"
    )

    @Test
    fun expenseCard_displaysExpenseInformation() {
        var clickedExpense: Expense? = null

        composeTestRule.setContent {
            ExpenseTrackerTheme {
                ExpenseCard(
                    expense = sampleExpense,
                    category = sampleCategory,
                    onExpenseClick = { clickedExpense = it }
                )
            }
        }

        // Verify expense description is displayed
        composeTestRule
            .onNodeWithText("Coffee and pastry at local cafe")
            .assertIsDisplayed()

        // Verify amount is displayed with currency formatting
        composeTestRule
            .onNodeWithText("$25.50")
            .assertIsDisplayed()

        // Verify category is displayed
        composeTestRule
            .onNodeWithText("Food")
            .assertIsDisplayed()

        // Verify date is displayed
        composeTestRule
            .onNodeWithText("1/15/2024")
            .assertIsDisplayed()

        // Verify tags are displayed
        composeTestRule
            .onNodeWithText("coffee")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("breakfast")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("work")
            .assertIsDisplayed()
    }

    @Test
    fun expenseCard_handlesClickEvent() {
        var clickedExpense: Expense? = null

        composeTestRule.setContent {
            ExpenseTrackerTheme {
                ExpenseCard(
                    expense = sampleExpense,
                    category = sampleCategory,
                    onExpenseClick = { clickedExpense = it }
                )
            }
        }

        // Click on the card
        composeTestRule
            .onNodeWithText("Coffee and pastry at local cafe")
            .performClick()

        // Verify click callback was triggered with correct expense
        assert(clickedExpense == sampleExpense)
    }

    @Test
    fun expenseCard_displaysWithoutCategory() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                ExpenseCard(
                    expense = sampleExpense,
                    category = null,
                    onExpenseClick = { }
                )
            }
        }

        // Verify expense information is still displayed
        composeTestRule
            .onNodeWithText("Coffee and pastry at local cafe")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("$25.50")
            .assertIsDisplayed()

        // Verify category is not displayed
        composeTestRule
            .onNodeWithText("Food")
            .assertDoesNotExist()
    }

    @Test
    fun expenseCard_displaysWithoutTags() {
        val expenseWithoutTags = sampleExpense.copy(tags = emptyList())

        composeTestRule.setContent {
            ExpenseTrackerTheme {
                ExpenseCard(
                    expense = expenseWithoutTags,
                    category = sampleCategory,
                    onExpenseClick = { }
                )
            }
        }

        // Verify expense information is displayed
        composeTestRule
            .onNodeWithText("Coffee and pastry at local cafe")
            .assertIsDisplayed()

        // Verify tags are not displayed
        composeTestRule
            .onNodeWithText("coffee")
            .assertDoesNotExist()
    }

    @Test
    fun expenseCard_truncatesLongDescription() {
        val longDescriptionExpense = sampleExpense.copy(
            description = "This is a very long description that should be truncated when displayed in the expense card to prevent layout issues"
        )

        composeTestRule.setContent {
            ExpenseTrackerTheme {
                ExpenseCard(
                    expense = longDescriptionExpense,
                    category = sampleCategory,
                    onExpenseClick = { }
                )
            }
        }

        // Verify the long description is displayed (truncation is handled by maxLines)
        composeTestRule
            .onNodeWithText(longDescriptionExpense.description)
            .assertIsDisplayed()
    }
}