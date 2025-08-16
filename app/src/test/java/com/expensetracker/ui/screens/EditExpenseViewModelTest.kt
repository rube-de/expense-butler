package com.expensetracker.ui.screens

import com.expensetracker.data.model.Category
import com.expensetracker.data.model.Expense
import com.expensetracker.data.repository.ExpenseRepository
import io.mockk.*
import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class EditExpenseViewModelTest {

    private lateinit var mockRepository: ExpenseRepository
    private lateinit var viewModel: EditExpenseViewModel
    private val testDispatcher = StandardTestDispatcher()

    private val sampleExpense = Expense(
        id = 1,
        amount = BigDecimal("25.50"),
        currency = "USD",
        description = "Coffee",
        categoryId = 1,
        tags = listOf("coffee", "work"),
        date = LocalDateTime.of(2024, 1, 15, 9, 30)
    )

    private val sampleCategories = listOf(
        Category(1, "Food", "#4CAF50", "restaurant", true),
        Category(2, "Shopping", "#9C27B0", "shopping_cart", true)
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        mockRepository = mockk(relaxed = true)
        
        // Default mock behavior
        every { mockRepository.getExpenseById(1L) } returns flowOf(sampleExpense)
        every { mockRepository.getAllCategories() } returns flowOf(sampleCategories)
        coEvery { mockRepository.getAllTags() } returns listOf("coffee", "work", "food", "weekly")
        coEvery { mockRepository.updateExpense(any()) } returns Result.success(Unit)
        coEvery { mockRepository.deleteExpense(any()) } returns Result.success(Unit)
        
        val savedStateHandle = SavedStateHandle(mapOf("expenseId" to 1L))
        viewModel = EditExpenseViewModel(mockRepository, savedStateHandle)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `should load expense data on initialization`() {
        val state = viewModel.uiState.value
        
        assertEquals(sampleExpense.amount, state.amount)
        assertEquals(sampleExpense.currency, state.currency)
        assertEquals(sampleExpense.description, state.description)
        assertEquals(sampleExpense.tags, state.selectedTags)
        assertFalse(state.isLoading)
    }

    @Test
    fun `should load categories on initialization`() {
        val state = viewModel.uiState.value
        
        assertEquals(sampleCategories, state.availableCategories)
        assertEquals(sampleCategories[0], state.selectedCategory) // First category matches expense categoryId
    }

    @Test
    fun `should update expense successfully`() = runTest {
        // Update expense data
        viewModel.updateAmount(BigDecimal("30.00"))
        viewModel.updateDescription("Updated Coffee")
        
        // Save expense
        viewModel.updateExpense()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify repository was called with updated data
        coVerify {
            mockRepository.updateExpense(
                match { expense ->
                    expense.id == 1L &&
                    expense.amount == BigDecimal("30.00") &&
                    expense.description == "Updated Coffee"
                }
            )
        }
        
        // Verify success state
        val state = viewModel.uiState.value
        assertTrue(state.isUpdated)
        assertFalse(state.isLoading)
    }

    @Test
    fun `should handle update expense error`() = runTest {
        val errorMessage = "Failed to update expense"
        coEvery { mockRepository.updateExpense(any()) } returns Result.failure(Exception(errorMessage))
        
        viewModel.updateExpense()
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assertFalse(state.isUpdated)
        assertEquals("Failed to update expense: $errorMessage", state.errorMessage)
    }

    @Test
    fun `should delete expense successfully`() = runTest {
        viewModel.deleteExpense()
        testDispatcher.scheduler.advanceUntilIdle()
        
        coVerify { mockRepository.deleteExpense(1L) }
        
        val state = viewModel.uiState.value
        assertTrue(state.isDeleted)
        assertFalse(state.isLoading)
    }

    @Test
    fun `should handle delete expense error`() = runTest {
        val errorMessage = "Failed to delete expense"
        coEvery { mockRepository.deleteExpense(1L) } returns Result.failure(Exception(errorMessage))
        
        viewModel.deleteExpense()
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModel.uiState.value
        assertFalse(state.isDeleted)
        assertEquals("Failed to delete expense: $errorMessage", state.errorMessage)
    }

    @Test
    fun `should validate form correctly`() {
        // Test valid form
        viewModel.updateAmount(BigDecimal("25.50"))
        viewModel.updateDescription("Coffee")
        viewModel.selectCategory(sampleCategories[0])
        
        val isValid = viewModel.validateForm()
        assertTrue(isValid)
        
        val state = viewModel.uiState.value
        assertNull(state.amountError)
        assertNull(state.descriptionError)
        assertNull(state.categoryError)
    }

    @Test
    fun `should validate amount correctly`() {
        // Test empty amount
        viewModel.updateAmount(null)
        val isValid1 = viewModel.validateForm()
        assertFalse(isValid1)
        
        val state1 = viewModel.uiState.value
        assertNotNull(state1.amountError)
        
        // Test zero amount
        viewModel.updateAmount(BigDecimal.ZERO)
        val isValid2 = viewModel.validateForm()
        assertFalse(isValid2)
        
        val state2 = viewModel.uiState.value
        assertNotNull(state2.amountError)
        
        // Test valid amount
        viewModel.updateAmount(BigDecimal("25.50"))
        viewModel.validateForm()
        
        val state3 = viewModel.uiState.value
        assertNull(state3.amountError)
    }

    @Test
    fun `should validate description correctly`() {
        // Test empty description
        viewModel.updateDescription("")
        val isValid1 = viewModel.validateForm()
        assertFalse(isValid1)
        
        val state1 = viewModel.uiState.value
        assertNotNull(state1.descriptionError)
        
        // Test valid description
        viewModel.updateDescription("Coffee")
        viewModel.validateForm()
        
        val state2 = viewModel.uiState.value
        assertNull(state2.descriptionError)
    }

    @Test
    fun `should handle expense not found`() {
        // Mock expense not found
        every { mockRepository.getExpenseById(999L) } returns flowOf(null)
        
        val savedStateHandleNotFound = SavedStateHandle(mapOf("expenseId" to 999L))
        val viewModelNotFound = EditExpenseViewModel(mockRepository, savedStateHandleNotFound)
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = viewModelNotFound.uiState.value
        assertEquals("Expense not found", state.errorMessage)
    }

    @Test
    fun `should not update expense when form is invalid`() = runTest {
        // Set invalid form data
        viewModel.updateAmount(null)
        viewModel.updateDescription("")
        
        viewModel.updateExpense()
        
        // Verify repository was not called
        coVerify(exactly = 0) { mockRepository.updateExpense(any()) }
        
        val state = viewModel.uiState.value
        assertFalse(state.isUpdated)
        assertNotNull(state.amountError)
        assertNotNull(state.descriptionError)
    }
}