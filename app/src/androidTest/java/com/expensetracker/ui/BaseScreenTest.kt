package com.expensetracker.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.expensetracker.MainActivity
import com.expensetracker.data.repository.ExpenseRepository
import com.expensetracker.test.TestDataInitializer
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import javax.inject.Inject

/**
 * Base test class that provides common setup for all screen tests.
 * Uses real MainActivity with navigation for production-ready testing approach.
 * Includes enhanced timing controls and test data initialization.
 */
@HiltAndroidTest
abstract class BaseScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var repository: ExpenseRepository

    @Inject
    lateinit var testDataInitializer: TestDataInitializer

    /**
     * Base setup method that should be called by all subclasses.
     * Handles Hilt injection and basic initialization.
     */
    @Before
    open fun baseSetup() {
        hiltRule.inject()
        // Initialize default categories for all tests
        runBlocking {
            testDataInitializer.initializeDefaultCategories()
        }
    }

    /**
     * Helper method to navigate to a specific screen using bottom navigation.
     * Includes retry logic and proper wait conditions for improved reliability.
     * 
     * @param buttonText The text of the navigation button to click
     */
    protected fun navigateToScreen(buttonText: String) {
        composeTestRule.waitForIdle()
        
        // First, wait for the navigation button to be available
        waitUntilNodeWithText(buttonText, timeoutMs = 10000)
        
        // Try multiple strategies to find and click the navigation item
        var clicked = false
        var attempts = 0
        val maxAttempts = 3
        
        while (!clicked && attempts < maxAttempts) {
            try {
                // Strategy 1: Direct text match
                composeTestRule.onNodeWithText(buttonText, useUnmergedTree = true).performClick()
                clicked = true
            } catch (e: AssertionError) {
                try {
                    // Strategy 2: Try with merged tree (default)
                    composeTestRule.onNodeWithText(buttonText).performClick()
                    clicked = true
                } catch (e2: AssertionError) {
                    try {
                        // Strategy 3: Find within NavigationBar specifically
                        composeTestRule.onNode(
                            hasText(buttonText) and hasAnyAncestor(hasTestTag("navigation_bar")),
                            useUnmergedTree = true
                        ).performClick()
                        clicked = true
                    } catch (e3: AssertionError) {
                        attempts++
                        if (attempts < maxAttempts) {
                            Thread.sleep(1000)
                            composeTestRule.waitForIdle()
                        }
                    }
                }
            }
        }
        
        if (!clicked) {
            // Last resort: try to click any node with the text
            composeTestRule.onAllNodesWithText(buttonText).onFirst().performClick()
        }
        
        // Wait for navigation animation
        composeTestRule.waitForIdle()
        Thread.sleep(2000) // Increased wait for navigation transition
        
        // Wait for the screen to be fully loaded
        waitForScreenToLoad()
    }

    /**
     * Waits for categories to be loaded before proceeding with tests.
     * Essential for tests that depend on category data.
     */
    protected fun waitForCategoriesToLoad(timeoutMs: Long = 10000) {
        runBlocking {
            testDataInitializer.waitForCategoriesLoaded(timeoutMs)
        }
        // Additional UI wait for categories to appear in components
        Thread.sleep(1000)
        composeTestRule.waitForIdle()
    }

    /**
     * Waits for a node with specific text to appear on screen.
     * More reliable than immediate assertions.
     */
    protected fun waitUntilNodeWithText(text: String, timeoutMs: Long = 5000) {
        composeTestRule.waitUntil(timeoutMs) {
            try {
                composeTestRule.onNodeWithText(text).assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }
    }

    /**
     * Waits for a node with specific test tag to appear on screen.
     */
    protected fun waitUntilNodeWithTag(tag: String, timeoutMs: Long = 5000) {
        composeTestRule.waitUntil(timeoutMs) {
            try {
                composeTestRule.onNodeWithTag(tag).assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }
    }

    /**
     * Waits for multiple nodes with a specific tag to be available.
     * Useful for waiting for category chips or expense items to load.
     */
    protected fun waitForNodesWithTag(tag: String, minCount: Int = 1, timeoutMs: Long = 10000) {
        composeTestRule.waitUntil(timeoutMs) {
            try {
                val nodes = composeTestRule.onAllNodesWithTag(tag).fetchSemanticsNodes()
                nodes.size >= minCount
            } catch (e: Exception) {
                false
            }
        }
    }

    /**
     * Safe click operation with retry logic.
     * Waits for the element to be clickable before performing click.
     */
    protected fun safeClick(matcher: SemanticsNodeInteraction, retries: Int = 2) {
        var attempt = 0
        while (attempt <= retries) {
            try {
                matcher.performClick()
                composeTestRule.waitForIdle()
                break
            } catch (e: Exception) {
                if (attempt == retries) throw e
                Thread.sleep(500)
                composeTestRule.waitForIdle()
                attempt++
            }
        }
    }

    /**
     * Safe text input operation with retry logic.
     */
    protected fun safeTypeText(matcher: SemanticsNodeInteraction, text: String, retries: Int = 2) {
        var attempt = 0
        while (attempt <= retries) {
            try {
                matcher.performTextInput(text)
                composeTestRule.waitForIdle()
                break
            } catch (e: Exception) {
                if (attempt == retries) throw e
                Thread.sleep(500)
                composeTestRule.waitForIdle()
                attempt++
            }
        }
    }

    /**
     * Waits for the current screen to be fully loaded.
     * Override in subclasses for screen-specific loading indicators.
     */
    protected open fun waitForScreenToLoad() {
        // Default implementation - wait for any loading indicators to disappear
        Thread.sleep(500)
        composeTestRule.waitForIdle()
    }

    /**
     * Waits for data to be loaded and displayed.
     * Useful for screens that show "loading" or "no data" states initially.
     */
    protected fun waitForDataToLoad(timeoutMs: Long = 5000) {
        composeTestRule.waitForIdle()
        // Give time for data loading operations to complete
        Thread.sleep(1000)
        composeTestRule.waitForIdle()
    }

    /**
     * Initializes test expenses for tests that need sample data.
     * Call this in setup methods for tests that require populated data.
     */
    protected fun initializeTestExpenses() {
        runBlocking {
            testDataInitializer.initializeTestExpenses()
        }
        // Wait for UI to reflect the new data
        composeTestRule.waitForIdle()
        Thread.sleep(500)
    }

    /**
     * Creates a single test expense and returns its ID.
     * Useful for tests that need specific expense data.
     */
    protected fun createTestExpense(
        amount: String = "25.50",
        description: String = "Test Expense",
        categoryId: Long = 1L,
        tags: List<String> = emptyList()
    ): Long? {
        return runBlocking {
            testDataInitializer.createTestExpense(amount, description, categoryId, tags)
        }
    }

    /**
     * Asserts that a node with text exists or shows alternative text.
     * Useful for handling empty states vs populated states.
     */
    protected fun assertTextExistsOrAlternative(primaryText: String, alternativeText: String) {
        try {
            composeTestRule.onNodeWithText(primaryText).assertIsDisplayed()
        } catch (e: AssertionError) {
            // If primary text doesn't exist, check for alternative
            composeTestRule.onNodeWithText(alternativeText).assertIsDisplayed()
        }
    }

    /**
     * Asserts that at least one of the provided texts exists on screen.
     * Useful for handling various UI states.
     */
    protected fun assertAnyTextExists(vararg texts: String) {
        val exceptions = mutableListOf<AssertionError>()
        for (text in texts) {
            try {
                composeTestRule.onNodeWithText(text).assertIsDisplayed()
                return // Found one, exit successfully
            } catch (e: AssertionError) {
                exceptions.add(e)
            }
        }
        // If we get here, none of the texts were found
        throw AssertionError("None of the expected texts were found: ${texts.joinToString(", ")}")
    }
}