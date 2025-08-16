package com.expensetracker.ui.screens

import com.expensetracker.data.model.Category
import com.expensetracker.data.model.Expense
import com.expensetracker.data.repository.ExpenseRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class AddExpenseViewModelTest {

    private lateinit var repository: ExpenseRepository
    private lateinit var viewModel: AddExpenseViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        repository = mockk(relaxed = true)
        
        // Mock default categories
        val defaultCategories = listOf(
            Category(1, "Food", "#4CAF50", "restaurant", true),
            Category(2, "Travel", "#2196F3", "flight", true),
            Category(3, "Entertainment", "#FF9800", "movie", true)
        )
        
        coEvery { repository.getAllCategories() } returns flowOf(defaultCategories)
        coEvery { repository.getAllTags() } returns listOf("coffee", "work", "groceries")
        coEvery { repository.insertExpense(any()) } returns Result.success(1L)
        
        viewModel = AddExpenseViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
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
        viewModel.validateForm()
        
        val state = viewModel.uiState.value
        assertTrue(state.amountError != null)
        
        // Test zero amount
        viewModel.updateAmount(BigDecimal.ZERO)
        viewModel.validateForm()
        
        val state2 = viewModel.uiState.value
        assertTrue(state2.amountError != null)
        
        // Test valid amount
        viewModel.updateAmount(BigDecimal("25.50"))
        viewModel.validateForm()
        
        val state3 = viewModel.uiState.value
        assertNull(state3.amountError)
    }

    @Test
    fun `should validate description correctly`() = runTest {
        // Test empty description
        viewModel.updateDescription("")
        viewModel.validateForm()
        
        val state = viewModel.uiState.value
        assertTrue(state.descriptionError != null)
        
        // Test blank description
        viewModel.updateDescription("   ")
        viewModel.validateForm()
        
        val state2 = viewModel.uiState.value
        assertTrue(state2.descriptionError != null)
        
        // Test valid description
        viewModel.updateDescription("Coffee")
        viewModel.validateForm()
        
        val state3 = viewModel.uiState.value
        assertNull(state3.descriptionError)
    }

    @Test
    fun `should validate category selection correctly`() = runTest {
        // Test no category selected
        viewModel.validateForm()
        
        val state = viewModel.uiState.value
        assertTrue(state.categoryError != null)
        
        // Test valid category selected
        val category = Category(1, "Food", "#4CAF50", "restaurant", true)
        viewModel.selectCategory(category)
        viewModel.validateForm()
        
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
        
        // Save expense
        viewModel.saveExpense()
        
        // Advance time to complete async operations
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify repository was called
        coVerify {
            repository.insertExpense(
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
        
        // Try to save expense
        viewModel.saveExpense()
        
        // Verify repository was not called
        coVerify(exactly = 0) { repository.insertExpense(any()) }
        
        // Verify error state
        val state = viewModel.uiState.value
        assertFalse(state.isSaved)
        assertTrue(state.amountError != null)
        assertTrue(state.descriptionError != null)
    }

    @Test
    fun `should handle repository error when saving expense`() = runTest {
        // Mock repository to return error
        coEvery { repository.insertExpense(any()) } returns Result.failure(Exception("Database error"))
        
        // Setup valid form data
        viewModel.updateAmount(BigDecimal("25.50"))
        viewModel.updateDescription("Coffee")
        viewModel.selectCategory(Category(1, "Food", "#4CAF50", "restaurant", true))
        
        // Save expense
        viewModel.saveExpense()
        
        // Advance time to complete async operations
        testDispatcher.scheduler.advanceUntilIdle()
        
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
        
        // Save expense
        viewModel.saveExpense()
        
        // Reset form
        viewModel.resetForm()
        
        // Verify form is reset
        val state = viewModel.uiState.value
        assertNull(state.amount)
        assertEquals("", state.description)
        assertNull(state.selectedCategory)
        assertEquals(emptyList<String>(), state.selectedTags)
        assertFalse(state.isSaved)
        assertNull(state.errorMessage)
    }
}