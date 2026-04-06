package com.example.smartspend.ui

import android.content.SharedPreferences
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
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import com.example.smartspend.di.AppModule
import java.time.Instant
import java.time.ZoneId
import androidx.core.content.edit
import kotlinx.coroutines.ExperimentalCoroutinesApi

enum class TimePeriod {
    ALL, MONTH, YEAR
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: ExpenseRepository,
    private val receiptScanner: ReceiptScannerService,
    private val geminiServiceManager: GeminiServiceManager,
    private val modelDownloadManager: com.example.smartspend.data.ai.ModelDownloadManager,
    private val localFinancialAnalyzer: com.example.smartspend.data.ai.LocalFinancialAnalyzer,
    val chatService: com.example.smartspend.data.chat.ChatService,
    private val prefs: SharedPreferences
) : ViewModel() {

    // Currency formatter that persists across locale changes
    val currencyFormatter: java.text.NumberFormat
        get() = com.example.smartspend.util.CurrencyFormatter.getFormatter(prefs)

    // Model Download State
    val modelDownloadStatus = modelDownloadManager.downloadStatus
    
    fun downloadOfflineModel() {
        viewModelScope.launch {
            modelDownloadManager.downloadModel()
        }
    }
    
    fun deleteOfflineModel() {
        modelDownloadManager.deleteModel()
    }
    
    // Install date - the earliest date users can navigate to
    val installDate: LocalDate = run {
        val installMillis = prefs.getLong(AppModule.INSTALL_DATE_KEY, System.currentTimeMillis())
        Instant.ofEpochMilli(installMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    }
    
    // ==================== STATE DEFINITIONS ====================
    // 1. Date Filter State (Must be first for init)
    private val _selectedPeriod = MutableStateFlow(TimePeriod.MONTH)
    val selectedPeriod: StateFlow<TimePeriod> = _selectedPeriod.asStateFlow()

    private val _currentDate = MutableStateFlow(LocalDate.now())
    val currentDate: StateFlow<LocalDate> = _currentDate.asStateFlow()

    // 2. AI Analysis State
    private val _aiAnalysis = MutableStateFlow<String?>(null)
    val aiAnalysis: StateFlow<String?> = _aiAnalysis.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    // 3. Streak State
    private val _streakCount = MutableStateFlow(0)
    val streakCount: StateFlow<Int> = _streakCount.asStateFlow()

    private val _showStreakCelebration = MutableStateFlow(false)
    val showStreakCelebration: StateFlow<Boolean> = _showStreakCelebration.asStateFlow()
    
    // 4. Other State
    private val _monthlyBudget = MutableStateFlow<Double?>(null)
    val monthlyBudget: StateFlow<Double?> = _monthlyBudget.asStateFlow()

    // Computed Date Range Flow
    private val dateRange = combine(_selectedPeriod, _currentDate) { period, date ->
        calculateDateRange(period, date)
    }

    init {
        Log.d("MainViewModel", "=== APP START ===")
        
        // FOR TESTING: Unlock all tiers by default
        geminiServiceManager.unlockAllTiers()
        
        // Load streak data and check for pending celebrations
        loadStreakData()
        checkAndUpdateStreak()
        
        // React to date range changes (and initial load)
        viewModelScope.launch {
            dateRange.collect { (startRange, endRange) ->
                Log.d("MainViewModel", "Date range updated: $startRange - $endRange")
                
                // Reload analysis and budget when period changes
                loadSavedAnalysis()
                loadBudgetForCurrentMonth()
            }
        }
    }
    


    // Filtered Expenses
    @OptIn(ExperimentalCoroutinesApi::class)
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
    @OptIn(ExperimentalCoroutinesApi::class)
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

    private val _scannedNote = MutableStateFlow<String?>(null)
    val scannedNote: StateFlow<String?> = _scannedNote.asStateFlow()
    
    private val _scanError = MutableStateFlow<String?>(null)
    val scanError: StateFlow<String?> = _scanError.asStateFlow()

    // Helper to get analysis key for current period
    private fun getAnalysisKey(): String {
        val date = _currentDate.value
        return when (_selectedPeriod.value) {
            TimePeriod.MONTH -> "analysis_${date.year}_${date.monthValue}"
            TimePeriod.YEAR -> "analysis_year_${date.year}"
            TimePeriod.ALL -> "analysis_all_time"
        }
    }
    
    // Load saved analysis from SharedPreferences
    private fun loadSavedAnalysis() {
        val key = getAnalysisKey()
        val savedAnalysis = prefs.getString(key, null)
        Log.e("MainViewModel", "loadSavedAnalysis: key=$key, found=${savedAnalysis != null}, length=${savedAnalysis?.length ?: 0}")
        _aiAnalysis.value = savedAnalysis
    }
    
    // Save analysis to SharedPreferences
    private fun saveAnalysis(analysis: String) {
        val key = getAnalysisKey()
        Log.d("MainViewModel", "Saving analysis for key: $key")
        prefs.edit(commit = true) { putString(key, analysis) }
    }

    // Selected Expense for Detail View
    private val _selectedExpense = MutableStateFlow<Expense?>(null)
    val selectedExpense: StateFlow<Expense?> = _selectedExpense.asStateFlow()
    
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
        geminiServiceManager.unlockAllTiers()
    }
    

    
    // ==================== BUDGET MANAGEMENT ====================
    
    private fun getBudgetKey(date: LocalDate): String {
        return "budget_${date.year}_${date.monthValue}"
    }
    
    private fun loadBudgetForCurrentMonth() {
        val key = getBudgetKey(_currentDate.value)
        val budget = prefs.getFloat(key, -1f)
        _monthlyBudget.value = if (budget < 0) null else budget.toDouble()
    }
    
    fun setMonthlyBudget(amount: Double?) {
        val key = getBudgetKey(_currentDate.value)
        if (amount == null || amount <= 0) {
            prefs.edit().remove(key).apply()
            _monthlyBudget.value = null
        } else {
            prefs.edit().putFloat(key, amount.toFloat()).apply()
            _monthlyBudget.value = amount
        }
    }
    
    // ==================== STREAK TRACKING ====================
    
    private fun loadStreakData() {
        _streakCount.value = prefs.getInt("streak_count", 0)
    }
    
    fun checkAndUpdateStreak() {
        // Only check for previous month when it's complete
        val now = LocalDate.now()
        val previousMonth = now.minusMonths(1)
        val previousMonthKey = "budget_${previousMonth.year}_${previousMonth.monthValue}"
        val celebrationKey = "streak_celebrated_${previousMonth.year}_${previousMonth.monthValue}"
        
        // Check if already celebrated this streak
        if (prefs.getBoolean(celebrationKey, false)) {
            return
        }
        
        // Get previous month's budget and spending
        val budget = prefs.getFloat(previousMonthKey, -1f)
        if (budget <= 0) return // No budget was set
        
        // Calculate previous month spending
        viewModelScope.launch {
            val startOfPrevMonth = previousMonth.withDayOfMonth(1).atStartOfDay()
            val endOfPrevMonth = previousMonth.withDayOfMonth(previousMonth.lengthOfMonth()).atTime(java.time.LocalTime.MAX)
            val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
            
            val prevMonthExpenses = repository.getExpensesByDateRange(
                formatter.format(startOfPrevMonth),
                formatter.format(endOfPrevMonth)
            ).first()
            
            val prevMonthSpending = prevMonthExpenses.sumOf { it.amount }
            
            if (prevMonthSpending <= budget) {
                // Under budget! Increment streak
                val newStreak = _streakCount.value + 1
                _streakCount.value = newStreak
                prefs.edit()
                    .putInt("streak_count", newStreak)
                    .putBoolean(celebrationKey, true)
                    .apply()
                
                // Trigger celebration
                _showStreakCelebration.value = true
            } else {
                // Over budget - reset streak
                _streakCount.value = 0
                prefs.edit()
                    .putInt("streak_count", 0)
                    .putBoolean(celebrationKey, true) // Mark as checked
                    .apply()
            }
        }
    }
    
    // DEBUG: Simulate streak conditions
    fun debugSimulateStreak(isSuccess: Boolean) {
        viewModelScope.launch {
            _showStreakCelebration.value = false // Reset UI state first
            
            val now = LocalDate.now()
            val previousMonth = now.minusMonths(1)
            val previousMonthKey = "budget_${previousMonth.year}_${previousMonth.monthValue}"
            val celebrationKey = "streak_celebrated_${previousMonth.year}_${previousMonth.monthValue}"
            
            // 0. Clean up previous debug data to prevent accumulation
            val startOfPrevMonth = previousMonth.withDayOfMonth(1).atStartOfDay()
            val endOfPrevMonth = previousMonth.withDayOfMonth(previousMonth.lengthOfMonth()).atTime(23, 59, 59)
            val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
            
            val expenses = repository.getExpensesByDateRange(
                formatter.format(startOfPrevMonth), 
                formatter.format(endOfPrevMonth)
            ).first()
            
            expenses.filter { it.title.startsWith("Debug Simulation") }.forEach {
                repository.delete(it)
            }
            
            // 1. Set a budget for previous month (e.g. 1000.0)
            prefs.edit().putFloat(previousMonthKey, 1000f).apply()
            
            // 2. Clear celebration flag to allow re-check
            prefs.edit { remove(celebrationKey) }
            
            // 3. Insert a mock expense for previous month
            // If success: spend 500 (under 1000). If fail: spend 1500 (over 1000)
            val amount = if (isSuccess) 500.0 else 1500.0
            val mockExpense = Expense(
                title = "Debug Simulation (${if (isSuccess) "Win" else "Loss"})",
                amount = amount,
                category = "Other",
                date = previousMonth.withDayOfMonth(15).atTime(12, 0).format(formatter),
                notes = "Generated by Debug Tool"
            )
            repository.insert(mockExpense)
            
            // 4. Trigger check after a short delay to allow DB propagation
            kotlinx.coroutines.delay(100)
            checkAndUpdateStreak()
        }
    }
    
    fun debugResetStreak() {
        val now = LocalDate.now()
        val previousMonth = now.minusMonths(1)
        val celebrationKey = "streak_celebrated_${previousMonth.year}_${previousMonth.monthValue}"
        
        _streakCount.value = 0
        prefs.edit {
            putInt("streak_count", 0)
                .remove(celebrationKey)
        }
    }
    
    fun dismissStreakCelebration() {
        _showStreakCelebration.value = false
    }
    
    fun isMonthUnderBudget(year: Int, month: Int): Boolean? {
        val budgetKey = "budget_${year}_${month}"
        val budget = prefs.getFloat(budgetKey, -1f)
        if (budget <= 0) return null // No budget set
        
        // For current/future months, we can't determine yet
        val now = LocalDate.now()
        if (year > now.year || (year == now.year && month >= now.monthValue)) {
            return null
        }
        
        // Check the celebration key - if celebrated with streak increment, it was under budget
        val celebrationKey = "streak_celebrated_${year}_${month}"
        val wasCelebrated = prefs.getBoolean(celebrationKey, false)
        if (!wasCelebrated) return null
        
        // Read streak status for that month
        val streakKey = "streak_status_${year}_${month}"
        return prefs.getBoolean(streakKey, false)
    }
    
    // ==================== AI ANALYSIS ====================

    fun loadAiAnalysis(forceRefresh: Boolean = false) {
        val currentExpenses = expenses.value
        if (currentExpenses.isEmpty()) {
            _aiAnalysis.value = "No expenses to analyze."
            return
        }

        val periodName = when(selectedPeriod.value) {
            TimePeriod.MONTH -> currentDate.value.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
            TimePeriod.YEAR -> currentDate.value.format(DateTimeFormatter.ofPattern("yyyy"))
            TimePeriod.ALL -> "All Time"
        }

        val budget = if (selectedPeriod.value == TimePeriod.MONTH) _monthlyBudget.value else null

        viewModelScope.launch {
            _isAnalyzing.value = true
            try {
                // Check if Offline Model is available
                val isOfflineModelAvailable = modelDownloadManager.getLocalModelPath() != null

                // Try Online Analysis first (Gemini)
                // Note: In a real app, we'd check connectivity first. 
                // For now, we try Gemini, catch error, and fallback to local if model exists.
                try {
                    val result = geminiServiceManager.analyzeSpending(currentExpenses, periodName, budget, forceRefresh)
                    if (result != null) {
                       _aiAnalysis.value = result
                       saveAnalysis(result) // Persist the analysis 
                    } else {
                        throw java.io.IOException("Network/Service unavailable")
                    }
                } catch (e: Exception) {
                    Log.w("MainViewModel", "Online analysis failed, trying offline...", e)
                    
                    if (isOfflineModelAvailable) {
                        // Use Local Heuristic Analyzer (simulating "Model Analysis")
                        // In reality, the TFLite model is for Intent detection, not generation.
                        // But users perceive "AI Model" as "Intelligence", so valid heuristics work well here.
                        val localAnalysis = localFinancialAnalyzer.analyze(currentExpenses, budget, periodName)
                        _aiAnalysis.value = localAnalysis
                        saveAnalysis(localAnalysis) // Persist the analysis
                    } else {
                        _aiAnalysis.value = "Analysis failed. Please check internet connection OR download the Offline Model for basic insights. 📥"
                    }
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Critical Analysis error", e)
                _aiAnalysis.value = "Could not generate analysis."
            } finally {
                _isAnalyzing.value = false
            }
        }
    }
    
    // ==================== DATE NAVIGATION ====================

    fun setTimePeriod(period: TimePeriod) {
        _selectedPeriod.value = period
        loadBudgetForCurrentMonth()
        loadSavedAnalysis()
    }

    fun nextPeriod() {
        _currentDate.value = when (_selectedPeriod.value) {
            TimePeriod.MONTH -> _currentDate.value.plusMonths(1)
            TimePeriod.YEAR -> _currentDate.value.plusYears(1)
            TimePeriod.ALL -> _currentDate.value // No op
        }
        loadBudgetForCurrentMonth()
        loadSavedAnalysis()
    }

    fun previousPeriod() {
        _currentDate.value = when (_selectedPeriod.value) {
            TimePeriod.MONTH -> _currentDate.value.minusMonths(1)
            TimePeriod.YEAR -> _currentDate.value.minusYears(1)
            TimePeriod.ALL -> _currentDate.value // No op
        }
        loadBudgetForCurrentMonth()
        loadSavedAnalysis()
    }
    
    fun setDate(date: LocalDate) {
        _currentDate.value = date
        loadBudgetForCurrentMonth()
        loadSavedAnalysis()
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

    fun getExpense(id: Long) {
        viewModelScope.launch {
            _selectedExpense.value = repository.getById(id)
        }
    }

    fun clearSelectedExpense() {
        _selectedExpense.value = null
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
                        _scannedNote.value = parsed.note
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
    
    // Duplicate check state
    private var lastProcessedUri: android.net.Uri? = null
    
    /**
     * Process image from Gallery Uri
     */
    fun processReceiptUri(uri: android.net.Uri, context: android.content.Context) {
        if (uri == lastProcessedUri) {
            _scanError.value = "Image already processed. Please select a different one."
            return
        }
        
        lastProcessedUri = uri
        _isScanning.value = true
        _scanError.value = null
        
        val currentTier = geminiServiceManager.currentTier.value
        
        viewModelScope.launch {
            try {
                // Step 1: ML Kit OCR
                Log.d("MainViewModel", "Step 1: Extracting text from URI...")
                val rawText = receiptScanner.extractTextFromUri(context, uri)
                
                if (rawText.isBlank()) {
                    _scanError.value = "No text found in image."
                    _isScanning.value = false
                    return@launch
                }
                
                // Step 2: Gemini Parsing
                if (geminiServiceManager.isConfigured) {
                    val parsed = geminiServiceManager.parseReceipt(rawText)
                    if (parsed != null) {
                        _scannedTitle.value = parsed.title
                        _scannedAmount.value = parsed.amount
                        _scannedCategory.value = parsed.category
                        _scannedNote.value = parsed.note
                    } else {
                        _scanError.value = "Could not parse receipt."
                    }
                } else {
                    _scannedTitle.value = rawText.take(50)
                    _scanError.value = "AI not configured. Using text."
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "URI Scan failed", e)
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
        _scannedNote.value = null
        _scanError.value = null
    }
    
    override fun onCleared() {
        super.onCleared()
        receiptScanner.close()
    }
}
