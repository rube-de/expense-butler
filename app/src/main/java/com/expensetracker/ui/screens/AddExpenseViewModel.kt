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
    val errorMessage: String? = null,
    val amountError: String? = null,
    val descriptionError: String? = null,
    val categoryError: String? = null
)

@HiltViewModel
class AddExpenseViewModel @Inject constructor(
    private val repository: ExpenseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddExpenseUiState())
    val uiState: StateFlow<AddExpenseUiState> = _uiState.asStateFlow()

    init {
        initializeDefaultCategories()
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            try {
                // Load categories
                repository.getAllCategories().collect { categories ->
                    _uiState.update { it.copy(availableCategories = categories) }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(errorMessage = "Failed to load categories: ${e.message}")
                }
            }
        }

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

    fun saveExpense() {
        if (!validateForm()) {
            return
        }

        val currentState = _uiState.value
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
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
                            errorMessage = "Failed to save expense: ${result.exceptionOrNull()?.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to save expense: ${e.message}"
                    )
                }
            }
        }
    }

    fun resetForm() {
        _uiState.update {
            AddExpenseUiState(
                availableCategories = it.availableCategories,
                availableTags = it.availableTags
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun initializeDefaultCategories() {
        viewModelScope.launch {
            try {
                repository.initializeDefaultCategories()
            } catch (e: Exception) {
                // Log error but don't show to user as this is initialization
            }
        }
    }
}