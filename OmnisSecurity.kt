package com.example.myempty.vietcore

import android.annotation.SuppressLint
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.Signature
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
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import java.util.zip.ZipFile

/**
 * OmnisSecurity: Hệ thống bảo mật đa tầng cho VietCore 2026.
 * Tích hợp: Kiểm soát toàn vẹn DEX, Chống giả lập, Chống nghe lén.
 * Nâng cấp: Chặn điều khiển từ xa & Giám sát trợ năng.
 * Trạng thái: Cơ chế tự hủy (Self-Destruct) đã bị loại bỏ hoàn toàn.
 */
class OmnisSecurity(private val context: Context) {

    private val AES_KEY = "VIETCORE_SECURE_KEY_2026_TOI_MOD" 
    private val securityExecutor = Executors.newScheduledThreadPool(2)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    
    private val THREAT_PACKAGES = arrayOf(
        "com.chelpus.lackypatch", "org.meowcat.edxposed.manager",
        "com.topjohnwu.magisk", "com.saurik.substrate", "com.zacharee1.systemuituner"
    )

    // --- CƠ CHẾ GIÁM SÁT THỜI GIAN THỰC ---

    fun startRealTimeIntelligence() {
        // Luồng 1: Giám sát Trợ năng & Toàn vẹn hệ thống (3s/lần)
        securityExecutor.scheduleWithFixedDelay({
            isIntegrityCompromised()
            // Không gọi tự hủy ở đây để đảm bảo ứng dụng không tự đóng.
        }, 0, 3, TimeUnit.SECONDS)

        // Luồng 2: Tái lập lá chắn âm thanh chống nghe lén (5s/lần)
        securityExecutor.scheduleWithFixedDelay({
            activateAudioShield()
        }, 0, 5, TimeUnit.SECONDS)
    }

    /**
     * Kiểm tra tổng hợp các mối đe dọa. 
     * Kết quả được MainActivity sử dụng để xử lý logic hiển thị Overlay.
     */
    fun isIntegrityCompromised(): Boolean {
        return isClassesDexTampered() || isRooted() || isHackerToolsDetected() || 
               isAppCloned() || isDebugging() || isManifestTampered() || 
               isResourceModified() || isWifiDebuggingEnabled() || 
               isMicrophoneInUse() || isEmulatorOrVirtualMachine() || isRemoteControlActive()
    }

    // --- NGĂN CHẶN ĐIỀU KHIỂN TỪ XA & TRỢ NĂNG (NEW) ---

    fun isRemoteControlActive(): Boolean {
        return try {
            val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
            val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            
            enabledServices.any { service ->
                val pkg = service.resolveInfo.serviceInfo.packageName
                // Bỏ qua các thành phần tin cậy của hệ thống
                !pkg.contains("com.google.android.gms") && 
                !pkg.contains("com.android.settings") &&
                (service.capabilities and AccessibilityServiceInfo.CAPABILITY_CAN_RETRIEVE_WINDOW_CONTENT) != 0
            }
        } catch (e: Exception) { false }
    }

    // --- PHÁT HIỆN GIẢ LẬP (STABLE) ---

    fun isEmulatorOrVirtualMachine(): Boolean {
        val finger = Build.FINGERPRINT
        val board = Build.BOARD.lowercase(Locale.US)
        val model = Build.MODEL.lowercase(Locale.US)
        val brand = Build.BRAND.lowercase(Locale.US)
        val device = Build.DEVICE.lowercase(Locale.US)
        val hardware = Build.HARDWARE.lowercase(Locale.US)

        return (finger.startsWith("generic") || 
                finger.startsWith("unknown") || 
                model.contains("google_sdk") || 
                model.contains("emulator") || 
                model.contains("android sdk built for x86") ||
                (brand.startsWith("generic") && device.startsWith("generic")) ||
                hardware.contains("goldfish") || 
                hardware.contains("ranchu") ||
                board.contains("nox") || 
                hardware.contains("bluestacks") ||
                model.contains("sdk_gphone"))
    }

    // --- TOÀN VẸN MÃ NGUỒN (DEX MONITOR) ---

    fun isClassesDexTampered(): Boolean {
        return try {
            val ai = context.applicationInfo
            val zf = ZipFile(File(ai.sourceDir))
            val dexEntry = zf.getEntry("classes.dex")
            dexEntry == null || isManifestStructuralTampered()
        } catch (e: Exception) { true }
    }

    // --- LÁ CHẮN ÂM THANH (ANTI-EAVESDROPPING) ---

    fun activateAudioShield() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                .setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build())
                .setAcceptsDelayedFocusGain(false)
                .build()
            audioManager.requestAudioFocus(focusRequest)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(null, AudioManager.STREAM_SYSTEM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
        }
    }

    fun isMicrophoneInUse(): Boolean {
        return audioManager.mode == AudioManager.MODE_IN_COMMUNICATION || 
               (audioManager.isMicrophoneMute.not() && audioManager.mode != AudioManager.MODE_NORMAL)
    }

    // --- CÁC TÍNH NĂNG BẢO MẬT CỐ ĐỊNH ---

    fun isRooted(): Boolean {
        val paths = arrayOf("/system/bin/su", "/system/xbin/su", "/sbin/su", "/data/local/xbin/su")
        return paths.any { File(it).exists() }
    }

    fun isHackerToolsDetected(): Boolean {
        return try {
            val file = File("/proc/self/maps")
            if (file.exists()) {
                file.bufferedReader().useLines { lines ->
                    lines.any { it.contains("frida") || it.contains("xposed") || it.contains("substrate") }
                }
            } else false
        } catch (e: Exception) { false }
    }

    fun isAppCloned(): Boolean {
        val path = context.filesDir.absolutePath
        return path.contains("999") || path.contains("101") || !path.startsWith("/data/user/0/")
    }

    fun isOriginalPackage(): Boolean = context.packageName == "com.example.myempty.vietcore"

    /**
     * Cơ chế tự hủy đã bị loại bỏ theo yêu cầu để tránh việc mất dữ liệu người dùng.
     */
    fun activateSelfDestruct() {
        // No-op: Hàm này để trống để giữ ổn định code cho MainActivity.kt
    }

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

    fun isDebugging(): Boolean = Debug.isDebuggerConnected() || 
            Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) != 0

    fun isWifiDebuggingEnabled(): Boolean = try {
        Settings.Global.getInt(context.contentResolver, "adb_wifi_enabled", 0) != 0
    } catch (e: Exception) { false }

    fun isManifestStructuralTampered(): Boolean {
        return try {
            val info = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS or PackageManager.GET_ACTIVITIES)
            (info.requestedPermissions?.size ?: 0 > 15 || info.activities?.size ?: 0 > 8)
        } catch (e: Exception) { true }
    }

    fun isManifestTampered(): Boolean = !isOriginalPackage()
    fun isResourceModified(): Boolean = !isSignatureValid()
    fun isBootloaderUnlocked(): Boolean = Build.BOOTLOADER.lowercase().contains("unlock")
    fun isCustomROMDetected(): Boolean = Build.TAGS?.contains("test-keys") ?: false
}
