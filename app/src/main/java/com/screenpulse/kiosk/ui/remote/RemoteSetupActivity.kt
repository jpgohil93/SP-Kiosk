package com.screenpulse.kiosk.ui.remote

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.screenpulse.kiosk.core.config.ConfigManager
import com.screenpulse.kiosk.core.remote.RemoteClient
import com.screenpulse.kiosk.core.remote.RemoteControlService
import com.screenpulse.kiosk.core.remote.ScreenCaptureService
import com.screenpulse.kiosk.databinding.ActivityRemoteSetupBinding

class RemoteSetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRemoteSetupBinding
    private val SCREEN_CAPTURE_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRemoteSetupBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

        setupUI()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun setupUI() {
        binding.btnEnableControl.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Cannot open settings", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnEnableScreen.setOnClickListener {
            val mpManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            startActivityForResult(mpManager.createScreenCaptureIntent(), SCREEN_CAPTURE_REQUEST_CODE)
        }

        binding.btnDone.setOnClickListener {
            finish()
        }
    }

    private fun updateStatus() {
        // Check Accessibility
        val isAccessibilityEnabled = isAccessibilityServiceEnabled(this, RemoteControlService::class.java)
        binding.txtControlStatus.text = if (isAccessibilityEnabled) "Enabled" else "Not Enabled"
        binding.btnEnableControl.isEnabled = !isAccessibilityEnabled

        // Update Config
        val config = ConfigManager.getConfig(this)
        
        // Ensure Device ID exists
        if (config.remoteDeviceId == null) {
            config.remoteDeviceId = java.util.UUID.randomUUID().toString()
            ConfigManager.saveConfig(this, config)
        }

        if (config.remoteControlEnabled != isAccessibilityEnabled) {
            config.remoteControlEnabled = isAccessibilityEnabled
            ConfigManager.saveConfig(this, config)
            // Re-init client if needed
            RemoteClient.init(config)
        }

        // Check Screen Capture (We rely on config flag or service running check)
        // For simplicity, we just check the config flag which we set on successful start
        binding.txtScreenStatus.text = if (config.remoteScreenEnabled) "Enabled" else "Not Enabled"
        
        binding.btnDone.isEnabled = isAccessibilityEnabled && config.remoteScreenEnabled
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SCREEN_CAPTURE_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                // Start Service
                val serviceIntent = Intent(this, ScreenCaptureService::class.java).apply {
                    putExtra(ScreenCaptureService.EXTRA_RESULT_CODE, resultCode)
                    putExtra(ScreenCaptureService.EXTRA_RESULT_DATA, data)
                }
                startForegroundService(serviceIntent)
                
                val config = ConfigManager.getConfig(this)
                config.remoteScreenEnabled = true
                ConfigManager.saveConfig(this, config)
                RemoteClient.init(config)
                
                Toast.makeText(this, "Screen Sharing Started", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun isAccessibilityServiceEnabled(context: Context, service: Class<out android.accessibilityservice.AccessibilityService>): Boolean {
        val expectedComponentName = "${context.packageName}/${service.name}"
        val enabledServicesSetting = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        
        val colonSplitter = android.text.TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServicesSetting)
        
        while (colonSplitter.hasNext()) {
            val componentName = colonSplitter.next()
            if (componentName.equals(expectedComponentName, ignoreCase = true)) {
                return true
            }
        }
        return false
    }
}
