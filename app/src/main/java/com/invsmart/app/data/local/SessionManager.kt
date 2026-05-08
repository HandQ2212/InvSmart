package com.invsmart.app.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun saveLoginSession(role: String) {
        prefs.edit {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_ROLE, role)
        }
    }

    fun saveRole(role: String) {
        prefs.edit { putString(KEY_ROLE, role) }
    }

    fun saveStoreId(storeId: String) {
        prefs.edit { putString(KEY_STORE_ID, storeId) }
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    fun getRole(): String = prefs.getString(KEY_ROLE, "") ?: ""

    fun getStoreId(): String = prefs.getString(KEY_STORE_ID, "") ?: ""

    fun clear() {
        prefs.edit { clear() }
    }

    fun clearSession() {
        prefs.edit {
            putBoolean(KEY_IS_LOGGED_IN, false)
            remove(KEY_ROLE)
            remove(KEY_STORE_ID)
        }
    }

    companion object {
        private const val PREF_NAME = "invsmart_session"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_ROLE = "role"
        private const val KEY_STORE_ID = "store_id"
    }
}
