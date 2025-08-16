package com.expensetracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.expensetracker.data.model.Category
import com.expensetracker.data.model.Expense
import com.expensetracker.ui.theme.ExpenseTrackerTheme
import com.expensetracker.ui.theme.spacing
import java.time.LocalDateTime
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseCard(
    expense: Expense,
    category: Category?,
    onExpenseClick: (Expense) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = { onExpenseClick(expense) },
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
                        text = expense.description,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (category != null) {
                        Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
                        CategoryChip(
                            category = category,
                            isSelected = false,
                            onCategoryClick = { },
                            modifier = Modifier
                        )
                    }
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = formatAmount(expense.amount, expense.currency),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Text(
                        text = formatDate(expense.date),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (expense.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
                TagRow(
                    tags = expense.tags,
                    maxVisibleTags = 3
                )
            }
        }
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

private fun formatDate(dateTime: LocalDateTime): String {
    return "${dateTime.monthValue}/${dateTime.dayOfMonth}/${dateTime.year}"
}

@Preview(showBackground = true)
@Composable
private fun ExpenseCardPreview() {
    ExpenseTrackerTheme {
        val sampleExpense = Expense(
            id = 1,
            amount = BigDecimal("25.50"),
            currency = "USD",
            description = "Coffee and pastry at local cafe",
            categoryId = 1,
            tags = listOf("coffee", "breakfast", "work"),
            date = LocalDateTime.of(2024, 1, 15, 9, 30)
        )
        
        val sampleCategory = Category(
            id = 1,
            name = "Food",
            color = "#4CAF50",
            icon = "restaurant"
        )
        
        ExpenseCard(
            expense = sampleExpense,
            category = sampleCategory,
            onExpenseClick = { },
            modifier = Modifier.padding(16.dp)
        )
    }
}