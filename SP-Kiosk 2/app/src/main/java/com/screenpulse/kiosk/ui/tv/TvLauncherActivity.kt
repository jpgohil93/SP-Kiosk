package com.screenpulse.kiosk.ui.tv

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.screenpulse.kiosk.ui.kiosk.KioskLauncherActivity

class TvLauncherActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Trampoline to the main Kiosk Launcher which handles logic
        val intent = Intent(this, KioskLauncherActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }
}
