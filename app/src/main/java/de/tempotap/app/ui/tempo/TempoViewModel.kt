package de.tempotap.app.ui.tempo

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.tempotap.app.TempoTapApplication
import de.tempotap.app.data.SessionRecord
import de.tempotap.app.tempo.Stability
import de.tempotap.app.tempo.TapSnapshot
import de.tempotap.app.tempo.TapTempoEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TempoUiState(
    val bpm: Double? = null,
    val tapCount: Int = 0,
    val lastIntervalMs: Double? = null,
    val variationPercent: Double? = null,
    val stability: Stability = Stability.NONE,
    val durationMs: Long = 0L,
    val saved: Boolean = false,
)

class TempoViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as TempoTapApplication).container
    private val engine = TapTempoEngine()

    private val _state = MutableStateFlow(TempoUiState())
    val state: StateFlow<TempoUiState> = _state.asStateFlow()

    private var finalizeJob: Job? = null
    private var lastTapNanos = 0L

    init {
        viewModelScope.launch {
            container.settingsRepository.settings.collect { settings ->
                engine.resetTimeoutMs = settings.resetTimeoutMs
            }
        }
    }

    fun tap() {
        val now = SystemClock.elapsedRealtimeNanos()
        if (lastTapNanos != 0L && now - lastTapNanos > engine.resetTimeoutMs * 1_000_000L) {
            // The pause started a new session; keep the previous one before the engine clears it.
            saveCurrentSession(showState = false)
        }
        lastTapNanos = now

        val snapshot = engine.tap(now)
        _state.update { previous ->
            snapshot.toUiState(saved = if (snapshot.tapCount <= 1) false else previous.saved)
        }
        scheduleFinalize()
    }

    fun reset() {
        finalizeJob?.cancel()
        finalizeJob = null
        saveCurrentSession(showState = false)
        engine.reset()
        lastTapNanos = 0L
        _state.value = TempoUiState()
    }

    private fun scheduleFinalize() {
        finalizeJob?.cancel()
        finalizeJob = viewModelScope.launch {
            delay(engine.resetTimeoutMs + 350L)
            saveCurrentSession(showState = true)
        }
    }

    private fun saveCurrentSession(showState: Boolean) {
        if (_state.value.saved) return
        val snapshot = engine.snapshot()
        val bpm = snapshot.bpm ?: return
        if (snapshot.tapCount < 3 || snapshot.durationMs < 600L) return

        val now = System.currentTimeMillis()
        val record = SessionRecord(
            id = now,
            bpm = bpm,
            taps = snapshot.tapCount,
            durationMs = snapshot.durationMs,
            createdAt = now,
        )
        val expectedTapCount = snapshot.tapCount

        viewModelScope.launch {
            container.sessionRepository.add(record)
            if (showState && engine.snapshot().tapCount == expectedTapCount) {
                _state.update { it.copy(saved = true) }
            }
        }
    }

    private fun TapSnapshot.toUiState(saved: Boolean) = TempoUiState(
        bpm = bpm,
        tapCount = tapCount,
        lastIntervalMs = lastIntervalMs,
        variationPercent = variationPercent,
        stability = stability,
        durationMs = durationMs,
        saved = saved,
    )
}
