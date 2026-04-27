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
 * Cơ chế: Giữ nguyên chế độ tự hủy và tích hợp lá chắn chống nghe lén/ghi âm.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var security: OmnisSecurity
    private lateinit var simulator: DeviceSimulator
    private lateinit var networkClient: SecurityClient

    private var apiStatus = "PENDING"
    private var updateJob: Job? = null
    private var selfDestructStarted = false
    private var currentErrorCode: Int = 0

    // Bảng màu hệ thống cố định
    private val COLOR_MATRIX_GREEN = Color.parseColor("#00FF41")
    private val COLOR_DANGER_RED = Color.parseColor("#FF0000")
    private val COLOR_WARNING_ORANGE = Color.parseColor("#FF8C00")
    private val COLOR_SCANNING_CYAN = Color.parseColor("#00FFFF")
    private val COLOR_OFFLINE_YELLOW = Color.parseColor("#FFFF00")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Lớp bảo mật chặn Capture/Record màn hình (Cố định)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        setContentView(R.layout.activity_main)

        // Khởi tạo bộ ba đồng bộ
        security = OmnisSecurity(this)
        simulator = DeviceSimulator(this)
        networkClient = SecurityClient.getInstance()

        // Kích hoạt quét đe dọa lân cận và lá chắn chống nghe lén ngầm
        security.startRealTimeIntelligence()

        setupFooterInfo()
        performInitialSecurityCheck()
    }

    /**
     * Kiểm tra tính toàn vẹn qua SecurityClient trước khi khởi động giám sát
     */
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
                    // --- ĐỒNG BỘ KIỂM TRA TỪ OMNISSECURITY ---
                    val isOriginal = security.isOriginalPackage()
                    val isOwner = isOriginal && !security.isSignatureValid()

                    val isLegacyOS = security.isLegacyOSDetected()
                    val isOutdatedHW = security.isOutdatedHardware()
                    val isEmulator = security.isEmulatorOrVirtualMachine()
                    val isRooted = security.isRooted()
                    val isCustomROM = security.isCustomROMDetected()
                    val isBootloaderUnlocked = security.isBootloaderUnlocked()
                    
                    val isCloned = security.isAppCloned()
                    val isDebugging = security.isDebugging()
                    val isHackerTools = security.isHackerToolsDetected()
                    
                    // Mới: Kiểm tra đe dọa lân cận, Wifi Debug và CHỐNG NGHE LÉN
                    val isNearbyThreat = security.isNearbyInterferenceDetected()
                    val isWifiDebug = security.isWifiDebuggingEnabled()
                    val isEavesdropping = security.isMicrophoneInUse() // Phát hiện ghi âm trái phép

                    val isNameTampered = security.isAppNameModified("VietCore")
                    val isManifestBroken = security.isManifestTampered()
                    val isResourceBroken = security.isResourceModified()
                    val isStructureModified = security.isManifestStructuralTampered()

                    val currentViolation = when {
                        !isOriginal || isManifestBroken || isResourceBroken || isStructureModified -> "APK INTEGRITY BREACH"
                        isHackerTools || isDebugging || isCloned -> "HACKER TOOLS DETECTED"
                        isNearbyThreat -> "PROXIMITY THREAT DETECTED"
                        isWifiDebug -> "WIRELESS DEBUGGING ACTIVE"
                        isEavesdropping -> "AUDIO SURVEILLANCE DETECTED" // Kích hoạt khi có nghe lén
                        !isOwner && (isRooted || isEmulator) -> "UNSECURE ENVIRONMENT"
                        !isOwner && (isCustomROM || isBootloaderUnlocked) -> "HARDWARE BREACH"
                        isLegacyOS || isOutdatedHW -> "OUTDATED ENVIRONMENT"
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
                            // --- ĐỒNG BỘ DỮ LIỆU TỪ DEVICESIMULATOR ---
                            val specs = simulator.getRealTimeSpecs()
                            tvDevice?.text = specs
                            updateSecurityStatusLabel(tvStatus)
                            errorOverlay?.visibility = View.GONE
                            currentErrorCode = 0 
                            
                            // Gửi báo cáo định kỳ
                            networkClient.sendSecureReport(security.encryptData(specs))
                        }
                    }
                } catch (e: Exception) {
                    // Bảo mật tuyệt đối
                }
                delay(2000)
            }
        }
    }

    /**
     * CHẾ ĐỘ TỰ HỦY CỐ ĐỊNH (4.0 GIÂY)
     */
    private fun handleBankGradeViolation(reason: String, statusView: TextView?, overlay: View?, errorText: TextView?) {
        val tvTimer = findViewById<TextView>(R.id.tv_self_destruct_timer)

        statusView?.text = "SHIELD: TERMINATED"
        statusView?.setTextColor(COLOR_DANGER_RED)
        overlay?.visibility = View.VISIBLE
        errorText?.text = "UNAUTHORIZED INTERVENTION: $reason"

        if (!selfDestructStarted) {
            selfDestructStarted = true
            updateJob?.cancel()

            lifecycleScope.launch(Dispatchers.Main) {
                // Đếm ngược chính xác từ 4.0s về 0s
                for (i in 40 downTo 0) {
                    val seconds = i / 10.0
                    tvTimer?.text = String.format(Locale.US, "SELF-DESTRUCT IN: %.1fs", seconds)
                    delay(100)
                }
                security.activateSelfDestruct() // Xóa sạch dấu vết
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
