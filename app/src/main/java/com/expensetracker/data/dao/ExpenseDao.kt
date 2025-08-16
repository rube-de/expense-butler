package com.expensetracker.data.dao

import androidx.room.*
import com.expensetracker.data.model.Expense
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime

@Dao
interface ExpenseDao {
    
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<Expense>>
    
    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getExpenseById(id: Long): Expense?
    
    @Query("SELECT * FROM expenses WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getExpensesByDateRange(startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<Expense>>
    
    @Query("SELECT * FROM expenses WHERE categoryId = :categoryId ORDER BY date DESC")
    fun getExpensesByCategory(categoryId: Long): Flow<List<Expense>>
    
    @Query("SELECT * FROM expenses WHERE tags LIKE '%' || :tag || '%' ORDER BY date DESC")
    fun getExpensesByTag(tag: String): Flow<List<Expense>>
    
    @Query("""
        SELECT * FROM expenses 
        WHERE (:categoryId IS NULL OR categoryId = :categoryId)
        AND (:startDate IS NULL OR date >= :startDate)
        AND (:endDate IS NULL OR date <= :endDate)
        AND (:searchText IS NULL OR description LIKE '%' || :searchText || '%')
        ORDER BY date DESC
    """)
    fun getFilteredExpenses(
        categoryId: Long? = null,
        startDate: LocalDateTime? = null,
        endDate: LocalDateTime? = null,
        searchText: String? = null
    ): Flow<List<Expense>>
    
    @Query("SELECT * FROM expenses WHERE description LIKE '%' || :searchText || '%' ORDER BY date DESC")
    fun searchExpensesByDescription(searchText: String): Flow<List<Expense>>
    
    @Query("SELECT SUM(amount) FROM expenses WHERE categoryId = :categoryId")
    suspend fun getTotalAmountByCategory(categoryId: Long): Double?
    
    @Query("SELECT SUM(amount) FROM expenses WHERE date >= :startDate AND date <= :endDate")
    suspend fun getTotalAmountByDateRange(startDate: LocalDateTime, endDate: LocalDateTime): Double?
    
    @Query("SELECT DISTINCT tags FROM expenses")
    suspend fun getAllTags(): List<String>
    
    @Query("SELECT COUNT(*) FROM expenses")
    suspend fun getExpenseCount(): Int
    
    @Insert
    suspend fun insertExpense(expense: Expense): Long
    
    @Insert
    suspend fun insertExpenses(expenses: List<Expense>): List<Long>
    
    @Update
    suspend fun updateExpense(expense: Expense)
    
    @Delete
    suspend fun deleteExpense(expense: Expense)
    
    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: Long)
    
    @Query("DELETE FROM expenses")
    suspend fun deleteAllExpenses()
    
    @Query("DELETE FROM expenses WHERE categoryId = :categoryId")
    suspend fun deleteExpensesByCategory(categoryId: Long)
}