package com.ben.periodt.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Extension property to create DataStore instance
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

// Preference key for dark theme setting
private val KEY_DARK_THEME = booleanPreferencesKey("dark_theme")

class ThemePrefs(private val context: Context) {

    /**
     * Flow that emits the current dark theme preference
     * Defaults to false (light theme) if no preference is set
     */
    val isDark: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_DARK_THEME] ?: false
    }

    /**
     * Saves the dark theme preference
     * @param enabled true for dark theme, false for light theme
     */
    suspend fun setDark(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_DARK_THEME] = enabled
        }
    }
}