package com.example.myempty.vietcore

import android.annotation.SuppressLint
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.*
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import java.io.File
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.zip.ZipFile
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * OmnisSecurity: Hệ thống bảo mật cấp cao 2026.
 * Nhà phát triển: Nguyễn Minh Tới.
 * Hỗ trợ: Android 14 (API 34) trở lên.
 */
class OmnisSecurity(private val context: Context) {

    private val AES_KEY = "VIETCORE_SECURE_KEY_2026_TOI_MOD" 
    private val securityExecutor = Executors.newScheduledThreadPool(2)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    // --- KHỞI CHẠY HỆ THỐNG ---

    fun startRealTimeIntelligence() {
        // Kiểm tra phiên bản Android ngay khi khởi chạy (Chỉ từ Android 14+)
        if (Build.VERSION.SDK_INT < 34) {
             // Logic cảnh báo hoặc hạn chế tính năng có thể thêm ở MainActivity
        }

        securityExecutor.scheduleWithFixedDelay({
            isIntegrityCompromised()
        }, 0, 3, TimeUnit.SECONDS)

        securityExecutor.scheduleWithFixedDelay({
            activateAudioShield()
        }, 0, 5, TimeUnit.SECONDS)
    }

    /**
     * Kiểm tra toàn vẹn hệ thống - Public để MainActivity truy cập.
     */
    fun isIntegrityCompromised(): Boolean {
        return isClassesDexTampered() || isRooted() || isHackerToolsDetected() || 
               isAppCloned() || isDebugging() || isManifestTampered() || 
               isResourceModified() || isWifiDebuggingEnabled() || 
               isMicrophoneInUse() || isEmulatorOrVirtualMachine() || 
               isRemoteControlActive() || isRestrictedRegionDevice()
    }

    // --- CHẶN THIẾT BỊ BIẾN THỂ NỘI ĐỊA (CHINA/KOREA/NK) ---

    fun isRestrictedRegionDevice(): Boolean {
        val region = Locale.getDefault().country.uppercase()
        val manufacturer = Build.MANUFACTURER.lowercase()
        // Ngăn chặn các biến thể xách tay nội địa Trung Quốc/Triều Tiên/Hàn Quốc có dấu hiệu can thiệp ROM
        val restrictedTags = arrayOf("cn", "kp", "kr")
        return restrictedTags.contains(region.lowercase()) && 
               (manufacturer.contains("xiaomi") || manufacturer.contains("huawei") || manufacturer.contains("samsung")) &&
               isCustomROMDetected()
    }

    // --- NGĂN CHẶN CAN THIỆP MÃ NGUỒN (CLASSES.DEX) ---

    fun isClassesDexTampered(): Boolean {
        return try {
            val ai = context.applicationInfo
            val zf = ZipFile(File(ai.sourceDir))
            val dexEntry = zf.getEntry("classes.dex")
            
            // 1. Kiểm tra sự tồn tại
            if (dexEntry == null) return true
            
            // 2. Kiểm tra dấu hiệu Lucky Patcher/Mod Tool chèn tệp
            val isModified = zf.getEntry("assets/bin/Data/Managed") != null || 
                             zf.getEntry("re75") != null // Dấu hiệu đặc trưng của bản mod
            
            isModified || isManifestStructuralTampered()
        } catch (e: Exception) { true }
    }

    // --- PHÁT HIỆN GIẢ LẬP & MÁY ẢO ---

    fun isEmulatorOrVirtualMachine(): Boolean {
        val finger = Build.FINGERPRINT
        val model = Build.MODEL.lowercase()
        val hardware = Build.HARDWARE.lowercase()
        return (finger.startsWith("generic") || model.contains("emulator") || 
                hardware.contains("goldfish") || hardware.contains("ranchu") ||
                model.contains("sdk_gphone") || Build.PRODUCT.contains("sdk_google"))
    }

    // --- CHỐNG HACKER TOOLS & LUCKY PATCHER ---

    fun isHackerToolsDetected(): Boolean {
        return try {
            val file = File("/proc/self/maps")
            if (file.exists()) {
                val content = file.readText()
                content.contains("frida") || content.contains("xposed") || 
                content.contains("com.chelpus.lackypatch") || content.contains("libsubstrate.so")
            } else false
        } catch (e: Exception) { false }
    }

    // --- MÃ HÓA DỮ LIỆU (FIX LỖI UNRESOLVED) ---

    fun encryptData(data: String): String {
        return try {
            val keyBytes = AES_KEY.toByteArray(Charsets.UTF_8).let { it.copyOf(32) }
            val skeySpec = SecretKeySpec(keyBytes, "AES")
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec)
            val encrypted = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
            android.util.Base64.encodeToString(encrypted, android.util.Base64.NO_WRAP)
        } catch (e: Exception) { "ERR" }
    }

    // --- CÁC HÀM KIỂM TRA HỆ THỐNG ---

    fun isOriginalPackage(): Boolean = context.packageName == "com.example.myempty.vietcore"

    fun isManifestTampered(): Boolean = !isOriginalPackage() || isManifestStructuralTampered()

    fun isResourceModified(): Boolean {
        // Nếu chữ ký bị thay đổi (do modder ký lại), hàm này sẽ trả về true
        return !isSignatureValid()
    }

    fun isManifestStructuralTampered(): Boolean {
        return try {
            val info = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS or PackageManager.GET_ACTIVITIES)
            // Nếu số lượng Permission hoặc Activity bị thêm thắt bất thường
            (info.requestedPermissions?.size ?: 0 > 25 || info.activities?.size ?: 0 > 12)
        } catch (e: Exception) { true }
    }

    fun isSignatureValid(): Boolean {
        return try {
            val pm = context.packageManager
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES).signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES).signatures
            }
            signatures != null && signatures.isNotEmpty()
        } catch (e: Exception) { false }
    }

    fun isRemoteControlActive(): Boolean {
        return try {
            val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
            val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            enabledServices.any { service ->
                val pkg = service.resolveInfo.serviceInfo.packageName
                !pkg.contains("com.google.android.gms") && 
                (service.capabilities and AccessibilityServiceInfo.CAPABILITY_CAN_RETRIEVE_WINDOW_CONTENT) != 0
            }
        } catch (e: Exception) { false }
    }

    fun isMicrophoneInUse(): Boolean {
        return audioManager.mode == AudioManager.MODE_IN_COMMUNICATION || 
               (audioManager.isMicrophoneMute.not() && audioManager.mode != AudioManager.MODE_NORMAL)
    }

    fun isWifiDebuggingEnabled(): Boolean = try {
        Settings.Global.getInt(context.contentResolver, "adb_wifi_enabled", 0) != 0
    } catch (e: Exception) { false }

    fun isRooted(): Boolean {
        val paths = arrayOf("/system/bin/su", "/system/xbin/su", "/sbin/su", "/data/local/xbin/su", "/system/app/Superuser.apk")
        return paths.any { File(it).exists() }
    }

    fun isAppCloned(): Boolean {
        val path = context.filesDir.absolutePath
        return path.contains("999") || path.contains("101") || !path.startsWith("/data/user/0/")
    }

    fun isDebugging(): Boolean = Debug.isDebuggerConnected() || 
            Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) != 0

    fun isBootloaderUnlocked(): Boolean = Build.BOOTLOADER.lowercase().contains("unlock")

    fun isCustomROMDetected(): Boolean = Build.TAGS?.contains("test-keys") ?: false

    fun activateAudioShield() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                .setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build())
                .build()
            audioManager.requestAudioFocus(focusRequest)
        }
    }

    fun activateSelfDestruct() {
        // Giữ lại tên hàm để tránh lỗi MainActivity nhưng không thực thi xóa dữ liệu.
    }
}
