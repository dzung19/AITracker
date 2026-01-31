package com.example.smartspend.data.ai

import android.content.SharedPreferences
import android.util.Log
import com.example.smartspend.di.GeminiApiKey
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
    @GeminiApiKey private val apiKey: String,
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
            // Default: Unlock ALL tiers for testing
            AiTier.entries.toSet()
        }
    }
    
    private fun saveUnlockedTiers(tiers: Set<AiTier>) {
        val ordinalSet = tiers.map { it.ordinal.toString() }.toSet()
        prefs.edit().putStringSet(KEY_UNLOCKED_TIERS, ordinalSet).apply()
    }
}
