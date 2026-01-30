package com.example.smartspend.ui

import android.app.Application
import android.util.Log
import androidx.camera.core.ImageProxy
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartspend.BuildConfig
import com.example.smartspend.data.ai.GeminiService
import com.example.smartspend.data.local.AppDatabase
import com.example.smartspend.data.local.Expense
import com.example.smartspend.data.repository.ExpenseRepository
import com.example.smartspend.data.scanner.ReceiptScannerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository: ExpenseRepository
    private val receiptScanner = ReceiptScannerService()
    
    // API key is read from local.properties via BuildConfig (secure, not in git)
    private val geminiService: GeminiService? = try {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isNotBlank()) {
            GeminiService(apiKey)
        } else {
            Log.w("MainViewModel", "GEMINI_API_KEY not set in local.properties")
            null
        }
    } catch (e: Exception) {
        Log.e("MainViewModel", "Failed to initialize Gemini", e)
        null
    }
    
    val expenses: StateFlow<List<Expense>>
    val totalSpending: StateFlow<Double>
    
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
    
    // Camera navigation state
    private val _showCamera = MutableStateFlow(false)
    val showCamera: StateFlow<Boolean> = _showCamera.asStateFlow()
    
    init {
        val database = AppDatabase.getDatabase(application)
        val expenseDao = database.expenseDao()
        repository = ExpenseRepository(expenseDao)
        
        expenses = repository.allExpenses
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        
        totalSpending = repository.totalSpending
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    }
    
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
    
    fun openCamera() {
        _showCamera.value = true
    }
    
    fun closeCamera() {
        _showCamera.value = false
    }
    
    /**
     * The HYBRID AI magic happens here:
     * 1. Receive image from CameraX
     * 2. Extract text using ML Kit (ON-DEVICE, FREE)
     * 3. Parse text using Gemini (CHEAP - text only)
     */
    fun processReceiptImage(imageProxy: ImageProxy) {
        _isScanning.value = true
        _scanError.value = null
        _showCamera.value = false
        
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
                
                // Step 2: Gemini parsing (CHEAP - text only)
                if (geminiService != null) {
                    Log.d("MainViewModel", "Step 2: Parsing with Gemini...")
                    val parsed = geminiService.parseReceiptText(rawText)
                    
                    if (parsed != null) {
                        _scannedTitle.value = parsed.title
                        _scannedAmount.value = parsed.amount
                        _scannedCategory.value = parsed.category
                        Log.d("MainViewModel", "Parsed: $parsed")
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
