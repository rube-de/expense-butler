package com.expensetracker.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.data.model.Category
import com.expensetracker.data.model.RecurrenceFrequency
import com.expensetracker.data.model.RecurringExpense
import com.expensetracker.data.repository.ExpenseRepository
import com.expensetracker.domain.error.UserFacingError
import com.expensetracker.domain.recurring.*
import com.expensetracker.domain.validation.AmountValidator
import com.expensetracker.domain.validation.CategoryValidator
import com.expensetracker.domain.validation.DescriptionValidator
import com.expensetracker.domain.validation.TagValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject

data class RecurringExpenseUiState(
    val recurringExpenses: List<RecurringExpense> = emptyList(),
    val availableCategories: List<Category> = emptyList(),
    val forecast: List<ForecastedExpense> = emptyList(),
    
    // Form fields
    val amount: BigDecimal? = null,
    val currency: String = "USD",
    val description: String = "",
    val frequency: RecurrenceFrequency = RecurrenceFrequency.MONTHLY,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val selectedCategory: Category? = null,
    val selectedTags: List<String> = emptyList(),
    
    // Edit mode state
    val isEditMode: Boolean = false,
    val currentEditingExpense: RecurringExpense? = null,
    val includePast: Boolean = false, // Toggle for modifying past expenses
    
    // UI state
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: UserFacingError? = null,
    val syncMessage: String = "",
    val lastSyncTime: Long = 0L,
    
    // Validation errors
    val amountError: String? = null,
    val descriptionError: String? = null,
    val categoryError: String? = null
)

sealed class RecurringExpenseUiEvent {
    // Form events
    data class AmountChanged(val amount: BigDecimal?) : RecurringExpenseUiEvent()
    data class CurrencyChanged(val currency: String) : RecurringExpenseUiEvent()
    data class DescriptionChanged(val description: String) : RecurringExpenseUiEvent()
    data class FrequencyChanged(val frequency: RecurrenceFrequency) : RecurringExpenseUiEvent()
    data class StartDateChanged(val startDate: LocalDate?) : RecurringExpenseUiEvent()
    data class EndDateChanged(val endDate: LocalDate?) : RecurringExpenseUiEvent()
    data class CategorySelected(val category: Category) : RecurringExpenseUiEvent()
    data class TagsChanged(val tags: List<String>) : RecurringExpenseUiEvent()
    
    // CRUD events
    object SaveRecurringExpense : RecurringExpenseUiEvent()
    data class EditRecurringExpense(val recurringExpense: RecurringExpense) : RecurringExpenseUiEvent()
    data class DeleteRecurringExpense(val recurringExpenseId: Long) : RecurringExpenseUiEvent()
    object CancelEdit : RecurringExpenseUiEvent()
    
    // Mode toggles
    object ToggleIncludePast : RecurringExpenseUiEvent()
    
    // Forecast events
    data class GenerateForecast(
        val recurringExpense: RecurringExpense,
        val period: ForecastPeriod,
        val fromDate: LocalDate? = null,
        val toDate: LocalDate? = null
    ) : RecurringExpenseUiEvent()
    object ClearForecast : RecurringExpenseUiEvent()
    
    // Sync events
    object ManualSync : RecurringExpenseUiEvent()
    
    // Utility events
    object ResetForm : RecurringExpenseUiEvent()
    object ClearError : RecurringExpenseUiEvent()
    object LoadInitialData : RecurringExpenseUiEvent()
}

@HiltViewModel
class RecurringExpenseViewModel @Inject constructor(
    private val repository: ExpenseRepository,
    private val generator: RecurringExpenseGenerator,
    private val generationManager: GenerationManager,
    private val modificationHandler: RecurringExpenseModificationHandler,
    private val amountValidator: AmountValidator,
    private val descriptionValidator: DescriptionValidator,
    private val categoryValidator: CategoryValidator,
    private val tagValidator: TagValidator
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecurringExpenseUiState())
    val uiState: StateFlow<RecurringExpenseUiState> = _uiState.asStateFlow()

    init {
        onUiEvent(RecurringExpenseUiEvent.LoadInitialData)
    }

    fun onUiEvent(event: RecurringExpenseUiEvent) {
        viewModelScope.launch {
            when (event) {
                is RecurringExpenseUiEvent.AmountChanged -> {
                    _uiState.update {
                        it.copy(
                            amount = event.amount,
                            amountError = null
                        )
                    }
                }
                is RecurringExpenseUiEvent.CurrencyChanged -> {
                    _uiState.update { it.copy(currency = event.currency) }
                }
                is RecurringExpenseUiEvent.DescriptionChanged -> {
                    _uiState.update {
                        it.copy(
                            description = event.description,
                            descriptionError = null
                        )
                    }
                }
                is RecurringExpenseUiEvent.FrequencyChanged -> {
                    _uiState.update { it.copy(frequency = event.frequency) }
                }
                is RecurringExpenseUiEvent.StartDateChanged -> {
                    _uiState.update { it.copy(startDate = event.startDate) }
                }
                is RecurringExpenseUiEvent.EndDateChanged -> {
                    _uiState.update { it.copy(endDate = event.endDate) }
                }
                is RecurringExpenseUiEvent.CategorySelected -> {
                    _uiState.update {
                        it.copy(
                            selectedCategory = event.category,
                            categoryError = null
                        )
                    }
                }
                is RecurringExpenseUiEvent.TagsChanged -> {
                    _uiState.update { it.copy(selectedTags = event.tags) }
                }
                is RecurringExpenseUiEvent.SaveRecurringExpense -> {
                    handleSaveRecurringExpense()
                }
                is RecurringExpenseUiEvent.EditRecurringExpense -> {
                    handleEditRecurringExpense(event.recurringExpense)
                }
                is RecurringExpenseUiEvent.DeleteRecurringExpense -> {
                    handleDeleteRecurringExpense(event.recurringExpenseId)
                }
                is RecurringExpenseUiEvent.CancelEdit -> {
                    handleCancelEdit()
                }
                is RecurringExpenseUiEvent.ToggleIncludePast -> {
                    _uiState.update { it.copy(includePast = !it.includePast) }
                }
                is RecurringExpenseUiEvent.GenerateForecast -> {
                    handleGenerateForecast(event.recurringExpense, event.period, event.fromDate, event.toDate)
                }
                is RecurringExpenseUiEvent.ClearForecast -> {
                    _uiState.update { it.copy(forecast = emptyList()) }
                }
                is RecurringExpenseUiEvent.ManualSync -> {
                    handleManualSync()
                }
                is RecurringExpenseUiEvent.ResetForm -> {
                    handleResetForm()
                }
                is RecurringExpenseUiEvent.ClearError -> {
                    _uiState.update { it.copy(errorMessage = null) }
                }
                is RecurringExpenseUiEvent.LoadInitialData -> {
                    handleLoadInitialData()
                }
            }
        }
    }

    private suspend fun handleLoadInitialData() {
        try {
            // Initialize categories first
            repository.initializeDefaultCategories()
        } catch (e: Exception) {
            println("Failed to initialize categories: ${e.message}")
        }
        
        // Start collecting recurring expenses
        viewModelScope.launch {
            try {
                repository.getAllRecurringExpenses().collect { expenses ->
                    _uiState.update { it.copy(recurringExpenses = expenses) }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = UserFacingError.LoadFailedWithReason(e.message ?: "Unknown error"))
                }
            }
        }
        
        // Start collecting categories separately
        viewModelScope.launch {
            try {
                repository.getAllCategories().collect { categories ->
                    _uiState.update { it.copy(availableCategories = categories) }
                }
            } catch (e: Exception) {
                // Category loading failure shouldn't break the whole screen
                println("Failed to load categories: ${e.message}")
            }
        }
        
        // Load last sync time
        try {
            val lastSyncTime = generationManager.getLastGenerationTime()
            _uiState.update { it.copy(lastSyncTime = lastSyncTime) }
        } catch (e: Exception) {
            println("Failed to load sync time: ${e.message}")
        }
    }

    private suspend fun handleSaveRecurringExpense() {
        if (!validateForm()) {
            return
        }

        val currentState = _uiState.value
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        try {
            if (currentState.isEditMode && currentState.currentEditingExpense != null) {
                // Update existing recurring expense
                val updatedExpense = createRecurringExpenseFromForm(currentState.currentEditingExpense.id)
                val result = modificationHandler.updateRecurringExpense(updatedExpense, currentState.includePast)
                
                if (result.isSuccess) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSaved = true,
                            isEditMode = false,
                            currentEditingExpense = null,
                            errorMessage = null
                        )
                    }
                    handleResetForm()
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = UserFacingError.SaveFailedWithReason(result.exceptionOrNull()?.message ?: "Update failed")
                        )
                    }
                }
            } else {
                // Create new recurring expense
                val recurringExpense = createRecurringExpenseFromForm()
                val result = repository.insertRecurringExpense(recurringExpense)
                
                if (result.isSuccess) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSaved = true,
                            errorMessage = null
                        )
                    }
                    handleResetForm()
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = UserFacingError.SaveFailedWithReason(result.exceptionOrNull()?.message ?: "Save failed")
                        )
                    }
                }
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = UserFacingError.SaveFailedWithReason(e.message ?: "Unknown error")
                )
            }
        }
    }

    private fun handleEditRecurringExpense(recurringExpense: RecurringExpense) {
        _uiState.update {
            it.copy(
                isEditMode = true,
                currentEditingExpense = recurringExpense,
                amount = recurringExpense.amount,
                currency = recurringExpense.currency,
                description = recurringExpense.description,
                frequency = recurringExpense.frequency,
                startDate = recurringExpense.startDate,
                endDate = recurringExpense.endDate,
                selectedTags = recurringExpense.tags,
                // Category will be set when categories are loaded
                isSaved = false,
                errorMessage = null
            )
        }
        
        // Find and set the category
        val category = _uiState.value.availableCategories.find { it.id == recurringExpense.categoryId }
        if (category != null) {
            _uiState.update { it.copy(selectedCategory = category) }
        }
    }

    private suspend fun handleDeleteRecurringExpense(recurringExpenseId: Long) {
        try {
            val result = repository.deleteRecurringExpense(recurringExpenseId)
            if (result.isFailure) {
                _uiState.update {
                    it.copy(errorMessage = UserFacingError.DeleteFailedWithReason(result.exceptionOrNull()?.message ?: "Delete failed"))
                }
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(errorMessage = UserFacingError.DeleteFailedWithReason(e.message ?: "Unknown error"))
            }
        }
    }

    private fun handleCancelEdit() {
        _uiState.update {
            it.copy(
                isEditMode = false,
                currentEditingExpense = null,
                includePast = false
            )
        }
        handleResetForm()
    }

    private suspend fun handleGenerateForecast(
        recurringExpense: RecurringExpense,
        period: ForecastPeriod,
        fromDate: LocalDate?,
        toDate: LocalDate?
    ) {
        try {
            val forecast = generator.generateForecast(
                recurringExpense = recurringExpense,
                period = period,
                fromDate = fromDate ?: LocalDate.now(),
                toDate = toDate
            )
            _uiState.update { it.copy(forecast = forecast) }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(errorMessage = UserFacingError.LoadFailedWithReason("Failed to generate forecast: ${e.message}"))
            }
        }
    }

    private suspend fun handleManualSync() {
        try {
            val result = generationManager.generateManually()
            _uiState.update {
                it.copy(
                    syncMessage = result.message,
                    lastSyncTime = result.timestamp
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(syncMessage = "Sync failed")
            }
        }
    }

    private fun handleResetForm() {
        _uiState.update {
            it.copy(
                amount = null,
                currency = "USD",
                description = "",
                frequency = RecurrenceFrequency.MONTHLY,
                startDate = null,
                endDate = null,
                selectedCategory = null,
                selectedTags = emptyList(),
                isEditMode = false,
                currentEditingExpense = null,
                includePast = false,
                isSaved = false,
                amountError = null,
                descriptionError = null,
                categoryError = null
            )
        }
    }

    private suspend fun validateForm(): Boolean {
        val currentState = _uiState.value
        
        val amountValidation = amountValidator.validate(currentState.amount)
        val descriptionValidation = descriptionValidator.validate(currentState.description)
        val categoryValidation = categoryValidator.validate(currentState.selectedCategory)
        val tagValidation = tagValidator.validateTagList(currentState.selectedTags)

        _uiState.update {
            it.copy(
                amountError = amountValidation.getErrorMessage(),
                descriptionError = descriptionValidation.getErrorMessage(),
                categoryError = categoryValidation.getErrorMessage()
            )
        }

        return amountValidation.isValid &&
               descriptionValidation.isValid &&
               categoryValidation.isValid &&
               tagValidation.isValid
    }

    private fun createRecurringExpenseFromForm(id: Long = 0L): RecurringExpense {
        val currentState = _uiState.value
        return RecurringExpense(
            id = id,
            amount = currentState.amount!!,
            currency = currentState.currency,
            description = currentState.description.trim(),
            categoryId = currentState.selectedCategory!!.id,
            tags = currentState.selectedTags,
            frequency = currentState.frequency,
            startDate = currentState.startDate ?: LocalDate.now(),
            endDate = currentState.endDate,
            lastGenerated = if (id == 0L) null else _uiState.value.currentEditingExpense?.lastGenerated,
            isActive = true
        )
    }
}