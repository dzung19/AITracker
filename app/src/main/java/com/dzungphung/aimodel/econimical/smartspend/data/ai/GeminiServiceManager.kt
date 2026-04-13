package com.dzungphung.aimodel.econimical.smartspend.data.ai

import android.content.SharedPreferences
import android.util.Log
import com.dzungphung.aimodel.econimical.smartspend.data.local.Expense
import com.dzungphung.aimodel.econimical.smartspend.di.GeminiApiKey
import com.dzungphung.aimodel.econimical.smartspend.util.CurrencyFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton manager for GeminiService that handles tier switching.
 * Uses Hilt for dependency injection.
 */
@Singleton
class GeminiServiceManager @Inject constructor(
    @param:GeminiApiKey private val apiKey: String,
    private val prefs: SharedPreferences
) {
    companion object {
        private const val TAG = "GeminiServiceManager"
        private const val KEY_AI_TIER = "ai_tier"
        private const val KEY_UNLOCKED_TIERS = "unlocked_tiers"
    }
    
    // Current tier state
    private val _currentTier = MutableStateFlow(loadSavedTier())
    val currentTier: StateFlow<AiTier> = _currentTier.asStateFlow()
    
    // Unlocked tiers
    private val _unlockedTiers = MutableStateFlow(loadUnlockedTiers())
    val unlockedTiers: StateFlow<Set<AiTier>> = _unlockedTiers.asStateFlow()
    
    // Current GeminiService instance
    private var _geminiService: GeminiService? = null

    // Cache for AI Summaries
    private data class CacheEntry(val summary: String, val timestamp: Long)
    private val _summaryCache = mutableMapOf<String, CacheEntry>()
    // 7 Days in milliseconds
    private val CACHE_EXPIRATION_MS = 7 * 24 * 60 * 60 * 1000L
    
    init {
        // Initialize service with current tier
        createService(_currentTier.value)
    }
    
    /**
     * Check if the API key is configured
     */
    val isConfigured: Boolean
        get() = apiKey.isNotBlank()
    
    /**
     * Get the current GeminiService instance
     */
    fun getService(): GeminiService? = _geminiService
    
    /**
     * Select and switch to a new AI tier
     */
    fun selectTier(tier: AiTier): Boolean {
        if (tier !in _unlockedTiers.value) {
            Log.w(TAG, "Tier ${tier.name} is not unlocked")
            return false
        }
        
        if (tier == _currentTier.value) {
            return true // Already on this tier
        }
        
        _currentTier.value = tier
        saveTier(tier)
        createService(tier)
        
        Log.d(TAG, "Switched to ${tier.displayName} (${tier.modelName})")
        return true
    }
    
    /**
     * Sync unlocked tiers based on actual Google Play purchases.
     * Revokes access to previously unlocked tiers if they are no longer in purchasedSkus.
     */
    fun syncPurchasedTiers(purchasedSkus: Set<String>) {
        val validTiers = mutableSetOf(AiTier.BASIC)
        AiTier.entries.forEach { tier ->
            val skuId = tier.skuId
            if (skuId != null && purchasedSkus.contains(skuId)) {
                validTiers.add(tier)
            }
        }
        
        // If current tier is no longer unlocked, fallback to BASIC
        if (_currentTier.value !in validTiers) {
            _currentTier.value = AiTier.BASIC
            saveTier(AiTier.BASIC)
            createService(AiTier.BASIC)
        }
        
        _unlockedTiers.value = validTiers
        saveUnlockedTiers(validTiers)
        Log.d(TAG, "Synced unlocked tiers from IAP: ${validTiers.map { it.displayName }}")
    }
    
    /**
     * Unlock a tier (call after successful in-app purchase)
     */
    fun unlockTier(tier: AiTier) {
        val updated = _unlockedTiers.value + tier
        _unlockedTiers.value = updated
        saveUnlockedTiers(updated)
        Log.d(TAG, "Unlocked tier: ${tier.displayName}")
    }
    
    /**
     * Unlock all tiers (for testing or premium subscription)
     */
    fun unlockAllTiers() {
        val allTiers = AiTier.entries.toSet()
        _unlockedTiers.value = allTiers
        saveUnlockedTiers(allTiers)
        Log.d(TAG, "Unlocked all tiers")
    }
    
    /**
     * Parse a receipt using the current tier's model
     */
    suspend fun parseReceipt(rawText: String): ParsedExpense? {
        return _geminiService?.parseReceiptText(rawText)
    }

    /**
     * Analyze spending with caching strategy (7-day validity)
     */
    suspend fun analyzeSpending(expenses: List<Expense>, period: String, budget: Double? = null, forceRefresh: Boolean = false): String? {
        val currentTime = System.currentTimeMillis()
        val cached = _summaryCache[period]

        // Check cache validity
        if (!forceRefresh && cached != null) {
            val age = currentTime - cached.timestamp
            if (age < CACHE_EXPIRATION_MS) {
                Log.d(TAG, "Returning cached summary for $period (Age: ${age / 1000 / 60} min)")
                return cached.summary
            }
        }

        // Fetch fresh from AI
        val currencyFormat = CurrencyFormatter.getFormatter(prefs)
        val result = _geminiService?.analyzeSpending(expenses, period, budget, currencyFormat)
        
        // Update cache if successful
        if (result != null) {
            _summaryCache[period] = CacheEntry(result, currentTime)
            Log.d(TAG, "Cached new summary for $period")
        }
        
        return result
    }
    
    /**
     * Generate a chat response using the active Tier.
     */
    suspend fun chat(prompt: String): String? {
        // If service is missing (no API key), return null to trigger fallback
        return _geminiService?.chat(prompt)
    }

    // ==================== PRIVATE METHODS ====================
    
    private fun createService(tier: AiTier) {
        _geminiService = if (apiKey.isNotBlank()) {
            try {
                GeminiService(apiKey, tier)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create GeminiService", e)
                null
            }
        } else {
            Log.w(TAG, "API key not configured")
            null
        }
    }
    
    private fun loadSavedTier(): AiTier {
        val ordinal = prefs.getInt(KEY_AI_TIER, 0)
        return AiTier.fromOrdinal(ordinal)
    }
    
    private fun saveTier(tier: AiTier) {
        prefs.edit().putInt(KEY_AI_TIER, tier.ordinal).apply()
    }
    
    private fun loadUnlockedTiers(): Set<AiTier> {
        val savedSet = prefs.getStringSet(KEY_UNLOCKED_TIERS, null)
        return if (savedSet != null) {
            savedSet.mapNotNull { ordinalStr ->
                ordinalStr.toIntOrNull()?.let { AiTier.fromOrdinal(it) }
            }.toSet()
        } else {
            // Default: Unlock only the BASIC tier
            setOf(AiTier.BASIC)
        }
    }
    
    private fun saveUnlockedTiers(tiers: Set<AiTier>) {
        val ordinalSet = tiers.map { it.ordinal.toString() }.toSet()
        prefs.edit().putStringSet(KEY_UNLOCKED_TIERS, ordinalSet).apply()
    }
}
