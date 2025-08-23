package com.expensetracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: BigDecimal,
    val currency: String,
    val description: String,
    val categoryId: Long,
    val tags: List<String>, // Will be converted using TypeConverter
    val date: LocalDateTime,
    val recurringExpenseId: Long? = null, // Links to source recurring expense if generated
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
) {
    init {
        require(amount > BigDecimal.ZERO) { "Amount must be positive" }
        require(currency.isNotBlank()) { "Currency cannot be blank" }
        require(description.isNotBlank()) { "Description cannot be blank" }
        require(categoryId > 0) { "Category ID must be positive" }
    }
}