package de.tempotap.app.audio

import android.os.SystemClock
import de.tempotap.app.data.MetronomeSound
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

    @Volatile
    private var sound = MetronomeSound.CLASSIC

    fun setSound(sound: MetronomeSound) {
        if (this.sound == sound) return
        this.sound = sound
        if (_state.value.isPlaying) restart()
    }

    fun setBpm(bpm: Int) {
        val clamped = bpm.coerceIn(MIN_BPM, MAX_BPM)
        if (clamped == _state.value.bpm) return
        _state.update { it.copy(bpm = clamped) }
        if (_state.value.isPlaying) restart()
    }

    fun adjustBpm(delta: Int) = setBpm(_state.value.bpm + delta)

    fun toggle() {
        if (_state.value.isPlaying) stop() else start()
    }

    fun start() {
        val snapshot = _state.value
        if (snapshot.isPlaying) return
        engine.start(snapshot.bpm, BEATS_PER_BAR, accentFirstBeat = true, sound = sound)
        _state.update { it.copy(isPlaying = true, currentBeat = 0) }
        startTicker()
    }

    fun stop() {
        tickerJob?.cancel()
        tickerJob = null
        engine.stop()
        _state.update { it.copy(isPlaying = false, currentBeat = -1) }
    }

    private fun restart() {
        val snapshot = _state.value
        engine.stop()
        engine.start(snapshot.bpm, BEATS_PER_BAR, accentFirstBeat = true, sound = sound)
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
                _state.update { it.copy(currentBeat = beat.coerceAtMost(BEATS_PER_BAR - 1)) }
                beat = (beat + 1) % BEATS_PER_BAR
                nextBeat += (60_000_000_000.0 / bpm).toLong()
                val delayMs = (nextBeat - SystemClock.elapsedRealtimeNanos()) / 1_000_000L
                if (delayMs > 0) delay(delayMs) else Thread.yield()
            }
        }
    }

    companion object {
        const val MIN_BPM = 30
        const val MAX_BPM = 300
        const val BEATS_PER_BAR = 4
    }
}
