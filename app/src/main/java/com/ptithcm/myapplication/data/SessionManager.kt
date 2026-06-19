package com.ptithcm.myapplication.data

import android.content.Context

enum class AppThemeMode(val value: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark");

    companion object {
        fun fromValue(value: String?): AppThemeMode = values().firstOrNull { it.value == value } ?: SYSTEM
    }
}

class SessionManager(context: Context) {
    private val preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun saveSession(user: UserSession) {
        preferences.edit()
            .putLong(KEY_USER_ID, user.id)
            .apply()
    }

    fun getUserId(): Long? {
        val userId = preferences.getLong(KEY_USER_ID, NO_USER_ID)
        return if (userId == NO_USER_ID) null else userId
    }

    fun clearSession() {
        preferences.edit().remove(KEY_USER_ID).apply()
    }

    fun saveThemeMode(themeMode: AppThemeMode) {
        preferences.edit()
            .putString(KEY_THEME_MODE, themeMode.value)
            .apply()
    }

    fun getThemeMode(): AppThemeMode = AppThemeMode.fromValue(preferences.getString(KEY_THEME_MODE, null))

    fun isDarkTheme(themeMode: AppThemeMode, systemDarkTheme: Boolean): Boolean = when (themeMode) {
        AppThemeMode.SYSTEM -> systemDarkTheme
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    companion object {
        private const val PREF_NAME = "auth_session"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val NO_USER_ID = -1L
    }
}
