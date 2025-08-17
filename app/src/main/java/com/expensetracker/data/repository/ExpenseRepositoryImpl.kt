package com.expensetracker.data.repository

import com.expensetracker.data.dao.CategoryDao
import com.expensetracker.data.dao.ExpenseDao
import com.expensetracker.data.dao.RecurringExpenseDao
import com.expensetracker.data.model.Category
import com.expensetracker.data.model.Expense
import com.expensetracker.data.model.RecurringExpense
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of ExpenseRepository with business logic validation and error handling.
 */
@Singleton
class ExpenseRepositoryImpl @Inject constructor(
    private val expenseDao: ExpenseDao,
    private val categoryDao: CategoryDao,
    private val recurringExpenseDao: RecurringExpenseDao
) : ExpenseRepository {

    // Expense operations
    override fun getAllExpenses(): Flow<List<Expense>> = expenseDao.getAllExpenses()

    override fun getExpenseById(id: Long): Flow<Expense?> = 
        expenseDao.getAllExpenses().map { expenses -> expenses.find { it.id == id } }

    override fun getExpensesByDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<Expense>> {
        val startDateTime = startDate.atStartOfDay()
        val endDateTime = endDate.atTime(23, 59, 59)
        return expenseDao.getExpensesByDateRange(startDateTime, endDateTime)
    }

    override fun getExpensesByCategory(categoryId: Long): Flow<List<Expense>> =
        expenseDao.getExpensesByCategory(categoryId)

    override fun getExpensesByTags(tags: List<String>): Flow<List<Expense>> =
        expenseDao.getAllExpenses().map { expenses ->
            expenses.filter { expense ->
                tags.any { tag -> expense.tags.contains(tag) }
            }
        }

    override fun searchExpenses(searchText: String): Flow<List<Expense>> =
        expenseDao.searchExpensesByDescription(searchText)

    override fun getFilteredExpenses(
        categoryId: Long?,
        startDate: LocalDate?,
        endDate: LocalDate?,
        searchText: String?,
        tags: List<String>?
    ): Flow<List<Expense>> {
        val startDateTime = startDate?.atStartOfDay()
        val endDateTime = endDate?.atTime(23, 59, 59)
        
        return expenseDao.getFilteredExpenses(categoryId, startDateTime, endDateTime, searchText)
            .map { expenses ->
                if (tags.isNullOrEmpty()) {
                    expenses
                } else {
                    expenses.filter { expense ->
                        tags.any { tag -> expense.tags.contains(tag) }
                    }
                }
            }
    }

    override suspend fun insertExpense(expense: Expense): Result<Long> {
        return try {
            // Normalize tags before validation
            val normalizedExpense = expense.copy(tags = normalizeTags(expense.tags))
            
            // Validate business rules
            validateExpense(normalizedExpense)
            
            // Check if category exists
            val category = categoryDao.getCategoryById(normalizedExpense.categoryId)
            if (category == null) {
                return Result.failure(IllegalArgumentException("Category with ID ${normalizedExpense.categoryId} does not exist"))
            }
            
            val expenseId = expenseDao.insertExpense(normalizedExpense)
            Result.success(expenseId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateExpense(expense: Expense): Result<Unit> {
        return try {
            // Normalize tags before validation
            val normalizedExpense = expense.copy(tags = normalizeTags(expense.tags))
            
            // Validate business rules
            validateExpense(normalizedExpense)
            
            // Check if expense exists
            val existingExpense = expenseDao.getExpenseById(normalizedExpense.id)
            if (existingExpense == null) {
                return Result.failure(IllegalArgumentException("Expense with ID ${normalizedExpense.id} does not exist"))
            }
            
            // Check if category exists
            val category = categoryDao.getCategoryById(normalizedExpense.categoryId)
            if (category == null) {
                return Result.failure(IllegalArgumentException("Category with ID ${normalizedExpense.categoryId} does not exist"))
            }
            
            // Update with current timestamp
            val updatedExpense = normalizedExpense.copy(updatedAt = LocalDateTime.now())
            expenseDao.updateExpense(updatedExpense)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteExpense(expenseId: Long): Result<Unit> {
        return try {
            val expense = expenseDao.getExpenseById(expenseId)
            if (expense == null) {
                return Result.failure(IllegalArgumentException("Expense with ID $expenseId does not exist"))
            }
            
            expenseDao.deleteExpenseById(expenseId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteExpensesByCategory(categoryId: Long): Result<Unit> {
        return try {
            expenseDao.deleteExpensesByCategory(categoryId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Category operations
    override fun getAllCategories(): Flow<List<Category>> = categoryDao.getAllCategories()

    override fun getCategoryById(id: Long): Flow<Category?> =
        categoryDao.getAllCategories().map { categories -> categories.find { it.id == id } }

    override fun getDefaultCategories(): Flow<List<Category>> = categoryDao.getDefaultCategories()

    override fun getCustomCategories(): Flow<List<Category>> = categoryDao.getCustomCategories()

    override fun searchCategories(searchText: String): Flow<List<Category>> =
        categoryDao.searchCategoriesByName(searchText)

    override suspend fun insertCategory(category: Category): Result<Long> {
        return try {
            // Validate business rules
            validateCategory(category)
            
            // Check for duplicate names
            val existingCategory = categoryDao.getCategoryByName(category.name)
            if (existingCategory != null) {
                return Result.failure(IllegalArgumentException("Category with name '${category.name}' already exists"))
            }
            
            val categoryId = categoryDao.insertCategory(category)
            Result.success(categoryId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateCategory(category: Category): Result<Unit> {
        return try {
            // Validate business rules
            validateCategory(category)
            
            // Check if category exists
            val existingCategory = categoryDao.getCategoryById(category.id)
            if (existingCategory == null) {
                return Result.failure(IllegalArgumentException("Category with ID ${category.id} does not exist"))
            }
            
            // Check for duplicate names (excluding current category)
            val duplicateCategory = categoryDao.getCategoryByName(category.name)
            if (duplicateCategory != null && duplicateCategory.id != category.id) {
                return Result.failure(IllegalArgumentException("Category with name '${category.name}' already exists"))
            }
            
            categoryDao.updateCategory(category)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteCategory(categoryId: Long): Result<Unit> {
        return try {
            val category = categoryDao.getCategoryById(categoryId)
            if (category == null) {
                return Result.failure(IllegalArgumentException("Category with ID $categoryId does not exist"))
            }
            
            // Check if category is being used by expenses
            val expenseCount = expenseDao.getAllExpenses().map { expenses ->
                expenses.count { it.categoryId == categoryId }
            }
            
            // For now, we'll allow deletion but could add a business rule to prevent it
            categoryDao.deleteCategoryById(categoryId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun initializeDefaultCategories(): Result<Unit> {
        return try {
            val defaultCategoryCount = categoryDao.getDefaultCategoryCount()
            if (defaultCategoryCount == 0) {
                val defaultCategories = createDefaultCategories()
                categoryDao.insertCategories(defaultCategories)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Tag operations
    override suspend fun getAllTags(): List<String> {
        return try {
            val allTagsString = expenseDao.getAllTags()
            val allTags = mutableSetOf<String>()
            
            allTagsString.forEach { tagsString ->
                // Parse the JSON string to extract individual tags
                val tags = parseTagsFromString(tagsString)
                allTags.addAll(tags)
            }
            
            allTags.toList().sorted()
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getTagSuggestions(input: String): List<String> {
        return try {
            if (input.isBlank()) {
                return getPopularTags(10)
            }
            
            val allTags = getAllTags()
            val suggestions = mutableListOf<String>()
            
            // First, add exact matches (case-insensitive)
            allTags.filter { tag ->
                tag.equals(input, ignoreCase = true)
            }.let { suggestions.addAll(it) }
            
            // Then, add tags that start with the input
            allTags.filter { tag ->
                tag.startsWith(input, ignoreCase = true) && !suggestions.contains(tag)
            }.let { suggestions.addAll(it) }
            
            // Finally, add tags that contain the input
            allTags.filter { tag ->
                tag.contains(input, ignoreCase = true) && !suggestions.contains(tag)
            }.let { suggestions.addAll(it) }
            
            suggestions.take(10)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getPopularTags(limit: Int): List<String> {
        return try {
            // Get all expenses and count tag frequency
            val allExpenses = expenseDao.getAllExpenses()
            val tagFrequency = mutableMapOf<String, Int>()
            
            // This is a simplified approach - in production you'd use a Flow and collect
            // For now, we'll return alphabetically sorted tags as a placeholder
            val allTags = getAllTags()
            allTags.take(limit)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Recurring expense operations
    override fun getAllRecurringExpenses(): Flow<List<RecurringExpense>> =
        recurringExpenseDao.getAllRecurringExpenses()

    override fun getActiveRecurringExpenses(): Flow<List<RecurringExpense>> =
        recurringExpenseDao.getActiveRecurringExpenses()

    override fun getRecurringExpenseById(id: Long): Flow<RecurringExpense?> =
        recurringExpenseDao.getAllRecurringExpenses().map { expenses -> expenses.find { it.id == id } }

    override fun getRecurringExpensesByCategory(categoryId: Long): Flow<List<RecurringExpense>> =
        recurringExpenseDao.getRecurringExpensesByCategory(categoryId)

    override suspend fun insertRecurringExpense(recurringExpense: RecurringExpense): Result<Long> {
        return try {
            // Validate business rules
            validateRecurringExpense(recurringExpense)
            
            // Check if category exists
            val category = categoryDao.getCategoryById(recurringExpense.categoryId)
            if (category == null) {
                return Result.failure(IllegalArgumentException("Category with ID ${recurringExpense.categoryId} does not exist"))
            }
            
            val recurringExpenseId = recurringExpenseDao.insertRecurringExpense(recurringExpense)
            Result.success(recurringExpenseId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateRecurringExpense(recurringExpense: RecurringExpense): Result<Unit> {
        return try {
            // Validate business rules
            validateRecurringExpense(recurringExpense)
            
            // Check if recurring expense exists
            val existingRecurringExpense = recurringExpenseDao.getRecurringExpenseById(recurringExpense.id)
            if (existingRecurringExpense == null) {
                return Result.failure(IllegalArgumentException("Recurring expense with ID ${recurringExpense.id} does not exist"))
            }
            
            // Check if category exists
            val category = categoryDao.getCategoryById(recurringExpense.categoryId)
            if (category == null) {
                return Result.failure(IllegalArgumentException("Category with ID ${recurringExpense.categoryId} does not exist"))
            }
            
            recurringExpenseDao.updateRecurringExpense(recurringExpense)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteRecurringExpense(recurringExpenseId: Long): Result<Unit> {
        return try {
            val recurringExpense = recurringExpenseDao.getRecurringExpenseById(recurringExpenseId)
            if (recurringExpense == null) {
                return Result.failure(IllegalArgumentException("Recurring expense with ID $recurringExpenseId does not exist"))
            }
            
            recurringExpenseDao.deleteRecurringExpenseById(recurringExpenseId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateRecurringExpenseActiveStatus(id: Long, isActive: Boolean): Result<Unit> {
        return try {
            val recurringExpense = recurringExpenseDao.getRecurringExpenseById(id)
            if (recurringExpense == null) {
                return Result.failure(IllegalArgumentException("Recurring expense with ID $id does not exist"))
            }
            
            recurringExpenseDao.updateActiveStatus(id, isActive)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateRecurringExpenseLastGenerated(id: Long, lastGenerated: LocalDate): Result<Unit> {
        return try {
            val recurringExpense = recurringExpenseDao.getRecurringExpenseById(id)
            if (recurringExpense == null) {
                return Result.failure(IllegalArgumentException("Recurring expense with ID $id does not exist"))
            }
            
            recurringExpenseDao.updateLastGenerated(id, lastGenerated)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getRecurringExpensesToGenerate(currentDate: LocalDate): List<RecurringExpense> {
        return try {
            recurringExpenseDao.getRecurringExpensesToGenerate(currentDate)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Analytics operations
    override suspend fun getTotalAmountByCategory(categoryId: Long): BigDecimal {
        return try {
            val total = expenseDao.getTotalAmountByCategory(categoryId)
            BigDecimal.valueOf(total ?: 0.0)
        } catch (e: Exception) {
            BigDecimal.ZERO
        }
    }

    override suspend fun getTotalAmountByDateRange(startDate: LocalDate, endDate: LocalDate): BigDecimal {
        return try {
            val startDateTime = startDate.atStartOfDay()
            val endDateTime = endDate.atTime(23, 59, 59)
            val total = expenseDao.getTotalAmountByDateRange(startDateTime, endDateTime)
            BigDecimal.valueOf(total ?: 0.0)
        } catch (e: Exception) {
            BigDecimal.ZERO
        }
    }

    override suspend fun getExpenseCount(): Int {
        return try {
            expenseDao.getExpenseCount()
        } catch (e: Exception) {
            0
        }
    }

    override suspend fun getCategoryCount(): Int {
        return try {
            categoryDao.getCategoryCount()
        } catch (e: Exception) {
            0
        }
    }

    override suspend fun getCategorySpendingBreakdown(startDate: LocalDate, endDate: LocalDate): Map<Long, BigDecimal> {
        return try {
            val startDateTime = startDate.atStartOfDay()
            val endDateTime = endDate.atTime(23, 59, 59)
            val expenses = expenseDao.getExpensesByDateRange(startDateTime, endDateTime)
            
            // This is a simplified approach - collect the flow once
            // In production, you'd handle this more efficiently
            val expenseList = mutableListOf<Expense>()
            expenses.collect { expenseList.addAll(it) }
            
            expenseList.groupBy { it.categoryId }
                .mapValues { (_, expenseList) -> expenseList.sumOf { it.amount } }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    override suspend fun getTagSpendingBreakdown(startDate: LocalDate, endDate: LocalDate): Map<String, BigDecimal> {
        return try {
            val startDateTime = startDate.atStartOfDay()
            val endDateTime = endDate.atTime(23, 59, 59)
            val expenses = expenseDao.getExpensesByDateRange(startDateTime, endDateTime)
            
            // This is a simplified approach - collect the flow once
            val expenseList = mutableListOf<Expense>()
            expenses.collect { expenseList.addAll(it) }
            
            val tagSpending = mutableMapOf<String, BigDecimal>()
            expenseList.forEach { expense ->
                expense.tags.forEach { tag ->
                    tagSpending[tag] = tagSpending.getOrDefault(tag, BigDecimal.ZERO) + expense.amount
                }
            }
            tagSpending
        } catch (e: Exception) {
            emptyMap()
        }
    }

    override suspend fun getMonthlySpendingTrends(startDate: LocalDate, endDate: LocalDate): List<Pair<LocalDate, BigDecimal>> {
        return try {
            val startDateTime = startDate.atStartOfDay()
            val endDateTime = endDate.atTime(23, 59, 59)
            val expenses = expenseDao.getExpensesByDateRange(startDateTime, endDateTime)
            
            // This is a simplified approach - collect the flow once
            val expenseList = mutableListOf<Expense>()
            expenses.collect { expenseList.addAll(it) }
            
            expenseList.groupBy { it.date.toLocalDate().withDayOfMonth(1) }
                .map { (month, expenseList) -> month to expenseList.sumOf { it.amount } }
                .sortedBy { it.first }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Private validation methods
    private fun validateExpense(expense: Expense) {
        require(expense.amount > BigDecimal.ZERO) { "Expense amount must be positive" }
        require(expense.currency.isNotBlank()) { "Currency cannot be blank" }
        require(expense.description.isNotBlank()) { "Description cannot be blank" }
        require(expense.categoryId > 0) { "Category ID must be positive" }
        validateTags(expense.tags)
    }

    private fun validateCategory(category: Category) {
        require(category.name.isNotBlank()) { "Category name cannot be blank" }
        require(category.name.length <= 50) { "Category name cannot exceed 50 characters" }
        require(category.color.matches(Regex("^#[0-9A-Fa-f]{6}$"))) { "Color must be a valid hex color code" }
        require(category.icon.isNotBlank()) { "Icon cannot be blank" }
    }

    private fun validateRecurringExpense(recurringExpense: RecurringExpense) {
        require(recurringExpense.amount > BigDecimal.ZERO) { "Recurring expense amount must be positive" }
        require(recurringExpense.currency.isNotBlank()) { "Currency cannot be blank" }
        require(recurringExpense.description.isNotBlank()) { "Description cannot be blank" }
        require(recurringExpense.categoryId > 0) { "Category ID must be positive" }
        validateTags(recurringExpense.tags)
        require(recurringExpense.endDate == null || recurringExpense.endDate.isAfter(recurringExpense.startDate)) {
            "End date must be after start date"
        }
    }

    private fun createDefaultCategories(): List<Category> {
        return listOf(
            Category(name = "Food & Dining", color = "#FF5722", icon = "restaurant", isDefault = true),
            Category(name = "Transportation", color = "#2196F3", icon = "directions_car", isDefault = true),
            Category(name = "Shopping", color = "#9C27B0", icon = "shopping_cart", isDefault = true),
            Category(name = "Entertainment", color = "#E91E63", icon = "movie", isDefault = true),
            Category(name = "Healthcare", color = "#4CAF50", icon = "local_hospital", isDefault = true),
            Category(name = "Travel", color = "#FF9800", icon = "flight", isDefault = true),
            Category(name = "Utilities", color = "#607D8B", icon = "home", isDefault = true),
            Category(name = "Education", color = "#3F51B5", icon = "school", isDefault = true),
            Category(name = "Personal Care", color = "#795548", icon = "spa", isDefault = true),
            Category(name = "Other", color = "#9E9E9E", icon = "category", isDefault = true)
        )
    }

    private fun parseTagsFromString(tagsString: String): List<String> {
        // This is a simplified implementation
        // In production, you'd use proper JSON parsing with kotlinx.serialization
        return try {
            if (tagsString.isBlank()) return emptyList()
            
            // Remove brackets and quotes, split by comma
            tagsString.trim('[', ']')
                .split(',')
                .map { it.trim().trim('"') }
                .filter { it.isNotBlank() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun validateTags(tags: List<String>) {
        require(tags.size <= 20) { "Maximum 20 tags allowed" }
        require(tags.all { it.isNotBlank() }) { "Tags cannot be blank" }
        require(tags.all { it.length <= 50 }) { "Tag length cannot exceed 50 characters" }
        require(tags.all { !it.contains(",") }) { "Tags cannot contain commas" }
        require(tags.all { !it.contains("[") && !it.contains("]") }) { "Tags cannot contain brackets" }
    }

    private fun normalizeTags(tags: List<String>): List<String> {
        return tags.map { tag ->
            tag.trim().lowercase()
        }.distinct()
    }
}