package com.example.turtle

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

class SettingsPreferences(private val context: Context) {
    companion object {
        private val Context.dataStore by preferencesDataStore(name = "settings")

        private val userId = stringPreferencesKey("userId")
        private val username = stringPreferencesKey("username")
        private val email = stringPreferencesKey("email")
    }

    val getUserId get() = context.dataStore.data.map { it[userId]!! }
    val getUsername get() = context.dataStore.data.map { it[username]!! }
    val getEmail get() = context.dataStore.data.map { it[email]!! }


    suspend fun setUserInfo(uid: String, uname: String, userEmail: String) {
        context.dataStore.edit {
            it[userId] = uid
            it[username] = uname
            it[email] = userEmail
        }
    }

    suspend fun clearPreferences() {
        context.dataStore.edit { it.clear() }
    }
}