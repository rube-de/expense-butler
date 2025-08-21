package com.expensetracker.ui.util

import android.util.Log
import androidx.compose.runtime.*

/**
 * Performance utilities for debugging Compose recompositions and optimizations.
 * Only active in debug builds to avoid performance impact in production.
 */

/**
 * Logs recomposition count for debugging performance issues.
 * Use this to track how often composables are recomposing.
 * 
 * @param tag Unique identifier for the composable being tracked
 */
@Composable
inline fun LogCompositions(tag: String) {
    // Log recompositions for debugging performance
    val count = remember { mutableStateOf(0) }
    SideEffect { 
        count.value++
        Log.d("Recomposition", "$tag: ${count.value}")
    }
}

/**
 * Measures and logs the execution time of expensive calculations.
 * Only active in debug builds.
 * 
 * @param tag Identifier for the operation being measured
 * @param block The operation to measure
 * @return Result of the block execution
 */
inline fun <T> measureAndLog(tag: String, block: () -> T): T {
    val startTime = System.currentTimeMillis()
    val result = block()
    val endTime = System.currentTimeMillis()
    Log.d("Performance", "$tag took ${endTime - startTime}ms")
    return result
}

/**
 * Extension function to create a stable callback wrapper that doesn't cause recompositions.
 * Useful for callbacks passed to child composables.
 * 
 * @param keys Dependencies that should trigger callback recreation
 * @param block The callback function
 * @return Stable callback that only changes when keys change
 */
@Composable
inline fun <T> stableCallback(
    vararg keys: Any?,
    crossinline block: (T) -> Unit
): (T) -> Unit {
    return remember(*keys) { { value: T -> block(value) } }
}

/**
 * Creates a stable callback for no-parameter functions.
 */
@Composable
inline fun stableCallback(
    vararg keys: Any?,
    crossinline block: () -> Unit
): () -> Unit {
    return remember(*keys) { { block() } }
}

/**
 * Marker annotation for classes that should be considered stable by Compose.
 * Use this for data classes that are effectively immutable.
 */
@Stable
annotation class ComposeStable