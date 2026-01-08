package com.example.objectid

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for handling object recognition logic.
 */
class RecognitionViewModel(application: Application) : AndroidViewModel(application) {

    // Use the application's recognition method which handles both Imagga and TensorFlow Lite
    private val app get() = getApplication<ObjectIDApplication>()
    
    // LiveData for UI updates
    private val _recognitionState = MutableLiveData<RecognitionUiState>()
    val recognitionState: LiveData<RecognitionUiState> = _recognitionState
    
    // Retry counter for recognition attempts
    private var retryCount = 0
    private val MAX_RETRIES = 3
    
    /**
     * Process an image for object recognition.
     *
     * @param bitmap The original image bitmap
     * @param annotationRect The rectangle drawn by the user around the object to recognize
     */
    fun processImage(bitmap: Bitmap, annotationRect: Rect) {
        viewModelScope.launch {
            _recognitionState.value = RecognitionUiState.Processing

            try {
                // Check if classifier is ready before processing
                if (!app.isClassifierReady) {
                    Log.w(TAG, "Classifier not ready, attempting retry...")
                    if (!app.retryClassifierInitialization()) {
                        val errorMsg = app.getClassifierError() ?: "Classifier initialization failed"
                        _recognitionState.value = RecognitionUiState.Error("Classifier not ready: $errorMsg")
                        return@launch
                    }
                }

                Log.d(TAG, "Using classifier: ${app.getClassifierInfo()}")

                // Perform processing on IO dispatcher
                val result = withContext(Dispatchers.IO) {
                    // Step 1: Crop to annotation
                    val cropped = ImageUtils.cropToAnnotation(bitmap, annotationRect)

                    // Step 2: For Imagga API, we can use the cropped image directly
                    // For TensorFlow Lite, we need to resize to 224x224
                    val processedImage = if (app.usingImaggaAPI) {
                        cropped // Imagga can handle various sizes
                    } else {
                        ImageUtils.resizeForModel(cropped) // TensorFlow Lite needs 224x224
                    }

                    // Step 3: Run recognition using the application's unified method
                    val recognitionResult = app.recognizeObject(processedImage)

                    // Recycle intermediate bitmaps to free memory
                    if (cropped != bitmap) {
                        ImageUtils.recycleBitmaps(cropped)
                    }
                    if (processedImage != cropped && processedImage != bitmap) {
                        ImageUtils.recycleBitmaps(processedImage)
                    }

                    recognitionResult
                }

                // Step 4: Process recognition result
                handleRecognitionResult(result)
            } catch (e: Exception) {
                Log.e(TAG, "Error during image processing: ${e.message}", e)
                _recognitionState.value = RecognitionUiState.Error("Processing failed: ${e.message}")
            }
        }
    }
    
    /**
     * Handle the recognition result based on confidence level.
     */
    private fun handleRecognitionResult(result: TFLiteObjectClassifier.RecognitionResult) {
        when (result) {
            is TFLiteObjectClassifier.RecognitionResult.Success -> {
                // Process based on confidence threshold
                when {
                    result.confidence > 0.75f -> {
                        // High confidence - show result
                        _recognitionState.value = RecognitionUiState.Success(
                            label = result.label.split(",")[0].trim(), // Clean up label
                            confidence = result.confidence
                        )
                        // Reset retry counter on success
                        retryCount = 0
                    }
                    result.confidence > 0.5f -> {
                        // Medium confidence - show result but suggest retry
                        _recognitionState.value = RecognitionUiState.PartialSuccess(
                            label = result.label.split(",")[0].trim(),
                            confidence = result.confidence
                        )
                    }
                    else -> {
                        // Low confidence - handle retry logic
                        retryCount++
                        if (retryCount < MAX_RETRIES) {
                            _recognitionState.value = RecognitionUiState.RetryNeeded(
                                "Low confidence (${result.confidence}). Try a clearer image."
                            )
                        } else {
                            _recognitionState.value = RecognitionUiState.Error(
                                "Unable to recognize object after multiple attempts."
                            )
                            // Reset retry counter
                            retryCount = 0
                        }
                    }
                }
            }
            is TFLiteObjectClassifier.RecognitionResult.Error -> {
                _recognitionState.value = RecognitionUiState.Error(result.message)
            }
        }
    }
    
    /**
     * Reset the recognition state.
     */
    fun resetState() {
        _recognitionState.value = RecognitionUiState.Idle
        retryCount = 0
    }

    /**
     * Manually retry model initialization.
     * This can be called when the user wants to retry after a model loading failure.
     */
    fun retryModelInitialization() {
        viewModelScope.launch {
            _recognitionState.value = RecognitionUiState.Processing

            try {
                val success = app.retryClassifierInitialization()

                if (success) {
                    _recognitionState.value = RecognitionUiState.Idle
                    Log.d(TAG, "Classifier initialization retry successful - now using: ${app.getClassifierInfo()}")
                } else {
                    val errorMsg = app.getClassifierError() ?: "Unknown initialization error"
                    _recognitionState.value = RecognitionUiState.Error("Classifier initialization failed: $errorMsg")
                    Log.e(TAG, "Classifier initialization retry failed: $errorMsg")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during classifier retry: ${e.message}", e)
                _recognitionState.value = RecognitionUiState.Error("Retry failed: ${e.message}")
            }
        }
    }
    
    /**
     * Clean up resources when the ViewModel is cleared.
     */
    override fun onCleared() {
        super.onCleared()
        // Resources are managed by the Application class
    }
    
    companion object {
        private const val TAG = "RecognitionViewModel"
    }
}

/**
 * UI state for recognition process.
 */
sealed class RecognitionUiState {
    object Idle : RecognitionUiState()
    object Processing : RecognitionUiState()
    data class Success(val label: String, val confidence: Float) : RecognitionUiState()
    data class PartialSuccess(val label: String, val confidence: Float) : RecognitionUiState()
    data class RetryNeeded(val message: String) : RecognitionUiState()
    data class Error(val message: String) : RecognitionUiState()
}
