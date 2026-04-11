package com.dzungphung.aimodel.econimical.smartspend.di

import android.content.Context
import android.content.SharedPreferences
import com.dzungphung.aimodel.econimical.smartspend.BuildConfig
import com.dzungphung.aimodel.econimical.smartspend.data.local.AppDatabase
import com.dzungphung.aimodel.econimical.smartspend.data.local.ExpenseDao
import com.dzungphung.aimodel.econimical.smartspend.data.repository.ExpenseRepository
import com.dzungphung.aimodel.econimical.smartspend.data.scanner.ReceiptScannerService
import com.daumo.ads.BillingManager
import com.dzungphung.aimodel.econimical.smartspend.data.ai.AiTier
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
    const val INSTALL_DATE_KEY = "app_install_date"
    
    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        // Store install date on first launch
        if (!prefs.contains(INSTALL_DATE_KEY)) {
            prefs.edit().putLong(INSTALL_DATE_KEY, System.currentTimeMillis()).apply()
        }
        
        return prefs
    }
    
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.Companion.getDatabase(context)
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
    fun provideBillingManager(@ApplicationContext context: Context): BillingManager {
        val aiTierSkus = AiTier.entries.mapNotNull { it.skuId }
        return BillingManager(
            context = context,
            productIds = aiTierSkus,
            onUserPurchasedRemoveAds = {
                // Future use
            }
        )
    }
    
    @Provides
    @Singleton
    @GeminiApiKey
    fun provideGeminiApiKey(): String {
        return BuildConfig.GEMINI_API_KEY
    }
}
