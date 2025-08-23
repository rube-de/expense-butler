package com.expensetracker.test

import com.expensetracker.data.model.Category
import com.expensetracker.data.model.Expense
import com.expensetracker.data.model.RecurringExpense
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.LocalDate

/**
 * Builder classes for creating consistent test data across all test classes.
 * Follows the builder pattern with sensible defaults and fluent API.
 */

/**
 * Builder for creating test Expense objects with sensible defaults
 */
class TestExpenseBuilder {
    private var id: Long = 0L
    private var amount: BigDecimal = BigDecimal("25.50")
    private var currency: String = "USD"
    private var description: String = "Test Expense"
    private var categoryId: Long = 1L
    private var tags: List<String> = emptyList()
    private var date: LocalDateTime = LocalDateTime.now()
    private var createdAt: LocalDateTime = LocalDateTime.now()
    private var updatedAt: LocalDateTime = LocalDateTime.now()

    fun withId(id: Long) = apply { this.id = id }
    fun withAmount(amount: String) = apply { this.amount = BigDecimal(amount) }
    fun withAmount(amount: BigDecimal) = apply { this.amount = amount }
    fun withCurrency(currency: String) = apply { this.currency = currency }
    fun withDescription(description: String) = apply { this.description = description }
    fun withCategoryId(categoryId: Long) = apply { this.categoryId = categoryId }
    fun withTags(vararg tags: String) = apply { this.tags = tags.toList() }
    fun withTags(tags: List<String>) = apply { this.tags = tags }
    fun withDate(date: LocalDateTime) = apply { this.date = date }
    fun withDateDaysAgo(days: Long) = apply { this.date = LocalDateTime.now().minusDays(days) }

    fun build() = Expense(
        id = id,
        amount = amount,
        currency = currency,
        description = description,
        categoryId = categoryId,
        tags = tags,
        date = date,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

/**
 * Builder for creating test Category objects with sensible defaults
 */
class TestCategoryBuilder {
    private var id: Long = 0L
    private var name: String = "Test Category"
    private var color: String = "#FF5722"
    private var icon: String = "restaurant"
    private var isDefault: Boolean = false
    private var createdAt: LocalDateTime = LocalDateTime.now()

    fun withId(id: Long) = apply { this.id = id }
    fun withName(name: String) = apply { this.name = name }
    fun withColor(color: String) = apply { this.color = color }
    fun withIcon(icon: String) = apply { this.icon = icon }
    fun withIsDefault(isDefault: Boolean) = apply { this.isDefault = isDefault }

    fun build() = Category(
        id = id,
        name = name,
        color = color,
        icon = icon,
        isDefault = isDefault,
        createdAt = createdAt
    )
}

/**
 * Builder for creating test RecurringExpense objects with sensible defaults
 */
class TestRecurringExpenseBuilder {
    private var id: Long = 0L
    private var amount: BigDecimal = BigDecimal("100.00")
    private var currency: String = "USD"
    private var description: String = "Monthly Subscription"
    private var categoryId: Long = 1L
    private var tags: List<String> = emptyList()
    private var frequency: com.expensetracker.data.model.RecurrenceFrequency = 
        com.expensetracker.data.model.RecurrenceFrequency.MONTHLY
    private var startDate: LocalDate = LocalDate.now()
    private var endDate: LocalDate? = null
    private var lastGenerated: LocalDate? = null
    private var isActive: Boolean = true
    private var createdAt: LocalDateTime = LocalDateTime.now()

    fun withId(id: Long) = apply { this.id = id }
    fun withAmount(amount: String) = apply { this.amount = BigDecimal(amount) }
    fun withAmount(amount: BigDecimal) = apply { this.amount = amount }
    fun withCurrency(currency: String) = apply { this.currency = currency }
    fun withDescription(description: String) = apply { this.description = description }
    fun withCategoryId(categoryId: Long) = apply { this.categoryId = categoryId }
    fun withTags(vararg tags: String) = apply { this.tags = tags.toList() }
    fun withTags(tags: List<String>) = apply { this.tags = tags }
    fun withFrequency(frequency: com.expensetracker.data.model.RecurrenceFrequency) = apply { 
        this.frequency = frequency 
    }
    fun withStartDate(startDate: LocalDate) = apply { this.startDate = startDate }
    fun withEndDate(endDate: LocalDate?) = apply { this.endDate = endDate }
    fun withLastGenerated(lastGenerated: LocalDate?) = apply { this.lastGenerated = lastGenerated }
    fun withIsActive(isActive: Boolean) = apply { this.isActive = isActive }

    fun build() = RecurringExpense(
        id = id,
        amount = amount,
        currency = currency,
        description = description,
        categoryId = categoryId,
        tags = tags,
        frequency = frequency,
        startDate = startDate,
        endDate = endDate,
        lastGenerated = lastGenerated,
        isActive = isActive,
        createdAt = createdAt
    )
}

/**
 * Factory object providing common test data configurations
 */
object TestData {
    
    /**
     * Creates a simple valid expense for basic tests
     */
    fun createValidExpense() = TestExpenseBuilder()
        .withAmount("25.50")
        .withDescription("Coffee")
        .withCategoryId(1L)
        .withTags("coffee", "drinks")
        .build()

    /**
     * Creates an expense with a specific amount for calculation tests
     */
    fun createExpenseWithAmount(amount: String) = TestExpenseBuilder()
        .withAmount(amount)
        .withDescription("Test expense for calculations")
        .build()

    /**
     * Creates multiple expenses for different categories (useful for analytics)
     */
    fun createExpensesForAnalytics(): List<Expense> {
        return listOf(
            TestExpenseBuilder()
                .withAmount("25.50")
                .withDescription("Coffee")
                .withCategoryId(1L) // Food & Dining
                .withTags("coffee")
                .withDateDaysAgo(1)
                .build(),
            TestExpenseBuilder()
                .withAmount("15.75")
                .withDescription("Bus ticket")
                .withCategoryId(2L) // Transportation
                .withTags("transport", "bus")
                .withDateDaysAgo(2)
                .build(),
            TestExpenseBuilder()
                .withAmount("120.00")
                .withDescription("Groceries")
                .withCategoryId(1L) // Food & Dining
                .withTags("groceries", "food")
                .withDateDaysAgo(3)
                .build(),
            TestExpenseBuilder()
                .withAmount("80.00")
                .withDescription("Movie tickets")
                .withCategoryId(3L) // Entertainment
                .withTags("movies", "entertainment")
                .withDateDaysAgo(5)
                .build()
        )
    }

    /**
     * Creates default categories that match the ones in the production app
     */
    fun createDefaultCategories(): List<Category> {
        return listOf(
            TestCategoryBuilder()
                .withId(1L)
                .withName("Food & Dining")
                .withColor("#FF5722")
                .withIcon("restaurant")
                .withIsDefault(true)
                .build(),
            TestCategoryBuilder()
                .withId(2L)
                .withName("Transportation")
                .withColor("#2196F3")
                .withIcon("directions_car")
                .withIsDefault(true)
                .build(),
            TestCategoryBuilder()
                .withId(3L)
                .withName("Entertainment")
                .withColor("#9C27B0")
                .withIcon("movie")
                .withIsDefault(true)
                .build(),
            TestCategoryBuilder()
                .withId(4L)
                .withName("Shopping")
                .withColor("#E91E63")
                .withIcon("shopping_bag")
                .withIsDefault(true)
                .build(),
            TestCategoryBuilder()
                .withId(5L)
                .withName("Healthcare")
                .withColor("#4CAF50")
                .withIcon("local_hospital")
                .withIsDefault(true)
                .build()
        )
    }

    /**
     * Creates a simple category for basic tests
     */
    fun createTestCategory() = TestCategoryBuilder()
        .withName("Test Category")
        .withColor("#FF5722")
        .withIcon("category")
        .build()

    /**
     * Creates a simple recurring expense for basic tests
     */
    fun createTestRecurringExpense() = TestRecurringExpenseBuilder()
        .withDescription("Monthly Rent")
        .withAmount("1200.00")
        .withCategoryId(1L)
        .withFrequency(com.expensetracker.data.model.RecurrenceFrequency.MONTHLY)
        .build()

    /**
     * Creates an expense with zero amount for validation tests
     */
    fun createInvalidExpenseZeroAmount() = TestExpenseBuilder()
        .withAmount(BigDecimal.ZERO)
        .withDescription("Invalid expense")
        .build()

    /**
     * Creates an expense with negative amount for validation tests
     */
    fun createInvalidExpenseNegativeAmount() = TestExpenseBuilder()
        .withAmount(BigDecimal("-10.00"))
        .withDescription("Invalid expense")
        .build()

    /**
     * Creates an expense with empty description for validation tests
     */
    fun createInvalidExpenseEmptyDescription() = TestExpenseBuilder()
        .withAmount("25.50")
        .withDescription("")
        .build()
}