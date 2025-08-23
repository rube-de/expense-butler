package com.expensetracker.domain.validation

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DescriptionValidator @Inject constructor() : Validator<String> {
    
    companion object {
        private const val MIN_LENGTH = 1
        private const val MAX_LENGTH = 500
    }
    
    override fun validate(value: String): ValidationResult {
        val trimmedValue = value.trim()
        
        return when {
            trimmedValue.isBlank() -> ValidationResult.Error("Description is required")
            trimmedValue.length < MIN_LENGTH -> ValidationResult.Error("Description cannot be empty")
            trimmedValue.length > MAX_LENGTH -> ValidationResult.Error("Description is too long (max $MAX_LENGTH characters)")
            else -> ValidationResult.Success
        }
    }
    
    fun filterDescriptionInput(input: String): String {
        // Remove excessive whitespace but allow normal spaces
        return input.replace(Regex("\\s+"), " ")
    }
}