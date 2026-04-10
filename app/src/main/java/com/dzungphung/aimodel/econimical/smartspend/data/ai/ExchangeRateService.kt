package com.dzungphung.aimodel.econimical.smartspend.data.ai

import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fetches and caches exchange rates from a free API.
 * Rates are cached locally for offline use and refreshed once per day.
 *
 * API: https://open.er-api.com/v6/latest/USD (free, no key, 1500 req/month)
 */
@Singleton
class ExchangeRateService @Inject constructor(
    private val prefs: SharedPreferences
) {
    companion object {
        private const val TAG = "ExchangeRateService"
        private const val KEY_RATES_JSON = "exchange_rates_json"
        private const val KEY_RATES_TIMESTAMP = "exchange_rates_timestamp"
        private const val KEY_BASE_CURRENCY = "exchange_rates_base"
        private const val CACHE_DURATION_MS = 24 * 60 * 60 * 1000L // 24 hours
        private const val API_URL = "https://open.er-api.com/v6/latest/USD"
    }

    // In-memory cache
    private var cachedRates: Map<String, Double>? = null
    private var cachedBase: String = "USD"

    /**
     * Convert an amount from one currency to another.
     * Returns the original amount if conversion is not possible.
     */
    fun convert(amount: Double, fromCurrency: String, toCurrency: String): Double {
        if (fromCurrency == toCurrency || fromCurrency.isBlank() || toCurrency.isBlank()) {
            return amount
        }

        val rates = cachedRates ?: loadCachedRates()
        if (rates == null) {
            Log.w(TAG, "No exchange rates available, returning original amount")
            return amount
        }

        // All rates are relative to USD (base)
        val fromRate = if (fromCurrency == cachedBase) 1.0 else rates[fromCurrency]
        val toRate = if (toCurrency == cachedBase) 1.0 else rates[toCurrency]

        if (fromRate == null || toRate == null) {
            Log.w(TAG, "Rate not found for $fromCurrency or $toCurrency")
            return amount
        }

        // Convert: amount in FROM → USD → TO
        val amountInBase = amount / fromRate
        return amountInBase * toRate
    }

    /**
     * Get the exchange rate from one currency to another.
     * Returns null if rate is unavailable.
     */
    fun getRate(fromCurrency: String, toCurrency: String): Double? {
        if (fromCurrency == toCurrency) return 1.0

        val rates = cachedRates ?: loadCachedRates() ?: return null

        val fromRate = if (fromCurrency == cachedBase) 1.0 else rates[fromCurrency] ?: return null
        val toRate = if (toCurrency == cachedBase) 1.0 else rates[toCurrency] ?: return null

        return toRate / fromRate
    }

    /**
     * Refresh exchange rates from the API. Call this on app start (if online).
     * Safe to call from any coroutine — runs on IO dispatcher.
     */
    suspend fun refreshRates() {
        val lastUpdate = prefs.getLong(KEY_RATES_TIMESTAMP, 0)
        val age = System.currentTimeMillis() - lastUpdate

        if (age < CACHE_DURATION_MS && cachedRates != null) {
            Log.d(TAG, "Rates still fresh (${age / 1000 / 60} min old)")
            return
        }

        withContext(Dispatchers.IO) {
            try {
                val response = URL(API_URL).readText()
                val json = JSONObject(response)

                if (json.getString("result") != "success") {
                    Log.w(TAG, "API returned non-success: ${json.getString("result")}")
                    return@withContext
                }

                val ratesJson = json.getJSONObject("rates")
                val rates = mutableMapOf<String, Double>()
                ratesJson.keys().forEach { key ->
                    rates[key] = ratesJson.getDouble(key)
                }

                // Cache to memory
                cachedRates = rates
                cachedBase = json.optString("base_code", "USD")

                // Cache to SharedPreferences for offline use
                prefs.edit()
                    .putString(KEY_RATES_JSON, ratesJson.toString())
                    .putString(KEY_BASE_CURRENCY, cachedBase)
                    .putLong(KEY_RATES_TIMESTAMP, System.currentTimeMillis())
                    .apply()

                Log.d(TAG, "Refreshed ${rates.size} exchange rates (base: $cachedBase)")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch exchange rates, using cached", e)
                // Fall back to cached rates
                loadCachedRates()
            }
        }
    }

    /**
     * Load rates from SharedPreferences cache.
     */
    private fun loadCachedRates(): Map<String, Double>? {
        val jsonStr = prefs.getString(KEY_RATES_JSON, null) ?: return null

        return try {
            val json = JSONObject(jsonStr)
            val rates = mutableMapOf<String, Double>()
            json.keys().forEach { key ->
                rates[key] = json.getDouble(key)
            }
            cachedBase = prefs.getString(KEY_BASE_CURRENCY, "USD") ?: "USD"
            cachedRates = rates
            Log.d(TAG, "Loaded ${rates.size} cached exchange rates")
            rates
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse cached rates", e)
            null
        }
    }
}
