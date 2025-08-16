package com.expensetracker.data.dao

import androidx.room.*
import com.expensetracker.data.model.RecurringExpense
import com.expensetracker.data.model.RecurrenceFrequency
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface RecurringExpenseDao {
    
    @Query("SELECT * FROM recurring_expenses ORDER BY createdAt DESC")
    fun getAllRecurringExpenses(): Flow<List<RecurringExpense>>
    
    @Query("SELECT * FROM recurring_expenses WHERE id = :id")
    suspend fun getRecurringExpenseById(id: Long): RecurringExpense?
    
    @Query("SELECT * FROM recurring_expenses WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getActiveRecurringExpenses(): Flow<List<RecurringExpense>>
    
    @Query("SELECT * FROM recurring_expenses WHERE isActive = 0 ORDER BY createdAt DESC")
    fun getInactiveRecurringExpenses(): Flow<List<RecurringExpense>>
    
    @Query("SELECT * FROM recurring_expenses WHERE categoryId = :categoryId ORDER BY createdAt DESC")
    fun getRecurringExpensesByCategory(categoryId: Long): Flow<List<RecurringExpense>>
    
    @Query("SELECT * FROM recurring_expenses WHERE frequency = :frequency ORDER BY createdAt DESC")
    fun getRecurringExpensesByFrequency(frequency: RecurrenceFrequency): Flow<List<RecurringExpense>>
    
    @Query("""
        SELECT * FROM recurring_expenses 
        WHERE isActive = 1 
        AND startDate <= :currentDate 
        AND (endDate IS NULL OR endDate >= :currentDate)
        ORDER BY startDate ASC
    """)
    fun getDueRecurringExpenses(currentDate: LocalDate): Flow<List<RecurringExpense>>
    
    @Query("""
        SELECT * FROM recurring_expenses 
        WHERE isActive = 1 
        AND startDate <= :currentDate 
        AND (endDate IS NULL OR endDate >= :currentDate)
        AND (lastGenerated IS NULL OR lastGenerated < :currentDate)
        ORDER BY startDate ASC
    """)
    suspend fun getRecurringExpensesToGenerate(currentDate: LocalDate): List<RecurringExpense>
    
    @Query("SELECT * FROM recurring_expenses WHERE description LIKE '%' || :searchText || '%' ORDER BY createdAt DESC")
    fun searchRecurringExpensesByDescription(searchText: String): Flow<List<RecurringExpense>>
    
    @Query("SELECT * FROM recurring_expenses WHERE tags LIKE '%' || :tag || '%' ORDER BY createdAt DESC")
    fun getRecurringExpensesByTag(tag: String): Flow<List<RecurringExpense>>
    
    @Query("SELECT COUNT(*) FROM recurring_expenses")
    suspend fun getRecurringExpenseCount(): Int
    
    @Query("SELECT COUNT(*) FROM recurring_expenses WHERE isActive = 1")
    suspend fun getActiveRecurringExpenseCount(): Int
    
    @Query("SELECT COUNT(*) FROM recurring_expenses WHERE isActive = 0")
    suspend fun getInactiveRecurringExpenseCount(): Int
    
    @Query("SELECT SUM(amount) FROM recurring_expenses WHERE isActive = 1")
    suspend fun getTotalActiveRecurringAmount(): Double?
    
    @Insert
    suspend fun insertRecurringExpense(recurringExpense: RecurringExpense): Long
    
    @Insert
    suspend fun insertRecurringExpenses(recurringExpenses: List<RecurringExpense>): List<Long>
    
    @Update
    suspend fun updateRecurringExpense(recurringExpense: RecurringExpense)
    
    @Query("UPDATE recurring_expenses SET lastGenerated = :lastGenerated WHERE id = :id")
    suspend fun updateLastGenerated(id: Long, lastGenerated: LocalDate)
    
    @Query("UPDATE recurring_expenses SET isActive = :isActive WHERE id = :id")
    suspend fun updateActiveStatus(id: Long, isActive: Boolean)
    
    @Delete
    suspend fun deleteRecurringExpense(recurringExpense: RecurringExpense)
    
    @Query("DELETE FROM recurring_expenses WHERE id = :id")
    suspend fun deleteRecurringExpenseById(id: Long)
    
    @Query("DELETE FROM recurring_expenses WHERE isActive = 0")
    suspend fun deleteInactiveRecurringExpenses()
    
    @Query("DELETE FROM recurring_expenses")
    suspend fun deleteAllRecurringExpenses()
    
    @Query("DELETE FROM recurring_expenses WHERE categoryId = :categoryId")
    suspend fun deleteRecurringExpensesByCategory(categoryId: Long)
}