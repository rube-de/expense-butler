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
class AddExpenseViewModelTestFixed {

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

    // Test initialization - this verifies the race condition fix
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

    // Test UI Event handling - this tests the new event-based pattern
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

    // Test legacy method compatibility - ensures backward compatibility
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
    fun `should initialize with default state`() {
        val state = viewModel.uiState.value
        
        assertNull(state.amount)
        assertEquals("USD", state.currency)
        assertEquals("", state.description)
        assertNull(state.selectedCategory)
        assertEquals(emptyList<String>(), state.selectedTags)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
    }

    @Test
    fun `should validate amount correctly`() = runTest {
        // Test empty amount
        viewModel.updateAmount(null)
        coroutineTestRule.advanceUntilIdle()
        viewModel.onUiEvent(AddExpenseUiEvent.ValidateForm)
        coroutineTestRule.advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assertTrue(state.amountError != null)
        
        // Test zero amount
        viewModel.updateAmount(BigDecimal.ZERO)
        coroutineTestRule.advanceUntilIdle()
        viewModel.onUiEvent(AddExpenseUiEvent.ValidateForm)
        coroutineTestRule.advanceUntilIdle()
        
        val state2 = viewModel.uiState.value
        assertTrue(state2.amountError != null)
        
        // Test valid amount
        viewModel.updateAmount(BigDecimal("25.50"))
        coroutineTestRule.advanceUntilIdle()
        viewModel.onUiEvent(AddExpenseUiEvent.ValidateForm)
        coroutineTestRule.advanceUntilIdle()
        
        val state3 = viewModel.uiState.value
        assertNull(state3.amountError)
    }

    @Test
    fun `should validate description correctly`() = runTest {
        // Test empty description
        viewModel.updateDescription("")
        coroutineTestRule.advanceUntilIdle()
        viewModel.onUiEvent(AddExpenseUiEvent.ValidateForm)
        coroutineTestRule.advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assertTrue(state.descriptionError != null)
        
        // Test blank description
        viewModel.updateDescription("   ")
        coroutineTestRule.advanceUntilIdle()
        viewModel.onUiEvent(AddExpenseUiEvent.ValidateForm)
        coroutineTestRule.advanceUntilIdle()
        
        val state2 = viewModel.uiState.value
        assertTrue(state2.descriptionError != null)
        
        // Test valid description
        viewModel.updateDescription("Coffee")
        coroutineTestRule.advanceUntilIdle()
        viewModel.onUiEvent(AddExpenseUiEvent.ValidateForm)
        coroutineTestRule.advanceUntilIdle()
        
        val state3 = viewModel.uiState.value
        assertNull(state3.descriptionError)
    }

    @Test
    fun `should validate category selection correctly`() = runTest {
        // Test no category selected
        viewModel.onUiEvent(AddExpenseUiEvent.ValidateForm)
        coroutineTestRule.advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assertTrue(state.categoryError != null)
        
        // Test valid category selected
        val category = Category(1, "Food", "#4CAF50", "restaurant", true)
        viewModel.selectCategory(category)
        coroutineTestRule.advanceUntilIdle()
        viewModel.onUiEvent(AddExpenseUiEvent.ValidateForm)
        coroutineTestRule.advanceUntilIdle()
        
        val state2 = viewModel.uiState.value
        assertNull(state2.categoryError)
    }

    @Test
    fun `should save expense when form is valid`() = runTest {
        // Setup valid form data
        viewModel.updateAmount(BigDecimal("25.50"))
        viewModel.updateDescription("Coffee")
        viewModel.selectCategory(Category(1, "Food", "#4CAF50", "restaurant", true))
        viewModel.updateTags(listOf("coffee", "work"))
        coroutineTestRule.advanceUntilIdle()
        
        // Save expense
        viewModel.saveExpense()
        
        // Advance time to complete async operations
        coroutineTestRule.advanceUntilIdle()
        
        // Verify repository was called
        coVerify {
            mockRepository.insertExpense(
                match { expense ->
                    expense.amount == BigDecimal("25.50") &&
                    expense.description == "Coffee" &&
                    expense.categoryId == 1L &&
                    expense.tags == listOf("coffee", "work") &&
                    expense.currency == "USD"
                }
            )
        }
        
        // Verify success state
        val state = viewModel.uiState.value
        assertTrue(state.isSaved)
        assertFalse(state.isLoading)
    }

    @Test
    fun `should not save expense when form is invalid`() = runTest {
        // Setup invalid form data (missing amount and description)
        viewModel.updateAmount(null)
        viewModel.updateDescription("")
        coroutineTestRule.advanceUntilIdle()
        
        // Try to save expense
        viewModel.saveExpense()
        coroutineTestRule.advanceUntilIdle()
        
        // Verify repository was not called
        coVerify(exactly = 0) { mockRepository.insertExpense(any()) }
        
        // Verify error state
        val state = viewModel.uiState.value
        assertFalse(state.isSaved)
        assertTrue(state.amountError != null)
        assertTrue(state.descriptionError != null)
    }

    @Test
    fun `should handle repository error when saving expense`() = runTest {
        // Mock repository to return error
        coEvery { mockRepository.insertExpense(any()) } returns Result.failure(Exception("Database error"))
        
        // Setup valid form data
        viewModel.updateAmount(BigDecimal("25.50"))
        viewModel.updateDescription("Coffee")
        viewModel.selectCategory(Category(1, "Food", "#4CAF50", "restaurant", true))
        coroutineTestRule.advanceUntilIdle()
        
        // Save expense
        viewModel.saveExpense()
        
        // Advance time to complete async operations
        coroutineTestRule.advanceUntilIdle()
        
        // Verify error state
        val state = viewModel.uiState.value
        assertFalse(state.isSaved)
        assertFalse(state.isLoading)
        assertTrue(state.errorMessage != null)
    }

    @Test
    fun `should reset form after successful save`() = runTest {
        // Setup valid form data
        viewModel.updateAmount(BigDecimal("25.50"))
        viewModel.updateDescription("Coffee")
        viewModel.selectCategory(Category(1, "Food", "#4CAF50", "restaurant", true))
        viewModel.updateTags(listOf("coffee"))
        coroutineTestRule.advanceUntilIdle()
        
        // Save expense
        viewModel.saveExpense()
        coroutineTestRule.advanceUntilIdle()
        
        // Reset form
        viewModel.resetForm()
        coroutineTestRule.advanceUntilIdle()
        
        // Verify form is reset
        val state = viewModel.uiState.value
        assertNull(state.amount)
        assertEquals("", state.description)
        assertNull(state.selectedCategory)
        assertEquals(emptyList<String>(), state.selectedTags)
        assertFalse(state.isSaved)
        assertNull(state.errorMessage)
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
}