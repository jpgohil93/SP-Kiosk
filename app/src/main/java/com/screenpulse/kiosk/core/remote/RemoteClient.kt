package com.screenpulse.kiosk.core.remote

import android.util.Log
import com.google.gson.Gson
import com.screenpulse.kiosk.core.config.KioskConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

object RemoteClient {
    private const val TAG = "RemoteClient"
    private var client: OkHttpClient? = null
    private var webSocket: WebSocket? = null
    private var config: KioskConfig? = null
    private var isConnected = false
    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO)

    // Callback for remote input commands
    var onRemoteInput: ((Int) -> Unit)? = null

    fun init(kioskConfig: KioskConfig) {
        config = kioskConfig
        client = OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build()
        
        if (config?.remoteControlEnabled == true || config?.remoteScreenEnabled == true) {
            connect()
        }
    }

    fun connect() {
        if (isConnected || config == null) return

        val baseUrl = config?.remoteApiBaseUrl ?: return
        // Replace http with ws
        val wsUrl = baseUrl.replace("http", "ws")
        
        val request = Request.Builder()
            .url(wsUrl)
            .build()

        webSocket = client?.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "Connected to Remote Backend")
                isConnected = true
                registerDevice()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "Disconnected: $reason")
                isConnected = false
                reconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "Connection failed", t)
                isConnected = false
                reconnect()
            }
        })
    }

    private fun registerDevice() {
        val deviceId = config?.remoteDeviceId ?: return
        val payload = mapOf(
            "type" to "REGISTER_DEVICE",
            "deviceId" to deviceId,
            "info" to mapOf(
                "name" to "Kiosk Device", // Could be configurable
                "remoteControl" to config?.remoteControlEnabled,
                "remoteScreen" to config?.remoteScreenEnabled
            )
        )
        webSocket?.send(gson.toJson(payload))
    }

    private fun handleMessage(text: String) {
        try {
            val data = gson.fromJson(text, Map::class.java)
            val type = data["type"] as? String

            if (type == "REMOTE_INPUT") {
                val keyCode = (data["keyCode"] as? Double)?.toInt()
                if (keyCode != null) {
                    onRemoteInput?.invoke(keyCode)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling message", e)
        }
    }

    private fun reconnect() {
        scope.launch {
            delay(5000) // Retry every 5 seconds
            Log.d(TAG, "Attempting reconnect...")
            connect()
        }
    }

    fun sendScreenFrame(base64Frame: String) {
        if (!isConnected) return
        val payload = mapOf(
            "type" to "SCREEN_FRAME",
            "payload" to base64Frame
        )
        webSocket?.send(gson.toJson(payload))
    }
}
