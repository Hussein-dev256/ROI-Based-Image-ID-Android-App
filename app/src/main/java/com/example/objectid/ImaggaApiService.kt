package com.example.objectid

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Service class for interacting with the Imagga API for image recognition.
 * 
 * To use this service, you need to:
 * 1. Sign up at https://imagga.com/
 * 2. Get your API key and secret from the dashboard
 * 3. Replace the placeholder credentials below
 */
class ImaggaApiService {
    
    companion object {
        private const val TAG = "ImaggaApiService"
        private const val BASE_URL = "https://api.imagga.com/v2"
        private const val TAGS_ENDPOINT = "$BASE_URL/tags"
        
        // TODO: Replace with your actual Imagga API credentials
        // Get these from https://imagga.com/profile/dashboard
        private const val API_KEY = "acc_6b4609e39946930"
        
        private const val API_SECRET = "a6acf2ddc83e5781568fbe9d555a5405"
        
        // Request timeout settings
        private const val CONNECT_TIMEOUT = 30L
        private const val READ_TIMEOUT = 60L
        private const val WRITE_TIMEOUT = 60L

        // Number of results to return
        private const val MAX_RESULTS = 3
    }
    
    private val client: OkHttpClient
    private val gson = Gson()
    
    init {
        // Create logging interceptor for debugging
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            Log.d(TAG, "HTTP: $message")
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        client = OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val original = chain.request()
                val credentials = Credentials.basic(API_KEY, API_SECRET)
                val request = original.newBuilder()
                    .header("Authorization", credentials)
                    .build()
                chain.proceed(request)
            }
            .build()
    }
    
    /**
     * Recognize objects in the given bitmap using Imagga API.
     * 
     * @param bitmap The image to analyze
     * @return ImaggaResult containing the recognition results or error
     */
    suspend fun recognizeImage(bitmap: Bitmap): ImaggaResult {
        return try {
            // Check if API credentials are configured
            if (API_KEY == "YOUR_API_KEY_HERE" || API_SECRET == "YOUR_API_SECRET_HERE") {
                return ImaggaResult.Error("Imagga API credentials not configured. Please set your API key and secret.")
            }
            
            // Try binary upload first (often more reliable than base64)
            val requestBody = createBinaryRequestBody(bitmap)

            // Create request with proper headers
            val request = Request.Builder()
                .url(TAGS_ENDPOINT)
                .post(requestBody)
                .addHeader("Accept", "application/json")
                .build()

            Log.d(TAG, "Sending request to Imagga API: $TAGS_ENDPOINT")
            Log.d(TAG, "Request headers: ${request.headers}")

            // Execute request
            val response = client.newCall(request).execute()

            Log.d(TAG, "Response code: ${response.code}")
            Log.d(TAG, "Response headers: ${response.headers}")

            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    Log.d(TAG, "Response body: $responseBody")
                    parseResponse(responseBody)
                } else {
                    ImaggaResult.Error("Empty response from Imagga API")
                }
            } else {
                val errorBody = response.body?.string() ?: "Unknown error"
                Log.e(TAG, "API request failed: ${response.code} - ${response.message}")
                Log.e(TAG, "Error body: $errorBody")

                // Provide more specific error messages
                val errorMessage = when (response.code) {
                    400 -> "Bad request - Check image format and size. Error: $errorBody"
                    401 -> "Unauthorized - Check your API credentials"
                    403 -> "Forbidden - Check your API permissions"
                    429 -> "Rate limit exceeded - Too many requests"
                    500 -> "Server error - Try again later"
                    else -> "API request failed: ${response.code} - $errorBody"
                }

                ImaggaResult.Error(errorMessage)
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network error: ${e.message}", e)
            ImaggaResult.Error("Network error: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error: ${e.message}", e)
            ImaggaResult.Error("Unexpected error: ${e.message}")
        }
    }
    
    /**
     * Convert bitmap to base64 string for API upload.
     */
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()

        // Resize bitmap if too large (Imagga has size limits)
        val maxDimension = 1024
        val scaledBitmap = if (bitmap.width > maxDimension || bitmap.height > maxDimension) {
            val scale = maxDimension.toFloat() / maxOf(bitmap.width, bitmap.height)
            val newWidth = (bitmap.width * scale).toInt()
            val newHeight = (bitmap.height * scale).toInt()
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } else {
            bitmap
        }

        // Compress bitmap to JPEG with good quality
        val quality = 85
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream)

        val byteArray = byteArrayOutputStream.toByteArray()
        Log.d(TAG, "Image size after compression: ${byteArray.size} bytes")

        // Clean up scaled bitmap if it's different from original
        if (scaledBitmap != bitmap) {
            scaledBitmap.recycle()
        }

        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    /**
     * Create binary request body for direct image upload.
     */
    private fun createBinaryRequestBody(bitmap: Bitmap): RequestBody {
        val byteArrayOutputStream = ByteArrayOutputStream()

        // Resize bitmap if too large
        val maxDimension = 1024
        val scaledBitmap = if (bitmap.width > maxDimension || bitmap.height > maxDimension) {
            val scale = maxDimension.toFloat() / maxOf(bitmap.width, bitmap.height)
            val newWidth = (bitmap.width * scale).toInt()
            val newHeight = (bitmap.height * scale).toInt()
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } else {
            bitmap
        }

        // Compress to JPEG
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, byteArrayOutputStream)
        val imageBytes = byteArrayOutputStream.toByteArray()

        Log.d(TAG, "Binary image size: ${imageBytes.size} bytes")

        // Clean up scaled bitmap if different from original
        if (scaledBitmap != bitmap) {
            scaledBitmap.recycle()
        }

        // Create multipart body with binary data
        return MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "image",
                "image.jpg",
                imageBytes.toRequestBody("image/jpeg".toMediaType())
            )
            .build()
    }

    /**
     * Parse the API response and extract recognition results.
     */
    private fun parseResponse(responseBody: String): ImaggaResult {
        return try {
            Log.d(TAG, "Parsing API response...")
            val response = gson.fromJson(responseBody, ImaggaResponse::class.java)

            if (response.result?.tags?.isNotEmpty() == true) {
                // Get top results (configurable number)
                val topTags = response.result.tags.take(MAX_RESULTS)

                // Create a combined label with all results
                val labels = topTags.mapIndexed { index, tag ->
                    val confidence = (tag.confidence / 100f * 100).toInt() // Convert to percentage
                    "${index + 1}. ${tag.tag.en} (${confidence}%)"
                }.joinToString("\n")

                // Use the highest confidence for the overall result
                val topConfidence = topTags[0].confidence / 100f

                Log.d(TAG, "Found ${topTags.size} results: $labels")

                ImaggaResult.Success(
                    label = labels,
                    confidence = topConfidence
                )
            } else {
                ImaggaResult.Error("No objects recognized in the image")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing response: ${e.message}", e)
            ImaggaResult.Error("Error parsing API response: ${e.message}")
        }
    }
    
    /**
     * Check if the service is properly configured.
     */
    fun isConfigured(): Boolean {
        return API_KEY != "YOUR_API_KEY_HERE" && API_SECRET != "YOUR_API_SECRET_HERE"
    }
    
    /**
     * Get configuration instructions for the user.
     */
    fun getConfigurationInstructions(): String {
        return """
            To use Imagga API:
            1. Sign up at https://imagga.com/
            2. Go to your dashboard: https://imagga.com/profile/dashboard
            3. Copy your API Key and API Secret
            4. Replace the placeholder values in ImaggaApiService.kt
        """.trimIndent()
    }
}

/**
 * Sealed class representing the result of an Imagga API call.
 */
sealed class ImaggaResult {
    data class Success(val label: String, val confidence: Float) : ImaggaResult()
    data class Error(val message: String) : ImaggaResult()
}

/**
 * Data classes for parsing Imagga API response.
 */
data class ImaggaResponse(
    @SerializedName("result") val result: ImaggaResultData?
)

data class ImaggaResultData(
    @SerializedName("tags") val tags: List<ImaggaTag>
)

data class ImaggaTag(
    @SerializedName("confidence") val confidence: Float,
    @SerializedName("tag") val tag: ImaggaTagName
)

data class ImaggaTagName(
    @SerializedName("en") val en: String
)
