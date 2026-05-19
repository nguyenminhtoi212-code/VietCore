package com.example.myempty.vietcore

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial

/**
 * LabsActivity: Trung tâm nghiên cứu và thử nghiệm VietCore Intelligence.
 * Tên cũ: ExperimentalActivity (Đã rút gọn để tối ưu mã nguồn).
 * Developer: Nguyen Minh Toi.
 */
class LabsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Cập nhật: Sử dụng layout activity_labs.xml đã được đổi tên
        setContentView(R.layout.activity_labs)

        // --- ÁNH XẠ THÀNH PHẦN (Khớp chính xác ID XML) ---
        val btnExitDev = findViewById<Button>(R.id.btn_close_dev_mode)
        val btnStressTest = findViewById<Button>(R.id.btn_run_stress_test)
        val btnBackHome = findViewById<ImageButton>(R.id.btn_dev_menu)
        
        val switchBypass = findViewById<SwitchMaterial>(R.id.switch_bypass_sig)
        val switchMemory = findViewById<SwitchMaterial>(R.id.switch_memory_scan)
        val switchAI = findViewById<SwitchMaterial>(R.id.switch_ai_predict)

        // --- XỬ LÝ CHUYỂN ĐỔI GIAO DIỆN ---
        
        // Quay về trang chủ từ Header
        btnBackHome.setOnClickListener {
            disableDevModeAndExit()
        }

        // Quay về trang chủ từ nút đóng cuối danh sách
        btnExitDev.setOnClickListener {
            disableDevModeAndExit()
        }

        // --- XỬ LÝ TÍNH NĂNG THỬ NGHIỆM ---
        
        // Gỡ bỏ xác minh chữ ký (Bypass Signature)
        switchBypass.setOnCheckedChangeListener { _, isChecked ->
            savePreference("bypass_signature", isChecked)
            showStatusToast(getString(R.string.switch_bypass_sig), isChecked)
        }

        // Quét mã Hex bộ nhớ sâu (Memory Scan)
        switchMemory.setOnCheckedChangeListener { _, isChecked ->
            savePreference("hex_scan", isChecked)
            showStatusToast(getString(R.string.switch_memory_scan), isChecked)
        }

        // Dự đoán tính toàn vẹn Neural (AI Predict)
        switchAI.setOnCheckedChangeListener { _, isChecked ->
            savePreference("ai_predict", isChecked)
            showStatusToast(getString(R.string.switch_ai_predict), isChecked)
        }

        // Nút Stress Test
        btnStressTest.setOnClickListener {
            Toast.makeText(this, "CRITICAL: System Stress Test Initialized...", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Tắt chế độ Labs và quay về màn hình chính.
     */
    private fun disableDevModeAndExit() {
        getSharedPreferences("settings", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("dev_mode_active", false)
            .apply()
        
        Toast.makeText(this, "> ${getString(R.string.toast_applying)}", Toast.LENGTH_SHORT).show()
        
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        
        finish()
    }

    /**
     * Lưu trạng thái Labs vào bộ nhớ riêng biệt (VietCore_Labs)
     */
    private fun savePreference(key: String, value: Boolean) {
        getSharedPreferences("settings", Context.MODE_PRIVATE)
            .edit().putBoolean(key, value).apply()
    }

    /**
     * Hiển thị trạng thái Terminal
     */
    private fun showStatusToast(feature: String, isEnabled: Boolean) {
        val status = if (isEnabled) "ENABLED" else "DISABLED"
        Toast.makeText(this, "$feature: $status", Toast.LENGTH_SHORT).show()
    }
}
