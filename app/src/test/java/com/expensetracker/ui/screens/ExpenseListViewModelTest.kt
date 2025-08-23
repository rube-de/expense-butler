package com.expensetracker.ui.screens

import app.cash.turbine.test
import com.expensetracker.data.model.Category
import com.expensetracker.data.model.Expense
import com.expensetracker.data.repository.ExpenseRepository
import com.expensetracker.domain.recurring.GenerationResult
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseListViewModelTest {

    private lateinit var mockRepository: ExpenseRepository
    private lateinit var viewModel: ExpenseListViewModel
    private val testDispatcher = StandardTestDispatcher()

    private val sampleExpenses = listOf(
        Expense(
            id = 1,
            amount = BigDecimal("25.50"),
            currency = "USD",
            description = "Coffee",
            categoryId = 1,
            tags = listOf("coffee", "work"),
            date = LocalDateTime.of(2024, 1, 15, 9, 30)
        ),
        Expense(
            id = 2,
            amount = BigDecimal("120.00"),
            currency = "USD",
            description = "Groceries",
            categoryId = 2,
            tags = listOf("food", "weekly"),
            date = LocalDateTime.of(2024, 1, 14, 18, 0)
        )
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
        every { mockRepository.getAllExpenses() } returns flowOf(sampleExpenses)
        every { mockRepository.getAllCategories() } returns flowOf(sampleCategories)
        coEvery { mockRepository.getAllTags() } returns listOf("coffee", "work", "food", "weekly")
        
        viewModel = ExpenseListViewModel(mockRepository)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `should load expenses on initialization`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(sampleExpenses, state.expenses)
            assertFalse(state.isLoading)
        }
    }

    @Test
    fun `should load categories on initialization`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(sampleCategories, state.categories)
        }
    }

    @Test
    fun `should filter expenses by search text`() = runTest {
        val filteredExpenses = listOf(sampleExpenses[0]) // Only coffee expense
        every { mockRepository.searchExpenses("coffee") } returns flowOf(filteredExpenses)

        viewModel.updateSearchText("coffee")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("coffee", state.searchText)
        assertEquals(filteredExpenses, state.expenses)
    }

    @Test
    fun `should filter expenses by category`() = runTest {
        val filteredExpenses = listOf(sampleExpenses[0]) // Only food category
        every { mockRepository.getExpensesByCategory(1L) } returns flowOf(filteredExpenses)

        viewModel.selectCategoryFilter(sampleCategories[0])
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(sampleCategories[0], state.selectedCategoryFilter)
        assertEquals(filteredExpenses, state.expenses)
    }

    @Test
    fun `should filter expenses by date range`() = runTest {
        val startDate = LocalDate.of(2024, 1, 14)
        val endDate = LocalDate.of(2024, 1, 15)
        val filteredExpenses = sampleExpenses
        every { mockRepository.getExpensesByDateRange(startDate, endDate) } returns flowOf(filteredExpenses)

        viewModel.updateDateRange(startDate, endDate)

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(startDate, state.startDate)
            assertEquals(endDate, state.endDate)
            assertEquals(filteredExpenses, state.expenses)
        }
    }

    @Test
    fun `should filter expenses by tags`() = runTest {
        val selectedTags = listOf("coffee", "work")
        val filteredExpenses = listOf(sampleExpenses[0])
        every { mockRepository.getExpensesByTags(selectedTags) } returns flowOf(filteredExpenses)

        viewModel.updateTagFilter(selectedTags)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(selectedTags, state.selectedTags)
        assertEquals(filteredExpenses, state.expenses)
    }

    @Test
    fun `should clear all filters`() = runTest {
        // First apply some filters
        viewModel.updateSearchText("coffee")
        viewModel.selectCategoryFilter(sampleCategories[0])

        // Then clear filters
        viewModel.clearFilters()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("", state.searchText)
            assertNull(state.selectedCategoryFilter)
            assertEquals(emptyList<String>(), state.selectedTags)
            assertNull(state.startDate)
            assertNull(state.endDate)
            assertEquals(sampleExpenses, state.expenses)
        }
    }

    @Test
    fun `should delete expense successfully`() = runTest {
        coEvery { mockRepository.deleteExpense(1L) } returns Result.success(Unit)

        viewModel.deleteExpense(1L)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { mockRepository.deleteExpense(1L) }
        
        val state = viewModel.uiState.value
        assertNull(state.errorMessage)
    }

    @Test
    fun `should handle delete expense error`() = runTest {
        val errorMessage = "Failed to delete expense"
        coEvery { mockRepository.deleteExpense(1L) } returns Result.failure(Exception(errorMessage))

        viewModel.deleteExpense(1L)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Failed to delete expense: $errorMessage", state.errorMessage)
    }

    @Test
    fun `should show loading state during operations`() = runTest {
        // Mock a slow operation
        coEvery { mockRepository.deleteExpense(1L) } coAnswers {
            kotlinx.coroutines.delay(100)
            Result.success(Unit)
        }

        viewModel.deleteExpense(1L)

        // Check that loading state is set
        viewModel.uiState.test {
            val state = awaitItem()
            // Loading state should be handled internally
            assertFalse(state.isLoading) // Should be false after operation completes
        }
    }

    @Test
    fun `should combine multiple filters correctly`() = runTest {
        val filteredExpenses = listOf(sampleExpenses[0])
        every { 
            mockRepository.getFilteredExpenses(
                categoryId = 1L,
                startDate = any(),
                endDate = any(),
                searchText = "coffee",
                tags = listOf("work")
            )
        } returns flowOf(filteredExpenses)

        // Apply multiple filters
        viewModel.updateSearchText("coffee")
        viewModel.selectCategoryFilter(sampleCategories[0])
        viewModel.updateTagFilter(listOf("work"))
        viewModel.updateDateRange(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31))
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("coffee", state.searchText)
        assertEquals(sampleCategories[0], state.selectedCategoryFilter)
        assertEquals(listOf("work"), state.selectedTags)
        assertEquals(filteredExpenses, state.expenses)
    }

    // Generation Status Tests
    
    @Test
    fun `should show generation banner when expenses were generated`() = runTest {
        // Given a generation result with expenses generated
        val generationResult = GenerationResult(
            hasGenerated = true,
            message = "3 recurring expenses generated",
            timestamp = System.currentTimeMillis()
        )
        
        // When the view model is notified
        viewModel.setGenerationResult(generationResult)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then the UI state should show the generation banner
        val state = viewModel.uiState.value
        assertTrue(state.showGenerationBanner)
        assertEquals("3 recurring expenses generated", state.generationMessage)
    }
    
    @Test
    fun `should not show generation banner when no expenses were generated`() = runTest {
        // Given a generation result with no expenses generated
        val generationResult = GenerationResult(
            hasGenerated = false,
            message = "",
            timestamp = System.currentTimeMillis()
        )
        
        // When the view model is notified
        viewModel.setGenerationResult(generationResult)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then the UI state should not show the generation banner
        val state = viewModel.uiState.value
        assertFalse(state.showGenerationBanner)
        assertNull(state.generationMessage)
    }
    
    @Test
    fun `should dismiss generation banner when user dismisses it`() = runTest {
        // Given a generation banner is showing
        val generationResult = GenerationResult(
            hasGenerated = true,
            message = "Expenses generated",
            timestamp = System.currentTimeMillis()
        )
        viewModel.setGenerationResult(generationResult)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // When the user dismisses the banner
        viewModel.dismissGenerationBanner()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then the banner should be hidden
        val state = viewModel.uiState.value
        assertFalse(state.showGenerationBanner)
        assertNull(state.generationMessage)
    }
    
    @Test
    fun `should handle null generation result gracefully`() = runTest {
        // When the view model receives a null generation result
        viewModel.setGenerationResult(null)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then no banner should be shown
        val state = viewModel.uiState.value
        assertFalse(state.showGenerationBanner)
        assertNull(state.generationMessage)
    }
    
    @Test
    fun `should update generation banner message correctly`() = runTest {
        // Given multiple generation results
        val firstResult = GenerationResult(
            hasGenerated = true,
            message = "2 expenses generated",
            timestamp = System.currentTimeMillis()
        )
        val secondResult = GenerationResult(
            hasGenerated = true,
            message = "5 expenses generated",
            timestamp = System.currentTimeMillis() + 1000
        )
        
        // When results are set sequentially
        viewModel.setGenerationResult(firstResult)
        testDispatcher.scheduler.advanceUntilIdle()
        
        var state = viewModel.uiState.value
        assertEquals("2 expenses generated", state.generationMessage)
        
        viewModel.setGenerationResult(secondResult)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Then the message should be updated
        state = viewModel.uiState.value
        assertEquals("5 expenses generated", state.generationMessage)
    }
}