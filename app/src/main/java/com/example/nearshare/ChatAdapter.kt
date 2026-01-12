package com.example.nearshare

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class ChatAdapter(
    private val messages: List<Message>,
    private val context: Context
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val container: LinearLayout = view as LinearLayout
        val tvMessage: TextView = view.findViewById(R.id.tvMessage)
        val ivImage: ImageView = view.findViewById(R.id.ivImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_message, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val msg = messages[position]

        // 1. Alignment & Background
        holder.container.gravity = if (msg.isReceived) Gravity.START else Gravity.END
        val bgColor = if (msg.isReceived) android.R.color.darker_gray else R.color.near_blue

        holder.tvMessage.visibility = View.GONE
        holder.ivImage.visibility = View.GONE

        // 2. Clear previous listeners
        holder.itemView.setOnClickListener(null)
        holder.itemView.setOnLongClickListener(null)

        // 3. Content Logic
        when (msg.type) {
            Message.TYPE_TEXT -> {
                holder.tvMessage.visibility = View.VISIBLE
                holder.tvMessage.setBackgroundColor(context.getColor(bgColor))
                holder.tvMessage.text = msg.content
            }
            Message.TYPE_IMAGE -> {
                holder.ivImage.visibility = View.VISIBLE
                holder.ivImage.setBackgroundColor(context.getColor(bgColor))
                val img = File(msg.content)
                if (img.exists()) {
                    holder.ivImage.setImageBitmap(BitmapFactory.decodeFile(img.absolutePath))
                    setupInteractions(holder, img, "image/*")
                }
            }
            Message.TYPE_VIDEO -> {
                holder.ivImage.visibility = View.VISIBLE
                holder.ivImage.setBackgroundColor(context.getColor(bgColor))
                val videoFile = File(msg.content)
                if (videoFile.exists()) {
                    val thumb = ThumbnailUtils.createVideoThumbnail(
                        videoFile.absolutePath,
                        MediaStore.Images.Thumbnails.MINI_KIND
                    )
                    if (thumb != null) holder.ivImage.setImageBitmap(thumb)
                    else holder.ivImage.setImageResource(R.drawable.ic_video)

                    setupInteractions(holder, videoFile, "video/*")
                }
            }
            Message.TYPE_DOC -> {
                holder.tvMessage.visibility = View.VISIBLE
                holder.tvMessage.setBackgroundColor(context.getColor(bgColor))
                val docFile = File(msg.content)
                holder.tvMessage.text = "📄 ${docFile.name}"
                setupInteractions(holder, docFile, "application/pdf")
            }
        }
    }

    private fun setupInteractions(holder: ChatViewHolder, file: File, mimeType: String) {
        // CLICK -> OPEN FILE
        holder.itemView.setOnClickListener {
            openFile(file, mimeType)
        }

        // LONG CLICK -> SAVE TO GALLERY
        holder.itemView.setOnLongClickListener {
            showSaveDialog(file)
            true
        }
    }

    private fun openFile(file: File, mimeType: String) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(uri, mimeType)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot open file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSaveDialog(file: File) {
        AlertDialog.Builder(context)
            .setTitle("Save File?")
            .setMessage("Do you want to save this file to your Downloads folder?")
            .setPositiveButton("Save") { _, _ -> saveToDownloads(file) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveToDownloads(file: File) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ Method (MediaStore)
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                    put(MediaStore.MediaColumns.MIME_TYPE, getMimeType(file.extension))
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                uri?.let {
                    resolver.openOutputStream(it)?.use { output ->
                        FileInputStream(file).copyTo(output)
                    }
                    Toast.makeText(context, "Saved to Downloads!", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Android 9 and below Method
                val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val destFile = File(downloadDir, file.name)
                FileInputStream(file).copyTo(FileOutputStream(destFile))
                Toast.makeText(context, "Saved to Downloads!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Save Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private fun getMimeType(extension: String): String {
        return when(extension.lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "mp4" -> "video/mp4"
            "pdf" -> "application/pdf"
            else -> "*/*"
        }
    }

    override fun getItemCount(): Int = messages.size
}