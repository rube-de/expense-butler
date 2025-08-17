package com.expensetracker.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.expensetracker.data.model.*
import com.expensetracker.ui.theme.ExpenseTrackerTheme
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

/**
 * UI tests for AnalyticsScreen composable.
 */
class AnalyticsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var mockViewModel: AnalyticsViewModel
    private lateinit var analyticsDataFlow: MutableStateFlow<AnalyticsData>
    private lateinit var isLoadingFlow: MutableStateFlow<Boolean>
    private lateinit var errorFlow: MutableStateFlow<String?>

    @Before
    fun setup() {
        mockViewModel = mockk(relaxed = true)
        analyticsDataFlow = MutableStateFlow(createEmptyAnalyticsData())
        isLoadingFlow = MutableStateFlow(false)
        errorFlow = MutableStateFlow(null)

        every { mockViewModel.analyticsData } returns analyticsDataFlow
        every { mockViewModel.isLoading } returns isLoadingFlow
        every { mockViewModel.error } returns errorFlow
    }

    @Test
    fun should_display_loading_indicator_when_loading() {
        isLoadingFlow.value = true

        composeTestRule.setContent {
            ExpenseTrackerTheme {
                AnalyticsScreen(viewModel = mockViewModel)
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Loading analytics")
            .assertIsDisplayed()
    }

    @Test
    fun should_display_error_message_when_error_occurs() {
        errorFlow.value = "Failed to load data"

        composeTestRule.setContent {
            ExpenseTrackerTheme {
                AnalyticsScreen(viewModel = mockViewModel)
            }
        }

        composeTestRule
            .onNodeWithText("Failed to load data")
            .assertIsDisplayed()
    }

    @Test
    fun should_display_total_spending_amount() {
        analyticsDataFlow.value = createAnalyticsDataWithSpending(BigDecimal("1250.50"))

        composeTestRule.setContent {
            ExpenseTrackerTheme {
                AnalyticsScreen(viewModel = mockViewModel)
            }
        }

        composeTestRule
            .onNodeWithText("$1,250.50")
            .assertIsDisplayed()
    }

    @Test
    fun should_display_period_selector_with_options() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                AnalyticsScreen(viewModel = mockViewModel)
            }
        }

        composeTestRule
            .onNodeWithText("Month")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Quarter")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Year")
            .assertIsDisplayed()
    }

    @Test
    fun should_display_empty_state_when_no_data_available() {
        analyticsDataFlow.value = createEmptyAnalyticsData()

        composeTestRule.setContent {
            ExpenseTrackerTheme {
                AnalyticsScreen(viewModel = mockViewModel)
            }
        }

        composeTestRule
            .onNodeWithText("No expense data available")
            .assertIsDisplayed()
    }

    private fun createEmptyAnalyticsData() = AnalyticsData(
        totalSpending = BigDecimal.ZERO,
        categoryBreakdown = emptyMap(),
        monthlyTrends = emptyList(),
        topTags = emptyList(),
        periodComparison = null
    )

    private fun createAnalyticsDataWithSpending(amount: BigDecimal) = AnalyticsData(
        totalSpending = amount,
        categoryBreakdown = emptyMap(),
        monthlyTrends = emptyList(),
        topTags = emptyList(),
        periodComparison = null
    )
}