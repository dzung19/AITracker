package com.example.smartspend.di

import android.content.Context
import android.content.SharedPreferences
import com.example.smartspend.BuildConfig
import com.example.smartspend.data.local.AppDatabase
import com.example.smartspend.data.local.ExpenseDao
import com.example.smartspend.data.repository.ExpenseRepository
import com.example.smartspend.data.scanner.ReceiptScannerService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    private const val PREFS_NAME = "smartspend_prefs"
    
    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }
    
    @Provides
    @Singleton
    fun provideExpenseDao(database: AppDatabase): ExpenseDao {
        return database.expenseDao()
    }
    
    @Provides
    @Singleton
    fun provideExpenseRepository(expenseDao: ExpenseDao): ExpenseRepository {
        return ExpenseRepository(expenseDao)
    }
    
    @Provides
    @Singleton
    fun provideReceiptScannerService(): ReceiptScannerService {
        return ReceiptScannerService()
    }
    
    @Provides
    @Singleton
    @GeminiApiKey
    fun provideGeminiApiKey(): String {
        return BuildConfig.GEMINI_API_KEY
    }
}
