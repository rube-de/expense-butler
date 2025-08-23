package com.expensetracker.test.utils

import kotlinx.coroutines.test.runTest
import org.junit.Assert.fail

/**
 * Test helper utilities for coroutine-based tests.
 */

/**
 * Asserts that a suspend function does not throw any exceptions.
 * 
 * This is a coroutine-aware version of JUnit's assertDoesNotThrow that handles
 * suspend functions properly. It's useful for testing that operations complete
 * without throwing exceptions in coroutine contexts.
 * 
 * @param block The suspend function to execute
 * @throws AssertionError if the block throws any exception
 * 
 * Example usage:
 * ```kotlin
 * @Test
 * fun `should not throw when operation succeeds`() = runTest {
 *     assertSuspendDoesNotThrow {
 *         myRepository.performOperation()
 *     }
 * }
 * ```
 */
fun assertSuspendDoesNotThrow(block: suspend () -> Unit) {
    try {
        runTest { block() }
    } catch (e: Exception) {
        fail("Expected no exception but got: ${e.message}")
    }
}