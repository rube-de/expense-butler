package com.expensetracker.data.model

import java.time.LocalDate

/**
 * Data class representing date range for filtering.
 */
data class DateRange(
    val startDate: LocalDate,
    val endDate: LocalDate
)

/**
 * Data class representing analytics filter state.
 */
data class AnalyticsFilter(
    val selectedCategories: List<Long> = emptyList(),
    val selectedTags: List<String> = emptyList(),
    val customDateRange: DateRange? = null,
    val timePeriod: TimePeriod = TimePeriod.MONTH
)

/**
 * Data class for category drill-down data.
 */
data class CategoryDrillDownData(
    val categoryId: Long,
    val categoryName: String,
    val expenses: List<Expense>,
    val totalAmount: java.math.BigDecimal,
    val tagBreakdown: Map<String, java.math.BigDecimal>,
    val dailyBreakdown: List<DailySpending>
)

/**
 * Data class for daily spending breakdown.
 */
data class DailySpending(
    val date: LocalDate,
    val amount: java.math.BigDecimal,
    val expenseCount: Int
)