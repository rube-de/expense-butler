package com.expensetracker.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {
    
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Add indices on frequently queried columns for better performance
            database.execSQL("CREATE INDEX IF NOT EXISTS index_expenses_date ON expenses(date)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_expenses_categoryId ON expenses(categoryId)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_expenses_createdAt ON expenses(createdAt)")
            
            // Add index on categories table for better lookup performance
            database.execSQL("CREATE INDEX IF NOT EXISTS index_categories_name ON categories(name)")
            
            // Add index on recurring expenses for scheduled processing
            database.execSQL("CREATE INDEX IF NOT EXISTS index_recurring_expenses_startDate ON recurring_expenses(startDate)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_recurring_expenses_isActive ON recurring_expenses(isActive)")
        }
    }
    
    // Future migrations will be added here
    // Example: val MIGRATION_2_3 = object : Migration(2, 3) { ... }
    
    val ALL_MIGRATIONS = arrayOf(
        MIGRATION_1_2
        // Future migrations will be added to this array
    )
}