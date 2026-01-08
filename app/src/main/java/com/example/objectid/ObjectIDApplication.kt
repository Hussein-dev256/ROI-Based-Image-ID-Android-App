package com.example.objectid

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * Application class for ObjectID app.
 * Handles global initialization and resources.
 */
class ObjectIDApplication : Application() {

    // Primary classifier - Imagga API
    lateinit var imaggaClassifier: ImaggaObjectClassifier
        private set

    // Fallback classifier - TensorFlow Lite
    lateinit var tfliteClassifier: TFLiteObjectClassifier
        private set

    // Track initialization status
    var isClassifierReady: Boolean = false
        private set

    var usingImaggaAPI: Boolean = false
        private set

    override fun onCreate() {
        super.onCreate()

        // Initialize TensorFlow Lite classifier with delay to allow native libraries to load
        initializeClassifierWithDelay()
    }

    private fun initializeClassifier() {
        try {
            // Try Imagga API first
            imaggaClassifier = ImaggaObjectClassifier(applicationContext)

            if (imaggaClassifier.isReady()) {
                isClassifierReady = true
                usingImaggaAPI = true
                Log.d(TAG, "Imagga API classifier initialized successfully")
            } else {
                Log.w(TAG, "Imagga API not configured, falling back to TensorFlow Lite")
                Log.i(TAG, imaggaClassifier.getConfigurationInstructions())

                // Fallback to TensorFlow Lite
                tfliteClassifier = TFLiteObjectClassifier(applicationContext)
                isClassifierReady = tfliteClassifier.isReady()
                usingImaggaAPI = false

                if (isClassifierReady) {
                    Log.d(TAG, "TensorFlow Lite fallback classifier initialized successfully")
                } else {
                    Log.e(TAG, "Both Imagga and TensorFlow Lite initialization failed")
                    Log.e(TAG, "Imagga error: ${imaggaClassifier.getInitializationError()}")
                    Log.e(TAG, "TensorFlow Lite error: ${tfliteClassifier.getInitializationError()}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize classifiers: ${e.message}", e)
            isClassifierReady = false
        }
    }

    /**
     * Initialize classifier with a delay to allow native libraries to fully load.
     * This helps prevent native address errors.
     */
    private fun initializeClassifierWithDelay() {
        // Try immediate initialization first
        initializeClassifier()

        // If it failed, try again after a delay
        if (!isClassifierReady) {
            Log.w(TAG, "Initial classifier initialization failed, retrying after delay...")
            Handler(Looper.getMainLooper()).postDelayed({
                if (!isClassifierReady) {
                    Log.d(TAG, "Attempting delayed classifier initialization...")
                    initializeClassifier()
                }
            }, 1000) // 1 second delay
        }
    }

    /**
     * Attempt to retry classifier initialization if it failed.
     * @return true if classifier is now ready, false otherwise
     */
    fun retryClassifierInitialization(): Boolean {
        if (isClassifierReady) return true

        Log.d(TAG, "Retrying global classifier initialization...")

        return try {
            // Try to reinitialize classifiers
            if (::imaggaClassifier.isInitialized) {
                isClassifierReady = imaggaClassifier.retryInitialization()
                if (isClassifierReady) {
                    usingImaggaAPI = true
                    Log.d(TAG, "Imagga classifier retry successful")
                    return true
                }
            }

            if (::tfliteClassifier.isInitialized) {
                isClassifierReady = tfliteClassifier.retryInitialization()
                if (isClassifierReady) {
                    usingImaggaAPI = false
                    Log.d(TAG, "TensorFlow Lite classifier retry successful")
                    return true
                }
            }

            // If retries failed, try full reinitialization
            if (!isClassifierReady) {
                initializeClassifier()
            }

            if (isClassifierReady) {
                Log.d(TAG, "Classifier retry successful")
            } else {
                Log.e(TAG, "All classifier retry attempts failed")
            }

            isClassifierReady
        } catch (e: Exception) {
            Log.e(TAG, "Exception during classifier retry: ${e.message}", e)
            false
        }
    }

    /**
     * Get detailed error information about classifier initialization.
     */
    fun getClassifierError(): String? {
        return if (usingImaggaAPI && ::imaggaClassifier.isInitialized) {
            imaggaClassifier.getInitializationError()
        } else if (!usingImaggaAPI && ::tfliteClassifier.isInitialized) {
            tfliteClassifier.getInitializationError()
        } else {
            "No classifier initialized"
        }
    }

    /**
     * Get the active classifier for recognition.
     */
    suspend fun recognizeObject(bitmap: android.graphics.Bitmap): TFLiteObjectClassifier.RecognitionResult {
        return if (usingImaggaAPI && ::imaggaClassifier.isInitialized) {
            imaggaClassifier.recognizeObject(bitmap)
        } else if (::tfliteClassifier.isInitialized) {
            tfliteClassifier.recognizeObject(bitmap)
        } else {
            TFLiteObjectClassifier.RecognitionResult.Error("No classifier available")
        }
    }

    /**
     * Get information about the active classifier.
     */
    fun getClassifierInfo(): String {
        return if (usingImaggaAPI) {
            "Using Imagga Cloud API"
        } else {
            "Using TensorFlow Lite (Local)"
        }
    }
    
    override fun onTerminate() {
        // Release classifier resources
        if (::imaggaClassifier.isInitialized) {
            imaggaClassifier.close()
        }
        if (::tfliteClassifier.isInitialized) {
            tfliteClassifier.close()
        }
        super.onTerminate()
    }
    
    companion object {
        private const val TAG = "ObjectIDApplication"
    }
}
