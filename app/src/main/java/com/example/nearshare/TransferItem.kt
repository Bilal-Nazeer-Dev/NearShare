package com.example.nearshare

data class TransferItem(
    val content: String,
    val isMe: Boolean,
    val isFile: Boolean = false,
    val fileName: String = "",
    val filePath: String? = null // Path to local file (for image loading)
)