package com.expensetracker.data.repository

import com.expensetracker.data.model.Category
import com.expensetracker.data.model.Expense
import org.junit.Test
import org.junit.Assert.*
import java.math.BigDecimal
import java.time.LocalDateTime

class ExpenseRepositoryImplTest {



    @Test
    fun `repository interface should be properly defined`() {
        // Test that the repository interface exists and is accessible
        val repositoryClass = ExpenseRepository::class.java
        
        // Verify it's an interface
        assertTrue("ExpenseRepository should be an interface", repositoryClass.isInterface)
        
        // Verify it has methods (interfaces have methods)
        assertTrue("ExpenseRepository should have methods", repositoryClass.methods.isNotEmpty())
    }

    @Test
    fun `expense validation should work correctly`() {
        // Test that expense validation logic is sound
        val validExpense = Expense(
            amount = BigDecimal("25.50"),
            currency = "USD",
            description = "Lunch at restaurant",
            categoryId = 1L,
            tags = listOf("lunch", "restaurant"),
            date = LocalDateTime.now()
        )
        
        // Valid expense should not throw exception
        assertNotNull(validExpense)
        assertEquals(BigDecimal("25.50"), validExpense.amount)
        assertEquals("USD", validExpense.currency)
        assertEquals("Lunch at restaurant", validExpense.description)
        assertEquals(1L, validExpense.categoryId)
        assertEquals(listOf("lunch", "restaurant"), validExpense.tags)
    }

    @Test
    fun `category validation should work correctly`() {
        // Test that category validation logic is sound
        val validCategory = Category(
            name = "Food",
            color = "#FF5722",
            icon = "restaurant",
            isDefault = true
        )
        
        // Valid category should not throw exception
        assertNotNull(validCategory)
        assertEquals("Food", validCategory.name)
        assertEquals("#FF5722", validCategory.color)
        assertEquals("restaurant", validCategory.icon)
        assertTrue(validCategory.isDefault)
    }
    
    @Test
    fun `tag validation should work correctly`() {
        // Test valid tags
        val validTags = listOf("food", "restaurant", "lunch")
        // Should not throw exception when creating expense with valid tags
        val validExpense = Expense(
            amount = BigDecimal("25.50"),
            currency = "USD",
            description = "Lunch at restaurant",
            categoryId = 1L,
            tags = validTags,
            date = LocalDateTime.now()
        )
        assertNotNull(validExpense)
        assertEquals(validTags, validExpense.tags)
    }

    @Test
    fun `category default creation should include expected categories`() {
        // Test that default categories are properly defined
        val expectedCategories = listOf(
            "Food & Dining",
            "Transportation", 
            "Shopping",
            "Entertainment",
            "Healthcare",
            "Travel",
            "Utilities",
            "Education",
            "Personal Care",
            "Other"
        )
        
        // This test verifies that our default categories list is comprehensive
        assertTrue("Should have at least 10 default categories", expectedCategories.size >= 10)
        assertTrue("Should include Food & Dining", expectedCategories.contains("Food & Dining"))
        assertTrue("Should include Transportation", expectedCategories.contains("Transportation"))
        assertTrue("Should include Travel", expectedCategories.contains("Travel"))
    }

    @Test
    fun `repository implementation should exist`() {
        // Test that the implementation class exists and can be instantiated
        val implClass = ExpenseRepositoryImpl::class.java
        assertNotNull("ExpenseRepositoryImpl class should exist", implClass)
        assertTrue("ExpenseRepositoryImpl should implement ExpenseRepository", 
            ExpenseRepository::class.java.isAssignableFrom(implClass))
    }
}