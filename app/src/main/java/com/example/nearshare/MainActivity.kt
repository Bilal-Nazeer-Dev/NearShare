package com.example.nearshare

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import com.example.nearshare.databinding.ActivityMainBinding
import com.example.nearshare.utils.PermissionHelper

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load Dark Mode
        val sharedPref = getSharedPreferences("NearShareSettings", Context.MODE_PRIVATE)
        if (sharedPref.getBoolean("DARK_MODE", false)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. CRITICAL: Request Permissions Immediately
        if (!PermissionHelper.hasPermissions(this)) {
            PermissionHelper.requestPermissions(this, 100)
        }

        setupListeners()
    }

    private fun setupListeners() {
        // Send Button - Open Discovery in SEND mode
        binding.cardSend.setOnClickListener {
            val intent = Intent(this, DeviceDiscoveryActivity::class.java)
            intent.putExtra("MODE", "SEND")
            startActivity(intent)
        }

        // Receive Button - Open Discovery in RECEIVE mode
        binding.cardReceive.setOnClickListener {
            val intent = Intent(this, DeviceDiscoveryActivity::class.java)
            intent.putExtra("MODE", "RECEIVE")
            startActivity(intent)
        }

        // History
        binding.cardHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        // Settings
        binding.cardSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    // Handle Permission Result
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permissions are required to find devices!", Toast.LENGTH_LONG).show()
        }
    }
}