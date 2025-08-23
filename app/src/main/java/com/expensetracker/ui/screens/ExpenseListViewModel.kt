package com.expensetracker.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.data.model.Category
import com.expensetracker.data.model.Expense
import com.expensetracker.data.repository.ExpenseRepository
import com.expensetracker.domain.recurring.GenerationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class ExpenseListUiState(
    val expenses: List<Expense> = emptyList(),
    val categories: List<Category> = emptyList(),
    val availableTags: List<String> = emptyList(),
    val searchText: String = "",
    val selectedCategoryFilter: Category? = null,
    val selectedTags: List<String> = emptyList(),
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showFilters: Boolean = false,
    val showGenerationBanner: Boolean = false,
    val generationMessage: String? = null
)

@HiltViewModel
class ExpenseListViewModel @Inject constructor(
    private val repository: ExpenseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpenseListUiState())
    val uiState: StateFlow<ExpenseListUiState> = _uiState.asStateFlow()

    // Filter state
    private val searchTextFlow = MutableStateFlow("")
    private val categoryFilterFlow = MutableStateFlow<Category?>(null)
    private val tagFilterFlow = MutableStateFlow<List<String>>(emptyList())
    private val dateRangeFlow = MutableStateFlow<Pair<LocalDate?, LocalDate?>>(null to null)

    init {
        initializeDefaultCategories()
        loadInitialData()
        setupFilteredExpenses()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            try {
                // Load categories using collectLatest for proper Flow collection
                repository.getAllCategories().collectLatest { categories ->
                    _uiState.update { it.copy(categories = categories) }
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

    private fun setupFilteredExpenses() {
        viewModelScope.launch {
            combine(
                searchTextFlow,
                categoryFilterFlow,
                tagFilterFlow,
                dateRangeFlow
            ) { searchText, categoryFilter, tagFilter, dateRange ->
                FilterCriteria(
                    searchText = searchText.takeIf { it.isNotBlank() },
                    categoryId = categoryFilter?.id,
                    tags = tagFilter.takeIf { it.isNotEmpty() },
                    startDate = dateRange.first,
                    endDate = dateRange.second
                )
            }.collectLatest { criteria ->
                try {
                    _uiState.update { it.copy(isLoading = true) }
                    
                    val expensesFlow = when {
                        criteria.searchText != null && criteria.categoryId == null && 
                        criteria.tags == null && criteria.startDate == null -> {
                            repository.searchExpenses(criteria.searchText)
                        }
                        criteria.categoryId != null && criteria.searchText == null && 
                        criteria.tags == null && criteria.startDate == null -> {
                            repository.getExpensesByCategory(criteria.categoryId)
                        }
                        criteria.tags != null && criteria.searchText == null && 
                        criteria.categoryId == null && criteria.startDate == null -> {
                            repository.getExpensesByTags(criteria.tags)
                        }
                        criteria.startDate != null && criteria.endDate != null && 
                        criteria.searchText == null && criteria.categoryId == null && criteria.tags == null -> {
                            repository.getExpensesByDateRange(criteria.startDate, criteria.endDate)
                        }
                        criteria.hasAnyFilter() -> {
                            repository.getFilteredExpenses(
                                categoryId = criteria.categoryId,
                                startDate = criteria.startDate,
                                endDate = criteria.endDate,
                                searchText = criteria.searchText,
                                tags = criteria.tags
                            )
                        }
                        else -> {
                            repository.getAllExpenses()
                        }
                    }
                    
                    expensesFlow.collect { expenses ->
                        _uiState.update { 
                            it.copy(
                                expenses = expenses,
                                isLoading = false,
                                errorMessage = null
                            )
                        }
                    }
                } catch (e: Exception) {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            errorMessage = "Failed to load expenses: ${e.message}"
                        )
                    }
                }
            }
        }
    }

    fun updateSearchText(searchText: String) {
        searchTextFlow.value = searchText
        _uiState.update { it.copy(searchText = searchText) }
    }

    fun selectCategoryFilter(category: Category?) {
        categoryFilterFlow.value = category
        _uiState.update { it.copy(selectedCategoryFilter = category) }
    }

    fun updateTagFilter(tags: List<String>) {
        tagFilterFlow.value = tags
        _uiState.update { it.copy(selectedTags = tags) }
    }

    fun updateDateRange(startDate: LocalDate?, endDate: LocalDate?) {
        dateRangeFlow.value = startDate to endDate
        _uiState.update { 
            it.copy(
                startDate = startDate,
                endDate = endDate
            )
        }
    }

    fun clearFilters() {
        searchTextFlow.value = ""
        categoryFilterFlow.value = null
        tagFilterFlow.value = emptyList()
        dateRangeFlow.value = null to null
        
        _uiState.update { 
            it.copy(
                searchText = "",
                selectedCategoryFilter = null,
                selectedTags = emptyList(),
                startDate = null,
                endDate = null
            )
        }
    }

    fun toggleFilters() {
        _uiState.update { it.copy(showFilters = !it.showFilters) }
    }

    fun deleteExpense(expenseId: Long) {
        viewModelScope.launch {
            try {
                val result = repository.deleteExpense(expenseId)
                if (result.isFailure) {
                    _uiState.update { 
                        it.copy(errorMessage = "Failed to delete expense: ${result.exceptionOrNull()?.message}")
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(errorMessage = "Failed to delete expense: ${e.message}")
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
    
    // Generation Result Handling
    
    fun setGenerationResult(result: GenerationResult?) {
        if (result == null) {
            // No generation result, don't show banner
            _uiState.update { 
                it.copy(
                    showGenerationBanner = false,
                    generationMessage = null
                )
            }
            return
        }
        
        if (result.hasGenerated) {
            // Show banner with generation message
            _uiState.update { 
                it.copy(
                    showGenerationBanner = true,
                    generationMessage = result.message
                )
            }
            
            // Refresh expenses to show newly generated ones
            loadInitialData()
        } else {
            // No expenses were generated, don't show banner
            _uiState.update { 
                it.copy(
                    showGenerationBanner = false,
                    generationMessage = null
                )
            }
        }
    }
    
    fun dismissGenerationBanner() {
        _uiState.update { 
            it.copy(
                showGenerationBanner = false,
                generationMessage = null
            )
        }
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

    private data class FilterCriteria(
        val searchText: String? = null,
        val categoryId: Long? = null,
        val tags: List<String>? = null,
        val startDate: LocalDate? = null,
        val endDate: LocalDate? = null
    ) {
        fun hasAnyFilter(): Boolean {
            return searchText != null || categoryId != null || 
                   tags != null || startDate != null || endDate != null
        }
    }
}