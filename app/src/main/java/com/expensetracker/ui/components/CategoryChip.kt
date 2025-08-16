package com.expensetracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.expensetracker.data.model.Category
import com.expensetracker.ui.theme.ExpenseTrackerTheme
import com.expensetracker.ui.theme.spacing

@Composable
fun CategoryChip(
    category: Category,
    isSelected: Boolean,
    onCategoryClick: (Category) -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) {
        Color(android.graphics.Color.parseColor(category.color))
    } else {
        MaterialTheme.colorScheme.surface
    }
    
    val contentColor = if (isSelected) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    
    val borderColor = if (isSelected) {
        Color.Transparent
    } else {
        Color(android.graphics.Color.parseColor(category.color))
    }
    
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onCategoryClick(category) }
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = MaterialTheme.spacing.medium,
                vertical = MaterialTheme.spacing.small
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.extraSmall)
        ) {
            // Category color indicator
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = if (isSelected) Color.White else Color(android.graphics.Color.parseColor(category.color)),
                        shape = RoundedCornerShape(4.dp)
                    )
            )
            
            Text(
                text = category.name,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
            )
        }
    }
}

@Composable
fun CategoryChipRow(
    categories: List<Category>,
    selectedCategory: Category?,
    onCategoryClick: (Category) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
    ) {
        categories.forEach { category ->
            CategoryChip(
                category = category,
                isSelected = selectedCategory?.id == category.id,
                onCategoryClick = onCategoryClick
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CategoryChipPreview() {
    ExpenseTrackerTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val sampleCategories = listOf(
                Category(1, "Food", "#4CAF50", "restaurant"),
                Category(2, "Travel", "#2196F3", "flight"),
                Category(3, "Entertainment", "#FF9800", "movie"),
                Category(4, "Shopping", "#9C27B0", "shopping_cart")
            )
            
            Text("Unselected:")
            CategoryChip(
                category = sampleCategories[0],
                isSelected = false,
                onCategoryClick = { }
            )
            
            Text("Selected:")
            CategoryChip(
                category = sampleCategories[0],
                isSelected = true,
                onCategoryClick = { }
            )
            
            Text("Category Row:")
            CategoryChipRow(
                categories = sampleCategories,
                selectedCategory = sampleCategories[1],
                onCategoryClick = { }
            )
        }
    }
}