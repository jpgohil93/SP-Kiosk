package com.screenpulse.kiosk.ui.admin

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.screenpulse.kiosk.core.config.ConfigManager
import com.screenpulse.kiosk.core.kiosk.KioskManager
import com.screenpulse.kiosk.core.permissions.PermissionManager

class AdminSettingsActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var toggleKioskButton: Button
    private lateinit var selectAppButton: Button
    private lateinit var exportButton: Button
    private lateinit var importButton: Button
    private lateinit var exitButton: Button

    private val exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            if (ConfigManager.exportConfig(this, uri)) {
                Toast.makeText(this, "Config exported", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Export failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            if (ConfigManager.importConfig(this, uri)) {
                Toast.makeText(this, "Config imported", Toast.LENGTH_SHORT).show()
                refreshUI()
            } else {
                Toast.makeText(this, "Import failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.screenpulse.kiosk.R.layout.activity_admin_settings)
        
        setupTiles()
        
        findViewById<Button>(com.screenpulse.kiosk.R.id.exitButton).setOnClickListener {
            finish()
        }
    }
    
    private fun setupTiles() {
        // Toggle Kiosk Tile
        val tileToggleKiosk = findViewById<android.view.View>(com.screenpulse.kiosk.R.id.tileToggleKiosk)
        tileToggleKiosk.setOnClickListener {
            val enabled = KioskManager.isKioskEnabled(this)
            if (enabled) {
                KioskManager.disableKioskMode(this)
                Toast.makeText(this, "Kiosk Mode Disabled", Toast.LENGTH_SHORT).show()
            } else {
                if (checkPermissions()) {
                    KioskManager.enableKioskMode(this)
                    Toast.makeText(this, "Kiosk Mode Enabled", Toast.LENGTH_SHORT).show()
                }
            }
            refreshUI()
        }

        // Kiosk Mode Tile
        val tileKioskMode = findViewById<android.view.View>(com.screenpulse.kiosk.R.id.tileKioskMode)
        tileKioskMode.findViewById<TextView>(com.screenpulse.kiosk.R.id.tileTitle).text = "Kiosk Mode"
        tileKioskMode.findViewById<TextView>(com.screenpulse.kiosk.R.id.tileDescription).text = "Configure mode & app"
        tileKioskMode.findViewById<android.widget.ImageView>(com.screenpulse.kiosk.R.id.tileIcon).setImageResource(android.R.drawable.ic_menu_preferences)
        tileKioskMode.setOnClickListener {
            showKioskModeDialog()
        }

        // Wi-Fi Tile
        val tileWifi = findViewById<android.view.View>(com.screenpulse.kiosk.R.id.tileWifi)
        tileWifi.findViewById<TextView>(com.screenpulse.kiosk.R.id.tileTitle).text = "Wi-Fi Profiles"
        tileWifi.findViewById<TextView>(com.screenpulse.kiosk.R.id.tileDescription).text = "Manage networks"
        tileWifi.findViewById<android.widget.ImageView>(com.screenpulse.kiosk.R.id.tileIcon).setImageResource(android.R.drawable.ic_menu_always_landscape_portrait)
        tileWifi.setOnClickListener {
            // TODO: Open Wi-Fi Settings (For now, just show toast or dialog)
            Toast.makeText(this, "Wi-Fi Settings coming soon", Toast.LENGTH_SHORT).show()
        }

        // Branding Tile
        val tileBranding = findViewById<android.view.View>(com.screenpulse.kiosk.R.id.tileBranding)
        tileBranding.findViewById<TextView>(com.screenpulse.kiosk.R.id.tileTitle).text = "Branding"
        tileBranding.findViewById<TextView>(com.screenpulse.kiosk.R.id.tileDescription).text = "Wallpaper & Logo"
        tileBranding.findViewById<android.widget.ImageView>(com.screenpulse.kiosk.R.id.tileIcon).setImageResource(android.R.drawable.ic_menu_gallery)
        tileBranding.setOnClickListener {
             Toast.makeText(this, "Branding Settings coming soon", Toast.LENGTH_SHORT).show()
        }

        // Updates Tile
        val tileUpdates = findViewById<android.view.View>(com.screenpulse.kiosk.R.id.tileUpdates)
        tileUpdates.findViewById<TextView>(com.screenpulse.kiosk.R.id.tileTitle).text = "Remote Updates"
        tileUpdates.findViewById<TextView>(com.screenpulse.kiosk.R.id.tileDescription).text = "Check for updates"
        tileUpdates.findViewById<android.widget.ImageView>(com.screenpulse.kiosk.R.id.tileIcon).setImageResource(android.R.drawable.ic_popup_sync)
        tileUpdates.setOnClickListener {
             Toast.makeText(this, "Update Check coming soon", Toast.LENGTH_SHORT).show()
        }

        // Export Tile
        val tileExport = findViewById<android.view.View>(com.screenpulse.kiosk.R.id.tileExport)
        tileExport.findViewById<TextView>(com.screenpulse.kiosk.R.id.tileTitle).text = "Export Config"
        tileExport.findViewById<TextView>(com.screenpulse.kiosk.R.id.tileDescription).text = "Save settings to file"
        tileExport.findViewById<android.widget.ImageView>(com.screenpulse.kiosk.R.id.tileIcon).setImageResource(android.R.drawable.ic_menu_save)
        tileExport.setOnClickListener {
            exportLauncher.launch("kiosk_config.json")
        }

        // Import Tile
        val tileImport = findViewById<android.view.View>(com.screenpulse.kiosk.R.id.tileImport)
        tileImport.findViewById<TextView>(com.screenpulse.kiosk.R.id.tileTitle).text = "Import Config"
        tileImport.findViewById<TextView>(com.screenpulse.kiosk.R.id.tileDescription).text = "Load settings from file"
        tileImport.findViewById<android.widget.ImageView>(com.screenpulse.kiosk.R.id.tileIcon).setImageResource(android.R.drawable.ic_menu_upload)
        tileImport.setOnClickListener {
            importLauncher.launch(arrayOf("application/json"))
        }

        // Remote Support Tile
        val tileRemote = findViewById<android.view.View>(com.screenpulse.kiosk.R.id.tileRemote)
        tileRemote.findViewById<TextView>(com.screenpulse.kiosk.R.id.tileTitle).text = "Remote Support"
        tileRemote.findViewById<TextView>(com.screenpulse.kiosk.R.id.tileDescription).text = "Enable Control & Screen"
        tileRemote.findViewById<android.widget.ImageView>(com.screenpulse.kiosk.R.id.tileIcon).setImageResource(android.R.drawable.ic_menu_help)
        tileRemote.setOnClickListener {
            startActivity(Intent(this, com.screenpulse.kiosk.ui.remote.RemoteSetupActivity::class.java))
        }
    }

    private fun showKioskModeDialog() {
        val config = ConfigManager.getConfig(this)
        val modes = arrayOf("Generic Kiosk (Select App)", "Screen Pulse TV Mode")
        val checkedItem = if (config.kioskMode == com.screenpulse.kiosk.core.config.KioskMode.GENERIC_KIOSK) 0 else 1

        AlertDialog.Builder(this)
            .setTitle("Select Kiosk Mode")
            .setSingleChoiceItems(modes, checkedItem) { dialog, which ->
                val newMode = if (which == 0) {
                    com.screenpulse.kiosk.core.config.KioskMode.GENERIC_KIOSK
                } else {
                    com.screenpulse.kiosk.core.config.KioskMode.SCREEN_PULSE_TV
                }
                ConfigManager.updateKioskMode(this, newMode)
                
                if (newMode == com.screenpulse.kiosk.core.config.KioskMode.GENERIC_KIOSK) {
                    showAppSelectionDialog()
                } else {
                    Toast.makeText(this, "Switched to TV Mode", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun refreshUI() {
        val enabled = KioskManager.isKioskEnabled(this)
        
        val tileToggleKiosk = findViewById<android.view.View>(com.screenpulse.kiosk.R.id.tileToggleKiosk)
        tileToggleKiosk.findViewById<TextView>(com.screenpulse.kiosk.R.id.tileTitle).text = if (enabled) "Turn Kiosk OFF" else "Turn Kiosk ON"
        tileToggleKiosk.findViewById<TextView>(com.screenpulse.kiosk.R.id.tileDescription).text = if (enabled) "Stop supervision" else "Start supervision"
        tileToggleKiosk.findViewById<android.widget.ImageView>(com.screenpulse.kiosk.R.id.tileIcon).setImageResource(
            if (enabled) android.R.drawable.ic_lock_power_off else android.R.drawable.ic_lock_idle_lock
        )
        
        // statusText.text = "Kiosk Mode: ${if (enabled) "ENABLED" else "DISABLED"}" // Removed legacy
        // toggleKioskButton.text = if (enabled) "Disable Kiosk Mode" else "Enable Kiosk Mode" // Removed legacy
        
        val config = ConfigManager.getConfig(this)
        
        if (config.kioskMode == com.screenpulse.kiosk.core.config.KioskMode.GENERIC_KIOSK) {
            // selectAppButton.visibility = android.view.View.VISIBLE // Removed legacy
            // selectAppButton.text = "App: ${config.approvedAppPackage ?: "None"}" // Removed legacy
        } else {
            // selectAppButton.visibility = android.view.View.GONE // Removed legacy
        }
    }
    
    private fun checkPermissions(): Boolean {
        if (!PermissionManager.hasOverlayPermission(this)) {
            PermissionManager.requestOverlayPermission(this)
            return false
        }
        if (!PermissionManager.hasUsageStatsPermission(this)) {
            PermissionManager.requestUsageStatsPermission(this)
            return false
        }
        return true
    }
    
    private fun showAppSelectionDialog() {
        val pm = packageManager
        val packages = pm.getInstalledApplications(android.content.pm.PackageManager.GET_META_DATA)
        val launchablePackages = packages.filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .sortedBy { it.loadLabel(pm).toString() }
            
        val names = launchablePackages.map { it.loadLabel(pm).toString() }.toTypedArray()
        
        AlertDialog.Builder(this)
            .setTitle("Select App")
            .setItems(names) { _, which ->
                val selected = launchablePackages[which]
                ConfigManager.updateApprovedApp(this, selected.packageName)
                refreshUI()
            }
            .show()
    }
    
    
}
