package com.expensetracker.data.repository

import com.expensetracker.data.model.Category
import com.expensetracker.data.model.Expense
import com.expensetracker.data.model.RecurringExpense
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Repository interface for expense management operations.
 * Provides a clean API for data access with business logic validation.
 */
interface ExpenseRepository {
    
    // Expense operations
    fun getAllExpenses(): Flow<List<Expense>>
    fun getExpenseById(id: Long): Flow<Expense?>
    fun getExpensesByDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<Expense>>
    fun getExpensesByCategory(categoryId: Long): Flow<List<Expense>>
    fun getExpensesByTags(tags: List<String>): Flow<List<Expense>>
    fun searchExpenses(searchText: String): Flow<List<Expense>>
    fun getFilteredExpenses(
        categoryId: Long? = null,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null,
        searchText: String? = null,
        tags: List<String>? = null
    ): Flow<List<Expense>>
    
    suspend fun insertExpense(expense: Expense): Result<Long>
    suspend fun updateExpense(expense: Expense): Result<Unit>
    suspend fun deleteExpense(expenseId: Long): Result<Unit>
    suspend fun deleteExpensesByCategory(categoryId: Long): Result<Unit>
    
    // Category operations
    fun getAllCategories(): Flow<List<Category>>
    fun getCategoryById(id: Long): Flow<Category?>
    fun getDefaultCategories(): Flow<List<Category>>
    fun getCustomCategories(): Flow<List<Category>>
    fun searchCategories(searchText: String): Flow<List<Category>>
    
    suspend fun insertCategory(category: Category): Result<Long>
    suspend fun updateCategory(category: Category): Result<Unit>
    suspend fun deleteCategory(categoryId: Long): Result<Unit>
    suspend fun initializeDefaultCategories(): Result<Unit>
    
    // Tag operations
    suspend fun getAllTags(): List<String>
    suspend fun getTagSuggestions(input: String): List<String>
    suspend fun getPopularTags(limit: Int = 10): List<String>
    
    // Recurring expense operations
    fun getAllRecurringExpenses(): Flow<List<RecurringExpense>>
    fun getActiveRecurringExpenses(): Flow<List<RecurringExpense>>
    fun getRecurringExpenseById(id: Long): Flow<RecurringExpense?>
    fun getRecurringExpensesByCategory(categoryId: Long): Flow<List<RecurringExpense>>
    
    suspend fun insertRecurringExpense(recurringExpense: RecurringExpense): Result<Long>
    suspend fun updateRecurringExpense(recurringExpense: RecurringExpense): Result<Unit>
    suspend fun deleteRecurringExpense(recurringExpenseId: Long): Result<Unit>
    suspend fun updateRecurringExpenseActiveStatus(id: Long, isActive: Boolean): Result<Unit>
    suspend fun updateRecurringExpenseLastGenerated(id: Long, lastGenerated: LocalDate): Result<Unit>
    suspend fun getRecurringExpensesToGenerate(currentDate: LocalDate): List<RecurringExpense>
    
    // Analytics operations
    suspend fun getTotalAmountByCategory(categoryId: Long): BigDecimal
    suspend fun getTotalAmountByDateRange(startDate: LocalDate, endDate: LocalDate): BigDecimal
    suspend fun getExpenseCount(): Int
    suspend fun getCategoryCount(): Int
}