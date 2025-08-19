package com.expensetracker.test

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule

/**
 * Extension functions for ComposeTestRule to provide more convenient and reliable test operations.
 * These extensions help reduce test flakiness and improve readability.
 */

/**
 * Safely performs a click with retry logic and proper waiting.
 * More reliable than direct performClick() for flaky UI elements.
 */
fun ComposeTestRule.safeClick(
    matcher: SemanticsNodeInteraction,
    retries: Int = 3,
    delayBetweenRetries: Long = 500
) {
    repeat(retries) { attempt ->
        try {
            matcher.performClick()
            waitForIdle()
            return // Success
        } catch (e: Exception) {
            if (attempt == retries - 1) throw e // Last attempt, throw the error
            Thread.sleep(delayBetweenRetries)
            waitForIdle()
        }
    }
}

/**
 * Safely types text with retry logic.
 * Handles cases where text input fields might not be ready immediately.
 */
fun ComposeTestRule.safeTypeText(
    matcher: SemanticsNodeInteraction,
    text: String,
    retries: Int = 3,
    delayBetweenRetries: Long = 500
) {
    repeat(retries) { attempt ->
        try {
            matcher.performTextInput(text)
            waitForIdle()
            return // Success
        } catch (e: Exception) {
            if (attempt == retries - 1) throw e // Last attempt, throw the error
            Thread.sleep(delayBetweenRetries)
            waitForIdle()
        }
    }
}

/**
 * Waits for a node to appear and then performs a click.
 * Combines waiting and clicking for better reliability.
 */
fun ComposeTestRule.waitAndClick(
    matcher: SemanticsNodeInteraction,
    timeoutMs: Long = 5000
) {
    waitUntilExists(matcher, timeoutMs)
    safeClick(matcher)
}

/**
 * Waits for a node with specific text to exist on screen.
 */
fun ComposeTestRule.waitUntilTextExists(
    text: String,
    timeoutMs: Long = 5000
) {
    waitUntil(timeoutMs) {
        try {
            onNodeWithText(text).assertExists()
            true
        } catch (e: AssertionError) {
            false
        }
    }
}

/**
 * Waits for a node with specific tag to exist on screen.
 */
fun ComposeTestRule.waitUntilTagExists(
    tag: String,
    timeoutMs: Long = 5000
) {
    waitUntil(timeoutMs) {
        try {
            onNodeWithTag(tag).assertExists()
            true
        } catch (e: AssertionError) {
            false
        }
    }
}

/**
 * Waits for a specific node to exist on screen.
 */
fun ComposeTestRule.waitUntilExists(
    matcher: SemanticsNodeInteraction,
    timeoutMs: Long = 5000
) {
    waitUntil(timeoutMs) {
        try {
            matcher.assertExists()
            true
        } catch (e: AssertionError) {
            false
        }
    }
}

/**
 * Waits for a node to be displayed (visible) on screen.
 */
fun ComposeTestRule.waitUntilDisplayed(
    matcher: SemanticsNodeInteraction,
    timeoutMs: Long = 5000
) {
    waitUntil(timeoutMs) {
        try {
            matcher.assertIsDisplayed()
            true
        } catch (e: AssertionError) {
            false
        }
    }
}

/**
 * Waits for text to be displayed on screen.
 */
fun ComposeTestRule.waitUntilTextDisplayed(
    text: String,
    timeoutMs: Long = 5000
) {
    waitUntilDisplayed(onNodeWithText(text), timeoutMs)
}

/**
 * Waits for multiple nodes with a tag to be available.
 * Useful for waiting for lists to load (e.g., category chips).
 */
fun ComposeTestRule.waitForNodesWithTag(
    tag: String,
    minCount: Int = 1,
    timeoutMs: Long = 10000
) {
    waitUntil(timeoutMs) {
        try {
            val nodes = onAllNodesWithTag(tag).fetchSemanticsNodes()
            nodes.size >= minCount
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Waits for nodes with specific text to be available.
 */
fun ComposeTestRule.waitForNodesWithText(
    text: String,
    minCount: Int = 1,
    timeoutMs: Long = 10000
) {
    waitUntil(timeoutMs) {
        try {
            val nodes = onAllNodesWithText(text).fetchSemanticsNodes()
            nodes.size >= minCount
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Performs a click and waits for the result.
 * Useful for navigation or actions that trigger UI changes.
 */
fun ComposeTestRule.clickAndWait(
    matcher: SemanticsNodeInteraction,
    waitMs: Long = 1000
) {
    safeClick(matcher)
    Thread.sleep(waitMs)
    waitForIdle()
}

/**
 * Types text and waits for the result.
 * Useful for form inputs that trigger validation or other UI changes.
 */
fun ComposeTestRule.typeAndWait(
    matcher: SemanticsNodeInteraction,
    text: String,
    waitMs: Long = 500
) {
    safeTypeText(matcher, text)
    Thread.sleep(waitMs)
    waitForIdle()
}

/**
 * Asserts that either primary text or alternative text exists.
 * Useful for handling different UI states (empty vs populated).
 */
fun ComposeTestRule.assertTextExistsOrAlternative(
    primaryText: String,
    alternativeText: String
) {
    try {
        onNodeWithText(primaryText).assertIsDisplayed()
    } catch (e: AssertionError) {
        onNodeWithText(alternativeText).assertIsDisplayed()
    }
}

/**
 * Asserts that at least one of the provided texts exists.
 * Throws meaningful error if none are found.
 */
fun ComposeTestRule.assertAnyTextExists(vararg texts: String) {
    for (text in texts) {
        try {
            onNodeWithText(text).assertIsDisplayed()
            return // Found one, exit successfully
        } catch (e: AssertionError) {
            // Continue to next text
        }
    }
    throw AssertionError("None of the expected texts were found: ${texts.joinToString(", ")}")
}

/**
 * Waits for data to load by checking for the absence of loading indicators.
 * Looks for common loading states and waits for them to disappear.
 */
fun ComposeTestRule.waitForDataToLoad(timeoutMs: Long = 5000) {
    // Wait for common loading indicators to disappear
    waitUntil(timeoutMs) {
        try {
            // If any of these loading indicators exist, keep waiting
            val loadingIndicators = listOf("Loading...", "Please wait", "Fetching data", "Loading categories...")
            val hasLoadingIndicator = loadingIndicators.any { text ->
                try {
                    onNodeWithText(text).assertExists()
                    true
                } catch (e: AssertionError) {
                    false
                }
            }
            !hasLoadingIndicator // Return true when no loading indicators are present
        } catch (e: Exception) {
            true // If there's an error checking, assume loading is complete
        }
    }
    waitForIdle()
}

/**
 * Waits specifically for categories to load in the AddExpense screen.
 * This ensures category chips are available before proceeding with tests.
 */
fun ComposeTestRule.waitForCategoriesToLoadInAddExpense(timeoutMs: Long = 15000) {
    // First wait for the basic screen structure
    waitUntilTextExists("Amount", timeoutMs = 5000)
    waitUntilTextExists("Category", timeoutMs = 2000)
    
    // Wait for category chips to appear (indicating categories have loaded)
    waitUntil(timeoutMs) {
        try {
            // Check if we have at least one category chip
            val categoryChips = onAllNodesWithTag("category_chip").fetchSemanticsNodes()
            if (categoryChips.isNotEmpty()) {
                true // Categories are loaded
            } else {
                // Check if "Loading categories..." text is gone
                try {
                    onNodeWithText("Loading categories...").assertDoesNotExist()
                    // If no loading text and no chips yet, wait a bit more
                    false
                } catch (e: AssertionError) {
                    // Loading text still exists, keep waiting
                    false
                }
            }
        } catch (e: Exception) {
            false
        }
    }
    
    // Additional wait for compose to settle
    waitForIdle()
}

/**
 * Enhanced category loading wait that also checks for UI state.
 * Combines multiple strategies to ensure categories are truly ready.
 */
fun ComposeTestRule.ensureCategoriesLoaded(timeoutMs: Long = 15000) {
    waitUntil(timeoutMs) {
        try {
            // Strategy 1: Check for category chips
            val chips = onAllNodesWithTag("category_chip").fetchSemanticsNodes()
            if (chips.isNotEmpty()) return@waitUntil true
            
            // Strategy 2: Check for known category names
            val defaultCategories = listOf("Food", "Transport", "Entertainment", "Shopping", "Health", "Travel")
            val foundCategory = defaultCategories.any { category ->
                try {
                    onNodeWithText(category).assertExists()
                    true
                } catch (e: AssertionError) {
                    false
                }
            }
            if (foundCategory) return@waitUntil true
            
            // Strategy 3: Ensure no loading indicators
            val loadingGone = try {
                onNodeWithText("Loading categories...").assertDoesNotExist()
                true
            } catch (e: AssertionError) {
                false
            }
            
            loadingGone
        } catch (e: Exception) {
            false
        }
    }
    
    waitForIdle()
}

/**
 * Waits for screen transitions to complete.
 * Includes both animation wait and idle wait.
 */
fun ComposeTestRule.waitForScreenTransition(waitMs: Long = 1500) {
    Thread.sleep(waitMs) // Wait for animations
    waitForIdle() // Wait for compose to settle
}

/**
 * Finds a text input field by looking for it within a parent with specific text.
 * Enhanced to handle the AddExpenseScreen structure with custom components.
 */
fun ComposeTestRule.findTextFieldInSection(sectionText: String): SemanticsNodeInteraction {
    return when (sectionText) {
        "Amount" -> {
            // For AmountInputWithCurrency -> AmountInput -> OutlinedTextField
            // Try multiple strategies
            try {
                // First try to find by test tag or content description
                onNode(
                    hasSetTextAction() and 
                    (hasContentDescription("Amount") or 
                     hasTestTag("amount_input"))
                )
            } catch (e: Exception) {
                try {
                    // Try to find any text field in the amount section
                    onAllNodes(hasSetTextAction())
                        .filterToOne(hasAnyAncestor(hasText("Amount")))
                } catch (e2: Exception) {
                    // Last resort: find first text field (might be amount)
                    onAllNodes(hasSetTextAction())[0]
                }
            }
        }
        "Description" -> {
            // For OutlinedTextField with "What did you spend on?" placeholder
            try {
                onNode(
                    hasSetTextAction() and 
                    (hasText("What did you spend on?") or 
                     hasContentDescription("Description") or
                     hasTestTag("description_input"))
                )
            } catch (e: Exception) {
                onNode(
                    hasSetTextAction() and hasAnyAncestor(hasText("Description"))
                )
            }
        }
        "Tags" -> {
            // For TagInput component
            try {
                onNode(
                    hasSetTextAction() and 
                    (hasText("Add tags...") or 
                     hasText("Add tags to organize your expenses...") or
                     hasContentDescription("Tags") or 
                     hasTestTag("tag_input"))
                )
            } catch (e: Exception) {
                onNode(
                    hasSetTextAction() and hasAnyAncestor(hasText("Tags"))
                )
            }
        }
        else -> {
            // Default fallback behavior
            onNode(
                hasSetTextAction() and hasAnyAncestor(hasText(sectionText))
            )
        }
    }
}

/**
 * Finds the first available category chip.
 * Useful for category selection in tests.
 */
fun ComposeTestRule.findFirstCategoryChip(): SemanticsNodeInteraction {
    waitForNodesWithTag("category_chip", minCount = 1, timeoutMs = 10000)
    return onAllNodesWithTag("category_chip")[0]
}

/**
 * Fills out a form field by section name and value.
 * Combines finding the field and typing text.
 */
fun ComposeTestRule.fillFormField(sectionName: String, value: String) {
    val field = findTextFieldInSection(sectionName)
    safeTypeText(field, value)
}

/**
 * Clicks on a category chip by index.
 * Waits for categories to load first.
 */
fun ComposeTestRule.selectCategoryByIndex(index: Int = 0) {
    waitForNodesWithTag("category_chip", minCount = index + 1, timeoutMs = 10000)
    safeClick(onAllNodesWithTag("category_chip")[index])
}

/**
 * Adds a tag using the tag input system.
 * Types the tag text - the TagInput component should handle adding it automatically.
 */
fun ComposeTestRule.addTag(tagText: String) {
    val tagField = findTextFieldInSection("Tags")
    safeTypeText(tagField, tagText)
    // TagInput component might auto-add on text input or require pressing Enter
    try {
        tagField.performImeAction()
    } catch (e: Exception) {
        // Ignore if IME action fails
    }
    waitForIdle()
}

/**
 * Completes a full expense form with all fields.
 * Convenient method for integration tests.
 */
fun ComposeTestRule.fillExpenseForm(
    amount: String = "25.50",
    description: String = "Test Expense",
    categoryIndex: Int = 0,
    tags: List<String> = emptyList()
) {
    // Fill amount
    fillFormField("Amount", amount)
    
    // Fill description
    fillFormField("Description", description)
    
    // Select category
    selectCategoryByIndex(categoryIndex)
    
    // Add tags
    tags.forEach { tag ->
        addTag(tag)
    }
    
    waitForIdle()
}

/**
 * Debug helper to print the current screen state when tests fail.
 * Prints the semantic tree to help identify what's actually on screen.
 */
fun ComposeTestRule.debugPrintScreen(tag: String = "DEBUG") {
    try {
        onRoot().printToLog(tag)
    } catch (e: Exception) {
        println("$tag: Failed to print screen state: ${e.message}")
    }
}

/**
 * Enhanced form field filler with debug information.
 * Provides better error messages when field finding fails.
 */
fun ComposeTestRule.fillFormFieldWithDebug(sectionName: String, value: String) {
    try {
        val field = findTextFieldInSection(sectionName)
        safeTypeText(field, value)
    } catch (e: Exception) {
        println("FORM_FIELD_ERROR: Failed to find/fill field '$sectionName' with value '$value': ${e.message}")
        debugPrintScreen("FORM_FIELD_FAIL_$sectionName")
        throw e
    }
}

/**
 * Enhanced category selection with debug information.
 */
fun ComposeTestRule.selectCategoryByIndexWithDebug(index: Int = 0) {
    try {
        waitForNodesWithTag("category_chip", minCount = index + 1, timeoutMs = 15000)
        safeClick(onAllNodesWithTag("category_chip")[index])
    } catch (e: Exception) {
        println("CATEGORY_ERROR: Failed to select category at index $index: ${e.message}")
        val chips = onAllNodesWithTag("category_chip").fetchSemanticsNodes()
        println("CATEGORY_DEBUG: Found ${chips.size} category chips")
        debugPrintScreen("CATEGORY_FAIL")
        throw e
    }
}

/**
 * Waits for a specific condition with debug output on timeout.
 */
fun ComposeTestRule.waitUntilWithDebug(
    timeoutMs: Long = 5000,
    debugTag: String = "WAIT",
    condition: () -> Boolean
) {
    try {
        waitUntil(timeoutMs, condition)
    } catch (e: Exception) {
        println("$debugTag: Timeout after ${timeoutMs}ms waiting for condition")
        debugPrintScreen("${debugTag}_TIMEOUT")
        throw e
    }
}