package com.example.objectid

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.classifier.ImageClassifier
import java.io.IOException

/**
 * Helper class for TensorFlow Lite object classification using MobileNetV3-Small model.
 * This class handles loading the model from assets, image preprocessing, and inference.
 */
class TFLiteObjectClassifier(private val context: Context) {

    private var classifier: ImageClassifier? = null
    private var simpleClassifier: SimpleTFLiteClassifier? = null
    private var initializationError: String? = null
    private var isInitialized = false
    private var usingSimpleClassifier = false

    init {
        initializeClassifier()
    }

    private fun initializeClassifier() {
        try {
            // First, verify the model file exists in assets
            if (!verifyModelExists()) {
                initializationError = "Model file 'MobileNet-v3-Large-Quantized.tflite' not found in assets"
                Log.e(TAG, initializationError!!)
                return
            }

            // Try multiple initialization approaches to handle native library issues
            classifier = tryInitializeWithFallback()

            if (classifier != null || usingSimpleClassifier) {
                isInitialized = true
                val method = if (usingSimpleClassifier) "simple classifier" else "task vision API"
                Log.d(TAG, "TFLite classifier initialized successfully using $method")
            } else {
                initializationError = "All initialization methods failed"
                Log.e(TAG, initializationError!!)
            }
        } catch (e: Exception) {
            initializationError = "Failed to initialize classifier: ${e.message}"
            Log.e(TAG, initializationError!!, e)

            // Log more specific error information
            when (e) {
                is IOException -> Log.e(TAG, "IO Error - Check if model file is accessible")
                is IllegalArgumentException -> Log.e(TAG, "Invalid model file or options")
                is UnsatisfiedLinkError -> Log.e(TAG, "Native library loading error - this is the 'native address' issue")
                is RuntimeException -> {
                    if (e.message?.contains("native") == true) {
                        Log.e(TAG, "Native runtime error - TensorFlow Lite native libraries issue")
                    } else {
                        Log.e(TAG, "Runtime error during initialization")
                    }
                }
                else -> Log.e(TAG, "Unexpected error during initialization: ${e.javaClass.simpleName}")
            }
        }
    }

    /**
     * Try different initialization approaches to handle native library issues.
     */
    private fun tryInitializeWithFallback(): ImageClassifier? {
        // Method 1: Standard initialization with basic options
        try {
            Log.d(TAG, "Trying standard initialization...")
            val basicOptions = ImageClassifier.ImageClassifierOptions.builder()
                .setMaxResults(3)
                .setScoreThreshold(0.3f)
                .build()

            return ImageClassifier.createFromFileAndOptions(
                context,
                "MobileNet-v3-Large-Quantized.tflite",
                basicOptions
            )
        } catch (e: Exception) {
            Log.w(TAG, "Standard initialization failed: ${e.message}")
        }

        // Method 2: Try with minimal options
        try {
            Log.d(TAG, "Trying minimal options initialization...")
            val minimalOptions = ImageClassifier.ImageClassifierOptions.builder()
                .setMaxResults(1)
                .build()

            return ImageClassifier.createFromFileAndOptions(
                context,
                "MobileNet-v3-Large-Quantized.tflite",
                minimalOptions
            )
        } catch (e: Exception) {
            Log.w(TAG, "Minimal options initialization failed: ${e.message}")
        }

        // Method 3: Try creating from file without custom options
        try {
            Log.d(TAG, "Trying default options initialization...")
            return ImageClassifier.createFromFile(context, "MobileNet-v3-Large-Quantized.tflite")
        } catch (e: Exception) {
            Log.w(TAG, "Default options initialization failed: ${e.message}")
        }

        // Method 4: Try simple classifier as fallback
        try {
            Log.d(TAG, "Trying simple classifier fallback...")
            simpleClassifier = SimpleTFLiteClassifier(context)
            if (simpleClassifier?.isReady() == true) {
                usingSimpleClassifier = true
                Log.d(TAG, "Simple classifier fallback successful")
                return null // We'll use simpleClassifier instead
            }
        } catch (e: Exception) {
            Log.w(TAG, "Simple classifier fallback failed: ${e.message}")
        }

        Log.e(TAG, "All initialization methods failed")
        return null
    }

    /**
     * Verify that the model file exists in the assets folder.
     */
    private fun verifyModelExists(): Boolean {
        return try {
            context.assets.open("MobileNet-v3-Large-Quantized.tflite").use {
                true
            }
        } catch (e: IOException) {
            Log.e(TAG, "Model file verification failed", e)
            false
        }
    }

    /**
     * Attempt to reinitialize the classifier if it failed previously.
     */
    fun retryInitialization(): Boolean {
        if (isInitialized) return true

        Log.d(TAG, "Retrying classifier initialization...")
        initializeClassifier()
        return isInitialized
    }

    /**
     * Check if the classifier is properly initialized.
     */
    fun isReady(): Boolean = isInitialized && (classifier != null || usingSimpleClassifier)

    /**
     * Get the initialization error message if any.
     */
    fun getInitializationError(): String? = initializationError
    
    /**
     * Recognize an object in the given bitmap.
     *
     * @param bitmap The input image bitmap
     * @return RecognitionResult containing either the classification result or an error
     */
    fun recognizeObject(bitmap: Bitmap): RecognitionResult {
        // Check if classifier is properly initialized
        if (!isReady()) {
            val errorMsg = initializationError ?: "Model not loaded - unknown error"
            Log.e(TAG, "Recognition failed: $errorMsg")
            return RecognitionResult.Error(errorMsg)
        }

        return try {
            if (usingSimpleClassifier) {
                // Use simple classifier
                simpleClassifier!!.recognizeObject(bitmap)
            } else {
                // Use task vision API classifier
                val tensorImage = TensorImage.fromBitmap(bitmap)
                val results = classifier!!.classify(tensorImage)

                if (results.isNotEmpty() && results[0].categories.isNotEmpty()) {
                    val topCategory = results[0].categories[0]
                    RecognitionResult.Success(
                        label = topCategory.label,
                        confidence = topCategory.score
                    )
                } else {
                    RecognitionResult.Error("No object recognized in the image")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during object recognition: ${e.message}", e)
            RecognitionResult.Error("Recognition failed: ${e.message}")
        }
    }
    
    /**
     * Release resources when the classifier is no longer needed.
     */
    fun close() {
        classifier?.close()
        classifier = null
        simpleClassifier?.close()
        simpleClassifier = null
        isInitialized = false
        usingSimpleClassifier = false
    }
    
    sealed class RecognitionResult {
        data class Success(val label: String, val confidence: Float) : RecognitionResult()
        data class Error(val message: String) : RecognitionResult()
    }
    
    companion object {
        private const val TAG = "TFLiteObjectClassifier"
    }
}

