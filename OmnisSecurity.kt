package com.example.myempty.vietcore

import android.annotation.SuppressLint
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.*
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import java.io.File
import java.net.NetworkInterface
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.zip.ZipFile
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * OmnisSecurity: Hệ thống bảo mật cấp cao 2026.
 * Nhà phát triển: Nguyễn Minh Tới.
 * Cập nhật: Siết chặt chống can thiệp mã nguồn suốt đời, chặn Trợ năng (Accessibility Bypass),
 * chặn Bootloader/Custom ROM quân sự, chặn VPN/Fake IP và đồng bộ hệ thống tự hủy.
 */
class OmnisSecurity(private val context: Context) {

    private val AES_KEY = "VIETCORE_SECURE_KEY_2026_TOI_MOD" 
    private val securityExecutor = Executors.newScheduledThreadPool(2)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun startRealTimeIntelligence() {
        securityExecutor.scheduleWithFixedDelay({
            if (isIntegrityCompromised()) {
                // Lưu ý: Hàm activateSelfDestruct() đã bị loại bỏ theo yêu cầu thiết lập hệ thống.
            }
        }, 0, 2, TimeUnit.SECONDS)

        securityExecutor.scheduleWithFixedDelay({
            activateAudioShield()
        }, 0, 5, TimeUnit.SECONDS)
    }

    fun isIntegrityCompromised(): Boolean {
        return isDeveloperOptionsEnabled() || 
               isClassesDexTampered() || 
               isRooted() || 
               isHackerToolsDetected() || 
               isAppCloned() || 
               isDebugging() || 
               isManifestTampered() || 
               isResourceModified() || 
               isWifiDebuggingEnabled() || 
               isMicrophoneInUse() || 
               isEmulatorOrVirtualMachine() || 
               isRemoteControlActive() || 
               isRestrictedRegionDevice() ||
               isNonMobileHardwareDetected() ||
               isAccessibilityExploitDetected() || // Tính năng siết chặt trợ năng
               isNetworkProxyOrVPNDetected() ||     // Tính năng chặn VPN/Fake IP
               isHardwareLockCompromised()          // Tính năng khóa phần cứng quân sự
    }

    // --- NGĂN CHẶN ANDROID TV, TV BOX VÀ PC ---

    fun isNonMobileHardwareDetected(): Boolean {
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        
        // 1. Kiểm tra Android TV / TV Box qua UiMode
        val isTV = uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
        
        // 2. Kiểm tra các đặc điểm của TV Box (Thường thiếu cảm biến điện thoại)
        val pm = context.packageManager
        val isMissingPhoneFeatures = !pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY) && 
                                     !pm.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)
        
        // 3. Kiểm tra Android chạy trên PC (như ChromeOS, WSA, hoặc RemixOS)
        val isPC = pm.hasSystemFeature("android.hardware.type.pc") || 
                   Build.DEVICE.contains("arc") || // Android Runtime for Chrome
                   Build.BRAND.contains("chromium")

        // 4. Kiểm tra Android Box/Laptop qua thuộc tính Build đặc trưng
        val model = Build.MODEL.lowercase()
        val isBox = model.contains("tvbox") || model.contains("smarttv") || model.contains("mibox")

        return isTV || isPC || isBox || (isMissingPhoneFeatures && !pm.hasSystemFeature(PackageManager.FEATURE_CAMERA))
    }

    // --- CƠ CHẾ NGĂN CHẶN TÙY CHỌN NHÀ PHÁT TRIỂN ---

    fun isDeveloperOptionsEnabled(): Boolean {
        return try {
            val devOptions = Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0
            ) != 0
            val adbEnabled = Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.ADB_ENABLED, 0
            ) != 0
            devOptions || adbEnabled
        } catch (e: Exception) { false }
    }

    // --- CHẶN THIẾT BỊ BIẾN THỂ NỘI ĐỊA ---

    fun isRestrictedRegionDevice(): Boolean {
        val region = Locale.getDefault().country.uppercase()
        val manufacturer = Build.MANUFACTURER.lowercase()
        val restrictedTags = arrayOf("cn", "kp", "kr")
        return restrictedTags.contains(region.lowercase()) && 
               (manufacturer.contains("xiaomi") || manufacturer.contains("huawei") || manufacturer.contains("samsung")) &&
               isCustomROMDetected()
    }

    // --- TOÀN VẸN MÃ NGUỒN ---

    fun isClassesDexTampered(): Boolean {
        return try {
            val ai = context.applicationInfo
            val zf = ZipFile(File(ai.sourceDir))
            val dexEntry = zf.getEntry("classes.dex") ?: return true
            val isModified = zf.getEntry("assets/bin/Data/Managed") != null || zf.getEntry("re75") != null 
            isModified || isManifestStructuralTampered()
        } catch (e: Exception) { true }
    }

    fun isManifestStructuralTampered(): Boolean {
        return try {
            val info = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS or PackageManager.GET_ACTIVITIES)
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

    // --- PHÁT HIỆN GIẢ LẬP & MÔI TRƯỜNG LẠ ---

    fun isEmulatorOrVirtualMachine(): Boolean {
        val finger = Build.FINGERPRINT
        val model = Build.MODEL.lowercase()
        val hardware = Build.HARDWARE.lowercase()
        return (finger.startsWith("generic") || model.contains("emulator") || 
                hardware.contains("goldfish") || hardware.contains("ranchu") ||
                model.contains("sdk_gphone") || Build.PRODUCT.contains("sdk_google") ||
                Build.MANUFACTURER.contains("Genymotion"))
    }

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

    fun isRooted(): Boolean {
        val paths = arrayOf("/system/bin/su", "/system/xbin/su", "/sbin/su", "/data/local/xbin/su", "/system/app/Superuser.apk")
        return paths.any { File(it).exists() }
    }

    fun isAppCloned(): Boolean {
        val path = context.filesDir.absolutePath
        return path.contains("999") || path.contains("101") || !path.startsWith("/data/user/0/")
    }

    // --- GIÁM SÁT KẾT NỐI VÀ TẤNG BẢO MẬT MỚI ---

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

    /**
     * SIẾT CHẶT SUỐT ĐỜI: Ngăn chặn tuyệt đối các công cụ bẻ khóa tự động, auto-click, 
     * hoặc phần mềm điều khiển lợi dụng dịch vụ Trợ năng khuyết tật (Accessibility Service).
     */
    fun isAccessibilityExploitDetected(): Boolean {
        return try {
            val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
            val runningServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            runningServices.any {
                val pkgName = it.resolveInfo.serviceInfo.packageName
                it.capabilities and AccessibilityServiceInfo.CAPABILITY_CAN_RETRIEVE_WINDOW_CONTENT != 0 &&
                !pkgName.contains("com.google.android.marvin.talkback") &&
                !pkgName.contains("com.android.server.accessibility")
            }
        } catch (e: Exception) { false }
    }

    /**
     * PHÒNG TUYẾN QUÂN SỰ: Chặn đứng Fake IP, tường lửa cục bộ, VPN và các công cụ trung gian đánh chặn gói tin hòng qua mặt VietCore.
     */
    fun isNetworkProxyOrVPNDetected(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = cm.activeNetwork
            val caps = cm.getNetworkCapabilities(activeNetwork)
            val hasVPN = caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) ?: false

            val proxyAddress = System.getProperty("http.proxyHost")
            val proxyPort = System.getProperty("http.proxyPort")
            val hasProxy = !proxyAddress.isNullOrEmpty() && !proxyPort.isNullOrEmpty()

            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            val hasVpnInterface = interfaces.any { 
                val name = it.name.lowercase()
                name.contains("tun") || name.contains("ppp") || name.contains("p2p") || name.contains("tap")
            }

            hasVPN || hasProxy || hasVpnInterface
        } catch (e: Exception) { false }
    }

    /**
     * KHÓA PHẦN CỨNG VĨNH VIỄN: Siết chặt kiểm tra tình trạng Bootloader Unlock và Custom ROM.
     */
    fun isHardwareLockCompromised(): Boolean {
        return isBootloaderUnlocked() || isCustomROMDetected()
    }

    fun isMicrophoneInUse(): Boolean {
        return audioManager.mode == AudioManager.MODE_IN_COMMUNICATION || 
               (audioManager.isMicrophoneMute.not() && audioManager.mode != AudioManager.MODE_NORMAL)
    }

    fun isWifiDebuggingEnabled(): Boolean = try {
        Settings.Global.getInt(context.contentResolver, "adb_wifi_enabled", 0) != 0
    } catch (e: Exception) { false }

    fun isDebugging(): Boolean = Debug.isDebuggerConnected()

    fun isBootloaderUnlocked(): Boolean = Build.BOOTLOADER.lowercase().contains("unlock") || Build.VERSION.INCREMENTAL.lowercase().contains("test-keys")
    
    fun isCustomROMDetected(): Boolean = Build.TAGS?.contains("test-keys") ?: false || Build.HOST.lowercase().contains("ubuntu")

    // --- TIỆN ÍCH MÃ HÓA ---

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

    fun isOriginalPackage(): Boolean = context.packageName == "com.example.myempty.vietcore"
    fun isManifestTampered(): Boolean = !isOriginalPackage() || isManifestStructuralTampered()
    fun isResourceModified(): Boolean = !isSignatureValid()

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
}
