package com.example.objectid

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * Simple TensorFlow Lite classifier that uses the core Interpreter API
 * instead of the Task Vision API to avoid native library issues.
 */
class SimpleTFLiteClassifier(private val context: Context) {
    
    private var interpreter: Interpreter? = null
    private var initializationError: String? = null
    private var isInitialized = false
    
    // Model input/output specifications
    private val INPUT_SIZE = 224
    private val PIXEL_SIZE = 3
    private val IMAGE_MEAN = 127.5f
    private val IMAGE_STD = 127.5f
    private val MAX_RESULTS = 3
    
    init {
        initializeInterpreter()
    }
    
    private fun initializeInterpreter() {
        try {
            // Load model from assets
            val modelBuffer = loadModelFile()
            
            // Create interpreter options
            val options = Interpreter.Options()
            options.setNumThreads(4)
            
            // Create interpreter
            interpreter = Interpreter(modelBuffer, options)
            isInitialized = true
            Log.d(TAG, "Simple TFLite interpreter initialized successfully")
            
        } catch (e: Exception) {
            initializationError = "Failed to initialize simple interpreter: ${e.message}"
            Log.e(TAG, initializationError!!, e)
            
            when (e) {
                is IOException -> Log.e(TAG, "IO Error loading model file")
                is IllegalArgumentException -> Log.e(TAG, "Invalid model file")
                else -> Log.e(TAG, "Unexpected error: ${e.javaClass.simpleName}")
            }
        }
    }
    
    private fun loadModelFile(): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd("MobileNet-v3-Large-Quantized.tflite")
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }
    
    /**
     * Recognize an object in the given bitmap using the simple interpreter.
     */
    fun recognizeObject(bitmap: Bitmap): TFLiteObjectClassifier.RecognitionResult {
        if (!isReady()) {
            val errorMsg = initializationError ?: "Simple interpreter not loaded"
            return TFLiteObjectClassifier.RecognitionResult.Error(errorMsg)
        }
        
        return try {
            // Preprocess image
            val inputBuffer = preprocessImage(bitmap)
            
            // Prepare output buffer
            val outputBuffer = Array(1) { FloatArray(1000) } // ImageNet has 1000 classes
            
            // Run inference
            interpreter!!.run(inputBuffer, outputBuffer)
            
            // Process results
            processResults(outputBuffer[0])
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during simple recognition: ${e.message}", e)
            TFLiteObjectClassifier.RecognitionResult.Error("Simple recognition failed: ${e.message}")
        }
    }
    
    private fun preprocessImage(bitmap: Bitmap): ByteBuffer {
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
        
        val byteBuffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * PIXEL_SIZE)
        byteBuffer.order(ByteOrder.nativeOrder())
        
        val intValues = IntArray(INPUT_SIZE * INPUT_SIZE)
        resizedBitmap.getPixels(intValues, 0, resizedBitmap.width, 0, 0, resizedBitmap.width, resizedBitmap.height)
        
        var pixel = 0
        for (i in 0 until INPUT_SIZE) {
            for (j in 0 until INPUT_SIZE) {
                val pixelValue = intValues[pixel++]
                
                // Extract RGB values and normalize
                val r = ((pixelValue shr 16) and 0xFF)
                val g = ((pixelValue shr 8) and 0xFF)
                val b = (pixelValue and 0xFF)
                
                // Normalize to [-1, 1] range
                byteBuffer.putFloat((r - IMAGE_MEAN) / IMAGE_STD)
                byteBuffer.putFloat((g - IMAGE_MEAN) / IMAGE_STD)
                byteBuffer.putFloat((b - IMAGE_MEAN) / IMAGE_STD)
            }
        }
        
        return byteBuffer
    }
    
    private fun processResults(output: FloatArray): TFLiteObjectClassifier.RecognitionResult {
        // Find the index with highest confidence
        var maxIndex = 0
        var maxConfidence = output[0]
        
        for (i in 1 until output.size) {
            if (output[i] > maxConfidence) {
                maxConfidence = output[i]
                maxIndex = i
            }
        }
        
        // Apply softmax to get probability
        val confidence = softmax(output)[maxIndex]
        
        return if (confidence > 0.3f) {
            // Use a simple label mapping (you could load from imagenet_labels.txt)
            val label = "Object_$maxIndex" // Simplified labeling
            TFLiteObjectClassifier.RecognitionResult.Success(label, confidence)
        } else {
            TFLiteObjectClassifier.RecognitionResult.Error("Low confidence: $confidence")
        }
    }
    
    private fun softmax(input: FloatArray): FloatArray {
        val result = FloatArray(input.size)
        var sum = 0.0f
        
        // Find max for numerical stability
        val max = input.maxOrNull() ?: 0f
        
        // Calculate exp and sum
        for (i in input.indices) {
            result[i] = kotlin.math.exp((input[i] - max).toDouble()).toFloat()
            sum += result[i]
        }
        
        // Normalize
        for (i in result.indices) {
            result[i] /= sum
        }
        
        return result
    }
    
    fun isReady(): Boolean = isInitialized && interpreter != null
    
    fun getInitializationError(): String? = initializationError
    
    fun retryInitialization(): Boolean {
        if (isInitialized) return true
        
        Log.d(TAG, "Retrying simple interpreter initialization...")
        initializeInterpreter()
        return isInitialized
    }
    
    fun close() {
        interpreter?.close()
        interpreter = null
        isInitialized = false
    }
    
    companion object {
        private const val TAG = "SimpleTFLiteClassifier"
    }
}
