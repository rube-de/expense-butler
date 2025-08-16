package com.expensetracker.data.repository

import app.cash.turbine.test
import com.expensetracker.data.dao.CategoryDao
import com.expensetracker.data.dao.ExpenseDao
import com.expensetracker.data.dao.RecurringExpenseDao
import com.expensetracker.data.model.Category
import com.expensetracker.data.model.Expense
import com.expensetracker.data.model.RecurringExpense
import com.expensetracker.data.model.RecurrenceFrequency
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Unit tests for ExpenseRepositoryImpl using mocked dependencies.
 * These tests focus on repository business logic, validation, and DAO interactions.
 * 
 * Note: Data model validation (like positive amounts) happens in the data class constructors,
 * so these tests focus on repository-specific logic like category existence checks,
 * tag normalization, and proper DAO method calls.
 */
class ExpenseRepositoryImplTest {

    private lateinit var mockExpenseDao: ExpenseDao
    private lateinit var mockCategoryDao: CategoryDao
    private lateinit var mockRecurringExpenseDao: RecurringExpenseDao
    private lateinit var repository: ExpenseRepositoryImpl

    @Before
    fun setup() {
        // Setup mocks for unit tests
        mockExpenseDao = mockk()
        mockCategoryDao = mockk()
        mockRecurringExpenseDao = mockk()
        repository = ExpenseRepositoryImpl(mockExpenseDao, mockCategoryDao, mockRecurringExpenseDao)
    }

    // EXPENSE OPERATIONS TESTS

    @Test
    fun `should insert expense successfully when valid data provided`() = runTest {
        // Arrange
        val category = createTestCategory(id = 1L)
        val expense = createTestExpense(categoryId = category.id)
        
        coEvery { mockCategoryDao.getCategoryById(category.id) } returns category
        coEvery { mockExpenseDao.insertExpense(any()) } returns 1L

        // Act
        val result = repository.insertExpense(expense)

        // Assert
        assertTrue("Should succeed with valid expense", result.isSuccess)
        val expenseId = result.getOrNull()
        assertNotNull("Should return expense ID", expenseId)
        assertEquals("Should return correct expense ID", 1L, expenseId)
        
        coVerify { mockCategoryDao.getCategoryById(category.id) }
        coVerify { mockExpenseDao.insertExpense(any()) }
    }

    @Test
    fun `should fail to insert expense when category does not exist`() = runTest {
        // Arrange
        val expense = createTestExpense(categoryId = 999L)
        coEvery { mockCategoryDao.getCategoryById(999L) } returns null

        // Act
        val result = repository.insertExpense(expense)

        // Assert
        assertTrue("Should fail with non-existent category", result.isFailure)
        assertEquals("Category with ID 999 does not exist", result.exceptionOrNull()?.message)
        coVerify { mockCategoryDao.getCategoryById(999L) }
    }

    @Test
    fun `should handle DAO exceptions during expense insertion`() = runTest {
        // Arrange
        val category = createTestCategory(id = 1L)
        val expense = createTestExpense(categoryId = category.id)
        
        coEvery { mockCategoryDao.getCategoryById(category.id) } returns category
        coEvery { mockExpenseDao.insertExpense(any()) } throws RuntimeException("Database error")

        // Act
        val result = repository.insertExpense(expense)

        // Assert
        assertTrue("Should fail when DAO throws exception", result.isFailure)
        assertTrue("Should contain original exception", result.exceptionOrNull() is RuntimeException)
        assertEquals("Database error", result.exceptionOrNull()?.message)
    }

    @Test
    fun `should normalize tags when inserting expense`() = runTest {
        // Arrange
        val category = createTestCategory(id = 1L)
        val expense = createTestExpense(
            categoryId = category.id,
            tags = listOf("  FOOD  ", "Restaurant", "LUNCH", "food") // Mixed case and whitespace
        )
        
        coEvery { mockCategoryDao.getCategoryById(category.id) } returns category
        coEvery { mockExpenseDao.insertExpense(any()) } returns 1L

        // Act
        val result = repository.insertExpense(expense)

        // Assert
        assertTrue("Should succeed with normalized tags", result.isSuccess)
        
        // Verify the expense passed to DAO has normalized tags
        coVerify { 
            mockExpenseDao.insertExpense(match { 
                it.tags == listOf("food", "restaurant", "lunch")
            })
        }
    }

    @Test
    fun `should validate maximum tag count`() = runTest {
        // Arrange - Create expense with too many tags (21 tags, limit is 20)
        val tooManyTags = (1..21).map { "tag$it" }
        
        // This test verifies the repository validation logic
        // We expect this to fail in the repository validation, not the data class
        try {
            val expense = createTestExpense(tags = tooManyTags)
            val category = createTestCategory(id = 1L)
            
            coEvery { mockCategoryDao.getCategoryById(1L) } returns category
            
            val result = repository.insertExpense(expense)
            
            // If we get here, the validation should catch it
            assertTrue("Should fail with too many tags", result.isFailure)
            assertEquals("Maximum 20 tags allowed", result.exceptionOrNull()?.message)
        } catch (e: IllegalArgumentException) {
            // If validation happens in data class constructor, that's also acceptable
            assertTrue("Should fail with validation error", e.message?.contains("tag") == true)
        }
    }

    @Test
    fun `should update expense successfully when valid data provided`() = runTest {
        // Arrange
        val category = createTestCategory(id = 1L)
        val existingExpense = createTestExpense(id = 1L, categoryId = category.id)
        val updatedExpense = existingExpense.copy(
            description = "Updated description",
            amount = BigDecimal("50.00")
        )
        
        coEvery { mockExpenseDao.getExpenseById(1L) } returns existingExpense
        coEvery { mockCategoryDao.getCategoryById(category.id) } returns category
        coEvery { mockExpenseDao.updateExpense(any()) } returns Unit

        // Act
        val result = repository.updateExpense(updatedExpense)

        // Assert
        assertTrue("Should succeed updating valid expense", result.isSuccess)
        coVerify { mockExpenseDao.getExpenseById(1L) }
        coVerify { mockCategoryDao.getCategoryById(category.id) }
        coVerify { mockExpenseDao.updateExpense(any()) }
    }

    @Test
    fun `should fail to update expense when expense does not exist`() = runTest {
        // Arrange
        val expense = createTestExpense(id = 999L)
        coEvery { mockExpenseDao.getExpenseById(999L) } returns null

        // Act
        val result = repository.updateExpense(expense)

        // Assert
        assertTrue("Should fail with non-existent expense", result.isFailure)
        assertEquals("Expense with ID 999 does not exist", result.exceptionOrNull()?.message)
        coVerify { mockExpenseDao.getExpenseById(999L) }
    }

    @Test
    fun `should delete expense successfully when expense exists`() = runTest {
        // Arrange
        val expense = createTestExpense(id = 1L)
        coEvery { mockExpenseDao.getExpenseById(1L) } returns expense
        coEvery { mockExpenseDao.deleteExpenseById(1L) } returns Unit

        // Act
        val result = repository.deleteExpense(1L)

        // Assert
        assertTrue("Should succeed deleting existing expense", result.isSuccess)
        coVerify { mockExpenseDao.getExpenseById(1L) }
        coVerify { mockExpenseDao.deleteExpenseById(1L) }
    }

    @Test
    fun `should fail to delete expense when expense does not exist`() = runTest {
        // Arrange
        coEvery { mockExpenseDao.getExpenseById(999L) } returns null

        // Act
        val result = repository.deleteExpense(999L)

        // Assert
        assertTrue("Should fail with non-existent expense", result.isFailure)
        assertEquals("Expense with ID 999 does not exist", result.exceptionOrNull()?.message)
        coVerify { mockExpenseDao.getExpenseById(999L) }
    }

    // CATEGORY OPERATIONS TESTS

    @Test
    fun `should insert category successfully when valid data provided`() = runTest {
        // Arrange
        val category = createTestCategory()
        coEvery { mockCategoryDao.getCategoryByName(category.name) } returns null
        coEvery { mockCategoryDao.insertCategory(category) } returns 1L

        // Act
        val result = repository.insertCategory(category)

        // Assert
        assertTrue("Should succeed with valid category", result.isSuccess)
        val categoryId = result.getOrNull()
        assertNotNull("Should return category ID", categoryId)
        assertEquals("Should return correct category ID", 1L, categoryId)
        
        coVerify { mockCategoryDao.getCategoryByName(category.name) }
        coVerify { mockCategoryDao.insertCategory(category) }
    }

    @Test
    fun `should fail to insert category when name already exists`() = runTest {
        // Arrange
        val existingCategory = createTestCategory(id = 1L, name = "Food")
        val duplicateCategory = createTestCategory(name = "Food")
        
        coEvery { mockCategoryDao.getCategoryByName("Food") } returns existingCategory

        // Act
        val result = repository.insertCategory(duplicateCategory)

        // Assert
        assertTrue("Should fail with duplicate name", result.isFailure)
        assertEquals("Category with name 'Food' already exists", result.exceptionOrNull()?.message)
        coVerify { mockCategoryDao.getCategoryByName("Food") }
    }

    @Test
    fun `should initialize default categories when none exist`() = runTest {
        // Arrange
        coEvery { mockCategoryDao.getDefaultCategoryCount() } returns 0
        coEvery { mockCategoryDao.insertCategories(any()) } returns listOf(1L, 2L, 3L)

        // Act
        val result = repository.initializeDefaultCategories()

        // Assert
        assertTrue("Should succeed initializing default categories", result.isSuccess)
        coVerify { mockCategoryDao.getDefaultCategoryCount() }
        coVerify { mockCategoryDao.insertCategories(any()) }
    }

    @Test
    fun `should not initialize default categories when they already exist`() = runTest {
        // Arrange
        coEvery { mockCategoryDao.getDefaultCategoryCount() } returns 10

        // Act
        val result = repository.initializeDefaultCategories()

        // Assert
        assertTrue("Should succeed without inserting", result.isSuccess)
        coVerify { mockCategoryDao.getDefaultCategoryCount() }
        coVerify(exactly = 0) { mockCategoryDao.insertCategories(any()) }
    }

    // REPOSITORY BUSINESS LOGIC TESTS

    @Test
    fun `should verify repository interface exists and is properly implemented`() {
        // Arrange & Act
        val repositoryClass = ExpenseRepository::class.java
        val implementationClass = ExpenseRepositoryImpl::class.java

        // Assert
        assertTrue("ExpenseRepository should be an interface", repositoryClass.isInterface)
        assertTrue("ExpenseRepositoryImpl should implement ExpenseRepository", 
            repositoryClass.isAssignableFrom(implementationClass))
        assertTrue("Repository should have methods", repositoryClass.methods.isNotEmpty())
    }

    @Test
    fun `should handle DAO exceptions gracefully`() = runTest {
        // Arrange
        val category = createTestCategory(id = 1L)
        coEvery { mockCategoryDao.getCategoryById(1L) } throws RuntimeException("Database connection failed")

        // Act
        val result = repository.insertCategory(category)

        // Assert
        assertTrue("Should handle DAO exceptions", result.isFailure)
        assertTrue("Should preserve original exception", result.exceptionOrNull() is RuntimeException)
    }

    // RECURRING EXPENSE OPERATIONS TESTS

    @Test
    fun `should insert recurring expense successfully when valid data provided`() = runTest {
        // Arrange
        val category = createTestCategory(id = 1L)
        val recurringExpense = createTestRecurringExpense(categoryId = category.id)
        
        coEvery { mockCategoryDao.getCategoryById(category.id) } returns category
        coEvery { mockRecurringExpenseDao.insertRecurringExpense(any()) } returns 1L

        // Act
        val result = repository.insertRecurringExpense(recurringExpense)

        // Assert
        assertTrue("Should succeed with valid recurring expense", result.isSuccess)
        val recurringExpenseId = result.getOrNull()
        assertNotNull("Should return recurring expense ID", recurringExpenseId)
        assertEquals("Should return correct ID", 1L, recurringExpenseId)
        
        coVerify { mockCategoryDao.getCategoryById(category.id) }
        coVerify { mockRecurringExpenseDao.insertRecurringExpense(any()) }
    }

    @Test
    fun `should fail to insert recurring expense when category does not exist`() = runTest {
        // Arrange
        val recurringExpense = createTestRecurringExpense(categoryId = 999L)
        coEvery { mockCategoryDao.getCategoryById(999L) } returns null

        // Act
        val result = repository.insertRecurringExpense(recurringExpense)

        // Assert
        assertTrue("Should fail with non-existent category", result.isFailure)
        assertEquals("Category with ID 999 does not exist", result.exceptionOrNull()?.message)
        coVerify { mockCategoryDao.getCategoryById(999L) }
    }

    // ANALYTICS OPERATIONS TESTS

    @Test
    fun `should calculate total amount by category correctly`() = runTest {
        // Arrange
        val categoryId = 1L
        coEvery { mockExpenseDao.getTotalAmountByCategory(categoryId) } returns 55.50

        // Act
        val total = repository.getTotalAmountByCategory(categoryId)

        // Assert
        assertEquals("Should calculate correct total", 0, BigDecimal("55.50").compareTo(total))
        coVerify { mockExpenseDao.getTotalAmountByCategory(categoryId) }
    }

    @Test
    fun `should handle null values in analytics operations`() = runTest {
        // Arrange
        coEvery { mockExpenseDao.getTotalAmountByCategory(any()) } returns null

        // Act
        val totalByCategory = repository.getTotalAmountByCategory(1L)

        // Assert
        assertEquals("Should return zero for null category total", 0, BigDecimal.ZERO.compareTo(totalByCategory))
    }

    // HELPER METHODS

    private fun createTestExpense(
        id: Long = 0L,
        amount: BigDecimal = BigDecimal("25.50"),
        currency: String = "USD",
        description: String = "Test expense",
        categoryId: Long = 1L,
        tags: List<String> = listOf("test"),
        date: LocalDateTime = LocalDateTime.now()
    ) = Expense(
        id = id,
        amount = amount,
        currency = currency,
        description = description,
        categoryId = categoryId,
        tags = tags,
        date = date,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )

    private fun createTestCategory(
        id: Long = 0L,
        name: String = "Test Category",
        color: String = "#FF5722",
        icon: String = "test_icon",
        isDefault: Boolean = false
    ) = Category(
        id = id,
        name = name,
        color = color,
        icon = icon,
        isDefault = isDefault
    )

    private fun createTestRecurringExpense(
        id: Long = 0L,
        amount: BigDecimal = BigDecimal("25.50"),
        currency: String = "USD",
        description: String = "Test recurring expense",
        categoryId: Long = 1L,
        tags: List<String> = listOf("test"),
        frequency: RecurrenceFrequency = RecurrenceFrequency.MONTHLY,
        startDate: LocalDate = LocalDate.now(),
        endDate: LocalDate? = null,
        isActive: Boolean = true
    ) = RecurringExpense(
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
        isActive = isActive,
        createdAt = LocalDateTime.now()
    )
}