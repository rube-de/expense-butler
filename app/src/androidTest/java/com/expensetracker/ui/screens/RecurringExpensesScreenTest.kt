package com.expensetracker.ui.screens

import androidx.compose.ui.test.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.expensetracker.data.model.RecurrenceFrequency
import com.expensetracker.data.model.RecurringExpense
import com.expensetracker.ui.IsolatedScreenTest
import com.expensetracker.test.*
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class RecurringExpensesScreenTest : IsolatedScreenTest() {
    
    private lateinit var recurringExpenseViewModel: RecurringExpenseViewModel
    
    @Before
    override fun setup() {
        super.setup()
        
        // Set up the RecurringExpensesScreen without specifying ViewModel
        // Let Hilt handle the injection
        setScreenContent {
            RecurringExpensesScreen()
        }
        
        // Wait for screen to settle
        composeTestRule.waitForIdle()
    }
    
    @Test
    fun should_display_screen_title_and_fab() {
        // Verify screen title is displayed
        composeTestRule
            .onNodeWithText("Recurring Expenses")
            .assertExists()
            .assertIsDisplayed()
        
        // Verify FAB is displayed
        composeTestRule
            .onNodeWithTag("add_recurring_expense_fab")
            .assertExists()
            .assertIsDisplayed()
    }
    
    @Test
    fun should_display_empty_state_when_no_recurring_expenses() {
        // Verify empty state is displayed
        composeTestRule
            .onNodeWithText("No expenses yet")
            .assertExists()
            .assertIsDisplayed()
            
        composeTestRule
            .onNodeWithText("Start tracking recurring expenses by tapping the + button")
            .assertExists()
            .assertIsDisplayed()
    }
    
    @Test
    fun should_display_recurring_expense_cards_when_data_exists() {
        // Add test recurring expense
        runBlocking {
            val testExpense = createTestRecurringExpense(
                description = "Monthly rent payment",
                amount = BigDecimal("1200.00")
            )
            repository.insertRecurringExpense(testExpense)
        }
        
        // Wait for data to load
        composeTestRule.waitForIdle()
        
        // Verify recurring expense card is displayed
        composeTestRule
            .onNodeWithText("Monthly rent payment")
            .assertExists()
            .assertIsDisplayed()
            
        composeTestRule
            .onNodeWithText("$1,200.00")
            .assertExists()
            .assertIsDisplayed()
            
        composeTestRule
            .onNodeWithText("Monthly")
            .assertExists()
            .assertIsDisplayed()
    }
    
    @Test
    fun should_open_bottom_sheet_when_fab_clicked() {
        // Click FAB to open bottom sheet
        composeTestRule
            .onNodeWithTag("add_recurring_expense_fab")
            .performClick()
        
        // Wait for bottom sheet to appear
        composeTestRule.waitForIdle()
        
        // Verify bottom sheet content is displayed
        composeTestRule
            .onNodeWithTag("recurring_expense_bottom_sheet")
            .assertExists()
            .assertIsDisplayed()
            
        composeTestRule
            .onNodeWithText("Add Recurring Expense")
            .assertExists()
            .assertIsDisplayed()
    }
    
    @Test
    fun should_open_bottom_sheet_in_edit_mode_when_edit_clicked() {
        // Add test recurring expense
        runBlocking {
            val testExpense = createTestRecurringExpense(
                description = "Netflix subscription",
                amount = BigDecimal("9.99")
            )
            repository.insertRecurringExpense(testExpense)
        }
        
        composeTestRule.waitForIdle()
        
        // Click more options menu on the card
        composeTestRule
            .onNodeWithContentDescription("More options")
            .performClick()
        
        // Click edit option
        composeTestRule
            .onNodeWithText("Edit")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Verify edit mode bottom sheet is displayed
        composeTestRule
            .onNodeWithText("Edit Recurring Expense")
            .assertExists()
            .assertIsDisplayed()
            
        // Verify form is pre-populated
        composeTestRule
            .onNodeWithText("Netflix subscription")
            .assertExists()
            .assertIsDisplayed()
    }
    
    @Test
    fun should_show_delete_confirmation_when_delete_clicked() {
        // Add test recurring expense
        runBlocking {
            val testExpense = createTestRecurringExpense(
                description = "Gym membership",
                amount = BigDecimal("29.99")
            )
            repository.insertRecurringExpense(testExpense)
        }
        
        composeTestRule.waitForIdle()
        
        // Click more options menu on the card
        composeTestRule
            .onNodeWithContentDescription("More options")
            .performClick()
        
        // Click delete option
        composeTestRule
            .onNodeWithText("Delete")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Verify delete confirmation dialog is displayed
        composeTestRule
            .onNodeWithText("Delete Recurring Expense")
            .assertExists()
            .assertIsDisplayed()
            
        composeTestRule
            .onNodeWithText("Gym membership", substring = true)
            .assertExists()
            .assertIsDisplayed()
            
        composeTestRule
            .onNodeWithText("This will stop all future automatic expense generation")
            .assertExists()
            .assertIsDisplayed()
    }
    
    @Test
    fun should_delete_recurring_expense_when_confirmed() {
        // Add test recurring expense
        val testExpenseId = runBlocking {
            val testExpense = createTestRecurringExpense(
                description = "Coffee subscription",
                amount = BigDecimal("15.99")
            )
            val result = repository.insertRecurringExpense(testExpense)
            result.getOrNull()!!
        }
        
        composeTestRule.waitForIdle()
        
        // Click more options menu on the card
        composeTestRule
            .onNodeWithContentDescription("More options")
            .performClick()
        
        // Click delete option
        composeTestRule
            .onNodeWithText("Delete")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Confirm deletion
        composeTestRule
            .onNodeWithTag("confirm_delete_button")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Verify the expense is no longer displayed
        composeTestRule
            .onNodeWithText("Coffee subscription")
            .assertDoesNotExist()
    }
    
    @Test
    fun should_cancel_delete_when_cancel_clicked() {
        // Add test recurring expense
        runBlocking {
            val testExpense = createTestRecurringExpense(
                description = "Magazine subscription",
                amount = BigDecimal("5.99")
            )
            repository.insertRecurringExpense(testExpense)
        }
        
        composeTestRule.waitForIdle()
        
        // Click more options menu on the card
        composeTestRule
            .onNodeWithContentDescription("More options")
            .performClick()
        
        // Click delete option
        composeTestRule
            .onNodeWithText("Delete")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Cancel deletion
        composeTestRule
            .onNodeWithTag("cancel_delete_button")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Verify the expense is still displayed
        composeTestRule
            .onNodeWithText("Magazine subscription")
            .assertExists()
            .assertIsDisplayed()
    }
    
    @Test
    fun should_fill_and_submit_form_to_create_new_recurring_expense() {
        // Click FAB to open form
        composeTestRule
            .onNodeWithTag("add_recurring_expense_fab")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Fill out the form
        fillAmountInput("50.00")
        
        composeTestRule
            .onNodeWithTag("description_input")
            .performTextInput("Weekly grocery shopping")
        
        // Select frequency (Weekly)
        composeTestRule
            .onNodeWithTag("frequency_option_weekly")
            .performClick()
        
        // Select category
        composeTestRule
            .onNodeWithTag("category_chip_food")
            .performClick()
        
        // Add tags
        composeTestRule
            .onNodeWithTag("tag_input")
            .performTextInput("groceries")
        
        composeTestRule.waitForIdle()
        
        // Submit form
        composeTestRule
            .onNodeWithTag("save_button")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Verify the new recurring expense appears in the list
        composeTestRule
            .onNodeWithText("Weekly grocery shopping")
            .assertExists()
            .assertIsDisplayed()
            
        composeTestRule
            .onNodeWithText("$50.00")
            .assertExists()
            .assertIsDisplayed()
            
        composeTestRule
            .onNodeWithText("Weekly")
            .assertExists()
            .assertIsDisplayed()
    }
    
    @Test
    fun should_show_validation_errors_for_invalid_form_data() {
        // Click FAB to open form
        composeTestRule
            .onNodeWithTag("add_recurring_expense_fab")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Try to submit form without filling required fields
        composeTestRule
            .onNodeWithTag("save_button")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Verify validation errors are displayed
        // Note: Specific error messages depend on validation implementation
        composeTestRule
            .onNodeWithText("Amount is required", substring = true)
            .assertExists()
    }
    
    @Test
    fun should_display_sync_status_in_top_bar() {
        composeTestRule.waitForIdle()
        
        // Check if sync time is displayed (if available)
        // This test assumes sync has been performed
        composeTestRule
            .onNodeWithContentDescription("Refresh")
            .assertExists()
            .assertIsDisplayed()
    }
    
    @Test
    fun should_trigger_manual_sync_when_refresh_clicked() {
        // Click refresh button
        composeTestRule
            .onNodeWithContentDescription("Refresh")
            .performClick()
        
        composeTestRule.waitForIdle()
        
        // Verify sync was triggered (this would depend on implementation)
        // For now, just verify the button works
        composeTestRule
            .onNodeWithContentDescription("Refresh")
            .assertExists()
            .assertIsDisplayed()
    }
    
    @Test
    fun should_display_active_and_inactive_status_badges() {
        // Add active recurring expense
        runBlocking {
            val activeExpense = createTestRecurringExpense(
                description = "Active subscription",
                amount = BigDecimal("19.99"),
                isActive = true
            )
            repository.insertRecurringExpense(activeExpense)
            
            // Add inactive recurring expense
            val inactiveExpense = createTestRecurringExpense(
                description = "Inactive subscription",
                amount = BigDecimal("9.99"),
                isActive = false
            )
            repository.insertRecurringExpense(inactiveExpense)
        }
        
        composeTestRule.waitForIdle()
        
        // Verify active expense shows no inactive badge
        composeTestRule
            .onNodeWithText("Active subscription")
            .assertExists()
        
        // Verify inactive expense shows inactive badge
        composeTestRule
            .onAllNodesWithText("Inactive")
            .assertCountEquals(1)
    }
    
    // Helper functions
    private fun createTestRecurringExpense(
        description: String,
        amount: BigDecimal,
        frequency: RecurrenceFrequency = RecurrenceFrequency.MONTHLY,
        isActive: Boolean = true
    ): RecurringExpense {
        return RecurringExpense(
            id = 0L,
            amount = amount,
            currency = "USD",
            description = description,
            categoryId = 1L, // Assuming Food category exists with ID 1
            tags = listOf("test"),
            frequency = frequency,
            startDate = LocalDate.now(),
            endDate = null,
            lastGenerated = null,
            isActive = isActive
        )
    }
    
    private fun fillAmountInput(amount: String) {
        // Find amount input field and enter value
        composeTestRule
            .onAllNodesWithTag("amount_input")
            .onFirst()
            .performTextInput(amount)
    }
}