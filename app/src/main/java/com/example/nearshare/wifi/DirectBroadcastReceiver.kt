package com.example.nearshare.wifi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pManager
import android.util.Log

/**
 * A BroadcastReceiver that notifies of important Wi-Fi p2p events.
 */
class DirectBroadcastReceiver(
    private val manager: WifiP2pManager,
    private val channel: WifiP2pManager.Channel,
    private val activity: WifiP2pActionCallback
) : BroadcastReceiver() {

    // Interface to communicate back to the Activity
    interface WifiP2pActionCallback {
        fun onWifiP2pStateEnabled(isEnabled: Boolean)
        fun onPeersAvailable(deviceList: Collection<android.net.wifi.p2p.WifiP2pDevice>)
        fun onConnectionInfoAvailable(info: android.net.wifi.p2p.WifiP2pInfo)
        fun onDeviceDisconnected()
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                // Determine if Wi-Fi P2P mode is enabled or not
                val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                val isEnabled = state == WifiP2pManager.WIFI_P2P_STATE_ENABLED
                activity.onWifiP2pStateEnabled(isEnabled)
            }

            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                // The peer list has changed! Request the new list.
                manager.requestPeers(channel) { peers ->
                    // Send the new list of devices to the activity
                    activity.onPeersAvailable(peers.deviceList)
                }
            }

            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                // Connection state changed!
                val networkInfo = intent.getParcelableExtra<NetworkInfo>(WifiP2pManager.EXTRA_NETWORK_INFO)

                if (networkInfo?.isConnected == true) {
                    // We are connected with the other device, request connection info
                    // to find the group owner IP
                    manager.requestConnectionInfo(channel) { info ->
                        activity.onConnectionInfoAvailable(info)
                    }
                } else {
                    // It's a disconnect
                    activity.onDeviceDisconnected()
                }
            }

            WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                // Respond to this device's wifi state changing
                // (e.g. update UI with our own device name/status)
                Log.d("NearShare", "This device details changed")
            }
        }
    }
}