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
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * SettingsActivity: VietCore Configuration.
 * Đã fix lỗi Unresolved reference 'fontFamily' và tối ưu độ ổn định.
 */
class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Toolbar Setup
        val toolbar = findViewById<Toolbar>(R.id.toolbar_settings)
        if (toolbar != null) {
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = "VietCore Config"
            toolbar.setNavigationOnClickListener { finish() }
        }

        // Ánh xạ các thành phần (Giữ nguyên toàn bộ ID mẫu)
        val btnLight = findViewById<LinearLayout>(R.id.btn_light_mode)
        val btnDark = findViewById<LinearLayout>(R.id.btn_dark_mode)
        val radioLight = findViewById<RadioButton>(R.id.radio_light)
        val radioDark = findViewById<RadioButton>(R.id.radio_dark)
        val btnPermissions = findViewById<LinearLayout>(R.id.btn_manage_permissions)
        val tvWhatsNew = findViewById<TextView>(R.id.tv_whats_new_content)
        val btnApply = findViewById<Button>(R.id.btn_apply_settings)
        val btnRecovery = findViewById<Button>(R.id.btn_system_recovery)

        // 1. Đồng bộ nội dung tóm tắt (Chỉ lấy các dòng có dấu >)
        tvWhatsNew.text = getSummaryFromAssets()

        // 2. Chức năng nhấn vào để xem toàn bộ ghi chú cập nhật
        tvWhatsNew.setOnClickListener {
            showFullUpdateLog()
        }

        // 3. Logic Theme (Kiểm tra và áp dụng)
        val currentMode = AppCompatDelegate.getDefaultNightMode()
        if (currentMode == AppCompatDelegate.MODE_NIGHT_YES) {
            radioDark.isChecked = true
            radioLight.isChecked = false
        } else {
            radioLight.isChecked = true
            radioDark.isChecked = false
        }

        btnLight?.setOnClickListener {
            radioLight.isChecked = true
            radioDark.isChecked = false
            updateTheme(AppCompatDelegate.MODE_NIGHT_NO)
        }

        btnDark?.setOnClickListener {
            radioDark.isChecked = true
            radioLight.isChecked = false
            updateTheme(AppCompatDelegate.MODE_NIGHT_YES)
        }

        // 4. Đồng bộ quyền hệ thống với AndroidManifest
        btnPermissions?.setOnClickListener {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
            startActivity(intent)
        }

        // 5. Nút Apply và Recovery (Giữ nguyên logic đóng Activity)
        btnApply?.setOnClickListener { finish() }
        btnRecovery?.setOnClickListener { finish() }
    }

    private fun updateTheme(mode: Int) {
        AppCompatDelegate.setDefaultNightMode(mode)
        delegate.applyDayNight()
    }

    // Hiển thị lớp ghi chú chi tiết (Phủ lên trên không ảnh hưởng cài đặt)
    private fun showFullUpdateLog() {
        val dialog = Dialog(this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
            setPadding(40, 40, 40, 40)
        }

        val scroll = ScrollView(this)
        val content = TextView(this).apply {
            text = readAllNotesFromAssets()
            setTextColor(Color.parseColor("#00FF41"))
            // FIX LỖI: Sử dụng setTypeface thay vì fontFamily trực tiếp
            typeface = Typeface.MONOSPACE
            textSize = 12f
        }

        scroll.addView(content)
        layout.addView(scroll)

        dialog.setContentView(layout)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.BLACK))
        dialog.show()
    }

    private fun getSummaryFromAssets(): String {
        return try {
            assets.open("UPDATE_NOTES.txt").bufferedReader().useLines { lines ->
                lines.filter { it.trim().startsWith(">") }.take(3).joinToString("\n")
            }
        } catch (e: Exception) { "> Loading system logs..." }
    }

    private fun readAllNotesFromAssets(): String {
        return try {
            assets.open("UPDATE_NOTES.txt").bufferedReader().use { it.readText() }
        } catch (e: Exception) { "> Kernel data secured." }
    }
}
