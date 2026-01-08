package com.example.objectid

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.objectid.databinding.ActivityMainBinding
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: RecognitionViewModel
    
    private var currentBitmap: Bitmap? = null

    companion object {
        private const val TAG = "MainActivity"
    }
    
    // ActivityResult handlers for camera and gallery
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as? Bitmap
            if (imageBitmap != null) {
                handleNewImage(imageBitmap)
            } else {
                showToast("Failed to capture image")
            }
        }
    }
    
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val selectedImageUri = result.data?.data
            if (selectedImageUri != null) {
                try {
                    val imageBitmap = loadBitmapFromUri(selectedImageUri)
                    handleNewImage(imageBitmap)
                } catch (e: IOException) {
                    showToast("Failed to load image: ${e.message}")
                }
            }
        }
    }
    
    // Permission launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            showToast("Camera permission is required for this feature")
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize view binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[RecognitionViewModel::class.java]
        
        // Set up click listeners
        setupClickListeners()
        
        // Observe recognition state
        observeRecognitionState()
        
        // Set up annotation view listener
        binding.annotationView.setOnAnnotationCompleteListener { rect ->
            binding.btnRecognize.isEnabled = true
        }

        // Set up back button listener
        binding.btnBack.setOnClickListener {
            onBackPressed()
        }
        
        // Hide back button if we're at the root of the back stack
        if (isTaskRoot) {
            binding.btnBack.visibility = View.GONE
        } else {
            binding.btnBack.visibility = View.VISIBLE
        }

        // Check model initialization status on startup
        checkModelInitialization()
    }
    
    private fun setupClickListeners() {
        // Capture image button
        binding.btnCapture.setOnClickListener {
            checkCameraPermissionAndOpen()
        }
        
        // Gallery button
        binding.btnGallery.setOnClickListener {
            openGallery()
        }
        
        // Annotate button
        binding.btnAnnotate.setOnClickListener {
            startAnnotation()
        }
        
        // Recognize button
        binding.btnRecognize.setOnClickListener {
            recognizeObject()
        }
        
        // Retry button
        binding.btnRetry.setOnClickListener {
            handleRetryAction()
        }
        
        // Reset button to return to default app state
        binding.btnReset.setOnClickListener {
            resetAppState()
        }
    }
    
    private fun observeRecognitionState() {
        viewModel.recognitionState.observe(this) { state ->
            when (state) {
                is RecognitionUiState.Idle -> {
                    showProgressBar(false)
                    binding.resultContainer.visibility = View.GONE
                    binding.btnRetry.visibility = View.GONE
                }
                is RecognitionUiState.Processing -> {
                    showProgressBar(true)
                    binding.resultContainer.visibility = View.GONE
                    binding.btnRetry.visibility = View.GONE
                }
                is RecognitionUiState.Success -> {
                    showProgressBar(false)
                    showResult(state.label, state.confidence)
                    binding.btnRetry.visibility = View.GONE
                }
                is RecognitionUiState.PartialSuccess -> {
                    showProgressBar(false)
                    showResult(state.label, state.confidence)
                    binding.btnRetry.visibility = View.VISIBLE
                    showToast("Medium confidence. Try again for a better result.")
                }
                is RecognitionUiState.RetryNeeded -> {
                    showProgressBar(false)
                    binding.resultContainer.visibility = View.GONE
                    binding.btnRetry.visibility = View.VISIBLE
                    showToast(state.message)
                }
                is RecognitionUiState.Error -> {
                    showProgressBar(false)
                    binding.resultContainer.visibility = View.GONE
                    binding.btnRetry.visibility = View.VISIBLE

                    // Check if this is a classifier loading error
                    if (state.message.contains("not loaded") ||
                        state.message.contains("initialization failed") ||
                        state.message.contains("not configured")) {
                        showToast("${state.message}. Tap retry to reload the classifier.")
                    } else {
                        showToast(state.message)
                    }
                }
            }
        }
    }

    private fun checkModelInitialization() {
        val app = application as ObjectIDApplication
        if (!app.isClassifierReady) {
            val errorMsg = app.getClassifierError() ?: "Unknown initialization error"
            Log.w(TAG, "Classifier not ready on startup: $errorMsg")
            showToast("Classifier initialization issue detected. You may need to retry when recognizing objects.")
        } else {
            Log.d(TAG, "Classifier initialized successfully on startup: ${app.getClassifierInfo()}")
        }
    }

    private fun checkCameraPermissionAndOpen() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == 
                    PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA) -> {
                showToast("Camera permission is required for this feature")
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }
    
    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraLauncher.launch(intent)
    }
    
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }
    
    private fun handleNewImage(bitmap: Bitmap) {
        // Store the bitmap and update UI
        currentBitmap = bitmap
        binding.imageView.setImageBitmap(bitmap)
        
        // Enable annotation button
        binding.btnAnnotate.isEnabled = true
        
        // Reset other UI elements
        binding.annotationView.clearAnnotation()
        binding.annotationView.visibility = View.GONE
        binding.annotationInstructions.visibility = View.GONE
        binding.btnRecognize.isEnabled = false
        binding.resultContainer.visibility = View.GONE
    }
    
    private fun startAnnotation() {
        if (currentBitmap != null) {
            // Show annotation view and instructions
            binding.annotationView.visibility = View.VISIBLE
            binding.annotationInstructions.visibility = View.VISIBLE
            
            // Clear previous annotations
            binding.annotationView.clearAnnotation()
            
            // Disable recognize button until annotation is complete
            binding.btnRecognize.isEnabled = false
        }
    }
    
    private fun recognizeObject() {
        val bitmap = currentBitmap
        if (bitmap != null) {
            // Get annotation rectangle
            val annotationRect = binding.annotationView.getAnnotationRect()
            
            // Process the image
            viewModel.processImage(bitmap, annotationRect)
        }
    }
    
    private fun handleRetryAction() {
        // Check the current state to determine what kind of retry is needed
        val currentState = viewModel.recognitionState.value

        if (currentState is RecognitionUiState.Error &&
            (currentState.message.contains("not loaded") ||
             currentState.message.contains("initialization failed") ||
             currentState.message.contains("not configured"))) {
            // This is a classifier initialization error - retry classifier loading
            viewModel.retryModelInitialization()
        } else {
            // This is a regular recognition error - reset state for new attempt
            resetState()
        }
    }

    private fun resetState() {
        // Clear annotation
        binding.annotationView.clearAnnotation()

        // Reset ViewModel state
        viewModel.resetState()

        // If we have an image, enable annotation button
        binding.btnAnnotate.isEnabled = currentBitmap != null
        binding.btnRecognize.isEnabled = false
    }
    
    private fun showProgressBar(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }
    
    private fun showResult(label: String, confidence: Float) {
        binding.resultContainer.visibility = View.VISIBLE

        // Check if the label contains multiple results (has line breaks)
        if (label.contains("\n")) {
            // Multiple results from Imagga API - don't repeat the title
            binding.tvRecognitionResult.text = label
            binding.tvConfidence.text = "Top confidence: ${(confidence * 100).toInt()}%"
        } else {
            // Single result (from TensorFlow Lite or single Imagga result)
            binding.tvRecognitionResult.text = getString(R.string.recognition_result, label)
            binding.tvConfidence.text = getString(R.string.confidence_score, confidence)
        }
    }
    
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    /**
     * Resets the app to its default state, clearing any image,
     * annotations, and results to start fresh
     */
    private fun resetAppState() {
        // Clear the bitmap and update UI
        currentBitmap = null
        binding.imageView.setImageDrawable(null)
        binding.annotationView.clearAnnotation()
        
        // Hide containers
        binding.resultContainer.visibility = View.GONE
        binding.btnRetry.visibility = View.GONE
        binding.annotationView.visibility = View.GONE
        binding.annotationInstructions.visibility = View.GONE
        
        // Reset button states
        binding.btnRecognize.isEnabled = false
        binding.btnAnnotate.isEnabled = false
        
        // Show appropriate controls
        binding.btnCapture.isEnabled = true
        binding.btnGallery.isEnabled = true
        
        // Optional: provide feedback to user
        showToast("Ready for new recognition")
    }
    
    private fun loadBitmapFromUri(uri: Uri): Bitmap {
        val inputStream = contentResolver.openInputStream(uri)
        return BitmapFactory.decodeStream(inputStream)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Recycle bitmaps to avoid memory leaks
        currentBitmap?.recycle()
        currentBitmap = null
    }
}
