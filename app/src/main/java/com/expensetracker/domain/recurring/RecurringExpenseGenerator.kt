package com.expensetracker.domain.recurring

import com.expensetracker.data.model.Expense
import com.expensetracker.data.model.RecurringExpense
import com.expensetracker.data.repository.ExpenseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles generation of actual expenses from recurring expense templates.
 * Provides both immediate generation for due expenses and forecast preview functionality.
 */
@Singleton
class RecurringExpenseGenerator @Inject constructor(
    private val repository: ExpenseRepository,
    private val scheduler: RecurringExpenseScheduler
) {

    companion object {
        private const val MAX_RETROACTIVE_DAYS = 30 // Limit retroactive generation to prevent abuse
    }

    /**
     * Generates all due expenses from recurring templates up to the specified date.
     * This is the main method called on app launch to create missed expenses.
     * 
     * @param until Generate expenses up to this date (inclusive), defaults to today
     */
    suspend fun generateDueExpenses(until: LocalDate = LocalDate.now()) {
        return withContext(Dispatchers.IO) {
            try {
                val recurringExpenses = repository.getRecurringExpensesToGenerate(until)
                
                for (recurringExpense in recurringExpenses) {
                    generateExpensesForRecurring(recurringExpense, until)
                }
            } catch (e: Exception) {
                // Log error but don't throw to prevent app crashes
                // In production, this should be logged to crash reporting service
                println("Error generating due expenses: ${e.message}")
            }
        }
    }

    /**
     * Generates a forecast of future expenses without saving them to the database.
     * Used for preview functionality in the UI.
     * 
     * @param recurringExpense The recurring expense template
     * @param period The forecast period (next month, rest of year, etc.)
     * @param fromDate Start date for forecast, defaults to today
     * @param toDate End date for custom period
     * @return List of forecasted expenses for preview
     */
    suspend fun generateForecast(
        recurringExpense: RecurringExpense,
        period: ForecastPeriod,
        fromDate: LocalDate = LocalDate.now(),
        toDate: LocalDate? = null
    ): List<ForecastedExpense> {
        return withContext(Dispatchers.Default) {
            val endDate = when (period) {
                ForecastPeriod.NEXT_MONTH -> fromDate.plusMonths(1)
                ForecastPeriod.REST_OF_YEAR -> LocalDate.of(fromDate.year, 12, 31)
                ForecastPeriod.CUSTOM -> toDate ?: fromDate.plusMonths(1)
            }

            // Validate date range
            if (endDate.isBefore(fromDate)) {
                return@withContext emptyList()
            }

            val occurrences = scheduler.calculateNextOccurrences(
                recurring = recurringExpense,
                from = fromDate,
                until = endDate
            )

            occurrences.map { date ->
                ForecastedExpense(
                    amount = recurringExpense.amount,
                    currency = recurringExpense.currency,
                    description = recurringExpense.description,
                    date = date,
                    categoryId = recurringExpense.categoryId,
                    tags = recurringExpense.tags,
                    sourceRecurringExpenseId = recurringExpense.id
                )
            }
        }
    }

    /**
     * Generates expenses for a specific recurring expense template.
     * Handles retroactive generation with reasonable limits.
     */
    private suspend fun generateExpensesForRecurring(
        recurringExpense: RecurringExpense,
        until: LocalDate
    ) {
        val startDate = calculateGenerationStartDate(recurringExpense, until)
        
        // Generate expenses for each due date
        var currentDate = startDate
        while (currentDate <= until) {
            if (scheduler.shouldGenerateOn(recurringExpense, currentDate)) {
                val result = generateSingleExpense(recurringExpense, currentDate)
                if (result.isSuccess) {
                    // Update lastGenerated to this date
                    repository.updateRecurringExpenseLastGenerated(recurringExpense.id, currentDate)
                } else {
                    // Log error but continue with other dates
                    println("Failed to generate expense for ${recurringExpense.description} on $currentDate: ${result.exceptionOrNull()?.message}")
                }
            }
            currentDate = currentDate.plusDays(1)
        }
    }

    /**
     * Calculates the appropriate start date for generation, considering retroactive limits.
     */
    private fun calculateGenerationStartDate(
        recurringExpense: RecurringExpense,
        until: LocalDate
    ): LocalDate {
        val lastGenerated = recurringExpense.lastGenerated
        val startDate = recurringExpense.startDate
        
        val candidateStartDate = when {
            lastGenerated != null -> lastGenerated.plusDays(1)
            else -> startDate
        }

        // Limit retroactive generation to prevent generating too many old expenses
        val earliestAllowed = until.minusDays(MAX_RETROACTIVE_DAYS.toLong())
        
        return maxOf(candidateStartDate, earliestAllowed, startDate)
    }

    /**
     * Generates a single expense from a recurring template for a specific date.
     */
    private suspend fun generateSingleExpense(
        recurringExpense: RecurringExpense,
        date: LocalDate
    ): Result<Long> {
        val expense = Expense(
            amount = recurringExpense.amount,
            currency = recurringExpense.currency,
            description = recurringExpense.description,
            categoryId = recurringExpense.categoryId,
            tags = recurringExpense.tags,
            date = date.atTime(12, 0), // Set to noon to avoid timezone issues
            recurringExpenseId = recurringExpense.id
        )

        return repository.insertExpense(expense)
    }
}