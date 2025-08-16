package com.expensetracker.data.dao

import androidx.room.*
import com.expensetracker.data.model.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<Category>>
    
    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): Category?
    
    @Query("SELECT * FROM categories WHERE name = :name")
    suspend fun getCategoryByName(name: String): Category?
    
    @Query("SELECT * FROM categories WHERE isDefault = 1 ORDER BY name ASC")
    fun getDefaultCategories(): Flow<List<Category>>
    
    @Query("SELECT * FROM categories WHERE isDefault = 0 ORDER BY name ASC")
    fun getCustomCategories(): Flow<List<Category>>
    
    @Query("SELECT * FROM categories WHERE name LIKE '%' || :searchText || '%' ORDER BY name ASC")
    fun searchCategoriesByName(searchText: String): Flow<List<Category>>
    
    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCategoryCount(): Int
    
    @Query("SELECT COUNT(*) FROM categories WHERE isDefault = 1")
    suspend fun getDefaultCategoryCount(): Int
    
    @Query("SELECT COUNT(*) FROM categories WHERE isDefault = 0")
    suspend fun getCustomCategoryCount(): Int
    
    @Insert
    suspend fun insertCategory(category: Category): Long
    
    @Insert
    suspend fun insertCategories(categories: List<Category>): List<Long>
    
    @Update
    suspend fun updateCategory(category: Category)
    
    @Delete
    suspend fun deleteCategory(category: Category)
    
    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategoryById(id: Long)
    
    @Query("DELETE FROM categories WHERE isDefault = 0")
    suspend fun deleteAllCustomCategories()
    
    @Query("DELETE FROM categories")
    suspend fun deleteAllCategories()
}