package com.expensetracker.data.dao

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.expensetracker.data.database.AppDatabase
import com.expensetracker.data.model.Category
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class CategoryDaoTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: AppDatabase
    private lateinit var categoryDao: CategoryDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        
        categoryDao = database.categoryDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndGetCategory() = runTest {
        val category = Category(
            name = "Food",
            color = "#FF5722",
            icon = "restaurant",
            isDefault = true
        )
        
        val categoryId = categoryDao.insertCategory(category)
        assertTrue(categoryId > 0)

        val retrievedCategory = categoryDao.getCategoryById(categoryId)
        assertNotNull(retrievedCategory)
        assertEquals(category.name, retrievedCategory!!.name)
        assertEquals(category.color, retrievedCategory.color)
        assertEquals(category.icon, retrievedCategory.icon)
        assertEquals(category.isDefault, retrievedCategory.isDefault)
    }

    @Test
    fun getAllCategories() = runTest {
        val categories = listOf(
            Category(name = "Food", color = "#FF5722", icon = "restaurant"),
            Category(name = "Transport", color = "#2196F3", icon = "directions_car"),
            Category(name = "Entertainment", color = "#9C27B0", icon = "movie")
        )
        
        categoryDao.insertCategories(categories)

        val allCategories = categoryDao.getAllCategories().first()
        assertEquals(3, allCategories.size)
        
        // Should be ordered by name ASC
        assertEquals("Entertainment", allCategories[0].name)
        assertEquals("Food", allCategories[1].name)
        assertEquals("Transport", allCategories[2].name)
    }

    @Test
    fun updateCategory() = runTest {
        val category = Category(
            name = "Original Name",
            color = "#FF5722",
            icon = "original_icon"
        )
        
        val categoryId = categoryDao.insertCategory(category)
        
        val updatedCategory = category.copy(
            id = categoryId,
            name = "Updated Name",
            color = "#2196F3",
            icon = "updated_icon"
        )
        
        categoryDao.updateCategory(updatedCategory)

        val retrievedCategory = categoryDao.getCategoryById(categoryId)
        assertNotNull(retrievedCategory)
        assertEquals("Updated Name", retrievedCategory!!.name)
        assertEquals("#2196F3", retrievedCategory.color)
        assertEquals("updated_icon", retrievedCategory.icon)
    }

    @Test
    fun deleteCategory() = runTest {
        val category = Category(
            name = "To be deleted",
            color = "#FF5722",
            icon = "delete"
        )
        
        val categoryId = categoryDao.insertCategory(category)
        
        // Verify category exists
        assertNotNull(categoryDao.getCategoryById(categoryId))
        
        // Delete category
        categoryDao.deleteCategoryById(categoryId)
        
        // Verify category is deleted
        assertNull(categoryDao.getCategoryById(categoryId))
    }

    @Test
    fun getCategoryByName() = runTest {
        val category = Category(
            name = "Unique Name",
            color = "#FF5722",
            icon = "unique"
        )
        
        categoryDao.insertCategory(category)

        val retrievedCategory = categoryDao.getCategoryByName("Unique Name")
        assertNotNull(retrievedCategory)
        assertEquals("Unique Name", retrievedCategory!!.name)
        
        // Test non-existent category
        val nonExistentCategory = categoryDao.getCategoryByName("Non-existent")
        assertNull(nonExistentCategory)
    }

    @Test
    fun getDefaultCategories() = runTest {
        val categories = listOf(
            Category(name = "Food", color = "#FF5722", icon = "restaurant", isDefault = true),
            Category(name = "Transport", color = "#2196F3", icon = "directions_car", isDefault = false),
            Category(name = "Entertainment", color = "#9C27B0", icon = "movie", isDefault = true),
            Category(name = "Shopping", color = "#FF9800", icon = "shopping_cart", isDefault = false)
        )
        
        categoryDao.insertCategories(categories)

        val defaultCategories = categoryDao.getDefaultCategories().first()
        assertEquals(2, defaultCategories.size)
        assertTrue(defaultCategories.all { it.isDefault })
        
        // Should be ordered by name ASC
        assertEquals("Entertainment", defaultCategories[0].name)
        assertEquals("Food", defaultCategories[1].name)
    }

    @Test
    fun getCustomCategories() = runTest {
        val categories = listOf(
            Category(name = "Food", color = "#FF5722", icon = "restaurant", isDefault = true),
            Category(name = "Transport", color = "#2196F3", icon = "directions_car", isDefault = false),
            Category(name = "Entertainment", color = "#9C27B0", icon = "movie", isDefault = true),
            Category(name = "Shopping", color = "#FF9800", icon = "shopping_cart", isDefault = false)
        )
        
        categoryDao.insertCategories(categories)

        val customCategories = categoryDao.getCustomCategories().first()
        assertEquals(2, customCategories.size)
        assertTrue(customCategories.all { !it.isDefault })
        
        // Should be ordered by name ASC
        assertEquals("Shopping", customCategories[0].name)
        assertEquals("Transport", customCategories[1].name)
    }

    @Test
    fun searchCategoriesByName() = runTest {
        val categories = listOf(
            Category(name = "Food & Drinks", color = "#FF5722", icon = "restaurant"),
            Category(name = "Fast Food", color = "#FF5722", icon = "fastfood"),
            Category(name = "Transport", color = "#2196F3", icon = "directions_car"),
            Category(name = "Entertainment", color = "#9C27B0", icon = "movie")
        )
        
        categoryDao.insertCategories(categories)

        // Search for categories containing "food"
        val foodCategories = categoryDao.searchCategoriesByName("food").first()
        assertEquals(2, foodCategories.size)
        assertTrue(foodCategories.all { it.name.contains("food", ignoreCase = true) })
        
        // Should be ordered by name ASC
        assertEquals("Fast Food", foodCategories[0].name)
        assertEquals("Food & Drinks", foodCategories[1].name)
    }

    @Test
    fun getCategoryCounts() = runTest {
        // Initially should be 0
        assertEquals(0, categoryDao.getCategoryCount())
        assertEquals(0, categoryDao.getDefaultCategoryCount())
        assertEquals(0, categoryDao.getCustomCategoryCount())

        val categories = listOf(
            Category(name = "Food", color = "#FF5722", icon = "restaurant", isDefault = true),
            Category(name = "Transport", color = "#2196F3", icon = "directions_car", isDefault = false),
            Category(name = "Entertainment", color = "#9C27B0", icon = "movie", isDefault = true)
        )
        
        categoryDao.insertCategories(categories)

        assertEquals(3, categoryDao.getCategoryCount())
        assertEquals(2, categoryDao.getDefaultCategoryCount())
        assertEquals(1, categoryDao.getCustomCategoryCount())
    }

    @Test
    fun deleteAllCustomCategories() = runTest {
        val categories = listOf(
            Category(name = "Food", color = "#FF5722", icon = "restaurant", isDefault = true),
            Category(name = "Transport", color = "#2196F3", icon = "directions_car", isDefault = false),
            Category(name = "Shopping", color = "#FF9800", icon = "shopping_cart", isDefault = false)
        )
        
        categoryDao.insertCategories(categories)

        // Verify initial state
        assertEquals(3, categoryDao.getCategoryCount())
        assertEquals(1, categoryDao.getDefaultCategoryCount())
        assertEquals(2, categoryDao.getCustomCategoryCount())

        // Delete all custom categories
        categoryDao.deleteAllCustomCategories()

        // Verify only default categories remain
        assertEquals(1, categoryDao.getCategoryCount())
        assertEquals(1, categoryDao.getDefaultCategoryCount())
        assertEquals(0, categoryDao.getCustomCategoryCount())
        
        val remainingCategories = categoryDao.getAllCategories().first()
        assertEquals(1, remainingCategories.size)
        assertEquals("Food", remainingCategories[0].name)
        assertTrue(remainingCategories[0].isDefault)
    }

    @Test
    fun deleteAllCategories() = runTest {
        val categories = listOf(
            Category(name = "Food", color = "#FF5722", icon = "restaurant", isDefault = true),
            Category(name = "Transport", color = "#2196F3", icon = "directions_car", isDefault = false)
        )
        
        categoryDao.insertCategories(categories)

        // Verify categories exist
        assertEquals(2, categoryDao.getCategoryCount())

        // Delete all categories
        categoryDao.deleteAllCategories()

        // Verify all categories are deleted
        assertEquals(0, categoryDao.getCategoryCount())
        val allCategories = categoryDao.getAllCategories().first()
        assertTrue(allCategories.isEmpty())
    }
}