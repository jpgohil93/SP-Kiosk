package com.screenpulse.kiosk.ui.tv

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.screenpulse.kiosk.R
import com.screenpulse.kiosk.core.config.KioskConfig
import com.screenpulse.kiosk.core.wifi.WifiConnectivityMonitor
import java.util.UUID

class TvPlayerActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var errorText: TextView
    private lateinit var connectivityMonitor: WifiConnectivityMonitor
    private lateinit var loadingOverlay: View

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Fullscreen and Keep Screen On
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
        
        setContentView(R.layout.activity_tv_player)

        webView = findViewById(R.id.webView)
        loadingIndicator = findViewById(R.id.loadingIndicator)
        errorText = findViewById(R.id.errorText)
        loadingOverlay = findViewById(R.id.loadingOverlay)
        
        connectivityMonitor = WifiConnectivityMonitor(this)

        setupWebView()
        loadPlayer()

        connectivityMonitor.isConnected.observe(this) { isConnected ->
            updateCacheMode(isConnected)
            if (isConnected) {
                errorText.visibility = View.GONE
                if (webView.url == null || webView.url == "about:blank") {
                    loadPlayer()
                }
            } else {
                // Optional: Show offline message overlay
                // For now, we let WebView handle it or show error if page fails
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false
        
        // Caching
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        // Enable AppCache (Deprecated/Removed in API 33, relying on standard HTTP cache)
        // settings.setAppCacheEnabled(true)
        // settings.setAppCachePath(cacheDir.absolutePath)

        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                // Allow Screen Pulse domains
                if (url.contains("screenpulse.online")) {
                    return false
                }
                return true // Block others
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                loadingOverlay.visibility = View.GONE
            }

            override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                // If offline and error occurs, we might be in a state where cache missed.
                // But we don't want to show error immediately if we can avoid it.
                if (connectivityMonitor.isConnected.value == true) {
                    loadingOverlay.visibility = View.VISIBLE
                    loadingIndicator.visibility = View.GONE
                    errorText.visibility = View.VISIBLE
                    errorText.text = "Error loading player. Retrying..."
                }
            }
        }
        
        // Disable long press
        webView.setOnLongClickListener { true }
        webView.isLongClickable = false
    }

    private fun updateCacheMode(isConnected: Boolean) {
        if (isConnected) {
            webView.settings.cacheMode = WebSettings.LOAD_DEFAULT
        } else {
            // When offline, try to load from cache even if expired
            webView.settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
        }
    }

    private fun loadPlayer() {
        // Strict URL as requested
        val url = "https://tv.screenpulse.online/"
        webView.loadUrl(url)
    }

    private fun getOrGenerateDeviceToken(): String {
        val prefs = getSharedPreferences("kiosk_prefs", Context.MODE_PRIVATE)
        var token = prefs.getString("device_token", null)
        if (token == null) {
            token = UUID.randomUUID().toString()
            prefs.edit().putString("device_token", token).apply()
        }
        return token
    }

    override fun onDestroy() {
        super.onDestroy()
        connectivityMonitor.cleanup()
    }

    private var tapCount = 0
    private val TAP_THRESHOLD = 5
    private val TAP_TIME_WINDOW = 5000L // 5 seconds
    private var firstTapTime = 0L

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
                    showPinDialog()
                    tapCount = 0
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun showPinDialog() {
        val dialog = com.screenpulse.kiosk.ui.admin.PinDialogFragment()
        dialog.show(supportFragmentManager, "PinDialog")
    }

    override fun onBackPressed() {
        // Block back button
    }
}
