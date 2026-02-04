package com.example.smartspend.data.chat

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.text.textclassifier.TextClassifier
import com.google.mediapipe.tasks.text.textclassifier.TextClassifier.TextClassifierOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import java.io.IOException

/**
 * Classifies the user's intent using a local MediaPipe TextClassifier (MobileBERT).
 * Falls back to basic keyword matching if the model is not found.
 */
@Singleton
class IntentClassifier @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var classifier: TextClassifier? = null
    private val MODEL_FILE = "mobilebert.tflite"

    init {
        try {
            // Initialize MediaPipe TextClassifier
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath(MODEL_FILE)
                .build()

            val options = TextClassifierOptions.builder()
                .setBaseOptions(baseOptions)
                .build()

            classifier = TextClassifier.createFromOptions(context, options)
            Log.d("IntentClassifier", "MediaPipe Model loaded successfully")
        } catch (e: Exception) {
            Log.w("IntentClassifier", "Model file not found or invalid. Falling back to rule-based logic.", e)
        }
    }

    fun classify(text: String): String {
        return try {
            if (classifier != null) {
                // MediaPipe Classification
                val results = classifier?.classify(text)
                
                // Extract top category
                val category = results?.classificationResult()
                    ?.classifications()
                    ?.firstOrNull()
                    ?.categories()
                    ?.maxByOrNull { it.score() }
                
                category?.categoryName() ?: "unknown"
            } else {
                // Fallback Logic (if model missing)
                simpleHeuristicClassify(text)
            }
        } catch (e: Exception) {
            Log.e("IntentClassifier", "Classification failed", e)
            "unknown"
        }
    }

    /**
     * Fallback heuristic if TFLite model is not present.
     * Mimics the categories the BERT model would detect.
     */
    private fun simpleHeuristicClassify(text: String): String {
        val lower = text.lowercase()
        return when {
            lower.contains("expensive") || lower.contains("cost") || lower.contains("price") -> "analyze_cost"
            lower.contains("save") || lower.contains("reduce") || lower.contains("budget") -> "saving_tip"
            lower.contains("judge") || lower.contains("bad") -> "judgement"
            lower.contains("hello") || lower.contains("hi") -> "greeting"
            else -> "unknown"
        }
    }
}
