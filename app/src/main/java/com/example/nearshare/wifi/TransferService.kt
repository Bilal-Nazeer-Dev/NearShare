package com.example.nearshare.wifi

import android.content.Context
import android.os.Handler
import android.util.Log
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets

class TransferService(
    private val context: Context,
    private val handler: Handler,
    private val onTextReceived: (String) -> Unit,
    private val onFileReceived: (String, String) -> Unit,
    private val onNameReceived: (String) -> Unit // NEW CALLBACK
) {

    private var socket: Socket? = null
    private var isRunning = false

    // Get My Saved Name
    private val myName: String
        get() = context.getSharedPreferences("NearShareSettings", Context.MODE_PRIVATE)
            .getString("USER_NAME", android.os.Build.MODEL) ?: "Unknown"

    fun startServer() {
        Thread {
            try {
                val serverSocket = ServerSocket(8888)
                socket = serverSocket.accept()
                isRunning = true

                // 1. Immediately Send My Name
                sendName(myName)

                // 2. Start Listening
                listenForData(socket!!)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    fun startClient(hostAddress: String) {
        Thread {
            try {
                val socket = Socket()
                socket.connect(InetSocketAddress(hostAddress, 8888), 5000)
                this.socket = socket
                isRunning = true

                // 1. Immediately Send My Name
                sendName(myName)

                // 2. Start Listening
                listenForData(socket)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun listenForData(socket: Socket) {
        try {
            val inputStream = socket.getInputStream()
            val buffer = ByteArray(4096)

            while (isRunning) {
                // Read Header Type (TEXT, FILE, or NAME)
                val headerBytes = ByteArray(4)
                if (inputStream.read(headerBytes) == -1) break
                val header = String(headerBytes, StandardCharsets.UTF_8)

                when (header) {
                    "NAME" -> {
                        // Read Name Length
                        val lenBytes = ByteArray(4)
                        inputStream.read(lenBytes)
                        val nameLen = java.nio.ByteBuffer.wrap(lenBytes).int

                        // Read Name
                        val nameBytes = ByteArray(nameLen)
                        inputStream.read(nameBytes)
                        val peerName = String(nameBytes, StandardCharsets.UTF_8)

                        handler.post { onNameReceived(peerName) }
                    }
                    "TEXT" -> {
                        // (Existing Text Logic...)
                        val sizeBytes = ByteArray(4)
                        inputStream.read(sizeBytes)
                        val size = java.nio.ByteBuffer.wrap(sizeBytes).int
                        val msgBytes = ByteArray(size)
                        var total = 0
                        while (total < size) {
                            val count = inputStream.read(msgBytes, total, size - total)
                            if (count == -1) break
                            total += count
                        }
                        handler.post { onTextReceived(String(msgBytes)) }
                    }
                    "FILE" -> {
                        // (Keep existing File logic here...)
                        handleFileReceive(inputStream)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sendName(name: String) {
        Thread {
            try {
                val outputStream = socket?.getOutputStream()
                val nameBytes = name.toByteArray(StandardCharsets.UTF_8)

                // Header "NAME"
                outputStream?.write("NAME".toByteArray(StandardCharsets.UTF_8))

                // Name Length (Int -> 4 bytes)
                outputStream?.write(java.nio.ByteBuffer.allocate(4).putInt(nameBytes.size).array())

                // Name Data
                outputStream?.write(nameBytes)
                outputStream?.flush()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    // (Keep existing sendText, sendFile, handleFileReceive methods exactly as they were...)
    // I am omitting them here for brevity, but DO NOT DELETE THEM from your code.
    // Just ensure sendName and the "NAME" case in listenForData are added.

    // ... Copy sendText and sendFile here ...
    fun sendText(message: String) {
        Thread {
            try {
                val outputStream = socket?.getOutputStream()
                val msgBytes = message.toByteArray()
                outputStream?.write("TEXT".toByteArray())
                outputStream?.write(java.nio.ByteBuffer.allocate(4).putInt(msgBytes.size).array())
                outputStream?.write(msgBytes)
                outputStream?.flush()
            } catch (e: Exception) { e.printStackTrace() }
        }.start()
    }

    fun sendFile(context: Context, uri: android.net.Uri, fileName: String) {
        // (Paste your existing sendFile code here)
        // Ensure to write "FILE" header first
        Thread {
            try {
                val outputStream = socket?.getOutputStream()
                val contentResolver = context.contentResolver
                val fileSize = contentResolver.openFileDescriptor(uri, "r")?.statSize ?: 0
                val inputStream = contentResolver.openInputStream(uri)

                outputStream?.write("FILE".toByteArray())

                // Send Filename Length & Name
                val nameBytes = fileName.toByteArray()
                outputStream?.write(java.nio.ByteBuffer.allocate(4).putInt(nameBytes.size).array())
                outputStream?.write(nameBytes)

                // Send File Size
                outputStream?.write(java.nio.ByteBuffer.allocate(8).putLong(fileSize).array())

                // Send Payload
                val buffer = ByteArray(4096)
                var bytesRead: Int
                while (inputStream?.read(buffer).also { bytesRead = it ?: -1 } != -1) {
                    outputStream?.write(buffer, 0, bytesRead)
                }
                outputStream?.flush()
                inputStream?.close()
            } catch (e: Exception) { e.printStackTrace() }
        }.start()
    }

    private fun handleFileReceive(inputStream: InputStream) {
        // (Paste your existing handleFileReceive logic here)
        // 1. Read Filename Length
        val nameLenBytes = ByteArray(4)
        inputStream.read(nameLenBytes)
        val nameLen = java.nio.ByteBuffer.wrap(nameLenBytes).int

        // 2. Read Filename
        val nameBytes = ByteArray(nameLen)
        inputStream.read(nameBytes)
        val fileName = String(nameBytes)

        // 3. Read File Size
        val sizeBytes = ByteArray(8)
        inputStream.read(sizeBytes)
        val fileSize = java.nio.ByteBuffer.wrap(sizeBytes).long

        // 4. Save File
        val file = java.io.File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), fileName)
        val fos = java.io.FileOutputStream(file)
        val buffer = ByteArray(4096)
        var totalRead: Long = 0
        while (totalRead < fileSize) {
            val read = inputStream.read(buffer, 0, minOf(4096, (fileSize - totalRead).toInt()))
            if (read == -1) break
            fos.write(buffer, 0, read)
            totalRead += read
        }
        fos.close()
        handler.post { onFileReceived(fileName, file.absolutePath) }
    }

    fun close() {
        isRunning = false
        socket?.close()
    }
}