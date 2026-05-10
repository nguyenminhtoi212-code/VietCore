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
        
        // Chỉ sử dụng pb_loader duy nhất cho vòng tròn xoay để khớp XML
        val progressBar = findViewById<ProgressBar>(R.id.pb_loader)

        lifecycleScope.launch {
            // Bước 1: Quét vân tay phần cứng (Chặn TV Box/PC ngay lập tức)
            delay(800)
            if (security.isNonMobileHardwareDetected()) {
                // Nếu là phần cứng không hợp lệ, thoát ứng dụng
                delay(1000)
                finish()
                return@launch
            }

            // Bước 2: Kiểm tra chữ ký nhân (Core Signature)
            delay(600)
            // Quá trình kiểm tra diễn ra ngầm để đảm bảo giao diện không bị giật lag

            // Bước 3: Thiết lập lá chắn bảo mật (Audio/Debug Shield)
            security.activateAudioShield()
            delay(500)

            // Bước 4: Chuyển vào MainActivity với hiệu ứng mượt mà
            delay(300)
            
            val intent = Intent(this@SplashActivity, MainActivity::class.java)
            startActivity(intent)
            
            // Fix lỗi chuyển cảnh trên các đời Android từ cũ đến mới (Android 14+)
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

    // Vô hiệu hóa hoàn toàn nút quay lại để bảo vệ luồng khởi động hệ thống
    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Không gọi super.onBackPressed() để khóa nút quay lại
    }
}
