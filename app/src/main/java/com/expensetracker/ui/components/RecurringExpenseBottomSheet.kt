package com.expensetracker.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.expensetracker.R
import com.expensetracker.data.model.Category
import com.expensetracker.data.model.RecurrenceFrequency
import com.expensetracker.ui.screens.RecurringExpenseUiEvent
import com.expensetracker.ui.screens.RecurringExpenseUiState
import com.expensetracker.ui.theme.ExpenseTrackerTheme
import com.expensetracker.ui.theme.spacing
import java.math.BigDecimal
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringExpenseBottomSheet(
    uiState: RecurringExpenseUiState,
    onEvent: (RecurringExpenseUiEvent) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier.testTag("recurring_expense_bottom_sheet")
    ) {
        RecurringExpenseForm(
            uiState = uiState,
            onEvent = onEvent,
            onDismiss = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.medium)
                .navigationBarsPadding()
        )
    }
}

@Composable
private fun RecurringExpenseForm(
    uiState: RecurringExpenseUiState,
    onEvent: (RecurringExpenseUiEvent) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.large)
    ) {
        // Header
        FormHeader(
            title = if (uiState.isEditMode) {
                stringResource(R.string.title_edit_recurring_expense)
            } else {
                stringResource(R.string.title_add_recurring_expense)
            },
            onDismiss = onDismiss
        )
        
        // Amount and Currency Section
        AmountSection(
            amount = uiState.amount,
            currency = uiState.currency,
            amountError = uiState.amountError,
            onAmountChanged = { onEvent(RecurringExpenseUiEvent.AmountChanged(it)) },
            onCurrencyChanged = { onEvent(RecurringExpenseUiEvent.CurrencyChanged(it)) }
        )
        
        // Description Section
        DescriptionSection(
            description = uiState.description,
            descriptionError = uiState.descriptionError,
            onDescriptionChanged = { onEvent(RecurringExpenseUiEvent.DescriptionChanged(it)) }
        )
        
        // Frequency Section
        FrequencySection(
            frequency = uiState.frequency,
            onFrequencyChanged = { onEvent(RecurringExpenseUiEvent.FrequencyChanged(it)) }
        )
        
        // Date Section
        DateSection(
            startDate = uiState.startDate,
            endDate = uiState.endDate,
            onStartDateChanged = { onEvent(RecurringExpenseUiEvent.StartDateChanged(it)) },
            onEndDateChanged = { onEvent(RecurringExpenseUiEvent.EndDateChanged(it)) }
        )
        
        // Category Section - Always show, even if empty (with error message)
        CategorySection(
            categories = uiState.availableCategories,
            selectedCategory = uiState.selectedCategory,
            categoryError = uiState.categoryError,
            onCategorySelected = { onEvent(RecurringExpenseUiEvent.CategorySelected(it)) }
        )
        
        // Tags Section
        TagsSection(
            selectedTags = uiState.selectedTags,
            onTagsChanged = { onEvent(RecurringExpenseUiEvent.TagsChanged(it)) }
        )
        
        // Edit Mode Options
        if (uiState.isEditMode) {
            EditModeOptions(
                includePast = uiState.includePast,
                onToggleIncludePast = { onEvent(RecurringExpenseUiEvent.ToggleIncludePast) }
            )
        }
        
        // Action Buttons
        ActionButtons(
            isLoading = uiState.isLoading,
            isEditMode = uiState.isEditMode,
            onSave = { onEvent(RecurringExpenseUiEvent.SaveRecurringExpense) },
            onCancel = onDismiss
        )
        
        // Add extra space at the bottom for better scrolling
        Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
    }
}

@Composable
private fun FormHeader(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Medium
        )
        
        TextButton(
            onClick = onDismiss,
            modifier = Modifier.testTag("cancel_button")
        ) {
            Text(stringResource(R.string.button_cancel))
        }
    }
}

@Composable
private fun AmountSection(
    amount: BigDecimal?,
    currency: String,
    amountError: String?,
    onAmountChanged: (BigDecimal?) -> Unit,
    onCurrencyChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
    ) {
        Text(
            text = stringResource(R.string.label_amount),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        
        AmountInputWithCurrency(
            amount = amount,
            currency = currency,
            onAmountChanged = onAmountChanged,
            onCurrencyChanged = onCurrencyChanged,
            isAmountError = amountError != null,
            amountErrorMessage = amountError
        )
    }
}

@Composable
private fun DescriptionSection(
    description: String,
    descriptionError: String?,
    onDescriptionChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
    ) {
        Text(
            text = stringResource(R.string.label_description),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        
        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChanged,
            label = { Text(stringResource(R.string.label_description)) },
            placeholder = { Text(stringResource(R.string.placeholder_description)) },
            isError = descriptionError != null,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("description_input"),
            singleLine = true
        )
        
        if (descriptionError != null) {
            Text(
                text = descriptionError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = MaterialTheme.spacing.medium)
            )
        }
    }
}

@Composable
private fun FrequencySection(
    frequency: RecurrenceFrequency,
    onFrequencyChanged: (RecurrenceFrequency) -> Unit,
    modifier: Modifier = Modifier
) {
    FrequencySelector(
        selectedFrequency = frequency,
        onFrequencySelected = onFrequencyChanged,
        modifier = modifier
    )
}

@Composable
private fun DateSection(
    startDate: LocalDate?,
    endDate: LocalDate?,
    onStartDateChanged: (LocalDate?) -> Unit,
    onEndDateChanged: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
    ) {
        Text(
            text = stringResource(R.string.label_dates),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            // Start Date
            DatePickerField(
                label = stringResource(R.string.label_start_date_form),
                selectedDate = startDate,
                onDateSelected = onStartDateChanged,
                modifier = Modifier.weight(1f).testTag("start_date_picker")
            )
            
            // End Date (Optional)
            DatePickerField(
                label = stringResource(R.string.label_end_date_form),
                selectedDate = endDate,
                onDateSelected = onEndDateChanged,
                isOptional = true,
                modifier = Modifier.weight(1f).testTag("end_date_picker")
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerField(
    label: String,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
    isOptional: Boolean = false
) {
    var showDatePicker by remember { mutableStateOf(false) }
    
    OutlinedTextField(
        value = selectedDate?.toString() ?: "",
        onValueChange = { },
        label = { Text(label) },
        placeholder = { 
            Text(if (isOptional) "Optional" else "Select date") 
        },
        readOnly = true,
        trailingIcon = {
            IconButton(onClick = { showDatePicker = true }) {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = "Select date"
                )
            }
        },
        modifier = modifier
    )
    
    if (showDatePicker) {
        DatePickerModalDialog(
            selectedDate = selectedDate,
            onDateSelected = { date ->
                onDateSelected(date)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
            isOptional = isOptional
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerModalDialog(
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate?) -> Unit,
    onDismiss: () -> Unit,
    isOptional: Boolean = false
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate?.let {
            it.toEpochDay() * 24 * 60 * 60 * 1000
        }
    )
    
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isOptional) {
                    TextButton(
                        onClick = {
                            onDateSelected(null)
                        }
                    ) {
                        Text("Clear")
                    }
                }
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val date = LocalDate.ofEpochDay(millis / (24 * 60 * 60 * 1000))
                            onDateSelected(date)
                        } ?: onDateSelected(LocalDate.now())
                    }
                ) {
                    Text("OK")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
private fun CategorySection(
    categories: List<Category>,
    selectedCategory: Category?,
    categoryError: String?,
    onCategorySelected: (Category) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
    ) {
        Text(
            text = stringResource(R.string.label_category),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        
        if (categories.isEmpty()) {
            // Show loading or empty state
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = "No categories available. Please wait...",
                    modifier = Modifier.padding(MaterialTheme.spacing.medium),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            CategoryGrid(
                categories = categories,
                selectedCategory = selectedCategory,
                onCategorySelected = onCategorySelected
            )
        }
        
        if (categoryError != null) {
            Text(
                text = categoryError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = MaterialTheme.spacing.medium)
            )
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
                            .testTag("category_chip_${category.name.lowercase()}")
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
private fun TagsSection(
    selectedTags: List<String>,
    onTagsChanged: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
    ) {
        Text(
            text = stringResource(R.string.label_tags),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
        
        TagInput(
            selectedTags = selectedTags,
            availableTags = emptyList(), // TODO: Add available tags from repository
            onTagsChanged = onTagsChanged,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("tag_input")
        )
    }
}

@Composable
private fun EditModeOptions(
    includePast: Boolean,
    onToggleIncludePast: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
        ) {
            Text(
                text = stringResource(R.string.title_edit_options),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = includePast,
                        onClick = onToggleIncludePast
                    )
                    .padding(vertical = MaterialTheme.spacing.small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = includePast,
                    onCheckedChange = { onToggleIncludePast() },
                    modifier = Modifier.testTag("include_past_checkbox")
                )
                
                Spacer(modifier = Modifier.width(MaterialTheme.spacing.small))
                
                Column {
                    Text(
                        text = stringResource(R.string.option_include_past_expenses),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = stringResource(R.string.option_include_past_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionButtons(
    isLoading: Boolean,
    isEditMode: Boolean,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
    ) {
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier
                .weight(1f)
                .testTag("cancel_button_bottom"),
            enabled = !isLoading
        ) {
            Text(stringResource(R.string.button_cancel))
        }
        
        Button(
            onClick = onSave,
            modifier = Modifier
                .weight(1f)
                .testTag("save_button"),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(
                    if (isEditMode) {
                        stringResource(R.string.button_update)
                    } else {
                        stringResource(R.string.button_save)
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Add Mode")
@Composable
private fun RecurringExpenseFormAddPreview() {
    ExpenseTrackerTheme {
        val sampleCategories = listOf(
            Category(id = 1, name = "Housing", color = "#2196F3", icon = "home"),
            Category(id = 2, name = "Food", color = "#4CAF50", icon = "restaurant"),
            Category(id = 3, name = "Transport", color = "#FF9800", icon = "directions_car")
        )
        
        val sampleUiState = RecurringExpenseUiState(
            availableCategories = sampleCategories,
            amount = BigDecimal("25.00"),
            currency = "USD",
            description = "Weekly groceries",
            frequency = RecurrenceFrequency.WEEKLY,
            startDate = LocalDate.now(),
            selectedTags = listOf("groceries", "food")
        )
        
        RecurringExpenseForm(
            uiState = sampleUiState,
            onEvent = { },
            onDismiss = { },
            modifier = Modifier.padding(16.dp)
        )
    }
}