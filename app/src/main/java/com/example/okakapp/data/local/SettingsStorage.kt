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

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "okak_settings")

class SettingsStorage(private val context: Context) {
    private val themeKey = stringPreferencesKey("theme")
    private val searchHistoryKey = stringPreferencesKey("search_history")

    val themeFlow: Flow<ThemeMode> = context.settingsStore.data.map { prefs ->
        ThemeMode.fromString(prefs[themeKey])
    }

    suspend fun getTheme(): ThemeMode = themeFlow.first()

    suspend fun setTheme(mode: ThemeMode) {
        context.settingsStore.edit { it[themeKey] = mode.name }
    }

    val searchHistoryFlow: Flow<List<String>> = context.settingsStore.data.map { prefs ->
        prefs[searchHistoryKey]?.split("\n")?.filter { it.isNotBlank() } ?: emptyList()
    }

    suspend fun getSearchHistory(): List<String> = searchHistoryFlow.first()

    suspend fun addSearchQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        context.settingsStore.edit { prefs ->
            val current = prefs[searchHistoryKey]?.split("\n")?.filter { it.isNotBlank() } ?: emptyList()
            val updated = (listOf(trimmed) + current.filter { it != trimmed }).take(10)
            prefs[searchHistoryKey] = updated.joinToString("\n")
        }
    }

    suspend fun clearSearchHistory() {
        context.settingsStore.edit { it.remove(searchHistoryKey) }
    }
}

enum class ThemeMode {
    SYSTEM, LIGHT, DARK;

    companion object {
        fun fromString(raw: String?): ThemeMode = entries.firstOrNull { it.name == raw } ?: SYSTEM
    }
}
