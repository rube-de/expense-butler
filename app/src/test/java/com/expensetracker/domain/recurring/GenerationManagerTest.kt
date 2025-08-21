package com.expensetracker.domain.recurring

import android.content.SharedPreferences
import com.expensetracker.data.model.RecurrenceFrequency
import com.expensetracker.data.model.RecurringExpense
import com.expensetracker.data.repository.ExpenseRepository
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class GenerationManagerTest {

    private lateinit var mockRepository: ExpenseRepository
    private lateinit var mockGenerator: RecurringExpenseGenerator
    private lateinit var mockPreferences: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    private lateinit var manager: GenerationManager

    @Before
    fun setup() {
        mockRepository = mockk(relaxed = true)
        mockGenerator = mockk(relaxed = true)
        mockPreferences = mockk(relaxed = true)
        mockEditor = mockk(relaxed = true)
        
        every { mockPreferences.edit() } returns mockEditor
        every { mockEditor.putLong(any(), any()) } returns mockEditor
        every { mockEditor.putString(any(), any()) } returns mockEditor
        every { mockEditor.apply() } just Runs
        
        manager = GenerationManager(mockRepository, mockGenerator, mockPreferences)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    // Once-per-day Generation Logic Tests

    @Test
    fun `should generate when no previous generation timestamp exists`() = runTest {
        every { mockPreferences.getLong(GenerationManager.LAST_GENERATION_KEY, 0L) } returns 0L
        coEvery { mockGenerator.generateDueExpenses(any()) } just Runs

        val result = manager.generateIfNeeded()

        assertTrue("Should generate when no previous timestamp", result.hasGenerated)
        coVerify(exactly = 1) { mockGenerator.generateDueExpenses(any()) }
        verify { mockEditor.putLong(GenerationManager.LAST_GENERATION_KEY, any()) }
        verify { mockEditor.apply() }
    }

    @Test
    fun `should generate when last generation was more than 24 hours ago`() = runTest {
        val now = System.currentTimeMillis()
        val yesterdayPlus = now - (25 * 60 * 60 * 1000L) // 25 hours ago
        
        every { mockPreferences.getLong(GenerationManager.LAST_GENERATION_KEY, 0L) } returns yesterdayPlus
        coEvery { mockGenerator.generateDueExpenses(any()) } just Runs

        val result = manager.generateIfNeeded()

        assertTrue("Should generate when more than 24 hours passed", result.hasGenerated)
        coVerify(exactly = 1) { mockGenerator.generateDueExpenses(any()) }
        verify { mockEditor.putLong(GenerationManager.LAST_GENERATION_KEY, any()) }
    }

    @Test
    fun `should not generate when last generation was less than 24 hours ago`() = runTest {
        val now = System.currentTimeMillis()
        val recentTime = now - (2 * 60 * 60 * 1000L) // 2 hours ago
        
        every { mockPreferences.getLong(GenerationManager.LAST_GENERATION_KEY, 0L) } returns recentTime
        coEvery { mockGenerator.generateDueExpenses(any()) } just Runs

        val result = manager.generateIfNeeded()

        assertFalse("Should not generate when less than 24 hours passed", result.hasGenerated)
        coVerify(exactly = 0) { mockGenerator.generateDueExpenses(any()) }
        verify(exactly = 0) { mockEditor.putLong(any(), any()) }
    }

    @Test
    fun `should generate when last generation was exactly 24 hours ago`() = runTest {
        val now = System.currentTimeMillis()
        val exactlyOneDayAgo = now - (24 * 60 * 60 * 1000L)
        
        every { mockPreferences.getLong(GenerationManager.LAST_GENERATION_KEY, 0L) } returns exactlyOneDayAgo
        coEvery { mockGenerator.generateDueExpenses(any()) } just Runs
        
        val result = manager.generateIfNeeded()

        assertTrue("Should generate when exactly 24 hours passed", result.hasGenerated)
        coVerify(exactly = 1) { mockGenerator.generateDueExpenses(any()) }
        verify { mockEditor.putLong(GenerationManager.LAST_GENERATION_KEY, any()) }
    }

    @Test
    fun `should use current date for generation`() = runTest {
        val today = LocalDate.now()
        every { mockPreferences.getLong(GenerationManager.LAST_GENERATION_KEY, 0L) } returns 0L
        coEvery { mockGenerator.generateDueExpenses(any()) } just Runs

        manager.generateIfNeeded()

        coVerify { mockGenerator.generateDueExpenses(today) }
    }

    // Manual Sync Tests

    @Test
    fun `should always generate when manual sync is called`() = runTest {
        val recentTime = System.currentTimeMillis() - (1 * 60 * 60 * 1000L) // 1 hour ago
        every { mockPreferences.getLong(GenerationManager.LAST_GENERATION_KEY, 0L) } returns recentTime
        coEvery { mockGenerator.generateDueExpenses(any()) } just Runs

        val result = manager.generateManually()

        assertTrue("Manual sync should always generate", result.hasGenerated)
        coVerify(exactly = 1) { mockGenerator.generateDueExpenses(any()) }
        verify { mockEditor.putLong(GenerationManager.LAST_GENERATION_KEY, any()) }
    }

    @Test
    fun `should update timestamp after manual generation`() = runTest {
        val beforeTime = System.currentTimeMillis()
        every { mockPreferences.getLong(GenerationManager.LAST_GENERATION_KEY, 0L) } returns 0L
        coEvery { mockGenerator.generateDueExpenses(any()) } just Runs

        manager.generateManually()

        val capturedTimestamp = slot<Long>()
        verify { mockEditor.putLong(GenerationManager.LAST_GENERATION_KEY, capture(capturedTimestamp)) }
        
        val afterTime = System.currentTimeMillis()
        assertTrue("Timestamp should be recent", capturedTimestamp.captured >= beforeTime)
        assertTrue("Timestamp should be recent", capturedTimestamp.captured <= afterTime)
    }

    @Test
    fun `should return correct generation count for manual sync`() = runTest {
        coEvery { mockGenerator.generateDueExpenses(any()) } just Runs
        every { mockPreferences.getLong(GenerationManager.LAST_GENERATION_KEY, 0L) } returns 0L

        val result = manager.generateManually()

        assertTrue("Manual sync should indicate generation occurred", result.hasGenerated)
        assertEquals("Manual generation message", "Recurring expenses updated", result.message)
    }

    // Error Handling Tests

    @Test
    fun `should handle generation exceptions gracefully`() = runTest {
        every { mockPreferences.getLong(GenerationManager.LAST_GENERATION_KEY, 0L) } returns 0L
        coEvery { mockGenerator.generateDueExpenses(any()) } throws Exception("Generation failed")

        val result = manager.generateIfNeeded()

        assertFalse("Should return false when generation fails", result.hasGenerated)
        assertTrue("Should contain error information", result.message.contains("error") || result.message.isEmpty())
        // Timestamp should not be updated on failure
        verify(exactly = 0) { mockEditor.putLong(any(), any()) }
    }

    @Test
    fun `should handle SharedPreferences exceptions gracefully`() = runTest {
        every { mockPreferences.getLong(GenerationManager.LAST_GENERATION_KEY, 0L) } throws Exception("Preferences error")
        coEvery { mockGenerator.generateDueExpenses(any()) } just Runs

        val result = manager.generateIfNeeded()

        // Should assume no previous generation and proceed
        assertTrue("Should generate when preferences access fails", result.hasGenerated)
        coVerify(exactly = 1) { mockGenerator.generateDueExpenses(any()) }
    }

    @Test
    fun `should not crash when editor apply fails`() = runTest {
        every { mockPreferences.getLong(GenerationManager.LAST_GENERATION_KEY, 0L) } returns 0L
        every { mockEditor.apply() } throws Exception("Apply failed")
        coEvery { mockGenerator.generateDueExpenses(any()) } just Runs

        // Should not throw exception
        assertDoesNotThrow {
            manager.generateIfNeeded()
        }
    }

    // Last Generation Timestamp Tests

    @Test
    fun `should return correct last generation time when exists`() = runTest {
        val expectedTime = System.currentTimeMillis() - (5 * 60 * 60 * 1000L) // 5 hours ago
        every { mockPreferences.getLong(GenerationManager.LAST_GENERATION_KEY, 0L) } returns expectedTime

        val lastGeneration = manager.getLastGenerationTime()

        assertEquals("Should return stored timestamp", expectedTime, lastGeneration)
    }

    @Test
    fun `should return zero when no previous generation exists`() = runTest {
        every { mockPreferences.getLong(GenerationManager.LAST_GENERATION_KEY, 0L) } returns 0L

        val lastGeneration = manager.getLastGenerationTime()

        assertEquals("Should return zero for no previous generation", 0L, lastGeneration)
    }

    @Test
    fun `should return time until next generation when within 24 hours`() = runTest {
        val twoHoursAgo = System.currentTimeMillis() - (2 * 60 * 60 * 1000L)
        every { mockPreferences.getLong(GenerationManager.LAST_GENERATION_KEY, 0L) } returns twoHoursAgo

        val timeUntilNext = manager.getTimeUntilNextGeneration()

        val expectedTime = 22 * 60 * 60 * 1000L // ~22 hours remaining
        val tolerance = 5 * 60 * 1000L // 5 minute tolerance
        assertTrue("Time until next should be around 22 hours", 
            Math.abs(timeUntilNext - expectedTime) < tolerance)
    }

    @Test
    fun `should return zero time until next generation when due`() = runTest {
        val twentyFiveHoursAgo = System.currentTimeMillis() - (25 * 60 * 60 * 1000L)
        every { mockPreferences.getLong(GenerationManager.LAST_GENERATION_KEY, 0L) } returns twentyFiveHoursAgo

        val timeUntilNext = manager.getTimeUntilNextGeneration()

        assertEquals("Should return zero when generation is due", 0L, timeUntilNext)
    }

    // Generation Result Tests

    @Test
    fun `should provide informative generation result when successful`() = runTest {
        every { mockPreferences.getLong(GenerationManager.LAST_GENERATION_KEY, 0L) } returns 0L
        coEvery { mockGenerator.generateDueExpenses(any()) } just Runs

        val result = manager.generateIfNeeded()

        assertTrue("Should indicate generation occurred", result.hasGenerated)
        assertEquals("Should provide user-friendly message", "Recurring expenses updated", result.message)
        assertTrue("Timestamp should be set", result.timestamp > 0)
    }

    @Test
    fun `should provide informative result when no generation needed`() = runTest {
        val recentTime = System.currentTimeMillis() - (1 * 60 * 60 * 1000L) // 1 hour ago
        every { mockPreferences.getLong(GenerationManager.LAST_GENERATION_KEY, 0L) } returns recentTime

        val result = manager.generateIfNeeded()

        assertFalse("Should indicate no generation occurred", result.hasGenerated)
        assertTrue("Should provide informative message", result.message.contains("up to date") || result.message.isEmpty())
    }

    // Helper functions

    private fun assertDoesNotThrow(block: suspend () -> Unit) {
        try {
            runTest { block() }
        } catch (e: Exception) {
            fail("Expected no exception but got: ${e.message}")
        }
    }
}