package com.example.smartspend.data.chat

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tensorflow.lite.task.text.nlclassifier.BertNLClassifier
import javax.inject.Inject
import javax.inject.Singleton
import java.io.IOException

/**
 * Classifies the user's intent using a local TensorFlow Lite BERT model.
 * Falls back to basic keyword matching if the model is not found.
 */
@Singleton
class IntentClassifier @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var classifier: BertNLClassifier? = null
    private val MODEL_FILE = "mobilebert.tflite"

    init {
        try {
            // Attempt to load the model from assets
            // Note: In a real app, you must download 'mobilebert.tflite' and place it in src/main/assets
            classifier = BertNLClassifier.createFromFile(context, MODEL_FILE)
            Log.d("IntentClassifier", "TFLite Model loaded successfully")
        } catch (e: IOException) {
            Log.w("IntentClassifier", "Model file not found. Falling back to rule-based logic.")
        } catch (e: Exception) {
            Log.e("IntentClassifier", "Error loading TFLite model", e)
        }
    }

    fun classify(text: String): String {
        return try {
            if (classifier != null) {
                // TFLite Classification
                val results = classifier?.classify(text) ?: return "unknown"
                // Return the category with the highest score
                results.maxByOrNull { it.score }?.label ?: "unknown"
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
