package com.example.myempty.vietcore

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
// Import R tường minh là cực kỳ quan trọng để fix lỗi Unresolved reference
import com.example.myempty.vietcore.R

/**
 * SettingsActivity: VietCore Recovery & Configuration System.
 * Developer: Nguyen Minh Toi.
 */
class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Cài đặt Toolbar để hỗ trợ nút Back và Tiêu đề
        // FIX LỖI: Đảm bảo ID khớp hoàn toàn với file XML
        val toolbar = findViewById<Toolbar>(R.id.toolbar_settings)
        if (toolbar != null) {
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = "VietCore Recovery"
            toolbar.setNavigationOnClickListener { finish() }
        }

        val btnLight = findViewById<LinearLayout>(R.id.btn_light_mode)
        val btnDark = findViewById<LinearLayout>(R.id.btn_dark_mode)
        val radioLight = findViewById<RadioButton>(R.id.radio_light)
        val radioDark = findViewById<RadioButton>(R.id.radio_dark)
        val btnApply = findViewById<Button>(R.id.btn_apply_settings)
        
        // FIX LỖI: Đảm bảo ID btn_system_recovery đã có trong activity_settings.xml
        val btnRecovery = findViewById<Button>(R.id.btn_system_recovery)

        // Kiểm tra logic theme hiện tại
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

        btnApply?.setOnClickListener { finish() }

        btnRecovery?.setOnClickListener { performSystemRecovery() }
    }

    private fun updateTheme(mode: Int) {
        AppCompatDelegate.setDefaultNightMode(mode)
        delegate.applyDayNight()
    }

    private fun performSystemRecovery() {
        // Thực hiện xóa cache bảo mật tại đây nếu cần
        finish()
    }

    companion object {
        @JvmStatic
        fun showThemeSettings(activity: AppCompatActivity) {
            val intent = android.content.Intent(activity, SettingsActivity::class.java)
            activity.startActivity(intent)
        }
    }
}
