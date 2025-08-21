package com.expensetracker.domain.recurring

import java.math.BigDecimal
import java.time.LocalDate

/**
 * Represents a forecasted expense that hasn't been saved to the database yet.
 * Used for preview functionality to show users what recurring expenses will look like.
 */
data class ForecastedExpense(
    val amount: BigDecimal,
    val currency: String,
    val description: String,
    val date: LocalDate,
    val categoryId: Long,
    val tags: List<String>,
    val sourceRecurringExpenseId: Long
) {
    init {
        require(amount > BigDecimal.ZERO) { "Amount must be positive" }
        require(currency.isNotBlank()) { "Currency cannot be blank" }
        require(description.isNotBlank()) { "Description cannot be blank" }
        require(categoryId > 0) { "Category ID must be positive" }
        require(sourceRecurringExpenseId > 0) { "Source recurring expense ID must be positive" }
    }
}

/**
 * Forecast period options for generating expense previews.
 */
enum class ForecastPeriod {
    NEXT_MONTH,
    REST_OF_YEAR,
    CUSTOM
}