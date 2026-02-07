package com.example.smartspend.data.ai

import android.content.Context
import android.util.Log
import com.example.smartspend.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelDownloadManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "ModelDownloadManager"
        private const val MODEL_FILENAME = "distilbert_financial.tflite"
        
        // Cloudflare R2 Configuration
        // Endpoint: https://<account_id>.r2.cloudflarestorage.com/<bucket>/<key>
        private const val MODEL_URL = BuildConfig.R2_MODEL_URL
        
        // API Token provided by user
        private const val API_TOKEN = BuildConfig.R2_API_TOKEN
    }

    private val _downloadStatus = MutableStateFlow<DownloadStatus>(DownloadStatus.Idle)
    val downloadStatus: StateFlow<DownloadStatus> = _downloadStatus.asStateFlow()

    private val client = OkHttpClient()

    init {
        checkLocalModel()
    }

    /**
     * Check if the model already exists locally
     */
    fun checkLocalModel() {
        val file = getModelFile()
        if (file.exists() && file.length() > 0) {
            _downloadStatus.value = DownloadStatus.Completed(file)
        } else {
            _downloadStatus.value = DownloadStatus.Idle
        }
    }

    /**
     * Get the local model file if it exists
     */
    fun getLocalModelPath(): File? {
        val file = getModelFile()
        return if (file.exists() && file.length() > 0) file else null
    }

    /**
     * Delete the local model file
     */
    fun deleteModel() {
        val file = getModelFile()
        if (file.exists()) {
            file.delete()
            _downloadStatus.value = DownloadStatus.Idle
            Log.d(TAG, "Local model deleted")
        }
    }

    /**
     * Download the model from the configured URL
     */
    suspend fun downloadModel() {
        withContext(Dispatchers.IO) {
            try {
                _downloadStatus.value = DownloadStatus.Downloading(0f)
                Log.d(TAG, "Starting download from: $MODEL_URL")

                val request = Request.Builder()
                    .url(MODEL_URL)
                    // Attempting to pass token as Bearer. 
                    // If R2 rejects this (needs SigV4), we log the error clearly.
                    .header("Authorization", "Bearer $API_TOKEN")
                    .build()

                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    throw IOException("Download failed: ${response.code} ${response.message}")
                }

                val body = response.body ?: throw IOException("Empty response body")
                val contentLength = body.contentLength()
                val file = getModelFile()
                
                // Ensure parent directory exists
                file.parentFile?.mkdirs()

                body.byteStream().use { input ->
                    FileOutputStream(file).use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var bytesRead: Int
                        var totalBytesRead: Long = 0
                        
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead
                            
                            if (contentLength > 0) {
                                val progress = totalBytesRead.toFloat() / contentLength
                                _downloadStatus.value = DownloadStatus.Downloading(progress)
                            }
                        }
                    }
                }
                
                Log.d(TAG, "Download complete: ${file.absolutePath}, Size: ${file.length()}")
                _downloadStatus.value = DownloadStatus.Completed(file)
                
            } catch (e: Exception) {
                Log.e(TAG, "Download error", e)
                _downloadStatus.value = DownloadStatus.Error(e.message ?: "Unknown error")
                // Clean up partial file
                val file = getModelFile()
                if (file.exists()) file.delete()
            }
        }
    }

    private fun getModelFile(): File {
        val modelsDir = File(context.filesDir, "models")
        if (!modelsDir.exists()) modelsDir.mkdirs()
        return File(modelsDir, MODEL_FILENAME)
    }

    sealed class DownloadStatus {
        data object Idle : DownloadStatus()
        data class Downloading(val progress: Float) : DownloadStatus() // 0.0 to 1.0
        data class Completed(val file: File) : DownloadStatus()
        data class Error(val message: String) : DownloadStatus()
    }
}
