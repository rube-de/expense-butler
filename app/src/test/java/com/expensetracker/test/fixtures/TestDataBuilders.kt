package com.expensetracker.test.fixtures

import com.expensetracker.data.model.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Test data builders following the Builder pattern for creating test objects
 * with sensible defaults that can be customized as needed.
 */

/**
 * Builder for creating test Expense objects with customizable properties.
 */
class ExpenseTestBuilder {
    private var id: Long = 1L
    private var amount: BigDecimal = BigDecimal("50.00")
    private var currency: String = "USD"
    private var description: String = "Test expense"
    private var categoryId: Long = 1L
    private var tags: List<String> = listOf("test", "sample")
    private var date: LocalDateTime = LocalDateTime.now()
    private var createdAt: LocalDateTime = LocalDateTime.now()
    private var updatedAt: LocalDateTime = LocalDateTime.now()
    
    fun withId(id: Long) = apply { this.id = id }
    fun withAmount(amount: BigDecimal) = apply { this.amount = amount }
    fun withAmount(amount: String) = apply { this.amount = BigDecimal(amount) }
    fun withCurrency(currency: String) = apply { this.currency = currency }
    fun withDescription(description: String) = apply { this.description = description }
    fun withCategoryId(categoryId: Long) = apply { this.categoryId = categoryId }
    fun withTags(vararg tags: String) = apply { this.tags = tags.toList() }
    fun withTags(tags: List<String>) = apply { this.tags = tags }
    fun withDate(date: LocalDateTime) = apply { this.date = date }
    fun withCreatedAt(createdAt: LocalDateTime) = apply { this.createdAt = createdAt }
    fun withUpdatedAt(updatedAt: LocalDateTime) = apply { this.updatedAt = updatedAt }
    
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
 * Builder for creating test Category objects.
 */
class CategoryTestBuilder {
    private var id: Long = 1L
    private var name: String = "Food"
    private var color: String = "#FF5722"
    private var icon: String = "restaurant"
    private var isDefault: Boolean = false
    private var createdAt: LocalDateTime = LocalDateTime.now()
    
    fun withId(id: Long) = apply { this.id = id }
    fun withName(name: String) = apply { this.name = name }
    fun withColor(color: String) = apply { this.color = color }
    fun withIcon(icon: String) = apply { this.icon = icon }
    fun asDefault() = apply { this.isDefault = true }
    fun withCreatedAt(createdAt: LocalDateTime) = apply { this.createdAt = createdAt }
    
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
 * Builder for creating test RecurringExpense objects.
 */
class RecurringExpenseTestBuilder {
    private var id: Long = 1L
    private var amount: BigDecimal = BigDecimal("100.00")
    private var currency: String = "USD"
    private var description: String = "Monthly subscription"
    private var categoryId: Long = 1L
    private var tags: List<String> = listOf("subscription", "monthly")
    private var frequency: RecurrenceFrequency = RecurrenceFrequency.MONTHLY
    private var startDate: LocalDate = LocalDate.now()
    private var endDate: LocalDate? = null
    private var lastGenerated: LocalDate? = null
    private var isActive: Boolean = true
    private var createdAt: LocalDateTime = LocalDateTime.now()
    
    fun withId(id: Long) = apply { this.id = id }
    fun withAmount(amount: BigDecimal) = apply { this.amount = amount }
    fun withAmount(amount: String) = apply { this.amount = BigDecimal(amount) }
    fun withCurrency(currency: String) = apply { this.currency = currency }
    fun withDescription(description: String) = apply { this.description = description }
    fun withCategoryId(categoryId: Long) = apply { this.categoryId = categoryId }
    fun withTags(vararg tags: String) = apply { this.tags = tags.toList() }
    fun withFrequency(frequency: RecurrenceFrequency) = apply { this.frequency = frequency }
    fun withStartDate(startDate: LocalDate) = apply { this.startDate = startDate }
    fun withEndDate(endDate: LocalDate?) = apply { this.endDate = endDate }
    fun withLastGenerated(lastGenerated: LocalDate?) = apply { this.lastGenerated = lastGenerated }
    fun asInactive() = apply { this.isActive = false }
    fun withCreatedAt(createdAt: LocalDateTime) = apply { this.createdAt = createdAt }
    
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
 * Builder for creating test AnalyticsData objects.
 */
class AnalyticsDataTestBuilder {
    private var totalSpending: BigDecimal = BigDecimal("500.00")
    private var categoryBreakdown: Map<Category, BigDecimal> = emptyMap()
    private var monthlyTrends: List<MonthlySpending> = emptyList()
    private var topTags: List<TagSpending> = emptyList()
    private var periodComparison: PeriodComparison? = null
    
    fun withTotalSpending(amount: BigDecimal) = apply { this.totalSpending = amount }
    fun withTotalSpending(amount: String) = apply { this.totalSpending = BigDecimal(amount) }
    fun withCategoryBreakdown(breakdown: Map<Category, BigDecimal>) = apply { this.categoryBreakdown = breakdown }
    fun withMonthlyTrends(trends: List<MonthlySpending>) = apply { this.monthlyTrends = trends }
    fun withTopTags(tags: List<TagSpending>) = apply { this.topTags = tags }
    fun withPeriodComparison(comparison: PeriodComparison?) = apply { this.periodComparison = comparison }
    
    fun build() = AnalyticsData(
        totalSpending = totalSpending,
        categoryBreakdown = categoryBreakdown,
        monthlyTrends = monthlyTrends,
        topTags = topTags,
        periodComparison = periodComparison
    )
}

/**
 * Convenience functions for quick test data creation.
 */
object TestData {
    fun expense(init: ExpenseTestBuilder.() -> Unit = {}): Expense {
        return ExpenseTestBuilder().apply(init).build()
    }
    
    fun category(init: CategoryTestBuilder.() -> Unit = {}): Category {
        return CategoryTestBuilder().apply(init).build()
    }
    
    fun recurringExpense(init: RecurringExpenseTestBuilder.() -> Unit = {}): RecurringExpense {
        return RecurringExpenseTestBuilder().apply(init).build()
    }
    
    fun analyticsData(init: AnalyticsDataTestBuilder.() -> Unit = {}): AnalyticsData {
        return AnalyticsDataTestBuilder().apply(init).build()
    }
    
    // Predefined test data sets
    fun sampleExpenses(): List<Expense> = listOf(
        expense { 
            withId(1)
            withAmount("50.00")
            withDescription("Lunch")
            withCategoryId(1)
            withTags("food", "restaurant")
        },
        expense {
            withId(2)
            withAmount("30.00")
            withDescription("Uber ride")
            withCategoryId(2)
            withTags("transport", "uber")
        },
        expense {
            withId(3)
            withAmount("100.00")
            withDescription("Groceries")
            withCategoryId(1)
            withTags("food", "shopping")
        }
    )
    
    fun sampleCategories(): List<Category> = listOf(
        category {
            withId(1)
            withName("Food")
            withIcon("restaurant")
            withColor("#FF5722")
        },
        category {
            withId(2)
            withName("Transport")
            withIcon("directions_car")
            withColor("#2196F3")
        },
        category {
            withId(3)
            withName("Entertainment")
            withIcon("movie")
            withColor("#9C27B0")
        }
    )
}