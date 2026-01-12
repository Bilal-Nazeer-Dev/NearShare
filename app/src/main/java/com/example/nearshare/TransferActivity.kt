package com.example.nearshare

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nearshare.databinding.ActivityTransferBinding
import java.io.File
import java.io.FileOutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors

class TransferActivity : AppCompatActivity(), TransferCallback {

    private lateinit var binding: ActivityTransferBinding
    private lateinit var chatAdapter: ChatAdapter
    private val messages = mutableListOf<Message>()
    private var sendReceiveThread: SendReceiveThread? = null
    private val executor = Executors.newSingleThreadExecutor()

    // 1. Image Picker
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { processFile(it, Message.TYPE_IMAGE, "jpg") }
    }

    // 2. Video Picker
    private val pickVideo = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { processFile(it, Message.TYPE_VIDEO, "mp4") }
    }

    // 3. Doc Picker
    private val pickDoc = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { processFile(it, Message.TYPE_DOC, "pdf") }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransferBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()

        val isOwner = intent.getBooleanExtra("IS_GROUP_OWNER", false)
        val ownerIp = intent.getStringExtra("GROUP_OWNER_ADDRESS")

        if (isOwner) startServer() else startClient(ownerIp)
    }

    private fun setupUI() {
        chatAdapter = ChatAdapter(messages, this)
        binding.rvChat.layoutManager = LinearLayoutManager(this)
        binding.rvChat.adapter = chatAdapter

        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                val msg = Message(text, false, Message.TYPE_TEXT)
                addMessage(msg)
                sendReceiveThread?.sendText(text)
                binding.etMessage.setText("")
            }
        }

        // Updated Attachment Button -> Shows Options
        binding.btnAttach.setOnClickListener {
            val options = arrayOf("Image", "Video", "Document")
            AlertDialog.Builder(this)
                .setTitle("Select File Type")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> pickImage.launch("image/*")
                        1 -> pickVideo.launch("video/*")
                        2 -> pickDoc.launch("application/pdf") // Change to */* for all docs
                    }
                }.show()
        }
    }

    private fun processFile(uri: Uri, type: Int, ext: String) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val tempFile = File(cacheDir, "temp_send.$ext")
            val outputStream = FileOutputStream(tempFile)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()

            val msg = Message(tempFile.absolutePath, false, type)
            addMessage(msg)
            sendReceiveThread?.sendFile(tempFile, type)
        } catch (e: Exception) {
            Toast.makeText(this, "File Error", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startServer() {
        binding.tvConnectionStatus.text = "Waiting..."
        executor.execute {
            try {
                val serverSocket = ServerSocket(8888)
                val socket = serverSocket.accept()
                runOnUiThread { binding.tvConnectionStatus.text = "Connected" }
                sendReceiveThread = SendReceiveThread(socket, this, this)
                sendReceiveThread?.start()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun startClient(ownerIp: String?) {
        binding.tvConnectionStatus.text = "Connecting..."
        executor.execute {
            try {
                val socket = Socket()
                socket.connect(InetSocketAddress(ownerIp, 8888), 5000)
                runOnUiThread { binding.tvConnectionStatus.text = "Connected" }
                sendReceiveThread = SendReceiveThread(socket, this, this)
                sendReceiveThread?.start()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    override fun onMessageReceived(message: Message) {
        addMessage(message)
    }

    override fun onTransferError(e: String) {
        Toast.makeText(this, e, Toast.LENGTH_SHORT).show()
    }

    private fun addMessage(msg: Message) {
        messages.add(msg)
        chatAdapter.notifyItemInserted(messages.size - 1)
        binding.rvChat.scrollToPosition(messages.size - 1)
    }
}