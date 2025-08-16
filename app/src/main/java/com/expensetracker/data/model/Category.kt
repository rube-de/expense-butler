package com.expensetracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val color: String, // Hex color code
    val icon: String, // Material icon name
    val isDefault: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    init {
        require(name.isNotBlank()) { "Category name cannot be blank" }
        require(color.matches(Regex("^#[0-9A-Fa-f]{6}$"))) { "Color must be a valid hex color code" }
        require(icon.isNotBlank()) { "Icon cannot be blank" }
    }
}