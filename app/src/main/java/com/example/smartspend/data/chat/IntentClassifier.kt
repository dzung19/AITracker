package com.example.smartspend.data.chat

import android.content.Context
import android.util.Log
import com.example.smartspend.data.ai.DeviceCapabilityManager
import com.example.smartspend.data.ai.DeviceCapabilityManager.ModelTier
import com.example.smartspend.data.ai.ModelDownloadManager
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.text.textclassifier.TextClassifier
import com.google.mediapipe.tasks.text.textclassifier.TextClassifier.TextClassifierOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Classifies the user's intent using a local MediaPipe TextClassifier.
 * 
 * Features:
 * - Device-capability-based model selection (LITE/STANDARD/FULL)
 * - NNAPI hardware acceleration on supported devices
 * - Graceful fallback to heuristics on low-end devices or errors
 */
@Singleton
class IntentClassifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceCapabilityManager: DeviceCapabilityManager,
    private val modelDownloadManager: ModelDownloadManager
) {
    private var classifier: TextClassifier? = null
    private val currentTier: ModelTier
    
    companion object {
        private const val TAG = "IntentClassifier"
        
        // Model files for different tiers - trained DistilBERT on personal finance data
        private const val MODEL_FILE_FULL = "distilbert_financial.tflite"
        private const val MODEL_FILE_STANDARD = "distilbert_financial.tflite"
    }

    init {
        currentTier = deviceCapabilityManager.getRecommendedTier()
        Log.d(TAG, "Device tier: $currentTier")
        Log.d(TAG, deviceCapabilityManager.getCapabilitySummary())
        
        initializeModel()
    }
    
    private fun initializeModel() {
        // Skip model loading on low-end devices - use heuristics only
        if (currentTier == ModelTier.LITE) {
            Log.i(TAG, "Low-end device detected. Using heuristic classification only.")
            return
        }
        
        try {
            val modelFile = when (currentTier) {
                ModelTier.FULL -> MODEL_FILE_FULL
                ModelTier.STANDARD -> MODEL_FILE_STANDARD
                ModelTier.LITE -> return  // Already handled above
            }
            
            // Build base options with optional NNAPI delegate
            val baseOptionsBuilder = BaseOptions.builder()

            // Check for downloaded model first
            val localModel = modelDownloadManager.getLocalModelPath()
            if (localModel != null) {
                Log.d(TAG, "Using downloaded offline model: ${localModel.absolutePath}")
                baseOptionsBuilder.setModelAssetPath(localModel.absolutePath)
            } else {
                Log.d(TAG, "Using bundled asset model: $modelFile")
                baseOptionsBuilder.setModelAssetPath(modelFile)
            }
            
            // Enable NNAPI hardware acceleration if available
            if (deviceCapabilityManager.isNnapiAvailable() && currentTier == ModelTier.FULL) {
                try {
                    baseOptionsBuilder.setDelegate(Delegate.GPU)
                    Log.d(TAG, "GPU delegate enabled for hardware acceleration")
                } catch (e: Exception) {
                    Log.w(TAG, "GPU delegate not available, using CPU", e)
                }
            }
            
            val options = TextClassifierOptions.builder()
                .setBaseOptions(baseOptionsBuilder.build())
                .build()

            classifier = TextClassifier.createFromOptions(context, options)
            Log.d(TAG, "Model loaded successfully: $modelFile (Tier: $currentTier)")
            
            // Warm up the model with a dummy classification
            warmUp()
            
        } catch (e: Exception) {
            Log.w(TAG, "Model loading failed. Falling back to heuristics.", e)
            classifier = null
        }
    }
    
    /**
     * Warm up the model by running a dummy classification.
     * This reduces latency on the first real classification.
     */
    private fun warmUp() {
        try {
            classifier?.classify("hello")
            Log.d(TAG, "Model warm-up complete")
        } catch (e: Exception) {
            Log.w(TAG, "Warm-up failed", e)
        }
    }

    fun classify(text: String): String {
        return try {
            when {
                // Low-end devices: always use heuristics
                currentTier == ModelTier.LITE -> simpleHeuristicClassify(text)
                
                // Model available: use ML classification
                classifier != null -> {
                    val results = classifier?.classify(text)
                    val category = results?.classificationResult()
                        ?.classifications()
                        ?.firstOrNull()
                        ?.categories()
                        ?.maxByOrNull { it.score() }
                    
                    category?.categoryName() ?: simpleHeuristicClassify(text)
                }
                
                // Model not loaded: fallback to heuristics
                else -> simpleHeuristicClassify(text)
            }
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "OOM during classification, falling back to heuristics", e)
            simpleHeuristicClassify(text)
        } catch (e: Exception) {
            Log.e(TAG, "Classification failed", e)
            simpleHeuristicClassify(text)
        }
    }
    
    /**
     * Returns the current model tier being used.
     */
    fun getCurrentTier(): ModelTier = currentTier
    
    /**
     * Returns true if the ML model is loaded and running.
     */
    fun isModelLoaded(): Boolean = classifier != null

    /**
     * Fallback heuristic for classification.
     * Matches the intents that the trained DistilBERT model will output.
     */
    private fun simpleHeuristicClassify(text: String): String {
        val lower = text.lowercase()
        return when {
            lower.containsAny("expensive", "cost", "price", "too much", "overspend", "worth", "afford") -> "analyze_cost"
            lower.containsAny("save", "reduce", "budget", "cut", "tip", "advice", "spend less") -> "saving_tip"
            lower.containsAny("judge", "opinion", "bad", "good", "regret", "worth it") -> "judgement"
            lower.containsAny("hello", "hi", "hey", "thanks") -> "greeting"
            lower.containsAny("category", "where", "breakdown", "spending on", "most money") -> "category_info"
            else -> "unknown"
        }
    }
    
    private fun String.containsAny(vararg keywords: String): Boolean {
        return keywords.any { this.contains(it) }
    }
}
