package com.example.smartspend.data.chat

import com.example.smartspend.data.ai.GeminiServiceManager
import com.example.smartspend.data.local.Expense
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class ChatMessage(
    val id: Long = System.currentTimeMillis(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Singleton
class ChatService @Inject constructor(
    private val intentClassifier: IntentClassifier,
    private val geminiServiceManager: GeminiServiceManager,
    private val prefs: android.content.SharedPreferences
) {

    private val currencyFormat get() = com.example.smartspend.util.CurrencyFormatter.getFormatter(prefs)

    suspend fun generateResponse(message: String, expense: Expense): ChatMessage {
        // 1. Try Gemini Cloud (Native Tier)
        if (geminiServiceManager.isConfigured) {
            val systemPrompt = "You are a financial advisor. Context: User spent ${currencyFormat.format(expense.amount)} on ${expense.title} (Category: ${expense.category}). User says: \"$message\". Reply concisely (max 2 sentences) with advice."
            val aiResponse = geminiServiceManager.chat(systemPrompt)
            if (!aiResponse.isNullOrBlank()) {
                return ChatMessage(text = aiResponse + " ✨", isUser = false)
            }
        }

        // 2. Fallback: Classifier + Rules (MobileBERT)
        delay(600)
        val intent = intentClassifier.classify(message)
        val responseText = when (intent) {
            "analyze_cost" -> analyzeExpenseCost(expense)
            "saving_tip" -> provideSavingTip(expense)
            "judgement" -> judgementalBot(expense)
            "greeting" -> "Hello! I'm ready to analyze your spending on ${expense.title}. 🤖"
            else -> "I see you spent ${currencyFormat.format(expense.amount)} on ${expense.title}. Ask me if this is 'expensive', how to 'save', or for a 'judgement'! 🧠"
        }
        return ChatMessage(text = responseText, isUser = false)
    }

    suspend fun generateAnalyticsResponse(message: String, expenses: List<Expense>, budget: Double): ChatMessage {
        val total = expenses.sumOf { it.amount }
        val topCategory = expenses.groupBy { it.category }
            .maxByOrNull { it.value.sumOf { exp -> exp.amount } }
            ?.key ?: "None"

        // 1. Try Gemini Cloud
        if (geminiServiceManager.isConfigured) {
            val systemPrompt = """
                You are an Expert Financial Advisor.
                
                **Financial Data:**
                - Total Spending: ${currencyFormat.format(total)}
                - Monthly Budget: ${currencyFormat.format(budget)}
                - Transactions: ${expenses.size}
                - Highest Cost Category: $topCategory
                
                **User Question:** "$message"
                
                **Instructions:**
                - If spending > budget, give a mild warning.
                - If spending < budget, encourage them to save the difference.
                - Provide specific, actionable advice based on the data.
                - If the user asks for savings, suggest cutting down on the top category.
                - Be friendly, concise (max 3 sentences), and motivational.
                - Use emojis to make it engaging.
            """.trimIndent()
            val aiResponse = geminiServiceManager.chat(systemPrompt)
            if (!aiResponse.isNullOrBlank()) {
                return ChatMessage(text = aiResponse + " 📊", isUser = false)
            }
        }

        // 2. Fallback
        delay(800)
        val intent = intentClassifier.classify(message)
        
        val responseText = when (intent) {
            "analyze_cost" -> "You've spent a total of ${currencyFormat.format(total)} in this period. Your highest spending category is $topCategory."
            "saving_tip" -> "Since $topCategory is your biggest expense, try setting a stricter budget for it! small habits add up. 📉"
            "judgement" -> if (total > 1000) "Wow! ${currencyFormat.format(total)}? Someone's been enjoying life a bit too much... 💸" else "Only ${currencyFormat.format(total)}? You're a saving machine! 🤖"
            "greeting" -> "Hi! I'm looking at your spending for this period. Total: ${currencyFormat.format(total)}. Ask me for insights! 📊"
            else -> "I analyzed ${expenses.size} transactions. Total: ${currencyFormat.format(total)}. Top Category: $topCategory. Ask me how to save! 💡"
        }
        return ChatMessage(text = responseText, isUser = false)
    }

    private fun analyzeExpenseCost(expense: Expense): String {
        return if (expense.amount > 100) {
            "Yes, ${currencyFormat.format(expense.amount)} is quite a lot for ${expense.category}. Try to find cheaper alternatives next time! 💸"
        } else {
            "For ${expense.category}, ${currencyFormat.format(expense.amount)} seems reasonable. Good job staying within limits! 👍"
        }
    }

    private fun provideSavingTip(expense: Expense): String {
        return when (expense.category.lowercase()) {
            "food", "dining" -> "Cooking at home can save you up to 70% compared to eating out! 🍳"
            "transport" -> "Walking or public transport fits the budget better than taxis. 🚶"
            "shopping" -> "Wait 24 hours before buying non-essentials to avoid impulse purchases. 🛍️"
            "coffee" -> "Making your own coffee saves about $3 per cup. That's $1000 a year! ☕"
            else -> "Tracking every penny is the first step to saving. You're doing great! 📈"
        }
    }

    private fun judgementalBot(expense: Expense): String {
         return if (expense.amount > 200) {
             "My circuits are overheating looking at this price! ${currencyFormat.format(expense.amount)}?! Please tell me it was worth it. 🤖💥"
         } else {
             "Acceptable. Keep it up human. 🤖"
         }
    }
}
