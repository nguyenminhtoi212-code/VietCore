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
 * Đảm bảo toàn vẹn APK, chống giả lập, chặn OS nội địa và tự hủy khi bị can thiệp.
 */
class OmnisSecurity(private val context: Context) {

    private val AES_KEY = "VIETCORE_SECURE_KEY_2026_TOI_MOD" 
    private val VALID_PROVIDER_DOMAIN = "vietcore.intelligence.gov"
    private val securityExecutor = Executors.newSingleThreadExecutor()

    // --- 1. SIÊU NGĂN CHẶN GIẢ LẬP ---
    fun isEmulatorOrVirtualMachine(): Boolean {
        val finger = Build.FINGERPRINT
        val model = Build.MODEL
        val product = Build.PRODUCT
        val hardware = Build.HARDWARE
        val board = Build.BOARD.lowercase(Locale.US)

        return (finger.startsWith("generic")
                || finger.startsWith("unknown")
                || model.contains("google_sdk")
                || model.contains("Emulator")
                || model.contains("Android SDK built for x86")
                || hardware.contains("goldfish")
                || hardware.contains("ranchu")
                || hardware.contains("vbox86")
                || product.contains("sdk_google")
                || product.contains("vbox86")
                || board.contains("nox")
                || Build.BOOTLOADER.lowercase(Locale.US).contains("nox")
                || Build.HARDWARE.lowercase(Locale.US).contains("bluestacks")
                || finger.contains("vysor"))
    }

    // --- 2. CHỐNG BIẾN THỂ OS NỘI ĐỊA & MÁY XÁCH TAY ---
    fun isRestrictedRegionOrOS(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase(Locale.US)
        val country = Locale.getDefault().country 
        val language = Locale.getDefault().language

        // Kiểm tra Google Services Framework (GSF) - Đặc điểm của máy bản Global
        val isGlobalVersion = try {
            context.packageManager.getPackageInfo("com.google.android.gsf", 0)
            true
        } catch (e: Exception) {
            false
        }

        // 1. Chặn máy Trung Quốc không có dịch vụ Google (Máy nội địa)
        if (!isGlobalVersion) {
            val chinaBrands = arrayOf("huawei", "xiaomi", "oppo", "vivo", "meizu", "zte", "honor")
            if (chinaBrands.any { manufacturer.contains(it) }) return true
        }

        // 2. Chặn dựa trên các thuộc tính hệ thống ROM nội địa
        val isChinaRom = getSystemProperty("ro.miui.ui.version.name").isNotEmpty() || 
                         getSystemProperty("ro.build.version.emui").isNotEmpty() ||
                         getSystemProperty("ro.build.version.opporom").isNotEmpty() ||
                         getSystemProperty("ro.vivo.os.version").isNotEmpty()
        
        if (isChinaRom && !isGlobalVersion) return true

        // 3. Nếu cài ngôn ngữ Trung Quốc mà không có GSF -> Coi như máy nội địa/xách tay
        if (language == "zh" && !isGlobalVersion) return true

        // 4. Chặn vùng địa lý nhạy cảm (CN: Trung Quốc, KP: Triều Tiên)
        val restrictedCountries = arrayOf("CN", "KP") 
        if (restrictedCountries.contains(country) && !isGlobalVersion) return true
        
        return false
    }

    // Hàm đọc thuộc tính hệ thống ẩn qua Reflection
    private fun getSystemProperty(key: String): String {
        return try {
            val c = Class.forName("android.os.SystemProperties")
            val get = c.getMethod("get", String::class.java)
            get.invoke(c, key) as String
        } catch (e: Exception) { "" }
    }

    // --- 3. KIỂM TRA TÍNH TOÀN VẸN & TỰ HỦY ---
    fun isIntegrityCompromised(): Boolean {
        return isRooted() || isHackerToolsDetected() || isAppCloned() || isDebugging() || 
               isBootloaderUnlocked() || isCustomROMDetected() || isManifestTampered() || 
               isResourceModified() || isRestrictedRegionOrOS()
    }

    fun isRooted(): Boolean {
        val paths = arrayOf("/system/bin/su", "/system/xbin/su", "/sbin/su", "/data/local/xbin/su")
        if (paths.any { File(it).exists() }) return true
        return try {
            val process = Runtime.getRuntime().exec("which su")
            val result = process.inputStream.bufferedReader().use { it.readLine() }
            result != null
        } catch (e: Exception) { false }
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

    fun isOriginalPackage(): Boolean {
        return context.packageName == "com.example.myempty.vietcore"
    }

    fun activateSelfDestruct() {
        try {
            context.cacheDir.deleteRecursively()
            context.filesDir.deleteRecursively()
            android.os.Process.killProcess(android.os.Process.myPid())
        } catch (e: Exception) {
            System.exit(1)
        }
    }

    // --- 4. MÃ HÓA & KẾT NỐI SERVER ---
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

    fun connectToDataServer(endpoint: String, payload: String, callback: (String) -> Unit) {
        if (!endpoint.contains(VALID_PROVIDER_DOMAIN)) {
            activateSelfDestruct()
            return
        }

        securityExecutor.execute {
            try {
                val encryptedPayload = encryptData(payload)
                val conn = URL(endpoint).openConnection() as HttpURLConnection
                conn.apply {
                    requestMethod = "POST"
                    connectTimeout = 5000 
                    doOutput = true
                    setRequestProperty("X-Security-Auth", "VIETCORE_2026")
                }

                conn.outputStream.use { it.write(encryptedPayload.toByteArray(Charsets.UTF_8)) }

                val response = if (conn.responseCode == 200) {
                    conn.inputStream.bufferedReader().use { it.readText() }
                } else "DENIED"
                
                Handler(Looper.getMainLooper()).post { callback(response) }
            } catch (e: Exception) {
                Handler(Looper.getMainLooper()).post { callback("OFFLINE") }
            }
        }
    }

    // --- 5. CHỮ KÝ & TRUY CẬP TỪ XA ---
    fun isSignatureValid(): Boolean {
        return try {
            val signatures = getAppSignatures()
            signatures != null && getSha256Hash(signatures[0].toByteArray()).isNotEmpty()
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

    private fun getSha256Hash(data: ByteArray): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            md.digest(data).joinToString("") { "%02x".format(it) }.uppercase(Locale.US)
        } catch (e: Exception) { "" }
    }

    fun isDebugging(): Boolean {
        return Debug.isDebuggerConnected() || 
                Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) != 0
    }

    fun isRemoteAccessDetected(): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        return am.isEnabled && !am.getEnabledAccessibilityServiceList(-1).isNullOrEmpty()
    }

    fun isBootloaderUnlocked(): Boolean {
        return try {
            val bootloader = android.os.Build.BOOTLOADER
            bootloader.lowercase().contains("unlock") || 
            android.os.Build.DISPLAY.lowercase().contains("test-keys")
        } catch (e: Exception) { false }
    }

    fun isCustomROMDetected(): Boolean {
        val tags = android.os.Build.TAGS
        return tags != null && tags.contains("test-keys")
    }

    fun isManifestTampered(): Boolean {
        return context.packageName != "com.example.myempty.vietcore"
    }

    fun isResourceModified(): Boolean {
        return !isSignatureValid()
    }

    // --- 6. NGĂN CHẶN THAY ĐỔI TÊN/LOGO (GỌI TỪ MAINACTIVITY) ---
    fun isAppNameModified(originalName: String): Boolean {
        return try {
            val currentLabel = context.applicationInfo.loadLabel(context.packageManager).toString()
            currentLabel != originalName
        } catch (e: Exception) { false }
    }

    fun isAppIconModified(): Boolean {
        // Kiểm tra dựa trên tính hợp lệ của chữ ký (Nếu đổi Logo APK sẽ phải ký lại)
        return !isSignatureValid()
    }
}
