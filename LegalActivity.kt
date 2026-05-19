package com.example.myempty.vietcore

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale

/**
 * LegalActivity: Quản lý điều khoản và xác nhận pháp lý người dùng.
 * TÍNH NĂNG: Đã đồng bộ gọi lại cài đặt là "settings".
 */
class LegalActivity : AppCompatActivity() {

    // --- ĐỒNG BỘ NGÔN NGỮ TỪ CÀI ĐẶT ---
    override fun attachBaseContext(newBase: Context) {
        // Gọi lại cài đặt từ file "settings"
        val sharedPref = newBase.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val languageCode = sharedPref.getString("Locale.Helper.Selected.Language", "en") ?: "en"
        
        val locale = Locale.forLanguageTag(languageCode)
        Locale.setDefault(locale)
        
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        
        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Sử dụng file "settings" để kiểm tra trạng thái LEGAL_ACCEPTED
        val sharedPref = getSharedPreferences("settings", Context.MODE_PRIVATE)
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
            tvContent.text = "> [SYSTEM ERROR]: Unauthorized access to legal assets."
        }

        // --- LOGIC XÁC NHẬN (CHECKBOX ĐÁNH DẤU) ---
        cbAgree.setOnCheckedChangeListener { _, isChecked ->
            btnAccept.isEnabled = isChecked
            btnAccept.alpha = if (isChecked) 1.0f else 0.5f
        }

        // Nếu từ chối: Đóng toàn bộ ứng dụng ngay lập tức
        btnDecline.setOnClickListener {
            finishAffinity()
        }

        // Nếu đồng ý: Lưu trạng thái vào "settings" và tiến vào Giao diện chính
        btnAccept.setOnClickListener {
            sharedPref.edit().putBoolean("LEGAL_ACCEPTED", true).apply()
            
            val intent = Intent(this@LegalActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Khóa nút quay lại để bắt buộc xác nhận điều khoản
    }
}
