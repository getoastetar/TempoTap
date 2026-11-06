package de.tempotap.app.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.tempotap.app.TempoTapApplication
import de.tempotap.app.data.AppLanguage
import de.tempotap.app.data.AppSettings
import de.tempotap.app.data.MetronomeSound
import de.tempotap.app.data.ThemeMode
import de.tempotap.app.util.AppLocale
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as TempoTapApplication).container
    private val repository = container.settingsRepository

    val settings: StateFlow<AppSettings> = repository.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = AppSettings(language = AppLocale.read(application)),
        )

    val sessionCount: StateFlow<Int> = container.sessionRepository.sessions
        .map { it.size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0,
        )

    fun setThemeMode(mode: ThemeMode) = update { repository.setThemeMode(mode) }

    fun setDynamicColor(enabled: Boolean) = update { repository.setDynamicColor(enabled) }

    fun setAmoledMode(enabled: Boolean) = update { repository.setAmoledMode(enabled) }

    fun setHaptics(enabled: Boolean) = update { repository.setHaptics(enabled) }

    fun setKeepScreenOn(enabled: Boolean) = update { repository.setKeepScreenOn(enabled) }

    fun setResetTimeout(millis: Long) = update { repository.setResetTimeout(millis) }

    fun setMetronomeSound(sound: MetronomeSound) = update { repository.setMetronomeSound(sound) }

    fun setLanguage(language: AppLanguage) = repository.setLanguage(language)

    fun clearHistory() {
        viewModelScope.launch { container.sessionRepository.clear() }
    }

    private fun update(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
