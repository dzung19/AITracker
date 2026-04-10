package com.dzungphung.aimodel.econimical.smartspend.util

import android.content.SharedPreferences
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Centralized currency formatter that persists the user's chosen currency
 * independently from the device locale. This prevents the issue where
 * switching the phone language (e.g., Vietnamese → English) would change
 * the currency symbol from đ to $ while keeping VND-scale numbers.
 *
 * On first launch, the currency is set from the device locale.
 * After that, it stays fixed unless the user explicitly changes it.
 */
object CurrencyFormatter {

    private const val KEY_CURRENCY_CODE = "user_currency_code"

    private var cachedFormatter: NumberFormat? = null
    private var cachedCurrencyCode: String? = null

    /**
     * Get a NumberFormat instance that always uses the persisted currency,
     * regardless of the current device locale.
     */
    fun getFormatter(prefs: SharedPreferences): NumberFormat {
        val code = getCurrencyCode(prefs)
        // Return cached formatter if currency hasn't changed
        if (code == cachedCurrencyCode && cachedFormatter != null) {
            return cachedFormatter!!
        }

        val locale = getLocaleForCurrency(code)
        val formatter = NumberFormat.getCurrencyInstance(locale)
        formatter.currency = Currency.getInstance(code)
        // For VND, don't show decimal places (đ doesn't use them)
        if (code == "VND") {
            formatter.maximumFractionDigits = 0
        }

        cachedFormatter = formatter
        cachedCurrencyCode = code
        return formatter
    }

    /**
     * Get the persisted currency code, or initialize from device locale.
     */
    fun getCurrencyCode(prefs: SharedPreferences): String {
        val saved = prefs.getString(KEY_CURRENCY_CODE, null)
        if (saved != null) return saved

        // First launch: detect from device locale and persist it
        val defaultCode = try {
            Currency.getInstance(Locale.getDefault()).currencyCode
        } catch (_: Exception) {
            "USD"
        }
        prefs.edit().putString(KEY_CURRENCY_CODE, defaultCode).apply()
        return defaultCode
    }

    /**
     * Change the user's currency. Clears cached formatter.
     */
    fun setCurrencyCode(prefs: SharedPreferences, code: String) {
        prefs.edit().putString(KEY_CURRENCY_CODE, code).apply()
        cachedFormatter = null
        cachedCurrencyCode = null
    }

    /**
     * Format a number using the persisted currency.
     */
    fun format(prefs: SharedPreferences, amount: Double): String {
        return getFormatter(prefs).format(amount)
    }

    /**
     * Format a number using a specific currency code.
     * Useful for displaying per-expense amounts in their original currency.
     */
    fun formatWithCode(amount: Double, currencyCode: String): String {
        val locale = getLocaleForCurrency(currencyCode)
        val formatter = NumberFormat.getCurrencyInstance(locale)
        formatter.currency = Currency.getInstance(currencyCode)
        if (currencyCode == "VND") {
            formatter.maximumFractionDigits = 0
        }
        return formatter.format(amount)
    }

    /**
     * Convenience alias for getCurrencyCode — the user's home currency.
     */
    fun getHomeCurrency(prefs: SharedPreferences): String = getCurrencyCode(prefs)

    /**
     * Resolve a currency code: if empty or blank, return the home currency.
     * Used because Expense.currencyCode defaults to "" for backward compatibility.
     */
    fun resolveCode(prefs: SharedPreferences, code: String): String {
        return if (code.isBlank()) getHomeCurrency(prefs) else code
    }

    /**
     * Map a currency code to a locale that formats it naturally.
     * e.g., VND → vi_VN (postfix đ), USD → en_US (prefix $)
     */
    private fun getLocaleForCurrency(code: String): Locale {
        return when (code) {
            "VND" -> Locale("vi", "VN")
            "USD" -> Locale.US
            "EUR" -> Locale.GERMANY
            "GBP" -> Locale.UK
            "JPY" -> Locale.JAPAN
            "KRW" -> Locale.KOREA
            "CNY" -> Locale.CHINA
            "THB" -> Locale("th", "TH")
            else -> Locale.getDefault()
        }
    }
}
