package com.expensetracker.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.data.model.Category
import com.expensetracker.data.model.Expense
import com.expensetracker.data.repository.ExpenseRepository
import com.expensetracker.domain.error.UserFacingError
import com.expensetracker.domain.validation.AmountValidator
import com.expensetracker.domain.validation.CategoryValidator
import com.expensetracker.domain.validation.DescriptionValidator
import com.expensetracker.domain.validation.TagValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDateTime
import javax.inject.Inject

data class AddExpenseUiState(
    val amount: BigDecimal? = null,
    val currency: String = "USD",
    val description: String = "",
    val selectedCategory: Category? = null,
    val selectedTags: List<String> = emptyList(),
    val availableCategories: List<Category> = emptyList(),
    val availableTags: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: UserFacingError? = null,
    val amountError: String? = null,
    val descriptionError: String? = null,
    val categoryError: String? = null
)

sealed class AddExpenseUiEvent {
    data class AmountChanged(val amount: BigDecimal?) : AddExpenseUiEvent()
    data class CurrencyChanged(val currency: String) : AddExpenseUiEvent()
    data class DescriptionChanged(val description: String) : AddExpenseUiEvent()
    data class CategorySelected(val category: Category) : AddExpenseUiEvent()
    data class TagsChanged(val tags: List<String>) : AddExpenseUiEvent()
    object SaveExpense : AddExpenseUiEvent()
    object ResetForm : AddExpenseUiEvent()
    object ClearError : AddExpenseUiEvent()
    object ValidateForm : AddExpenseUiEvent()
    object LoadInitialData : AddExpenseUiEvent()
}

@HiltViewModel
class AddExpenseViewModel @Inject constructor(
    private val repository: ExpenseRepository,
    private val amountValidator: AmountValidator,
    private val descriptionValidator: DescriptionValidator,
    private val categoryValidator: CategoryValidator,
    private val tagValidator: TagValidator
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddExpenseUiState())
    val uiState: StateFlow<AddExpenseUiState> = _uiState.asStateFlow()
    
    // Expose validators for UI components
    fun getAmountValidator() = amountValidator
    fun getDescriptionValidator() = descriptionValidator
    fun getTagValidator() = tagValidator
    fun getCategoryValidator() = categoryValidator

    init {
        // Initialize data using the event system for consistency
        onUiEvent(AddExpenseUiEvent.LoadInitialData)
    }

    private suspend fun handleLoadInitialData() {
        try {
            // Initialize default categories first
            repository.initializeDefaultCategories()
            
            // Load categories and tags sequentially to avoid race conditions
            val tags = repository.getAllTags()
            _uiState.update { it.copy(availableTags = tags) }
            
            // Now collect categories - this will continue to update state
            repository.getAllCategories().collectLatest { categories ->
                _uiState.update { it.copy(availableCategories = categories) }
            }
        } catch (e: Exception) {
            _uiState.update { 
                it.copy(errorMessage = UserFacingError.LoadFailedWithReason(e.message ?: "Unknown error"))
            }
        }
    }

    private fun loadInitialData() {
        onUiEvent(AddExpenseUiEvent.LoadInitialData)
    }

    fun onUiEvent(event: AddExpenseUiEvent) {
        viewModelScope.launch {
            when (event) {
                is AddExpenseUiEvent.AmountChanged -> {
                    _uiState.update { 
                        it.copy(
                            amount = event.amount,
                            amountError = null
                        )
                    }
                }
                is AddExpenseUiEvent.CurrencyChanged -> {
                    _uiState.update { it.copy(currency = event.currency) }
                }
                is AddExpenseUiEvent.DescriptionChanged -> {
                    _uiState.update { 
                        it.copy(
                            description = event.description,
                            descriptionError = null
                        )
                    }
                }
                is AddExpenseUiEvent.CategorySelected -> {
                    _uiState.update { 
                        it.copy(
                            selectedCategory = event.category,
                            categoryError = null
                        )
                    }
                }
                is AddExpenseUiEvent.TagsChanged -> {
                    _uiState.update { it.copy(selectedTags = event.tags) }
                }
                is AddExpenseUiEvent.SaveExpense -> {
                    handleSaveExpense()
                }
                is AddExpenseUiEvent.ResetForm -> {
                    _uiState.update {
                        AddExpenseUiState(
                            availableCategories = it.availableCategories,
                            availableTags = it.availableTags
                        )
                    }
                }
                is AddExpenseUiEvent.ClearError -> {
                    _uiState.update { it.copy(errorMessage = null) }
                }
                is AddExpenseUiEvent.ValidateForm -> {
                    handleValidateForm()
                }
                is AddExpenseUiEvent.LoadInitialData -> {
                    handleLoadInitialData()
                }
            }
        }
    }

    fun updateAmount(amount: BigDecimal?) {
        onUiEvent(AddExpenseUiEvent.AmountChanged(amount))
    }

    fun updateCurrency(currency: String) {
        onUiEvent(AddExpenseUiEvent.CurrencyChanged(currency))
    }

    fun updateDescription(description: String) {
        onUiEvent(AddExpenseUiEvent.DescriptionChanged(description))
    }

    fun selectCategory(category: Category) {
        onUiEvent(AddExpenseUiEvent.CategorySelected(category))
    }

    fun updateTags(tags: List<String>) {
        onUiEvent(AddExpenseUiEvent.TagsChanged(tags))
    }

    private suspend fun handleValidateForm(): Boolean {
        val currentState = _uiState.value
        
        // Validate using validators
        val amountValidation = amountValidator.validate(currentState.amount)
        val descriptionValidation = descriptionValidator.validate(currentState.description)
        val categoryValidation = categoryValidator.validate(currentState.selectedCategory)
        val tagValidation = tagValidator.validateTagList(currentState.selectedTags)

        // Update state with validation results
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

    fun validateForm(): Boolean {
        // Legacy method for backward compatibility - triggers event-based validation
        onUiEvent(AddExpenseUiEvent.ValidateForm)
        // Wait for validation to complete before returning result
        val currentState = _uiState.value
        return currentState.run {
            amountError == null && descriptionError == null && categoryError == null
        }
    }

    private suspend fun handleSaveExpense() {
        if (!handleValidateForm()) {
            return
        }

        val currentState = _uiState.value
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        try {
            val expense = Expense(
                amount = currentState.amount!!,
                currency = currentState.currency,
                description = currentState.description.trim(),
                categoryId = currentState.selectedCategory!!.id,
                tags = currentState.selectedTags,
                date = LocalDateTime.now()
            )

            val result = repository.insertExpense(expense)
            
            if (result.isSuccess) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        isSaved = true,
                        errorMessage = null
                    )
                }
            } else {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = UserFacingError.SaveFailedWithReason(result.exceptionOrNull()?.message ?: "Unknown error")
                    )
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

    fun saveExpense() {
        onUiEvent(AddExpenseUiEvent.SaveExpense)
    }

    fun resetForm() {
        onUiEvent(AddExpenseUiEvent.ResetForm)
    }

    fun clearError() {
        onUiEvent(AddExpenseUiEvent.ClearError)
    }
}