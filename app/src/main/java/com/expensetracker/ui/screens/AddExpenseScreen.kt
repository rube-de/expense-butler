package com.expensetracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import android.content.res.Configuration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.expensetracker.R
import com.expensetracker.data.model.Category
import com.expensetracker.domain.error.UserFacingError
import com.expensetracker.ui.components.*
import com.expensetracker.ui.theme.ExpenseTrackerTheme
import com.expensetracker.ui.theme.spacing
import com.expensetracker.ui.util.toDisplayString
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    onNavigateBack: () -> Unit,
    onExpenseSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddExpenseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Handle successful save
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onExpenseSaved()
        }
    }

    // Show error snackbar
    uiState.errorMessage?.let { userFacingError ->
        LaunchedEffect(userFacingError) {
            // In a real app, you might want to show a Snackbar here
            // For now, we'll just clear the error after showing it
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        stringResource(R.string.title_add_expense),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Medium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        AddExpenseContent(
            uiState = uiState,
            onAmountChanged = viewModel::updateAmount,
            onCurrencyChanged = viewModel::updateCurrency,
            onDescriptionChanged = viewModel::updateDescription,
            onCategorySelected = viewModel::selectCategory,
            onTagsChanged = viewModel::updateTags,
            onSaveExpense = viewModel::saveExpense,
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        )
    }
}

@Composable
private fun AddExpenseContent(
    uiState: AddExpenseUiState,
    onAmountChanged: (BigDecimal?) -> Unit,
    onCurrencyChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onCategorySelected: (Category) -> Unit,
    onTagsChanged: (List<String>) -> Unit,
    onSaveExpense: () -> Unit,
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
                    text = stringResource(R.string.label_amount),
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
                    text = stringResource(R.string.label_description),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                OutlinedTextField(
                    value = uiState.description,
                    onValueChange = onDescriptionChanged,
                    label = { Text(stringResource(R.string.label_description)) },
                    placeholder = { Text(stringResource(R.string.placeholder_description)) },
                    isError = uiState.descriptionError != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("description_input"),
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
                    text = stringResource(R.string.label_category),
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
                        text = stringResource(R.string.loading_categories),
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
                    text = stringResource(R.string.label_tags),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                TagInput(
                    selectedTags = uiState.selectedTags,
                    availableTags = uiState.availableTags,
                    onTagsChanged = onTagsChanged,
                    placeholder = stringResource(R.string.placeholder_tags_organize)
                )
            }
        }

        // Save Button
        Button(
            onClick = onSaveExpense,
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
                Text(stringResource(R.string.status_saving))
            } else {
                Text(
                    stringResource(R.string.button_save),
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
                    text = uiState.errorMessage.toDisplayString(),
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
                        modifier = Modifier
                            .weight(1f)
                            .testTag("category_chip")
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

// Sample data for previews
private val sampleCategories = listOf(
    Category(1, "Food", "#4CAF50", "restaurant", true),
    Category(2, "Travel", "#2196F3", "flight", true),
    Category(3, "Entertainment", "#FF9800", "movie", true),
    Category(4, "Shopping", "#9C27B0", "shopping_cart", true)
)

private val sampleTags = listOf("coffee", "work", "breakfast", "lunch", "meeting", "personal")

@Preview(showBackground = true, name = "Filled Form")
@Composable
private fun AddExpenseScreenPreview_FilledForm() {
    ExpenseTrackerTheme {
        AddExpenseContent(
            uiState = AddExpenseUiState(
                amount = BigDecimal("25.50"),
                description = "Coffee",
                availableCategories = sampleCategories,
                selectedCategory = sampleCategories[0],
                selectedTags = listOf("coffee", "work"),
                availableTags = sampleTags
            ),
            onAmountChanged = { },
            onCurrencyChanged = { },
            onDescriptionChanged = { },
            onCategorySelected = { },
            onTagsChanged = { },
            onSaveExpense = { }
        )
    }
}

@Preview(showBackground = true, name = "Empty Form")
@Composable
private fun AddExpenseScreenPreview_Empty() {
    ExpenseTrackerTheme {
        AddExpenseContent(
            uiState = AddExpenseUiState(
                amount = null,
                description = "",
                availableCategories = sampleCategories,
                selectedCategory = null,
                selectedTags = emptyList(),
                availableTags = sampleTags
            ),
            onAmountChanged = { },
            onCurrencyChanged = { },
            onDescriptionChanged = { },
            onCategorySelected = { },
            onTagsChanged = { },
            onSaveExpense = { }
        )
    }
}

@Preview(showBackground = true, name = "Validation Errors")
@Composable
private fun AddExpenseScreenPreview_ValidationErrors() {
    ExpenseTrackerTheme {
        AddExpenseContent(
            uiState = AddExpenseUiState(
                amount = BigDecimal.ZERO,
                description = "",
                availableCategories = sampleCategories,
                selectedCategory = null,
                selectedTags = emptyList(),
                availableTags = sampleTags,
                amountError = "Amount must be greater than 0",
                descriptionError = "Description is required",
                categoryError = "Please select a category"
            ),
            onAmountChanged = { },
            onCurrencyChanged = { },
            onDescriptionChanged = { },
            onCategorySelected = { },
            onTagsChanged = { },
            onSaveExpense = { }
        )
    }
}

@Preview(showBackground = true, name = "Loading")
@Composable
private fun AddExpenseScreenPreview_Loading() {
    ExpenseTrackerTheme {
        AddExpenseContent(
            uiState = AddExpenseUiState(
                amount = BigDecimal("150.00"),
                description = "Team lunch",
                availableCategories = sampleCategories,
                selectedCategory = sampleCategories[0],
                selectedTags = listOf("work", "lunch"),
                availableTags = sampleTags,
                isLoading = true
            ),
            onAmountChanged = { },
            onCurrencyChanged = { },
            onDescriptionChanged = { },
            onCategorySelected = { },
            onTagsChanged = { },
            onSaveExpense = { }
        )
    }
}

@Preview(
    showBackground = true, 
    name = "Dark Theme",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun AddExpenseScreenPreview_Dark() {
    ExpenseTrackerTheme {
        AddExpenseContent(
            uiState = AddExpenseUiState(
                amount = BigDecimal("75.25"),
                description = "Movie tickets",
                availableCategories = sampleCategories,
                selectedCategory = sampleCategories[2], // Entertainment
                selectedTags = listOf("entertainment", "personal"),
                availableTags = sampleTags
            ),
            onAmountChanged = { },
            onCurrencyChanged = { },
            onDescriptionChanged = { },
            onCategorySelected = { },
            onTagsChanged = { },
            onSaveExpense = { }
        )
    }
}

@Preview(showBackground = true, name = "Error State")
@Composable
private fun AddExpenseScreenPreview_ErrorState() {
    ExpenseTrackerTheme {
        AddExpenseContent(
            uiState = AddExpenseUiState(
                amount = BigDecimal("25.50"),
                description = "Coffee",
                availableCategories = sampleCategories,
                selectedCategory = sampleCategories[0],
                selectedTags = listOf("coffee", "work"),
                availableTags = sampleTags,
                errorMessage = UserFacingError.SaveFailed
            ),
            onAmountChanged = { },
            onCurrencyChanged = { },
            onDescriptionChanged = { },
            onCategorySelected = { },
            onTagsChanged = { },
            onSaveExpense = { }
        )
    }
}