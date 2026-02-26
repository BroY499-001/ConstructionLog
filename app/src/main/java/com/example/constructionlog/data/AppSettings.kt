package com.example.constructionlog.data

import android.content.Context

class AppSettings(context: Context) {
    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun isAppAuthEnabled(): Boolean = prefs.getBoolean(KEY_APP_AUTH_ENABLED, false)

    fun setAppAuthEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_APP_AUTH_ENABLED, enabled).apply()
    }

    fun getReauthSeconds(): Int = prefs.getInt(KEY_REAUTH_SECONDS, DEFAULT_REAUTH_SECONDS)

    fun setReauthSeconds(seconds: Int) {
        if (seconds in REAUTH_OPTIONS_SECONDS) {
            prefs.edit().putInt(KEY_REAUTH_SECONDS, seconds).apply()
        }
    }

    companion object {
        private const val PREF_NAME = "construction_log_settings"
        private const val KEY_APP_AUTH_ENABLED = "app_auth_enabled"
        private const val KEY_REAUTH_SECONDS = "reauth_seconds"

        const val DEFAULT_REAUTH_SECONDS = 300
        val REAUTH_OPTIONS_SECONDS = listOf(30, 60, 300)
    }
}
