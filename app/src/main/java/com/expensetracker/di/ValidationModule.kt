package com.expensetracker.di

import com.expensetracker.domain.validation.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ValidationModule {
    
    @Provides
    @Singleton
    fun provideAmountValidator(): AmountValidator {
        return AmountValidator()
    }
    
    @Provides
    @Singleton
    fun provideDescriptionValidator(): DescriptionValidator {
        return DescriptionValidator()
    }
    
    @Provides
    @Singleton
    fun provideTagValidator(): TagValidator {
        return TagValidator()
    }
    
    @Provides
    @Singleton
    fun provideCategoryValidator(): CategoryValidator {
        return CategoryValidator()
    }
}