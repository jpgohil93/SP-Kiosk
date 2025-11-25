package com.screenpulse.kiosk.core.config

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

object ConfigManager {

    private const val PREF_NAME = "kiosk_config_prefs"
    private const val KEY_CONFIG_JSON = "config_json"
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    
    private var cachedConfig: KioskConfig? = null

    fun getConfig(context: Context): KioskConfig {
        if (cachedConfig == null) {
            loadConfig(context)
        }
        return cachedConfig!!
    }

    private fun loadConfig(context: Context) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_CONFIG_JSON, null)
        
        cachedConfig = if (json != null) {
            try {
                gson.fromJson(json, KioskConfig::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
                KioskConfig()
            }
        } else {
            KioskConfig()
        }
    }

    fun saveConfig(context: Context, config: KioskConfig) {
        cachedConfig = config
        val json = gson.toJson(config)
        val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CONFIG_JSON, json).apply()
    }

    fun exportConfig(context: Context, uri: Uri): Boolean {
        return try {
            val json = gson.toJson(getConfig(context))
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    writer.write(json)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun importConfig(context: Context, uri: Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    val json = reader.readText()
                    val newConfig = gson.fromJson(json, KioskConfig::class.java)
                    // Validate version or migrate if needed
                    saveConfig(context, newConfig)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    fun updateKioskEnabled(context: Context, enabled: Boolean) {
        val config = getConfig(context)
        config.kioskEnabled = enabled
        saveConfig(context, config)
    }
    
    fun updateApprovedApp(context: Context, packageName: String) {
        val config = getConfig(context)
        config.approvedAppPackage = packageName
        saveConfig(context, config)
    }

    fun updateKioskMode(context: Context, mode: com.screenpulse.kiosk.core.config.KioskMode) {
        val config = getConfig(context)
        config.kioskMode = mode
        saveConfig(context, config)
    }
}
