package com.expensetracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(tableName = "recurring_expenses")
data class RecurringExpense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: BigDecimal,
    val currency: String,
    val description: String,
    val categoryId: Long,
    val tags: List<String>, // Will be converted using TypeConverter
    val frequency: RecurrenceFrequency,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val lastGenerated: LocalDate?,
    val isActive: Boolean = true,
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    init {
        require(amount > BigDecimal.ZERO) { "Amount must be positive" }
        require(currency.isNotBlank()) { "Currency cannot be blank" }
        require(description.isNotBlank()) { "Description cannot be blank" }
        require(categoryId > 0) { "Category ID must be positive" }
        require(endDate == null || endDate.isAfter(startDate)) { "End date must be after start date" }
    }
}

enum class RecurrenceFrequency {
    DAILY, WEEKLY, MONTHLY, YEARLY
}