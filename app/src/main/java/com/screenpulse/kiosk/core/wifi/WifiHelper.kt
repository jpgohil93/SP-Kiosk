package com.screenpulse.kiosk.core.wifi

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.ScanResult
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class WifiHelper(private val context: Context) {

    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun scanAvailableNetworks(): Flow<List<ScanResult>> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                if (intent.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        trySend(wifiManager.scanResults)
                    } else {
                        trySend(emptyList())
                    }
                }
            }
        }

        context.registerReceiver(receiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
        
        val success = wifiManager.startScan()
        if (!success) {
            // Scan failed (e.g. throttled)
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                trySend(wifiManager.scanResults) // Send cached results
            }
        }

        awaitClose {
            context.unregisterReceiver(receiver)
        }
    }

    fun connectToNetwork(ssid: String, password: String?, securityType: String, callback: (Boolean) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val suggestionBuilder = WifiNetworkSuggestion.Builder()
                .setSsid(ssid)
            
            if (!password.isNullOrEmpty()) {
                suggestionBuilder.setWpa2Passphrase(password)
            }
            
            val suggestion = suggestionBuilder.build()
            val status = wifiManager.addNetworkSuggestions(listOf(suggestion))
            
            if (status == WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
                // We can't force connection on Q+, but we can hint.
                // Also, we can try to bind to it if we want to verify, but for now just return success.
                callback(true)
            } else {
                callback(false)
            }
        } else {
            // Legacy
            val conf = WifiConfiguration()
            conf.SSID = "\"$ssid\""
            if (!password.isNullOrEmpty()) {
                conf.preSharedKey = "\"$password\""
            } else {
                conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
            }

            val netId = wifiManager.addNetwork(conf)
            if (netId != -1) {
                wifiManager.disconnect()
                wifiManager.enableNetwork(netId, true)
                wifiManager.reconnect()
                callback(true)
            } else {
                callback(false)
            }
        }
    }
}
