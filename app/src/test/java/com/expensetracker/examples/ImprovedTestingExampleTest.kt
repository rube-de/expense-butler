package com.expensetracker.examples

import com.expensetracker.test.fakes.FakeExpenseRepository
import com.expensetracker.test.fixtures.TestData
import com.expensetracker.test.rules.CoroutineTestRule
import com.expensetracker.test.rules.MockKRule
import com.google.common.truth.Truth.assertThat
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import org.junit.Rule
import org.junit.Test
import org.junit.jupiter.api.DisplayName
import java.math.BigDecimal

/**
 * Example test demonstrating the improved testing infrastructure.
 * 
 * This test class shows how to use:
 * - Test fixtures and builders for creating test data
 * - Fake repository for controlled testing
 * - Custom test rules for coroutine testing
 * - Google Truth assertions for better error messages
 * - Kotest assertions for Kotlin-first testing
 * - JUnit 5 features like @DisplayName (when using JUnit 5 tests)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ImprovedTestingExampleTest {
    
    @get:Rule
    val coroutineTestRule = CoroutineTestRule()
    
    @get:Rule
    val mockKRule = MockKRule()
    
    private val fakeRepository = FakeExpenseRepository()
    
    @Test
    fun `should create test data using builders`() {
        // Using the test data builders for clean test data creation
        val expense = TestData.expense {
            withAmount("150.00")
            withDescription("Team lunch")
            withCategoryId(1)
            withTags("food", "business", "team")
        }
        
        // Google Truth assertions with better error messages
        assertThat(expense.amount).isEqualTo(BigDecimal("150.00"))
        assertThat(expense.description).isEqualTo("Team lunch")
        assertThat(expense.tags).containsExactly("food", "business", "team")
    }
    
    @Test
    fun `should use fake repository for testing`() = coroutineTestRule.runTest {
        // Setup fake data
        val testExpenses = TestData.sampleExpenses()
        val testCategories = TestData.sampleCategories()
        
        fakeRepository.setExpenses(*testExpenses.toTypedArray())
        fakeRepository.setCategories(*testCategories.toTypedArray())
        
        // Test repository operations
        val expenses = fakeRepository.getAllExpenses().first()
        val categories = fakeRepository.getAllCategories().first()
        
        // Kotest assertions for Kotlin-first testing
        expenses.size shouldBe 3
        categories.size shouldBe 3
        
        // Google Truth for more complex assertions
        assertThat(expenses).hasSize(3)
        assertThat(expenses.map { it.description }).containsExactly(
            "Lunch", "Uber ride", "Groceries"
        )
    }
    
    @Test
    fun `should simulate errors with fake repository`() = coroutineTestRule.runTest {
        // Configure fake to throw error
        fakeRepository.shouldThrowError = true
        fakeRepository.errorMessage = "Database connection failed"
        
        // Test error handling
        val result = fakeRepository.insertExpense(
            TestData.expense { withDescription("Test expense") }
        )
        
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).isEqualTo("Database connection failed")
    }
    
    @Test
    fun `should track repository operations for verification`() = coroutineTestRule.runTest {
        val expense = TestData.expense {
            withId(1)
            withAmount("100.00")
            withDescription("Test purchase")
        }
        
        // Perform operations
        fakeRepository.insertExpense(expense)
        fakeRepository.updateExpense(expense.copy(amount = BigDecimal("150.00")))
        fakeRepository.deleteExpense(expense.id)
        
        // Verify operations were tracked
        assertThat(fakeRepository.insertedExpenses).hasSize(1)
        assertThat(fakeRepository.updatedExpenses).hasSize(1)
        assertThat(fakeRepository.deletedExpenseIds).contains(1L)
    }
    
    @Test
    fun `should use coroutine test rule for time control`() = coroutineTestRule.runTest {
        // The test rule handles dispatcher setup and teardown automatically
        // You can also control time advancement
        coroutineTestRule.advanceUntilIdle()
        
        // Test async operations
        val expenses = fakeRepository.getAllExpenses().first()
        assertThat(expenses).isEmpty()
    }
    
    @Test
    fun `should combine Truth and Kotest assertions`() {
        val category = TestData.category {
            withName("Transportation")
            withIcon("directions_car")
            withColor("#2196F3")
            asDefault()
        }
        
        // Mix assertion libraries based on what's most expressive
        
        // Kotest for simple equality
        category.name shouldBe "Transportation"
        category.isDefault shouldBe true
        
        // Truth for more complex assertions
        assertThat(category.color).startsWith("#")
        assertThat(category.icon).contains("car")
    }
    
    @Test
    fun `should use mockk with test rules for automatic cleanup`() = coroutineTestRule.runTest {
        // MockK rule automatically clears mocks after each test
        val mockedService: SomeService = mockk()
        
        coEvery { mockedService.getData() } returns "mocked data"
        
        val result = mockedService.getData()
        assertThat(result).isEqualTo("mocked data")
        
        // Mocks are automatically cleared by MockKRule after test
    }
}

// Example interface for mocking demonstration
interface SomeService {
    suspend fun getData(): String
}