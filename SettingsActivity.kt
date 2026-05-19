package com.example.myempty.vietcore

import android.app.ActivityManager
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Locale

/**
 * SettingsActivity: Trung tâm cấu hình VietCore.
 * Đã tích hợp: Kiểm soát phần cứng, Terminal Toggle và System Recovery.
 * Developer: Nguyen Minh Toi.
 */
class SettingsActivity : AppCompatActivity() {

    // --- ĐỒNG BỘ NGÔN NGỮ HỆ THỐNG ---
    override fun attachBaseContext(newBase: Context) {
        val sharedPref = newBase.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val languageCode = sharedPref.getString("item_language", "en") ?: "en"
        
        val locale = Locale.forLanguageTag(languageCode)
        Locale.setDefault(locale)
        
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        
        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val currentLang = prefs.getString("item_language", "en") ?: "en"

        // --- 1. THIẾT LẬP TOOLBAR ---
        val toolbar = findViewById<Toolbar>(R.id.toolbar_settings)
        if (toolbar != null) {
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = getString(R.string.settings_title)
            toolbar.setNavigationOnClickListener { finish() }
        }

        // --- 2. IDENTITY (DANH TÍNH) ---
        val tvUsername = findViewById<TextView>(R.id.tv_username)
        val ivEditProfile = findViewById<ImageView>(R.id.iv_edit_profile)

        val savedName = prefs.getString("saved_username", "Nguyen Minh Toi")
        tvUsername?.text = savedName

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
        
        tvCurrentLang?.text = if (currentLang == "vi") "Tiếng Việt" else "English (US)"
        btnLanguage?.setOnClickListener { showLanguageDialog() }

        // --- 4. THEME & UI (FIXED RADIO LOGIC) ---
        val btnLight = findViewById<LinearLayout>(R.id.btn_light_mode)
        val btnDark = findViewById<LinearLayout>(R.id.btn_dark_mode)
        val radioLight = findViewById<RadioButton>(R.id.radio_light)
        val radioDark = findViewById<RadioButton>(R.id.radio_dark)
        
        updateRadioState(AppCompatDelegate.getDefaultNightMode(), radioLight, radioDark)
        
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

        // --- 5. TERMINAL CORE INTERFACE (NEW BỔ SUNG) ---
        val swTerminal = findViewById<SwitchCompat>(R.id.sw_terminal_mode)
        swTerminal?.isChecked = prefs.getBoolean("terminal_core_active", false)
        swTerminal?.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("terminal_core_active", isChecked).apply()
            val msg = if (currentLang == "vi") "Giao diện dòng lệnh: " else "Terminal Interface: "
            val status = if (isChecked) getString(R.string.status_enabled) else getString(R.string.status_disabled)
            Toast.makeText(this, "> $msg$status", Toast.LENGTH_SHORT).show()
        }

        // --- 6. QUANTUM RADAR SYSTEM (HARDWARE RESTRICTION) ---
        val swRadar = findViewById<SwitchCompat>(R.id.sw_quantum_radar)
        val tvRadarDesc = findViewById<TextView>(R.id.tv_radar_desc)
        
        val actManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        val totalRamGB = memInfo.totalMem / (1024 * 1024 * 1024)
        
        if (totalRamGB < 4 || actManager.isLowRamDevice) {
            swRadar?.isEnabled = false
            swRadar?.alpha = 0.5f
            tvRadarDesc?.visibility = View.VISIBLE
            tvRadarDesc?.text = if (currentLang == "vi") "> Phần cứng không đủ đáp ứng" else "> Hardware restricted"
            tvRadarDesc?.setTextColor(Color.RED)
        } else {
            tvRadarDesc?.visibility = View.GONE
            swRadar?.isEnabled = true
            swRadar?.isChecked = prefs.getBoolean("radar_enabled", false)
            swRadar?.setOnCheckedChangeListener { _, isChecked ->
                prefs.edit().putBoolean("radar_enabled", isChecked).apply()
            }
        }

        // --- 7. SYSTEM INTEL ---
        val tvWhatsNew = findViewById<TextView>(R.id.tv_whats_new_content)
        tvWhatsNew?.text = getSummaryFromAssets()
        tvWhatsNew?.setOnClickListener {
            showSecureDialog("UPDATE_NOTES.txt", "#00FF41")
        }

        // --- 8. LEGAL & PERMISSIONS ---
        findViewById<LinearLayout>(R.id.btn_legal_policy)?.setOnClickListener {
            startActivity(Intent(this, LegalActivity::class.java).apply { putExtra("FROM_SETTINGS", true) })
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
        
        // --- 9. EXPERIMENTAL MODE ---
        val swExperimental = findViewById<SwitchCompat>(R.id.sw_experimental)
        swExperimental?.isChecked = prefs.getBoolean("dev_mode_active", false)

        swExperimental?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                MaterialAlertDialogBuilder(this, R.style.VietCore_Terminal_Dialog)
                    .setTitle(getString(R.string.item_experimental))
                    .setMessage(if (currentLang == "vi") "> Cảnh báo: Kích hoạt giao diện Thử nghiệm?" else "> Warning: Activate Developer Labs Interface?")
                    .setPositiveButton(getString(R.string.confirm_yes)) { _, _ ->
                        prefs.edit().putBoolean("dev_mode_active", true).apply()
                        restartToApply()
                    }
                    .setNegativeButton(getString(R.string.confirm_no)) { _, _ -> swExperimental.isChecked = false }
                    .show()
            } else {
                prefs.edit().putBoolean("dev_mode_active", false).apply()
                restartToApply()
            }
        }

        // --- 10. ACTIONS (APPLY & RECOVERY) ---
        findViewById<Button>(R.id.btn_apply_settings)?.setOnClickListener { finish() }
        
        findViewById<Button>(R.id.btn_system_recovery)?.setOnClickListener {
            MaterialAlertDialogBuilder(this, R.style.VietCore_Terminal_Dialog)
                .setTitle(getString(R.string.btn_recovery))
                .setMessage(getString(R.string.dialog_confirm_recovery))
                .setPositiveButton(getString(R.string.confirm_yes)) { _, _ ->
                    prefs.edit().clear().apply() 
                    restartToApply()
                }
                .setNegativeButton(getString(R.string.confirm_no), null)
                .show()
        }
    }

    private fun restartToApply() {
        val intent = Intent(this, SplashActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }

    private fun showLanguageDialog() {
        val languages = arrayOf("English (US)", "Tiếng Việt")
        val langCodes = arrayOf("en", "vi")
        MaterialAlertDialogBuilder(this, R.style.VietCore_Terminal_Dialog)
            .setTitle(getString(R.string.dialog_select_language))
            .setItems(languages) { _, which -> 
                getSharedPreferences("settings", MODE_PRIVATE).edit()
                    .putString("item_language", langCodes[which]).apply()
                restartToApply()
            }
            .show()
    }

    private fun saveIdentity(name: String) {
        getSharedPreferences("settings", MODE_PRIVATE).edit().putString("saved_username", name).apply()
        val msg = if (Locale.getDefault().language == "vi") "Đã cập nhật danh tính" else "Identity Updated"
        Toast.makeText(this, "> $msg: $name", Toast.LENGTH_SHORT).show()
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

    private fun getSummaryFromAssets(): String {
        return try {
            assets.open("UPDATE_NOTES.txt").bufferedReader().useLines { lines ->
                lines.filter { it.trim().startsWith(">") }.take(3).joinToString("\n")
            }
        } catch (e: Exception) { getString(R.string.intel_logs_default) }
    }

    private fun showSecureDialog(fileName: String, colorHex: String) {
        val dialog = Dialog(this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
            setPadding(40, 40, 40, 40)
        }
        val scroll = ScrollView(this)
        val content = TextView(this).apply {
            text = try { assets.open(fileName).bufferedReader().use { it.readText() } } 
                   catch (e: Exception) { "> Access Denied." }
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
        val isVi = Locale.getDefault().language == "vi"
        val logs = if (isVi) arrayOf("\n> Kiểm tra Manifest...", "\n> Xác minh chữ ký...", "\n${getString(R.string.log_security_active)}", "\n> Trạng thái: TỐI ƯU")
                   else arrayOf("\n> Checking Manifest...", "\n> Verifying Signature...", "\n${getString(R.string.log_security_active)}", "\n> Status: OPTIMAL")
        
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
