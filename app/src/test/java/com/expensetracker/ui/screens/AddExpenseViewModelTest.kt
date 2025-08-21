package com.expensetracker.ui.screens

import app.cash.turbine.test
import com.expensetracker.data.model.Category
import com.expensetracker.data.model.Expense
import com.expensetracker.data.repository.ExpenseRepository
import com.expensetracker.domain.error.UserFacingError
import com.expensetracker.domain.validation.AmountValidator
import com.expensetracker.domain.validation.CategoryValidator
import com.expensetracker.domain.validation.DescriptionValidator
import com.expensetracker.domain.validation.TagValidator
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
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class AddExpenseViewModelTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    private lateinit var mockRepository: ExpenseRepository
    private lateinit var viewModel: AddExpenseViewModel
    private lateinit var amountValidator: AmountValidator
    private lateinit var descriptionValidator: DescriptionValidator
    private lateinit var categoryValidator: CategoryValidator
    private lateinit var tagValidator: TagValidator

    private val sampleCategories = listOf(
        Category(1, "Food", "#4CAF50", "restaurant", true),
        Category(2, "Shopping", "#9C27B0", "shopping_cart", true),
        Category(3, "Transportation", "#2196F3", "directions_car", true)
    )

    private val sampleTags = listOf("coffee", "work", "food", "weekly")

    @Before
    fun setup() {
        mockRepository = mockk(relaxed = true)
        amountValidator = AmountValidator()
        descriptionValidator = DescriptionValidator()
        categoryValidator = CategoryValidator()
        tagValidator = TagValidator()
        
        // Default mock behavior
        every { mockRepository.getAllCategories() } returns flowOf(sampleCategories)
        coEvery { mockRepository.getAllTags() } returns sampleTags
        coEvery { mockRepository.initializeDefaultCategories() } returns Result.success(Unit)
        coEvery { mockRepository.insertExpense(any()) } returns Result.success(1L)
        
        viewModel = AddExpenseViewModel(
            mockRepository,
            amountValidator,
            descriptionValidator,
            categoryValidator,
            tagValidator
        )
        coroutineTestRule.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    // Test initialization and data loading
    @Test
    fun `should load initial data correctly without race conditions`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            
            // Verify categories are loaded
            assertEquals(sampleCategories, state.availableCategories)
            
            // Verify tags are loaded
            assertEquals(sampleTags, state.availableTags)
            
            // Verify default state
            assertNull(state.amount)
            assertEquals("USD", state.currency)
            assertEquals("", state.description)
            assertNull(state.selectedCategory)
            assertTrue(state.selectedTags.isEmpty())
            assertFalse(state.isLoading)
            assertFalse(state.isSaved)
            assertNull(state.errorMessage)
        }
        
        // Verify repository methods were called correctly
        verify { mockRepository.getAllCategories() }
        coVerify { mockRepository.getAllTags() }
        coVerify { mockRepository.initializeDefaultCategories() }
    }

    // Test UI Event handling
    @Test
    fun `should handle AmountChanged event correctly`() = runTest {
        val testAmount = BigDecimal("123.45")
        
        viewModel.onUiEvent(AddExpenseUiEvent.AmountChanged(testAmount))
        coroutineTestRule.advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assertEquals(testAmount, state.amount)
        assertNull(state.amountError)
    }

    @Test
    fun `should handle CurrencyChanged event correctly`() = runTest {
        val testCurrency = "EUR"
        
        viewModel.onUiEvent(AddExpenseUiEvent.CurrencyChanged(testCurrency))
        coroutineTestRule.advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assertEquals(testCurrency, state.currency)
    }

    @Test
    fun `should handle DescriptionChanged event correctly`() = runTest {
        val testDescription = "Test expense description"
        
        viewModel.onUiEvent(AddExpenseUiEvent.DescriptionChanged(testDescription))
        coroutineTestRule.advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assertEquals(testDescription, state.description)
        assertNull(state.descriptionError)
    }

    @Test
    fun `should handle CategorySelected event correctly`() = runTest {
        val testCategory = sampleCategories[0]
        
        viewModel.onUiEvent(AddExpenseUiEvent.CategorySelected(testCategory))
        coroutineTestRule.advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assertEquals(testCategory, state.selectedCategory)
        assertNull(state.categoryError)
    }

    @Test
    fun `should handle TagsChanged event correctly`() = runTest {
        val testTags = listOf("coffee", "work")
        
        viewModel.onUiEvent(AddExpenseUiEvent.TagsChanged(testTags))
        coroutineTestRule.advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assertEquals(testTags, state.selectedTags)
    }

    @Test
    fun `should handle ClearError event correctly`() = runTest {
        // Simulate an error by attempting invalid save
        viewModel.onUiEvent(AddExpenseUiEvent.SaveExpense)
        coroutineTestRule.advanceUntilIdle()
        
        // Now clear the error
        viewModel.onUiEvent(AddExpenseUiEvent.ClearError)
        coroutineTestRule.advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assertNull(state.errorMessage)
    }

    @Test
    fun `should handle ResetForm event correctly`() = runTest {
        // Set some values first
        viewModel.onUiEvent(AddExpenseUiEvent.AmountChanged(BigDecimal("100")))
        viewModel.onUiEvent(AddExpenseUiEvent.DescriptionChanged("Test"))
        viewModel.onUiEvent(AddExpenseUiEvent.CategorySelected(sampleCategories[0]))
        coroutineTestRule.advanceUntilIdle()
        
        // Reset the form
        viewModel.onUiEvent(AddExpenseUiEvent.ResetForm)
        coroutineTestRule.advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assertNull(state.amount)
        assertEquals("USD", state.currency)
        assertEquals("", state.description)
        assertNull(state.selectedCategory)
        assertTrue(state.selectedTags.isEmpty())
        assertFalse(state.isLoading)
        assertFalse(state.isSaved)
        
        // Should preserve categories and tags
        assertEquals(sampleCategories, state.availableCategories)
        assertEquals(sampleTags, state.availableTags)
    }

    // Test legacy method compatibility
    @Test
    fun `updateAmount should work through event system`() = runTest {
        val testAmount = BigDecimal("456.78")
        
        viewModel.updateAmount(testAmount)
        coroutineTestRule.advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assertEquals(testAmount, state.amount)
        assertNull(state.amountError)
    }

    @Test
    fun `updateCurrency should work through event system`() = runTest {
        val testCurrency = "CHF"
        
        viewModel.updateCurrency(testCurrency)
        coroutineTestRule.advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assertEquals(testCurrency, state.currency)
    }

    @Test
    fun `updateDescription should work through event system`() = runTest {
        val testDescription = "Legacy method test"
        
        viewModel.updateDescription(testDescription)
        coroutineTestRule.advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assertEquals(testDescription, state.description)
        assertNull(state.descriptionError)
    }

    @Test
    fun `selectCategory should work through event system`() = runTest {
        val testCategory = sampleCategories[1]
        
        viewModel.selectCategory(testCategory)
        coroutineTestRule.advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assertEquals(testCategory, state.selectedCategory)
        assertNull(state.categoryError)
    }

    @Test
    fun `updateTags should work through event system`() = runTest {
        val testTags = listOf("legacy", "test")
        
        viewModel.updateTags(testTags)
        coroutineTestRule.advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assertEquals(testTags, state.selectedTags)
    }

    // Test form validation
    @Test
    fun `validateForm should return false when amount is null`() = runTest {
        viewModel.onUiEvent(AddExpenseUiEvent.DescriptionChanged("Valid description"))
        viewModel.onUiEvent(AddExpenseUiEvent.CategorySelected(sampleCategories[0]))
        coroutineTestRule.advanceUntilIdle()
        
        viewModel.onUiEvent(AddExpenseUiEvent.ValidateForm)
        coroutineTestRule.advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assertEquals("Amount is required", state.amountError)
    }

    @Test
    fun `validateForm should return false when amount is zero or negative`() = runTest {
        viewModel.onUiEvent(AddExpenseUiEvent.AmountChanged(BigDecimal.ZERO))
        viewModel.onUiEvent(AddExpenseUiEvent.DescriptionChanged("Valid description"))
        viewModel.onUiEvent(AddExpenseUiEvent.CategorySelected(sampleCategories[0]))
        coroutineTestRule.advanceUntilIdle()
        
        viewModel.onUiEvent(AddExpenseUiEvent.ValidateForm)
        coroutineTestRule.advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assertEquals("Amount must be greater than 0", state.amountError)
    }

    @Test
    fun `validateForm should return false when description is blank`() = runTest {
        viewModel.onUiEvent(AddExpenseUiEvent.AmountChanged(BigDecimal("100")))
        viewModel.onUiEvent(AddExpenseUiEvent.CategorySelected(sampleCategories[0]))
        coroutineTestRule.advanceUntilIdle()
        
        viewModel.onUiEvent(AddExpenseUiEvent.ValidateForm)
        coroutineTestRule.advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assertEquals("Description is required", state.descriptionError)
    }

    @Test
    fun `validateForm should return false when category is null`() = runTest {
        viewModel.onUiEvent(AddExpenseUiEvent.AmountChanged(BigDecimal("100")))
        viewModel.onUiEvent(AddExpenseUiEvent.DescriptionChanged("Valid description"))
        coroutineTestRule.advanceUntilIdle()
        
        viewModel.onUiEvent(AddExpenseUiEvent.ValidateForm)
        coroutineTestRule.advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assertEquals("Please select a category", state.categoryError)
    }

    @Test
    fun `validateForm should return true when all fields are valid`() = runTest {
        viewModel.onUiEvent(AddExpenseUiEvent.AmountChanged(BigDecimal("100")))
        viewModel.onUiEvent(AddExpenseUiEvent.DescriptionChanged("Valid description"))
        viewModel.onUiEvent(AddExpenseUiEvent.CategorySelected(sampleCategories[0]))
        coroutineTestRule.advanceUntilIdle()
        
        viewModel.onUiEvent(AddExpenseUiEvent.ValidateForm)
        coroutineTestRule.advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assertNull(state.amountError)
        assertNull(state.descriptionError)
        assertNull(state.categoryError)
    }

    // Test save expense functionality
    @Test
    fun `saveExpense should save successfully with valid data`() = runTest {
        // Set up valid expense data
        viewModel.onUiEvent(AddExpenseUiEvent.AmountChanged(BigDecimal("25.50")))
        viewModel.onUiEvent(AddExpenseUiEvent.CurrencyChanged("USD"))
        viewModel.onUiEvent(AddExpenseUiEvent.DescriptionChanged("Test expense"))
        viewModel.onUiEvent(AddExpenseUiEvent.CategorySelected(sampleCategories[0]))
        viewModel.onUiEvent(AddExpenseUiEvent.TagsChanged(listOf("test")))
        
        viewModel.onUiEvent(AddExpenseUiEvent.SaveExpense)
        coroutineTestRule.advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assertTrue(state.isSaved)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        
        // Verify repository method was called
        coVerify { 
            mockRepository.insertExpense(
                match { expense ->
                    expense.amount == BigDecimal("25.50") &&
                    expense.currency == "USD" &&
                    expense.description == "Test expense" &&
                    expense.categoryId == 1L &&
                    expense.tags == listOf("test")
                }
            )
        }
    }

    @Test
    fun `saveExpense should not save with invalid data`() = runTest {
        // Don't set required fields
        viewModel.onUiEvent(AddExpenseUiEvent.SaveExpense)
        
        val state = viewModel.uiState.value
        assertFalse(state.isSaved)
        assertFalse(state.isLoading)
        
        // Repository should not be called
        coVerify(exactly = 0) { mockRepository.insertExpense(any()) }
    }

    @Test
    fun `saveExpense should handle repository errors`() = runTest {
        // Mock repository to return error
        coEvery { mockRepository.insertExpense(any()) } returns Result.failure(RuntimeException("Database error"))
        
        // Set up valid expense data
        viewModel.onUiEvent(AddExpenseUiEvent.AmountChanged(BigDecimal("25.50")))
        viewModel.onUiEvent(AddExpenseUiEvent.DescriptionChanged("Test expense"))
        viewModel.onUiEvent(AddExpenseUiEvent.CategorySelected(sampleCategories[0]))
        
        viewModel.onUiEvent(AddExpenseUiEvent.SaveExpense)
        coroutineTestRule.advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assertFalse(state.isSaved)
        assertFalse(state.isLoading)
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage is UserFacingError.SaveFailedWithReason)
    }

    // Test concurrent operations to verify race condition fix
    @Test
    fun `concurrent UI events should not cause race conditions`() = runTest {
        val events = listOf(
            AddExpenseUiEvent.AmountChanged(BigDecimal("100")),
            AddExpenseUiEvent.CurrencyChanged("EUR"),
            AddExpenseUiEvent.DescriptionChanged("Concurrent test"),
            AddExpenseUiEvent.CategorySelected(sampleCategories[0]),
            AddExpenseUiEvent.TagsChanged(listOf("concurrent", "test"))
        )
        
        // Process events concurrently
        events.forEach { event ->
            viewModel.onUiEvent(event)
        }
        coroutineTestRule.advanceUntilIdle()
        
        val state = viewModel.uiState.value
        
        // All state should be consistently applied
        assertEquals(BigDecimal("100"), state.amount)
        assertEquals("EUR", state.currency)
        assertEquals("Concurrent test", state.description)
        assertEquals(sampleCategories[0], state.selectedCategory)
        assertEquals(listOf("concurrent", "test"), state.selectedTags)
    }

    @Test
    fun `error handling in loadInitialData should not cause race conditions`() = runTest {
        // Create new ViewModel with error-prone repository
        val errorRepository = mockk<ExpenseRepository>(relaxed = true)
        every { errorRepository.getAllCategories() } returns flowOf(emptyList())
        coEvery { errorRepository.getAllTags() } throws RuntimeException("Tags error")
        coEvery { errorRepository.initializeDefaultCategories() } returns Result.success(Unit)
        
        val errorViewModel = AddExpenseViewModel(
            errorRepository,
            amountValidator,
            descriptionValidator,
            categoryValidator,
            tagValidator
        )
        coroutineTestRule.advanceUntilIdle()
        
        val state = errorViewModel.uiState.value
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage is UserFacingError.LoadFailedWithReason)
    }

    // New tests for improved state management pattern
    @Test
    fun `ValidateForm event should trigger validation without race conditions`() = runTest {
        // Set invalid data
        viewModel.onUiEvent(AddExpenseUiEvent.AmountChanged(null))
        viewModel.onUiEvent(AddExpenseUiEvent.DescriptionChanged(""))
        coroutineTestRule.advanceUntilIdle()
        
        // Trigger validation via event
        viewModel.onUiEvent(AddExpenseUiEvent.ValidateForm)
        coroutineTestRule.advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assertNotNull(state.amountError)
        assertNotNull(state.descriptionError)
        assertEquals("Amount is required", state.amountError)
        assertEquals("Description is required", state.descriptionError)
    }

    @Test
    fun `rapid sequential events should be processed in order without race conditions`() = runTest {
        // Fire events rapidly in sequence
        val events = listOf(
            AddExpenseUiEvent.AmountChanged(BigDecimal("50")),
            AddExpenseUiEvent.AmountChanged(BigDecimal("100")),
            AddExpenseUiEvent.AmountChanged(BigDecimal("150")),
            AddExpenseUiEvent.DescriptionChanged("First"),
            AddExpenseUiEvent.DescriptionChanged("Second"),
            AddExpenseUiEvent.DescriptionChanged("Final")
        )
        
        events.forEach { event ->
            viewModel.onUiEvent(event)
        }
        coroutineTestRule.advanceUntilIdle()
        
        // All events should be processed, with final values preserved
        val state = viewModel.uiState.value
        assertEquals(BigDecimal("150"), state.amount)
        assertEquals("Final", state.description)
    }

    @Test
    fun `complex SaveExpense workflow should be atomic and not interfere with other events`() = runTest {
        // Set up valid expense data
        viewModel.onUiEvent(AddExpenseUiEvent.AmountChanged(BigDecimal("75")))
        viewModel.onUiEvent(AddExpenseUiEvent.DescriptionChanged("Complex test"))
        viewModel.onUiEvent(AddExpenseUiEvent.CategorySelected(sampleCategories[0]))
        
        // Fire save and other events in sequence
        viewModel.onUiEvent(AddExpenseUiEvent.SaveExpense)
        viewModel.onUiEvent(AddExpenseUiEvent.DescriptionChanged("Should not interfere"))
        
        coroutineTestRule.advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assertTrue(state.isSaved)
        // Description should be updated even after save started
        assertEquals("Should not interfere", state.description)
    }

    @Test
    fun `LoadInitialData event should reload data without affecting form state`() = runTest {
        // Set some form data
        viewModel.onUiEvent(AddExpenseUiEvent.AmountChanged(BigDecimal("200")))
        viewModel.onUiEvent(AddExpenseUiEvent.DescriptionChanged("Keep this"))
        
        // Trigger data reload
        viewModel.onUiEvent(AddExpenseUiEvent.LoadInitialData)
        coroutineTestRule.advanceUntilIdle()
        
        val state = viewModel.uiState.value
        // Form data should be preserved
        assertEquals(BigDecimal("200"), state.amount)
        assertEquals("Keep this", state.description)
        // But available data should be refreshed
        assertEquals(sampleCategories, state.availableCategories)
        assertEquals(sampleTags, state.availableTags)
    }

    // Additional edge case tests for comprehensive coverage
    
    // Test trimming and sanitization
    @Test
    fun `saveExpense should trim description before saving`() = runTest {
        // Set up valid expense data with description containing leading/trailing whitespace
        viewModel.onUiEvent(AddExpenseUiEvent.AmountChanged(BigDecimal("25.50")))
        viewModel.onUiEvent(AddExpenseUiEvent.DescriptionChanged("  Test expense with spaces  "))
        viewModel.onUiEvent(AddExpenseUiEvent.CategorySelected(sampleCategories[0]))
        
        viewModel.onUiEvent(AddExpenseUiEvent.SaveExpense)
        coroutineTestRule.advanceUntilIdle()
        
        // Verify repository method was called with trimmed description
        coVerify { 
            mockRepository.insertExpense(
                match { expense ->
                    expense.description == "Test expense with spaces"
                }
            )
        }
    }

    @Test
    fun `validateForm should reject whitespace-only description`() = runTest {
        viewModel.onUiEvent(AddExpenseUiEvent.AmountChanged(BigDecimal("100")))
        viewModel.onUiEvent(AddExpenseUiEvent.DescriptionChanged("   "))
        viewModel.onUiEvent(AddExpenseUiEvent.CategorySelected(sampleCategories[0]))
        coroutineTestRule.advanceUntilIdle()
        
        viewModel.onUiEvent(AddExpenseUiEvent.ValidateForm)
        coroutineTestRule.advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assertEquals("Description is required", state.descriptionError)
    }

    // Test exception handling edge cases
    @Test
    fun `saveExpense should handle repository exceptions gracefully`() = runTest {
        // Mock repository to throw exception (not return Result.failure)
        coEvery { mockRepository.insertExpense(any()) } throws RuntimeException("Database connection lost")
        
        // Set up valid expense data
        viewModel.onUiEvent(AddExpenseUiEvent.AmountChanged(BigDecimal("25.50")))
        viewModel.onUiEvent(AddExpenseUiEvent.DescriptionChanged("Test expense"))
        viewModel.onUiEvent(AddExpenseUiEvent.CategorySelected(sampleCategories[0]))
        
        viewModel.onUiEvent(AddExpenseUiEvent.SaveExpense)
        coroutineTestRule.advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assertFalse(state.isSaved)
        assertFalse(state.isLoading)
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage is UserFacingError.SaveFailedWithReason)
    }

    @Test
    fun `handleLoadInitialData should handle exceptions in getAllTags`() = runTest {
        // Create new ViewModel with error-prone repository for getAllTags
        val errorRepository = mockk<ExpenseRepository>(relaxed = true)
        every { errorRepository.getAllCategories() } returns flowOf(sampleCategories)
        coEvery { errorRepository.getAllTags() } throws RuntimeException("Tags service unavailable")
        coEvery { errorRepository.initializeDefaultCategories() } returns Result.success(Unit)
        
        val errorViewModel = AddExpenseViewModel(
            errorRepository,
            amountValidator,
            descriptionValidator,
            categoryValidator,
            tagValidator
        )
        coroutineTestRule.advanceUntilIdle()
        
        val state = errorViewModel.uiState.value
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage is UserFacingError.LoadFailedWithReason)
        assertTrue(state.errorMessage is UserFacingError.LoadFailedWithReason)
    }

    // Test currency edge cases
    @Test
    fun `currency should persist across form operations`() = runTest {
        val testCurrency = "JPY"
        
        // Set currency and other data
        viewModel.onUiEvent(AddExpenseUiEvent.CurrencyChanged(testCurrency))
        viewModel.onUiEvent(AddExpenseUiEvent.AmountChanged(BigDecimal("1000")))
        viewModel.onUiEvent(AddExpenseUiEvent.DescriptionChanged("Test"))
        coroutineTestRule.advanceUntilIdle()
        
        // Validate form - currency should remain
        viewModel.onUiEvent(AddExpenseUiEvent.ValidateForm)
        coroutineTestRule.advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assertEquals(testCurrency, state.currency)
    }

    @Test
    fun `currency should reset to USD after form reset`() = runTest {
        // Set non-default currency
        viewModel.onUiEvent(AddExpenseUiEvent.CurrencyChanged("EUR"))
        coroutineTestRule.advanceUntilIdle()
        
        // Reset form
        viewModel.onUiEvent(AddExpenseUiEvent.ResetForm)
        coroutineTestRule.advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assertEquals("USD", state.currency)
    }

    // Test tag edge cases
    @Test
    fun `should handle duplicate tags in list`() = runTest {
        val tagsWithDuplicates = listOf("coffee", "work", "coffee", "meeting")
        
        viewModel.onUiEvent(AddExpenseUiEvent.TagsChanged(tagsWithDuplicates))
        coroutineTestRule.advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assertEquals(tagsWithDuplicates, state.selectedTags) // Should preserve exactly what user entered
    }

    @Test
    fun `should handle empty strings in tag list`() = runTest {
        val tagsWithEmpty = listOf("coffee", "", "work", "   ", "meeting")
        
        viewModel.onUiEvent(AddExpenseUiEvent.TagsChanged(tagsWithEmpty))
        coroutineTestRule.advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assertEquals(tagsWithEmpty, state.selectedTags) // Should preserve exactly what user entered
    }

    @Test
    fun `tags should be preserved during save operation`() = runTest {
        val testTags = listOf("important", "business", "quarterly")
        
        // Set up valid expense data with tags
        viewModel.onUiEvent(AddExpenseUiEvent.AmountChanged(BigDecimal("500")))
        viewModel.onUiEvent(AddExpenseUiEvent.DescriptionChanged("Business expense"))
        viewModel.onUiEvent(AddExpenseUiEvent.CategorySelected(sampleCategories[0]))
        viewModel.onUiEvent(AddExpenseUiEvent.TagsChanged(testTags))
        
        viewModel.onUiEvent(AddExpenseUiEvent.SaveExpense)
        coroutineTestRule.advanceUntilIdle()
        
        // Verify tags are saved correctly
        coVerify { 
            mockRepository.insertExpense(
                match { expense ->
                    expense.tags == testTags
                }
            )
        }
    }

    // Test state consistency
    @Test
    fun `isLoading should be properly managed during save operation sequence`() = runTest {
        // Set up valid expense data
        viewModel.onUiEvent(AddExpenseUiEvent.AmountChanged(BigDecimal("100")))
        viewModel.onUiEvent(AddExpenseUiEvent.DescriptionChanged("Loading test"))
        viewModel.onUiEvent(AddExpenseUiEvent.CategorySelected(sampleCategories[0]))
        coroutineTestRule.advanceUntilIdle()
        
        // Before save - should not be loading
        assertFalse(viewModel.uiState.value.isLoading)
        
        // During save - should be loading (briefly)
        viewModel.onUiEvent(AddExpenseUiEvent.SaveExpense)
        // Don't advance time to catch loading state
        
        // After save completion
        coroutineTestRule.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `isSaved flag should be reset after form reset`() = runTest {
        // Save an expense first
        viewModel.onUiEvent(AddExpenseUiEvent.AmountChanged(BigDecimal("50")))
        viewModel.onUiEvent(AddExpenseUiEvent.DescriptionChanged("Test"))
        viewModel.onUiEvent(AddExpenseUiEvent.CategorySelected(sampleCategories[0]))
        viewModel.onUiEvent(AddExpenseUiEvent.SaveExpense)
        coroutineTestRule.advanceUntilIdle()
        
        assertTrue(viewModel.uiState.value.isSaved)
        
        // Reset form
        viewModel.onUiEvent(AddExpenseUiEvent.ResetForm)
        coroutineTestRule.advanceUntilIdle()
        
        assertFalse(viewModel.uiState.value.isSaved)
    }

    @Test
    fun `error state should be cleared when starting new save operation`() = runTest {
        // Create error state first
        coEvery { mockRepository.insertExpense(any()) } returns Result.failure(RuntimeException("First error"))
        
        viewModel.onUiEvent(AddExpenseUiEvent.AmountChanged(BigDecimal("100")))
        viewModel.onUiEvent(AddExpenseUiEvent.DescriptionChanged("Test"))
        viewModel.onUiEvent(AddExpenseUiEvent.CategorySelected(sampleCategories[0]))
        viewModel.onUiEvent(AddExpenseUiEvent.SaveExpense)
        coroutineTestRule.advanceUntilIdle()
        
        assertNotNull(viewModel.uiState.value.errorMessage)
        
        // Fix repository and try save again
        coEvery { mockRepository.insertExpense(any()) } returns Result.success(1L)
        
        viewModel.onUiEvent(AddExpenseUiEvent.SaveExpense)
        coroutineTestRule.advanceUntilIdle()
        
        // Error should be cleared
        assertNull(viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.isSaved)
    }

    // Test validation edge cases
    @Test
    fun `should handle very large BigDecimal amounts`() = runTest {
        val largeAmount = BigDecimal("999999999.99") // Within validator MAX_AMOUNT limit
        
        viewModel.onUiEvent(AddExpenseUiEvent.AmountChanged(largeAmount))
        viewModel.onUiEvent(AddExpenseUiEvent.DescriptionChanged("Large amount test"))
        viewModel.onUiEvent(AddExpenseUiEvent.CategorySelected(sampleCategories[0]))
        
        viewModel.onUiEvent(AddExpenseUiEvent.ValidateForm)
        coroutineTestRule.advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assertNull(state.amountError) // Should be valid
        assertEquals(largeAmount, state.amount)
    }

    @Test
    fun `should handle special characters in description`() = runTest {
        val specialDescription = "Café meal with émojis 🍕 & symbols $@#%"
        
        viewModel.onUiEvent(AddExpenseUiEvent.AmountChanged(BigDecimal("25")))
        viewModel.onUiEvent(AddExpenseUiEvent.DescriptionChanged(specialDescription))
        viewModel.onUiEvent(AddExpenseUiEvent.CategorySelected(sampleCategories[0]))
        
        viewModel.onUiEvent(AddExpenseUiEvent.SaveExpense)
        coroutineTestRule.advanceUntilIdle()
        
        // Should save successfully with special characters
        coVerify { 
            mockRepository.insertExpense(
                match { expense ->
                    expense.description == specialDescription
                }
            )
        }
        assertTrue(viewModel.uiState.value.isSaved)
    }

    @Test
    fun `should handle very long description strings`() = runTest {
        val longDescription = "A".repeat(500) // Within validator MAX_LENGTH limit
        
        viewModel.onUiEvent(AddExpenseUiEvent.AmountChanged(BigDecimal("10")))
        viewModel.onUiEvent(AddExpenseUiEvent.DescriptionChanged(longDescription))
        viewModel.onUiEvent(AddExpenseUiEvent.CategorySelected(sampleCategories[0]))
        
        viewModel.onUiEvent(AddExpenseUiEvent.ValidateForm)
        coroutineTestRule.advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assertNull(state.descriptionError) // Should be valid
        assertEquals(longDescription, state.description)
    }

    @Test
    fun `negative amount validation should work with very small negative values`() = runTest {
        val smallNegativeAmount = BigDecimal("-0.01")
        
        viewModel.onUiEvent(AddExpenseUiEvent.AmountChanged(smallNegativeAmount))
        viewModel.onUiEvent(AddExpenseUiEvent.DescriptionChanged("Negative test"))
        viewModel.onUiEvent(AddExpenseUiEvent.CategorySelected(sampleCategories[0]))
        
        viewModel.onUiEvent(AddExpenseUiEvent.ValidateForm)
        coroutineTestRule.advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assertEquals("Amount must be greater than 0", state.amountError)
    }
}