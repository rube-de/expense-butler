package com.expensetracker.test.rules

import io.mockk.clearAllMocks
import io.mockk.unmockkAll
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * JUnit Rule for MockK that automatically clears all mocks after each test.
 * This ensures test isolation and prevents mock state from leaking between tests.
 * 
 * Usage:
 * ```
 * @get:Rule
 * val mockKRule = MockKRule()
 * 
 * @Test
 * fun myTest() {
 *     val mock = mockk<MyClass>()
 *     // Test code
 *     // Mocks are automatically cleared after test
 * }
 * ```
 */
class MockKRule : TestWatcher() {
    
    override fun finished(description: Description) {
        super.finished(description)
        // Clear all mocks after each test
        clearAllMocks()
        unmockkAll()
    }
}