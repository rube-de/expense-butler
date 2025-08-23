package com.expensetracker.di

import android.content.Context
import androidx.room.Room
import com.expensetracker.data.database.AppDatabase
import com.expensetracker.data.dao.CategoryDao
import com.expensetracker.data.dao.ExpenseDao
import com.expensetracker.data.dao.RecurringExpenseDao
import com.expensetracker.data.repository.ExpenseRepository
import com.expensetracker.data.repository.ExpenseRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

/**
 * Test Hilt module that provides test versions of dependencies.
 * Uses in-memory database for testing.
 */
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [DatabaseModule::class, RepositoryModule::class]
)
object TestModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
    }

    @Provides
    @Singleton
    fun provideExpenseDao(database: AppDatabase): ExpenseDao {
        return database.expenseDao()
    }

    @Provides
    @Singleton
    fun provideCategoryDao(database: AppDatabase): CategoryDao {
        return database.categoryDao()
    }

    @Provides
    @Singleton
    fun provideRecurringExpenseDao(database: AppDatabase): RecurringExpenseDao {
        return database.recurringExpenseDao()
    }

    @Provides
    @Singleton
    fun provideExpenseRepository(
        expenseDao: ExpenseDao,
        categoryDao: CategoryDao,
        recurringExpenseDao: RecurringExpenseDao
    ): ExpenseRepository {
        return ExpenseRepositoryImpl(
            expenseDao = expenseDao,
            categoryDao = categoryDao,
            recurringExpenseDao = recurringExpenseDao
        )
    }
}