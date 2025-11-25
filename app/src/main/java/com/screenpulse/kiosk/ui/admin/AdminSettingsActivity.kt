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
        setupStatusCard()
        
        findViewById<Button>(com.screenpulse.kiosk.R.id.exitButton).setOnClickListener {
            finish()
        }
        
        refreshUI()
    }
    
    private fun setupStatusCard() {
        val statusCard = findViewById<android.view.View>(com.screenpulse.kiosk.R.id.statusCard)
        statusCard.setOnClickListener {
            val enabled = KioskManager.isKioskEnabled(this)
            if (enabled) {
                KioskManager.disableKioskMode(this)
            } else {
                if (checkPermissions()) {
                    KioskManager.enableKioskMode(this)
                }
            }
            refreshUI()
        }
    }
    
    private fun setupTiles() {
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
        val statusTitle = findViewById<TextView>(com.screenpulse.kiosk.R.id.statusTitle)
        val statusDesc = findViewById<TextView>(com.screenpulse.kiosk.R.id.statusDescription)
        val statusIcon = findViewById<android.widget.ImageView>(com.screenpulse.kiosk.R.id.statusIcon)
        val statusSwitch = findViewById<android.widget.Switch>(com.screenpulse.kiosk.R.id.statusSwitch)
        
        statusSwitch.isChecked = enabled
        
        if (enabled) {
            statusTitle.text = "Kiosk Mode is ACTIVE"
            statusTitle.setTextColor(resources.getColor(com.screenpulse.kiosk.R.color.primary_action))
            statusDesc.text = "App is locked. Tap to disable."
            statusIcon.setImageResource(android.R.drawable.ic_lock_lock)
            statusIcon.setColorFilter(resources.getColor(com.screenpulse.kiosk.R.color.primary_action))
        } else {
            statusTitle.text = "Kiosk Mode is DISABLED"
            statusTitle.setTextColor(resources.getColor(com.screenpulse.kiosk.R.color.text_error))
            statusDesc.text = "Device is unlocked. Tap to enable."
            statusIcon.setImageResource(android.R.drawable.ic_lock_power_off)
            statusIcon.setColorFilter(resources.getColor(com.screenpulse.kiosk.R.color.text_secondary))
        }
        
        // Update App Selection visibility in Kiosk Mode Dialog, not here anymore
        // But we can update the Kiosk Mode tile description
        val config = ConfigManager.getConfig(this)
        val tileKioskMode = findViewById<android.view.View>(com.screenpulse.kiosk.R.id.tileKioskMode)
        val modeDesc = if (config.kioskMode == com.screenpulse.kiosk.core.config.KioskMode.GENERIC_KIOSK) {
            "Generic: ${config.approvedAppPackage ?: "No App"}"
        } else {
            "Screen Pulse TV Player"
        }
        tileKioskMode.findViewById<TextView>(com.screenpulse.kiosk.R.id.tileDescription).text = modeDesc
    }
    
    private fun checkPermissions(): Boolean {
        // Check Special Permissions
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
