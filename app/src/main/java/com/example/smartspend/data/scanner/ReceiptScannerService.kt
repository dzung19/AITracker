package com.example.smartspend.data.scanner

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Service that handles the "Hybrid AI" flow:
 * 1. Receives an image from CameraX
 * 2. Uses ML Kit (ON-DEVICE, FREE) to extract raw text
 * 3. Returns the raw text for further processing by Gemini
 */
class ReceiptScannerService {
    
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    
    /**
     * Extracts text from an ImageProxy using ML Kit.
     * This runs entirely on-device - no API cost!
     */
    suspend fun extractTextFromImage(imageProxy: ImageProxy): String {
        return suspendCancellableCoroutine { continuation ->
            try {
                val bitmap = imageProxyToBitmap(imageProxy)
                val inputImage = InputImage.fromBitmap(bitmap, 0)
                
                textRecognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        val extractedText = visionText.text
                        Log.d("ReceiptScanner", "Extracted text: $extractedText")
                        imageProxy.close()
                        continuation.resume(extractedText)
                    }
                    .addOnFailureListener { e ->
                        Log.e("ReceiptScanner", "Text recognition failed", e)
                        imageProxy.close()
                        continuation.resumeWithException(e)
                    }
            } catch (e: Exception) {
                Log.e("ReceiptScanner", "Error processing image", e)
                imageProxy.close()
                continuation.resumeWithException(e)
            }
        }
    }
    
    /**
     * Converts ImageProxy to Bitmap.
     * Handles rotation based on image metadata.
     */
    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap {
        val buffer = imageProxy.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: throw IllegalStateException("Failed to decode image")
        
        // Handle rotation
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        return if (rotationDegrees != 0) {
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }
    }
    
    fun close() {
        textRecognizer.close()
    }
}
