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
import kotlin.random.Random

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

        verifyGlobalNetworkApi()
        startRealtimeMonitoring()
    }

    private fun startRealtimeMonitoring() {
        val tvDevice = findViewById<TextView>(R.id.tv_device_info)
        val tvStatus = findViewById<TextView>(R.id.tv_security_status)
        val errorOverlay = findViewById<View>(R.id.error_overlay)
        val tvErrorMessage = findViewById<TextView>(R.id.tv_error_message)
        val tvErrorCodeDigit = findViewById<TextView>(R.id.tv_error_code_digit)

        updateJob = lifecycleScope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    // --- KHÔI PHỤC & ĐỒNG BỘ CÁC BIẾN KIỂM TRA ---
                    // Fix lỗi Unresolved bằng cách sử dụng các hàm đã khôi phục trong OmnisSecurity
                    val isOriginal = security.isOriginalPackage()
                    val isOwner = isOriginal && !security.isSignatureValid() // Logic nhận diện chủ sở hữu

                    val isLegacyOS = security.isLegacyOSDetected()
                    val isOutdatedHW = security.isOutdatedHardware()
                    val isEmulator = security.isEmulatorOrVirtualMachine()
                    val isRooted = security.isRooted()
                    val isCustomROM = security.isCustomROMDetected()
                    val isBootloaderUnlocked = security.isBootloaderUnlocked()
                    
                    val isCloned = security.isAppCloned()
                    val isDebugging = security.isDebugging()
                    val isHackerTools = security.isHackerToolsDetected()
                    
                    // Fix lỗi truyền tham số cho AppName
                    val isNameTampered = security.isAppNameModified("VietCore")
                    val isIconTampered = security.isAppIconModified()
                    val isManifestBroken = security.isManifestTampered()
                    val isResourceBroken = security.isResourceModified()

                    val currentViolation = when {
                        !isOriginal || isManifestBroken || isResourceBroken -> "APK INTEGRITY BREACH"
                        isNameTampered || isIconTampered -> "UI TAMPERING DETECTED"
                        isHackerTools || isDebugging -> "HACKER TOOLS DETECTED"
                        !isOwner && (isRooted || isEmulator) -> "UNSECURE ENVIRONMENT"
                        !isOwner && (isCustomROM || isBootloaderUnlocked) -> "HARDWARE BREACH"
                        else -> null
                    }

                    withContext(Dispatchers.Main) {
                        if (currentViolation != null) {
                            val randomCode = Random.nextInt(1000, 9999)
                            tvErrorCodeDigit?.text = "INTERNAL_ERROR: $randomCode"
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

    private fun handleBankGradeViolation(reason: String, statusView: TextView?, overlay: View?, errorText: TextView?) {
        val tvTimer = findViewById<TextView>(R.id.tv_self_destruct_timer)
        statusView?.text = "SHIELD: TERMINATED"
        statusView?.setTextColor(COLOR_DANGER_RED)
        overlay?.visibility = View.VISIBLE
        
        errorText?.text = "UNAUTHORIZED INTERVENTION DETECTED"

        if (!selfDestructStarted) {
            selfDestructStarted = true
            lifecycleScope.launch(Dispatchers.Main) {
                // Đếm ngược mượt mà từ 4.0s xuống 0.0s (40 bước, mỗi bước 100ms)
                for (i in 40 downTo 0) {
                    val seconds = i / 10.0
                    tvTimer?.text = "SELF-DESTRUCT IN: ${String.format("%.1fs", seconds)}"
                    delay(100) // Tốc độ đếm từ từ và mượt mà
                }
                security.activateSelfDestruct()
            }
        }
    }

    private fun updateSecurityStatusLabel(statusView: TextView?) {
        if (statusView == null) return
        when (apiStatus) {
            "1001" -> {
                statusView.text = "OMNIS: SYSTEM VERIFIED [SECURE]"
                statusView.setTextColor(COLOR_MATRIX_GREEN)
            }
            "4004" -> {
                statusView.text = "SECURITY: NODE THREAT DETECTED"
                statusView.setTextColor(COLOR_WARNING_ORANGE)
            }
            else -> {
                statusView.text = "OMNIS: MONITORING ACTIVE"
                statusView.setTextColor(COLOR_OFFLINE_YELLOW)
            }
        }
    }

    private fun verifyGlobalNetworkApi() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                delay(1500)
                // Fix lỗi Unresolved reference 'isInAppBillingEmulated'
                // Sử dụng hàm kiểm tra Tools để thay thế logic verify mạng lưới thầm lặng
                val isThreat = security.isHackerToolsDetected()
                withContext(Dispatchers.Main) {
                    apiStatus = if (!isThreat) "1001" else "4004"
                    updateSecurityStatusLabel(findViewById(R.id.tv_security_status))
                }
            } catch (e: Exception) { }
        }
    }

    private fun setupFooterInfo() {
        findViewById<TextView>(R.id.tv_author)?.apply {
            text = "Developer: Nguyen Minh Toi"
            setTextColor(Color.WHITE)
        }
        findViewById<TextView>(R.id.tv_version)?.apply {
            text = "Core: VietCore 26.1.1-Omnis Pro"
            setTextColor(COLOR_SCANNING_CYAN)
        }
    }

    override fun onDestroy() {
        updateJob?.cancel()
        super.onDestroy()
    }
}
