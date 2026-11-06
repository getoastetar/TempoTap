package de.tempotap.app.audio

import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class MetronomeState(
    val bpm: Int = 120,
    val beatsPerBar: Int = 4,
    val isPlaying: Boolean = false,
    val currentBeat: Int = -1,
    val autoStopMinutes: Int? = null,
    val remainingSeconds: Int? = null,
)

/**
 * App-scoped controller so the metronome keeps playing while navigating between screens.
 */
class MetronomeController {

    private val engine = MetronomeEngine()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _state = MutableStateFlow(MetronomeState())
    val state: StateFlow<MetronomeState> = _state.asStateFlow()

    private var tickerJob: Job? = null
    private var autoStopJob: Job? = null

    @Volatile
    private var accentFirstBeat = true

    fun setAccentFirstBeat(enabled: Boolean) {
        accentFirstBeat = enabled
    }

    fun setBpm(bpm: Int) {
        val clamped = bpm.coerceIn(MIN_BPM, MAX_BPM)
        if (clamped == _state.value.bpm) return
        _state.update { it.copy(bpm = clamped) }
        if (_state.value.isPlaying) restart()
    }

    fun adjustBpm(delta: Int) = setBpm(_state.value.bpm + delta)

    fun setBeatsPerBar(beats: Int) {
        val clamped = beats.coerceIn(1, 12)
        if (clamped == _state.value.beatsPerBar) return
        _state.update { it.copy(beatsPerBar = clamped) }
        if (_state.value.isPlaying) restart()
    }

    fun toggle() {
        if (_state.value.isPlaying) stop() else start()
    }

    fun start() {
        val snapshot = _state.value
        if (snapshot.isPlaying) return
        engine.start(snapshot.bpm, snapshot.beatsPerBar, accentFirstBeat)
        _state.update {
            it.copy(
                isPlaying = true,
                currentBeat = 0,
                remainingSeconds = it.autoStopMinutes?.times(60),
            )
        }
        startTicker()
        startAutoStop()
    }

    fun stop() {
        tickerJob?.cancel()
        tickerJob = null
        autoStopJob?.cancel()
        autoStopJob = null
        engine.stop()
        _state.update {
            it.copy(isPlaying = false, currentBeat = -1, remainingSeconds = null)
        }
    }

    fun setAutoStop(minutes: Int?) {
        _state.update {
            it.copy(
                autoStopMinutes = minutes,
                remainingSeconds = if (minutes != null && it.isPlaying) minutes * 60 else null,
            )
        }
        if (_state.value.isPlaying) startAutoStop() else autoStopJob?.cancel()
    }

    private fun restart() {
        val snapshot = _state.value
        engine.stop()
        engine.start(snapshot.bpm, snapshot.beatsPerBar, accentFirstBeat)
        _state.update { it.copy(currentBeat = 0) }
        startTicker()
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            var beat = 0
            var nextBeat = SystemClock.elapsedRealtimeNanos()
            while (isActive && _state.value.isPlaying) {
                val bpm = _state.value.bpm
                val beatsPerBar = _state.value.beatsPerBar
                _state.update { it.copy(currentBeat = beat.coerceAtMost(beatsPerBar - 1)) }
                beat = (beat + 1) % beatsPerBar
                nextBeat += (60_000_000_000.0 / bpm).toLong()
                val delayMs = (nextBeat - SystemClock.elapsedRealtimeNanos()) / 1_000_000L
                if (delayMs > 0) delay(delayMs) else Thread.yield()
            }
        }
    }

    private fun startAutoStop() {
        autoStopJob?.cancel()
        val minutes = _state.value.autoStopMinutes ?: return
        autoStopJob = scope.launch {
            var remaining = minutes * 60
            while (remaining > 0 && isActive) {
                _state.update { it.copy(remainingSeconds = remaining) }
                delay(1_000)
                remaining--
            }
            if (isActive && _state.value.isPlaying) {
                stop()
            }
        }
    }

    companion object {
        const val MIN_BPM = 30
        const val MAX_BPM = 300
    }
}
