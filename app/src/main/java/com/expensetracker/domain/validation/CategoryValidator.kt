package com.expensetracker.domain.validation

import com.expensetracker.data.model.Category
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryValidator @Inject constructor() : Validator<Category?> {
    
    override fun validate(value: Category?): ValidationResult {
        return when {
            value == null -> ValidationResult.Error("Please select a category")
            else -> ValidationResult.Success
        }
    }
    
    fun validateCategoryCreation(name: String, color: String, icon: String): ValidationResult {
        return when {
            name.isBlank() -> ValidationResult.Error("Category name is required")
            name.length < 2 -> ValidationResult.Error("Category name must be at least 2 characters")
            name.length > 50 -> ValidationResult.Error("Category name is too long (max 50 characters)")
            !color.matches(Regex("^#[0-9A-Fa-f]{6}$")) -> ValidationResult.Error("Invalid color format (must be hex color)")
            icon.isBlank() -> ValidationResult.Error("Category icon is required")
            else -> ValidationResult.Success
        }
    }
    
    fun validateCategoryName(name: String): ValidationResult {
        return when {
            name.isBlank() -> ValidationResult.Error("Category name is required")
            name.length < 2 -> ValidationResult.Error("Category name must be at least 2 characters")
            name.length > 50 -> ValidationResult.Error("Category name is too long (max 50 characters)")
            name.trim() != name -> ValidationResult.Error("Category name cannot start or end with spaces")
            else -> ValidationResult.Success
        }
    }
}