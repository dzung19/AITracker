package com.example.smartspend.data.ai

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Service that uses Gemini Flash to parse receipt text.
 * 
 * COST OPTIMIZATION:
 * - We only send TEXT (not images) to Gemini
 * - Text tokens are MUCH cheaper than image tokens
 * - Gemini Flash is the cheapest option with free tier
 */
class GeminiService(apiKey: String) {
    
    private val json = Json { 
        ignoreUnknownKeys = true 
        isLenient = true
    }
    
    private val model = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = apiKey,
        generationConfig = generationConfig {
            temperature = 0.2f // Low temperature for more consistent parsing
            maxOutputTokens = 256 // Keep response short to save tokens
        }
    )
    
    /**
     * Parses raw receipt text and extracts structured expense data.
     * Returns null if parsing fails.
     */
    suspend fun parseReceiptText(rawText: String): ParsedExpense? {
        if (rawText.isBlank()) {
            return null
        }
        
        val prompt = """
You are a receipt parser. Extract expense information from this receipt text.

RECEIPT TEXT:
$rawText

Respond with ONLY a JSON object in this exact format (no markdown, no explanation):
{"title": "store or item name", "amount": 0.00, "category": "Food|Transport|Shopping|Entertainment|Bills|Other"}

Rules:
- title: The store name or main item purchased
- amount: The total amount (just the number, no currency symbol)
- category: Must be exactly one of: Food, Transport, Shopping, Entertainment, Bills, Other

JSON:
""".trimIndent()

        return try {
            val response = model.generateContent(prompt)
            val jsonText = response.text?.trim()
                ?.removePrefix("```json")
                ?.removePrefix("```")
                ?.removeSuffix("```")
                ?.trim()
            
            Log.d("GeminiService", "Raw response: $jsonText")
            
            jsonText?.let { 
                json.decodeFromString<ParsedExpense>(it)
            }
        } catch (e: Exception) {
            Log.e("GeminiService", "Failed to parse receipt", e)
            null
        }
    }
}

@Serializable
data class ParsedExpense(
    val title: String,
    val amount: Double,
    val category: String
)
