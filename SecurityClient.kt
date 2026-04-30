package com.example.myempty.vietcore

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CertificatePinner
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * SecurityClient: Quản lý giao thức mạng bảo mật cho VietCore.
 * Tích hợp đa máy chủ: VietCore Gov, iCloud Storage, và IP Intelligence.
 * Nhà phát triển: Nguyễn Minh Tới.
 */
class SecurityClient private constructor() {

    companion object {
        // 1. SSL Pinning để bảo vệ kết nối đến các máy chủ quan trọng
        private val certificatePinner = CertificatePinner.Builder()
            .add("vietcore.intelligence.gov", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
            .add("storage.icloud.com", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
            .build()

        // 2. Cấu hình Client với các thông số siết chặt
        private val client = OkHttpClient.Builder()
            .certificatePinner(certificatePinner)
            .connectTimeout(10, TimeUnit.SECONDS) // Giảm timeout để phản ứng nhanh hơn
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .followRedirects(false) // Chống redirect đến trang web giả mạo
            .retryOnConnectionFailure(false)
            .build()

        @Volatile
        private var INSTANCE: SecurityClient? = null

        fun getInstance(): SecurityClient {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SecurityClient().also { INSTANCE = it }
            }
        }
    }

    /**
     * Tự động lấy địa chỉ IP từ các nguồn uy tín (AWS/Ipify) 
     * dùng để đối chiếu với vùng cấm (China/Korea).
     */
    suspend fun fetchCurrentIP(): String = withContext(Dispatchers.IO) {
        val sources = arrayOf("https://checkip.amazonaws.com/", "https://api.ipify.org/")
        for (url in sources) {
            try {
                val request = Request.Builder().url(url).get().build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val ip = response.body?.string()?.trim()
                        if (!ip.isNullOrEmpty()) return@withContext ip
                    }
                }
            } catch (e: Exception) { continue }
        }
        return@withContext "IP_UNKNOWN"
    }

    /**
     * Giao thức xác minh tính toàn vẹn với máy chủ Chính phủ VietCore.
     */
    suspend fun verifyLaunchIntegrity(payload: String): String = withContext(Dispatchers.IO) {
        return@withContext performPostRequest("https://vietcore.intelligence.gov/check", payload)
    }

    /**
     * Đồng bộ dữ liệu bảo mật lên iCloud Storage (VietCore Repository).
     */
    suspend fun syncToICloud(payload: String): String = withContext(Dispatchers.IO) {
        return@withContext performPostRequest("https://storage.icloud.com/vietcore/", payload)
    }

    /**
     * Hàm thực thi yêu cầu POST bảo mật với Header tùy chỉnh cho phiên bản 26.1.3.
     */
    private fun performPostRequest(url: String, payload: String): String {
        return try {
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = payload.toRequestBody(mediaType)
            
            val request = Request.Builder()
                .url(url)
                .post(body)
                .addHeader("X-Security-Token", "VietCore-Omnis-Pro-2026")
                .addHeader("X-Developer-ID", "NguyenMinhToi") // Định danh nhà phát triển duy nhất
                .addHeader("User-Agent", "Omnis-Security-Shield-V26")
                .build()

            client.newCall(request).execute().use { response ->
                when {
                    response.isSuccessful -> response.body?.string() ?: "SUCCESS_EMPTY"
                    response.code == 403 -> "ACCESS_FORBIDDEN"
                    else -> "SERVER_ERROR: ${response.code}"
                }
            }
        } catch (e: Exception) {
            "NETWORK_COMPROMISED: ${e.localizedMessage}"
        }
    }
}
