package com.dzungphung.aimodel.econimical.smartspend

import android.app.Application
import android.util.Log
import com.daumo.ads.DynamicAdsManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SmartSpendApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize WireGuard Backend
        if (isMainProcess()) {
            try {
                // Check if ads should be disabled
                val adsDisabled = try {
                    BuildConfig.ADS_DISABLED
                } catch (e: Exception) {
                    Log.e("RingtoneApplication", "Error reading BuildConfig.ADS_DISABLED", e)
                    false // Default to false if we can't determine the value
                }

                DynamicAdsManager.initialize(this, adsDisabled)
            } catch (e: Exception) {
                Log.e("DynamicAdsManager", "init error", e)
            }
        }
    }

    private fun isMainProcess(): Boolean {
        return applicationInfo.packageName == getProcessName()
    }
}
