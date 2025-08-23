package com.expensetracker.domain.validation

interface Validator<T> {
    fun validate(value: T): ValidationResult
}