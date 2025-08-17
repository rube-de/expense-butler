package com.expensetracker.ui.screens

import com.expensetracker.data.model.Category
import com.expensetracker.data.model.MonthlySpending
import org.junit.Test
import org.junit.Assert.*
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Unit tests for analytics chart data processing.
 */
class AnalyticsChartTest {

    @Test
    fun `should convert category breakdown to pie chart data`() {
        val categories = listOf(
            Category(1L, "Food", "#FF5722", "restaurant", true),
            Category(2L, "Travel", "#2196F3", "flight", true)
        )
        val categoryBreakdown = mapOf(
            categories[0] to BigDecimal("500.00"),
            categories[1] to BigDecimal("750.50")
        )
        
        val chartData = convertToPieChartData(categoryBreakdown)
        
        assertEquals(2, chartData.size)
        assertTrue(chartData.any { it.label == "Food" && it.value == 500.0f })
        assertTrue(chartData.any { it.label == "Travel" && it.value == 750.5f })
    }

    @Test
    fun `should convert monthly trends to line chart data`() {
        val monthlyTrends = listOf(
            MonthlySpending(LocalDate.of(2024, 1, 1), BigDecimal("1000.00"), 15),
            MonthlySpending(LocalDate.of(2024, 2, 1), BigDecimal("1200.00"), 18),
            MonthlySpending(LocalDate.of(2024, 3, 1), BigDecimal("950.00"), 12)
        )
        
        val chartData = convertToLineChartData(monthlyTrends)
        
        assertEquals(3, chartData.size)
        assertEquals(1000.0f, chartData[0].y)
        assertEquals(1200.0f, chartData[1].y)
        assertEquals(950.0f, chartData[2].y)
    }

    @Test
    fun `should handle empty category breakdown`() {
        val chartData = convertToPieChartData(emptyMap())
        assertTrue(chartData.isEmpty())
    }

    @Test
    fun `should handle empty monthly trends`() {
        val chartData = convertToLineChartData(emptyList())
        assertTrue(chartData.isEmpty())
    }
}

// Data classes for chart data
data class PieChartData(
    val label: String,
    val value: Float,
    val color: String
)

data class LineChartData(
    val x: Float,
    val y: Float,
    val label: String
)

// Helper functions to convert data for charts
fun convertToPieChartData(categoryBreakdown: Map<Category, BigDecimal>): List<PieChartData> {
    return categoryBreakdown.map { (category, amount) ->
        PieChartData(
            label = category.name,
            value = amount.toFloat(),
            color = category.color
        )
    }
}

fun convertToLineChartData(monthlyTrends: List<MonthlySpending>): List<LineChartData> {
    return monthlyTrends.mapIndexed { index, monthlySpending ->
        LineChartData(
            x = index.toFloat(),
            y = monthlySpending.amount.toFloat(),
            label = monthlySpending.month.toString()
        )
    }
}