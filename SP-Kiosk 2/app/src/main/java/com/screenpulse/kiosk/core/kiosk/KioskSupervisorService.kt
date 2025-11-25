package com.screenpulse.kiosk.core.kiosk

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.screenpulse.kiosk.R
import com.screenpulse.kiosk.core.config.ConfigManager
import java.util.SortedMap
import java.util.TreeMap

class KioskSupervisorService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val checkInterval = 2000L // Check every 2 seconds
    private val CHANNEL_ID = "KioskServiceChannel"

    private val checkRunnable = object : Runnable {
        override fun run() {
            checkForegroundApp()
            handler.postDelayed(this, checkInterval)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        handler.post(checkRunnable)
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(checkRunnable)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun checkForegroundApp() {
        if (!KioskManager.isKioskEnabled(this)) {
            stopSelf()
            return
        }

        val config = ConfigManager.getConfig(this)
        val myPackage = packageName
        
        // Determine allowed package based on mode
        val allowedPackage = if (config.kioskMode == com.screenpulse.kiosk.core.config.KioskMode.SCREEN_PULSE_TV) {
            myPackage
        } else {
            config.approvedAppPackage
        }

        if (allowedPackage.isNullOrEmpty()) return

        val currentPackage = getForegroundPackage()

        if (currentPackage != null && currentPackage != allowedPackage && currentPackage != myPackage) {
            // User escaped! Bring approved app back.
            KioskManager.launchApprovedApp(this)
        }
    }

    private fun getForegroundPackage(): String? {
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val time = System.currentTimeMillis()
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 10, time)

        if (stats != null && stats.isNotEmpty()) {
            val mySortedMap: SortedMap<Long, android.app.usage.UsageStats> = TreeMap()
            for (usageStats in stats) {
                mySortedMap[usageStats.lastTimeUsed] = usageStats
            }
            if (mySortedMap.isNotEmpty()) {
                return mySortedMap[mySortedMap.lastKey()]?.packageName
            }
        }
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Kiosk Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Kiosk Mode Active")
            .setContentText("Monitoring application usage")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()
    }
}
