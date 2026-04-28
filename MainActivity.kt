package com.example.myempty.vietcore

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import java.util.Locale
import kotlin.random.Random

/**
 * MainActivity: Trung tâm điều phối bảo mật VietCore 2026.
 * Đồng bộ: Chống điều khiển từ xa, Giám sát Trợ năng và Toàn vẹn DEX.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var security: OmnisSecurity
    private lateinit var simulator: DeviceSimulator
    private lateinit var networkClient: SecurityClient

    private var apiStatus = "PENDING"
    private var updateJob: Job? = null
    private var selfDestructStarted = false
    private var currentErrorCode: Int = 0

    // Bảng màu hệ thống
    private val COLOR_MATRIX_GREEN = Color.parseColor("#00FF41")
    private val COLOR_DANGER_RED = Color.parseColor("#FF0000")
    private val COLOR_SCANNING_CYAN = Color.parseColor("#00FFFF")
    private val COLOR_OFFLINE_YELLOW = Color.parseColor("#FFFF00")

    private val AUTHORIZED_PACKAGE = "com.example.myempty.vietcore"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ngăn chặn chụp ảnh và quay màn hình
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        setContentView(R.layout.activity_main)

        security = OmnisSecurity(this)
        simulator = DeviceSimulator(this)
        networkClient = SecurityClient.getInstance()
        security.startRealTimeIntelligence()

        setupFooterInfo()
        performInitialSecurityCheck()
    }

    private fun performInitialSecurityCheck() {
        lifecycleScope.launch(Dispatchers.Main) {
            val payload = security.encryptData("LAUNCH_INIT_${System.currentTimeMillis()}")
            val response = networkClient.verifyLaunchIntegrity(payload)
            
            if (response.contains("GENUINE") || response == "GENUINE_EMPTY") {
                apiStatus = "1001"
                startRealtimeMonitoring()
            } else if (response.contains("CONNECTION_COMPROMISED")) {
                apiStatus = "OFFLINE"
                startRealtimeMonitoring() 
            } else {
                handleBankGradeViolation("SERVER_INTEGRITY_REJECTION", null, findViewById(R.id.error_overlay), null)
            }
        }
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
                    val currentPkg = packageName
                    val isWrongIdentity = currentPkg != AUTHORIZED_PACKAGE
                    
                    val isOriginal = security.isOriginalPackage()
                    val isOwner = isOriginal && security.isSignatureValid()

                    // Các chỉ số bảo mật từ OmnisSecurity
                    val isEmulator = security.isEmulatorOrVirtualMachine() 
                    val isRooted = security.isRooted()
                    val isCustomROM = security.isCustomROMDetected()
                    val isBootloaderUnlocked = security.isBootloaderUnlocked()
                    
                    val isCloned = security.isAppCloned()
                    val isDebugging = security.isDebugging()
                    val isHackerTools = security.isHackerToolsDetected()
                    val isWifiDebug = security.isWifiDebuggingEnabled()
                    
                    val isDexModified = security.isClassesDexTampered() 
                    val isManifestBroken = security.isManifestTampered()
                    val isResourceBroken = security.isResourceModified()
                    val isStructureModified = security.isManifestStructuralTampered()

                    // Chỉ số mới: Điều khiển từ xa & Nghe lén
                    val isRemoteControl = security.isRemoteControlActive()
                    val isEavesdropping = security.isMicrophoneInUse()

                    val currentViolation = when {
                        isWrongIdentity || isManifestBroken || isStructureModified -> "APK IDENTITY BREACH"
                        isDexModified -> "CORE LOGIC TAMPERED (DEX)"
                        isResourceBroken -> "RESOURCE INTEGRITY BREACH"
                        isRemoteControl -> "REMOTE CONTROL / ACCESSIBILITY EXPLOIT"
                        isHackerTools || isDebugging || isCloned -> "HACKER TOOLS DETECTED"
                        isWifiDebug -> "WIRELESS DEBUGGING ACTIVE"
                        isEavesdropping -> "AUDIO SURVEILLANCE DETECTED"
                        !isOwner && (isRooted || isEmulator) -> "UNSECURE ENVIRONMENT"
                        !isOwner && (isCustomROM || isBootloaderUnlocked) -> "HARDWARE BREACH"
                        else -> null
                    }

                    withContext(Dispatchers.Main) {
                        if (currentViolation != null) {
                            if (currentErrorCode == 0) {
                                currentErrorCode = Random.nextInt(1000, 9999)
                            }
                            tvErrorCodeDigit?.text = "INTERNAL_ERROR: $currentErrorCode"
                            handleBankGradeViolation(currentViolation, tvStatus, errorOverlay, tvErrorMessage)
                        } else {
                            val specs = simulator.getRealTimeSpecs()
                            tvDevice?.text = specs
                            updateSecurityStatusLabel(tvStatus)
                            errorOverlay?.visibility = View.GONE
                            currentErrorCode = 0 
                            
                            networkClient.sendSecureReport(security.encryptData(specs))
                        }
                    }
                } catch (e: Exception) {
                    // Fail-safe logic
                }
                delay(2000)
            }
        }
    }

    /**
     * Hiển thị Overlay vi phạm. 
     * Lưu ý: activateSelfDestruct() hiện đã được vô hiệu hóa bên trong OmnisSecurity.
     */
    private fun handleBankGradeViolation(reason: String, statusView: TextView?, overlay: View?, errorText: TextView?) {
        val tvTimer = findViewById<TextView>(R.id.tv_self_destruct_timer)

        statusView?.text = "SHIELD: TERMINATED"
        statusView?.setTextColor(COLOR_DANGER_RED)
        overlay?.visibility = View.VISIBLE
        errorText?.text = "INTEGRITY BREACH: $reason"

        if (!selfDestructStarted) {
            selfDestructStarted = true
            updateJob?.cancel()

            lifecycleScope.launch(Dispatchers.Main) {
                // Hiển thị đếm ngược nhưng không xóa dữ liệu (Theo OmnisSecurity cập nhật)
                for (i in 40 downTo 0) {
                    val seconds = i / 10.0
                    tvTimer?.text = String.format(Locale.US, "SHIELD LOCKDOWN IN: %.1fs", seconds)
                    delay(100)
                }
                // Gọi hàm stub - Không còn gây ra mất dữ liệu
                security.activateSelfDestruct() 
                finishAffinity()
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
            "OFFLINE" -> {
                statusView.text = "OMNIS: OFFLINE MODE ACTIVE"
                statusView.setTextColor(COLOR_OFFLINE_YELLOW)
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
            text = "Core: VietCore 26.1.2-Omnis Pro"
            setTextColor(COLOR_SCANNING_CYAN)
        }
    }

    override fun onDestroy() {
        updateJob?.cancel()
        super.onDestroy()
    }
}
