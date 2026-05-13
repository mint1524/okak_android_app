package com.example.okakapp.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "okak_prefs")

class TokenStorage(private val context: Context) {

    private val accessKey = stringPreferencesKey("access_token")
    private val refreshKey = stringPreferencesKey("refresh_token")

    @Volatile private var cachedAccess: String? = null
    @Volatile private var cachedRefresh: String? = null

    val tokenFlow: Flow<String?> = context.dataStore.data.map { it[accessKey] }

    suspend fun get(): String? = tokenFlow.first().also { cachedAccess = it }
    suspend fun getRefresh(): String? = context.dataStore.data.map { it[refreshKey] }.first().also { cachedRefresh = it }

    fun getCached(): String? = cachedAccess
    fun getCachedRefresh(): String? = cachedRefresh

    suspend fun save(access: String, refresh: String?) {
        context.dataStore.edit {
            it[accessKey] = access
            if (refresh != null) it[refreshKey] = refresh
        }
        cachedAccess = access
        cachedRefresh = refresh
    }

    suspend fun clear() {
        context.dataStore.edit {
            it.remove(accessKey)
            it.remove(refreshKey)
        }
        cachedAccess = null
        cachedRefresh = null
    }
}
