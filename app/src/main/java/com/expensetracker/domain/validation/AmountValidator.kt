package com.expensetracker.domain.validation

import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AmountValidator @Inject constructor() : Validator<BigDecimal?> {
    
    companion object {
        private val MIN_AMOUNT = BigDecimal.ZERO
        private val MAX_AMOUNT = BigDecimal("999999999.99")
    }
    
    override fun validate(value: BigDecimal?): ValidationResult {
        return when {
            value == null -> ValidationResult.Error("Amount is required")
            value <= MIN_AMOUNT -> ValidationResult.Error("Amount must be greater than 0")
            value > MAX_AMOUNT -> ValidationResult.Error("Amount is too large")
            else -> ValidationResult.Success
        }
    }
    
    fun validateAmountString(amountString: String): ValidationResult {
        return when {
            amountString.isBlank() -> ValidationResult.Error("Amount is required")
            else -> {
                try {
                    val amount = BigDecimal(amountString)
                    validate(amount)
                } catch (e: NumberFormatException) {
                    ValidationResult.Error("Invalid amount format")
                }
            }
        }
    }
    
    fun filterAmountInput(input: String): String {
        // Allow only numbers and decimal point
        val filtered = input.filter { it.isDigit() || it == '.' }
        
        // Prevent multiple decimal points
        val decimalCount = filtered.count { it == '.' }
        return if (decimalCount <= 1) {
            filtered
        } else {
            val firstDecimalIndex = filtered.indexOf('.')
            filtered.substring(0, firstDecimalIndex + 1) + 
            filtered.substring(firstDecimalIndex + 1).replace(".", "")
        }
    }
}