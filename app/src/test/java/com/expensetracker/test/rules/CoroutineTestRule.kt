package com.expensetracker.test.rules

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * JUnit Rule for testing coroutines.
 * 
 * This rule configures the test dispatcher for coroutine testing,
 * allowing tests to control time and execute coroutines synchronously.
 * 
 * Usage:
 * ```
 * @get:Rule
 * val coroutineTestRule = CoroutineTestRule()
 * 
 * @Test
 * fun myTest() = coroutineTestRule.runTest {
 *     // Test code with coroutines
 * }
 * ```
 */
@ExperimentalCoroutinesApi
class CoroutineTestRule : TestWatcher() {
    
    val testDispatcher = StandardTestDispatcher()
    
    override fun starting(description: Description) {
        super.starting(description)
        Dispatchers.setMain(testDispatcher)
    }
    
    override fun finished(description: Description) {
        super.finished(description)
        Dispatchers.resetMain()
    }
    
    /**
     * Convenience function to run a test with the test scope.
     */
    fun runTest(
        testBody: suspend TestScope.() -> Unit
    ) = kotlinx.coroutines.test.runTest(testDispatcher) {
        testBody()
    }
    
    /**
     * Advances the test dispatcher's virtual time until all enqueued tasks are executed.
     */
    fun advanceUntilIdle() {
        testDispatcher.scheduler.advanceUntilIdle()
    }
    
    /**
     * Runs all currently pending tasks without advancing virtual time.
     */
    fun runCurrent() {
        testDispatcher.scheduler.runCurrent()
    }
    
    /**
     * Advances the test dispatcher's virtual time by the specified amount.
     */
    fun advanceTimeBy(delayMs: Long) {
        testDispatcher.scheduler.advanceTimeBy(delayMs)
    }
}