package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.exactTimeDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "exact_time_settings"
)

data class ExactTimeSettings(
    val use24HourFormat: Boolean = true,
    val showCentiseconds: Boolean = true,
    val clockStyleHybrid: Boolean = true,
    val tickCadence: TickCadence = TickCadence.OFF,
    val hapticTickEnabled: Boolean = true,
    val audioTickEnabled: Boolean = false,
    val themeMode: AppThemeMode = AppThemeMode.DARK,
    val referenceCityIndex: Int = 0
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val USE_24_HOUR = booleanPreferencesKey("use_24_hour")
        val SHOW_CENTISECONDS = booleanPreferencesKey("show_centiseconds")
        val CLOCK_STYLE_HYBRID = booleanPreferencesKey("clock_style_hybrid")
        val TICK_CADENCE_SECONDS = intPreferencesKey("tick_cadence_seconds")
        val HAPTIC_TICK_ENABLED = booleanPreferencesKey("haptic_tick_enabled")
        val AUDIO_TICK_ENABLED = booleanPreferencesKey("audio_tick_enabled")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val REFERENCE_CITY_INDEX = intPreferencesKey("reference_city_index")
    }

    val settingsFlow: Flow<ExactTimeSettings> = context.exactTimeDataStore.data.map { prefs ->
        ExactTimeSettings(
            use24HourFormat = prefs[Keys.USE_24_HOUR] ?: true,
            showCentiseconds = prefs[Keys.SHOW_CENTISECONDS] ?: true,
            clockStyleHybrid = prefs[Keys.CLOCK_STYLE_HYBRID] ?: true,
            tickCadence = TickCadence.fromSeconds(prefs[Keys.TICK_CADENCE_SECONDS] ?: 0),
            hapticTickEnabled = prefs[Keys.HAPTIC_TICK_ENABLED] ?: true,
            audioTickEnabled = prefs[Keys.AUDIO_TICK_ENABLED] ?: false,
            themeMode = AppThemeMode.fromName(prefs[Keys.THEME_MODE] ?: AppThemeMode.DARK.name),
            referenceCityIndex = prefs[Keys.REFERENCE_CITY_INDEX] ?: 0
        )
    }

    suspend fun setUse24HourFormat(enabled: Boolean) {
        context.exactTimeDataStore.edit { it[Keys.USE_24_HOUR] = enabled }
    }

    suspend fun setShowCentiseconds(enabled: Boolean) {
        context.exactTimeDataStore.edit { it[Keys.SHOW_CENTISECONDS] = enabled }
    }

    suspend fun setClockStyleHybrid(hybrid: Boolean) {
        context.exactTimeDataStore.edit { it[Keys.CLOCK_STYLE_HYBRID] = hybrid }
    }

    suspend fun setTickCadence(cadence: TickCadence) {
        context.exactTimeDataStore.edit { it[Keys.TICK_CADENCE_SECONDS] = cadence.seconds }
    }

    suspend fun setHapticTickEnabled(enabled: Boolean) {
        context.exactTimeDataStore.edit { it[Keys.HAPTIC_TICK_ENABLED] = enabled }
    }

    suspend fun setAudioTickEnabled(enabled: Boolean) {
        context.exactTimeDataStore.edit { it[Keys.AUDIO_TICK_ENABLED] = enabled }
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.exactTimeDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setReferenceCityIndex(index: Int) {
        context.exactTimeDataStore.edit { it[Keys.REFERENCE_CITY_INDEX] = index }
    }
}
