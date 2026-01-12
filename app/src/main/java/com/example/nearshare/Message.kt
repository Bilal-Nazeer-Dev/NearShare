package com.example.nearshare

data class Message(
    val content: String,      // Text message OR File Path
    val isReceived: Boolean,
    val type: Int             // 1=Text, 2=Image, 3=Video, 4=Doc
) {
    companion object {
        const val TYPE_TEXT = 1
        const val TYPE_IMAGE = 2
        const val TYPE_VIDEO = 3
        const val TYPE_DOC = 4
    }
}