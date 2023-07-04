package com.example.turtle

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.turtle.data.Profile
import kotlinx.coroutines.flow.map

class SettingsPreferences(private val context: Context) {
    companion object {
        private val Context.dataStore by preferencesDataStore(name = "settings")

        private val userId = stringPreferencesKey("userId")
        private val username = stringPreferencesKey("username")
    }

    val getUserId get() = context.dataStore.data.map { it[userId]!! }
    val getUsername get() = context.dataStore.data.map { it[username]!! }


    suspend fun setUserInfo(profile: Profile) {
        context.dataStore.edit {
            it[userId] = profile.userId!!
            it[username] = profile.username!!
        }
    }
}