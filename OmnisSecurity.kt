package com.example.myempty.vietcore

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.*
import android.provider.Settings
import android.util.Base64
import android.view.accessibility.AccessibilityManager
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.*
import java.util.concurrent.Executors
import java.util.zip.ZipFile
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * OmnisSecurity: Hệ thống bảo mật hợp nhất VietCore 2026.
 * Đã tích hợp: Chống Lucky Patcher, Legacy OS Deprecation, Dex/Signature Integrity, 
 * và Hardware Sensor Validation.
 * Developer: Nguyen Minh Toi
 */
class OmnisSecurity(private val context: Context) {

    private val AES_KEY = "VIETCORE_SECURE_KEY_2026_TOI_MOD" 
    private val VALID_PROVIDER_DOMAIN = "vietcore.intelligence.gov"
    private val ORIG_PKG = "com.example.myempty.vietcore"
    private val ORIG_MIN_SDK = 29
    private val ORIG_TARGET_SDK = 34
    
    private val securityExecutor = Executors.newSingleThreadExecutor()

    // --- 0. NGĂN CHẶN HỆ ĐIỀU HÀNH & PHẦN CỨNG LỖI THỜI ---
    fun isLegacyOSDetected(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q

    fun isOutdatedHardware(): Boolean {
        val abis = Build.SUPPORTED_ABIS
        return abis.none { it.contains("arm64-v8a") }
    }

    // --- 1. NGĂN CHẶN CAN THIỆP CẤU TRÚC (SDK, DEX, SIGNATURE) ---
    fun isSdkModified(): Boolean {
        val appInfo = context.applicationInfo
        return appInfo.minSdkVersion != ORIG_MIN_SDK || appInfo.targetSdkVersion != ORIG_TARGET_SDK
    }

    fun isDexPatched(): Boolean {
        return try {
            val zipFile = ZipFile(File(context.packageCodePath))
            val isPatched = zipFile.getEntry("META-INF/PATCHED.SF") != null || 
                             zipFile.getEntry("META-INF/PATCHED.RSA") != null ||
                             zipFile.getEntry("assets/bin/Data/Managed/Metadata") != null
            zipFile.close()
            isPatched
        } catch (e: Exception) { false }
    }

    @SuppressLint("PackageManagerGetSignatures")
    fun isSignatureSpoofed(): Boolean {
        return try {
            val pm = context.packageManager
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pm.getPackageInfo(ORIG_PKG, PackageManager.GET_SIGNING_CERTIFICATES).signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(ORIG_PKG, PackageManager.GET_SIGNATURES).signatures
            }
            signatures == null || signatures.isEmpty()
        } catch (e: Exception) { true }
    }

    // --- 2. NGĂN CHẶN CÔNG CỤ CAN THIỆP (LUCKY PATCHER, TOOLS) ---
    fun isLuckyPatcherDetected(): Boolean {
        val checkers = arrayOf(
            "com.chelpus.lackypatch", "com.dimonvideo.luckypatcher",
            "com.android.vending.billing.InAppBillingService.LUCK",
            "org.chelpus.store"
        )
        return checkers.any { pkg ->
            try { context.packageManager.getPackageInfo(pkg, 0); true } catch (e: Exception) { false }
        }
    }

    fun isInAppBillingEmulated(): Boolean {
        val proxyHost = System.getProperty("http.proxyHost")
        val proxyPort = System.getProperty("http.proxyPort")
        return !proxyHost.isNullOrEmpty() || proxyPort == "8080"
    }

    // --- 3. GIẢ LẬP & PHẦN CỨNG ---
    fun isEmulatorOrVirtualMachine(): Boolean {
        val finger = Build.FINGERPRINT
        val basicCheck = (finger.startsWith("generic") || finger.startsWith("unknown") || 
                Build.MODEL.contains("google_sdk") || Build.HARDWARE.contains("goldfish") ||
                Build.BOARD.lowercase(Locale.US).contains("nox") || 
                Build.HARDWARE.lowercase(Locale.US).contains("bluestacks"))
        
        // Kiểm tra cảm biến vật lý (Máy ảo thường thiếu Accelerometer/Gyro)
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val isHardwareReal = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null
        
        return basicCheck || !isHardwareReal
    }

    // --- 4. TỔNG HỢP TOÀN VẸN HỆ THỐNG ---
    fun isIntegrityCompromised(): Boolean {
        return isLegacyOSDetected() || isOutdatedHardware() || isRooted() || 
               isDexPatched() || isSdkModified() || isLuckyPatcherDetected() ||
               isInAppBillingEmulated() || isSignatureSpoofed() || 
               isAppCloned() || isDebugging() || !isOriginalPackage()
    }

    fun isRooted(): Boolean {
        val paths = arrayOf("/system/bin/su", "/system/xbin/su", "/sbin/su", "/data/local/xbin/su")
        return paths.any { File(it).exists() }
    }

    fun isAppCloned(): Boolean {
        val path = context.filesDir.absolutePath
        return path.contains("999") || path.contains("101") || !path.startsWith("/data/user/0/")
    }

    fun isOriginalPackage(): Boolean = context.packageName == ORIG_PKG

    fun activateSelfDestruct() {
        try {
            context.cacheDir.deleteRecursively()
            context.filesDir.deleteRecursively()
            android.os.Process.killProcess(android.os.Process.myPid())
        } catch (e: Exception) { System.exit(1) }
    }

    // --- 5. BẢO MẬT NÂNG CAO (KẾT NỐI & CHỮ KÝ) ---
    fun isSignatureValid(): Boolean {
        return try {
            val pm = context.packageManager
            val sigs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES).signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES).signatures
            }
            sigs != null && sigs.isNotEmpty()
        } catch (e: Exception) { false }
    }

    fun isDebugging(): Boolean {
        return Debug.isDebuggerConnected() || 
                Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) != 0
    }

    fun isRemoteAccessDetected(): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        return am.isEnabled && !am.getEnabledAccessibilityServiceList(-1).isNullOrEmpty()
    }

    fun isBootloaderUnlocked(): Boolean = Build.BOOTLOADER.lowercase().contains("unlock") || Build.DISPLAY.lowercase().contains("test-keys")

    fun isManifestTampered(): Boolean = !isOriginalPackage()
    fun isResourceModified(): Boolean = !isSignatureValid()
}
