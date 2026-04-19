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
 * Thiết kế Singleton để tránh xung đột tài nguyên trên các thiết bị.
 */
class SecurityClient private constructor() {

    companion object {
        // 1. Cấu hình SSL Pinning chống tấn công giả mạo (MITM)
        private val certificatePinner = CertificatePinner.Builder()
            .add("api.yourserver.com", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
            .add("vietcore.intelligence.gov", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
            .build()

        // 2. Singleton OkHttpClient tối ưu hóa kết nối
        private val client = OkHttpClient.Builder()
            .certificatePinner(certificatePinner)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .followRedirects(false)
            .retryOnConnectionFailure(false)
            .connectionPool(okhttp3.ConnectionPool(5, 5, TimeUnit.MINUTES))
            .build()

        @Volatile
        private var INSTANCE: SecurityClient? = null

        // Phương thức lấy thực thể duy nhất để tránh báo lỗi trùng lặp instance
        fun getInstance(): SecurityClient {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SecurityClient().also { INSTANCE = it }
            }
        }
    }

    /**
     * Xác minh tính toàn vẹn khi khởi chạy (Dành cho MainActivity khởi động)
     */
    suspend fun verifyLaunchIntegrity(payload: String): String = withContext(Dispatchers.IO) {
        return@withContext performPostRequest("https://vietcore.intelligence.gov/check", payload)
    }

    /**
     * Gửi báo cáo an ninh (Dành cho báo cáo định kỳ)
     */
    suspend fun sendSecureReport(payload: String): String = withContext(Dispatchers.IO) {
        return@withContext performPostRequest("https://api.yourserver.com/v1/security/report", payload)
    }

    /**
     * Hàm dùng chung để thực hiện yêu cầu POST an toàn, tránh lặp code gây lỗi
     */
    private fun performPostRequest(url: String, payload: String): String {
        return try {
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = payload.toRequestBody(mediaType)
            
            val request = Request.Builder()
                .url(url)
                .post(body)
                .addHeader("X-Security-Token", "VietCore-Omnis-Pro-2026")
                .addHeader("User-Agent", "Omnis-Security-Shield-V26")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string() ?: "GENUINE_EMPTY"
                } else {
                    "SERVER_DENIED: ${response.code}"
                }
            }
        } catch (e: Exception) {
            "CONNECTION_COMPROMISED: ${e.localizedMessage}"
        }
    }
}
