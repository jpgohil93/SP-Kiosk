package com.screenpulse.kiosk.core.kiosk

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.screenpulse.kiosk.core.config.ConfigManager

object KioskManager {
    
    fun isKioskEnabled(context: Context): Boolean {
        return ConfigManager.getConfig(context).kioskEnabled
    }

    fun launchApprovedApp(context: Context) {
        val config = ConfigManager.getConfig(context)
        
        if (config.kioskMode == com.screenpulse.kiosk.core.config.KioskMode.SCREEN_PULSE_TV) {
            val intent = Intent(context, com.screenpulse.kiosk.ui.tv.TvPlayerActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return
        }

        val packageName = config.approvedAppPackage
        
        if (packageName.isNullOrEmpty()) {
            Toast.makeText(context, "No approved app selected", Toast.LENGTH_LONG).show()
            return
        }

        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
        } else {
            Toast.makeText(context, "Approved app not found: $packageName", Toast.LENGTH_LONG).show()
        }
    }

    fun enableKioskMode(context: Context) {
        ConfigManager.updateKioskEnabled(context, true)
        startSupervisorService(context)
        scheduleUpdateWorker(context)
        launchApprovedApp(context)
    }

    fun disableKioskMode(context: Context) {
        ConfigManager.updateKioskEnabled(context, false)
        stopSupervisorService(context)
    }

    fun startSupervisorService(context: Context) {
        val intent = Intent(context, KioskSupervisorService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun stopSupervisorService(context: Context) {
        val intent = Intent(context, KioskSupervisorService::class.java)
        context.stopService(intent)
    }

    fun scheduleUpdateWorker(context: Context) {
        val config = ConfigManager.getConfig(context)
        val interval = config.update.checkIntervalHours.toLong()
        
        val constraints = androidx.work.Constraints.Builder()
            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
            .build()

        val updateRequest = androidx.work.PeriodicWorkRequestBuilder<com.screenpulse.kiosk.core.update.UpdateWorker>(
            interval, java.util.concurrent.TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()

        androidx.work.WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "KioskUpdateWorker",
            androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
            updateRequest
        )
    }
}
