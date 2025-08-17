package com.expensetracker.data.model

import java.math.BigDecimal
import java.time.LocalDate

/**
 * Data class representing analytics information for expense tracking.
 */
data class AnalyticsData(
    val totalSpending: BigDecimal,
    val categoryBreakdown: Map<Category, BigDecimal>,
    val monthlyTrends: List<MonthlySpending>,
    val topTags: List<TagSpending>,
    val periodComparison: PeriodComparison?
)

/**
 * Represents spending data for a specific month.
 */
data class MonthlySpending(
    val month: LocalDate, // First day of the month
    val amount: BigDecimal,
    val expenseCount: Int
)

/**
 * Represents spending data for a specific tag.
 */
data class TagSpending(
    val tag: String,
    val amount: BigDecimal,
    val expenseCount: Int
)

/**
 * Represents comparison between two time periods.
 */
data class PeriodComparison(
    val currentPeriod: PeriodData,
    val previousPeriod: PeriodData,
    val percentageChange: Double
)

/**
 * Represents spending data for a specific time period.
 */
data class PeriodData(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val totalAmount: BigDecimal,
    val expenseCount: Int,
    val averagePerDay: BigDecimal
)

/**
 * Enum representing different time period types for analytics.
 */
enum class TimePeriod {
    MONTH,
    QUARTER,
    YEAR
}