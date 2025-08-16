package com.expensetracker.di

import android.content.Context
import androidx.room.Room
import com.expensetracker.data.dao.CategoryDao
import com.expensetracker.data.dao.ExpenseDao
import com.expensetracker.data.dao.RecurringExpenseDao
import com.expensetracker.data.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "expense_tracker_database"
        )
        .fallbackToDestructiveMigration() // For development - remove in production
        .build()
    }

    @Provides
    fun provideExpenseDao(database: AppDatabase): ExpenseDao {
        return database.expenseDao()
    }

    @Provides
    fun provideCategoryDao(database: AppDatabase): CategoryDao {
        return database.categoryDao()
    }

    @Provides
    fun provideRecurringExpenseDao(database: AppDatabase): RecurringExpenseDao {
        return database.recurringExpenseDao()
    }
}