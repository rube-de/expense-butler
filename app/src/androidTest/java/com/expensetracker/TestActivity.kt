package com.expensetracker

import androidx.activity.ComponentActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * Test activity for isolated Compose UI testing.
 * This activity is used in instrumented tests to provide a proper Hilt-enabled
 * context for testing individual screens without navigation complexity.
 */
@AndroidEntryPoint
class TestActivity : ComponentActivity()