package com.expensetracker.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.data.model.*
import com.expensetracker.data.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * ViewModel for analytics screen that processes expense data and provides insights.
 */
@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val repository: ExpenseRepository
) : ViewModel() {

    companion object {
        private const val DEFAULT_TOP_TAGS_LIMIT = 10
        private const val PERCENTAGE_SCALE = 4
        private const val AVERAGE_SCALE = 2
    }

    private val _analyticsData = MutableStateFlow(
        AnalyticsData(
            totalSpending = BigDecimal.ZERO,
            categoryBreakdown = emptyMap(),
            monthlyTrends = emptyList(),
            topTags = emptyList(),
            periodComparison = null
        )
    )
    val analyticsData: StateFlow<AnalyticsData> = _analyticsData.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /**
     * Loads analytics data for the specified time period.
     */
    fun loadAnalytics(period: TimePeriod) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val (startDate, endDate) = getPeriodDateRange(period)
                
                combine(
                    repository.getAllExpenses(),
                    repository.getAllCategories()
                ) { expenses, categories ->
                    processAnalyticsData(expenses, categories, startDate, endDate)
                }.collect { analyticsData ->
                    _analyticsData.value = analyticsData
                }
            } catch (e: Exception) {
                _error.value = "Failed to load analytics data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Compares data between two time periods.
     */
    fun comparePeriodsData(currentPeriod: TimePeriod, previousPeriod: TimePeriod) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val (currentStart, currentEnd) = getPeriodDateRange(currentPeriod)
                val (previousStart, previousEnd) = getPreviousPeriodDateRange(previousPeriod)
                
                combine(
                    repository.getAllExpenses(),
                    repository.getAllCategories()
                ) { expenses, categories ->
                    val currentData = processAnalyticsData(expenses, categories, currentStart, currentEnd)
                    val currentPeriodData = calculatePeriodData(expenses, currentStart, currentEnd)
                    val previousPeriodData = calculatePeriodData(expenses, previousStart, previousEnd)
                    
                    val percentageChange = calculatePercentageChange(
                        currentPeriodData.totalAmount,
                        previousPeriodData.totalAmount
                    )
                    
                    val periodComparison = PeriodComparison(
                        currentPeriod = currentPeriodData,
                        previousPeriod = previousPeriodData,
                        percentageChange = percentageChange
                    )
                    
                    currentData.copy(periodComparison = periodComparison)
                }.collect { analyticsData ->
                    _analyticsData.value = analyticsData
                }
            } catch (e: Exception) {
                _error.value = "Failed to compare periods: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Clears any error state.
     */
    fun clearError() {
        _error.value = null
    }

    private fun processAnalyticsData(
        expenses: List<Expense>,
        categories: List<Category>,
        startDate: LocalDate,
        endDate: LocalDate
    ): AnalyticsData {
        val filteredExpenses = filterExpensesByDateRange(expenses, startDate, endDate)
        
        return AnalyticsData(
            totalSpending = calculateTotalSpending(filteredExpenses),
            categoryBreakdown = calculateCategoryBreakdown(filteredExpenses, categories),
            monthlyTrends = calculateMonthlyTrends(filteredExpenses),
            topTags = calculateTopTags(filteredExpenses),
            periodComparison = null // Will be set separately if needed
        )
    }

    private fun filterExpensesByDateRange(
        expenses: List<Expense>,
        startDate: LocalDate,
        endDate: LocalDate
    ): List<Expense> {
        return expenses.filter { expense ->
            val expenseDate = expense.date.toLocalDate()
            !expenseDate.isBefore(startDate) && !expenseDate.isAfter(endDate)
        }
    }

    private fun calculateTotalSpending(expenses: List<Expense>): BigDecimal {
        return expenses.sumOf { it.amount }
    }

    private fun calculateCategoryBreakdown(
        expenses: List<Expense>,
        categories: List<Category>
    ): Map<Category, BigDecimal> {
        val categoryMap = categories.associateBy { it.id }
        return expenses
            .groupBy { it.categoryId }
            .mapNotNull { (categoryId, expenseList) ->
                categoryMap[categoryId]?.let { category ->
                    category to expenseList.sumOf { it.amount }
                }
            }
            .toMap()
    }

    private fun calculateMonthlyTrends(expenses: List<Expense>): List<MonthlySpending> {
        return expenses
            .groupBy { it.date.toLocalDate().withDayOfMonth(1) }
            .map { (month, expenseList) ->
                MonthlySpending(
                    month = month,
                    amount = expenseList.sumOf { it.amount },
                    expenseCount = expenseList.size
                )
            }
            .sortedBy { it.month }
    }

    private fun calculateTopTags(expenses: List<Expense>): List<TagSpending> {
        val tagMap = mutableMapOf<String, MutableList<Expense>>()
        
        expenses.forEach { expense ->
            expense.tags.forEach { tag ->
                tagMap.getOrPut(tag) { mutableListOf() }.add(expense)
            }
        }
        
        return tagMap.map { (tag, expenseList) ->
            TagSpending(
                tag = tag,
                amount = expenseList.sumOf { it.amount },
                expenseCount = expenseList.size
            )
        }.sortedByDescending { it.amount }
            .take(DEFAULT_TOP_TAGS_LIMIT)
    }

    private fun calculatePeriodData(
        expenses: List<Expense>,
        startDate: LocalDate,
        endDate: LocalDate
    ): PeriodData {
        val filteredExpenses = filterExpensesByDateRange(expenses, startDate, endDate)
        val totalAmount = filteredExpenses.sumOf { it.amount }
        val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1
        val averagePerDay = if (daysBetween > 0) {
            totalAmount.divide(BigDecimal(daysBetween), AVERAGE_SCALE, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }
        
        return PeriodData(
            startDate = startDate,
            endDate = endDate,
            totalAmount = totalAmount,
            expenseCount = filteredExpenses.size,
            averagePerDay = averagePerDay
        )
    }

    private fun calculatePercentageChange(current: BigDecimal, previous: BigDecimal): Double {
        return if (previous == BigDecimal.ZERO) {
            if (current == BigDecimal.ZERO) 0.0 else 100.0
        } else {
            val change = current.subtract(previous)
            change.divide(previous, PERCENTAGE_SCALE, RoundingMode.HALF_UP)
                .multiply(BigDecimal(100))
                .toDouble()
        }
    }

    private fun getPeriodDateRange(period: TimePeriod): Pair<LocalDate, LocalDate> {
        val now = LocalDate.now()
        return when (period) {
            TimePeriod.MONTH -> {
                val startOfMonth = now.withDayOfMonth(1)
                val endOfMonth = now.withDayOfMonth(now.lengthOfMonth())
                startOfMonth to endOfMonth
            }
            TimePeriod.QUARTER -> {
                val quarterStart = now.withDayOfMonth(1).withMonth(((now.monthValue - 1) / 3) * 3 + 1)
                val quarterEnd = quarterStart.plusMonths(2).withDayOfMonth(quarterStart.plusMonths(2).lengthOfMonth())
                quarterStart to quarterEnd
            }
            TimePeriod.YEAR -> {
                val startOfYear = now.withDayOfYear(1)
                val endOfYear = now.withDayOfYear(now.lengthOfYear())
                startOfYear to endOfYear
            }
        }
    }

    private fun getPreviousPeriodDateRange(period: TimePeriod): Pair<LocalDate, LocalDate> {
        val now = LocalDate.now()
        return when (period) {
            TimePeriod.MONTH -> {
                val previousMonth = now.minusMonths(1)
                val startOfMonth = previousMonth.withDayOfMonth(1)
                val endOfMonth = previousMonth.withDayOfMonth(previousMonth.lengthOfMonth())
                startOfMonth to endOfMonth
            }
            TimePeriod.QUARTER -> {
                val previousQuarter = now.minusMonths(3)
                val quarterStart = previousQuarter.withDayOfMonth(1).withMonth(((previousQuarter.monthValue - 1) / 3) * 3 + 1)
                val quarterEnd = quarterStart.plusMonths(2).withDayOfMonth(quarterStart.plusMonths(2).lengthOfMonth())
                quarterStart to quarterEnd
            }
            TimePeriod.YEAR -> {
                val previousYear = now.minusYears(1)
                val startOfYear = previousYear.withDayOfYear(1)
                val endOfYear = previousYear.withDayOfYear(previousYear.lengthOfYear())
                startOfYear to endOfYear
            }
        }
    }
}