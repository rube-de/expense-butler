package com.expensetracker.ui.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.expensetracker.data.model.Category
import com.expensetracker.ui.theme.ExpenseTrackerTheme
import org.junit.Rule
import org.junit.Test

class CategoryChipTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleCategory = Category(
        id = 1,
        name = "Food",
        color = "#4CAF50",
        icon = "restaurant"
    )

    @Test
    fun categoryChip_displaysUnselectedState() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                CategoryChip(
                    category = sampleCategory,
                    isSelected = false,
                    onCategoryClick = { }
                )
            }
        }

        // Verify category name is displayed
        composeTestRule
            .onNodeWithText("Food")
            .assertIsDisplayed()

        // Verify chip is clickable
        composeTestRule
            .onNodeWithText("Food")
            .assertHasClickAction()
    }

    @Test
    fun categoryChip_displaysSelectedState() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                CategoryChip(
                    category = sampleCategory,
                    isSelected = true,
                    onCategoryClick = { }
                )
            }
        }

        // Verify category name is displayed
        composeTestRule
            .onNodeWithText("Food")
            .assertIsDisplayed()
    }

    @Test
    fun categoryChip_handlesClickEvent() {
        var clickedCategory: Category? = null

        composeTestRule.setContent {
            ExpenseTrackerTheme {
                CategoryChip(
                    category = sampleCategory,
                    isSelected = false,
                    onCategoryClick = { clickedCategory = it }
                )
            }
        }

        // Click on the chip
        composeTestRule
            .onNodeWithText("Food")
            .performClick()

        // Verify click callback was triggered with correct category
        assert(clickedCategory == sampleCategory)
    }

    @Test
    fun categoryChipRow_displaysMultipleCategories() {
        val categories = listOf(
            Category(1, "Food", "#4CAF50", "restaurant"),
            Category(2, "Travel", "#2196F3", "flight"),
            Category(3, "Entertainment", "#FF9800", "movie")
        )

        composeTestRule.setContent {
            ExpenseTrackerTheme {
                CategoryChipRow(
                    categories = categories,
                    selectedCategory = categories[1], // Travel selected
                    onCategoryClick = { }
                )
            }
        }

        // Verify all categories are displayed
        composeTestRule
            .onNodeWithText("Food")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Travel")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Entertainment")
            .assertIsDisplayed()
    }

    @Test
    fun categoryChipRow_handlesSelection() {
        val categories = listOf(
            Category(1, "Food", "#4CAF50", "restaurant"),
            Category(2, "Travel", "#2196F3", "flight")
        )
        var selectedCategory: Category? = null

        composeTestRule.setContent {
            ExpenseTrackerTheme {
                CategoryChipRow(
                    categories = categories,
                    selectedCategory = null,
                    onCategoryClick = { selectedCategory = it }
                )
            }
        }

        // Click on Travel category
        composeTestRule
            .onNodeWithText("Travel")
            .performClick()

        // Verify correct category was selected
        assert(selectedCategory == categories[1])
    }
}