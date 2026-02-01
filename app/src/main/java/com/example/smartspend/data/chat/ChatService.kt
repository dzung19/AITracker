package com.example.smartspend.data.chat

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
class ChatService @Inject constructor() {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())

    suspend fun generateResponse(message: String, expense: Expense): ChatMessage {
        // Simulate network/processing delay for "AI" feel
        delay(800)
        
        val lowerMessage = message.lowercase()
        val responseText = when {
            lowerMessage.contains("high") || lowerMessage.contains("expensive") -> {
                analyzeExpenseCost(expense)
            }
            lowerMessage.contains("save") || lowerMessage.contains("reduce") -> {
                provideSavingTip(expense)
            }
            lowerMessage.contains("judge") || lowerMessage.contains("bad") -> {
                judgementalBot(expense)
            }
            else -> {
                "I see you spent ${currencyFormat.format(expense.amount)} on ${expense.title}. You can ask me if this is 'expensive', how to 'save' money, or for my 'judge'ment! 🤖"
            }
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
