package com.screenpulse.kiosk.core.wifi

import android.content.Context
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import com.screenpulse.kiosk.core.config.WifiProfile

object WifiConfigManager {

    fun applyWifiProfiles(context: Context, profiles: List<WifiProfile>) {
        if (profiles.isEmpty()) return

        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val suggestions = ArrayList<WifiNetworkSuggestion>()
            for (profile in profiles) {
                val builder = WifiNetworkSuggestion.Builder()
                    .setSsid(profile.ssid)
                
                if (!profile.password.isNullOrEmpty()) {
                    builder.setWpa2Passphrase(profile.password)
                }
                
                suggestions.add(builder.build())
            }
            
            val status = wifiManager.addNetworkSuggestions(suggestions)
            if (status != WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
                // Handle error or log
            }
        } else {
            // Legacy way
            if (!wifiManager.isWifiEnabled) {
                @Suppress("DEPRECATION")
                wifiManager.isWifiEnabled = true
            }

            for (profile in profiles) {
                val conf = WifiConfiguration()
                conf.SSID = "\"${profile.ssid}\""
                if (!profile.password.isNullOrEmpty()) {
                    conf.preSharedKey = "\"${profile.password}\""
                } else {
                    conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
                }
                
                @Suppress("DEPRECATION")
                val netId = wifiManager.addNetwork(conf)
                if (netId != -1) {
                    @Suppress("DEPRECATION")
                    wifiManager.enableNetwork(netId, false)
                }
            }
        }
    }
}
