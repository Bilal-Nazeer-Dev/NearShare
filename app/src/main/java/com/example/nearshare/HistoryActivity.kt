package com.example.nearshare

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nearshare.databinding.ActivityHistoryBinding
import java.io.File

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var adapter: HistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnClear.setOnClickListener { showClearDialog() }

        setupList()
    }

    private fun setupList() {
        // Pass the click function to the adapter
        adapter = HistoryAdapter { filePath ->
            openFile(filePath)
        }

        binding.rvHistory.layoutManager = LinearLayoutManager(this)
        binding.rvHistory.adapter = adapter
        loadData()
    }

    private fun openFile(filePath: String) {
        val file = File(filePath)
        if (file.exists()) {
            try {
                // Generate URI using FileProvider
                val uri = FileProvider.getUriForFile(
                    this,
                    "${packageName}.fileprovider",
                    file
                )

                val intent = Intent(Intent.ACTION_VIEW)
                intent.setDataAndType(uri, getMimeType(filePath))
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Cannot open this file type", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "File no longer exists", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getMimeType(url: String): String {
        return when (url.substringAfterLast('.', "")) {
            "jpg", "jpeg", "png" -> "image/*"
            "pdf" -> "application/pdf"
            "mp4" -> "video/*"
            "mp3" -> "audio/*"
            else -> "*/*"
        }
    }

    private fun loadData() {
        val list = HistoryRepository.getHistory(this)
        if (list.isNotEmpty()) {
            adapter.setList(list)
            binding.rvHistory.visibility = View.VISIBLE
            binding.tvEmpty.visibility = View.GONE
            binding.btnClear.visibility = View.VISIBLE
        } else {
            binding.rvHistory.visibility = View.GONE
            binding.tvEmpty.visibility = View.VISIBLE
            binding.btnClear.visibility = View.INVISIBLE
        }
    }

    private fun showClearDialog() {
        AlertDialog.Builder(this)
            .setTitle("Clear History")
            .setMessage("Are you sure you want to delete all transfer history?")
            .setPositiveButton("Clear") { _, _ ->
                HistoryRepository.clearHistory(this)
                loadData()
                Toast.makeText(this, "History cleared", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}