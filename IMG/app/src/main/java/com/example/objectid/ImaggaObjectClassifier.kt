package com.example.objectid

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Object classifier that uses the Imagga API for image recognition.
 * This replaces the TensorFlow Lite model with cloud-based recognition.
 */
class ImaggaObjectClassifier(private val context: Context) {
    
    private val imaggaService = ImaggaApiService()
    private var initializationError: String? = null
    private var isInitialized = false
    
    init {
        initializeClassifier()
    }
    
    private fun initializeClassifier() {
        try {
            if (imaggaService.isConfigured()) {
                isInitialized = true
                Log.d(TAG, "Imagga classifier initialized successfully")
            } else {
                initializationError = "Imagga API not configured. Please set your API credentials."
                Log.e(TAG, initializationError!!)
                Log.i(TAG, imaggaService.getConfigurationInstructions())
            }
        } catch (e: Exception) {
            initializationError = "Failed to initialize Imagga classifier: ${e.message}"
            Log.e(TAG, initializationError!!, e)
        }
    }
    
    /**
     * Recognize an object in the given bitmap using Imagga API.
     * 
     * @param bitmap The input image bitmap
     * @return RecognitionResult containing either the classification result or an error
     */
    suspend fun recognizeObject(bitmap: Bitmap): TFLiteObjectClassifier.RecognitionResult {
        if (!isReady()) {
            val errorMsg = initializationError ?: "Imagga classifier not ready"
            Log.e(TAG, "Recognition failed: $errorMsg")
            return TFLiteObjectClassifier.RecognitionResult.Error(errorMsg)
        }
        
        return try {
            Log.d(TAG, "Starting Imagga API recognition...")
            
            // Call Imagga API on IO dispatcher
            val result = withContext(Dispatchers.IO) {
                imaggaService.recognizeImage(bitmap)
            }
            
            // Convert Imagga result to our standard format
            when (result) {
                is ImaggaResult.Success -> {
                    Log.d(TAG, "Imagga recognition successful: ${result.label} (${result.confidence})")
                    TFLiteObjectClassifier.RecognitionResult.Success(
                        label = result.label,
                        confidence = result.confidence
                    )
                }
                is ImaggaResult.Error -> {
                    Log.e(TAG, "Imagga recognition failed: ${result.message}")
                    TFLiteObjectClassifier.RecognitionResult.Error(result.message)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during Imagga recognition: ${e.message}", e)
            TFLiteObjectClassifier.RecognitionResult.Error("Recognition failed: ${e.message}")
        }
    }
    
    /**
     * Check if the classifier is properly initialized and configured.
     */
    fun isReady(): Boolean = isInitialized && imaggaService.isConfigured()
    
    /**
     * Get the initialization error message if any.
     */
    fun getInitializationError(): String? = initializationError
    
    /**
     * Attempt to reinitialize the classifier if it failed previously.
     */
    fun retryInitialization(): Boolean {
        if (isInitialized && imaggaService.isConfigured()) return true
        
        Log.d(TAG, "Retrying Imagga classifier initialization...")
        initializeClassifier()
        return isReady()
    }
    
    /**
     * Get configuration instructions for setting up Imagga API.
     */
    fun getConfigurationInstructions(): String {
        return imaggaService.getConfigurationInstructions()
    }
    
    /**
     * Check if this classifier requires internet connection.
     */
    fun requiresInternet(): Boolean = true
    
    /**
     * Get the type of this classifier.
     */
    fun getClassifierType(): String = "Imagga Cloud API"
    
    /**
     * Release resources when the classifier is no longer needed.
     * For Imagga API, there's no persistent connection to close.
     */
    fun close() {
        // No resources to clean up for API-based classifier
        Log.d(TAG, "Imagga classifier closed")
    }
    
    companion object {
        private const val TAG = "ImaggaObjectClassifier"
    }
}

/**
 * Extension function to check network connectivity.
 */
fun Context.isNetworkAvailable(): Boolean {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
    val activeNetwork = connectivityManager.activeNetwork ?: return false
    val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
    return networkCapabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
