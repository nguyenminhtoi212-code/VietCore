package com.example.myempty.vietcore

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.*
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.WindowManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

/**
 * DeviceSimulator: Hệ thống mô phỏng và thu thập thông số thiết bị thời gian thực.
 * Được thiết kế để cung cấp dữ liệu cho báo cáo bảo mật của VietCore.
 */
class DeviceSimulator(private val context: Context) {

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    private suspend fun getPublicIP(): String = withContext(Dispatchers.IO) {
        val providers = listOf("https://api.ipify.org", "https://checkip.amazonaws.com")
        for (urlPath in providers) {
            try {
                val url = URL(urlPath)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 2000
                val ip = connection.inputStream.bufferedReader().use { it.readLine() }
                connection.disconnect()
                if (!ip.isNullOrEmpty()) return@withContext ip
            } catch (e: Exception) { continue }
        }
        "SECURE_GATEWAY"
    }

    /**
     * Khôi phục nhận diện Quốc gia cho SIM & eSIM
     */
    private fun getSimDetailedStatus(): String {
        return try {
            val sm = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
            val activeSubscriptions = sm.activeSubscriptionInfoList
            
            if (activeSubscriptions.isNullOrEmpty()) {
                "SIM Status: No SIM Detected"
            } else {
                val sb = StringBuilder()
                activeSubscriptions.forEachIndexed { index, info ->
                    val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && info.isEmbedded) "eSIM" else "Physical SIM"
                    
                    // Nhận diện quốc gia qua mã ISO của SIM
                    val countryIso = info.countryIso.uppercase()
                    val locale = Locale.Builder().setRegion(countryIso).build()
                    val countryName = if (countryIso.isNotEmpty()) locale.getDisplayCountry(Locale.US) else "Global"
                    
                    sb.append("Slot ${index + 1}: ${info.displayName} ($type) - $countryName\n")
                }
                sb.toString().trim()
            }
        } catch (e: Exception) { "SIM: Access Denied" }
    }

    /**
     * Khôi phục nhận diện Quốc gia cho WiFi & Modem
     */
    private fun getNetworkMetrics(): String {
        return try {
            val network = connectivityManager.activeNetwork
            val caps = connectivityManager.getNetworkCapabilities(network) ?: return "OFFLINE"
            val speedMbps = caps.linkDownstreamBandwidthKbps / 1000.0
            val speedDisplay = String.format("%.1f Mbps", speedMbps)
            
            val systemCountry = context.resources.configuration.locales[0].displayCountry
            
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                @Suppress("DEPRECATION")
                val ssid = wifiManager.connectionInfo.ssid.replace("\"", "")
                val wifiName = if (ssid == "<unknown ssid>") "Gateway" else ssid
                "WiFi: $wifiName | Speed: $speedDisplay | Region: $systemCountry"
            } else {
                val operatorName = telephonyManager.networkOperatorName
                "Mobile: $operatorName | Speed: $speedDisplay | Region: $systemCountry"
            }
        } catch (e: Exception) { "Network: Analyzing..." }
    }

    private fun getKernelVersion(): String = System.getProperty("os.version") ?: "Unknown"

    private fun getSELinuxStatus(): String {
        return try {
            val process = Runtime.getRuntime().exec("getenforce")
            val result = process.inputStream.bufferedReader().use { it.readLine() }
            result ?: "Enforcing"
        } catch (e: Exception) { "Enforcing" }
    }

    @SuppressLint("HardwareIds", "MissingPermission")
    private fun getSerialNumber(): String {
        return try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                @Suppress("DEPRECATION") Build.SERIAL
            } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                Build.getSerial()
            } else {
                "SN-${(Build.MANUFACTURER + Build.MODEL).hashCode().toString().uppercase().replace("-", "")}"
            }
        } catch (e: Exception) { "PROTECTED" }
    }

    @SuppressLint("HardwareIds", "MissingPermission")
    suspend fun getRealTimeSpecs(): String {
        val publicIP = getPublicIP()
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)?.uppercase() ?: "UNKNOWN"

        return """
            [VIETCORE INTELLIGENCE REPORT]
            Time: $time
            ----------------------------
            Device: ${Build.MANUFACTURER.uppercase()} ${Build.MODEL}
            Codename: ${Build.DEVICE} | Board: ${Build.BOARD}
            
            [SYSTEM & SOFTWARE]
            OS Version: Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})
            Build ID: ${Build.ID}
            Build Number: ${Build.DISPLAY}
            Kernel: ${getKernelVersion()}
            SELinux: ${getSELinuxStatus()}
            Google Play System: ${if (Build.VERSION.SDK_INT >= 29) Build.VERSION.SECURITY_PATCH else "N/A"}
            Security Patch: ${Build.VERSION.SECURITY_PATCH}
            
            [TELECOM & MODEM]
            ${getSimDetailedStatus()}
            ${getNetworkMetrics()}
            Baseband: ${Build.getRadioVersion() ?: "Unknown"}
            Carrier Software: ${telephonyManager.simOperatorName}
            Public IP: $publicIP
            
            [IDENTITY & HARDWARE]
            Android ID: $androidId
            Serial No: ${getSerialNumber()}
            IMEI/AID: ${if (Build.VERSION.SDK_INT < 29) "REDACTED" else "SECURE_HASH"}
            CPU: ${Build.HARDWARE.uppercase()} (${Build.SUPPORTED_ABIS.firstOrNull()})
            Thermal: ${getTemperature()}
            
            [METRICS]
            Display: ${getResolution()}
            Storage: ${getStorageStatus()}
            Battery: $batteryLevel% | Uptime: ${SystemClock.elapsedRealtime() / 1000}s
            ----------------------------
        """.trimIndent()
    }

    private fun getTemperature(): String {
        val intent = context.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
        val temp = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        return "${temp / 10.0}°C"
    }

    private fun getStorageStatus(): String {
        return try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val total = (stat.blockCountLong * stat.blockSizeLong) / (1024 * 1024 * 1024)
            val avail = (stat.availableBlocksLong * stat.blockSizeLong) / (1024 * 1024 * 1024)
            "${avail}GB / ${total}GB"
        } catch (e: Exception) { "N/A" }
    }

    private fun getResolution(): String {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = wm.currentWindowMetrics.bounds
            "${bounds.width()}x${bounds.height()}"
        } else {
            val dm = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(dm)
            "${dm.widthPixels}x${dm.heightPixels}"
        }
    }

    private val batteryLevel: Int get() {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }
}
