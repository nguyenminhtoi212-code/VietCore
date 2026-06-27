package com.example.myempty.vietcore

import android.annotation.SuppressLint
import android.content.Context
import android.app.ActivityManager
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
 * DeviceSimulator: Real-time device metrics collection and simulation system.
 * Designed to provide comprehensive data for VietCore security reporting.
 */
class DeviceSimulator(private val context: Context) {

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    /**
     * Helper function to dynamically retrieve string resource ID to guarantee compile stability.
     */
    private fun getStringResId(resName: String): Int {
        return context.resources.getIdentifier(resName, "string", context.packageName)
    }

    private fun getSafeString(resName: String): String {
        val id = getStringResId(resName)
        return if (id != 0) context.getString(id) else ""
    }

    private fun getSafeString(resName: String, vararg formatArgs: Any): String {
        val id = getStringResId(resName)
        return if (id != 0) context.getString(id, *formatArgs) else ""
    }

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
        getSafeString("value_secure_gateway")
    }

    /**
     * Resolves and provides detailed status for SIM & eSIM cards
     */
    private fun getSimDetailedStatus(): String {
        return try {
            val sm = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
            val activeSubscriptions = sm.activeSubscriptionInfoList
            
            if (activeSubscriptions.isNullOrEmpty()) {
                getSafeString("sim_status_none")
            } else {
                val sb = StringBuilder()
                activeSubscriptions.forEachIndexed { index, info ->
                    val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && info.isEmbedded) {
                        getSafeString("sim_type_esim")
                    } else {
                        getSafeString("sim_type_physical")
                    }
                    
                    // Identify country location via SIM ISO code
                    val countryIso = info.countryIso.uppercase()
                    val locale = Locale.Builder().setRegion(countryIso).build()
                    val countryName = if (countryIso.isNotEmpty()) {
                        locale.getDisplayCountry(Locale.US)
                    } else {
                        getSafeString("value_global")
                    }
                    
                    val line = getSafeString(
                        "sim_slot_format", 
                        index + 1, 
                        info.displayName ?: "", 
                        type, 
                        countryName
                    )
                    sb.append(line).append("\n")
                }
                sb.toString().trim()
            }
        } catch (e: Exception) { 
            getSafeString("sim_status_denied") 
        }
    }

    /**
     * Resolves network parameters for WiFi & Mobile Modem
     */
    private fun getNetworkMetrics(): String {
        return try {
            val network = connectivityManager.activeNetwork
            val caps = connectivityManager.getNetworkCapabilities(network) 
                ?: return getSafeString("network_offline")
                
            val speedMbps = caps.linkDownstreamBandwidthKbps / 1000.0
            val speedDisplay = String.format(Locale.US, "%.1f Mbps", speedMbps)
            
            val systemCountry = context.resources.configuration.locales[0].displayCountry
            
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                @Suppress("DEPRECATION")
                val ssid = wifiManager.connectionInfo.ssid.replace("\"", "")
                val wifiName = if (ssid == "<unknown ssid>") {
                    getSafeString("network_gateway_default")
                } else {
                    ssid
                }
                getSafeString("network_wifi_format", wifiName, speedDisplay, systemCountry)
            } else {
                val operatorName = telephonyManager.networkOperatorName
                getSafeString("network_mobile_format", operatorName, speedDisplay, systemCountry)
            }
        } catch (e: Exception) { 
            getSafeString("network_analyzing") 
        }
    }

    private fun getKernelVersion(): String = System.getProperty("os.version") 
        ?: getSafeString("value_unknown")

    private fun getSELinuxStatus(): String {
        return try {
            val process = Runtime.getRuntime().exec("getenforce")
            val result = process.inputStream.bufferedReader().use { it.readLine() }
            result ?: getSafeString("value_enforcing")
        } catch (e: Exception) { 
            getSafeString("value_enforcing") 
        }
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
        } catch (e: Exception) { 
            getSafeString("value_protected") 
        }
    }

    /**
     * Resolves graphics and GPU configuration metrics
     */
    private fun getGraphicsMetrics(): String {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val configInfo = activityManager.deviceConfigurationInfo
            val openGlVersion = configInfo.glEsVersion

            // Check system properties for hardware renderer fallbacks often set by SOC layers
            val glRenderer = SystemPropertiesProxy.get(context, "ro.hardware.egl", "")
                .ifEmpty { SystemPropertiesProxy.get(context, "ro.board.platform", getSafeString("value_unknown")) }

            // Safe lookup format: "OpenGL ES $openGlVersion ($glRenderer)"
            getSafeString("report_graphics_format", openGlVersion, glRenderer.uppercase())
        } catch (e: Exception) {
            getSafeString("value_unknown")
        }
    }

    @SuppressLint("HardwareIds", "MissingPermission")
    suspend fun getRealTimeSpecs(): String {
        val publicIP = getPublicIP()
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)?.uppercase() 
            ?: getSafeString("value_unknown")

        val googlePlayPatch = if (Build.VERSION.SDK_INT >= 29) {
            Build.VERSION.SECURITY_PATCH
        } else {
            getSafeString("value_not_available")
        }

        val imeiStatus = if (Build.VERSION.SDK_INT < 29) {
            getSafeString("value_redacted")
        } else {
            getSafeString("value_secure_hash")
        }

        val sb = StringBuilder()
        sb.append(getSafeString("report_header")).append("\n")
        sb.append(getSafeString("report_time", time)).append("\n")
        sb.append(getSafeString("report_divider")).append("\n")
        
        sb.append(getSafeString("report_device", Build.MANUFACTURER.uppercase(), Build.MODEL)).append("\n")
        sb.append(getSafeString("report_codename", Build.DEVICE, Build.BOARD)).append("\n\n")
        
        sb.append(getSafeString("section_system_software")).append("\n")
        sb.append(getSafeString("report_os_version", Build.VERSION.RELEASE, Build.VERSION.SDK_INT)).append("\n")
        sb.append(getSafeString("report_build_id", Build.ID)).append("\n")
        sb.append(getSafeString("report_build_number", Build.DISPLAY)).append("\n")
        sb.append(getSafeString("report_kernel", getKernelVersion())).append("\n")
        sb.append(getSafeString("report_selinux", getSELinuxStatus())).append("\n")
        sb.append(getSafeString("report_google_play", googlePlayPatch)).append("\n")
        sb.append(getSafeString("report_security_patch", Build.VERSION.SECURITY_PATCH)).append("\n\n")
        
        sb.append(getSafeString("section_telecom_modem")).append("\n")
        sb.append(getSimDetailedStatus()).append("\n")
        sb.append(getNetworkMetrics()).append("\n")
        sb.append(getSafeString("report_baseband", Build.getRadioVersion() ?: getSafeString("value_unknown"))).append("\n")
        sb.append(getSafeString("report_carrier_software", telephonyManager.simOperatorName)).append("\n")
        sb.append(getSafeString("report_public_ip", publicIP)).append("\n\n")
        
        sb.append(getSafeString("section_identity_hardware")).append("\n")
        sb.append(getSafeString("report_android_id", androidId)).append("\n")
        sb.append(getSafeString("report_serial_no", getSerialNumber())).append("\n")
        sb.append(getSafeString("report_imei_aid", imeiStatus)).append("\n")
        sb.append(getSafeString("report_cpu", Build.HARDWARE.uppercase(), Build.SUPPORTED_ABIS.firstOrNull() ?: "")).append("\n")
        sb.append(getSafeString("report_graphics", getGraphicsMetrics())).append("\n")
        sb.append(getSafeString("report_thermal", getTemperature())).append("\n\n")
        
        sb.append(getSafeString("section_metrics")).append("\n")
        sb.append(getSafeString("report_display", getResolution())).append("\n")
        sb.append(getSafeString("report_storage", getStorageStatus())).append("\n")
        sb.append(getSafeString("report_battery_uptime", batteryLevel, SystemClock.elapsedRealtime() / 1000)).append("\n")
        sb.append(getSafeString("report_divider"))

        return sb.toString()
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
        } catch (e: Exception) { 
            getSafeString("value_not_available") 
        }
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

    /**
     * Internal proxy reflection helper to safely look up hidden build/SOC specifications.
     */
    private object SystemPropertiesProxy {
        fun get(context: Context, key: String, def: String): String {
            return try {
                val cl = context.classLoader
                val systemProperties = cl.loadClass("android.os.SystemProperties")
                val get = systemProperties.getMethod("get", String::class.java, String::class.java)
                get.invoke(systemProperties, key, def) as String
            } catch (e: Exception) {
                def
            }
        }
    }
}
