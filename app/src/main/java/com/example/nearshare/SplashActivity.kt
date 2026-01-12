package com.example.nearshare

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.example.nearshare.databinding.ActivitySplashBinding

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Load Custom Animations
        // Ensure you created 'slide_up.xml' in res/anim as shown above.
        // If you didn't, change R.anim.slide_up back to android.R.anim.slide_in_left
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)
        val fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)

        // 2. Start Animations
        binding.ivLogo.startAnimation(slideUp)

        // Delay the text slightly for a nicer effect
        fadeIn.startOffset = 500
        binding.tvTitle.startAnimation(fadeIn)
        binding.tvSubtitle.startAnimation(fadeIn)

        // 3. Navigate
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, 2200) // Increased slightly to let animation finish
    }
}