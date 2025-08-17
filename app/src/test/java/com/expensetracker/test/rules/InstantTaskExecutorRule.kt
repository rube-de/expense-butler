package com.expensetracker.test.rules

import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * JUnit Rule that swaps the background executor used by Architecture Components
 * with a different one which executes each task synchronously.
 * 
 * This is useful for testing ViewModels and LiveData objects,
 * ensuring that all background work happens synchronously in tests.
 * 
 * Usage:
 * ```
 * @get:Rule
 * val instantTaskExecutorRule = InstantTaskExecutorRule()
 * ```
 */
class InstantTaskExecutorRule : TestWatcher() {
    
    override fun starting(description: Description) {
        super.starting(description)
        ArchTaskExecutor.getInstance().setDelegate(object : TaskExecutor() {
            override fun executeOnDiskIO(runnable: Runnable) {
                runnable.run()
            }
            
            override fun postToMainThread(runnable: Runnable) {
                runnable.run()
            }
            
            override fun executeOnMainThread(runnable: Runnable) {
                runnable.run()
            }
            
            override fun isMainThread(): Boolean {
                return true
            }
        })
    }
    
    override fun finished(description: Description) {
        super.finished(description)
        ArchTaskExecutor.getInstance().setDelegate(null)
    }
}