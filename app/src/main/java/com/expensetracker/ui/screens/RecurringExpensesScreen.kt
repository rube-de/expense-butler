package com.expensetracker.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.expensetracker.R
import com.expensetracker.data.model.Category
import com.expensetracker.data.model.RecurrenceFrequency
import com.expensetracker.data.model.RecurringExpense
import com.expensetracker.domain.error.UserFacingError
import com.expensetracker.ui.components.RecurringExpenseCard
import com.expensetracker.ui.components.RecurringExpenseBottomSheet
import com.expensetracker.ui.theme.ExpenseTrackerTheme
import com.expensetracker.ui.theme.spacing
import com.expensetracker.ui.util.LogCompositions
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringExpensesScreen(
    modifier: Modifier = Modifier,
    viewModel: RecurringExpenseViewModel = hiltViewModel()
) {
    LogCompositions("RecurringExpensesScreen")
    
    val uiState by viewModel.uiState.collectAsState()
    
    // Local state for dialogs and bottom sheets
    var showBottomSheet by remember { mutableStateOf(false) }
    var expenseToDelete by remember { mutableStateOf<RecurringExpense?>(null) }
    
    // Handle form state changes
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            showBottomSheet = false
        }
    }
    
    // Handle error messages
    uiState.errorMessage?.let { errorMessage ->
        LaunchedEffect(errorMessage) {
            // In a real app, you might want to show a Snackbar here
            viewModel.onUiEvent(RecurringExpenseUiEvent.ClearError)
        }
    }

    Scaffold(
        topBar = {
            RecurringExpensesTopBar(
                syncMessage = uiState.syncMessage,
                lastSyncTime = uiState.lastSyncTime,
                onSyncClick = { viewModel.onUiEvent(RecurringExpenseUiEvent.ManualSync) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.onUiEvent(RecurringExpenseUiEvent.ResetForm)
                    showBottomSheet = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("add_recurring_expense_fab")
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.cd_add_expense)
                )
            }
        }
    ) { paddingValues ->
        RecurringExpensesContent(
            uiState = uiState,
            onEdit = { recurringExpense ->
                viewModel.onUiEvent(RecurringExpenseUiEvent.EditRecurringExpense(recurringExpense))
                showBottomSheet = true
            },
            onDelete = { recurringExpense ->
                expenseToDelete = recurringExpense
            },
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        )
    }
    
    // Bottom Sheet for Add/Edit
    if (showBottomSheet) {
        RecurringExpenseBottomSheet(
            uiState = uiState,
            onEvent = viewModel::onUiEvent,
            onDismiss = { 
                showBottomSheet = false
                viewModel.onUiEvent(RecurringExpenseUiEvent.CancelEdit)
            }
        )
    }
    
    // Delete confirmation dialog
    expenseToDelete?.let { expense ->
        RecurringExpenseDeleteDialog(
            recurringExpense = expense,
            onConfirm = {
                viewModel.onUiEvent(RecurringExpenseUiEvent.DeleteRecurringExpense(expense.id))
                expenseToDelete = null
            },
            onDismiss = {
                expenseToDelete = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecurringExpensesTopBar(
    syncMessage: String,
    lastSyncTime: Long,
    onSyncClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = { 
            Column {
                Text(
                    stringResource(R.string.title_recurring_expenses),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium
                )
                if (lastSyncTime > 0L) {
                    Text(
                        text = formatSyncTime(lastSyncTime),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = onSyncClick) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.action_refresh)
                )
            }
        },
        modifier = modifier
    )
}

@Composable
private fun RecurringExpensesContent(
    uiState: RecurringExpenseUiState,
    onEdit: (RecurringExpense) -> Unit,
    onDelete: (RecurringExpense) -> Unit,
    modifier: Modifier = Modifier
) {
    LogCompositions("RecurringExpensesContent")
    
    when {
        uiState.isLoading && uiState.recurringExpenses.isEmpty() -> {
            LoadingState(modifier = modifier)
        }
        uiState.errorMessage != null && uiState.recurringExpenses.isEmpty() -> {
            ErrorState(
                message = uiState.errorMessage,
                modifier = modifier
            )
        }
        uiState.recurringExpenses.isEmpty() -> {
            EmptyState(modifier = modifier)
        }
        else -> {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(horizontal = MaterialTheme.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                contentPadding = PaddingValues(vertical = MaterialTheme.spacing.medium)
            ) {
                items(uiState.recurringExpenses, key = { it.id }) { recurringExpense ->
                    val category = uiState.availableCategories.find { it.id == recurringExpense.categoryId }
                    
                    RecurringExpenseCard(
                        recurringExpense = recurringExpense,
                        category = category,
                        onEdit = onEdit,
                        onDelete = onDelete,
                        modifier = Modifier.testTag("recurring_expense_card_${recurringExpense.id}")
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
        ) {
            CircularProgressIndicator()
            Text(
                text = stringResource(R.string.loading_expenses),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorState(
    message: UserFacingError,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
        ) {
            Text(
                text = when (message) {
                    is UserFacingError.LoadFailedWithReason -> message.technicalReason
                    is UserFacingError.SaveFailedWithReason -> message.technicalReason
                    is UserFacingError.DeleteFailedWithReason -> message.technicalReason
                    is UserFacingError.UpdateFailedWithReason -> message.technicalReason
                    else -> stringResource(R.string.error_unexpected)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = MaterialTheme.spacing.large)
            )
        }
    }
}

@Composable
private fun EmptyState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
        ) {
            Text(
                text = stringResource(R.string.empty_expenses_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Start tracking recurring expenses by tapping the + button",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = MaterialTheme.spacing.large)
            )
        }
    }
}

// Delete dialog implementation

@Composable
private fun RecurringExpenseDeleteDialog(
    recurringExpense: RecurringExpense,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                text = stringResource(R.string.dialog_delete_recurring_title),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
            ) {
                Text(
                    text = stringResource(
                        R.string.dialog_delete_recurring_message,
                        recurringExpense.description
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
                
                // Show expense details in a card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(MaterialTheme.spacing.medium),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.label_amount),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatAmount(recurringExpense.amount, recurringExpense.currency),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.label_frequency),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = when (recurringExpense.frequency) {
                                    RecurrenceFrequency.DAILY -> stringResource(R.string.frequency_daily)
                                    RecurrenceFrequency.WEEKLY -> stringResource(R.string.frequency_weekly)
                                    RecurrenceFrequency.MONTHLY -> stringResource(R.string.frequency_monthly)
                                    RecurrenceFrequency.YEARLY -> stringResource(R.string.frequency_yearly)
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        
                        recurringExpense.lastGenerated?.let { lastGenerated ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(R.string.label_last_generated_short),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatDate(lastGenerated),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
                
                Text(
                    text = stringResource(R.string.dialog_delete_recurring_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.testTag("confirm_delete_button")
            ) {
                Text(
                    text = stringResource(R.string.button_delete),
                    color = MaterialTheme.colorScheme.onError
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_delete_button")
            ) {
                Text(stringResource(R.string.button_cancel))
            }
        }
    )
}

private fun formatSyncTime(timestamp: Long): String {
    val instant = Instant.ofEpochMilli(timestamp)
    val zonedDateTime = instant.atZone(ZoneId.systemDefault())
    val formatter = DateTimeFormatter.ofPattern("MMM dd, HH:mm")
    return "Last sync: ${zonedDateTime.format(formatter)}"
}

private fun formatAmount(amount: BigDecimal, currency: String): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale.getDefault())
    formatter.currency = Currency.getInstance(currency)
    return formatter.format(amount)
}

private fun formatDate(date: LocalDate): String {
    return "${date.monthValue}/${date.dayOfMonth}/${date.year}"
}

@Preview(showBackground = true, name = "Loading State")
@Composable
private fun RecurringExpensesScreenLoadingPreview() {
    ExpenseTrackerTheme {
        LoadingState()
    }
}

@Preview(showBackground = true, name = "Empty State")
@Composable
private fun RecurringExpensesScreenEmptyPreview() {
    ExpenseTrackerTheme {
        EmptyState()
    }
}

@Preview(showBackground = true, name = "List with Items")
@Composable
private fun RecurringExpensesScreenPreview() {
    ExpenseTrackerTheme {
        val sampleRecurringExpenses = listOf(
            RecurringExpense(
                id = 1,
                amount = BigDecimal("1200.00"),
                currency = "USD",
                description = "Monthly rent payment",
                categoryId = 1,
                tags = listOf("rent", "housing"),
                frequency = RecurrenceFrequency.MONTHLY,
                startDate = LocalDate.of(2024, 1, 1),
                endDate = null,
                lastGenerated = LocalDate.of(2024, 1, 1),
                isActive = true
            ),
            RecurringExpense(
                id = 2,
                amount = BigDecimal("9.99"),
                currency = "USD",
                description = "Netflix subscription",
                categoryId = 2,
                tags = listOf("entertainment", "streaming"),
                frequency = RecurrenceFrequency.MONTHLY,
                startDate = LocalDate.of(2023, 6, 1),
                endDate = null,
                lastGenerated = LocalDate.of(2024, 1, 1),
                isActive = true
            )
        )
        
        val sampleCategories = listOf(
            Category(id = 1, name = "Housing", color = "#2196F3", icon = "home"),
            Category(id = 2, name = "Entertainment", color = "#FF5722", icon = "movie")
        )
        
        val sampleUiState = RecurringExpenseUiState(
            recurringExpenses = sampleRecurringExpenses,
            availableCategories = sampleCategories,
            lastSyncTime = System.currentTimeMillis()
        )
        
        RecurringExpensesContent(
            uiState = sampleUiState,
            onEdit = { },
            onDelete = { }
        )
    }
}