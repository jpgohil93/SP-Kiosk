package com.screenpulse.kiosk

import android.content.Context
import android.content.SharedPreferences
import com.screenpulse.kiosk.core.config.ConfigManager
import com.screenpulse.kiosk.core.config.KioskConfig
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

class KioskUnitTests {

    @Mock
    lateinit var mockContext: Context

    @Mock
    lateinit var mockSharedPreferences: SharedPreferences

    @Mock
    lateinit var mockEditor: SharedPreferences.Editor

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        `when`(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockSharedPreferences)
        `when`(mockSharedPreferences.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor)
    }

    @Test
    fun testDefaultConfig() {
        // Test that default config is created when no prefs exist
        `when`(mockSharedPreferences.getString(anyString(), any())).thenReturn(null)
        
        val config = ConfigManager.getConfig(mockContext)
        assertNotNull(config)
        assertEquals("0000", config.adminPin)
        assertFalse(config.kioskEnabled)
    }

    @Test
    fun testSaveAndLoadConfig() {
        // Test saving config updates the shared prefs
        val config = KioskConfig(adminPin = "1234", kioskEnabled = true)
        ConfigManager.saveConfig(mockContext, config)
        
        verify(mockEditor).putString(eq("config_json"), contains("1234"))
        verify(mockEditor).apply()
    }

    @Test
    fun testUpdateKioskEnabled() {
        // Test helper method for toggling kiosk mode
        `when`(mockSharedPreferences.getString(anyString(), any())).thenReturn(null) // Start with default
        
        ConfigManager.updateKioskEnabled(mockContext, true)
        
        verify(mockEditor).putString(eq("config_json"), contains("\"kioskEnabled\": true"))
    }

    @Test
    fun testUpdateApprovedApp() {
        // Test helper method for setting approved app
        `when`(mockSharedPreferences.getString(anyString(), any())).thenReturn(null)
        
        val pkg = "com.example.app"
        ConfigManager.updateApprovedApp(mockContext, pkg)
        
        verify(mockEditor).putString(eq("config_json"), contains(pkg))
    }

    @Test
    fun testKioskModeDefaults() {
        `when`(mockSharedPreferences.getString(anyString(), any())).thenReturn(null)
        val config = ConfigManager.getConfig(mockContext)
        assertEquals(com.screenpulse.kiosk.core.config.KioskMode.GENERIC_KIOSK, config.kioskMode)
    }

    @Test
    fun testUpdateKioskMode() {
        `when`(mockSharedPreferences.getString(anyString(), any())).thenReturn(null)
        
        ConfigManager.updateKioskMode(mockContext, com.screenpulse.kiosk.core.config.KioskMode.SCREEN_PULSE_TV)
        
        verify(mockEditor).putString(eq("config_json"), contains("SCREEN_PULSE_TV"))
    }

    @Test
    fun testWifiOnboardingPersistence() {
        val config = KioskConfig(wifiOnboardingCompleted = true)
        ConfigManager.saveConfig(mockContext, config)
        
        verify(mockEditor).putString(eq("config_json"), contains("\"wifiOnboardingCompleted\": true"))
    }
}
