package de.tempotap.app.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import de.tempotap.app.data.MetronomeSound
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Sample-accurate metronome based on a streaming [AudioTrack]. A full bar is rendered
 * into memory and written in small chunks, which keeps the timing independent from
 * coroutine scheduling jitter and makes stopping responsive.
 */
class MetronomeEngine {

    private var thread: Thread? = null

    @Volatile
    private var track: AudioTrack? = null

    @Volatile
    private var running = false

    @Synchronized
    fun start(
        bpm: Int,
        beatsPerBar: Int,
        accentFirstBeat: Boolean,
        sound: MetronomeSound,
    ) {
        stopInternal()
        running = true
        thread = Thread(
            { render(bpm, beatsPerBar, accentFirstBeat, sound) },
            "TempoTap-Metronome",
        ).apply {
            priority = Thread.MAX_PRIORITY
            start()
        }
    }

    @Synchronized
    fun stop() {
        stopInternal()
    }

    private fun stopInternal() {
        running = false
        track?.let { current ->
            runCatching { current.pause() }
            runCatching { current.flush() }
        }
        thread?.join(400)
        track?.let { current -> runCatching { current.release() } }
        track = null
        thread = null
    }

    private fun render(
        bpm: Int,
        beatsPerBar: Int,
        accentFirstBeat: Boolean,
        sound: MetronomeSound,
    ) {
        val sampleRate = 44_100
        val minBuffer = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minBuffer <= 0) return

        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(max(minBuffer, sampleRate))
            .build()

        track = audioTrack
        if (!running) {
            runCatching { audioTrack.release() }
            return
        }

        try {
            audioTrack.play()
            val bar = renderBar(
                bpm = bpm,
                beatsPerBar = beatsPerBar,
                accentFirstBeat = accentFirstBeat,
                sound = sound,
                sampleRate = sampleRate,
            )

            var offset = 0
            while (running && !Thread.currentThread().isInterrupted) {
                val chunk = min(2_048, bar.size - offset)
                val written = audioTrack.write(bar, offset, chunk)
                if (written <= 0) break
                offset += written
                if (offset >= bar.size) offset = 0
            }
        } finally {
            runCatching { audioTrack.stop() }
            runCatching { audioTrack.release() }
            if (track === audioTrack) track = null
        }
    }

    private fun renderBar(
        bpm: Int,
        beatsPerBar: Int,
        accentFirstBeat: Boolean,
        sound: MetronomeSound,
        sampleRate: Int,
    ): ShortArray {
        val safeBpm = bpm.coerceIn(20, 300)
        val safeBeats = beatsPerBar.coerceIn(1, 12)
        val samplesPerBar = ((60.0 / safeBpm) * safeBeats * sampleRate).roundToInt()
        val bar = ShortArray(samplesPerBar)

        for (beat in 0 until safeBeats) {
            val start = ((beat * 60.0 / safeBpm) * sampleRate).roundToInt()
            val accent = accentFirstBeat && beat == 0
            val voice = clickVoice(sound, accent)
            val clickSamples = (sampleRate * voice.durationSeconds).roundToInt()

            for (index in 0 until clickSamples) {
                val position = start + index
                if (position >= bar.size) break
                val seconds = index.toDouble() / sampleRate
                val attack = 1.0 - exp(-seconds * voice.attackRate)
                val decay = exp(-seconds * voice.decayRate)
                val tone = voice.partials.sumOf { (ratio, gain) ->
                    sin(2.0 * PI * voice.frequency * ratio * seconds) * gain
                }
                val value = (tone * attack * decay * voice.amplitude * Short.MAX_VALUE)
                    .toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                bar[position] = (bar[position] + value).coerceIn(
                    Short.MIN_VALUE.toInt(),
                    Short.MAX_VALUE.toInt(),
                ).toShort()
            }
        }
        return bar
    }

    private fun clickVoice(sound: MetronomeSound, accent: Boolean): ClickVoice = when (sound) {
        MetronomeSound.CLASSIC -> ClickVoice(
            frequency = if (accent) 1_880.0 else 1_240.0,
            amplitude = if (accent) 0.62 else 0.48,
            attackRate = 1_400.0,
            decayRate = 72.0,
            durationSeconds = 0.045,
            partials = listOf(1.0 to 0.82, 1.5 to 0.14),
        )

        MetronomeSound.ANALOG -> ClickVoice(
            frequency = if (accent) 760.0 else 520.0,
            amplitude = if (accent) 0.66 else 0.52,
            attackRate = 900.0,
            decayRate = 46.0,
            durationSeconds = 0.06,
            partials = listOf(1.0 to 0.78, 2.0 to 0.30, 3.0 to 0.12),
        )

        MetronomeSound.DIGITAL -> ClickVoice(
            frequency = if (accent) 2_680.0 else 1_980.0,
            amplitude = if (accent) 0.58 else 0.44,
            attackRate = 2_200.0,
            decayRate = 125.0,
            durationSeconds = 0.03,
            partials = listOf(1.0 to 0.90, 2.0 to 0.08),
        )
    }

    private data class ClickVoice(
        val frequency: Double,
        val amplitude: Double,
        val attackRate: Double,
        val decayRate: Double,
        val durationSeconds: Double,
        val partials: List<Pair<Double, Double>>,
    )
}
