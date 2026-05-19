package com.example.myempty.vietcore

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*

/**
 * SplashActivity: Hệ thống khởi động VietCore.
 * ĐÃ FIX LỖI: Cảnh báo Deprecated cho phương thức onBackPressed.
 * Developer: Nguyen Minh Toi.
 */
class SplashActivity : AppCompatActivity() {

    // Giả định OmnisSecurity đã được định nghĩa trong project của bạn
    private lateinit var security: OmnisSecurity
    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        security = OmnisSecurity(this)
        tvStatus = findViewById(R.id.tv_boot_status)
        val progressBar = findViewById<ProgressBar>(R.id.pb_loader)

        lifecycleScope.launch {
            // --- BƯỚC 1: KIỂM TRA PHẦN CỨNG ---
            updateStatus(getString(R.string.log_hardware_scan)) 
            delay(1000)
            
            if (security.isNonMobileHardwareDetected()) {
                updateStatus(getString(R.string.log_hardware_invalid))
                delay(1200)
                finish()
                return@launch
            }

            // --- BƯỚC 2: KÍCH HOẠT LÕI BẢO MẬT ---
            updateStatus(getString(R.string.log_security_active))
            delay(700)
            security.activateAudioShield() 
            
            updateStatus(getString(R.string.log_scanning_integrity))
            delay(800)

            // --- BƯỚC 3: ĐIỀU PHỐI GIAO DIỆN ---
            updateStatus(getString(R.string.toast_applying))
            delay(500)
            
            val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
            
            val accepted = prefs.getBoolean("LEGAL_ACCEPTED", false)
            val isRadarEnabled = prefs.getBoolean("radar_enabled", false)
            val isDevModeActive = prefs.getBoolean("dev_mode_active", false)
            val isTerminalActive = prefs.getBoolean("terminal_core_active", false)

            val nextIntent = Intent()
            val pkg = packageName

            when {
                !accepted -> nextIntent.setClassName(pkg, "$pkg.LegalActivity")
                isTerminalActive -> nextIntent.setClassName(pkg, "$pkg.TerminalActivity")
                isDevModeActive -> nextIntent.setClassName(pkg, "$pkg.LabsActivity")
                isRadarEnabled -> nextIntent.setClassName(pkg, "$pkg.RadarActivity")
                else -> nextIntent.setClassName(pkg, "$pkg.MainActivity")
            }
            
            try {
                nextIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(nextIntent)
            } catch (e: Exception) {
                val failsafe = Intent(this@SplashActivity, MainActivity::class.java)
                failsafe.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(failsafe)
            }
            
            applyTransition()
            finish()
        }
    }

    private fun updateStatus(msg: String) {
        tvStatus.post {
            tvStatus.text = msg
        }
    }

    private fun applyTransition() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(
                OVERRIDE_TRANSITION_OPEN,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    // FIX LỖI TẠI ĐÂY: Bổ sung Deprecated để khớp với định nghĩa hệ thống
    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        // Khóa nút back bằng cách không gọi super.onBackPressed()
    }
}
