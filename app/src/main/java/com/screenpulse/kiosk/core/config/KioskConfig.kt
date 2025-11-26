package com.screenpulse.kiosk.core.config

import com.google.gson.annotations.SerializedName

enum class KioskMode {
    GENERIC_KIOSK,
    SCREEN_PULSE_TV
}

data class KioskConfig(
    @SerializedName("version") val version: Int = 1,
    @SerializedName("kioskEnabled") var kioskEnabled: Boolean = false,
    @SerializedName("kioskMode") var kioskMode: KioskMode = KioskMode.GENERIC_KIOSK,
    @SerializedName("wifiOnboardingCompleted") var wifiOnboardingCompleted: Boolean = false,
    @SerializedName("deviceToken") var deviceToken: String? = null,
    @SerializedName("approvedAppPackage") var approvedAppPackage: String? = null,
    @SerializedName("adminPin") var adminPin: String = "0000",
    @SerializedName("wifiProfiles") var wifiProfiles: MutableList<WifiProfile> = mutableListOf(),
    @SerializedName("branding") var branding: BrandingConfig = BrandingConfig(),
    @SerializedName("update") var update: UpdateConfig = UpdateConfig(),
    @SerializedName("remoteControlEnabled") var remoteControlEnabled: Boolean = false,
    @SerializedName("remoteScreenEnabled") var remoteScreenEnabled: Boolean = false,
    @SerializedName("remoteDeviceId") var remoteDeviceId: String? = null,
    @SerializedName("remoteApiBaseUrl") var remoteApiBaseUrl: String = "http://192.168.1.179:8080" // LAN IP for physical device testing
)

data class WifiProfile(
    @SerializedName("ssid") val ssid: String,
    @SerializedName("password") val password: String?,
    @SerializedName("security") val security: String // "NONE", "WPA", "WEP"
)

data class BrandingConfig(
    @SerializedName("wallpaperUri") var wallpaperUri: String? = null
)

data class UpdateConfig(
    @SerializedName("serverUrl") var serverUrl: String? = null,
    @SerializedName("checkIntervalHours") var checkIntervalHours: Int = 6
)
