package com.expensetracker.data.model

import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDateTime

class CategoryTest {

    @Test
    fun `create valid category should succeed`() {
        val category = Category(
            name = "Food",
            color = "#FF5722",
            icon = "restaurant",
            isDefault = true
        )

        assertEquals("Food", category.name)
        assertEquals("#FF5722", category.color)
        assertEquals("restaurant", category.icon)
        assertTrue(category.isDefault)
        assertTrue(category.id == 0L) // Auto-generated ID starts at 0
    }

    @Test
    fun `create category with blank name should throw exception`() {
        assertThrows(IllegalArgumentException::class.java) {
            Category(
                name = "",
                color = "#FF5722",
                icon = "restaurant"
            )
        }
    }

    @Test
    fun `create category with invalid color format should throw exception`() {
        assertThrows(IllegalArgumentException::class.java) {
            Category(
                name = "Food",
                color = "FF5722", // Missing #
                icon = "restaurant"
            )
        }
    }

    @Test
    fun `create category with short color code should throw exception`() {
        assertThrows(IllegalArgumentException::class.java) {
            Category(
                name = "Food",
                color = "#FF57", // Too short
                icon = "restaurant"
            )
        }
    }

    @Test
    fun `create category with long color code should throw exception`() {
        assertThrows(IllegalArgumentException::class.java) {
            Category(
                name = "Food",
                color = "#FF57221", // Too long
                icon = "restaurant"
            )
        }
    }

    @Test
    fun `create category with invalid color characters should throw exception`() {
        assertThrows(IllegalArgumentException::class.java) {
            Category(
                name = "Food",
                color = "#GG5722", // Invalid hex characters
                icon = "restaurant"
            )
        }
    }

    @Test
    fun `create category with blank icon should throw exception`() {
        assertThrows(IllegalArgumentException::class.java) {
            Category(
                name = "Food",
                color = "#FF5722",
                icon = ""
            )
        }
    }

    @Test
    fun `create category with lowercase hex color should succeed`() {
        val category = Category(
            name = "Transport",
            color = "#2196f3",
            icon = "directions_car"
        )

        assertEquals("#2196f3", category.color)
    }

    @Test
    fun `create category with uppercase hex color should succeed`() {
        val category = Category(
            name = "Entertainment",
            color = "#9C27B0",
            icon = "movie"
        )

        assertEquals("#9C27B0", category.color)
    }

    @Test
    fun `create category with mixed case hex color should succeed`() {
        val category = Category(
            name = "Health",
            color = "#4CaF50",
            icon = "local_hospital"
        )

        assertEquals("#4CaF50", category.color)
    }

    @Test
    fun `category should have automatic timestamp`() {
        val beforeCreation = LocalDateTime.now().minusSeconds(1)
        val category = Category(
            name = "Shopping",
            color = "#FF9800",
            icon = "shopping_cart"
        )
        val afterCreation = LocalDateTime.now().plusSeconds(1)

        assertTrue(category.createdAt.isAfter(beforeCreation))
        assertTrue(category.createdAt.isBefore(afterCreation))
    }

    @Test
    fun `category should default to non-default status`() {
        val category = Category(
            name = "Custom",
            color = "#607D8B",
            icon = "category"
        )

        assertFalse(category.isDefault)
    }

    @Test
    fun `category can be marked as default`() {
        val category = Category(
            name = "Food",
            color = "#FF5722",
            icon = "restaurant",
            isDefault = true
        )

        assertTrue(category.isDefault)
    }
}