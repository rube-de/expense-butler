package com.expensetracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.expensetracker.data.model.Category
import com.expensetracker.data.model.Expense
import com.expensetracker.ui.components.ExpenseCard
import com.expensetracker.ui.components.SearchBar
import com.expensetracker.ui.theme.ExpenseTrackerTheme
import com.expensetracker.ui.theme.spacing
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseListScreen(
    onNavigateToAddExpense: () -> Unit,
    onNavigateToEditExpense: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExpenseListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Handle error messages
    uiState.errorMessage?.let { errorMessage ->
        LaunchedEffect(errorMessage) {
            // In a real app, you might want to show a Snackbar here
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Expenses",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Medium
                    )
                },
                actions = {
                    IconButton(onClick = viewModel::toggleFilters) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = "Toggle filters"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddExpense,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add expense"
                )
            }
        }
    ) { paddingValues ->
        ExpenseListContent(
            uiState = uiState,
            onExpenseClick = { expense -> onNavigateToEditExpense(expense.id) },
            onDeleteExpense = viewModel::deleteExpense,
            onSearchTextChanged = viewModel::updateSearchText,
            onCategoryFilterSelected = viewModel::selectCategoryFilter,
            onTagFilterChanged = viewModel::updateTagFilter,
            onDateRangeChanged = viewModel::updateDateRange,
            onClearFilters = viewModel::clearFilters,
            onToggleFilters = viewModel::toggleFilters,
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        )
    }
}

@Composable
fun ExpenseListContent(
    uiState: ExpenseListUiState,
    onExpenseClick: (Expense) -> Unit,
    onDeleteExpense: (Long) -> Unit,
    onSearchTextChanged: (String) -> Unit,
    onCategoryFilterSelected: (Category?) -> Unit,
    onTagFilterChanged: (List<String>) -> Unit,
    onDateRangeChanged: (LocalDate?, LocalDate?) -> Unit,
    onClearFilters: () -> Unit,
    onToggleFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(MaterialTheme.spacing.medium)
    ) {
        // Search Bar
        SearchBar(
            searchText = uiState.searchText,
            onSearchTextChanged = onSearchTextChanged,
            placeholder = "Search expenses...",
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))

        // Filters Section
        if (uiState.showFilters) {
            FilterSection(
                uiState = uiState,
                onCategoryFilterSelected = onCategoryFilterSelected,
                onTagFilterChanged = onTagFilterChanged,
                onDateRangeChanged = onDateRangeChanged,
                onClearFilters = onClearFilters
            )
            
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
        }

        // Content
        when {
            uiState.isLoading -> {
                LoadingState(modifier = Modifier.fillMaxSize())
            }
            uiState.errorMessage != null -> {
                ErrorState(
                    message = uiState.errorMessage,
                    modifier = Modifier.fillMaxSize()
                )
            }
            uiState.expenses.isEmpty() -> {
                EmptyState(modifier = Modifier.fillMaxSize())
            }
            else -> {
                ExpenseList(
                    expenses = uiState.expenses,
                    categories = uiState.categories,
                    onExpenseClick = onExpenseClick,
                    onDeleteExpense = onDeleteExpense,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun FilterSection(
    uiState: ExpenseListUiState,
    onCategoryFilterSelected: (Category?) -> Unit,
    onTagFilterChanged: (List<String>) -> Unit,
    onDateRangeChanged: (LocalDate?, LocalDate?) -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filters",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                TextButton(onClick = onClearFilters) {
                    Text("Clear All")
                }
            }

            // Category Filter
            Text(
                text = "Category",
                style = MaterialTheme.typography.labelLarge
            )
            
            // Simple category selection for now
            if (uiState.categories.isNotEmpty()) {
                Text(
                    text = uiState.selectedCategoryFilter?.name ?: "All Categories",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Date Range Filter
            Text(
                text = "Date Range",
                style = MaterialTheme.typography.labelLarge
            )
            
            Text(
                text = if (uiState.startDate != null && uiState.endDate != null) {
                    "${uiState.startDate} - ${uiState.endDate}"
                } else {
                    "All Dates"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Tags Filter
            Text(
                text = "Tags",
                style = MaterialTheme.typography.labelLarge
            )
            
            Text(
                text = if (uiState.selectedTags.isNotEmpty()) {
                    uiState.selectedTags.joinToString(", ")
                } else {
                    "All Tags"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ExpenseList(
    expenses: List<Expense>,
    categories: List<Category>,
    onExpenseClick: (Expense) -> Unit,
    onDeleteExpense: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
    ) {
        items(
            items = expenses,
            key = { expense -> expense.id }
        ) { expense ->
            val category = categories.find { it.id == expense.categoryId }
            
            ExpenseCard(
                expense = expense,
                category = category,
                onExpenseClick = onExpenseClick
            )
        }
    }
}

@Composable
private fun LoadingState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.testTag("loading_indicator")
        )
    }
}

@Composable
private fun EmptyState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
        ) {
            Text(
                text = "No expenses found",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "Start tracking your expenses by adding your first expense",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(MaterialTheme.spacing.medium),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ExpenseListScreenPreview() {
    ExpenseTrackerTheme {
        val sampleExpenses = listOf(
            Expense(
                id = 1,
                amount = BigDecimal("25.50"),
                currency = "USD",
                description = "Coffee",
                categoryId = 1,
                tags = listOf("coffee", "work"),
                date = LocalDateTime.of(2024, 1, 15, 9, 30)
            ),
            Expense(
                id = 2,
                amount = BigDecimal("120.00"),
                currency = "USD",
                description = "Groceries",
                categoryId = 2,
                tags = listOf("food", "weekly"),
                date = LocalDateTime.of(2024, 1, 14, 18, 0)
            )
        )
        
        val sampleCategories = listOf(
            Category(1, "Food", "#4CAF50", "restaurant", true),
            Category(2, "Shopping", "#9C27B0", "shopping_cart", true)
        )
        
        ExpenseListContent(
            uiState = ExpenseListUiState(
                expenses = sampleExpenses,
                categories = sampleCategories,
                showFilters = true
            ),
            onExpenseClick = { },
            onDeleteExpense = { },
            onSearchTextChanged = { },
            onCategoryFilterSelected = { },
            onTagFilterChanged = { },
            onDateRangeChanged = { _, _ -> },
            onClearFilters = { },
            onToggleFilters = { }
        )
    }
}