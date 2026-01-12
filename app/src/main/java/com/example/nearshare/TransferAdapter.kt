package com.example.nearshare

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.nearshare.databinding.ItemTransferReceivedBinding
import com.example.nearshare.databinding.ItemTransferSentBinding

class TransferAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<TransferItem>()
    private val TYPE_SENT = 1
    private val TYPE_RECEIVED = 2

    fun addItem(item: TransferItem) {
        items.add(item)
        notifyItemInserted(items.size - 1)
    }

    override fun getItemViewType(position: Int): Int {
        return if (items[position].isMe) TYPE_SENT else TYPE_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_SENT) {
            val binding = ItemTransferSentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            SentHolder(binding)
        } else {
            val binding = ItemTransferReceivedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            ReceivedHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is SentHolder) holder.bind(items[position])
        if (holder is ReceivedHolder) holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    class SentHolder(private val binding: ItemTransferSentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: TransferItem) {
            // Reset visibility
            binding.tvMessage.visibility = View.GONE
            binding.fileContainer.visibility = View.GONE
            binding.cardImage.visibility = View.GONE

            if (item.isFile) {
                if (isImageFile(item.fileName) && item.filePath != null) {
                    binding.cardImage.visibility = View.VISIBLE
                    binding.ivImage.setImageURI(Uri.parse(item.filePath))
                } else {
                    binding.fileContainer.visibility = View.VISIBLE
                    binding.tvFileName.text = item.fileName
                }
            } else {
                binding.tvMessage.visibility = View.VISIBLE
                binding.tvMessage.text = item.content
            }
        }
    }

    class ReceivedHolder(private val binding: ItemTransferReceivedBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: TransferItem) {
            binding.tvMessage.visibility = View.GONE
            binding.fileContainer.visibility = View.GONE
            binding.cardImage.visibility = View.GONE

            if (item.isFile) {
                if (isImageFile(item.fileName) && item.filePath != null) {
                    binding.cardImage.visibility = View.VISIBLE
                    binding.ivImage.setImageURI(Uri.parse(item.filePath))
                } else {
                    binding.fileContainer.visibility = View.VISIBLE
                    binding.tvFileName.text = item.fileName
                }
            } else {
                binding.tvMessage.visibility = View.VISIBLE
                binding.tvMessage.text = item.content
            }
        }
    }

    companion object {
        fun isImageFile(path: String): Boolean {
            val p = path.lowercase()
            return p.endsWith(".jpg") || p.endsWith(".jpeg") || p.endsWith(".png") || p.endsWith(".webp")
        }
    }
}