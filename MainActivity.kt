package com.example.myempty.vietcore

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.*
import java.util.Locale
import kotlin.random.Random

/**
 * MainActivity: VietCore 2026 Security Coordination Center.
 * Developer: Nguyen Minh Toi.
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

    // --- FIX LỖI: ĐỒNG BỘ NGÔN NGỮ ---
    override fun attachBaseContext(newBase: Context) {
        // Lấy ngôn ngữ đã lưu, nếu chưa có thì mặc định là tiếng Anh "en" hoặc tiếng Việt "vi"
        val lang = LocaleHelper.getLanguage(newBase) ?: "en"
        super.attachBaseContext(LocaleHelper.setLocale(newBase, lang))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Chặn ảnh chụp màn hình và ghi màn hình
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        setContentView(R.layout.activity_main)

        // --- THIẾT LẬP TOOLBAR ---
        val toolbar = findViewById<Toolbar>(R.id.toolbar_secure)
        if (toolbar != null) {
            setSupportActionBar(toolbar)
            supportActionBar?.title = getString(R.string.main_title)
        }

        // Khởi tạo các module lõi
        security = OmnisSecurity(this)
        simulator = DeviceSimulator(this)
        networkClient = SecurityClient.getInstance()
        
        security.startRealTimeIntelligence()

        setupHeaderInfo() 
        performInitialSecurityCheck()
    }

    // --- HỆ THỐNG MENU ĐIỀU HƯỚNG ---

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Add trực tiếp để đảm bảo đồng bộ dịch thuật ngay cả khi chuyển ngôn ngữ nóng
        menu.add(0, R.id.action_settings, 0, getString(R.string.menu_settings))
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
            
        menu.add(0, R.id.nav_exit, 1, getString(R.string.menu_exit))
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
            
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.nav_exit -> {
                showExitConfirmation()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showExitConfirmation() {
        MaterialAlertDialogBuilder(this, R.style.VietCore_Terminal_Dialog)
            .setTitle(getString(R.string.dialog_exit_title))
            .setMessage(getString(R.string.dialog_exit_msg))
            .setPositiveButton(getString(R.string.confirm_yes)) { _, _ ->
                updateJob?.cancel()
                finishAffinity()
            }
            .setNegativeButton(getString(R.string.confirm_no), null)
            .show()
    }

    // --- LOGIC BẢO MẬT & GIÁM SÁT ---

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
                    val currentViolation = checkSecurityViolations()

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

    private fun checkSecurityViolations(): String? {
        val isOriginal = security.isOriginalPackage()
        val isOwner = isOriginal && security.isSignatureValid()

        if (security.isNonMobileHardwareDetected()) return "HARDWARE RESTRICTION: MOBILE ONLY"
        if (security.isDeveloperOptionsEnabled()) return "DEVELOPER OPTIONS RESTRICTED"
        
        if (packageName != AUTHORIZED_PACKAGE || security.isManifestTampered() || security.isManifestStructuralTampered()) {
            return "APK IDENTITY BREACH"
        }
        
        if (security.isClassesDexTampered()) return "CORE LOGIC TAMPERED (DEX)"
        if (security.isResourceModified()) return "RESOURCE INTEGRITY BREACH"
        
        if (security.isRemoteControlActive()) return "REMOTE CONTROL EXPLOIT"
        if (security.isMicrophoneInUse()) return "AUDIO SURVEILLANCE DETECTED"
        if (security.isHackerToolsDetected() || security.isDebugging() || security.isAppCloned()) {
            return "HACKER TOOLS DETECTED"
        }
        if (security.isWifiDebuggingEnabled()) return "WIRELESS DEBUGGING ACTIVE"

        if (!isOwner) {
            if (security.isRooted() || security.isEmulatorOrVirtualMachine()) return "UNSECURE ENVIRONMENT"
            if (security.isCustomROMDetected() || security.isBootloaderUnlocked()) return "HARDWARE BREACH"
        }

        return null
    }

    private fun handleBankGradeViolation(reason: String, statusView: TextView?, overlay: View?, errorText: TextView?) {
        val tvTimer = findViewById<TextView>(R.id.tv_self_destruct_timer)
        statusView?.text = getString(R.string.status_unauthorized)
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
                statusView.text = getString(R.string.status_authorized)
                statusView.setTextColor(COLOR_MATRIX_GREEN)
            }
            "OFFLINE" -> {
                statusView.text = "OMNIS: OFFLINE MODE ACTIVE"
                statusView.setTextColor(COLOR_OFFLINE_YELLOW)
            }
            else -> {
                statusView.text = getString(R.string.log_security_active)
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
            text = getString(R.string.version_label)
            setTextColor(COLOR_SCANNING_CYAN)
        }
    }

    override fun onDestroy() {
        updateJob?.cancel()
        super.onDestroy()
    }
}
