package com.example.myempty.vietcore

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import java.util.Locale
import kotlin.random.Random

/**
 * MainActivity: VietCore 2026 Security Coordination Center.
 * Developer: Nguyen Minh Toi.
 * Integration: Non-mobile hardware blocking (TV Box, PC, Emulator) 
 * and Secure Navigation Menu System.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var security: OmnisSecurity
    private lateinit var simulator: DeviceSimulator
    private lateinit var networkClient: SecurityClient

    private var apiStatus = "PENDING"
    private var updateJob: Job? = null
    private var selfDestructStarted = false
    private var currentErrorCode: Int = 0

    private val COLOR_MATRIX_GREEN = Color.parseColor("#00FF41")
    private val COLOR_DANGER_RED = Color.parseColor("#FF0000")
    private val COLOR_SCANNING_CYAN = Color.parseColor("#00FFFF")
    private val COLOR_OFFLINE_YELLOW = Color.parseColor("#FFFF00")

    private val AUTHORIZED_PACKAGE = "com.example.myempty.vietcore"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Block screenshots and screen recording (Bank-Grade Security)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        setContentView(R.layout.activity_main)

        // Setup Header/ActionBar
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        supportActionBar?.title = "VietCore Dashboard"

        // Initialize Core Modules
        security = OmnisSecurity(this)
        simulator = DeviceSimulator(this)
        networkClient = SecurityClient.getInstance()
        
        // Activate AI Shield
        security.startRealTimeIntelligence()

        setupHeaderInfo() 
        performInitialSecurityCheck()
    }

    // --- NAVIGATION MENU SYSTEM ---

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Tự động hiển thị menu từ tài nguyên XML đã tạo
        menuInflater.inflate(R.menu.security_nav_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.nav_back -> {
                // Quay lại nhưng vẫn giữ ứng dụng chạy mượt mà
                onBackPressedDispatcher.onBackPressed()
                true
            }
            R.id.nav_exit -> {
                // Chỉ thoát khi người dùng chủ động xác nhận OK
                showExitConfirmation()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showExitConfirmation() {
        // Cố định sử dụng SecurityDialogTheme để đồng bộ giao diện Dark Mode
        AlertDialog.Builder(this, R.style.SecurityDialogTheme)
            .setTitle("Exit Confirmation")
            .setMessage("The VietCore system will safely disconnect. Do you wish to proceed?")
            .setPositiveButton("OK") { _, _ ->
                // Hủy bỏ tiến trình cập nhật trước khi đóng hoàn toàn
                updateJob?.cancel()
                finishAffinity()
                System.exit(0)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // --- ADVANCED SECURITY LOGIC ---

    private fun performInitialSecurityCheck() {
        lifecycleScope.launch(Dispatchers.Main) {
            val payload = security.encryptData("LAUNCH_INIT_${System.currentTimeMillis()}")
            val response = networkClient.verifyLaunchIntegrity(payload)
            
            if (response.contains("GENUINE") || response == "GENUINE_EMPTY" || response.contains("SUCCESS")) {
                apiStatus = "1001"
                startRealtimeMonitoring()
            } else if (response.contains("NETWORK_COMPROMISED") || response.contains("OFFLINE")) {
                apiStatus = "OFFLINE"
                startRealtimeMonitoring() 
            } else {
                val errorOverlay = findViewById<View>(R.id.error_overlay)
                handleBankGradeViolation("SERVER_INTEGRITY_REJECTION", null, errorOverlay, null)
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

                    // Hardware & Environment Verification (Blocks TV Box/PC)
                    val isEmulator = security.isEmulatorOrVirtualMachine() 
                    val isRooted = security.isRooted()
                    val isCustomROM = security.isCustomROMDetected()
                    val isBootloaderUnlocked = security.isBootloaderUnlocked()
                    val isNonMobile = security.isNonMobileHardwareDetected() 
                    
                    val isCloned = security.isAppCloned()
                    val isDebugging = security.isDebugging()
                    val isHackerTools = security.isHackerToolsDetected()
                    val isWifiDebug = security.isWifiDebuggingEnabled()
                    val isDevOptions = security.isDeveloperOptionsEnabled()
                    
                    val isDexModified = security.isClassesDexTampered() 
                    val isManifestBroken = security.isManifestTampered()
                    val isResourceBroken = security.isResourceModified()
                    val isStructureModified = security.isManifestStructuralTampered()

                    val isRemoteControl = security.isRemoteControlActive()
                    val isEavesdropping = security.isMicrophoneInUse()

                    // Violation Logic
                    val currentViolation = when {
                        isNonMobile -> "HARDWARE RESTRICTION: MOBILE ONLY"
                        isDevOptions -> "DEVELOPER OPTIONS RESTRICTED"
                        isWrongIdentity || isManifestBroken || isStructureModified -> "APK IDENTITY BREACH"
                        isDexModified -> "CORE LOGIC TAMPERED (DEX)"
                        isResourceBroken -> "RESOURCE INTEGRITY BREACH"
                        isRemoteControl -> "REMOTE CONTROL EXPLOIT"
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
                            networkClient.syncToICloud(security.encryptData(specs))
                        }
                    }
                } catch (e: Exception) {}
                delay(2000) 
            }
        }
    }

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
                for (i in 40 downTo 0) {
                    val seconds = i / 10.0
                    tvTimer?.text = String.format(Locale.US, "SHIELD LOCKDOWN IN: %.1fs", seconds)
                    delay(100)
                }
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

    private fun setupHeaderInfo() {
        findViewById<TextView>(R.id.tv_author)?.apply {
            text = "Developer: Nguyen Minh Toi"
            setTextColor(Color.parseColor("#888888"))
        }
        findViewById<TextView>(R.id.tv_version)?.apply {
            text = "Core: VietCore 26.1.5-Beta"
            setTextColor(COLOR_SCANNING_CYAN)
        }
    }

    override fun onDestroy() {
        // Đảm bảo Job được hủy để tránh treo load bộ nhớ
        updateJob?.cancel()
        super.onDestroy()
    }
}
