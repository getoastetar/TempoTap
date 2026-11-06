package de.tempotap.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import de.tempotap.app.util.AppLocale
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(context: Context) {

    private val appContext = context.applicationContext
    private val dataStore = appContext.settingsDataStore
    private val languageFlow = MutableStateFlow(AppLocale.read(appContext))

    val settings: Flow<AppSettings> = combine(
        dataStore.data.catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        },
        languageFlow,
    ) { prefs, language ->
        AppSettings(
            themeMode = prefs[Keys.THEME]?.let { value ->
                runCatching { ThemeMode.valueOf(value) }.getOrNull()
            } ?: ThemeMode.SYSTEM,
            dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: true,
            amoledMode = prefs[Keys.AMOLED] ?: false,
            haptics = prefs[Keys.HAPTICS] ?: true,
            keepScreenOn = prefs[Keys.KEEP_SCREEN_ON] ?: false,
            resetTimeoutMs = prefs[Keys.RESET_TIMEOUT] ?: 2_500L,
            accentFirstBeat = prefs[Keys.ACCENT] ?: true,
            defaultBeatsPerBar = prefs[Keys.BEATS_PER_BAR] ?: 4,
            metronomeSound = prefs[Keys.METRONOME_SOUND]?.let { value ->
                runCatching { MetronomeSound.valueOf(value) }.getOrNull()
            } ?: MetronomeSound.CLASSIC,
            language = language,
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) = edit { it[Keys.THEME] = mode.name }

    suspend fun setDynamicColor(enabled: Boolean) = edit { it[Keys.DYNAMIC_COLOR] = enabled }

    suspend fun setAmoledMode(enabled: Boolean) = edit { it[Keys.AMOLED] = enabled }

    suspend fun setHaptics(enabled: Boolean) = edit { it[Keys.HAPTICS] = enabled }

    suspend fun setKeepScreenOn(enabled: Boolean) = edit { it[Keys.KEEP_SCREEN_ON] = enabled }

    suspend fun setResetTimeout(millis: Long) = edit { it[Keys.RESET_TIMEOUT] = millis }

    suspend fun setAccentFirstBeat(enabled: Boolean) = edit { it[Keys.ACCENT] = enabled }

    suspend fun setDefaultBeatsPerBar(beats: Int) = edit { it[Keys.BEATS_PER_BAR] = beats }

    suspend fun setMetronomeSound(sound: MetronomeSound) = edit { it[Keys.METRONOME_SOUND] = sound.name }

    fun setLanguage(language: AppLanguage) {
        AppLocale.write(appContext, language)
        languageFlow.value = language
    }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        dataStore.edit(block)
    }

    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val AMOLED = booleanPreferencesKey("amoled_mode")
        val HAPTICS = booleanPreferencesKey("haptics")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val RESET_TIMEOUT = longPreferencesKey("reset_timeout")
        val ACCENT = booleanPreferencesKey("accent_first_beat")
        val BEATS_PER_BAR = intPreferencesKey("beats_per_bar")
        val METRONOME_SOUND = stringPreferencesKey("metronome_sound")
    }
}
