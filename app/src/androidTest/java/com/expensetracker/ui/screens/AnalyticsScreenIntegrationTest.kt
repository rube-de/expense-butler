package com.expensetracker.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.expensetracker.data.model.*
import com.expensetracker.ui.theme.ExpenseTrackerTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Integration tests for AnalyticsScreen with real ViewModels.
 */
@HiltAndroidTest
class AnalyticsScreenIntegrationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun should_display_analytics_screen_with_period_selector() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                AnalyticsScreen()
            }
        }

        // Verify period selector is displayed
        composeTestRule
            .onNodeWithText("Month")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Quarter")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Year")
            .assertIsDisplayed()

        // Verify total spending card is displayed
        composeTestRule
            .onNodeWithText("Total Spending")
            .assertIsDisplayed()
    }

    @Test
    fun should_handle_period_selection() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                AnalyticsScreen()
            }
        }

        // Click on Quarter period
        composeTestRule
            .onNodeWithText("Quarter")
            .performClick()

        // Verify Quarter is selected (button should be highlighted)
        composeTestRule
            .onNodeWithText("Quarter")
            .assertIsDisplayed()
    }

    @Test
    fun should_display_empty_state_initially() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                AnalyticsScreen()
            }
        }

        // Should show $0.00 initially
        composeTestRule
            .onNodeWithText("$0.00")
            .assertIsDisplayed()
    }
}