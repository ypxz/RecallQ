package com.recalldeck.app.data.repo

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val retentionTarget: Double = 0.9,
    val newPerDay: Int = 20,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 9,
    val reminderMinute: Int = 0,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val autoSuspendMastered: Boolean = false,
)

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            retentionTarget = prefs[RETENTION_TARGET] ?: 0.9,
            newPerDay = prefs[NEW_PER_DAY] ?: 20,
            reminderEnabled = prefs[REMINDER_ENABLED] ?: false,
            reminderHour = prefs[REMINDER_HOUR] ?: 9,
            reminderMinute = prefs[REMINDER_MINUTE] ?: 0,
            themeMode = prefs[THEME_MODE]?.let { ThemeMode.valueOf(it) } ?: ThemeMode.SYSTEM,
            autoSuspendMastered = prefs[AUTO_SUSPEND_MASTERED] ?: false,
        )
    }

    suspend fun setRetentionTarget(value: Double) = dataStore.edit { it[RETENTION_TARGET] = value }

    suspend fun setNewPerDay(value: Int) = dataStore.edit { it[NEW_PER_DAY] = value }

    suspend fun setReminderEnabled(value: Boolean) = dataStore.edit { it[REMINDER_ENABLED] = value }

    suspend fun setReminderTime(hour: Int, minute: Int) = dataStore.edit {
        it[REMINDER_HOUR] = hour
        it[REMINDER_MINUTE] = minute
    }

    suspend fun setThemeMode(value: ThemeMode) = dataStore.edit { it[THEME_MODE] = value.name }

    suspend fun setAutoSuspendMastered(value: Boolean) =
        dataStore.edit { it[AUTO_SUSPEND_MASTERED] = value }

    companion object {
        private val RETENTION_TARGET = doublePreferencesKey("retention_target")
        private val NEW_PER_DAY = intPreferencesKey("new_per_day")
        private val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        private val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        private val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val AUTO_SUSPEND_MASTERED = booleanPreferencesKey("auto_suspend_mastered")
    }
}
