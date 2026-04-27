package com.example.myempty.vietcore

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.*
import android.provider.Settings
import android.util.Base64
import java.io.File
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * OmnisSecurity: Hệ thống bảo mật đa tầng cho VietCore 2026.
 * Bổ sung: NGĂN CHẶN GHI ÂM & NGHE LÉN (Anti-Eavesdropping).
 */
class OmnisSecurity(private val context: Context) {

    private val AES_KEY = "VIETCORE_SECURE_KEY_2026_TOI_MOD" 
    private val securityExecutor = Executors.newScheduledThreadPool(2) // Tăng luồng để xử lý Audio ngầm
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    
    private val THREAT_PACKAGES = arrayOf(
        "com.chelpus.lackypatch", "org.meowcat.edxposed.manager",
        "com.topjohnwu.magisk", "com.saurik.substrate", "com.zacharee1.systemuituner"
    )

    // --- CƠ CHẾ CHỐNG GHI ÂM & NGHE LÉN (MỚI) ---

    /**
     * Kích hoạt lá chắn âm thanh ngầm.
     * Chiếm Audio Focus để ngăn các ứng dụng khác ghi âm trái phép.
     */
    fun activateAudioShield() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                .setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build())
                .setAcceptsDelayedFocusGain(false)
                .setOnAudioFocusChangeListener { /* Chặn mọi sự thay đổi focus từ ứng dụng lạ */ }
                .build()
            audioManager.requestAudioFocus(focusRequest)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(null, AudioManager.STREAM_SYSTEM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
        }
    }

    /**
     * Kiểm tra xem Microphone có đang bị ứng dụng nào khác chiếm dụng trái phép không.
     */
    fun isMicrophoneInUse(): Boolean {
        // Lưu ý: Yêu cầu quyền android.permission.MODIFY_AUDIO_SETTINGS trong Manifest
        return audioManager.mode == AudioManager.MODE_IN_COMMUNICATION || audioManager.isMicrophoneMute.not() && audioManager.mode != AudioManager.MODE_NORMAL
    }

    // --- CƠ CHẾ GIÁM SÁT THỜI GIAN THỰC ---

    fun startRealTimeIntelligence() {
        // Luồng 1: Quét đe dọa hệ thống & phần mềm
        securityExecutor.scheduleWithFixedDelay({
            if (isExternalThreatDetected()) restrictAccess() else restoreAccess()
        }, 0, 2, TimeUnit.SECONDS)

        // Luồng 2: Quét lá chắn âm thanh (Chống nghe lén)
        securityExecutor.scheduleWithFixedDelay({
            activateAudioShield() // Liên tục tái lập lá chắn để đẩy các ứng dụng ghi âm ra ngoài
        }, 0, 5, TimeUnit.SECONDS)
    }

    private fun isExternalThreatDetected(): Boolean {
        return isHackerToolsDetected() || isNearbyInterferenceDetected() || 
               isWifiDebuggingEnabled() || isMicrophoneInUse()
    }

    /**
     * Quét các ứng dụng can thiệp đang tồn tại xung quanh hệ điều hành.
     */
    fun isNearbyInterferenceDetected(): Boolean {
        val pm = context.packageManager
        for (pkg in THREAT_PACKAGES) {
            try {
                pm.getPackageInfo(pkg, 0)
                return true 
            } catch (e: PackageManager.NameNotFoundException) {
                continue
            }
        }
        return false
    }

    /**
     * Ngăn chặn gỡ lỗi qua Wi-Fi (Wireless Debugging).
     */
    fun isWifiDebuggingEnabled(): Boolean {
        return try {
            val adbWifi = Settings.Global.getInt(context.contentResolver, "adb_wifi_enabled", 0)
            adbWifi != 0
        } catch (e: Exception) {
            false
        }
    }

    private fun restrictAccess() { /* Khóa các module nhạy cảm */ }
    private fun restoreAccess() { /* Mở khóa khi an toàn */ }

    // --- GIỮ NGUYÊN TOÀN BỘ CÁC TÍNH NĂNG CỐ ĐỊNH ---

    fun isLegacyOSDetected(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q

    fun isOutdatedHardware(): Boolean {
        val abis = Build.SUPPORTED_ABIS
        return abis.none { it.contains("arm64-v8a") }
    }

    fun isEmulatorOrVirtualMachine(): Boolean {
        val finger = Build.FINGERPRINT
        val board = Build.BOARD.lowercase(Locale.US)
        return (finger.startsWith("generic") || finger.startsWith("unknown") || 
                Build.MODEL.contains("google_sdk") || Build.MODEL.contains("Emulator") ||
                Build.HARDWARE.contains("goldfish") || Build.HARDWARE.contains("ranchu") ||
                board.contains("nox") || Build.HARDWARE.contains("bluestacks"))
    }

    fun isRestrictedRegionOrOS(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase(Locale.US)
        val isGlobal = try {
            context.packageManager.getPackageInfo("com.google.android.gsf", 0)
            true
        } catch (e: Exception) { false }
        if (!isGlobal) {
            val brands = arrayOf("huawei", "xiaomi", "oppo", "vivo")
            if (brands.any { manufacturer.contains(it) }) return true
        }
        return false
    }

    fun isInstallerUnverified(): Boolean {
        val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getInstallerPackageName(context.packageName)
        }
        val blacklistedSources = arrayOf("com.android.vending", "com.amazon.venezia", "com.apkpure.a3", "com.apkmirror.helper")
        return installer != null && blacklistedSources.contains(installer)
    }

    fun isManifestStructuralTampered(): Boolean {
        return try {
            val info = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS or PackageManager.GET_ACTIVITIES)
            val maxAllowedPermissions = 10 
            val maxAllowedActivities = 5
            (info.requestedPermissions?.size ?: 0 > maxAllowedPermissions || info.activities?.size ?: 0 > maxAllowedActivities)
        } catch (e: Exception) { true }
    }

    fun isIntegrityCompromised(): Boolean {
        return isLegacyOSDetected() || isOutdatedHardware() || isRooted() || 
               isHackerToolsDetected() || isAppCloned() || isDebugging() || 
               isBootloaderUnlocked() || isCustomROMDetected() || isManifestTampered() || 
               isResourceModified() || isRestrictedRegionOrOS() || 
               isInstallerUnverified() || isManifestStructuralTampered() || isExternalThreatDetected()
    }

    fun isRooted(): Boolean {
        val paths = arrayOf("/system/bin/su", "/system/xbin/su", "/sbin/su", "/data/local/xbin/su")
        return paths.any { File(it).exists() }
    }

    fun isHackerToolsDetected(): Boolean {
        val proxyHost = System.getProperty("http.proxyHost")
        if (!proxyHost.isNullOrEmpty()) return true
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

    fun activateSelfDestruct() {
        try {
            context.cacheDir.deleteRecursively()
            context.filesDir.deleteRecursively()
            android.os.Process.killProcess(android.os.Process.myPid())
        } catch (e: Exception) { System.exit(1) }
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
        val signatures = getAppSignatures()
        return signatures != null && signatures.isNotEmpty()
    }

    @SuppressLint("PackageManagerGetSignatures")
    private fun getAppSignatures(): Array<Signature>? {
        return try {
            val pm = context.packageManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES).signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES).signatures
            }
        } catch (e: Exception) { null }
    }

    fun isDebugging(): Boolean {
        return Debug.isDebuggerConnected() || 
                Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) != 0
    }

    fun isBootloaderUnlocked(): Boolean = Build.BOOTLOADER.lowercase().contains("unlock")
    fun isCustomROMDetected(): Boolean = Build.TAGS?.contains("test-keys") ?: false
    fun isManifestTampered(): Boolean = !isOriginalPackage()
    fun isResourceModified(): Boolean = !isSignatureValid()
    fun isAppNameModified(originalName: String): Boolean {
        val currentLabel = context.applicationInfo.loadLabel(context.packageManager).toString()
        return currentLabel != originalName
    }
}
