package com.expensetracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.expensetracker.data.model.Category
import com.expensetracker.data.model.DateRange
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Dialog for filtering analytics data by categories, tags, and date range.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsFilterDialog(
    onDismiss: () -> Unit,
    onApplyFilter: (selectedCategories: List<Long>, selectedTags: List<String>, dateRange: DateRange?) -> Unit,
    availableCategories: List<Category>,
    availableTags: List<String>,
    currentSelectedCategories: List<Long> = emptyList(),
    currentSelectedTags: List<String> = emptyList(),
    currentDateRange: DateRange? = null
) {
    var selectedCategories by remember { mutableStateOf(currentSelectedCategories) }
    var selectedTags by remember { mutableStateOf(currentSelectedTags) }
    var customDateRange by remember { mutableStateOf(currentDateRange) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Filter Analytics",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Categories Section
                item {
                    Column {
                        Text(
                            text = "Categories",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        availableCategories.forEach { category ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedCategories = if (category.id in selectedCategories) {
                                            selectedCategories - category.id
                                        } else {
                                            selectedCategories + category.id
                                        }
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = category.id in selectedCategories,
                                    onCheckedChange = { checked ->
                                        selectedCategories = if (checked) {
                                            selectedCategories + category.id
                                        } else {
                                            selectedCategories - category.id
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = category.name,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
                
                // Tags Section
                item {
                    Column {
                        Text(
                            text = "Tags",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            availableTags.forEach { tag ->
                                FilterChip(
                                    selected = tag in selectedTags,
                                    onClick = {
                                        selectedTags = if (tag in selectedTags) {
                                            selectedTags - tag
                                        } else {
                                            selectedTags + tag
                                        }
                                    },
                                    label = { Text(tag) },
                                    leadingIcon = if (tag in selectedTags) {
                                        { Icon(Icons.Default.Check, contentDescription = null) }
                                    } else null
                                )
                            }
                        }
                    }
                }
                
                // Date Range Section
                item {
                    Column {
                        Text(
                            text = "Custom Date Range",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDatePicker = true }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.DateRange,
                                    contentDescription = "Select date range"
                                )
                                Text(
                                    text = customDateRange?.let {
                                        "${it.startDate.format(DateTimeFormatter.ISO_LOCAL_DATE)} - ${it.endDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}"
                                    } ?: "No custom date range",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (customDateRange != null) {
                                    IconButton(
                                        onClick = { customDateRange = null }
                                    ) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear date range")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onApplyFilter(selectedCategories, selectedTags, customDateRange)
                    onDismiss()
                }
            ) {
                Text("Apply Filter")
            }
        },
        dismissButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = {
                        selectedCategories = emptyList()
                        selectedTags = emptyList()
                        customDateRange = null
                    }
                ) {
                    Text("Clear All")
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
    
    // Date Range Picker Dialog
    if (showDatePicker) {
        DateRangePickerDialog(
            onDismiss = { showDatePicker = false },
            onDateRangeSelected = { start, end ->
                customDateRange = DateRange(start, end)
                showDatePicker = false
            }
        )
    }
}

/**
 * Simple date range picker dialog.
 */
@Composable
fun DateRangePickerDialog(
    onDismiss: () -> Unit,
    onDateRangeSelected: (LocalDate, LocalDate) -> Unit
) {
    var startDate by remember { mutableStateOf(LocalDate.now().minusMonths(1)) }
    var endDate by remember { mutableStateOf(LocalDate.now()) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Date Range") },
        text = {
            Column {
                // Simplified date picker - in a real app you'd use a proper date picker
                Text("Start Date: ${startDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}")
                Spacer(modifier = Modifier.height(8.dp))
                Text("End Date: ${endDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}")
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Quick selection buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = {
                            startDate = LocalDate.now().minusDays(7)
                            endDate = LocalDate.now()
                        }
                    ) {
                        Text("Last 7 days")
                    }
                    TextButton(
                        onClick = {
                            startDate = LocalDate.now().minusDays(30)
                            endDate = LocalDate.now()
                        }
                    ) {
                        Text("Last 30 days")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onDateRangeSelected(startDate, endDate) }
            ) {
                Text("Select")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * A simple flow layout for arranging chips.
 */
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    // Simplified implementation - in production, use Accompanist FlowRow or similar
    Row(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement
    ) {
        content()
    }
}