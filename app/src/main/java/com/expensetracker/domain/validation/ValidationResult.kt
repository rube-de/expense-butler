package com.expensetracker.domain.validation

sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val message: String) : ValidationResult()
    
    val isValid: Boolean
        get() = this is Success
    
    val isError: Boolean
        get() = this is Error
    
    fun getErrorMessage(): String? {
        return when (this) {
            is Error -> message
            is Success -> null
        }
    }
}