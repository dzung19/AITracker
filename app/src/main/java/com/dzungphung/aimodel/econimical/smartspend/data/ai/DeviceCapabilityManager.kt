package com.dzungphung.aimodel.econimical.smartspend.data.ai

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Detects device capabilities and recommends appropriate AI model tier.
 * Ensures the app works on both low-end and flagship devices.
 */
@Singleton
class DeviceCapabilityManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Model tiers based on device capabilities:
     * - LITE: Low-end devices (<2GB RAM) - Use heuristics only, no ML model
     * - STANDARD: Mid-range devices (2-4GB RAM) - Use quantized model
     * - FULL: Flagship devices (4GB+ RAM) - Use full-precision model
     */
    enum class ModelTier {
        LITE,      // Heuristics only
        STANDARD,  // Quantized DistilBERT
        FULL       // Full DistilBERT
    }

    private val activityManager: ActivityManager by lazy {
        context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    }

    /**
     * Returns the total RAM in bytes.
     */
    fun getTotalMemory(): Long {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        return memInfo.totalMem
    }

    /**
     * Returns available RAM in bytes.
     */
    fun getAvailableMemory(): Long {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        return memInfo.availMem
    }

    /**
     * Determines the appropriate model tier based on device RAM.
     */
    fun getRecommendedTier(): ModelTier {
        val totalRam = getTotalMemory()
        
        return when {
            totalRam < 2_000_000_000L -> ModelTier.LITE      // < 2GB: heuristics only
            totalRam < 4_000_000_000L -> ModelTier.STANDARD  // 2-4GB: quantized model
            else -> ModelTier.FULL                           // 4GB+: full model
        }
    }

    /**
     * Check if the device has low memory (system flag).
     */
    fun isLowRamDevice(): Boolean {
        return activityManager.isLowRamDevice
    }

    /**
     * Check if NNAPI (Neural Network API) is likely available.
     * NNAPI was introduced in Android 8.1 (API 27).
     */
    fun isNnapiAvailable(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1
    }

    /**
     * Get a human-readable device capability summary for debugging.
     */
    fun getCapabilitySummary(): String {
        val totalRamMb = getTotalMemory() / (1024 * 1024)
        val availRamMb = getAvailableMemory() / (1024 * 1024)
        val tier = getRecommendedTier()
        
        return buildString {
            append("Device Capabilities:\n")
            append("  Total RAM: ${totalRamMb}MB\n")
            append("  Available RAM: ${availRamMb}MB\n")
            append("  Low RAM Device: ${isLowRamDevice()}\n")
            append("  NNAPI Available: ${isNnapiAvailable()}\n")
            append("  Recommended Tier: $tier")
        }
    }
}
