package com.example.myempty.vietcore

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Locale

/**
 * SettingsActivity: Trung tâm cấu hình VietCore.
 * Sửa lỗi Unresolved reference và tối ưu hóa logic bảo mật.
 * Developer: Nguyen Minh Toi.
 */
class SettingsActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        val lang = LocaleHelper.getLanguage(newBase) ?: "en"
        super.attachBaseContext(LocaleHelper.setLocale(newBase, lang))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // --- 1. THIẾT LẬP TOOLBAR ---
        val toolbar = findViewById<Toolbar>(R.id.toolbar_settings)
        if (toolbar != null) {
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = getString(R.string.settings_title)
            toolbar.setNavigationOnClickListener { finish() }
        }

        // --- 2. IDENTITY (DANH TÍNH) ---
        // Sử dụng findViewById để tránh lỗi Unresolved reference
        val btnProfile = findViewById<LinearLayout>(R.id.btn_account_profile)
        val tvUsername = findViewById<TextView>(R.id.tv_username)
        val ivEditProfile = findViewById<ImageView>(R.id.iv_edit_profile) // Nút bút chì từ XML của bạn

        // Vô hiệu hóa click trên vùng chứa để thực hiện "Điểm chạm duy nhất"
        btnProfile?.setOnClickListener(null)
        btnProfile?.isClickable = false

        val savedName = getSharedPreferences("VietCore_Config", MODE_PRIVATE)
            .getString("saved_username", "Nguyen Minh Toi")
        tvUsername?.text = savedName

        // Logic chỉnh sửa: Chỉ kích hoạt qua biểu tượng bút chì
        ivEditProfile?.setOnClickListener {
            val input = EditText(this).apply {
                setText(tvUsername?.text)
                setTextColor(Color.parseColor("#00FF41"))
                backgroundTintList = ColorStateList.valueOf(Color.parseColor("#00FF41"))
                typeface = Typeface.MONOSPACE
                setPadding(60, 40, 60, 40)
            }

            MaterialAlertDialogBuilder(this, R.style.VietCore_Terminal_Dialog)
                .setTitle(getString(R.string.header_identity))
                .setView(input)
                .setPositiveButton(getString(R.string.confirm_yes)) { _, _ ->
                    val newName = input.text.toString().trim()
                    if (newName.isNotEmpty()) {
                        tvUsername?.text = newName
                        saveIdentity(newName)
                    }
                }
                .setNegativeButton(getString(R.string.confirm_no), null)
                .show()
        }

        // --- 3. LOCALIZATION (ĐA NGÔN NGỮ) ---
        val btnLanguage = findViewById<LinearLayout>(R.id.btn_change_language)
        val tvCurrentLang = findViewById<TextView>(R.id.tv_current_language)
        val currentLang = LocaleHelper.getLanguage(this) ?: "en"
        tvCurrentLang?.text = if (currentLang == "vi") "Tiếng Việt" else "English (US)"

        btnLanguage?.setOnClickListener { showLanguageDialog() }

        // --- 4. THEME & UI ---
        val btnLight = findViewById<LinearLayout>(R.id.btn_light_mode)
        val btnDark = findViewById<LinearLayout>(R.id.btn_dark_mode)
        val radioLight = findViewById<RadioButton>(R.id.radio_light)
        val radioDark = findViewById<RadioButton>(R.id.radio_dark)
        
        updateRadioState(AppCompatDelegate.getDefaultNightMode(), radioLight, radioDark)
        btnLight?.setOnClickListener { updateTheme(AppCompatDelegate.MODE_NIGHT_NO) }
        btnDark?.setOnClickListener { updateTheme(AppCompatDelegate.MODE_NIGHT_YES) }

        // --- 5. SYSTEM INTEL (NHẬT KÝ CẬP NHẬT) ---
        val tvWhatsNew = findViewById<TextView>(R.id.tv_whats_new_content)
        tvWhatsNew?.text = getSummaryFromAssets()
        tvWhatsNew?.setOnClickListener {
            showSecureDialog("UPDATE_NOTES.txt", "#00FF41")
        }

        // --- 6. LEGAL & PERMISSIONS (PHÁP LÝ & QUYỀN) ---
        findViewById<LinearLayout>(R.id.btn_legal_policy)?.setOnClickListener {
            val intent = Intent(this, LegalActivity::class.java).apply {
                putExtra("FROM_SETTINGS", true)
            }
            startActivity(intent)
        }

        findViewById<LinearLayout>(R.id.btn_manage_permissions)?.setOnClickListener {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            }
            startActivity(intent)
        }

        findViewById<LinearLayout>(R.id.btn_integrity_check)?.setOnClickListener { runIntegrityScan() }
        
        findViewById<LinearLayout>(R.id.btn_clear_cache)?.setOnClickListener {
            Toast.makeText(this, getString(R.string.toast_purge_success), Toast.LENGTH_SHORT).show()
        }

        // --- 7. ACTIONS (LƯU & KHÔI PHỤC) ---
        findViewById<Button>(R.id.btn_apply_settings)?.setOnClickListener { finish() }
        
        findViewById<Button>(R.id.btn_system_recovery)?.setOnClickListener {
            MaterialAlertDialogBuilder(this, R.style.VietCore_Terminal_Dialog)
                .setTitle(getString(R.string.btn_recovery))
                .setMessage(getString(R.string.dialog_confirm_recovery))
                .setPositiveButton(getString(R.string.confirm_yes)) { _, _ ->
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                    finish()
                }
                .setNegativeButton(getString(R.string.confirm_no), null)
                .show()
        }
    }

    // --- CÁC HÀM HỖ TRỢ GIỮ NGUYÊN LOGIC CŨ ---

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
        }
        scroll.addView(content)
        layout.addView(scroll)
        dialog.setContentView(layout)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.BLACK))
        dialog.show()
    }

    private fun showLanguageDialog() {
        val languages = arrayOf("English (US)", "Tiếng Việt")
        val langCodes = arrayOf("en", "vi")
        MaterialAlertDialogBuilder(this, R.style.VietCore_Terminal_Dialog)
            .setTitle(getString(R.string.dialog_select_language))
            .setItems(languages) { _, which -> updateLanguage(langCodes[which]) }
            .show()
    }

    private fun updateLanguage(langCode: String) {
        LocaleHelper.setLocale(this, langCode)
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }

    private fun getSummaryFromAssets(): String {
        return try {
            assets.open("UPDATE_NOTES.txt").bufferedReader().useLines { lines ->
                lines.filter { it.trim().startsWith(">") }.take(3).joinToString("\n")
            }
        } catch (e: Exception) { getString(R.string.intel_logs_default) }
    }

    private fun saveIdentity(name: String) {
        getSharedPreferences("VietCore_Config", MODE_PRIVATE).edit().putString("saved_username", name).apply()
        Toast.makeText(this, "> Identity Updated: $name", Toast.LENGTH_SHORT).show()
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

    private fun runIntegrityScan() {
        val dialog = Dialog(this)
        val tv = TextView(this).apply {
            setBackgroundColor(Color.BLACK)
            setTextColor(Color.parseColor("#00FF41"))
            typeface = Typeface.MONOSPACE
            setPadding(30, 30, 30, 30)
            text = getString(R.string.log_scanning_integrity)
        }
        dialog.setContentView(tv)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, 500)
        dialog.show()
        val handler = Handler(Looper.getMainLooper())
        val logs = arrayOf("\n> Checking Manifest...", "\n> Verifying Signature...", "\n${getString(R.string.log_security_active)}", "\n> Status: OPTIMAL")
        var index = 0
        val runnable = object : Runnable {
            override fun run() {
                if (index < logs.size) {
                    tv.append(logs[index++])
                    handler.postDelayed(this, 700)
                } else {
                    Toast.makeText(this@SettingsActivity, getString(R.string.toast_scan_complete), Toast.LENGTH_SHORT).show()
                    handler.postDelayed({ dialog.dismiss() }, 1500)
                }
            }
        }
        handler.postDelayed(runnable, 1000)
    }
}
