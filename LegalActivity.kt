package com.example.myempty.vietcore

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * LegalActivity: Quản lý điều khoản và xác nhận pháp lý người dùng.
 * Đảm bảo tính minh bạch và an toàn hệ thống trước khi truy cập Core.
 */
class LegalActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Kiểm tra nếu đã đồng ý trước đó thì vào thẳng MainActivity
        val sharedPref = getSharedPreferences("VietCorePrefs", Context.MODE_PRIVATE)
        if (sharedPref.getBoolean("LEGAL_ACCEPTED", false)) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_legal)

        val tvContent = findViewById<TextView>(R.id.tv_legal_content)
        val btnAccept = findViewById<Button>(R.id.btn_accept)
        val btnDecline = findViewById<Button>(R.id.btn_decline)
        val cbAgree = findViewById<CheckBox>(R.id.cb_agree)

        // Đọc nội dung điều khoản từ assets/LEGAL_POLICY.txt
        try {
            val inputStream = assets.open("LEGAL_POLICY.txt")
            val reader = BufferedReader(InputStreamReader(inputStream))
            val content = reader.readText()
            tvContent.text = content
            inputStream.close()
        } catch (e: Exception) {
            tvContent.text = "> [SYSTEM ERROR]: Không thể tải nội dung điều khoản pháp lý."
        }

        // --- LOGIC XÁC NHẬN (CHECKBOX ĐÁNH DẤU) ---
        cbAgree.setOnCheckedChangeListener { _, isChecked ->
            // Kích hoạt nút Accept nếu đã tích chọn đồng ý
            btnAccept.isEnabled = isChecked
            // Điều chỉnh độ mờ để biểu thị trạng thái kích hoạt
            btnAccept.alpha = if (isChecked) 1.0f else 0.5f
        }

        // Nếu từ chối: Đóng toàn bộ ứng dụng ngay lập tức
        btnDecline.setOnClickListener {
            finishAffinity()
        }

        // Nếu đồng ý: Lưu trạng thái và tiến vào Giao diện chính (MainActivity)
        btnAccept.setOnClickListener {
            // Lưu trạng thái đã đồng ý vào hệ thống VietCore
            sharedPref.edit().putBoolean("LEGAL_ACCEPTED", true).apply()
            
            // Theo yêu cầu: Chuyển trực tiếp sang MainActivity, không qua LoginActivity
            val intent = Intent(this@LegalActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    // Khóa nút quay lại để bắt buộc thực hiện lựa chọn pháp lý
    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Không thực hiện hành động gì để bảo vệ luồng xác nhận
    }
}
