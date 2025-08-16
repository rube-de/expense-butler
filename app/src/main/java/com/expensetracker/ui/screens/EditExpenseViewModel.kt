package com.expensetracker.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.data.model.Category
import com.expensetracker.data.model.Expense
import com.expensetracker.data.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDateTime
import javax.inject.Inject

data class EditExpenseUiState(
    val originalExpense: Expense? = null,
    val amount: BigDecimal? = null,
    val currency: String = "USD",
    val description: String = "",
    val selectedCategory: Category? = null,
    val selectedTags: List<String> = emptyList(),
    val availableCategories: List<Category> = emptyList(),
    val availableTags: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val isUpdated: Boolean = false,
    val isDeleted: Boolean = false,
    val errorMessage: String? = null,
    val amountError: String? = null,
    val descriptionError: String? = null,
    val categoryError: String? = null,
    val showDeleteConfirmation: Boolean = false
)

@HiltViewModel
class EditExpenseViewModel @Inject constructor(
    private val repository: ExpenseRepository,
    savedStateHandle: androidx.lifecycle.SavedStateHandle
) : ViewModel() {
    
    private val expenseId: Long = savedStateHandle.get<Long>("expenseId") ?: 0L

    private val _uiState = MutableStateFlow(EditExpenseUiState())
    val uiState: StateFlow<EditExpenseUiState> = _uiState.asStateFlow()

    init {
        loadExpenseData()
        loadInitialData()
    }

    private fun loadExpenseData() {
        viewModelScope.launch {
            try {
                repository.getExpenseById(expenseId).collect { expense ->
                    if (expense != null) {
                        _uiState.update { 
                            it.copy(
                                originalExpense = expense,
                                amount = expense.amount,
                                currency = expense.currency,
                                description = expense.description,
                                selectedTags = expense.tags,
                                isLoading = false,
                                errorMessage = null
                            )
                        }
                        
                        // Load the category for this expense
                        loadCategoryForExpense(expense.categoryId)
                    } else {
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                errorMessage = "Expense not found"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load expense: ${e.message}"
                    )
                }
            }
        }
    }

    private fun loadCategoryForExpense(categoryId: Long) {
        viewModelScope.launch {
            try {
                repository.getAllCategories().collect { categories ->
                    val selectedCategory = categories.find { it.id == categoryId }
                    _uiState.update { 
                        it.copy(
                            availableCategories = categories,
                            selectedCategory = selectedCategory
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(errorMessage = "Failed to load categories: ${e.message}")
                }
            }
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            try {
                // Load available tags
                val tags = repository.getAllTags()
                _uiState.update { it.copy(availableTags = tags) }
            } catch (e: Exception) {
                // Tags are optional, so don't show error for this
            }
        }
    }

    fun updateAmount(amount: BigDecimal?) {
        _uiState.update { 
            it.copy(
                amount = amount,
                amountError = null
            )
        }
    }

    fun updateCurrency(currency: String) {
        _uiState.update { it.copy(currency = currency) }
    }

    fun updateDescription(description: String) {
        _uiState.update { 
            it.copy(
                description = description,
                descriptionError = null
            )
        }
    }

    fun selectCategory(category: Category) {
        _uiState.update { 
            it.copy(
                selectedCategory = category,
                categoryError = null
            )
        }
    }

    fun updateTags(tags: List<String>) {
        _uiState.update { it.copy(selectedTags = tags) }
    }

    fun validateForm(): Boolean {
        val currentState = _uiState.value
        var isValid = true
        var amountError: String? = null
        var descriptionError: String? = null
        var categoryError: String? = null

        // Validate amount
        if (currentState.amount == null) {
            amountError = "Amount is required"
            isValid = false
        } else if (currentState.amount <= BigDecimal.ZERO) {
            amountError = "Amount must be greater than 0"
            isValid = false
        }

        // Validate description
        if (currentState.description.isBlank()) {
            descriptionError = "Description is required"
            isValid = false
        }

        // Validate category
        if (currentState.selectedCategory == null) {
            categoryError = "Please select a category"
            isValid = false
        }

        _uiState.update {
            it.copy(
                amountError = amountError,
                descriptionError = descriptionError,
                categoryError = categoryError
            )
        }

        return isValid
    }

    fun updateExpense() {
        if (!validateForm()) {
            return
        }

        val currentState = _uiState.value
        val originalExpense = currentState.originalExpense ?: return
        
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val updatedExpense = originalExpense.copy(
                    amount = currentState.amount!!,
                    currency = currentState.currency,
                    description = currentState.description.trim(),
                    categoryId = currentState.selectedCategory!!.id,
                    tags = currentState.selectedTags
                )

                val result = repository.updateExpense(updatedExpense)
                
                if (result.isSuccess) {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            isUpdated = true,
                            errorMessage = null
                        )
                    }
                } else {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            errorMessage = "Failed to update expense: ${result.exceptionOrNull()?.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to update expense: ${e.message}"
                    )
                }
            }
        }
    }

    fun showDeleteConfirmation() {
        _uiState.update { it.copy(showDeleteConfirmation = true) }
    }

    fun hideDeleteConfirmation() {
        _uiState.update { it.copy(showDeleteConfirmation = false) }
    }

    fun deleteExpense() {
        _uiState.update { 
            it.copy(
                isLoading = true, 
                errorMessage = null,
                showDeleteConfirmation = false
            )
        }

        viewModelScope.launch {
            try {
                val result = repository.deleteExpense(expenseId)
                
                if (result.isSuccess) {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            isDeleted = true,
                            errorMessage = null
                        )
                    }
                } else {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            errorMessage = "Failed to delete expense: ${result.exceptionOrNull()?.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to delete expense: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun hasChanges(): Boolean {
        val currentState = _uiState.value
        val original = currentState.originalExpense ?: return false
        
        return original.amount != currentState.amount ||
               original.currency != currentState.currency ||
               original.description != currentState.description ||
               original.categoryId != currentState.selectedCategory?.id ||
               original.tags != currentState.selectedTags
    }
}