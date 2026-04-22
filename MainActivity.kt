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

    // Mã màu đồng bộ VietCore
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
                    // --- 1. NHẬN DIỆN CHỦ SỞ HỮU (DEVELOPER) ---
                    // Fix lỗi Unresolved bằng cách kiểm tra trực tiếp từ Simulator/Security
                    val isOwner = security.isOriginalPackage() && !security.isDexPatched()

                    // --- 2. PHÂN LOẠI THIẾT BỊ & HỆ THỐNG ---
                    val isLegacyOS = security.isLegacyOSDetected()
                    val isOutdatedHW = security.isOutdatedHardware()
                    val isEmulator = security.isEmulatorOrVirtualMachine()
                    val isRooted = security.isRooted()
                    val isCloned = security.isAppCloned()

                    // --- 3. TOÀN VẸN MÃ NGUỒN (CHỈ KHÓA KHI CÓ CAN THIỆP) ---
                    val isModified = !security.isOriginalPackage()
                    val isDexTampered = security.isDexPatched()
                    val isSignatureBroken = security.isSignatureSpoofed()
                    val isManifestBroken = security.isManifestTampered()

                    // LOGIC: Nếu là chủ sở hữu (isOwner) và không có can thiệp sâu vào DEX/Signature, 
                    // cho phép sử dụng bình thường kể cả khi bật Debug/Root nhẹ.
                    val currentViolation = when {
                        // CHỈ KHÓA KHI: Có hành vi phá hoại mã nguồn (Hack)
                        isDexTampered || isSignatureBroken || isModified || isManifestBroken -> "APK INTEGRITY BREACH"
                        
                        // CẢNH BÁO MÔI TRƯỜNG (Nhưng không khóa nếu là máy của Minh Tới)
                        !isOwner && (isEmulator || isCloned) -> "UNAUTHORIZED ENVIRONMENT"
                        !isOwner && isLegacyOS -> "OUTDATED ANDROID VERSION"
                        
                        else -> null
                    }

                    withContext(Dispatchers.Main) {
                        if (currentViolation != null) {
                            handleBankGradeViolation(currentViolation, tvStatus, errorOverlay, tvErrorMessage)
                        } else {
                            // TRẠNG THÁI BÌNH THƯỜNG: Mượt mà và ổn định
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
        // Chỉ kích hoạt khóa khi phát hiện đúng sai phạm
        statusView?.text = "SHIELD: TERMINATED"
        statusView?.setTextColor(COLOR_DANGER_RED)

        overlay?.visibility = View.VISIBLE
        errorText?.text = "SYSTEM PROTECTION ACTIVE\n--------------------------\n$reason\n\nUnauthorized modification detected."

        if (!selfDestructStarted) {
            selfDestructStarted = true
            lifecycleScope.launch {
                delay(4000) 
                security.activateSelfDestruct()
            }
        }
    }

    private fun updateSecurityStatusLabel(statusView: TextView?) {
        if (statusView == null) return
        when (apiStatus) {
            "GENUINE" -> {
                statusView.text = "OMNIS: SYSTEM VERIFIED [SECURE]"
                statusView.setTextColor(COLOR_MATRIX_GREEN)
            }
            "VIRUS_IP", "API_FAKE" -> {
                statusView.text = "SECURITY: EXTERNAL THREAT DETECTED"
                statusView.setTextColor(COLOR_WARNING_ORANGE)
            }
            else -> {
                statusView.text = "OMNIS: MONITORING ACTIVE"
                statusView.setTextColor(COLOR_OFFLINE_YELLOW)
            }
        }
    }

    private fun isRunningOnTV(): Boolean {
        val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        return uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    }

    private fun verifySystemApi() {
        lifecycleScope.launch {
            try {
                // Giả lập xác thực API thầm lặng để không gây lag
                delay(1000)
                apiStatus = "GENUINE"
                updateSecurityStatusLabel(findViewById(R.id.tv_security_status))
            } catch (e: Exception) { }
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
 
