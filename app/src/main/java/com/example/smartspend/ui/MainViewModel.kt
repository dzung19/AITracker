package com.example.smartspend.ui

import android.util.Log
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartspend.data.ai.AiTier
import com.example.smartspend.data.ai.GeminiServiceManager
import com.example.smartspend.data.local.Expense
import com.example.smartspend.data.repository.ExpenseRepository
import com.example.smartspend.data.scanner.ReceiptScannerService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

enum class TimePeriod {
    ALL, MONTH, YEAR
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: ExpenseRepository,
    private val receiptScanner: ReceiptScannerService,
    private val geminiServiceManager: GeminiServiceManager
) : ViewModel() {
    
    init {
        // FOR TESTING: Unlock all tiers by default
        geminiServiceManager.unlockAllTiers()
    }
    
    // Date Filter State
    private val _selectedPeriod = MutableStateFlow(TimePeriod.MONTH)
    val selectedPeriod: StateFlow<TimePeriod> = _selectedPeriod.asStateFlow()

    private val _currentDate = MutableStateFlow(LocalDate.now())
    val currentDate: StateFlow<LocalDate> = _currentDate.asStateFlow()

    // Computed Date Range Flow
    private val dateRange = combine(_selectedPeriod, _currentDate) { period, date ->
        calculateDateRange(period, date)
    }

    // Filtered Expenses
    val expenses: StateFlow<List<Expense>> = dateRange
        .flatMapLatest { (start, end) ->
            if (start == null || end == null) {
                repository.allExpenses
            } else {
                repository.getExpensesByDateRange(start, end)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered Total Spending
    val totalSpending: StateFlow<Double> = dateRange
        .flatMapLatest { (start, end) ->
            if (start == null || end == null) {
                repository.totalSpending
            } else {
                repository.getTotalSpendingByDateRange(start, end)
            }
        }
        .map { it ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    
    // AI Tier state - delegated to GeminiServiceManager
    val currentAiTier: StateFlow<AiTier> = geminiServiceManager.currentTier
    val unlockedTiers: StateFlow<Set<AiTier>> = geminiServiceManager.unlockedTiers
    
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
    
    // ==================== AI TIER MANAGEMENT ====================
    
    /**
     * Select an AI tier for scanning
     */
    fun selectAiTier(tier: AiTier) {
        geminiServiceManager.selectTier(tier)
    }
    
    /**
     * Unlock a tier (call this when in-app purchase is successful)
     */
    fun unlockTier(tier: AiTier) {
        geminiServiceManager.unlockTier(tier)
    }
    
    /**
     * Unlock all tiers (for testing or premium purchase)
     */
    fun unlockAllTiers() {
        geminiServiceManager.unlockAllTiers()
    }
    
    // ==================== DATE NAVIGATION ====================

    fun setTimePeriod(period: TimePeriod) {
        _selectedPeriod.value = period
    }

    fun nextPeriod() {
        _currentDate.value = when (_selectedPeriod.value) {
            TimePeriod.MONTH -> _currentDate.value.plusMonths(1)
            TimePeriod.YEAR -> _currentDate.value.plusYears(1)
            TimePeriod.ALL -> _currentDate.value // No op
        }
    }

    fun previousPeriod() {
        _currentDate.value = when (_selectedPeriod.value) {
            TimePeriod.MONTH -> _currentDate.value.minusMonths(1)
            TimePeriod.YEAR -> _currentDate.value.minusYears(1)
            TimePeriod.ALL -> _currentDate.value // No op
        }
    }

    private fun calculateDateRange(period: TimePeriod, date: LocalDate): Pair<String?, String?> {
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        return when (period) {
            TimePeriod.ALL -> null to null
            TimePeriod.MONTH -> {
                val start = date.with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay()
                val end = date.with(TemporalAdjusters.lastDayOfMonth()).atTime(LocalTime.MAX)
                start.format(formatter) to end.format(formatter)
            }
            TimePeriod.YEAR -> {
                val start = date.with(TemporalAdjusters.firstDayOfYear()).atStartOfDay()
                val end = date.with(TemporalAdjusters.lastDayOfYear()).atTime(LocalTime.MAX)
                start.format(formatter) to end.format(formatter)
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
    
    // ==================== RECEIPT SCANNING ====================
    
    /**
     * The HYBRID AI magic happens here:
     * 1. Receive image from CameraX
     * 2. Extract text using ML Kit (ON-DEVICE, FREE)
     * 3. Parse text using Gemini with selected tier (CHEAP - text only)
     */
    fun processReceiptImage(imageProxy: ImageProxy) {
        _isScanning.value = true
        _scanError.value = null
        
        val currentTier = geminiServiceManager.currentTier.value
        
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
                if (geminiServiceManager.isConfigured) {
                    Log.d("MainViewModel", "Step 2: Parsing with ${currentTier.displayName} (${currentTier.modelName})...")
                    val parsed = geminiServiceManager.parseReceipt(rawText)
                    
                    if (parsed != null) {
                        _scannedTitle.value = parsed.title
                        _scannedAmount.value = parsed.amount
                        _scannedCategory.value = parsed.category
                        Log.d("MainViewModel", "Parsed with ${currentTier.displayName}: $parsed")
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
