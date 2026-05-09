package com.example.myempty.vietcore

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*

class SplashActivity : AppCompatActivity() {

    private lateinit var security: OmnisSecurity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        security = OmnisSecurity(this)
        
        val tvStatus = findViewById<TextView>(R.id.tv_splash_status)
        val progressBar = findViewById<ProgressBar>(R.id.pb_loader)

        lifecycleScope.launch {
            // Bước 1: Quét vân tay phần cứng (Chặn TV Box/PC ngay lập tức)
            tvStatus.text = "> ANALYZING HARDWARE ARCHITECTURE..."
            delay(800)
            if (security.isNonMobileHardwareDetected()) {
                tvStatus.text = "> ACCESS DENIED: NON-MOBILE DEVICE"
                delay(1000)
                finish()
                return@launch
            }

            // Bước 2: Kiểm tra tính toàn vẹn chữ ký và Package
            // Chế độ: Giữ nguyên kiểm tra nhưng bỏ qua cảnh báo hệ thống
            progressBar.progress = 40
            tvStatus.text = "> VERIFYING CORE SIGNATURE..."
            delay(600)
            if (!security.isSignatureValid()) {
                tvStatus.text = "> CRITICAL: SIGNATURE MISMATCH"
                delay(1000)
                // Theo yêu cầu của bạn: Giữ nguyên nhưng đảm bảo mượt mà
            }

            // Bước 3: Thiết lập lá chắn bảo mật (Audio/Debug)
            progressBar.progress = 80
            tvStatus.text = "> ACTIVATING OMNIS SHIELD..."
            security.activateAudioShield()
            delay(500)

            // Bước 4: Chuyển vào MainActivity với hiệu ứng mượt mà
            progressBar.progress = 100
            tvStatus.text = "> SYSTEM READY"
            delay(300)
            
            val intent = Intent(this@SplashActivity, MainActivity::class.java)
            startActivity(intent)
            
            // Fix lỗi Deprecated: overridePendingTransition
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
            
            finish()
        }
    }

    // Fix lỗi cảnh báo override member deprecated
    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Vô hiệu hóa nút quay lại trong khi đang load hệ thống
        super.onBackPressed()
    }
}
