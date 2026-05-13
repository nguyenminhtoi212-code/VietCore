 package com.example.myempty.vietcore

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar

/**
 * SettingsActivity: VietCore Configuration Center.
 * Developer: Nguyen Minh Toi.
 * Hệ thống bảo mật 2 lớp: Nhật ký cập nhật (Dialog) & Chính sách pháp lý (Activity).
 */
class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // --- THIẾT LẬP TOOLBAR (Đồng bộ tiêu đề hệ thống) ---
        val toolbar = findViewById<Toolbar>(R.id.toolbar_settings)
        if (toolbar != null) {
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = "Settings" 
            toolbar.setNavigationOnClickListener { finish() }
        }

        // --- ÁNH XẠ CÁC THÀNH PHẦN (Cố định theo XML VietCore) ---
        val btnLight = findViewById<LinearLayout>(R.id.btn_light_mode)
        val btnDark = findViewById<LinearLayout>(R.id.btn_dark_mode)
        val radioLight = findViewById<RadioButton>(R.id.radio_light)
        val radioDark = findViewById<RadioButton>(R.id.radio_dark)
        val btnPermissions = findViewById<LinearLayout>(R.id.btn_manage_permissions)
        
        // Cố định ID mục Pháp lý để tránh lỗi Unresolved reference
        val btnLegal = findViewById<LinearLayout>(R.id.btn_legal_policy)
        val tvLegalTitle = findViewById<TextView>(R.id.tv_legal_title)
        
        val tvWhatsNew = findViewById<TextView>(R.id.tv_whats_new_content)
        val btnApply = findViewById<Button>(R.id.btn_apply_settings)
        val btnRecovery = findViewById<Button>(R.id.btn_system_recovery)

        // 1. Đồng bộ "What's New" (Layer 1: Internal Logs)
        tvWhatsNew?.text = getSummaryFromAssets()
        tvWhatsNew?.setOnClickListener {
            showSecureDialog("UPDATE_NOTES.txt", "#00FF41") // Hiển thị màu xanh Kernel
        }

        // 2. Chính sách pháp lý (Layer 2: External Compliance)
        // Sử dụng Intent để mở LegalActivity thay vì Dialog để người dùng xem lại đầy đủ
        btnLegal?.setOnClickListener {
            val intent = Intent(this, LegalActivity::class.java).apply {
                // Flag báo hiệu xem lại từ Settings, không được tự động thoát app
                putExtra("FROM_SETTINGS", true)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            startActivity(intent)
        }

        // 3. Logic Theme (Kiểm tra và đồng bộ trạng thái RadioButton)
        val currentMode = AppCompatDelegate.getDefaultNightMode()
        updateRadioState(currentMode, radioLight, radioDark)

        btnLight?.setOnClickListener {
            updateTheme(AppCompatDelegate.MODE_NIGHT_NO)
            updateRadioState(AppCompatDelegate.MODE_NIGHT_NO, radioLight, radioDark)
        }

        btnDark?.setOnClickListener {
            updateTheme(AppCompatDelegate.MODE_NIGHT_YES)
            updateRadioState(AppCompatDelegate.MODE_NIGHT_YES, radioLight, radioDark)
        }

        // 4. Quản lý quyền hệ thống (Permission Sync)
        btnPermissions?.setOnClickListener {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
            startActivity(intent)
        }

        // 5. Kết thúc phiên làm việc (Apply & Recovery)
        btnApply?.setOnClickListener { finish() }
        btnRecovery?.setOnClickListener { 
            // Logic Recovery: Thoát ra màn hình chính
            finish() 
        }
    }

    private fun updateTheme(mode: Int) {
        AppCompatDelegate.setDefaultNightMode(mode)
        delegate.applyDayNight()
    }

    private fun updateRadioState(mode: Int, light: RadioButton?, dark: RadioButton?) {
        if (mode == AppCompatDelegate.MODE_NIGHT_YES) {
            dark?.isChecked = true
            light?.isChecked = false
        } else {
            light?.isChecked = true
            dark?.isChecked = false
        }
    }

    /**
     * showSecureDialog: Hiển thị nội dung kernel từ assets.
     * Tối ưu cho phong cách High-Tech Terminal.
     */
    private fun showSecureDialog(fileName: String, colorHex: String) {
        val dialog = Dialog(this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
            setPadding(40, 40, 40, 40)
        }

        val scroll = ScrollView(this)
        val content = TextView(this).apply {
            text = try {
                assets.open(fileName).bufferedReader().use { it.readText() }
            } catch (e: Exception) { "> Access Denied: Kernel data secured." }
            
            setTextColor(Color.parseColor(colorHex))
            typeface = Typeface.MONOSPACE
            textSize = 12f
            setLineSpacing(8f, 1f)
        }

        scroll.addView(content)
        layout.addView(scroll)

        dialog.setContentView(layout)
        dialog.window?.let {
            it.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            it.setBackgroundDrawable(ColorDrawable(Color.BLACK))
        }
        dialog.show()
    }

    private fun getSummaryFromAssets(): String {
        return try {
            assets.open("UPDATE_NOTES.txt").bufferedReader().useLines { lines ->
                // Chỉ lấy các dòng bắt đầu bằng dấu ">" để hiển thị tóm tắt
                lines.filter { it.trim().startsWith(">") }.take(3).joinToString("\n")
            }
        } catch (e: Exception) { "> VietCore Intelligence: System Active" }
    }
}
