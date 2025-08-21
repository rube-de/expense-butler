package com.expensetracker.domain.recurring

import com.expensetracker.data.model.RecurrenceFrequency
import com.expensetracker.data.model.RecurringExpense
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles scheduling and calculation logic for recurring expenses.
 * Provides methods to calculate future occurrences and determine if expenses should be generated.
 */
@Singleton
class RecurringExpenseScheduler @Inject constructor() {

    companion object {
        private const val MAX_OCCURRENCES = 1000 // Prevent memory issues with very large ranges
    }

    /**
     * Calculates all occurrence dates for a recurring expense within a given range.
     * 
     * @param recurring The recurring expense to calculate occurrences for
     * @param from Start date for calculation (inclusive)
     * @param until End date for calculation (inclusive)
     * @return List of dates when the expense should occur, limited to MAX_OCCURRENCES
     */
    fun calculateNextOccurrences(
        recurring: RecurringExpense,
        from: LocalDate,
        until: LocalDate
    ): List<LocalDate> {
        if (from.isAfter(until) || !recurring.isActive) {
            return emptyList()
        }

        val effectiveStartDate = maxOf(from, recurring.startDate)
        val effectiveEndDate = if (recurring.endDate != null) {
            minOf(until, recurring.endDate)
        } else {
            until
        }

        if (effectiveStartDate.isAfter(effectiveEndDate)) {
            return emptyList()
        }

        val occurrences = mutableListOf<LocalDate>()
        var currentDate = effectiveStartDate

        while (currentDate <= effectiveEndDate && occurrences.size < MAX_OCCURRENCES) {
            if (isValidOccurrenceDate(recurring, currentDate)) {
                occurrences.add(currentDate)
            }
            currentDate = calculateNextDateForFrequency(recurring, currentDate)
        }

        return occurrences
    }

    /**
     * Determines if an expense should be generated on a specific date.
     * Considers the recurring schedule, active status, and previous generation.
     * 
     * @param recurring The recurring expense to check
     * @param date The date to check
     * @return true if the expense should be generated on this date
     */
    fun shouldGenerateOn(recurring: RecurringExpense, date: LocalDate): Boolean {
        if (!recurring.isActive) return false
        if (date.isBefore(recurring.startDate)) return false
        if (recurring.endDate != null && date.isAfter(recurring.endDate)) return false
        if (recurring.lastGenerated != null && !date.isAfter(recurring.lastGenerated)) return false

        return isValidOccurrenceDate(recurring, date)
    }

    /**
     * Gets the next occurrence date after a given date.
     * 
     * @param recurring The recurring expense
     * @param afterDate The date after which to find the next occurrence
     * @return The next occurrence date, or null if no more occurrences
     */
    fun getNextOccurrence(recurring: RecurringExpense, afterDate: LocalDate): LocalDate? {
        if (!recurring.isActive) return null

        // For weekly and other patterns, we need to find the next valid occurrence
        // not just add the frequency to the afterDate
        var candidateDate = afterDate.plusDays(1)
        
        // Search for the next valid occurrence (with reasonable limit)
        var attempts = 0
        while (attempts < 366) { // Max one year search
            if (isValidOccurrenceDate(recurring, candidateDate)) {
                if (recurring.endDate == null || !candidateDate.isAfter(recurring.endDate)) {
                    return candidateDate
                }
                return null
            }
            candidateDate = candidateDate.plusDays(1)
            attempts++
        }

        return null
    }

    /**
     * Checks if a given date is a valid occurrence for the recurring expense based on its frequency.
     */
    private fun isValidOccurrenceDate(recurring: RecurringExpense, date: LocalDate): Boolean {
        return when (recurring.frequency) {
            RecurrenceFrequency.DAILY -> {
                // Every day from start date
                true
            }
            RecurrenceFrequency.WEEKLY -> {
                // Same day of week as start date
                date.dayOfWeek == recurring.startDate.dayOfWeek
            }
            RecurrenceFrequency.MONTHLY -> {
                // Same day of month as start date, handling month-end intelligently
                isValidMonthlyDate(recurring.startDate, date)
            }
            RecurrenceFrequency.YEARLY -> {
                // Same month and day as start date, handling leap year
                isValidYearlyDate(recurring.startDate, date)
            }
        }
    }

    /**
     * Calculates the next date based on the frequency.
     */
    private fun calculateNextDateForFrequency(recurring: RecurringExpense, currentDate: LocalDate): LocalDate {
        return when (recurring.frequency) {
            RecurrenceFrequency.DAILY -> currentDate.plusDays(1)
            RecurrenceFrequency.WEEKLY -> currentDate.plusWeeks(1)
            RecurrenceFrequency.MONTHLY -> calculateNextMonthlyDate(recurring.startDate, currentDate)
            RecurrenceFrequency.YEARLY -> calculateNextYearlyDate(recurring.startDate, currentDate)
        }
    }

    /**
     * Validates if a date is correct for monthly frequency.
     * Handles month-end dates intelligently (e.g., 31st becomes last day of month).
     */
    private fun isValidMonthlyDate(startDate: LocalDate, checkDate: LocalDate): Boolean {
        val startDay = startDate.dayOfMonth
        val checkDay = checkDate.dayOfMonth
        val lastDayOfCheckMonth = checkDate.lengthOfMonth()

        return if (startDay <= lastDayOfCheckMonth) {
            // Normal case: start day exists in check month
            checkDay == startDay
        } else {
            // Month-end case: start day doesn't exist in check month, use last day
            checkDay == lastDayOfCheckMonth
        }
    }

    /**
     * Validates if a date is correct for yearly frequency.
     * Handles leap year (Feb 29) intelligently.
     */
    private fun isValidYearlyDate(startDate: LocalDate, checkDate: LocalDate): Boolean {
        val startMonth = startDate.month
        val startDay = startDate.dayOfMonth
        val checkMonth = checkDate.month
        val checkDay = checkDate.dayOfMonth

        if (startMonth != checkMonth) return false

        // Handle February 29th for non-leap years
        if (startDate.month.value == 2 && startDay == 29) {
            if (checkDate.isLeapYear) {
                return checkDay == 29
            } else {
                return checkDay == 28 // Use Feb 28 for non-leap years
            }
        }

        return checkDay == startDay
    }

    /**
     * Calculates the next monthly occurrence date, handling month-end cases.
     */
    private fun calculateNextMonthlyDate(startDate: LocalDate, currentDate: LocalDate): LocalDate {
        val startDay = startDate.dayOfMonth
        val nextMonth = currentDate.plusMonths(1)
        val lastDayOfNextMonth = nextMonth.lengthOfMonth()

        return if (startDay <= lastDayOfNextMonth) {
            nextMonth.withDayOfMonth(startDay)
        } else {
            // Use last day of month if start day doesn't exist
            nextMonth.withDayOfMonth(lastDayOfNextMonth)
        }
    }

    /**
     * Calculates the next yearly occurrence date, handling leap year cases.
     */
    private fun calculateNextYearlyDate(startDate: LocalDate, currentDate: LocalDate): LocalDate {
        val nextYear = currentDate.plusYears(1)
        
        // Handle February 29th case
        if (startDate.month.value == 2 && startDate.dayOfMonth == 29) {
            return if (nextYear.isLeapYear) {
                nextYear.withMonth(2).withDayOfMonth(29)
            } else {
                nextYear.withMonth(2).withDayOfMonth(28)
            }
        }

        return nextYear.withMonth(startDate.month.value).withDayOfMonth(startDate.dayOfMonth)
    }
}