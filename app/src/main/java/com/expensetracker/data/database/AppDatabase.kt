package com.expensetracker.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.expensetracker.data.converter.Converters
import com.expensetracker.data.dao.CategoryDao
import com.expensetracker.data.dao.ExpenseDao
import com.expensetracker.data.dao.RecurringExpenseDao
import com.expensetracker.data.model.Category
import com.expensetracker.data.model.Expense
import com.expensetracker.data.model.RecurringExpense

@Database(
    entities = [Expense::class, Category::class, RecurringExpense::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun expenseDao(): ExpenseDao
    abstract fun categoryDao(): CategoryDao
    abstract fun recurringExpenseDao(): RecurringExpenseDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        // Future migrations will be added here
        // val MIGRATION_1_2 = object : Migration(1, 2) {
        //     override fun migrate(database: SupportSQLiteDatabase) {
        //         // Migration logic here
        //     }
        // }
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "expense_tracker_database"
                )
                // .addMigrations(MIGRATION_1_2) // Add migrations as needed
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}