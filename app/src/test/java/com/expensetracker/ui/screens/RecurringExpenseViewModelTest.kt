package com.expensetracker.ui.screens

import com.expensetracker.data.model.Category
import com.expensetracker.data.model.RecurrenceFrequency
import com.expensetracker.data.model.RecurringExpense
import com.expensetracker.data.repository.ExpenseRepository
import com.expensetracker.domain.error.UserFacingError
import com.expensetracker.domain.recurring.*
import com.expensetracker.domain.validation.AmountValidator
import com.expensetracker.domain.validation.CategoryValidator
import com.expensetracker.domain.validation.DescriptionValidator
import com.expensetracker.domain.validation.TagValidator
import com.expensetracker.domain.validation.ValidationResult
import com.expensetracker.test.rules.CoroutineTestRule
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class RecurringExpenseViewModelTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    private lateinit var mockRepository: ExpenseRepository
    private lateinit var mockGenerator: RecurringExpenseGenerator
    private lateinit var mockGenerationManager: GenerationManager
    private lateinit var mockModificationHandler: RecurringExpenseModificationHandler
    private lateinit var mockAmountValidator: AmountValidator
    private lateinit var mockDescriptionValidator: DescriptionValidator
    private lateinit var mockCategoryValidator: CategoryValidator
    private lateinit var mockTagValidator: TagValidator
    private lateinit var viewModel: RecurringExpenseViewModel

    @Before
    fun setup() {
        mockRepository = mockk(relaxed = true)
        mockGenerator = mockk(relaxed = true)
        mockGenerationManager = mockk(relaxed = true)
        mockModificationHandler = mockk(relaxed = true)
        mockAmountValidator = mockk(relaxed = true)
        mockDescriptionValidator = mockk(relaxed = true)
        mockCategoryValidator = mockk(relaxed = true)
        mockTagValidator = mockk(relaxed = true)

        // Setup default mock flows
        every { mockRepository.getAllRecurringExpenses() } returns flowOf(emptyList())
        every { mockRepository.getAllCategories() } returns flowOf(emptyList())
        every { mockGenerationManager.getLastGenerationTime() } returns System.currentTimeMillis()
        
        // Setup validator mocks to return Success by default
        every { mockAmountValidator.validate(any()) } returns ValidationResult.Success
        every { mockAmountValidator.validate(null) } returns ValidationResult.Error("Amount is required")
        every { mockDescriptionValidator.validate(any()) } returns ValidationResult.Success
        every { mockCategoryValidator.validate(any()) } returns ValidationResult.Success
        every { mockCategoryValidator.validate(null) } returns ValidationResult.Error("Category is required")
        every { mockTagValidator.validateTagList(any()) } returns ValidationResult.Success

        viewModel = RecurringExpenseViewModel(
            repository = mockRepository,
            generator = mockGenerator,
            generationManager = mockGenerationManager,
            modificationHandler = mockModificationHandler,
            amountValidator = mockAmountValidator,
            descriptionValidator = mockDescriptionValidator,
            categoryValidator = mockCategoryValidator,
            tagValidator = mockTagValidator
        )
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    // UI State Initialization Tests

    @Test
    fun `should initialize with empty state`() = runTest {
        val initialState = viewModel.uiState.value

        assertFalse("Should not be loading initially", initialState.isLoading)
        assertTrue("Recurring expenses list should be empty", initialState.recurringExpenses.isEmpty())
        assertTrue("Categories should be empty", initialState.availableCategories.isEmpty())
        assertTrue("Forecast should be empty", initialState.forecast.isEmpty())
        assertFalse("Should not be in edit mode", initialState.isEditMode)
        assertFalse("Should not include past by default", initialState.includePast)
        assertNull("No error initially", initialState.errorMessage)
    }

    @Test
    fun `should load recurring expenses on initialization`() = runTest {
        val recurringExpenses = listOf(createTestRecurringExpense(id = 1L))
        every { mockRepository.getAllRecurringExpenses() } returns flowOf(recurringExpenses)

        viewModel.onUiEvent(RecurringExpenseUiEvent.LoadInitialData)
        coroutineTestRule.advanceUntilIdle() // Wait for all coroutines to complete

        verify { mockRepository.getAllRecurringExpenses() }
    }

    @Test
    fun `should load categories on initialization`() = runTest {
        val categories = listOf(createTestCategory(id = 1L, name = "Food"))
        every { mockRepository.getAllCategories() } returns flowOf(categories)

        viewModel.onUiEvent(RecurringExpenseUiEvent.LoadInitialData)
        coroutineTestRule.advanceUntilIdle() // Wait for all coroutines to complete

        verify { mockRepository.getAllCategories() }
    }

    // Add/Edit Recurring Expense Tests

    @Test
    fun `should update amount when amount changed event received`() = runTest {
        val newAmount = BigDecimal("150.00")

        viewModel.onUiEvent(RecurringExpenseUiEvent.AmountChanged(newAmount))
        coroutineTestRule.advanceUntilIdle()

        assertEquals("Amount should be updated", newAmount, viewModel.uiState.value.amount)
        assertNull("Amount error should be cleared", viewModel.uiState.value.amountError)
    }

    @Test
    fun `should update currency when currency changed event received`() = runTest {
        val newCurrency = "EUR"

        viewModel.onUiEvent(RecurringExpenseUiEvent.CurrencyChanged(newCurrency))
        coroutineTestRule.advanceUntilIdle()

        assertEquals("Currency should be updated", newCurrency, viewModel.uiState.value.currency)
    }

    @Test
    fun `should update description when description changed event received`() = runTest {
        val newDescription = "Monthly subscription"

        viewModel.onUiEvent(RecurringExpenseUiEvent.DescriptionChanged(newDescription))
        coroutineTestRule.advanceUntilIdle()

        assertEquals("Description should be updated", newDescription, viewModel.uiState.value.description)
        assertNull("Description error should be cleared", viewModel.uiState.value.descriptionError)
    }

    @Test
    fun `should update frequency when frequency changed event received`() = runTest {
        val newFrequency = RecurrenceFrequency.WEEKLY

        viewModel.onUiEvent(RecurringExpenseUiEvent.FrequencyChanged(newFrequency))
        coroutineTestRule.advanceUntilIdle()

        assertEquals("Frequency should be updated", newFrequency, viewModel.uiState.value.frequency)
    }

    @Test
    fun `should update start date when start date changed event received`() = runTest {
        val newStartDate = LocalDate.of(2024, 6, 15)

        viewModel.onUiEvent(RecurringExpenseUiEvent.StartDateChanged(newStartDate))
        coroutineTestRule.advanceUntilIdle()

        assertEquals("Start date should be updated", newStartDate, viewModel.uiState.value.startDate)
    }

    @Test
    fun `should update end date when end date changed event received`() = runTest {
        val newEndDate = LocalDate.of(2024, 12, 31)

        viewModel.onUiEvent(RecurringExpenseUiEvent.EndDateChanged(newEndDate))
        coroutineTestRule.advanceUntilIdle()

        assertEquals("End date should be updated", newEndDate, viewModel.uiState.value.endDate)
    }

    @Test
    fun `should update selected category when category selected event received`() = runTest {
        val category = createTestCategory(id = 2L, name = "Transport")

        viewModel.onUiEvent(RecurringExpenseUiEvent.CategorySelected(category))
        coroutineTestRule.advanceUntilIdle()

        assertEquals("Selected category should be updated", category, viewModel.uiState.value.selectedCategory)
        assertNull("Category error should be cleared", viewModel.uiState.value.categoryError)
    }

    @Test
    fun `should update tags when tags changed event received`() = runTest {
        val newTags = listOf("subscription", "entertainment")

        viewModel.onUiEvent(RecurringExpenseUiEvent.TagsChanged(newTags))
        coroutineTestRule.advanceUntilIdle()

        assertEquals("Tags should be updated", newTags, viewModel.uiState.value.selectedTags)
    }

    // Save Recurring Expense Tests

    @Test
    fun `should save new recurring expense when valid data provided`() = runTest {
        setupValidFormState()
        coEvery { mockRepository.insertRecurringExpense(any()) } returns Result.success(1L)

        viewModel.onUiEvent(RecurringExpenseUiEvent.SaveRecurringExpense)
        coroutineTestRule.advanceUntilIdle()

        coVerify(exactly = 1) { mockRepository.insertRecurringExpense(any()) }
        assertTrue("Should indicate save success", viewModel.uiState.value.isSaved)
        assertFalse("Should not be loading after save", viewModel.uiState.value.isLoading)
    }

    @Test
    fun `should show loading state during save operation`() = runTest {
        setupValidFormState()
        coEvery { mockRepository.insertRecurringExpense(any()) } coAnswers {
            // Check loading state during the call
            assertTrue("Should be loading during save", viewModel.uiState.value.isLoading)
            Result.success(1L)
        }

        viewModel.onUiEvent(RecurringExpenseUiEvent.SaveRecurringExpense)
    }

    @Test
    fun `should not save when validation fails`() = runTest {
        // Don't set up valid state - leave amount as null
        viewModel.onUiEvent(RecurringExpenseUiEvent.SaveRecurringExpense)

        coVerify(exactly = 0) { mockRepository.insertRecurringExpense(any()) }
        assertFalse("Should not indicate save success", viewModel.uiState.value.isSaved)
    }

    @Test
    fun `should handle save error gracefully`() = runTest {
        setupValidFormState()
        val errorMessage = "Database error"
        coEvery { mockRepository.insertRecurringExpense(any()) } returns Result.failure(Exception(errorMessage))

        viewModel.onUiEvent(RecurringExpenseUiEvent.SaveRecurringExpense)
        coroutineTestRule.advanceUntilIdle()

        assertFalse("Should not indicate save success", viewModel.uiState.value.isSaved)
        assertNotNull("Should show error message", viewModel.uiState.value.errorMessage)
        assertFalse("Should not be loading after error", viewModel.uiState.value.isLoading)
    }

    // Edit Mode Tests

    @Test
    fun `should enter edit mode when editing recurring expense`() = runTest {
        val recurringExpense = createTestRecurringExpense(id = 1L)

        viewModel.onUiEvent(RecurringExpenseUiEvent.EditRecurringExpense(recurringExpense))
        coroutineTestRule.advanceUntilIdle()

        assertTrue("Should be in edit mode", viewModel.uiState.value.isEditMode)
        assertEquals("Should populate form with expense data", recurringExpense.amount, viewModel.uiState.value.amount)
        assertEquals("Should populate description", recurringExpense.description, viewModel.uiState.value.description)
        assertEquals("Should populate frequency", recurringExpense.frequency, viewModel.uiState.value.frequency)
    }

    @Test
    fun `should exit edit mode when canceling edit`() = runTest {
        val recurringExpense = createTestRecurringExpense(id = 1L)
        viewModel.onUiEvent(RecurringExpenseUiEvent.EditRecurringExpense(recurringExpense))

        viewModel.onUiEvent(RecurringExpenseUiEvent.CancelEdit)

        assertFalse("Should not be in edit mode", viewModel.uiState.value.isEditMode)
        assertNull("Should clear current editing expense", viewModel.uiState.value.currentEditingExpense)
    }

    @Test
    fun `should toggle include past mode`() = runTest {
        val initialIncludePast = viewModel.uiState.value.includePast

        viewModel.onUiEvent(RecurringExpenseUiEvent.ToggleIncludePast)
        coroutineTestRule.advanceUntilIdle()

        assertEquals("Include past should be toggled", !initialIncludePast, viewModel.uiState.value.includePast)
    }

    @Test
    fun `should update existing recurring expense when in edit mode`() = runTest {
        val recurringExpense = createTestRecurringExpense(id = 1L)
        viewModel.onUiEvent(RecurringExpenseUiEvent.EditRecurringExpense(recurringExpense))
        setupValidFormState()
        coEvery { mockModificationHandler.updateRecurringExpense(any(), any()) } returns Result.success(Unit)

        viewModel.onUiEvent(RecurringExpenseUiEvent.SaveRecurringExpense)
        coroutineTestRule.advanceUntilIdle()

        coVerify(exactly = 1) { mockModificationHandler.updateRecurringExpense(any(), false) }
        assertFalse("Should exit edit mode after save", viewModel.uiState.value.isEditMode)
    }

    @Test
    fun `should use include past setting when updating in edit mode`() = runTest {
        val recurringExpense = createTestRecurringExpense(id = 1L)
        viewModel.onUiEvent(RecurringExpenseUiEvent.EditRecurringExpense(recurringExpense))
        viewModel.onUiEvent(RecurringExpenseUiEvent.ToggleIncludePast) // Enable include past
        setupValidFormState()
        coEvery { mockModificationHandler.updateRecurringExpense(any(), any()) } returns Result.success(Unit)

        viewModel.onUiEvent(RecurringExpenseUiEvent.SaveRecurringExpense)
        coroutineTestRule.advanceUntilIdle()

        coVerify(exactly = 1) { mockModificationHandler.updateRecurringExpense(any(), true) }
    }

    // Delete Tests

    @Test
    fun `should delete recurring expense when delete confirmed`() = runTest {
        val expenseId = 1L
        coEvery { mockRepository.deleteRecurringExpense(expenseId) } returns Result.success(Unit)

        viewModel.onUiEvent(RecurringExpenseUiEvent.DeleteRecurringExpense(expenseId))
        coroutineTestRule.advanceUntilIdle()

        coVerify(exactly = 1) { mockRepository.deleteRecurringExpense(expenseId) }
    }

    @Test
    fun `should handle delete error gracefully`() = runTest {
        val expenseId = 1L
        coEvery { mockRepository.deleteRecurringExpense(expenseId) } returns Result.failure(Exception("Delete failed"))

        viewModel.onUiEvent(RecurringExpenseUiEvent.DeleteRecurringExpense(expenseId))
        coroutineTestRule.advanceUntilIdle()

        assertNotNull("Should show error message", viewModel.uiState.value.errorMessage)
    }

    // Forecast Tests

    @Test
    fun `should generate forecast when generate forecast event received`() = runTest {
        val recurringExpense = createTestRecurringExpense(id = 1L)
        val expectedForecast = listOf(
            ForecastedExpense(
                amount = BigDecimal("100.00"),
                currency = "USD",
                description = "Test expense",
                date = LocalDate.now().plusDays(1),
                categoryId = 1L,
                tags = emptyList(),
                sourceRecurringExpenseId = 1L
            )
        )
        coEvery { mockGenerator.generateForecast(recurringExpense, ForecastPeriod.NEXT_MONTH, any(), any()) } returns expectedForecast

        viewModel.onUiEvent(RecurringExpenseUiEvent.GenerateForecast(recurringExpense, ForecastPeriod.NEXT_MONTH))
        coroutineTestRule.advanceUntilIdle()

        assertEquals("Should update forecast", expectedForecast, viewModel.uiState.value.forecast)
        coVerify(exactly = 1) { mockGenerator.generateForecast(any(), any(), any(), any()) }
    }

    @Test
    fun `should clear forecast when clear forecast event received`() = runTest {
        // First set some forecast data
        val recurringExpense = createTestRecurringExpense(id = 1L)
        val forecast = listOf(createTestForecastedExpense())
        coEvery { mockGenerator.generateForecast(any(), any(), any(), any()) } returns forecast
        viewModel.onUiEvent(RecurringExpenseUiEvent.GenerateForecast(recurringExpense, ForecastPeriod.NEXT_MONTH))

        // Then clear it
        viewModel.onUiEvent(RecurringExpenseUiEvent.ClearForecast)

        assertTrue("Forecast should be empty", viewModel.uiState.value.forecast.isEmpty())
    }

    // Manual Sync Tests

    @Test
    fun `should trigger manual sync when manual sync event received`() = runTest {
        val generationResult = GenerationResult(hasGenerated = true, message = "Updated", timestamp = System.currentTimeMillis())
        coEvery { mockGenerationManager.generateManually() } returns generationResult

        viewModel.onUiEvent(RecurringExpenseUiEvent.ManualSync)
        coroutineTestRule.advanceUntilIdle()

        coVerify(exactly = 1) { mockGenerationManager.generateManually() }
        assertEquals("Should show sync message", generationResult.message, viewModel.uiState.value.syncMessage)
    }

    @Test
    fun `should show sync status when manual sync completes`() = runTest {
        val generationResult = GenerationResult(hasGenerated = true, message = "Expenses updated", timestamp = System.currentTimeMillis())
        coEvery { mockGenerationManager.generateManually() } returns generationResult

        viewModel.onUiEvent(RecurringExpenseUiEvent.ManualSync)
        coroutineTestRule.advanceUntilIdle()

        assertEquals("Should update sync message", "Expenses updated", viewModel.uiState.value.syncMessage)
        assertEquals("Should update last sync time", generationResult.timestamp, viewModel.uiState.value.lastSyncTime)
    }

    // Form Reset Tests

    @Test
    fun `should reset form when reset form event received`() = runTest {
        // First populate form
        setupValidFormState()

        viewModel.onUiEvent(RecurringExpenseUiEvent.ResetForm)
        coroutineTestRule.advanceUntilIdle()

        assertNull("Amount should be reset", viewModel.uiState.value.amount)
        assertEquals("Currency should be reset to default", "USD", viewModel.uiState.value.currency)
        assertEquals("Description should be reset", "", viewModel.uiState.value.description)
        assertEquals("Frequency should be reset to default", RecurrenceFrequency.MONTHLY, viewModel.uiState.value.frequency)
        assertNull("Selected category should be reset", viewModel.uiState.value.selectedCategory)
        assertTrue("Tags should be reset", viewModel.uiState.value.selectedTags.isEmpty())
        assertFalse("Should not be in edit mode", viewModel.uiState.value.isEditMode)
    }

    // Error Handling Tests

    @Test
    fun `should clear error when clear error event received`() = runTest {
        // First set an error state
        viewModel.onUiEvent(RecurringExpenseUiEvent.SaveRecurringExpense) // This should cause validation error
        coroutineTestRule.advanceUntilIdle()

        viewModel.onUiEvent(RecurringExpenseUiEvent.ClearError)
        coroutineTestRule.advanceUntilIdle()

        assertNull("Error message should be cleared", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `should handle repository exception during data loading`() = runTest {
        every { mockRepository.getAllRecurringExpenses() } throws Exception("Database error")

        viewModel.onUiEvent(RecurringExpenseUiEvent.LoadInitialData)
        coroutineTestRule.advanceUntilIdle()

        assertNotNull("Should show error message", viewModel.uiState.value.errorMessage)
    }

    // Form Validation Tests

    @Test
    fun `should validate form before saving`() = runTest {
        // Setup invalid form (missing amount)
        viewModel.onUiEvent(RecurringExpenseUiEvent.DescriptionChanged("Test"))
        coroutineTestRule.advanceUntilIdle()

        viewModel.onUiEvent(RecurringExpenseUiEvent.SaveRecurringExpense)
        coroutineTestRule.advanceUntilIdle()

        coVerify(exactly = 0) { mockRepository.insertRecurringExpense(any()) }
        assertNotNull("Should show validation error", viewModel.uiState.value.amountError)
    }

    // Helper methods for test setup

    private fun createTestRecurringExpense(
        id: Long = 1L,
        amount: BigDecimal = BigDecimal("100.00"),
        currency: String = "USD",
        description: String = "Test recurring expense",
        categoryId: Long = 1L,
        frequency: RecurrenceFrequency = RecurrenceFrequency.MONTHLY,
        startDate: LocalDate = LocalDate.now(),
        endDate: LocalDate? = null,
        tags: List<String> = emptyList()
    ): RecurringExpense {
        return RecurringExpense(
            id = id,
            amount = amount,
            currency = currency,
            description = description,
            categoryId = categoryId,
            tags = tags,
            frequency = frequency,
            startDate = startDate,
            endDate = endDate,
            lastGenerated = null,
            isActive = true
        )
    }

    private fun createTestCategory(
        id: Long = 1L,
        name: String = "Food"
    ): Category {
        return Category(
            id = id,
            name = name,
            color = "#FF5722",
            icon = "restaurant",
            isDefault = true
        )
    }

    private fun createTestForecastedExpense(): ForecastedExpense {
        return ForecastedExpense(
            amount = BigDecimal("50.00"),
            currency = "USD",
            description = "Forecasted expense",
            date = LocalDate.now().plusDays(7),
            categoryId = 1L,
            tags = emptyList(),
            sourceRecurringExpenseId = 1L
        )
    }

    private suspend fun setupValidFormState() {
        viewModel.onUiEvent(RecurringExpenseUiEvent.AmountChanged(BigDecimal("100.00")))
        viewModel.onUiEvent(RecurringExpenseUiEvent.DescriptionChanged("Valid description"))
        viewModel.onUiEvent(RecurringExpenseUiEvent.CategorySelected(createTestCategory()))
        viewModel.onUiEvent(RecurringExpenseUiEvent.FrequencyChanged(RecurrenceFrequency.MONTHLY))
        viewModel.onUiEvent(RecurringExpenseUiEvent.StartDateChanged(LocalDate.now()))
        coroutineTestRule.advanceUntilIdle()
    }
}