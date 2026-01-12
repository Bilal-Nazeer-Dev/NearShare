package com.example.nearshare

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.*
import java.net.Socket

// Updated Callback Interface
interface TransferCallback {
    fun onMessageReceived(message: Message)
    fun onTransferError(e: String)
}

class SendReceiveThread(
    private val socket: Socket,
    private val context: Context,
    private val callback: TransferCallback
) : Thread() {

    private var inputStream: DataInputStream? = null
    private var outputStream: DataOutputStream? = null
    private var isRunning = true
    private val mainHandler = Handler(Looper.getMainLooper())
    private val BUFFER_SIZE = 8192

    override fun run() {
        try {
            inputStream = DataInputStream(BufferedInputStream(socket.getInputStream()))
            outputStream = DataOutputStream(BufferedOutputStream(socket.getOutputStream()))

            while (isRunning && !socket.isClosed) {
                // 1. Read Type
                val type = inputStream!!.readInt()
                // 2. Read Size
                val size = inputStream!!.readLong()

                when (type) {
                    Message.TYPE_TEXT -> {
                        val bytes = ByteArray(size.toInt())
                        inputStream!!.readFully(bytes)
                        val text = String(bytes, Charsets.UTF_8)
                        notifyUI(Message(text, true, Message.TYPE_TEXT))
                    }
                    Message.TYPE_IMAGE -> receiveFile(size, "img", ".jpg", Message.TYPE_IMAGE)
                    Message.TYPE_VIDEO -> receiveFile(size, "vid", ".mp4", Message.TYPE_VIDEO)
                    Message.TYPE_DOC -> receiveFile(size, "doc", ".pdf", Message.TYPE_DOC)
                }
            }
        } catch (e: IOException) {
            if (isRunning) mainHandler.post { callback.onTransferError("Disconnected: ${e.message}") }
        } finally {
            close()
        }
    }

    private fun receiveFile(size: Long, prefix: String, ext: String, msgType: Int) {
        try {
            val file = File(context.getExternalFilesDir(null), "${prefix}_${System.currentTimeMillis()}$ext")
            val fos = FileOutputStream(file)
            val buffer = ByteArray(BUFFER_SIZE)
            var remaining = size

            while (remaining > 0) {
                val read = inputStream!!.read(buffer, 0, Math.min(buffer.size.toLong(), remaining).toInt())
                if (read == -1) break
                fos.write(buffer, 0, read)
                remaining -= read
            }
            fos.close()
            notifyUI(Message(file.absolutePath, true, msgType))
        } catch (e: IOException) {
            e.printStackTrace()
            close()
        }
    }

    // Generic Sender for all file types
    fun sendFile(file: File, type: Int) {
        Thread {
            try {
                val fis = FileInputStream(file)
                val size = file.length()
                val buffer = ByteArray(BUFFER_SIZE)

                synchronized(outputStream!!) {
                    outputStream!!.writeInt(type)
                    outputStream!!.writeLong(size)
                    var read: Int
                    while (fis.read(buffer).also { read = it } != -1) {
                        outputStream!!.write(buffer, 0, read)
                    }
                    outputStream!!.flush()
                }
                fis.close()
            } catch (e: Exception) {
                mainHandler.post { callback.onTransferError("Send Failed") }
            }
        }.start()
    }

    fun sendText(msg: String) {
        Thread {
            try {
                val bytes = msg.toByteArray()
                synchronized(outputStream!!) {
                    outputStream!!.writeInt(Message.TYPE_TEXT)
                    outputStream!!.writeLong(bytes.size.toLong())
                    outputStream!!.write(bytes)
                    outputStream!!.flush()
                }
            } catch (e: Exception) {
                mainHandler.post { callback.onTransferError("Send Failed") }
            }
        }.start()
    }

    private fun notifyUI(msg: Message) {
        mainHandler.post { callback.onMessageReceived(msg) }
    }

    fun close() {
        isRunning = false
        try { socket.close() } catch (e: Exception) {}
    }
}