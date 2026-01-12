package com.example.nearshare

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nearshare.databinding.ActivityDeviceDiscoveryBinding
import com.example.nearshare.utils.PermissionHelper
import com.example.nearshare.wifi.DirectBroadcastReceiver

class DeviceDiscoveryActivity : AppCompatActivity(), DirectBroadcastReceiver.WifiP2pActionCallback {

    private lateinit var binding: ActivityDeviceDiscoveryBinding
    private lateinit var deviceAdapter: DeviceAdapter

    private lateinit var manager: WifiP2pManager
    private lateinit var channel: WifiP2pManager.Channel
    private lateinit var receiver: DirectBroadcastReceiver
    private lateinit var intentFilter: IntentFilter

    private val peersList = mutableListOf<WifiP2pDevice>()
    private var isHostMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDeviceDiscoveryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mode = intent.getStringExtra("MODE") ?: "SEND"
        isHostMode = (mode == "RECEIVE")

        setupWifiP2p()
        setupUI()
        startRadarAnimation()
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(receiver, intentFilter)

        // CLEANUP: Just remove old groups quietly on start
        manager.removeGroup(channel, null)

        if (PermissionHelper.hasPermissions(this)) {
            Handler(Looper.getMainLooper()).postDelayed({
                if (isHostMode) {
                    startHostMode()
                } else {
                    startDiscoveryMode()
                }
            }, 800)
        }
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(receiver)
            manager.stopPeerDiscovery(channel, null)
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun setupWifiP2p() {
        manager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        channel = manager.initialize(this, mainLooper, null)
        receiver = DirectBroadcastReceiver(manager, channel, this)

        intentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { finish() }

        if (isHostMode) {
            binding.tvTitle.text = "Waiting for Sender..."
            binding.tvStatus.text = "Creating Group..."
        } else {
            binding.tvTitle.text = "Select Device"
            binding.tvStatus.text = "Searching..."
        }

        deviceAdapter = DeviceAdapter { viewDevice ->
            if (!isHostMode) {
                connectToDevice(viewDevice)
            }
        }

        binding.rvDevices.layoutManager = LinearLayoutManager(this)
        binding.rvDevices.adapter = deviceAdapter
    }

    // --- MODE 1: HOST (Receiver) ---
    @SuppressLint("MissingPermission")
    private fun startHostMode() {
        // Host tries to create a group. If it fails, we fall back to discovery.
        manager.createGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                binding.tvStatus.text = "Ready to Receive"
            }
            override fun onFailure(reason: Int) {
                binding.tvStatus.text = "Visible (Discovery Mode)"
                manager.discoverPeers(channel, null)
            }
        })
    }

    // --- MODE 2: CLIENT (Sender) ---
    @SuppressLint("MissingPermission")
    private fun startDiscoveryMode() {
        manager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                binding.tvStatus.text = "Searching..."
            }
            override fun onFailure(reason: Int) {
                binding.tvStatus.text = "Discovery Failed ($reason)"
            }
        })
    }

    // ----------------------------------------------------------------
    // THE "COMPATIBILITY MODE" CONNECT FIX
    // ----------------------------------------------------------------
    @SuppressLint("MissingPermission")
    private fun connectToDevice(device: WifiDevice) {
        val actualDevice = peersList.find { it.deviceName == device.name } ?: return

        binding.tvStatus.text = "Connecting..."

        // 1. Compatibility Fix: DO NOT manually stop discovery.
        // Some Mediatek chips need discovery ACTIVE to find the peer during connect.

        val config = WifiP2pConfig().apply {
            deviceAddress = actualDevice.deviceAddress
            // 2. Compatibility Fix: REMOVED wps.setup = PBC
            // 3. Compatibility Fix: Force Client Mode (0) to avoid conflict
            groupOwnerIntent = 0
        }

        manager.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                binding.tvStatus.text = "Invitation Sent to ${device.name}"
                Toast.makeText(applicationContext, "Invitation Sent!", Toast.LENGTH_SHORT).show()
            }

            override fun onFailure(reason: Int) {
                if (reason == 2) { // BUSY
                    binding.tvStatus.text = "Busy... Retrying in 1s"
                    // 4. Retry Logic: Wait 1s and try again
                    Handler(Looper.getMainLooper()).postDelayed({
                        connectToDevice(device)
                    }, 1000)
                } else {
                    binding.tvStatus.text = "Failed ($reason)"
                    Toast.makeText(applicationContext, "Connection Failed ($reason)", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    // --- Callbacks ---

    override fun onWifiP2pStateEnabled(isEnabled: Boolean) {
        if (!isEnabled) {
            binding.tvStatus.text = "Enable Wi-Fi"
            binding.radarContainer.visibility = View.INVISIBLE
        } else {
            binding.radarContainer.visibility = View.VISIBLE
        }
    }

    override fun onPeersAvailable(deviceList: Collection<WifiP2pDevice>) {
        if (!isHostMode) {
            peersList.clear()
            peersList.addAll(deviceList)

            val viewList = deviceList.map {
                WifiDevice(it.deviceName, getDeviceStatus(it.status))
            }
            deviceAdapter.updateList(viewList)

            if (viewList.isNotEmpty()) {
                binding.tvStatus.text = "Found ${viewList.size} Devices"
            }
        }
    }

    override fun onConnectionInfoAvailable(info: WifiP2pInfo) {
        if (info.groupFormed) {
            // SUCCESS! Connection made.
            manager.stopPeerDiscovery(channel, null)
            val intent = Intent(this, TransferActivity::class.java)
            intent.putExtra("IS_GROUP_OWNER", info.isGroupOwner)
            intent.putExtra("GROUP_OWNER_ADDRESS", info.groupOwnerAddress?.hostAddress)
            startActivity(intent)
            finish()
        }
    }

    override fun onDeviceDisconnected() {
        if (isHostMode) {
            startHostMode()
        } else {
            startDiscoveryMode()
        }
    }

    private fun getDeviceStatus(statusCode: Int): String {
        return when (statusCode) {
            WifiP2pDevice.AVAILABLE -> "Available"
            WifiP2pDevice.INVITED -> "Invited"
            WifiP2pDevice.CONNECTED -> "Connected"
            WifiP2pDevice.FAILED -> "Failed"
            WifiP2pDevice.UNAVAILABLE -> "Unavailable"
            else -> "Unknown"
        }
    }

    private fun startRadarAnimation() {
        val pulseAnim = AnimationUtils.loadAnimation(this, R.anim.pulse)
        binding.pulse1.startAnimation(pulseAnim)
        val pulseAnimDelayed = AnimationUtils.loadAnimation(this, R.anim.pulse)
        pulseAnimDelayed.startOffset = 500
        binding.pulse2.startAnimation(pulseAnimDelayed)
    }
}