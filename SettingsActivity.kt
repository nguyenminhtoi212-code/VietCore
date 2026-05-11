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
 * Đồng bộ hệ thống bảo mật 2 lớp: Nhật ký cập nhật & Chính sách pháp lý độc lập.
 */
class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // --- THIẾT LẬP TOOLBAR ---
        val toolbar = findViewById<Toolbar>(R.id.toolbar_settings)
        if (toolbar != null) {
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = "VietCore Config"
            toolbar.setNavigationOnClickListener { finish() }
        }

        // --- ÁNH XẠ CÁC THÀNH PHẦN (Cố định ID từ XML) ---
        val btnLight = findViewById<LinearLayout>(R.id.btn_light_mode)
        val btnDark = findViewById<LinearLayout>(R.id.btn_dark_mode)
        val radioLight = findViewById<RadioButton>(R.id.radio_light)
        val radioDark = findViewById<RadioButton>(R.id.radio_dark)
        val btnPermissions = findViewById<LinearLayout>(R.id.btn_manage_permissions)
        val btnLegal = findViewById<LinearLayout>(R.id.btn_legal_policy)
        val tvWhatsNew = findViewById<TextView>(R.id.tv_whats_new_content)
        val btnApply = findViewById<Button>(R.id.btn_apply_settings)
        val btnRecovery = findViewById<Button>(R.id.btn_system_recovery)

        // 1. Đồng bộ nội dung tóm tắt "What's New" (Layer 1)
        tvWhatsNew?.text = getSummaryFromAssets()
        tvWhatsNew?.setOnClickListener {
            showSecureDialog("UPDATE_NOTES.txt", "#00FF41") // Hiện toàn bộ nhật ký
        }

        // 2. Đồng bộ Chính sách pháp lý độc lập (Layer 2)
        btnLegal?.setOnClickListener {
            showSecureDialog("LEGAL_POLICY.txt", "#FFFFFF") // Hiện điều khoản pháp lý
        }

        // 3. Logic Theme (Kiểm tra trạng thái hệ thống)
        val currentMode = AppCompatDelegate.getDefaultNightMode()
        if (currentMode == AppCompatDelegate.MODE_NIGHT_YES) {
            radioDark?.isChecked = true
            radioLight?.isChecked = false
        } else {
            radioLight?.isChecked = true
            radioDark?.isChecked = false
        }

        btnLight?.setOnClickListener {
            radioLight?.isChecked = true
            radioDark?.isChecked = false
            updateTheme(AppCompatDelegate.MODE_NIGHT_NO)
        }

        btnDark?.setOnClickListener {
            radioDark?.isChecked = true
            radioLight?.isChecked = false
            updateTheme(AppCompatDelegate.MODE_NIGHT_YES)
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
        btnRecovery?.setOnClickListener { finish() }
    }

    private fun updateTheme(mode: Int) {
        AppCompatDelegate.setDefaultNightMode(mode)
        delegate.applyDayNight()
    }

    /**
     * showSecureDialog: Hiển thị nội dung từ assets trong một lớp Dialog bảo mật.
     * Tối ưu cho màn hình OLED với nền True Black.
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
                lines.filter { it.trim().startsWith(">") }.take(3).joinToString("\n")
            }
        } catch (e: Exception) { "> Loading VietCore Intelligence logs..." }
    }
}
