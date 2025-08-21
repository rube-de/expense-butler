package com.expensetracker.domain.recurring

import com.expensetracker.data.model.Expense
import com.expensetracker.data.model.RecurringExpense
import com.expensetracker.data.repository.ExpenseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles modification of recurring expenses with smart handling of past generated expenses.
 * Provides options to update only future occurrences or include past expenses within limits.
 */
@Singleton
class RecurringExpenseModificationHandler @Inject constructor(
    private val repository: ExpenseRepository,
    private val generator: RecurringExpenseGenerator
) {

    companion object {
        private const val MAX_PAST_MODIFICATION_DAYS = 30L // Limit past modifications to 30 days
    }

    /**
     * Updates a recurring expense with optional modification of past generated expenses.
     * 
     * @param updatedExpense The updated recurring expense data
     * @param includePast Whether to also update past generated expenses (within 30-day limit)
     * @return Result indicating success or failure
     */
    suspend fun updateRecurringExpense(
        updatedExpense: RecurringExpense,
        includePast: Boolean
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Validate modification constraints
                validateModificationConstraints(updatedExpense)
                
                // Update the recurring expense record
                val updateResult = repository.updateRecurringExpense(updatedExpense)
                if (updateResult.isFailure) {
                    return@withContext updateResult
                }
                
                // If includePast is true, update past generated expenses
                if (includePast) {
                    updatePastGeneratedExpenses(updatedExpense)
                }
                
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Deletes a recurring expense and cleans up all associated generated expenses.
     * 
     * @param recurringExpenseId The ID of the recurring expense to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteRecurringExpenseWithCleanup(recurringExpenseId: Long): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Get all generated expenses for this recurring expense
                val generatedExpenses = repository.getExpensesByRecurringExpenseId(recurringExpenseId).first()
                
                // Delete all generated expenses (continue even if some fail)
                for (expense in generatedExpenses) {
                    try {
                        repository.deleteExpense(expense.id)
                    } catch (e: Exception) {
                        println("Failed to delete generated expense ${expense.id}: ${e.message}")
                        // Continue with other deletions
                    }
                }
                
                // Delete the recurring expense itself
                val deleteResult = repository.deleteRecurringExpense(recurringExpenseId)
                deleteResult
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Validates constraints for modifying a recurring expense.
     * Throws exception if modification would create invalid state.
     */
    private fun validateModificationConstraints(updatedExpense: RecurringExpense) {
        // Check if start date is in the future but expenses have already been generated
        if (updatedExpense.startDate.isAfter(LocalDate.now()) && 
            updatedExpense.lastGenerated != null) {
            throw IllegalArgumentException(
                "Validation error: Cannot set start date in future for recurring expense that has already generated expenses"
            )
        }
        
        // Additional validation can be added here as needed
    }

    /**
     * Updates past generated expenses to match the updated recurring expense.
     * Only updates expenses within the allowed time window (30 days).
     */
    private suspend fun updatePastGeneratedExpenses(updatedExpense: RecurringExpense) {
        try {
            val generatedExpenses = repository.getExpensesByRecurringExpenseId(updatedExpense.id).first()
            val cutoffDate = LocalDate.now().minusDays(MAX_PAST_MODIFICATION_DAYS)
            
            for (expense in generatedExpenses) {
                // Only update expenses within the allowed time window
                if (expense.date.toLocalDate().isAfter(cutoffDate)) {
                    try {
                        val updatedExpenseRecord = expense.copy(
                            amount = updatedExpense.amount,
                            currency = updatedExpense.currency,
                            description = updatedExpense.description,
                            categoryId = updatedExpense.categoryId,
                            tags = updatedExpense.tags,
                            updatedAt = LocalDateTime.now()
                            // Keep original createdAt and date
                        )
                        
                        repository.updateExpense(updatedExpenseRecord)
                    } catch (e: Exception) {
                        println("Failed to update generated expense ${expense.id}: ${e.message}")
                        // Continue with other updates
                    }
                }
            }
        } catch (e: Exception) {
            println("Failed to fetch past expenses for modification: ${e.message}")
            // Don't fail the entire operation if past expense updates fail
        }
    }
}