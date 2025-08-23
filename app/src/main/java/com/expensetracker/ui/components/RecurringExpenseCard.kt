package com.expensetracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.expensetracker.R
import com.expensetracker.data.model.Category
import com.expensetracker.data.model.RecurrenceFrequency
import com.expensetracker.data.model.RecurringExpense
import com.expensetracker.ui.theme.ExpenseTrackerTheme
import com.expensetracker.ui.theme.spacing
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringExpenseCard(
    recurringExpense: RecurringExpense,
    category: Category?,
    onEdit: (RecurringExpense) -> Unit,
    onDelete: (RecurringExpense) -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.medium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = recurringExpense.description,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                    ) {
                        if (category != null) {
                            CategoryChip(
                                category = category,
                                isSelected = false,
                                onCategoryClick = { }
                            )
                        }
                        
                        FrequencyBadge(frequency = recurringExpense.frequency)
                        
                        if (!recurringExpense.isActive) {
                            StatusBadge(
                                text = stringResource(R.string.status_inactive),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.cd_more_options)
                            )
                        }
                        
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_edit)) },
                                onClick = {
                                    showMenu = false
                                    onEdit(recurringExpense)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_delete)) },
                                onClick = {
                                    showMenu = false
                                    onDelete(recurringExpense)
                                }
                            )
                        }
                    }
                    
                    Text(
                        text = formatAmount(recurringExpense.amount, recurringExpense.currency),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (recurringExpense.tags.isNotEmpty()) {
                    TagRow(
                        tags = recurringExpense.tags,
                        maxVisibleTags = 3,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = stringResource(R.string.label_start_date, formatDate(recurringExpense.startDate)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    recurringExpense.endDate?.let { endDate ->
                        Text(
                            text = stringResource(R.string.label_end_date, formatDate(endDate)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    recurringExpense.lastGenerated?.let { lastGenerated ->
                        Text(
                            text = stringResource(R.string.label_last_generated, formatDate(lastGenerated)),
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
private fun FrequencyBadge(
    frequency: RecurrenceFrequency,
    modifier: Modifier = Modifier
) {
    val frequencyText = when (frequency) {
        RecurrenceFrequency.DAILY -> stringResource(R.string.frequency_daily)
        RecurrenceFrequency.WEEKLY -> stringResource(R.string.frequency_weekly)
        RecurrenceFrequency.MONTHLY -> stringResource(R.string.frequency_monthly)
        RecurrenceFrequency.YEARLY -> stringResource(R.string.frequency_yearly)
    }
    
    StatusBadge(
        text = frequencyText,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = modifier
    )
}

@Composable
private fun StatusBadge(
    text: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(
                horizontal = MaterialTheme.spacing.small,
                vertical = MaterialTheme.spacing.extraSmall
            ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun TagRow(
    tags: List<String>,
    maxVisibleTags: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall)
    ) {
        val visibleTags = tags.take(maxVisibleTags)
        val remainingCount = tags.size - maxVisibleTags
        
        visibleTags.forEach { tag ->
            TagChip(tag = tag)
        }
        
        if (remainingCount > 0) {
            TagChip(tag = "+$remainingCount")
        }
    }
}

@Composable
private fun TagChip(
    tag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            text = tag,
            modifier = Modifier.padding(
                horizontal = MaterialTheme.spacing.small,
                vertical = MaterialTheme.spacing.extraSmall
            ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

private fun formatAmount(amount: BigDecimal, currency: String): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale.getDefault())
    formatter.currency = Currency.getInstance(currency)
    return formatter.format(amount)
}

private fun formatDate(date: LocalDate): String {
    return "${date.monthValue}/${date.dayOfMonth}/${date.year}"
}

@Preview(showBackground = true, name = "Active Recurring Expense")
@Composable
private fun RecurringExpenseCardPreview() {
    ExpenseTrackerTheme {
        val sampleRecurringExpense = RecurringExpense(
            id = 1,
            amount = BigDecimal("1200.00"),
            currency = "USD",
            description = "Monthly rent payment",
            categoryId = 1,
            tags = listOf("rent", "housing", "monthly"),
            frequency = RecurrenceFrequency.MONTHLY,
            startDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 12, 31),
            lastGenerated = LocalDate.of(2024, 1, 1),
            isActive = true
        )
        
        val sampleCategory = Category(
            id = 1,
            name = "Housing",
            color = "#2196F3",
            icon = "home"
        )
        
        RecurringExpenseCard(
            recurringExpense = sampleRecurringExpense,
            category = sampleCategory,
            onEdit = { },
            onDelete = { },
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, name = "Inactive Recurring Expense")
@Composable
private fun RecurringExpenseCardInactivePreview() {
    ExpenseTrackerTheme {
        val sampleRecurringExpense = RecurringExpense(
            id = 2,
            amount = BigDecimal("9.99"),
            currency = "USD",
            description = "Netflix subscription (cancelled)",
            categoryId = 2,
            tags = listOf("entertainment", "streaming"),
            frequency = RecurrenceFrequency.MONTHLY,
            startDate = LocalDate.of(2023, 6, 1),
            endDate = LocalDate.of(2024, 1, 15),
            lastGenerated = LocalDate.of(2024, 1, 1),
            isActive = false
        )
        
        val sampleCategory = Category(
            id = 2,
            name = "Entertainment",
            color = "#FF5722",
            icon = "movie"
        )
        
        RecurringExpenseCard(
            recurringExpense = sampleRecurringExpense,
            category = sampleCategory,
            onEdit = { },
            onDelete = { },
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, name = "Weekly Recurring Expense")
@Composable
private fun RecurringExpenseCardWeeklyPreview() {
    ExpenseTrackerTheme {
        val sampleRecurringExpense = RecurringExpense(
            id = 3,
            amount = BigDecimal("25.00"),
            currency = "USD",
            description = "Weekly grocery shopping",
            categoryId = 3,
            tags = listOf("groceries", "food"),
            frequency = RecurrenceFrequency.WEEKLY,
            startDate = LocalDate.of(2024, 1, 7),
            endDate = null,
            lastGenerated = LocalDate.of(2024, 1, 21),
            isActive = true
        )
        
        val sampleCategory = Category(
            id = 3,
            name = "Food",
            color = "#4CAF50",
            icon = "restaurant"
        )
        
        RecurringExpenseCard(
            recurringExpense = sampleRecurringExpense,
            category = sampleCategory,
            onEdit = { },
            onDelete = { },
            modifier = Modifier.padding(16.dp)
        )
    }
}