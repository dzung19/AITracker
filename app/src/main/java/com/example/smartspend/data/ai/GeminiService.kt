package com.example.smartspend.data.ai

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Model specification with token limits and rate limits
 */
data class ModelSpec(
    val modelName: String,
    val inputTokenLimit: Int,
    val outputTokenLimit: Int,
    val requestsPerDay: Int,      // RPD - Free tier limit
    val requestsPerMinute: Int,   // RPM - Free tier limit
    val description: String
)

/**
 * AI Tier levels for in-app purchase integration.
 * Users select a tier (not a specific model) based on their subscription.
 * 
 * TIER SYSTEM:
 * ┌──────────┬─────────────────────┬───────┬───────┬──────────────────────────────┐
 * │ Tier     │ Model               │ RPD   │ RPM   │ Best For                     │
 * ├──────────┼─────────────────────┼───────┼───────┼──────────────────────────────┤
 * │ BASIC    │ Gemini 2.5 Flash-Lite│ 1000  │ 30    │ Simple receipts, high volume │
 * │ STANDARD │ Gemini 2.5 Flash    │ 250   │ 15    │ Most receipts, good accuracy │
 * │ ADVANCED │ Gemini 2.5 Pro      │ 100   │ 10    │ Complex receipts, best parse │
 * │ ELITE    │ Gemini 3 Flash      │ 20    │ 5     │ Cutting-edge AI, experimental│
 * └──────────┴─────────────────────┴───────┴───────┴──────────────────────────────┘
 */
enum class AiTier(
    val displayName: String,
    val modelName: String,
    val requestsPerDay: Int,
    val requestsPerMinute: Int,
    val description: String
) {
    /**
     * BASIC Tier (Free / Default)
     * - Model: Gemini 2.5 Flash-Lite
     * - Best for: High volume scanning, simple receipts
     * - RPD: 1000/day (highest quota)
     */
    BASIC(
        displayName = "Basic AI",
        modelName = GeminiModels.GEMINI_2_5_FLASH_LITE,
        requestsPerDay = 1000,
        requestsPerMinute = 30,
        description = "Fast & efficient for everyday receipts"
    ),
    
    /**
     * STANDARD Tier (Basic In-App Purchase)
     * - Model: Gemini 2.5 Flash
     * - Best for: Most use cases, balanced speed/accuracy
     * - RPD: 250/day
     */
    STANDARD(
        displayName = "Standard AI",
        modelName = GeminiModels.GEMINI_2_5_FLASH,
        requestsPerDay = 250,
        requestsPerMinute = 15,
        description = "Balanced performance for most receipts"
    ),
    
    /**
     * ADVANCED Tier (Premium In-App Purchase)
     * - Model: Gemini 2.5 Pro
     * - Best for: Complex receipts, better reasoning
     * - RPD: 100/day
     */
    ADVANCED(
        displayName = "Advanced AI",
        modelName = GeminiModels.GEMINI_2_5_PRO,
        requestsPerDay = 100,
        requestsPerMinute = 10,
        description = "Superior accuracy for complex receipts"
    ),
    
    /**
     * ELITE Tier (Top-tier In-App Purchase)
     * - Model: Gemini 3 Flash Preview
     * - Best for: Cutting-edge AI, experimental features
     * - RPD: 20/day (limited preview quota)
     */
    ELITE(
        displayName = "Elite AI",
        modelName = GeminiModels.GEMINI_3_FLASH_PREVIEW,
        requestsPerDay = 20,
        requestsPerMinute = 5,
        description = "Cutting-edge AI with latest capabilities"
    );
    
    companion object {
        /** Get tier by ordinal (for SharedPreferences storage) */
        fun fromOrdinal(ordinal: Int): AiTier = entries.getOrElse(ordinal) { BASIC }
        
        /** Get tier by model name */
        fun fromModelName(modelName: String): AiTier? = 
            entries.find { it.modelName == modelName }
    }
}

/**
 * Available Gemini API Models (as of January 2026)
 * 
 * Reference: https://ai.google.dev/gemini-api/docs/models
 * 
 * TOKEN LIMITS SUMMARY:
 * ┌─────────────────────────────────┬───────────────┬────────────────┬──────┬──────┐
 * │ Model                           │ Input Tokens  │ Output Tokens  │ RPD  │ RPM  │
 * ├─────────────────────────────────┼───────────────┼────────────────┼──────┼──────┤
 * │ Gemini 3 Pro Preview            │ 1,048,576     │ 65,536         │ 10   │ 2    │
 * │ Gemini 3 Flash Preview          │ 1,048,576     │ 65,536         │ 20   │ 5    │
 * │ Gemini 2.5 Flash                │ 1,048,576     │ 65,536         │ 250  │ 15   │
 * │ Gemini 2.5 Flash-Lite           │ 1,048,576     │ 65,536         │ 1000 │ 30   │
 * │ Gemini 2.5 Pro                  │ 1,048,576     │ 65,536         │ 100  │ 10   │
 * └─────────────────────────────────┴───────────────┴────────────────┴──────┴──────┘
 * 
 * Note: RPD/RPM are Free Tier limits. Paid tiers have higher limits.
 */
object GeminiModels {
    
    // ==================== TOKEN LIMITS ====================
    
    /** Standard context window (1M tokens) */
    const val TOKEN_LIMIT_1M = 1_048_576
    
    /** Medium context window (128K tokens) */
    const val TOKEN_LIMIT_128K = 131_072
    
    /** Small context window (64K tokens) */
    const val TOKEN_LIMIT_64K = 65_536
    
    /** TTS input limit (8K tokens) */
    const val TOKEN_LIMIT_8K = 8_192
    
    /** Standard output limit (64K tokens) */
    const val OUTPUT_LIMIT_64K = 65_536
    
    /** Image model output limit (32K tokens) */
    const val OUTPUT_LIMIT_32K = 32_768
    
    /** TTS output limit (16K tokens) */
    const val OUTPUT_LIMIT_16K = 16_384
    
    /** Live API output limit (8K tokens) */
    const val OUTPUT_LIMIT_8K = 8_192
    
    // ==================== GEMINI 3 FAMILY ====================
    // Latest generation, most intelligent models (Preview - limited RPD)
    
    /** Most intelligent model - Input: 1M, Output: 64K, RPD: 10 */
    const val GEMINI_3_PRO_PREVIEW = "gemini-3-pro-preview"
    val GEMINI_3_PRO_PREVIEW_SPEC = ModelSpec(
        GEMINI_3_PRO_PREVIEW, TOKEN_LIMIT_1M, OUTPUT_LIMIT_64K,
        requestsPerDay = 10, requestsPerMinute = 2,
        "Most intelligent model for multimodal understanding and agentic tasks"
    )
    
    /** Balanced model - Input: 1M, Output: 64K, RPD: 20 */
    const val GEMINI_3_FLASH_PREVIEW = "gemini-3-flash-preview"
    val GEMINI_3_FLASH_PREVIEW_SPEC = ModelSpec(
        GEMINI_3_FLASH_PREVIEW, TOKEN_LIMIT_1M, OUTPUT_LIMIT_64K,
        requestsPerDay = 20, requestsPerMinute = 5,
        "Balanced model for speed, scale, and frontier intelligence"
    )
    
    // ==================== GEMINI 2.5 FLASH FAMILY ====================
    // Fast and intelligent models - Best price-performance
    
    /** Fast & intelligent (STABLE) - Input: 1M, Output: 64K, RPD: 250 */
    const val GEMINI_2_5_FLASH = "gemini-2.5-flash"
    val GEMINI_2_5_FLASH_SPEC = ModelSpec(
        GEMINI_2_5_FLASH, TOKEN_LIMIT_1M, OUTPUT_LIMIT_64K,
        requestsPerDay = 250, requestsPerMinute = 15,
        "Best price-performance for large scale processing and agentic use cases"
    )
    
    /** Flash Preview - Input: 1M, Output: 64K, RPD: 250 */
    const val GEMINI_2_5_FLASH_PREVIEW = "gemini-2.5-flash-preview-09-2025"
    val GEMINI_2_5_FLASH_PREVIEW_SPEC = ModelSpec(
        GEMINI_2_5_FLASH_PREVIEW, TOKEN_LIMIT_1M, OUTPUT_LIMIT_64K,
        requestsPerDay = 250, requestsPerMinute = 15,
        "Preview version with latest features"
    )
    
    /** Image generation - Input: 64K, Output: 32K, RPD: 100 */
    const val GEMINI_2_5_FLASH_IMAGE = "gemini-2.5-flash-image"
    val GEMINI_2_5_FLASH_IMAGE_SPEC = ModelSpec(
        GEMINI_2_5_FLASH_IMAGE, TOKEN_LIMIT_64K, OUTPUT_LIMIT_32K,
        requestsPerDay = 100, requestsPerMinute = 10,
        "Image generation variant"
    )
    
    /** Live/Real-time - Input: 128K, Output: 8K, RPD: 100 */
    const val GEMINI_2_5_FLASH_LIVE = "gemini-2.5-flash-native-audio-preview-12-2025"
    val GEMINI_2_5_FLASH_LIVE_SPEC = ModelSpec(
        GEMINI_2_5_FLASH_LIVE, TOKEN_LIMIT_128K, OUTPUT_LIMIT_8K,
        requestsPerDay = 100, requestsPerMinute = 10,
        "Real-time audio/video for Live API"
    )
    
    /** Text-to-Speech - Input: 8K, Output: 16K, RPD: 100 */
    const val GEMINI_2_5_FLASH_TTS = "gemini-2.5-flash-preview-tts"
    val GEMINI_2_5_FLASH_TTS_SPEC = ModelSpec(
        GEMINI_2_5_FLASH_TTS, TOKEN_LIMIT_8K, OUTPUT_LIMIT_16K,
        requestsPerDay = 100, requestsPerMinute = 10,
        "Text-to-Speech generation"
    )
    
    // ==================== GEMINI 2.5 FLASH-LITE FAMILY ====================
    // Ultra fast, cost-optimized models - Highest RPD
    
    /** Ultra fast & cheap (STABLE) - Input: 1M, Output: 64K, RPD: 1000 */
    const val GEMINI_2_5_FLASH_LITE = "gemini-2.5-flash-lite"
    val GEMINI_2_5_FLASH_LITE_SPEC = ModelSpec(
        GEMINI_2_5_FLASH_LITE, TOKEN_LIMIT_1M, OUTPUT_LIMIT_64K,
        requestsPerDay = 1000, requestsPerMinute = 30,
        "Fastest model optimized for cost-efficiency and high throughput"
    )
    
    /** Flash-Lite Preview - Input: 1M, Output: 64K, RPD: 1000 */
    const val GEMINI_2_5_FLASH_LITE_PREVIEW = "gemini-2.5-flash-lite-preview-09-2025"
    val GEMINI_2_5_FLASH_LITE_PREVIEW_SPEC = ModelSpec(
        GEMINI_2_5_FLASH_LITE_PREVIEW, TOKEN_LIMIT_1M, OUTPUT_LIMIT_64K,
        requestsPerDay = 1000, requestsPerMinute = 30,
        "Preview version of Flash-Lite"
    )
    
    // ==================== GEMINI 2.5 PRO FAMILY ====================
    // Advanced thinking model - Best accuracy
    
    /** Advanced thinking (STABLE) - Input: 1M, Output: 64K, RPD: 100 */
    const val GEMINI_2_5_PRO = "gemini-2.5-pro"
    val GEMINI_2_5_PRO_SPEC = ModelSpec(
        GEMINI_2_5_PRO, TOKEN_LIMIT_1M, OUTPUT_LIMIT_64K,
        requestsPerDay = 100, requestsPerMinute = 10,
        "State-of-the-art thinking for complex reasoning in code, math, STEM"
    )
    
    /** Pro Text-to-Speech - Input: 8K, Output: 16K, RPD: 50 */
    const val GEMINI_2_5_PRO_TTS = "gemini-2.5-pro-preview-tts"
    val GEMINI_2_5_PRO_TTS_SPEC = ModelSpec(
        GEMINI_2_5_PRO_TTS, TOKEN_LIMIT_8K, OUTPUT_LIMIT_16K,
        requestsPerDay = 50, requestsPerMinute = 5,
        "Pro-level Text-to-Speech generation"
    )
    
    // ==================== UTILITY FUNCTIONS ====================
    
    /** Get all available model specs */
    val ALL_SPECS = listOf(
        GEMINI_3_PRO_PREVIEW_SPEC,
        GEMINI_3_FLASH_PREVIEW_SPEC,
        GEMINI_2_5_FLASH_SPEC,
        GEMINI_2_5_FLASH_PREVIEW_SPEC,
        GEMINI_2_5_FLASH_IMAGE_SPEC,
        GEMINI_2_5_FLASH_LIVE_SPEC,
        GEMINI_2_5_FLASH_TTS_SPEC,
        GEMINI_2_5_FLASH_LITE_SPEC,
        GEMINI_2_5_FLASH_LITE_PREVIEW_SPEC,
        GEMINI_2_5_PRO_SPEC,
        GEMINI_2_5_PRO_TTS_SPEC
    )
    
    /** Get spec by model name */
    fun getSpec(modelName: String): ModelSpec? = ALL_SPECS.find { it.modelName == modelName }
    
    /** Get output token limit for a model (returns default 64K if not found) */
    fun getOutputLimit(modelName: String): Int = getSpec(modelName)?.outputTokenLimit ?: OUTPUT_LIMIT_64K
    
    /** Get input token limit for a model (returns default 1M if not found) */
    fun getInputLimit(modelName: String): Int = getSpec(modelName)?.inputTokenLimit ?: TOKEN_LIMIT_1M
    
    /** Get requests per day limit for a model */
    fun getRPD(modelName: String): Int = getSpec(modelName)?.requestsPerDay ?: 250
    
    /** Get requests per minute limit for a model */
    fun getRPM(modelName: String): Int = getSpec(modelName)?.requestsPerMinute ?: 15
}

/**
 * Service that uses Gemini AI to parse receipt text.
 * Supports multiple AI tiers based on user's in-app purchase level.
 * 
 * COST OPTIMIZATION:
 * - We only send TEXT (not images) to Gemini
 * - Text tokens are MUCH cheaper than image tokens
 * - Model selection based on user's tier
 * 
 * @param apiKey The Gemini API key
 * @param tier The AI tier to use (default: BASIC for free users)
 */
class GeminiService(
    private val apiKey: String,
    private val tier: AiTier = AiTier.BASIC
) {
    
    private val json = Json { 
        ignoreUnknownKeys = true 
        isLenient = true
    }
    
    private val model = GenerativeModel(
        modelName = tier.modelName,
        apiKey = apiKey,
        generationConfig = generationConfig {
            temperature = 0.1f // Very low for consistent JSON output
            maxOutputTokens = GeminiModels.getOutputLimit(tier.modelName)
        }
    )
    
    /** Current tier being used */
    val currentTier: AiTier get() = tier
    
    /** Model name being used */
    val currentModelName: String get() = tier.modelName
    
    /** Daily request limit for current tier */
    val dailyRequestLimit: Int get() = tier.requestsPerDay
    
    /**
     * Parses raw receipt text and extracts structured expense data.
     * Returns null if parsing fails.
     */
    suspend fun parseReceiptText(rawText: String): ParsedExpense? {
        if (rawText.isBlank()) {
            return null
        }
        
        // Truncate very long receipts to save tokens (first 2000 chars is enough)
        val truncatedText = if (rawText.length > 2000) rawText.take(2000) else rawText
        
        // Improved prompt that's explicit about returning ONE summary JSON
        val prompt = """You are an expert receipt parser for Vietnamese and English receipts. Analyze this receipt text and extract the TOTAL expense.

RECEIPT TEXT:
$truncatedText

INSTRUCTIONS:
1. Find the STORE NAME (Merchant/Vendor):
   - Usually the FIRST line of the receipt.
   - Often in ALL CAPS.
   - Ignore distinct common prefixes like "Chao mung", "Welcome", "Hoa don", "Receipt".
   - If identifying a chain (e.g., Starbucks, Highland Coffee, Circle K, 7-Eleven, WinMart), use the brand name.

2. Find the TOTAL AMOUNT:
   - Look for keywords: "Total", "Grand Total", "Tong", "Tong cong", "Thanh tien", "Phai thu".
   - It is usually the largest amount or the final amount at the bottom.
   - Ignore tax/VAT lines unless included in total.

3. Categorize as ONE of: 
   - Food (Restaurants, Coffee, Groceries, Supermarkets)
   - Transport (Grab, Gas, Parking, Taxi)
   - Shopping (Clothing, Electronics, Home goods)
   - Entertainment (Movies, Games, Events)
   - Bills (Electricity, Water, Internet, Phone)
   - Investment (Stocks, Crypto, Gold, Savings)
   - Other (Services, Medical, Education)

Return EXACTLY ONE JSON object with the TOTAL purchase:
{"title":"Store Name","amount":123.45,"category":"Category"}

IMPORTANT: Return ONLY the raw JSON. No markdown formatting, no code blocks, no explanations."""

        return try {
            val response = model.generateContent(prompt)
            val responseText = response.text?.trim() ?: ""
            
            Log.d("GeminiService", "Tier: ${tier.displayName}, Model: ${tier.modelName}")
            Log.d("GeminiService", "Raw response: $responseText")
            
            // Extract JSON from response (handles markdown blocks and extracts first valid JSON)
            val jsonText = extractFirstJson(responseText)
            
            Log.d("GeminiService", "Extracted JSON: $jsonText")
            
            jsonText?.let { 
                json.decodeFromString<ParsedExpense>(it)
            }
        } catch (e: Exception) {
            Log.e("GeminiService", "Failed to parse receipt with ${tier.displayName}", e)
            null
        }
    }
    
    /**
     * Extracts the first valid JSON object from a response string.
     * Handles responses with markdown code blocks, multiple JSON objects, or extra text.
     */
    private fun extractFirstJson(text: String): String? {
        // Clean up markdown code blocks
        var cleaned = text
            .replace("```json", "")
            .replace("```", "")
            .trim()
        
        // If the text starts with '{', try to find the matching '}'
        if (cleaned.startsWith("{")) {
            var braceCount = 0
            var endIndex = -1
            
            for (i in cleaned.indices) {
                when (cleaned[i]) {
                    '{' -> braceCount++
                    '}' -> {
                        braceCount--
                        if (braceCount == 0) {
                            endIndex = i
                            break
                        }
                    }
                }
            }
            
            if (endIndex > 0) {
                return cleaned.substring(0, endIndex + 1)
            }
        }
        
        // Try to extract using regex as fallback
        val jsonPattern = """\{[^{}]*"title"[^{}]*"amount"[^{}]*"category"[^{}]*\}""".toRegex()
        val match = jsonPattern.find(cleaned)
        if (match != null) {
            return match.value
        }
        
        // Last resort: find first { and last } that contains our expected fields
        val firstBrace = cleaned.indexOf('{')
        if (firstBrace >= 0) {
            val endBrace = cleaned.indexOf('}', firstBrace)
            if (endBrace > firstBrace) {
                val candidate = cleaned.substring(firstBrace, endBrace + 1)
                if (candidate.contains("title") && candidate.contains("amount")) {
                    return candidate
                }
            }
        }
        
        return null
    }
    
    companion object {
        /**
         * Create a GeminiService with the specified tier.
         * Factory method for easy tier-based instantiation.
         */
        fun withTier(apiKey: String, tier: AiTier): GeminiService {
            return GeminiService(apiKey, tier)
        }
        
        /**
         * Create a GeminiService from saved tier ordinal (e.g., from SharedPreferences).
         */
        fun fromSavedTier(apiKey: String, tierOrdinal: Int): GeminiService {
            return GeminiService(apiKey, AiTier.fromOrdinal(tierOrdinal))
        }
    }
}

@Serializable
data class ParsedExpense(
    val title: String,
    val amount: Double,
    val category: String
)
