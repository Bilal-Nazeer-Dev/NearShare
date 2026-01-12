package com.example.nearshare

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.text.format.Formatter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.nearshare.databinding.ActivitySettingsBinding
import java.io.File

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences("NearShareSettings", Context.MODE_PRIVATE)

        setupUI()
        loadUserProfile()
    }

    override fun onResume() {
        super.onResume()
        // Recalculate storage every time screen opens for "Real-Time" feel
        updateStorageInfo()
    }

    private fun setupUI() {
        // Back Button
        binding.btnBack.setOnClickListener { finish() }

        // Edit Profile Button
        binding.tvEditProfile.setOnClickListener {
            showEditNameDialog()
        }

        // Dark Mode Toggle
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("DARK_MODE", isChecked).apply()
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }

        // Load saved toggle state
        binding.switchDarkMode.isChecked = prefs.getBoolean("DARK_MODE", false)
    }

    private fun loadUserProfile() {
        // Load Custom Name (or default to Model)
        val defaultName = "${Build.MANUFACTURER} ${Build.MODEL}".uppercase()
        val savedName = prefs.getString("USER_NAME", defaultName)
        binding.tvDeviceName.text = savedName
        binding.tvDeviceModel.text = defaultName
    }

    private fun updateStorageInfo() {
        // 1. Get the Data Directory (Internal Storage)
        val path = Environment.getDataDirectory()
        val stat = StatFs(path.path)

        // 2. Calculate Total and Free Bytes
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availableBlocks = stat.availableBlocksLong

        val totalSize = totalBlocks * blockSize
        val freeSize = availableBlocks * blockSize
        val usedSize = totalSize - freeSize

        // 3. Calculate Percentage
        val progress = if (totalSize > 0) {
            ((usedSize.toDouble() / totalSize.toDouble()) * 100).toInt()
        } else {
            0
        }

        // 4. Update UI
        binding.progressStorage.progress = progress
        binding.tvStoragePercent.text = "$progress%"

        // Format nicely (e.g., "8.70 GB free of 50.21 GB")
        val freeReadable = Formatter.formatFileSize(this, freeSize)
        val totalReadable = Formatter.formatFileSize(this, totalSize)
        binding.tvStorageDetails.text = "$freeReadable free of $totalReadable"
    }

    private fun showEditNameDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_name, null)
        val editText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etDeviceName)

        editText.setText(prefs.getString("USER_NAME", ""))
        editText.selectAll()

        AlertDialog.Builder(this)
            .setTitle("Edit Device Name")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    prefs.edit().putString("USER_NAME", newName).apply()
                    binding.tvDeviceName.text = newName
                    Toast.makeText(this, "Name saved!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}