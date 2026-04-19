package com.example.myempty.vietcore

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private lateinit var security: OmnisSecurity
    private lateinit var simulator: DeviceSimulator 
    
    private var isInitialLoaded = false
    private var apiStatus = "PENDING" 
    private var updateJob: Job? = null
    private var selfDestructStarted = false

    private val COLOR_MATRIX_GREEN = Color.parseColor("#00FF41")
    private val COLOR_DANGER_RED = Color.parseColor("#FF0000")
    private val COLOR_WARNING_ORANGE = Color.parseColor("#FF8C00")
    private val COLOR_API_PURPLE = Color.parseColor("#D000FF")
    private val COLOR_SCANNING_CYAN = Color.parseColor("#00FFFF")
    private val COLOR_OFFLINE_YELLOW = Color.parseColor("#FFFF00")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE, 
            WindowManager.LayoutParams.FLAG_SECURE
        )
        
        setContentView(R.layout.activity_main)

        security = OmnisSecurity(this)
        simulator = DeviceSimulator(this) 

        setupFooterInfo()

        val tvStatus = findViewById<TextView>(R.id.tv_security_status)
        tvStatus?.text = "OMNIS: INITIALIZING SECURE ENGINE..."
        tvStatus?.setTextColor(COLOR_SCANNING_CYAN)

        verifySystemApi()
        startRealtimeMonitoring()
    }

    private fun startRealtimeMonitoring() {
        val tvDevice = findViewById<TextView>(R.id.tv_device_info)
        val tvStatus = findViewById<TextView>(R.id.tv_security_status)
        val errorOverlay = findViewById<View>(R.id.error_overlay)
        val tvErrorMessage = findViewById<TextView>(R.id.tv_error_message)

        updateJob = lifecycleScope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    // --- 1. PHÂN LOẠI THIẾT BỊ & PHẦN CỨNG ---
                    val isAndroidTV = isRunningOnTV()
                    val isTVBox = android.os.Build.MODEL.contains("TV Box", ignoreCase = true) || 
                                 android.os.Build.PRODUCT.contains("tv", ignoreCase = true)
                    
                    // MỚI: Kiểm tra Android đời cũ và phần cứng lạc hậu (32-bit)
                    val isLegacyOS = security.isLegacyOSDetected()
                    val isOutdatedHW = security.isOutdatedHardware()
                    
                    val isBootloaderUnlocked = security.isBootloaderUnlocked()
                    val isCustomROM = security.isCustomROMDetected()

                    // --- 2. TOÀN VẸN TÀI NGUYÊN ---
                    val isManifestTampered = security.isManifestTampered()
                    val isResourcesTampered = security.isResourceModified()
                    
                    val isAppNameChanged = checkAppNameInternal("VietCore")
                    val isAppIconChanged = checkAppIconInternal()

                    // --- 3. KIỂM TRA CAN THIỆP HỆ THỐNG ---
                    val isRooted = security.isRooted()
                    val isEmulator = security.isEmulatorOrVirtualMachine()
                    val isCloned = security.isAppCloned()
                    val isModified = !security.isOriginalPackage()
                    val isRemote = security.isRemoteAccessDetected()
                    val isCompromised = security.isIntegrityCompromised()

                    val currentViolation = when {
                        isLegacyOS -> "OUTDATED ANDROID VERSION" // Chặn Android cũ
                        isOutdatedHW -> "INCOMPATIBLE HARDWARE"  // Chặn 32-bit
                        isAndroidTV || isTVBox -> "UNSUPPORTED PLATFORM"
                        isBootloaderUnlocked || isCustomROM -> "UNSECURE HARDWARE"
                        isAppNameChanged -> "APP NAME ALTERATION DETECTED"
                        isAppIconChanged -> "APP LOGO TAMPERED"
                        isManifestTampered || isResourcesTampered || isModified -> "APK INTEGRITY BREACH"
                        isRemote -> "REMOTE ACCESS DETECTED"
                        isRooted -> "SYSTEM INTEGRITY BREACH"
                        isEmulator -> "VIRTUAL ENVIRONMENT DETECTED"
                        isCloned -> "CLONED APP DETECTED"
                        isCompromised -> "SECURITY POLICY VIOLATION"
                        else -> null
                    }

                    withContext(Dispatchers.Main) {
                        if (currentViolation != null) {
                            handleBankGradeViolation(currentViolation, tvStatus, errorOverlay, tvErrorMessage)
                        } else {
                            val specs = simulator.getRealTimeSpecs()
                            tvDevice?.text = specs
                            updateSecurityStatusLabel(tvStatus)
                            errorOverlay?.visibility = View.GONE
                            isInitialLoaded = true
                        }
                    }
                } catch (e: Exception) { }
                delay(2000) 
            }
        }
    }

    private fun checkAppNameInternal(expectedName: String): Boolean {
        return try {
            val currentLabel = applicationInfo.loadLabel(packageManager).toString()
            currentLabel != expectedName
        } catch (e: Exception) { false }
    }

    private fun checkAppIconInternal(): Boolean {
        return try {
            val currentIconRes = applicationInfo.icon
            val originalIconRes = R.mipmap.ic_launcher 
            currentIconRes != originalIconRes
        } catch (e: Exception) { false }
    }

    private fun handleBankGradeViolation(reason: String, statusView: TextView?, overlay: View?, errorText: TextView?) {
        val alertMsg = "SYSTEM PROTECTION ACTIVE\n" +
                      "--------------------------\n" +
                      "CURRENT ERROR: $reason\n\n" +
                      "Status: Unauthorized modification detected.\n" +
                      "Security action: App functionality disabled."

        statusView?.text = "SHIELD: TERMINATED"
        statusView?.setTextColor(COLOR_DANGER_RED)

        overlay?.visibility = View.VISIBLE
        errorText?.text = alertMsg
        errorText?.setTextColor(COLOR_DANGER_RED)

        if (!selfDestructStarted) {
            selfDestructStarted = true
            lifecycleScope.launch {
                delay(4000) 
                security.activateSelfDestruct()
            }
        }
    }

    private fun isRunningOnTV(): Boolean {
        val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        return uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    }

    private fun verifySystemApi() {
        val payload = "{\"device\":\"${android.os.Build.MODEL}\", \"os\":\"VietCore-Omnis-Pro\"}"
        lifecycleScope.launch {
            try {
                val result = SecurityClient.getInstance().verifyLaunchIntegrity(payload)
                withContext(Dispatchers.Main) {
                    apiStatus = when {
                        result.contains("API_GENUINE") -> "GENUINE"
                        result.contains("CONNECTION_COMPROMISED") -> "VIRUS_IP"
                        result.contains("SERVER_DENIED") -> "API_FAKE"
                        else -> "PENDING"
                    }
                    updateSecurityStatusLabel(findViewById(R.id.tv_security_status))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { apiStatus = "PENDING" }
            }
        }
    }

    private fun updateSecurityStatusLabel(statusView: TextView?) {
        if (statusView == null) return
        when (apiStatus) {
            "VIRUS_IP" -> {
                statusView.text = "SECURITY: MALICIOUS IP DETECTED"
                statusView.setTextColor(COLOR_WARNING_ORANGE)
            }
            "API_FAKE" -> {
                statusView.text = "SECURITY: MITM ATTACK DETECTED"
                statusView.setTextColor(COLOR_API_PURPLE)
            }
            "GENUINE" -> {
                statusView.text = "OMNIS: SYSTEM VERIFIED [SECURE]"
                statusView.setTextColor(COLOR_MATRIX_GREEN)
            }
            else -> {
                statusView.text = "OMNIS: MONITORING ACTIVE"
                statusView.setTextColor(COLOR_OFFLINE_YELLOW)
            }
        }
    }

    private fun setupFooterInfo() {
        findViewById<TextView>(R.id.tv_author)?.apply {
            text = "Developer: Nguyen Minh Toi"
            setTextColor(Color.WHITE)
        }
        findViewById<TextView>(R.id.tv_version)?.apply {
            text = "Core: VietCore 26.1.0-Omnis Pro"
            setTextColor(COLOR_SCANNING_CYAN)
        }
    }

    override fun onDestroy() {
        updateJob?.cancel()
        super.onDestroy()
    }
}
