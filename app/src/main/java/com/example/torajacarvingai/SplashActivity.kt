package com.example.torajacarvingai

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Menunda selama 3000 milidetik (3 detik) lalu pindah ke halaman berikutnya
        Handler(Looper.getMainLooper()).postDelayed({
            val sharedPreferences = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val isFirstRun = sharedPreferences.getBoolean("is_first_run", true)

            val intent = if (isFirstRun) {
                Intent(this, OnboardingActivity::class.java)
            } else {
                Intent(this, MainMenuActivity::class.java)
            }
            startActivity(intent)
            finish() // Tutup splash screen agar tidak bisa di-back
        }, 3000)
    }
}