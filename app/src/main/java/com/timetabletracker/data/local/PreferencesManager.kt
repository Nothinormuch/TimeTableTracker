package com.timetabletracker.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Manages all app preferences: encrypted credentials/tokens + plain settings.
 */
class PreferencesManager(context: Context) {

    companion object {
        private const val ENCRYPTED_PREFS_FILE = "timetable_secure_prefs"
        private const val PLAIN_PREFS_FILE = "timetable_plain_prefs"

        // Encrypted keys
        const val KEY_USER_ID = "user_id"
        const val KEY_PASSWORD = "password"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_TOKEN_EXPIRY = "token_expiry"
        const val KEY_STUDENT_ID = "student_id"

        // Plain keys
        const val KEY_CHECK_HOUR = "check_hour"
        const val KEY_CHECK_MINUTE = "check_minute"
        const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        const val KEY_LAST_CHECK_TIME = "last_check_time"
        const val KEY_IS_MONITORING_ACTIVE = "is_monitoring_active"

        // Defaults
        const val DEFAULT_CHECK_HOUR = 8
        const val DEFAULT_CHECK_MINUTE = 0
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        ENCRYPTED_PREFS_FILE,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val plainPrefs: SharedPreferences =
        context.getSharedPreferences(PLAIN_PREFS_FILE, Context.MODE_PRIVATE)

    // ─── Encrypted getters/setters ───────────────────────────────────────────

    var userId: String
        get() = encryptedPrefs.getString(KEY_USER_ID, "") ?: ""
        set(value) = encryptedPrefs.edit().putString(KEY_USER_ID, value).apply()

    var password: String
        get() = encryptedPrefs.getString(KEY_PASSWORD, "") ?: ""
        set(value) = encryptedPrefs.edit().putString(KEY_PASSWORD, value).apply()

    var accessToken: String
        get() = encryptedPrefs.getString(KEY_ACCESS_TOKEN, "") ?: ""
        set(value) = encryptedPrefs.edit().putString(KEY_ACCESS_TOKEN, value).apply()

    var tokenExpiry: Long
        get() = encryptedPrefs.getLong(KEY_TOKEN_EXPIRY, 0L)
        set(value) = encryptedPrefs.edit().putLong(KEY_TOKEN_EXPIRY, value).apply()

    var studentId: String
        get() = encryptedPrefs.getString(KEY_STUDENT_ID, "") ?: ""
        set(value) = encryptedPrefs.edit().putString(KEY_STUDENT_ID, value).apply()

    // ─── Plain getters/setters ───────────────────────────────────────────────

    var checkHour: Int
        get() = plainPrefs.getInt(KEY_CHECK_HOUR, DEFAULT_CHECK_HOUR)
        set(value) = plainPrefs.edit().putInt(KEY_CHECK_HOUR, value).apply()

    var checkMinute: Int
        get() = plainPrefs.getInt(KEY_CHECK_MINUTE, DEFAULT_CHECK_MINUTE)
        set(value) = plainPrefs.edit().putInt(KEY_CHECK_MINUTE, value).apply()

    var notificationsEnabled: Boolean
        get() = plainPrefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
        set(value) = plainPrefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, value).apply()

    var lastCheckTime: String
        get() = plainPrefs.getString(KEY_LAST_CHECK_TIME, "") ?: ""
        set(value) = plainPrefs.edit().putString(KEY_LAST_CHECK_TIME, value).apply()

    var isMonitoringActive: Boolean
        get() = plainPrefs.getBoolean(KEY_IS_MONITORING_ACTIVE, false)
        set(value) = plainPrefs.edit().putBoolean(KEY_IS_MONITORING_ACTIVE, value).apply()

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Returns true if a cached token exists and has more than 5 minutes remaining.
     */
    fun isTokenValid(): Boolean {
        val token = accessToken
        if (token.isBlank()) return false
        val remaining = tokenExpiry - System.currentTimeMillis()
        return remaining > 5 * 60 * 1000L // 5 minutes buffer
    }

    /**
     * Stores a freshly obtained access token with a 45-minute TTL.
     */
    fun saveToken(token: String) {
        accessToken = token
        tokenExpiry = System.currentTimeMillis() + 45 * 60 * 1000L
    }

    fun isLoggedIn(): Boolean = userId.isNotBlank() && password.isNotBlank()

    fun clearTokens() {
        encryptedPrefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_TOKEN_EXPIRY)
            .apply()
    }
}
