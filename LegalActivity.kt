package com.example.myempty.vietcore

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.io.InputStreamReader

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

        // Đọc nội dung từ assets/LEGAL_POLICY.txt
        try {
            val inputStream = assets.open("LEGAL_POLICY.txt")
            val reader = BufferedReader(InputStreamReader(inputStream))
            val content = reader.readText()
            tvContent.text = content
            inputStream.close()
        } catch (e: Exception) {
            tvContent.text = "Error loading Legal Terms."
        }

        // Nếu từ chối: Thoát ứng dụng ngay lập tức
        btnDecline.setOnClickListener {
            finishAffinity()
            System.exit(0)
        }

        // Nếu đồng ý: Lưu trạng thái và mở MainActivity
        btnAccept.setOnClickListener {
            sharedPref.edit().putBoolean("LEGAL_ACCEPTED", true).apply()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}
