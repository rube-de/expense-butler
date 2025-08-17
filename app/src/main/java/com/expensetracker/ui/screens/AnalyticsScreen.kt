package com.expensetracker.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import com.expensetracker.data.model.TimePeriod
import com.expensetracker.data.model.Category
import com.expensetracker.data.model.MonthlySpending

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.*

/**
 * Analytics screen displaying expense insights and charts.
 */
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val analyticsData by viewModel.analyticsData.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    var selectedPeriod by remember { mutableStateOf(TimePeriod.MONTH) }
    
    LaunchedEffect(selectedPeriod) {
        viewModel.loadAnalytics(selectedPeriod)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Period Selector
        PeriodSelector(
            selectedPeriod = selectedPeriod,
            onPeriodSelected = { selectedPeriod = it }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .semantics { contentDescription = "Loading analytics" },
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            error != null -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error ?: "",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            
            analyticsData.totalSpending == BigDecimal.ZERO && 
            analyticsData.categoryBreakdown.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No expense data available",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        // Total Spending Card
                        TotalSpendingCard(totalSpending = analyticsData.totalSpending)
                    }
                    
                    item {
                        // Category Breakdown Chart
                        if (analyticsData.categoryBreakdown.isNotEmpty()) {
                            CategoryBreakdownChart(categoryBreakdown = analyticsData.categoryBreakdown)
                        }
                    }
                    
                    item {
                        // Monthly Trends Chart
                        if (analyticsData.monthlyTrends.isNotEmpty()) {
                            MonthlyTrendsChart(monthlyTrends = analyticsData.monthlyTrends)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PeriodSelector(
    selectedPeriod: TimePeriod,
    onPeriodSelected: (TimePeriod) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TimePeriod.values().forEach { period ->
            Button(
                onClick = { onPeriodSelected(period) },
                modifier = Modifier.weight(1f),
                colors = if (selectedPeriod == period) {
                    ButtonDefaults.buttonColors()
                } else {
                    ButtonDefaults.outlinedButtonColors()
                }
            ) {
                Text(
                    text = when (period) {
                        TimePeriod.MONTH -> "Month"
                        TimePeriod.QUARTER -> "Quarter"
                        TimePeriod.YEAR -> "Year"
                    }
                )
            }
        }
    }
}

@Composable
private fun TotalSpendingCard(totalSpending: BigDecimal) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Total Spending",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formatCurrency(totalSpending),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun CategoryBreakdownChart(
    categoryBreakdown: Map<Category, BigDecimal>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Category breakdown chart" }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Category Breakdown",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            if (categoryBreakdown.isNotEmpty()) {
                // Simple pie chart using Canvas
                SimplePieChart(
                    data = categoryBreakdown,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Legend
                CategoryLegend(categoryBreakdown = categoryBreakdown)
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No category data available",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthlyTrendsChart(
    monthlyTrends: List<MonthlySpending>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Monthly spending trends chart" }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Monthly Trends",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            if (monthlyTrends.isNotEmpty()) {
                VicoLineChart(
                    monthlyTrends = monthlyTrends,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No trend data available",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun SimplePieChart(
    data: Map<Category, BigDecimal>,
    modifier: Modifier = Modifier
) {
    val total = data.values.sumOf { it }
    if (total == BigDecimal.ZERO) return
    
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = minOf(size.width, size.height) / 2f * 0.8f
        
        var startAngle = 0f
        
        data.forEach { (category, amount) ->
            val sweepAngle = (amount.toFloat() / total.toFloat()) * 360f
            
            drawArc(
                color = Color(android.graphics.Color.parseColor(category.color)),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2)
            )
            
            startAngle += sweepAngle
        }
    }
}

@Composable
private fun CategoryLegend(
    categoryBreakdown: Map<Category, BigDecimal>
) {
    val total = categoryBreakdown.values.sumOf { it }
    
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.heightIn(max = 120.dp)
    ) {
        items(categoryBreakdown.toList()) { (category, amount) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                Color(android.graphics.Color.parseColor(category.color)),
                                CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = formatCurrency(amount),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    if (total > BigDecimal.ZERO) {
                        val percentage = (amount.toDouble() / total.toDouble() * 100)
                        Text(
                            text = "${percentage.toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VicoLineChart(
    monthlyTrends: List<MonthlySpending>,
    modifier: Modifier = Modifier
) {
    SimpleLineChart(
        data = monthlyTrends.map { it.amount.toFloat() },
        modifier = modifier
    )
}

@Composable
private fun SimpleLineChart(
    data: List<Float>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return
    
    val maxValue = data.maxOrNull() ?: 0f
    val minValue = data.minOrNull() ?: 0f
    val range = maxValue - minValue
    
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val padding = 40f
        
        val chartWidth = width - (padding * 2)
        val chartHeight = height - (padding * 2)
        
        if (data.size > 1) {
            val stepX = chartWidth / (data.size - 1)
            
            for (i in 0 until data.size - 1) {
                val x1 = padding + (i * stepX)
                val y1 = padding + chartHeight - ((data[i] - minValue) / range * chartHeight)
                val x2 = padding + ((i + 1) * stepX)
                val y2 = padding + chartHeight - ((data[i + 1] - minValue) / range * chartHeight)
                
                drawLine(
                    color = Color(0xFF2196F3),
                    start = Offset(x1, y1),
                    end = Offset(x2, y2),
                    strokeWidth = 3f
                )
            }
            
            // Draw points
            data.forEachIndexed { index, value ->
                val x = padding + (index * stepX)
                val y = padding + chartHeight - ((value - minValue) / range * chartHeight)
                
                drawCircle(
                    color = Color(0xFF2196F3),
                    radius = 6f,
                    center = Offset(x, y)
                )
            }
        }
    }
}

private fun formatCurrency(amount: BigDecimal): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale.US)
    return formatter.format(amount)
}