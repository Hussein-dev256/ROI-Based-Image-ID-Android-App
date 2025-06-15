package com.example.objectid

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    companion object {
        private const val SPLASH_DURATION = 4500L // 4.5 seconds
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            setContentView(R.layout.activity_splash)
            
            // Find views safely with null checks
            val logoImageView = findViewById<ImageView>(R.id.ivLogo)
            val appNameTextView = findViewById<TextView>(R.id.tvAppName)
            
            try {
                // Load animation safely
                val fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in)
                
                // Apply animations with null checks
                logoImageView?.startAnimation(fadeInAnimation)
                appNameTextView?.startAnimation(fadeInAnimation)
            } catch (e: Exception) {
                // Animation failed, but we can continue without animations
                Log.e("SplashActivity", "Animation failed: ${e.message}")
            }
            
            // Navigate to MainActivity after SPLASH_DURATION
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    val intent = Intent(this@SplashActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()  // Close the splash activity so it's not in the back stack
                } catch (e: Exception) {
                    Log.e("SplashActivity", "Failed to start MainActivity: ${e.message}")
                    finish() // Still finish this activity
                }
            }, SPLASH_DURATION)
            
        } catch (e: Exception) {
            // If anything fails in splash screen, go directly to MainActivity
            Log.e("SplashActivity", "Splash screen failed to load: ${e.message}")
            try {
                startActivity(Intent(this, MainActivity::class.java))
            } catch (e2: Exception) {
                Log.e("SplashActivity", "Both splash and direct MainActivity navigation failed")
            }
            finish()
        }
    }
}
