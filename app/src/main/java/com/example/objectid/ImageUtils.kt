package com.example.objectid

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect

/**
 * Utility class for image processing operations related to object recognition.
 */
object ImageUtils {
    
    /**
     * Crop a bitmap to the specified annotation rectangle.
     * 
     * @param original The original bitmap
     * @param bbox The bounding box rectangle to crop to
     * @return The cropped bitmap
     */
    fun cropToAnnotation(original: Bitmap, bbox: Rect): Bitmap {
        // Ensure the rectangle is within the bitmap bounds
        val safeRect = Rect(
            bbox.left.coerceIn(0, original.width),
            bbox.top.coerceIn(0, original.height),
            bbox.right.coerceIn(0, original.width),
            bbox.bottom.coerceIn(0, original.height)
        )
        
        // Ensure width and height are at least 1 pixel
        if (safeRect.width() < 1 || safeRect.height() < 1) {
            return original // Cannot crop, return original
        }
        
        return try {
            Bitmap.createBitmap(
                original,
                safeRect.left,
                safeRect.top,
                safeRect.width(),
                safeRect.height()
            )
        } catch (e: Exception) {
            // In case of OOM errors, return the original
            original
        }
    }
    
    /**
     * Resize a bitmap to the dimensions required by the model (224x224).
     * 
     * @param input The input bitmap to resize
     * @return The resized bitmap
     */
    fun resizeForModel(input: Bitmap): Bitmap {
        return try {
            Bitmap.createScaledBitmap(input, MODEL_INPUT_SIZE, MODEL_INPUT_SIZE, true)
        } catch (e: OutOfMemoryError) {
            // If we run out of memory, try to downsample first
            downsampleBitmap(input)
        }
    }
    
    /**
     * Downsample a bitmap by factor of 2 until it's small enough to be processed.
     * This is a fallback for low-memory devices.
     * 
     * @param input The input bitmap to downsample
     * @return The downsampled and resized bitmap
     */
    private fun downsampleBitmap(input: Bitmap): Bitmap {
        var currentBitmap = input
        var scale = 0.5f
        
        while (true) {
            try {
                val downsampled = scaleBitmap(currentBitmap, scale)
                // If successful, resize to required dimensions
                return Bitmap.createScaledBitmap(
                    downsampled, 
                    MODEL_INPUT_SIZE, 
                    MODEL_INPUT_SIZE, 
                    true
                )
            } catch (e: OutOfMemoryError) {
                // Still OOM, reduce scale further
                if (scale <= 0.1f) {
                    // Can't downsample any further, just return a tiny bitmap
                    return Bitmap.createBitmap(MODEL_INPUT_SIZE, MODEL_INPUT_SIZE, Bitmap.Config.ARGB_8888)
                }
                scale *= 0.5f
            }
        }
    }
    
    /**
     * Scale a bitmap by the given factor.
     * 
     * @param bitmap The bitmap to scale
     * @param scaleFactor The factor to scale by (0.5 = half size)
     * @return The scaled bitmap
     */
    private fun scaleBitmap(bitmap: Bitmap, scaleFactor: Float): Bitmap {
        val matrix = Matrix()
        matrix.postScale(scaleFactor, scaleFactor)
        return Bitmap.createBitmap(
            bitmap, 0, 0, 
            bitmap.width, bitmap.height, 
            matrix, true
        )
    }
    
    /**
     * Recycle bitmaps safely to avoid memory leaks.
     * 
     * @param bitmaps Array of bitmaps to recycle
     */
    fun recycleBitmaps(vararg bitmaps: Bitmap?) {
        bitmaps.forEach { bitmap ->
            if (bitmap != null && !bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
    }
    
    // Constants
    const val MODEL_INPUT_SIZE = 224 // Model input size (224x224)
}
