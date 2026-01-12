package com.example.nearshare

data class HistoryItem(
    val fileName: String,
    val fileSize: String,
    val status: String,
    val timestamp: Long,
    val filePath: String // New Field!
)