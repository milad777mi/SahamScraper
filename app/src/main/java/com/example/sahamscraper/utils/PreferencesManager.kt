package com.example.sahamscraper.utils

import android.content.Context
import androidx.preference.PreferenceManager

class PreferencesManager(context: Context) {
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    // ==========================================
    // 🔄 زمان بر حسب میلی‌ثانیه (به جای ساعت)
    // ==========================================
    var intervalMillis: Long
        get() = prefs.getLong("interval_millis", 12 * 60 * 60 * 1000L) // پیش‌فرض ۱۲ ساعت
        set(value) = prefs.edit().putLong("interval_millis", value).apply()

    var lastRunTime: Long
        get() = prefs.getLong("last_run", 0L)
        set(value) = prefs.edit().putLong("last_run", value).apply()

    // ==========================================
    // 📌 ذخیره و بازیابی داده‌های آفلاین
    // ==========================================
    fun savePendingPrices(price490: String, price532: String, price1000: String) {
        prefs.edit().apply {
            putString("pending_price490", price490)
            putString("pending_price532", price532)
            putString("pending_price1000", price1000)
            putBoolean("has_pending", true)
            apply()
        }
    }

    fun getPendingPrices(): Triple<String, String, String>? {
        if (!prefs.getBoolean("has_pending", false)) return null
        return Triple(
            prefs.getString("pending_price490", "نامشخص") ?: "نامشخص",
            prefs.getString("pending_price532", "نامشخص") ?: "نامشخص",
            prefs.getString("pending_price1000", "نامشخص") ?: "نامشخص"
        )
    }

    fun clearPending() {
        prefs.edit().apply {
            putBoolean("has_pending", false)
            remove("pending_price490")
            remove("pending_price532")
            remove("pending_price1000")
            apply()
        }
    }
}
