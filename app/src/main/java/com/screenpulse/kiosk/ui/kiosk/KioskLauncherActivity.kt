package com.screenpulse.kiosk.ui.kiosk

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.screenpulse.kiosk.core.config.ConfigManager
import com.screenpulse.kiosk.core.kiosk.KioskManager
import com.screenpulse.kiosk.core.permissions.PermissionManager
import com.screenpulse.kiosk.core.systemui.SystemUiController
import com.screenpulse.kiosk.ui.admin.AdminSettingsActivity
import com.screenpulse.kiosk.ui.admin.PinDialogFragment

class KioskLauncherActivity : AppCompatActivity() {

    private val launchHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val launchRunnable = Runnable {
        if (KioskManager.isKioskEnabled(this)) {
            KioskManager.launchApprovedApp(this)
            KioskManager.startSupervisorService(this)
        }
    }
    private var isLaunchScheduled = false

    private lateinit var systemUiController: SystemUiController
    private lateinit var gestureDetector: GestureDetector
    private var tapCount = 0
    private val TAP_THRESHOLD = 5
    private val TAP_TIME_WINDOW = 5000L // 5 seconds
    private var firstTapTime = 0L

    private lateinit var connectivityMonitor: com.screenpulse.kiosk.core.wifi.WifiConnectivityMonitor
    private var isConnected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        connectivityMonitor = com.screenpulse.kiosk.core.wifi.WifiConnectivityMonitor(this)
        connectivityMonitor.isConnected.observe(this) { connected ->
            isConnected = connected
        }

        // Initialize Remote Client
        com.screenpulse.kiosk.core.remote.RemoteClient.init(ConfigManager.getConfig(this))

        systemUiController = SystemUiController(this)
        setupGestureDetector()
        
        setContentView(com.screenpulse.kiosk.R.layout.activity_kiosk_launcher)
        
        val wallpaperView = findViewById<ImageView>(com.screenpulse.kiosk.R.id.wallpaperView)
        val setupButton = findViewById<android.widget.Button>(com.screenpulse.kiosk.R.id.setupButton)
        
        // Load branding wallpaper if available
        val config = ConfigManager.getConfig(this)
        if (!config.branding.wallpaperUri.isNullOrEmpty()) {
            try {
                val uri = android.net.Uri.parse(config.branding.wallpaperUri)
                val inputStream = contentResolver.openInputStream(uri)
                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                wallpaperView.setImageBitmap(bitmap)
                wallpaperView.alpha = 1.0f // Reset alpha if custom wallpaper
                inputStream?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        setupButton.setOnClickListener {
            showPinDialog()
        }
        
        // Setup touch listener on root view for gesture
        findViewById<View>(android.R.id.content).setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }

        // Initial check (don't launch yet, wait for onResume)
        if (!KioskManager.isKioskEnabled(this)) {
            setupButton.visibility = View.VISIBLE
        } else {
            setupButton.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        systemUiController.enableImmersiveMode()
        
        val config = ConfigManager.getConfig(this)
        
        // Check Wi-Fi Onboarding
        if (!config.wifiOnboardingCompleted && !isConnected) {
            // Launch Captive Wifi Activity
            val intent = Intent(this, com.screenpulse.kiosk.ui.wifi.CaptiveWifiActivity::class.java)
            startActivityForResult(intent, 1001)
            return
        }

        // Check Remote Support Setup
        if (config.wifiOnboardingCompleted && (!config.remoteControlEnabled || !config.remoteScreenEnabled)) {
            val intent = Intent(this, com.screenpulse.kiosk.ui.remote.RemoteSetupActivity::class.java)
            startActivity(intent)
            // We don't return here because we want to allow the launcher to proceed in background or just pause
            // But actually, we should probably return to avoid launching the kiosk app on top immediately
            // However, RemoteSetupActivity is just a standard activity.
            // Let's return to ensure it takes focus.
            return
        }

        if (KioskManager.isKioskEnabled(this)) {
            systemUiController.addStatusBarOverlay()
            scheduleLaunch()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001) {
            // Returned from Wifi Onboarding
            val config = ConfigManager.getConfig(this)
            config.wifiOnboardingCompleted = true
            ConfigManager.saveConfig(this, config)
            // onResume will be called again to proceed
        }
    }

    override fun onPause() {
        super.onPause()
        systemUiController.removeStatusBarOverlay()
        cancelLaunch()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            systemUiController.enableImmersiveMode()
        }
    }

    private fun scheduleLaunch() {
        if (!isLaunchScheduled) {
            // 3 second delay to allow for exit gesture
            launchHandler.postDelayed(launchRunnable, 3000)
            isLaunchScheduled = true
        }
    }

    private fun cancelLaunch() {
        launchHandler.removeCallbacks(launchRunnable)
        isLaunchScheduled = false
    }

    private fun setupGestureDetector() {
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean {
                val currentTime = System.currentTimeMillis()
                if (tapCount == 0 || (currentTime - firstTapTime > TAP_TIME_WINDOW)) {
                    tapCount = 1
                    firstTapTime = currentTime
                } else {
                    tapCount++
                }

                if (tapCount >= TAP_THRESHOLD) {
                    cancelLaunch() // Stop auto-launch when gesture detected
                    showPinDialog()
                    tapCount = 0
                }
                return true
            }
        })
    }

    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
        if (event.action == android.view.KeyEvent.ACTION_DOWN) {
            val keyCode = event.keyCode
            if (keyCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER || keyCode == android.view.KeyEvent.KEYCODE_ENTER) {
                val currentTime = System.currentTimeMillis()
                if (tapCount == 0 || (currentTime - firstTapTime > TAP_TIME_WINDOW)) {
                    tapCount = 1
                    firstTapTime = currentTime
                } else {
                    tapCount++
                }

                if (tapCount >= TAP_THRESHOLD) {
                    cancelLaunch() // Stop auto-launch when gesture detected
                    showPinDialog()
                    tapCount = 0
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun showPinDialog() {
        val dialog = PinDialogFragment()
        dialog.show(supportFragmentManager, "PinDialog")
    }
    override fun onDestroy() {
        super.onDestroy()
        if (::connectivityMonitor.isInitialized) {
            connectivityMonitor.cleanup()
        }
    }
}
