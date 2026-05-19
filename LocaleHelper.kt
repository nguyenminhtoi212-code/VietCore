package com.example.myempty.vietcore

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

/**
 * LocaleHelper: Quản lý đa ngôn ngữ đặc biệt cho VietCore.
 * Đã đồng bộ tên cài đặt là "settings" để tương thích với hệ thống tiếng Anh.
 */
object LocaleHelper {
    private const val SELECTED_LANGUAGE = "Locale.Helper.Selected.Language"
    private const val PREF_NAME = "settings" // Tên cài đặt gọi lại tiếng Anh

    fun setLocale(context: Context, language: String?): Context {
        persist(context, language)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            updateResources(context, language)
        } else {
            updateResourcesLegacy(context, language)
        }
    }

    fun getLanguage(context: Context): String? {
        val preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return preferences.getString(SELECTED_LANGUAGE, Locale.getDefault().toLanguageTag())
    }

    private fun persist(context: Context, language: String?) {
        val preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        preferences.edit().putString(SELECTED_LANGUAGE, language).apply()
    }

    private fun updateResources(context: Context, language: String?): Context {
        // Sử dụng forLanguageTag để hỗ trợ định dạng quốc gia (ví dụ: vi-VN)
        val locale = Locale.forLanguageTag(language ?: "en")
        Locale.setDefault(locale)
        
        val configuration: Configuration = context.resources.configuration
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)
        
        return context.createConfigurationContext(configuration)
    }

    @Suppress("DEPRECATION")
    private fun updateResourcesLegacy(context: Context, language: String?): Context {
        val locale = Locale.forLanguageTag(language ?: "en")
        Locale.setDefault(locale)
        
        val resources = context.resources
        val configuration = resources.configuration
        
        configuration.setLocale(locale)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            configuration.setLayoutDirection(locale)
        }
        
        resources.updateConfiguration(configuration, resources.displayMetrics)
        return context
    }
}
