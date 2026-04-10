package com.dzungphung.aimodel.econimical.smartspend.data.ai

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
        
        // R2 Configuration
        private const val FULL_URL = BuildConfig.R2_MODEL_URL
        private const val ACCESS_KEY = BuildConfig.R2_ACCESS_KEY_ID
        private const val SECRET_KEY = BuildConfig.R2_SECRET_ACCESS_KEY
    }

    private val _downloadStatus = MutableStateFlow<DownloadStatus>(DownloadStatus.Idle)
    val downloadStatus: StateFlow<DownloadStatus> = _downloadStatus.asStateFlow()

    private val client = OkHttpClient()

    init {
        checkLocalModel()
    }

    fun checkLocalModel() {
        val file = getModelFile()
        if (file.exists() && file.length() > 0) {
            _downloadStatus.value = DownloadStatus.Completed(file)
        } else {
            _downloadStatus.value = DownloadStatus.Idle
        }
    }

    fun getLocalModelPath(): File? {
        val file = getModelFile()
        return if (file.exists() && file.length() > 0) file else null
    }

    fun deleteModel() {
        val file = getModelFile()
        if (file.exists()) {
            file.delete()
            _downloadStatus.value = DownloadStatus.Idle
            Log.d(TAG, "Local model deleted")
        }
    }

    suspend fun downloadModel() {
        withContext(Dispatchers.IO) {
            try {
                _downloadStatus.value = DownloadStatus.Downloading(0f)
                Log.d(TAG, "Starting download from: $FULL_URL")

                // Generate SigV4 Headers
                val headers = AwsSigV4Signer.getSignedHeaders(
                    method = "GET",
                    url = FULL_URL,
                    accessKey = ACCESS_KEY,
                    secretKey = SECRET_KEY
                )

                val requestBuilder = Request.Builder().url(FULL_URL)
                headers.forEach { (key, value) ->
                    requestBuilder.header(key, value)
                }
                
                val request = requestBuilder.build()
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    throw IOException("Download failed: ${response.code} ${response.message} body: ${response.body?.string()}")
                }

                val body = response.body ?: throw IOException("Empty response body")
                val contentLength = body.contentLength()
                val file = getModelFile()
                
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
        data class Downloading(val progress: Float) : DownloadStatus()
        data class Completed(val file: File) : DownloadStatus()
        data class Error(val message: String) : DownloadStatus()
    }
}
