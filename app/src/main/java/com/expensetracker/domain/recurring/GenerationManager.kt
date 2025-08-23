package com.expensetracker.domain.recurring

import android.content.SharedPreferences
import com.expensetracker.data.repository.ExpenseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages automatic generation of recurring expenses with once-per-day logic.
 * Handles timing, user feedback, and error recovery for expense generation.
 */
@Singleton
class GenerationManager @Inject constructor(
    private val repository: ExpenseRepository,
    private val generator: RecurringExpenseGenerator,
    private val preferences: SharedPreferences
) {

    companion object {
        const val LAST_GENERATION_KEY = "last_generation_timestamp"
        private const val TWENTY_FOUR_HOURS_MS = 24 * 60 * 60 * 1000L
    }

    /**
     * Generates recurring expenses if 24 hours have passed since last generation.
     * This is the main method called on app launch.
     * 
     * @return GenerationResult indicating if generation occurred and user feedback
     */
    suspend fun generateIfNeeded(): GenerationResult {
        return withContext(Dispatchers.IO) {
            try {
                val lastGeneration = getLastGenerationTime()
                val currentTime = System.currentTimeMillis()
                
                if (shouldGenerate(lastGeneration, currentTime)) {
                    performGeneration(currentTime)
                } else {
                    GenerationResult(
                        hasGenerated = false,
                        message = "", // No message needed when up to date
                        timestamp = lastGeneration
                    )
                }
            } catch (e: Exception) {
                // Log error but don't show to user as per requirements
                println("GenerationManager error during generateIfNeeded: ${e.message}")
                
                // If preferences failed, assume no previous generation and try to generate
                if (isPreferencesError(e)) {
                    try {
                        performGeneration(System.currentTimeMillis())
                    } catch (genException: Exception) {
                        println("GenerationManager error during fallback generation: ${genException.message}")
                        GenerationResult(hasGenerated = false, message = "", timestamp = 0L)
                    }
                } else {
                    GenerationResult(hasGenerated = false, message = "", timestamp = 0L)
                }
            }
        }
    }

    /**
     * Manually triggers generation regardless of timing.
     * Used for user-initiated sync or debugging.
     * 
     * @return GenerationResult with user feedback
     */
    suspend fun generateManually(): GenerationResult {
        return withContext(Dispatchers.IO) {
            try {
                val currentTime = System.currentTimeMillis()
                performGeneration(currentTime)
            } catch (e: Exception) {
                println("GenerationManager error during manual generation: ${e.message}")
                GenerationResult(
                    hasGenerated = false,
                    message = "",
                    timestamp = getLastGenerationTime()
                )
            }
        }
    }

    /**
     * Gets the timestamp of the last successful generation.
     * 
     * @return Timestamp in milliseconds, or 0 if never generated
     */
    fun getLastGenerationTime(): Long {
        return try {
            preferences.getLong(LAST_GENERATION_KEY, 0L)
        } catch (e: Exception) {
            println("GenerationManager error accessing preferences: ${e.message}")
            0L
        }
    }

    /**
     * Calculates how much time remains until the next generation is allowed.
     * 
     * @return Milliseconds until next generation, or 0 if generation is due now
     */
    fun getTimeUntilNextGeneration(): Long {
        val lastGeneration = getLastGenerationTime()
        val currentTime = System.currentTimeMillis()
        val timeSinceLastGeneration = currentTime - lastGeneration
        val timeUntilNext = TWENTY_FOUR_HOURS_MS - timeSinceLastGeneration
        
        return maxOf(0L, timeUntilNext)
    }

    /**
     * Determines if generation should occur based on timing.
     */
    private fun shouldGenerate(lastGeneration: Long, currentTime: Long): Boolean {
        if (lastGeneration == 0L) return true // Never generated before
        
        val timeSinceLastGeneration = currentTime - lastGeneration
        return timeSinceLastGeneration >= TWENTY_FOUR_HOURS_MS
    }

    /**
     * Performs the actual generation and updates the timestamp.
     */
    private suspend fun performGeneration(currentTime: Long): GenerationResult {
        try {
            // Generate expenses for current date
            generator.generateDueExpenses(LocalDate.now())
            
            // Update timestamp after successful generation
            updateLastGenerationTime(currentTime)
            
            return GenerationResult(
                hasGenerated = true,
                message = "Recurring expenses updated",
                timestamp = currentTime
            )
        } catch (e: Exception) {
            println("GenerationManager error during expense generation: ${e.message}")
            throw e // Re-throw to be handled by caller
        }
    }

    /**
     * Updates the last generation timestamp in preferences.
     */
    private fun updateLastGenerationTime(timestamp: Long) {
        try {
            preferences.edit()
                .putLong(LAST_GENERATION_KEY, timestamp)
                .apply()
        } catch (e: Exception) {
            println("GenerationManager error updating timestamp: ${e.message}")
            // Don't throw - generation was successful, timestamp update is secondary
        }
    }

    /**
     * Checks if an exception is related to SharedPreferences access.
     */
    private fun isPreferencesError(exception: Exception): Boolean {
        return exception.message?.contains("preferences", ignoreCase = true) == true ||
               exception.javaClass.simpleName.contains("Preferences", ignoreCase = true)
    }
}

/**
 * Result of a generation attempt with user feedback information.
 */
data class GenerationResult(
    val hasGenerated: Boolean,
    val message: String,
    val timestamp: Long
)