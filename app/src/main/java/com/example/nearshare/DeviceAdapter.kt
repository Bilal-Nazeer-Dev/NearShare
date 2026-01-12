package com.example.nearshare

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.nearshare.databinding.ItemDeviceBinding

// Simple data class for a Device
data class WifiDevice(val name: String, val status: String)

class DeviceAdapter(
    private val onDeviceClick: (WifiDevice) -> Unit
) : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

    private val devices = mutableListOf<WifiDevice>()

    fun updateList(newDevices: List<WifiDevice>) {
        devices.clear()
        devices.addAll(newDevices)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val binding = ItemDeviceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DeviceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = devices[position]
        holder.bind(device)
    }

    override fun getItemCount() = devices.size

    inner class DeviceViewHolder(private val binding: ItemDeviceBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(device: WifiDevice) {
            binding.tvDeviceName.text = device.name
            binding.tvDeviceStatus.text = device.status

            binding.root.setOnClickListener {
                onDeviceClick(device)
            }
        }
    }
}