package com.expensetracker.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
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
import com.expensetracker.ui.components.AnalyticsFilterDialog
import com.expensetracker.ui.components.CategoryDrillDownView
import com.expensetracker.ui.util.LogCompositions
import com.expensetracker.ui.util.stableCallback
import java.io.File
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.*

/**
 * Analytics screen displaying expense insights and charts with filtering and drill-down capabilities.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    LogCompositions("AnalyticsScreen")
    
    val analyticsData by viewModel.analyticsData.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val currentFilter by viewModel.currentFilter.collectAsState()
    val categoryDrillDownData by viewModel.categoryDrillDownData.collectAsState()
    val availableCategories by viewModel.availableCategories.collectAsState()
    val availableTags by viewModel.availableTags.collectAsState()
    
    var selectedPeriod by remember { mutableStateOf(TimePeriod.MONTH) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var showExportSuccess by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    LaunchedEffect(selectedPeriod) {
        viewModel.loadAnalytics(selectedPeriod)
    }
    
    // Show drill-down view if category is selected
    categoryDrillDownData?.let { drillDown ->
        CategoryDrillDownView(
            drillDownData = drillDown,
            onBack = { viewModel.closeDrillDown() }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top Bar with Period Selector and Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Period Selector
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.Start
            ) {
                PeriodSelector(
                    selectedPeriod = selectedPeriod,
                    onPeriodSelected = { 
                        selectedPeriod = it
                        viewModel.loadAnalytics(it)
                    }
                )
            }
            
            // Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Filter Button
                IconButton(onClick = { showFilterDialog = true }) {
                    Badge(
                        containerColor = if (currentFilter.selectedCategories.isNotEmpty() || 
                                            currentFilter.selectedTags.isNotEmpty() || 
                                            currentFilter.customDateRange != null) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            Color.Transparent
                        }
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Filter",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                
                // Export Button
                IconButton(
                    onClick = {
                        val exportData = viewModel.exportAnalyticsData()
                        // In a real app, you'd save this to a file or share it
                        val file = File(context.cacheDir, "analytics_export.csv")
                        file.writeText(exportData)
                        showExportSuccess = true
                    }
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "Export"
                    )
                }
                
                // Clear Filters Button (shown only when filters are active)
                if (currentFilter.selectedCategories.isNotEmpty() || 
                    currentFilter.selectedTags.isNotEmpty() || 
                    currentFilter.customDateRange != null) {
                    IconButton(onClick = { viewModel.clearFilters() }) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Clear filters"
                        )
                    }
                }
            }
        }
        
        // Active Filters Display
        if (currentFilter.selectedCategories.isNotEmpty() || 
            currentFilter.selectedTags.isNotEmpty() || 
            currentFilter.customDateRange != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "Active Filters",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (currentFilter.selectedCategories.isNotEmpty()) {
                        Text(
                            text = "Categories: ${currentFilter.selectedCategories.size} selected",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    
                    if (currentFilter.selectedTags.isNotEmpty()) {
                        Text(
                            text = "Tags: ${currentFilter.selectedTags.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    
                    currentFilter.customDateRange?.let { range ->
                        Text(
                            text = "Date: ${range.startDate} to ${range.endDate}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
        
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
                    modifier = Modifier
                        .fillMaxSize()
                        .semantics { contentDescription = "Empty analytics state" },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No expense data available",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.semantics { contentDescription = "No data message" }
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
                        // Category Breakdown Chart with drill-down
                        if (analyticsData.categoryBreakdown.isNotEmpty()) {
                            // Create stable callback to prevent unnecessary recompositions
                            val onCategoryClick = stableCallback(viewModel) { category: Category ->
                                viewModel.drillDownCategory(category.id)
                            }
                            
                            CategoryBreakdownChart(
                                categoryBreakdown = analyticsData.categoryBreakdown,
                                onCategoryClick = onCategoryClick
                            )
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
    
    // Filter Dialog
    if (showFilterDialog) {
        AnalyticsFilterDialog(
            onDismiss = { showFilterDialog = false },
            onApplyFilter = { categories, tags, dateRange ->
                viewModel.applyFilters(categories, tags, dateRange)
            },
            availableCategories = availableCategories,
            availableTags = availableTags,
            currentSelectedCategories = currentFilter.selectedCategories,
            currentSelectedTags = currentFilter.selectedTags,
            currentDateRange = currentFilter.customDateRange
        )
    }
    
    // Export Success Snackbar
    if (showExportSuccess) {
        LaunchedEffect(showExportSuccess) {
            kotlinx.coroutines.delay(3000)
            showExportSuccess = false
        }
        
        Snackbar(
            modifier = Modifier.padding(16.dp),
            action = {
                TextButton(onClick = { showExportSuccess = false }) {
                    Text("Dismiss")
                }
            }
        ) {
            Text("Analytics exported successfully")
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
    LogCompositions("TotalSpendingCard")
    
    // Memoize expensive currency formatting
    val formattedAmount by remember(totalSpending) {
        derivedStateOf { formatCurrency(totalSpending) }
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Total spending card" }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Total Spending",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { contentDescription = "Total spending title" }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formattedAmount,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { contentDescription = "Total spending amount" }
            )
        }
    }
}

@Composable
private fun CategoryBreakdownChart(
    categoryBreakdown: Map<Category, BigDecimal>,
    onCategoryClick: (Category) -> Unit = {}
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
                
                // Legend with clickable items
                CategoryLegend(
                    categoryBreakdown = categoryBreakdown,
                    onCategoryClick = onCategoryClick
                )
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

// Optimized data structure for pie chart calculations
@Stable
private data class PieChartSegment(
    val color: Color,
    val startAngle: Float,
    val sweepAngle: Float
)

@Composable
private fun SimplePieChart(
    data: Map<Category, BigDecimal>,
    modifier: Modifier = Modifier
) {
    LogCompositions("SimplePieChart")
    
    // Memoize expensive pie chart calculations
    val chartSegments by remember(data) {
        derivedStateOf {
            val total = data.values.sumOf { it }
            if (total == BigDecimal.ZERO) {
                emptyList<PieChartSegment>()
            } else {
                var currentAngle = 0f
                data.map { (category, amount) ->
                    val sweepAngle = (amount.toFloat() / total.toFloat()) * 360f
                    val segment = PieChartSegment(
                        color = Color(android.graphics.Color.parseColor(category.color)),
                        startAngle = currentAngle,
                        sweepAngle = sweepAngle
                    )
                    currentAngle += sweepAngle
                    segment
                }
            }
        }
    }
    
    if (chartSegments.isEmpty()) return
    
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = minOf(size.width, size.height) / 2f * 0.8f
        
        chartSegments.forEach { segment ->
            drawArc(
                color = segment.color,
                startAngle = segment.startAngle,
                sweepAngle = segment.sweepAngle,
                useCenter = true,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2)
            )
        }
    }
}

// Optimized data structure for category legend
@Stable
private data class CategoryLegendItem(
    val category: Category,
    val amount: BigDecimal,
    val formattedAmount: String,
    val percentage: String,
    val color: Color
)

@Composable
private fun CategoryLegend(
    categoryBreakdown: Map<Category, BigDecimal>,
    onCategoryClick: (Category) -> Unit = {}
) {
    LogCompositions("CategoryLegend")
    
    // Memoize legend calculations and formatting
    val legendItems by remember(categoryBreakdown) {
        derivedStateOf {
            val total = categoryBreakdown.values.sumOf { it }
            categoryBreakdown.map { (category, amount) ->
                val percentage = if (total > BigDecimal.ZERO) {
                    "${(amount.toDouble() / total.toDouble() * 100).toInt()}%"
                } else {
                    "0%"
                }
                CategoryLegendItem(
                    category = category,
                    amount = amount,
                    formattedAmount = formatCurrency(amount),
                    percentage = percentage,
                    color = Color(android.graphics.Color.parseColor(category.color))
                )
            }
        }
    }
    
    // Create stable callback to prevent recompositions
    val stableOnCategoryClick = stableCallback(onCategoryClick) { category: Category ->
        onCategoryClick(category)
    }
    
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.heightIn(max = 120.dp)
    ) {
        items(
            items = legendItems,
            key = { it.category.id }
        ) { item ->
            CategoryLegendRow(
                item = item,
                onClick = { stableOnCategoryClick(item.category) }
            )
        }
    }
}

@Composable
private fun CategoryLegendRow(
    item: CategoryLegendItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
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
                    .background(item.color, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = item.category.name,
                style = MaterialTheme.typography.bodySmall
            )
        }
        
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = item.formattedAmount,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = item.percentage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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