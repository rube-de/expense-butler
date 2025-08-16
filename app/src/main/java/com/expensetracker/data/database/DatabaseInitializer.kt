package com.expensetracker.data.database

import com.expensetracker.data.dao.CategoryDao
import com.expensetracker.data.model.Category
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseInitializer @Inject constructor(
    private val categoryDao: CategoryDao
) {
    
    suspend fun initializeDatabase() {
        // Check if default categories already exist
        val existingCategoriesCount = categoryDao.getDefaultCategoryCount()
        
        if (existingCategoriesCount == 0) {
            // Insert default categories
            val defaultCategories = listOf(
                Category(
                    name = "Food & Dining",
                    color = "#FF5722",
                    icon = "restaurant",
                    isDefault = true
                ),
                Category(
                    name = "Transportation",
                    color = "#2196F3",
                    icon = "directions_car",
                    isDefault = true
                ),
                Category(
                    name = "Shopping",
                    color = "#FF9800",
                    icon = "shopping_cart",
                    isDefault = true
                ),
                Category(
                    name = "Entertainment",
                    color = "#9C27B0",
                    icon = "movie",
                    isDefault = true
                ),
                Category(
                    name = "Healthcare",
                    color = "#4CAF50",
                    icon = "local_hospital",
                    isDefault = true
                ),
                Category(
                    name = "Housing",
                    color = "#795548",
                    icon = "home",
                    isDefault = true
                ),
                Category(
                    name = "Utilities",
                    color = "#607D8B",
                    icon = "flash_on",
                    isDefault = true
                ),
                Category(
                    name = "Travel",
                    color = "#00BCD4",
                    icon = "flight",
                    isDefault = true
                ),
                Category(
                    name = "Education",
                    color = "#3F51B5",
                    icon = "school",
                    isDefault = true
                ),
                Category(
                    name = "Personal Care",
                    color = "#E91E63",
                    icon = "spa",
                    isDefault = true
                ),
                Category(
                    name = "Gifts & Donations",
                    color = "#8BC34A",
                    icon = "card_giftcard",
                    isDefault = true
                ),
                Category(
                    name = "Business",
                    color = "#FFC107",
                    icon = "business",
                    isDefault = true
                ),
                Category(
                    name = "Other",
                    color = "#9E9E9E",
                    icon = "category",
                    isDefault = true
                )
            )
            
            categoryDao.insertCategories(defaultCategories)
        }
    }
}