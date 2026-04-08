package com.daumo.ads

import android.util.Log
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.tasks.await

data class AdConfigSet(
    val appOpenAdId: String,
    val bannerAdId: String,
    val interstitialAdId: String,
    val removeAdsSku: String,
    val priority: Int = 1,
    val isActive: Boolean = true
)

class FirebaseAdConfig private constructor() {
    private val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
    private val TAG = "ADS_DEBUG"

    // Local cache for parsed ad configs
    private var cachedAdConfigs: List<AdConfigSet>? = null
    private var lastConfigFetchTime: Long = 0

    companion object {
        @Volatile
        private var INSTANCE: FirebaseAdConfig? = null

        fun getInstance(): FirebaseAdConfig {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirebaseAdConfig().also { INSTANCE = it }
            }
        }

        // Empty fallback configs - will use build config values
        private val FALLBACK_CONFIGS = emptyList<AdConfigSet>()
    }

    suspend fun initialize(adsDisabled: Boolean = false): Boolean {
        // If ads are disabled, don't initialize Firebase Remote Config
        if (adsDisabled) {
            return false
        }

        return try {
            val configSettings = FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(3600) // 1 hour
                .build()

            remoteConfig.setConfigSettingsAsync(configSettings)
            Log.d(TAG, "Firebase Remote Config settings applied")

            // Set default values
            setDefaultValues()
            Log.d(TAG, "Default values set")

            // Fetch remote config
            remoteConfig.fetch(0).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    remoteConfig.activate().addOnCompleteListener { activateTask ->
                        if (activateTask.isSuccessful) {
                            // Log the actual data that was fetched
                            val configsJson = remoteConfig.getString("ad_configs")
                            // Only clear cache if the data has actually changed
                            if (hasConfigsChanged(configsJson)) {
                                clearCache()
                            } else {
                            }
                        } else {
                            Log.w(TAG, "❌ Failed to activate remote config, using defaults")
                            Log.w(TAG, "Activation error: ${activateTask.exception?.message}")
                        }
                    }
                } else {
                    Log.w(TAG, "❌ Failed to fetch remote config, using defaults")
                    Log.w(TAG, "Fetch error: ${task.exception?.message}")
                }
            }.await()

            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initialize Firebase Remote Config", e)
            Log.e(TAG, "Firebase initialization error: ${e.message}")
            false
        }
    }

    private fun setDefaultValues() {
        val defaults = mutableMapOf<String, Any>()

        // No default configs - only use Firebase remote config
        defaults["ad_configs"] = "[]"
        defaults["failed_ad_ids"] = "[]"
        defaults["config_update_time"] = System.currentTimeMillis().toString()

        remoteConfig.setDefaultsAsync(defaults)
    }

    fun getActiveAdConfigs(): List<AdConfigSet> {
        Log.d(TAG, "🔄 getActiveAdConfigs() called")

        // Check if we have cached configs that are still valid
        cachedAdConfigs?.let { cached ->
            Log.d(TAG, "📦 Returning cached ad configs (${cached.size} items)")
            return cached
        }

        return try {
            val configsJson = remoteConfig.getString("ad_configs")
            val failedAdIdsJson = remoteConfig.getString("failed_ad_ids")

            val configs = parseAdConfigs(configsJson)
            val failedAdIds = parseFailedAdIds(failedAdIdsJson)

            // Log each config's status for debugging
            configs.forEach { config ->
            }

            val activeConfigs = configs.filter { it.isActive && it.appOpenAdId !in failedAdIds }

            // Cache the active configs
            cachedAdConfigs = activeConfigs
            lastConfigFetchTime = System.currentTimeMillis()

            // If no active configs from Firebase, use fallback configs
            if (activeConfigs.isEmpty()) {
                Log.w(TAG, "⚠️  No active configs from Firebase, using fallback configs")
                FALLBACK_CONFIGS.forEach { config ->
                }
                return FALLBACK_CONFIGS
            }

            val sortedConfigs = activeConfigs.sortedBy { it.priority }

            sortedConfigs
        } catch (e: Exception) {
            Log.e(TAG, "Error getting active ad configs", e)
            Log.w(TAG, "⚠️  Error getting configs, using fallback configs")
            FALLBACK_CONFIGS.forEach { config ->
            }
            FALLBACK_CONFIGS
        }
    }

    fun getBestAdConfig(): AdConfigSet? {
        val activeConfigs = getActiveAdConfigs()
        activeConfigs.forEach { config ->
        }

        val bestConfig = activeConfigs.firstOrNull()
        if (bestConfig != null) {
        } else {
        }
        return bestConfig
    }

    suspend fun markAdConfigAsFailed(adUnitId: String) {
        try {
            val failedAdIdsJson = remoteConfig.getString("failed_ad_ids")
            val failedAdIds = parseFailedAdIds(failedAdIdsJson).toMutableSet()

            failedAdIds.add(adUnitId)

            val updatedFailedIdsJson = failedAdIds.joinToString(",", "[", "]") { "\"$it\"" }

            // Use setDefaultsAsync to update local values
            val defaults = mutableMapOf<String, Any>()
            defaults["failed_ad_ids"] = updatedFailedIdsJson
            remoteConfig.setDefaultsAsync(defaults)

            // Clear cache when failed ad IDs are updated
            clearCache()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mark ad config as failed", e)
        }
    }

    suspend fun resetFailedAdConfigs() {
        try {
            // Use setDefaultsAsync to update local values
            val defaults = mutableMapOf<String, Any>()
            defaults["failed_ad_ids"] = "[]"
            defaults["config_update_time"] = System.currentTimeMillis().toString()
            remoteConfig.setDefaultsAsync(defaults)

            // Clear cache when failed ad IDs are reset
            clearCache()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reset failed ad configs", e)
        }
    }

    /**
     * Check if ads are disabled via Firebase Remote Config
     */
    fun isAdsDisabled(): Boolean {
        return try {
            val adsDisabledValue = remoteConfig.getBoolean("ads_disabled")
            adsDisabledValue
        } catch (e: Exception) {
            Log.e(TAG, "Error getting ads_disabled value from Firebase", e)
            false
        }
    }

    /**
     * Check if the Firebase configs have changed compared to the cached data
     */
    private fun hasConfigsChanged(newConfigsJson: String): Boolean {
        // If we don't have cached configs, treat as changed
        val cachedConfigs = cachedAdConfigs
        if (cachedConfigs == null) {
            return true
        }

        // Parse the new configs to compare
        val newConfigs = try {
            parseAdConfigs(newConfigsJson)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing new configs for comparison", e)
            // If we can't parse new configs, treat as changed to be safe
            return true
        }

        // Compare the configs
        val hasChanged = cachedConfigs != newConfigs
        if (hasChanged) {
        } else {
        }
        return hasChanged
    }

    /**
     * Clear the local cache of parsed ad configs
     */
    private fun clearCache() {
        Log.d(TAG, "🧹 Clearing local cache")
        cachedAdConfigs = null
        lastConfigFetchTime = 0
    }

    private fun parseAdConfigs(json: String): List<AdConfigSet> {
        // Simple JSON parsing - in production, consider using a proper JSON library
        return try {
            // Try to parse JSON from Firebase - accept any valid AdMob application ID format
            if (json.contains("ca-app-pub-")) {
                return parseFirebaseConfig(json)
            }

            Log.w(TAG, "⚠️  Firebase config not found or invalid")
            emptyList() // No fallback configs
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing ad configs", e)
            emptyList() // No fallback configs
        }
    }

    private fun parseFirebaseConfig(json: String): List<AdConfigSet> {
        return try {
            // Extract the ad configs array from Firebase Remote Config JSON structure
            val adConfigsJson = extractAdConfigsJson(json)
            if (adConfigsJson.isNullOrEmpty()) {
                Log.w(TAG, "⚠️  Could not extract ad configs JSON, no fallback configs available")
                return emptyList()
            }

            // Use Gson to parse the JSON array
            val gson = Gson()
            val listType = object : TypeToken<List<Map<String, Any>>>() {}.type
            val configList: List<Map<String, Any>> = gson.fromJson(adConfigsJson, listType)

            val configs = configList.mapNotNull { configMap ->
                try {
                    // Skip applicationId field as it's initialized in manifest per Admob rules
                    // Use empty string for applicationId since it's not needed
                    val appOpenAdId = configMap["appOpenAdId"] as? String ?: return@mapNotNull null
                    val bannerAdId = configMap["bannerAdId"] as? String ?: return@mapNotNull null
                    val interstitialAdId = configMap["interstitialAdId"] as? String ?: return@mapNotNull null
                    val removeAdsSku = configMap["removeAdsSku"] as? String ?: "remove_ads_sku"
                    val priority = (configMap["priority"] as? Double)?.toInt() ?: (configMap["priority"] as? Int) ?: 1
                    val isActive = configMap["isActive"] as? Boolean ?: true

                    val config = AdConfigSet(
                        appOpenAdId = appOpenAdId,
                        bannerAdId = bannerAdId,
                        interstitialAdId = interstitialAdId,
                        removeAdsSku = removeAdsSku,
                        priority = priority,
                        isActive = isActive
                    )
                    Log.d(TAG, "📱 Parsed Firebase config with Gson: AppOpen: ${config.appOpenAdId}, Banner: ${config.bannerAdId}, Interstitial: ${config.interstitialAdId}")
                    config
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️  Failed to parse individual config: $e")
                    null
                }
            }

            // Log all configs for debugging with actual ad codes
            configs.forEach { config ->
            }

            configs.sortedBy { it.priority }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Firebase config JSON with Gson", e)
            emptyList() // No fallback configs
        }
    }

    private fun extractAdConfigsJson(json: String): String? {
        return try {
            // First try to extract from Firebase Remote Config structure
            val adConfigsValuePattern = """"ad_configs":\s*\{[^}]*"value":\s*"\[([^\]]+)\]"""".toRegex()
            val match = adConfigsValuePattern.find(json)

            if (match != null) {
                // Found the ad configs array content, reconstruct the full array
                val arrayContent = match.groupValues[1]
                return "[$arrayContent]"
            }

            // If not found in Firebase structure, check if it's already a direct array
            if (json.trim().startsWith("[") && json.trim().endsWith("]")) {
                return json
            }

            // Try to find any array that contains ad config fields
            val arrayPattern = """\[[^]]*"applicationId":[^]]*\]""".toRegex()
            val arrayMatch = arrayPattern.find(json)
            if (arrayMatch != null) {
                return arrayMatch.value
            }

            Log.w(TAG, "Could not find ad configs array in JSON")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting ad configs JSON", e)
            null
        }
    }

    private fun parseFailedAdIds(json: String): Set<String> {
        return try {
            // Simple JSON parsing for failed ad IDs
            emptySet() // For now, return empty set
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing failed ad IDs", e)
            emptySet()
        }
    }
}
