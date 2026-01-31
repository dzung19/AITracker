package com.example.smartspend.ui

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.camera.core.ImageProxy
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartspend.BuildConfig
import com.example.smartspend.data.ai.AiTier
import com.example.smartspend.data.ai.GeminiService
import com.example.smartspend.data.local.AppDatabase
import com.example.smartspend.data.local.Expense
import com.example.smartspend.data.repository.ExpenseRepository
import com.example.smartspend.data.scanner.ReceiptScannerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import androidx.core.content.edit

class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    companion object {
        private const val PREFS_NAME = "smartspend_prefs"
        private const val KEY_AI_TIER = "ai_tier"
        private const val KEY_UNLOCKED_TIERS = "unlocked_tiers"
    }
    
    private val repository: ExpenseRepository
    private val receiptScanner = ReceiptScannerService()
    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    // API key is read from local.properties via BuildConfig (secure, not in git)
    private val apiKey: String? = try {
        val key = BuildConfig.GEMINI_API_KEY
        if (key.isNotBlank()) key else null
    } catch (e: Exception) {
        Log.e("MainViewModel", "Failed to get API key", e)
        null
    }
    
    val expenses: StateFlow<List<Expense>>
    val totalSpending: StateFlow<Double>
    
    // AI Tier state
    private val _currentAiTier = MutableStateFlow(loadSavedTier())
    val currentAiTier: StateFlow<AiTier> = _currentAiTier.asStateFlow()
    
    private val _unlockedTiers = MutableStateFlow(loadUnlockedTiers())
    val unlockedTiers: StateFlow<Set<AiTier>> = _unlockedTiers.asStateFlow()
    
    // Scanning state
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()
    
    private val _scannedTitle = MutableStateFlow<String?>(null)
    val scannedTitle: StateFlow<String?> = _scannedTitle.asStateFlow()
    
    private val _scannedAmount = MutableStateFlow<Double?>(null)
    val scannedAmount: StateFlow<Double?> = _scannedAmount.asStateFlow()
    
    private val _scannedCategory = MutableStateFlow<String?>(null)
    val scannedCategory: StateFlow<String?> = _scannedCategory.asStateFlow()
    
    private val _scanError = MutableStateFlow<String?>(null)
    val scanError: StateFlow<String?> = _scanError.asStateFlow()
    
    // Pending tier for when camera opens
    private var pendingTier: AiTier = AiTier.BASIC
    
    init {
        val database = AppDatabase.getDatabase(application)
        val expenseDao = database.expenseDao()
        repository = ExpenseRepository(expenseDao)
        
        expenses = repository.allExpenses
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        
        totalSpending = repository.totalSpending
            .map { it ?: 0.0 }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    }
    
    // ==================== AI TIER MANAGEMENT ====================
    
    /**
     * Select an AI tier for scanning
     */
    fun selectAiTier(tier: AiTier) {
        if (tier in _unlockedTiers.value) {
            _currentAiTier.value = tier
            pendingTier = tier
            saveTier(tier)
        }
    }
    
    /**
     * Unlock a tier (call this when in-app purchase is successful)
     */
    fun unlockTier(tier: AiTier) {
        val updated = _unlockedTiers.value + tier
        _unlockedTiers.value = updated
        saveUnlockedTiers(updated)
    }
    
    /**
     * Unlock all tiers (for testing or premium purchase)
     */
    fun unlockAllTiers() {
        val allTiers = AiTier.entries.toSet()
        _unlockedTiers.value = allTiers
        saveUnlockedTiers(allTiers)
    }
    
    private fun loadSavedTier(): AiTier {
        val ordinal = prefs.getInt(KEY_AI_TIER, 0)
        return AiTier.fromOrdinal(ordinal)
    }
    
    private fun saveTier(tier: AiTier) {
        prefs.edit { putInt(KEY_AI_TIER, tier.ordinal) }
    }
    
    private fun loadUnlockedTiers(): Set<AiTier> {
        val savedSet = prefs.getStringSet(KEY_UNLOCKED_TIERS, null)
        return if (savedSet != null) {
            savedSet.mapNotNull { ordinalStr ->
                ordinalStr.toIntOrNull()?.let { AiTier.fromOrdinal(it) }
            }.toSet()
        } else {
            // Default: BASIC tier is always unlocked (free tier)
            setOf(AiTier.BASIC)
        }
    }
    
    private fun saveUnlockedTiers(tiers: Set<AiTier>) {
        val ordinalSet = tiers.map { it.ordinal.toString() }.toSet()
        prefs.edit().putStringSet(KEY_UNLOCKED_TIERS, ordinalSet).apply()
    }
    
    /**
     * Create a GeminiService with the specified tier
     */
    private fun createGeminiService(tier: AiTier): GeminiService? {
        return apiKey?.let { key ->
            try {
                GeminiService(key, tier)
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to create GeminiService", e)
                null
            }
        }
    }
    
    // ==================== EXPENSE MANAGEMENT ====================
    
    fun addExpense(title: String, amount: Double, category: String, notes: String?) {
        viewModelScope.launch {
            val expense = Expense(
                title = title,
                amount = amount,
                category = category,
                notes = notes
            )
            repository.insert(expense)
            clearScannedData()
        }
    }
    
    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.delete(expense)
        }
    }
    

    /**
     * The HYBRID AI magic happens here:
     * 1. Receive image from CameraX
     * 2. Extract text using ML Kit (ON-DEVICE, FREE)
     * 3. Parse text using Gemini with selected tier (CHEAP - text only)
     */
    fun processReceiptImage(imageProxy: ImageProxy) {
        _isScanning.value = true
        _scanError.value = null
        
        val tier = pendingTier
        
        viewModelScope.launch {
            try {
                // Step 1: ML Kit OCR (FREE - on device)
                Log.d("MainViewModel", "Step 1: Extracting text with ML Kit...")
                val rawText = receiptScanner.extractTextFromImage(imageProxy)
                Log.d("MainViewModel", "Extracted text: $rawText")
                
                if (rawText.isBlank()) {
                    _scanError.value = "No text found in image. Please try again."
                    _isScanning.value = false
                    return@launch
                }
                
                // Step 2: Gemini parsing with selected tier
                val geminiService = createGeminiService(tier)
                if (geminiService != null) {
                    Log.d("MainViewModel", "Step 2: Parsing with ${tier.displayName} (${tier.modelName})...")
                    val parsed = geminiService.parseReceiptText(rawText)
                    
                    if (parsed != null) {
                        _scannedTitle.value = parsed.title
                        _scannedAmount.value = parsed.amount
                        _scannedCategory.value = parsed.category
                        Log.d("MainViewModel", "Parsed with ${tier.displayName}: $parsed")
                    } else {
                        _scanError.value = "Could not parse receipt. Please enter manually."
                    }
                } else {
                    // Fallback: Just use the raw text as title
                    _scanError.value = "AI not configured. Using raw OCR text."
                    _scannedTitle.value = rawText.take(50)
                }
                
            } catch (e: Exception) {
                Log.e("MainViewModel", "Scan failed", e)
                _scanError.value = "Scan failed: ${e.message}"
            } finally {
                _isScanning.value = false
            }
        }
    }
    
    fun clearScannedData() {
        _scannedTitle.value = null
        _scannedAmount.value = null
        _scannedCategory.value = null
        _scanError.value = null
    }
    
    override fun onCleared() {
        super.onCleared()
        receiptScanner.close()
    }
}
