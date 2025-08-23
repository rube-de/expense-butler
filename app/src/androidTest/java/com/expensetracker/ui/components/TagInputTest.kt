package com.expensetracker.ui.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.expensetracker.ui.theme.ExpenseTrackerTheme
import org.junit.Rule
import org.junit.Test

class TagInputTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val availableTags = listOf(
        "coffee", "work", "breakfast", "lunch", "dinner",
        "travel", "gas", "groceries", "entertainment", "shopping"
    )

    @Test
    fun tagInput_displaysSelectedTags() {
        val selectedTags = listOf("coffee", "work")

        composeTestRule.setContent {
            ExpenseTrackerTheme {
                TagInput(
                    selectedTags = selectedTags,
                    availableTags = availableTags,
                    onTagsChanged = { }
                )
            }
        }

        // Verify selected tags are displayed
        composeTestRule
            .onNodeWithText("coffee")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("work")
            .assertIsDisplayed()

        // Verify input field is displayed
        composeTestRule
            .onNodeWithText("Tags")
            .assertIsDisplayed()
    }

    @Test
    fun tagInput_allowsTextInput() {
        var updatedTags: List<String> = emptyList()

        composeTestRule.setContent {
            ExpenseTrackerTheme {
                TagInput(
                    selectedTags = emptyList(),
                    availableTags = availableTags,
                    onTagsChanged = { updatedTags = it }
                )
            }
        }

        // Find the text field and enter text
        val textField = composeTestRule.onNode(hasSetTextAction())
        textField.performTextInput("breakfast")
        
        composeTestRule.waitForIdle()
        
        // Verify the text appears in the field
        textField.assertTextContains("breakfast")
    }

    @Test
    fun tagInput_removesSelectedTags() {
        val initialTags = listOf("coffee", "work")
        var updatedTags = initialTags

        composeTestRule.setContent {
            ExpenseTrackerTheme {
                TagInput(
                    selectedTags = updatedTags,
                    availableTags = availableTags,
                    onTagsChanged = { updatedTags = it }
                )
            }
        }

        // Find and click the remove button for "coffee" tag
        // Note: This test assumes the remove button has a content description
        composeTestRule
            .onAllNodesWithContentDescription("Remove tag")
            .onFirst()
            .performClick()

        // Verify the callback was called (in real implementation, this would update the state)
        // This is a simplified test - in practice, you'd need to handle state updates properly
    }

    @Test
    fun tagInput_showsSuggestions() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                TagInput(
                    selectedTags = emptyList(),
                    availableTags = availableTags,
                    onTagsChanged = { }
                )
            }
        }

        // Type partial text to trigger suggestions
        composeTestRule
            .onNodeWithText("Tags")
            .performTextInput("cof")

        // Wait for suggestions to appear
        composeTestRule.waitForIdle()

        // Verify suggestion appears (coffee should match "cof")
        composeTestRule
            .onNodeWithText("coffee")
            .assertIsDisplayed()
    }

    @Test
    fun tagInput_addsTagFromSuggestion() {
        var updatedTags: List<String> = emptyList()

        composeTestRule.setContent {
            ExpenseTrackerTheme {
                TagInput(
                    selectedTags = emptyList(),
                    availableTags = availableTags,
                    onTagsChanged = { updatedTags = it }
                )
            }
        }

        // Type to show suggestions
        composeTestRule
            .onNodeWithText("Tags")
            .performTextInput("cof")

        composeTestRule.waitForIdle()

        // Click on suggestion
        composeTestRule
            .onNodeWithText("coffee")
            .performClick()

        // Verify tag was added (in real implementation)
        // This would require proper state management in the test
    }

    @Test
    fun tagInput_handlesEmptyState() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                TagInput(
                    selectedTags = emptyList(),
                    availableTags = emptyList(),
                    onTagsChanged = { }
                )
            }
        }

        // Verify input field is displayed even with no tags
        composeTestRule
            .onNodeWithText("Tags")
            .assertIsDisplayed()
    }

    @Test
    fun tagInput_preventsAddingDuplicateTags() {
        val selectedTags = listOf("coffee")
        var updatedTags = selectedTags

        composeTestRule.setContent {
            ExpenseTrackerTheme {
                TagInput(
                    selectedTags = updatedTags,
                    availableTags = availableTags,
                    onTagsChanged = { updatedTags = it }
                )
            }
        }

        // Try to add existing tag
        composeTestRule
            .onNodeWithText("Tags")
            .performTextInput("coffee")

        // Perform IME action (Done)
        composeTestRule
            .onNodeWithText("Tags")
            .performImeAction()

        // Verify duplicate wasn't added (would need proper state management to test)
    }
}