package com.smartplug.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import com.smartplug.app.ui.localization.AppLanguage

private val Context.dataStore by preferencesDataStore(name = "smartplug_prefs")

enum class AppThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val soundEnabled: Boolean = true,
    val hapticEnabled: Boolean = true,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val sidebarHidden: Boolean = false,
    val language: AppLanguage = AppLanguage.INDONESIAN,
)

/** Non-secret UI/app preferences. Secrets live in [SecureTokenStore] instead. */
@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val HAPTIC_ENABLED = booleanPreferencesKey("haptic_enabled")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SIDEBAR_HIDDEN = booleanPreferencesKey("sidebar_hidden")
        val LANGUAGE = stringPreferencesKey("language")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
            hapticEnabled = prefs[Keys.HAPTIC_ENABLED] ?: true,
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { AppThemeMode.valueOf(it) }.getOrNull() }
                ?: AppThemeMode.SYSTEM,
            sidebarHidden = prefs[Keys.SIDEBAR_HIDDEN] ?: false,
            language = prefs[Keys.LANGUAGE]?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() }
                ?: AppLanguage.INDONESIAN,
        )
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SOUND_ENABLED] = enabled }
    }

    suspend fun setHapticEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HAPTIC_ENABLED] = enabled }
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setSidebarHidden(hidden: Boolean) {
        context.dataStore.edit { it[Keys.SIDEBAR_HIDDEN] = hidden }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { it[Keys.LANGUAGE] = language.name }
    }

    /** Restores non-secret app preferences after an explicit full app reset. */
    suspend fun reset() {
        context.dataStore.edit { it.clear() }
    }
}
