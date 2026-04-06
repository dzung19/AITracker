package com.example.smartspend.data.ai

import com.example.smartspend.data.local.Expense
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generates financial insights using local heuristics and template-based logic.
 * Used when the offline model is available but internet is not.
 */
@Singleton
class LocalFinancialAnalyzer @Inject constructor(
    private val prefs: android.content.SharedPreferences
) {

    fun analyze(expenses: List<Expense>, budget: Double?, periodName: String): String {
        if (expenses.isEmpty()) return "No expenses recorded for $periodName."

        val currencyFormat = com.example.smartspend.util.CurrencyFormatter.getFormatter(prefs)
        val total = expenses.sumOf { it.amount }
        val formattedTotal = currencyFormat.format(total)

        val sb = StringBuilder()
        sb.append("📊 **Offline Analysis ($periodName)**\n\n")
        sb.append("💰 **Total Spent:** $formattedTotal\n")

        // Budget Analysis
        if (budget != null && budget > 0) {
            val remaining = budget - total
            val percentage = (total / budget) * 100
            
            sb.append("🎯 **Budget:** ${currencyFormat.format(budget)}\n")
            if (remaining >= 0) {
                sb.append("✅ **Remaining:** ${currencyFormat.format(remaining)} (${String.format("%.1f", 100 - percentage)}% left)\n")
                sb.append("   Great job staying within budget! 👏\n")
            } else {
                sb.append("⚠️ **Over Budget:** ${currencyFormat.format(Math.abs(remaining))}\n")
                sb.append("   Try to cut back on discretionary spending. 📉\n")
            }
        } else {
            sb.append("ℹ️ *Set a budget in Settings to track goals!*\n")
        }

        sb.append("\n**Top Categories:**\n")
        
        // Category Analysis
        val categories = expenses.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
            .take(3)

        categories.forEachIndexed { index, (cat, amount) ->
            val icon = when(index) {
                0 -> "🥇"
                1 -> "🥈"
                2 -> "🥉"
                else -> "•"
            }
            sb.append("$icon **$cat:** ${currencyFormat.format(amount)}\n")
        }
        
        // Specific Insight
        val topCategory = categories.firstOrNull()
        if (topCategory != null) {
            sb.append("\n💡 **Tip:** Your highest spending is in **${topCategory.first}**. ")
            when (topCategory.first.lowercase()) {
                "food" -> sb.append("Consider cooking at home more often! 🍳")
                "transport" -> sb.append("Look for ride-share passes or public transit options. 🚌")
                "shopping" -> sb.append("Wait 24 hours before big purchases to avoid impulse buys. 🛍️")
                "entertainment" -> sb.append("Look for free local events or happy hours! 🎟️")
                else -> sb.append("Review these transactions to see if they were essential. 🧐")
            }
        }

        return sb.toString()
    }
}
