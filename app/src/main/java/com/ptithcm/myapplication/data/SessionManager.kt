package com.ptithcm.myapplication.data

import android.content.Context

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
        preferences.edit().clear().apply()
    }

    companion object {
        private const val PREF_NAME = "auth_session"
        private const val KEY_USER_ID = "user_id"
        private const val NO_USER_ID = -1L
    }
}
