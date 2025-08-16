package com.expensetracker.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.expensetracker.data.model.Category
import com.expensetracker.data.model.Expense
import com.expensetracker.ui.theme.ExpenseTrackerTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class ExpenseListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleExpenses = listOf(
        Expense(
            id = 1,
            amount = BigDecimal("25.50"),
            currency = "USD",
            description = "Coffee",
            categoryId = 1,
            tags = listOf("coffee", "work"),
            date = LocalDateTime.of(2024, 1, 15, 9, 30)
        ),
        Expense(
            id = 2,
            amount = BigDecimal("120.00"),
            currency = "USD",
            description = "Groceries",
            categoryId = 2,
            tags = listOf("food", "weekly"),
            date = LocalDateTime.of(2024, 1, 14, 18, 0)
        )
    )

    private val sampleCategories = listOf(
        Category(1, "Food", "#4CAF50", "restaurant", true),
        Category(2, "Shopping", "#9C27B0", "shopping_cart", true)
    )

    @Test
    fun should_display_expense_list() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                ExpenseListContent(
                    uiState = ExpenseListUiState(
                        expenses = sampleExpenses,
                        categories = sampleCategories
                    ),
                    onExpenseClick = { },
                    onDeleteExpense = { },
                    onSearchTextChanged = { },
                    onCategoryFilterSelected = { },
                    onTagFilterChanged = { },
                    onDateRangeChanged = { _, _ -> },
                    onClearFilters = { },
                    onToggleFilters = { }
                )
            }
        }

        // Verify expenses are displayed
        composeTestRule.onNodeWithText("Coffee").assertIsDisplayed()
        composeTestRule.onNodeWithText("Groceries").assertIsDisplayed()
        composeTestRule.onNodeWithText("$25.50").assertIsDisplayed()
        composeTestRule.onNodeWithText("$120.00").assertIsDisplayed()
    }

    @Test
    fun should_display_search_functionality() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                ExpenseListContent(
                    uiState = ExpenseListUiState(
                        expenses = sampleExpenses,
                        categories = sampleCategories,
                        searchText = "coffee"
                    ),
                    onExpenseClick = { },
                    onDeleteExpense = { },
                    onSearchTextChanged = { },
                    onCategoryFilterSelected = { },
                    onTagFilterChanged = { },
                    onDateRangeChanged = { _, _ -> },
                    onClearFilters = { },
                    onToggleFilters = { }
                )
            }
        }

        // Verify search field exists and has correct value
        composeTestRule.onNodeWithText("coffee").assertIsDisplayed()
    }

    @Test
    fun should_display_filter_options_when_expanded() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                ExpenseListContent(
                    uiState = ExpenseListUiState(
                        expenses = sampleExpenses,
                        categories = sampleCategories,
                        showFilters = true
                    ),
                    onExpenseClick = { },
                    onDeleteExpense = { },
                    onSearchTextChanged = { },
                    onCategoryFilterSelected = { },
                    onTagFilterChanged = { },
                    onDateRangeChanged = { _, _ -> },
                    onClearFilters = { },
                    onToggleFilters = { }
                )
            }
        }

        // Verify filter options are displayed
        composeTestRule.onNodeWithText("Category").assertIsDisplayed()
        composeTestRule.onNodeWithText("Date Range").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tags").assertIsDisplayed()
    }

    @Test
    fun should_handle_expense_click() {
        var clickedExpense: Expense? = null

        composeTestRule.setContent {
            ExpenseTrackerTheme {
                ExpenseListContent(
                    uiState = ExpenseListUiState(
                        expenses = sampleExpenses,
                        categories = sampleCategories
                    ),
                    onExpenseClick = { clickedExpense = it },
                    onDeleteExpense = { },
                    onSearchTextChanged = { },
                    onCategoryFilterSelected = { },
                    onTagFilterChanged = { },
                    onDateRangeChanged = { _, _ -> },
                    onClearFilters = { },
                    onToggleFilters = { }
                )
            }
        }

        // Click on first expense
        composeTestRule.onNodeWithText("Coffee").performClick()

        // Verify callback was called
        assert(clickedExpense?.description == "Coffee")
    }

    @Test
    fun should_display_empty_state_when_no_expenses() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                ExpenseListContent(
                    uiState = ExpenseListUiState(
                        expenses = emptyList(),
                        categories = sampleCategories
                    ),
                    onExpenseClick = { },
                    onDeleteExpense = { },
                    onSearchTextChanged = { },
                    onCategoryFilterSelected = { },
                    onTagFilterChanged = { },
                    onDateRangeChanged = { _, _ -> },
                    onClearFilters = { },
                    onToggleFilters = { }
                )
            }
        }

        // Verify empty state is displayed
        composeTestRule.onNodeWithText("No expenses found").assertIsDisplayed()
    }

    @Test
    fun should_display_loading_state() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                ExpenseListContent(
                    uiState = ExpenseListUiState(
                        expenses = emptyList(),
                        categories = sampleCategories,
                        isLoading = true
                    ),
                    onExpenseClick = { },
                    onDeleteExpense = { },
                    onSearchTextChanged = { },
                    onCategoryFilterSelected = { },
                    onTagFilterChanged = { },
                    onDateRangeChanged = { _, _ -> },
                    onClearFilters = { },
                    onToggleFilters = { }
                )
            }
        }

        // Verify loading indicator is displayed
        composeTestRule.onNode(hasTestTag("loading_indicator")).assertIsDisplayed()
    }

    @Test
    fun should_display_error_message() {
        val errorMessage = "Failed to load expenses"

        composeTestRule.setContent {
            ExpenseTrackerTheme {
                ExpenseListContent(
                    uiState = ExpenseListUiState(
                        expenses = emptyList(),
                        categories = sampleCategories,
                        errorMessage = errorMessage
                    ),
                    onExpenseClick = { },
                    onDeleteExpense = { },
                    onSearchTextChanged = { },
                    onCategoryFilterSelected = { },
                    onTagFilterChanged = { },
                    onDateRangeChanged = { _, _ -> },
                    onClearFilters = { },
                    onToggleFilters = { }
                )
            }
        }

        // Verify error message is displayed
        composeTestRule.onNodeWithText(errorMessage).assertIsDisplayed()
    }
}