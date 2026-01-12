package com.example.nearshare

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.nearshare.databinding.ItemHistoryBinding

class HistoryAdapter(
    private val onItemClick: (String) -> Unit // Click Callback
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    private val items = mutableListOf<HistoryItem>()

    fun setList(newItems: List<HistoryItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class HistoryViewHolder(private val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: HistoryItem) {
            binding.tvFileName.text = item.fileName
            binding.tvStatus.text = item.status

            // 1. Calculate "Time Ago" (e.g., "5 mins ago")
            val timeAgo = DateUtils.getRelativeTimeSpanString(
                item.timestamp,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            )
            binding.tvDetails.text = "${item.fileSize} • $timeAgo"

            // 2. Set Icon based on Status
            if (item.status == "Sent") {
                binding.ivIcon.setImageResource(android.R.drawable.stat_sys_upload)
            } else {
                binding.ivIcon.setImageResource(android.R.drawable.stat_sys_download)
            }

            // 3. Handle Click to Open File
            binding.root.setOnClickListener {
                if (item.filePath.isNotEmpty()) {
                    onItemClick(item.filePath)
                }
            }
        }
    }
}