package com.expensetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import com.expensetracker.domain.recurring.GenerationManager
import com.expensetracker.domain.recurring.GenerationResult
import com.expensetracker.ui.navigation.ExpenseTrackerApp
import com.expensetracker.ui.theme.ExpenseTrackerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var generationManager: GenerationManager
    
    // Store generation result to pass to UI
    private val generationResult = mutableStateOf<GenerationResult?>(null)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Trigger recurring expense generation in background
        triggerRecurringExpenseGeneration()
        
        setContent {
            ExpenseTrackerTheme {
                // Main app entry point with navigation
                // Pass generation result to the app for UI feedback
                ExpenseTrackerApp(generationResult = generationResult.value)
            }
        }
    }
    
    private fun triggerRecurringExpenseGeneration() {
        // Launch in lifecycle scope to avoid blocking UI
        lifecycleScope.launch {
            try {
                // Generate recurring expenses if needed (once per day)
                val result = generationManager.generateIfNeeded()
                
                // Store result for UI feedback
                generationResult.value = result
                
                // Log for debugging (production would use proper logging)
                if (result.hasGenerated) {
                    println("MainActivity: Recurring expenses generated - ${result.message}")
                }
            } catch (e: Exception) {
                // Log error but don't crash the app
                println("MainActivity: Error generating recurring expenses - ${e.message}")
                
                // Set empty result to indicate no generation
                generationResult.value = GenerationResult(
                    hasGenerated = false,
                    message = "",
                    timestamp = 0L
                )
            }
        }
    }
}