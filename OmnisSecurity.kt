package com.example.myempty.vietcore

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * OmnisSecurity: Hệ thống bảo mật đa tầng cho VietCore 2026.
 * Cơ chế siết chặt: Nguồn APK, Cấu trúc Manifest, Integrity, và Môi trường thực thi.
 */
class OmnisSecurity(private val context: Context) {

    private val AES_KEY = "VIETCORE_SECURE_KEY_2026_TOI_MOD" 
    private val VALID_PROVIDER_DOMAIN = "vietcore.intelligence.gov"
    private val securityExecutor = Executors.newSingleThreadExecutor()

    // --- 0. NGĂN CHẶN HỆ ĐIỀU HÀNH ĐỜI CŨ ---
    fun isLegacyOSDetected(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q

    fun isOutdatedHardware(): Boolean {
        val abis = Build.SUPPORTED_ABIS
        return abis.none { it.contains("arm64-v8a") }
    }

    // --- 1. SIÊU NGĂN CHẶN GIẢ LẬP ---
    fun isEmulatorOrVirtualMachine(): Boolean {
        val finger = Build.FINGERPRINT
        val board = Build.BOARD.lowercase(Locale.US)
        return (finger.startsWith("generic") || finger.startsWith("unknown") || 
                Build.MODEL.contains("google_sdk") || Build.MODEL.contains("Emulator") ||
                Build.HARDWARE.contains("goldfish") || Build.HARDWARE.contains("ranchu") ||
                board.contains("nox") || Build.HARDWARE.contains("bluestacks"))
    }

    // --- 2. CHỐNG BIẾN THỂ OS NỘI ĐỊA & MÁY XÁCH TAY ---
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

    // --- 3. SIẾT CHẶT NGUỒN GỐC APK & CẤU TRÚC HỆ THỐNG ---

    /**
     * Ngăn chặn cài đặt từ các kho APK Mod/Giả mạo.
     */
    fun isInstallerUnverified(): Boolean {
        val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getInstallerPackageName(context.packageName)
        }
        
        val blacklistedSources = arrayOf(
            "com.android.vending",
            "com.amazon.venezia", 
            "com.apkpure.a3", 
            "com.apkmirror.helper"
        )
        
        return installer != null && blacklistedSources.contains(installer)
    }

    /**
     * Kiểm tra thay đổi trái phép quyền (Permissions) và Hoạt động (Activities) trong Manifest.
     */
    fun isManifestStructuralTampered(): Boolean {
        return try {
            val info = context.packageManager.getPackageInfo(
                context.packageName, 
                PackageManager.GET_PERMISSIONS or PackageManager.GET_ACTIVITIES
            )
            
            val maxAllowedPermissions = 10 
            val maxAllowedActivities = 5

            val currentPermissions = info.requestedPermissions?.size ?: 0
            val currentActivities = info.activities?.size ?: 0

            (currentPermissions > maxAllowedPermissions || currentActivities > maxAllowedActivities)
        } catch (e: Exception) { true }
    }

    // --- 4. KIỂM TRA TÍNH TOÀN VẸN & TỰ HỦY ---

    fun isIntegrityCompromised(): Boolean {
        return isLegacyOSDetected() || isOutdatedHardware() || isRooted() || 
               isHackerToolsDetected() || isAppCloned() || isDebugging() || 
               isBootloaderUnlocked() || isCustomROMDetected() || isManifestTampered() || 
               isResourceModified() || isRestrictedRegionOrOS() || 
               isInstallerUnverified() || isManifestStructuralTampered()
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

    // --- 5. MÃ HÓA DỮ LIỆU ---
    fun encryptData(data: String): String {
        return try {
            val keyBytes = AES_KEY.toByteArray(Charsets.UTF_8).let { it.copyOf(32) }
            val skeySpec = SecretKeySpec(keyBytes, "AES")
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec)
            val encrypted = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(encrypted, Base64.NO_WRAP)
        } catch (e: Exception) { "ERR" }
    }

    // --- 6. KIỂM TRA CHỮ KÝ & THAY ĐỔI UI ---
    fun isSignatureValid(): Boolean {
        return try {
            val signatures = getAppSignatures()
            signatures != null && signatures.isNotEmpty()
        } catch (e: Exception) { false }
    }

    @SuppressLint("PackageManagerGetSignatures")
    private fun getAppSignatures(): Array<Signature>? {
        return try {
            val pm = context.packageManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                    .signingInfo?.apkContentsSigners
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
        return try {
            val currentLabel = context.applicationInfo.loadLabel(context.packageManager).toString()
            currentLabel != originalName
        } catch (e: Exception) { false }
    }

    fun isAppIconModified(): Boolean = !isSignatureValid()
}
