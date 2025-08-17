package com.expensetracker.test.fakes

import com.expensetracker.data.model.*
import com.expensetracker.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Fake implementation of ExpenseRepository for testing.
 * Provides in-memory storage and controllable behavior for tests.
 * 
 * This is a simplified implementation focusing on the most commonly used methods.
 * Methods not implemented will throw NotImplementedError.
 */
class FakeExpenseRepository : ExpenseRepository {
    
    // In-memory storage
    private val expenses = mutableListOf<Expense>()
    private val categories = mutableListOf<Category>()
    private val recurringExpenses = mutableListOf<RecurringExpense>()
    
    // Observable state flows
    private val expensesFlow = MutableStateFlow<List<Expense>>(emptyList())
    private val categoriesFlow = MutableStateFlow<List<Category>>(emptyList())
    private val recurringExpensesFlow = MutableStateFlow<List<RecurringExpense>>(emptyList())
    
    // Error simulation
    var shouldThrowError = false
    var errorMessage = "Fake repository error"
    
    // Tracking for test verification
    val insertedExpenses = mutableListOf<Expense>()
    val updatedExpenses = mutableListOf<Expense>()
    val deletedExpenseIds = mutableListOf<Long>()
    
    // Expense operations
    override fun getAllExpenses(): Flow<List<Expense>> {
        if (shouldThrowError) {
            return flow { throw Exception(errorMessage) }
        }
        return expensesFlow
    }
    
    override fun getExpenseById(id: Long): Flow<Expense?> {
        return expensesFlow.map { expenses ->
            expenses.find { it.id == id }
        }
    }
    
    override fun getExpensesByDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<Expense>> {
        if (shouldThrowError) {
            return flow { throw Exception(errorMessage) }
        }
        return expensesFlow.map { expenses ->
            expenses.filter { expense ->
                val expenseDate = expense.date.toLocalDate()
                !expenseDate.isBefore(startDate) && !expenseDate.isAfter(endDate)
            }
        }
    }
    
    override fun getExpensesByCategory(categoryId: Long): Flow<List<Expense>> {
        if (shouldThrowError) {
            return flow { throw Exception(errorMessage) }
        }
        return expensesFlow.map { expenses ->
            expenses.filter { it.categoryId == categoryId }
        }
    }
    
    override fun getExpensesByTags(tags: List<String>): Flow<List<Expense>> {
        if (shouldThrowError) {
            return flow { throw Exception(errorMessage) }
        }
        return expensesFlow.map { expenses ->
            expenses.filter { expense ->
                expense.tags.any { it in tags }
            }
        }
    }
    
    override fun searchExpenses(searchText: String): Flow<List<Expense>> {
        return expensesFlow.map { expenses ->
            expenses.filter { expense ->
                expense.description.contains(searchText, ignoreCase = true)
            }
        }
    }
    
    override fun getFilteredExpenses(
        categoryId: Long?,
        startDate: LocalDate?,
        endDate: LocalDate?,
        searchText: String?,
        tags: List<String>?
    ): Flow<List<Expense>> {
        return expensesFlow.map { expenses ->
            var filtered = expenses
            categoryId?.let { id ->
                filtered = filtered.filter { it.categoryId == id }
            }
            startDate?.let { start ->
                filtered = filtered.filter { !it.date.toLocalDate().isBefore(start) }
            }
            endDate?.let { end ->
                filtered = filtered.filter { !it.date.toLocalDate().isAfter(end) }
            }
            searchText?.let { text ->
                filtered = filtered.filter { it.description.contains(text, ignoreCase = true) }
            }
            tags?.let { tagList ->
                filtered = filtered.filter { expense ->
                    expense.tags.any { it in tagList }
                }
            }
            filtered
        }
    }
    
    override suspend fun insertExpense(expense: Expense): Result<Long> {
        return try {
            if (shouldThrowError) {
                throw Exception(errorMessage)
            }
            expenses.add(expense)
            insertedExpenses.add(expense)
            expensesFlow.value = expenses.toList()
            Result.success(expense.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateExpense(expense: Expense): Result<Unit> {
        return try {
            if (shouldThrowError) {
                throw Exception(errorMessage)
            }
            val index = expenses.indexOfFirst { it.id == expense.id }
            if (index != -1) {
                expenses[index] = expense
                updatedExpenses.add(expense)
                expensesFlow.value = expenses.toList()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteExpense(expenseId: Long): Result<Unit> {
        return try {
            if (shouldThrowError) {
                throw Exception(errorMessage)
            }
            expenses.removeIf { it.id == expenseId }
            deletedExpenseIds.add(expenseId)
            expensesFlow.value = expenses.toList()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteExpensesByCategory(categoryId: Long): Result<Unit> {
        return try {
            expenses.removeIf { it.categoryId == categoryId }
            expensesFlow.value = expenses.toList()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Category operations
    override fun getAllCategories(): Flow<List<Category>> {
        if (shouldThrowError) {
            return flow { throw Exception(errorMessage) }
        }
        return categoriesFlow
    }
    
    override fun getCategoryById(id: Long): Flow<Category?> {
        return categoriesFlow.map { categories ->
            categories.find { it.id == id }
        }
    }
    
    override fun getDefaultCategories(): Flow<List<Category>> {
        return categoriesFlow.map { categories ->
            categories.filter { it.isDefault }
        }
    }
    
    override fun getCustomCategories(): Flow<List<Category>> {
        return categoriesFlow.map { categories ->
            categories.filter { !it.isDefault }
        }
    }
    
    override fun searchCategories(searchText: String): Flow<List<Category>> {
        return categoriesFlow.map { categories ->
            categories.filter { it.name.contains(searchText, ignoreCase = true) }
        }
    }
    
    override suspend fun insertCategory(category: Category): Result<Long> {
        return try {
            if (shouldThrowError) {
                throw Exception(errorMessage)
            }
            categories.add(category)
            categoriesFlow.value = categories.toList()
            Result.success(category.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateCategory(category: Category): Result<Unit> {
        return try {
            val index = categories.indexOfFirst { it.id == category.id }
            if (index != -1) {
                categories[index] = category
                categoriesFlow.value = categories.toList()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteCategory(categoryId: Long): Result<Unit> {
        return try {
            categories.removeIf { it.id == categoryId }
            categoriesFlow.value = categories.toList()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun initializeDefaultCategories(): Result<Unit> {
        return Result.success(Unit)
    }
    
    // Tag operations
    override suspend fun getAllTags(): List<String> {
        if (shouldThrowError) {
            throw Exception(errorMessage)
        }
        return expenses.flatMap { it.tags }.distinct().sorted()
    }
    
    override suspend fun getTagSuggestions(input: String): List<String> {
        return getAllTags().filter { it.startsWith(input, ignoreCase = true) }
    }
    
    override suspend fun getPopularTags(limit: Int): List<String> {
        return expenses.flatMap { it.tags }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { it.key }
    }
    
    // Recurring expense operations
    override fun getAllRecurringExpenses(): Flow<List<RecurringExpense>> {
        if (shouldThrowError) {
            return flow { throw Exception(errorMessage) }
        }
        return recurringExpensesFlow
    }
    
    override fun getActiveRecurringExpenses(): Flow<List<RecurringExpense>> {
        return recurringExpensesFlow.map { recurring ->
            recurring.filter { it.isActive }
        }
    }
    
    override fun getRecurringExpenseById(id: Long): Flow<RecurringExpense?> {
        return recurringExpensesFlow.map { recurring ->
            recurring.find { it.id == id }
        }
    }
    
    override fun getRecurringExpensesByCategory(categoryId: Long): Flow<List<RecurringExpense>> {
        return recurringExpensesFlow.map { recurring ->
            recurring.filter { it.categoryId == categoryId }
        }
    }
    
    override suspend fun insertRecurringExpense(recurringExpense: RecurringExpense): Result<Long> {
        return try {
            if (shouldThrowError) {
                throw Exception(errorMessage)
            }
            recurringExpenses.add(recurringExpense)
            recurringExpensesFlow.value = recurringExpenses.toList()
            Result.success(recurringExpense.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateRecurringExpense(recurringExpense: RecurringExpense): Result<Unit> {
        return try {
            val index = recurringExpenses.indexOfFirst { it.id == recurringExpense.id }
            if (index != -1) {
                recurringExpenses[index] = recurringExpense
                recurringExpensesFlow.value = recurringExpenses.toList()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteRecurringExpense(recurringExpenseId: Long): Result<Unit> {
        return try {
            recurringExpenses.removeIf { it.id == recurringExpenseId }
            recurringExpensesFlow.value = recurringExpenses.toList()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateRecurringExpenseActiveStatus(id: Long, isActive: Boolean): Result<Unit> {
        return try {
            val expense = recurringExpenses.find { it.id == id }
            expense?.let {
                val updated = it.copy(isActive = isActive)
                updateRecurringExpense(updated)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateRecurringExpenseLastGenerated(id: Long, lastGenerated: LocalDate): Result<Unit> {
        return try {
            val expense = recurringExpenses.find { it.id == id }
            expense?.let {
                val updated = it.copy(lastGenerated = lastGenerated)
                updateRecurringExpense(updated)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getRecurringExpensesToGenerate(currentDate: LocalDate): List<RecurringExpense> {
        return recurringExpenses.filter { it.isActive }
    }
    
    // Analytics operations (simplified implementations)
    override suspend fun getTotalAmountByCategory(categoryId: Long): BigDecimal {
        return expenses.filter { it.categoryId == categoryId }
            .sumOf { it.amount }
    }
    
    override suspend fun getTotalAmountByDateRange(startDate: LocalDate, endDate: LocalDate): BigDecimal {
        return expenses.filter { expense ->
            val date = expense.date.toLocalDate()
            !date.isBefore(startDate) && !date.isAfter(endDate)
        }.sumOf { it.amount }
    }
    
    override suspend fun getExpenseCount(): Int = expenses.size
    
    override suspend fun getCategoryCount(): Int = categories.size
    
    override suspend fun getCategorySpendingBreakdown(startDate: LocalDate, endDate: LocalDate): Map<Long, BigDecimal> {
        return expenses.filter { expense ->
            val date = expense.date.toLocalDate()
            !date.isBefore(startDate) && !date.isAfter(endDate)
        }.groupBy { it.categoryId }
            .mapValues { (_, expenses) -> expenses.sumOf { it.amount } }
    }
    
    override suspend fun getTagSpendingBreakdown(startDate: LocalDate, endDate: LocalDate): Map<String, BigDecimal> {
        val result = mutableMapOf<String, BigDecimal>()
        expenses.filter { expense ->
            val date = expense.date.toLocalDate()
            !date.isBefore(startDate) && !date.isAfter(endDate)
        }.forEach { expense ->
            expense.tags.forEach { tag ->
                result[tag] = result.getOrDefault(tag, BigDecimal.ZERO) + expense.amount
            }
        }
        return result
    }
    
    override suspend fun getMonthlySpendingTrends(startDate: LocalDate, endDate: LocalDate): List<Pair<LocalDate, BigDecimal>> {
        return expenses.filter { expense ->
            val date = expense.date.toLocalDate()
            !date.isBefore(startDate) && !date.isAfter(endDate)
        }.groupBy { it.date.toLocalDate().withDayOfMonth(1) }
            .map { (month, expenses) -> month to expenses.sumOf { it.amount } }
            .sortedBy { it.first }
    }
    
    // Helper methods for test setup
    fun setExpenses(vararg expenses: Expense) {
        this.expenses.clear()
        this.expenses.addAll(expenses)
        expensesFlow.value = this.expenses.toList()
    }
    
    fun setCategories(vararg categories: Category) {
        this.categories.clear()
        this.categories.addAll(categories)
        categoriesFlow.value = this.categories.toList()
    }
    
    fun setRecurringExpenses(vararg recurringExpenses: RecurringExpense) {
        this.recurringExpenses.clear()
        this.recurringExpenses.addAll(recurringExpenses)
        recurringExpensesFlow.value = this.recurringExpenses.toList()
    }
    
    fun clearAll() {
        expenses.clear()
        categories.clear()
        recurringExpenses.clear()
        insertedExpenses.clear()
        updatedExpenses.clear()
        deletedExpenseIds.clear()
        expensesFlow.value = emptyList()
        categoriesFlow.value = emptyList()
        recurringExpensesFlow.value = emptyList()
    }
}