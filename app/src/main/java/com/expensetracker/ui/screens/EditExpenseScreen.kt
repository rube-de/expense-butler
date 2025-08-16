package com.expensetracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.expensetracker.data.model.Category
import com.expensetracker.ui.components.*
import com.expensetracker.ui.theme.ExpenseTrackerTheme
import com.expensetracker.ui.theme.spacing
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditExpenseScreen(
    onNavigateBack: () -> Unit,
    onExpenseUpdated: () -> Unit,
    onExpenseDeleted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditExpenseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Handle successful update
    LaunchedEffect(uiState.isUpdated) {
        if (uiState.isUpdated) {
            onExpenseUpdated()
        }
    }

    // Handle successful deletion
    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            onExpenseDeleted()
        }
    }

    // Show error snackbar
    uiState.errorMessage?.let { errorMessage ->
        LaunchedEffect(errorMessage) {
            // In a real app, you might want to show a Snackbar here
            viewModel.clearError()
        }
    }

    // Delete confirmation dialog
    if (uiState.showDeleteConfirmation) {
        DeleteConfirmationDialog(
            onConfirm = viewModel::deleteExpense,
            onDismiss = viewModel::hideDeleteConfirmation
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Edit Expense",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Medium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Navigate back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::showDeleteConfirmation) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete expense",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.originalExpense == null && uiState.errorMessage == null) {
            // Loading state
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            EditExpenseContent(
                uiState = uiState,
                onAmountChanged = viewModel::updateAmount,
                onCurrencyChanged = viewModel::updateCurrency,
                onDescriptionChanged = viewModel::updateDescription,
                onCategorySelected = viewModel::selectCategory,
                onTagsChanged = viewModel::updateTags,
                onUpdateExpense = viewModel::updateExpense,
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        }
    }
}

@Composable
private fun EditExpenseContent(
    uiState: EditExpenseUiState,
    onAmountChanged: (BigDecimal?) -> Unit,
    onCurrencyChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onCategorySelected: (Category) -> Unit,
    onTagsChanged: (List<String>) -> Unit,
    onUpdateExpense: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(MaterialTheme.spacing.medium),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.large)
    ) {
        // Amount and Currency Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(MaterialTheme.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
            ) {
                Text(
                    text = "Amount",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                AmountInputWithCurrency(
                    amount = uiState.amount,
                    currency = uiState.currency,
                    onAmountChanged = onAmountChanged,
                    onCurrencyChanged = onCurrencyChanged,
                    isAmountError = uiState.amountError != null,
                    amountErrorMessage = uiState.amountError
                )
            }
        }

        // Description Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(MaterialTheme.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
            ) {
                Text(
                    text = "Description",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                OutlinedTextField(
                    value = uiState.description,
                    onValueChange = onDescriptionChanged,
                    label = { Text("Description") },
                    placeholder = { Text("What did you spend on?") },
                    isError = uiState.descriptionError != null,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                if (uiState.descriptionError != null) {
                    Text(
                        text = uiState.descriptionError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = MaterialTheme.spacing.medium)
                    )
                }
            }
        }

        // Category Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(MaterialTheme.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
            ) {
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                if (uiState.availableCategories.isNotEmpty()) {
                    CategoryGrid(
                        categories = uiState.availableCategories,
                        selectedCategory = uiState.selectedCategory,
                        onCategorySelected = onCategorySelected
                    )
                } else {
                    Text(
                        text = "Loading categories...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (uiState.categoryError != null) {
                    Text(
                        text = uiState.categoryError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = MaterialTheme.spacing.medium)
                    )
                }
            }
        }

        // Tags Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(MaterialTheme.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
            ) {
                Text(
                    text = "Tags",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                TagInput(
                    selectedTags = uiState.selectedTags,
                    availableTags = uiState.availableTags,
                    onTagsChanged = onTagsChanged,
                    placeholder = "Add tags to organize your expenses..."
                )
            }
        }

        // Update Button
        Button(
            onClick = onUpdateExpense,
            enabled = !uiState.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
                Text("Updating...")
            } else {
                Text(
                    "Update Expense",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        // Error message display
        if (uiState.errorMessage != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = uiState.errorMessage,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(MaterialTheme.spacing.medium)
                )
            }
        }
    }
}

@Composable
private fun CategoryGrid(
    categories: List<Category>,
    selectedCategory: Category?,
    onCategorySelected: (Category) -> Unit,
    modifier: Modifier = Modifier
) {
    // Simple grid layout using rows
    val chunkedCategories = categories.chunked(2)
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
    ) {
        chunkedCategories.forEach { rowCategories ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
            ) {
                rowCategories.forEach { category ->
                    CategoryChip(
                        category = category,
                        isSelected = selectedCategory?.id == category.id,
                        onCategoryClick = onCategorySelected,
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // Fill remaining space if odd number of categories in row
                if (rowCategories.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Delete Expense")
        },
        text = {
            Text("Are you sure you want to delete this expense? This action cannot be undone.")
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun EditExpenseScreenPreview() {
    ExpenseTrackerTheme {
        val sampleCategories = listOf(
            Category(1, "Food", "#4CAF50", "restaurant", true),
            Category(2, "Travel", "#2196F3", "flight", true),
            Category(3, "Entertainment", "#FF9800", "movie", true),
            Category(4, "Shopping", "#9C27B0", "shopping_cart", true)
        )
        
        EditExpenseContent(
            uiState = EditExpenseUiState(
                amount = BigDecimal("25.50"),
                description = "Coffee",
                availableCategories = sampleCategories,
                selectedCategory = sampleCategories[0],
                selectedTags = listOf("coffee", "work"),
                availableTags = listOf("coffee", "work", "breakfast", "lunch")
            ),
            onAmountChanged = { },
            onCurrencyChanged = { },
            onDescriptionChanged = { },
            onCategorySelected = { },
            onTagsChanged = { },
            onUpdateExpense = { }
        )
    }
}