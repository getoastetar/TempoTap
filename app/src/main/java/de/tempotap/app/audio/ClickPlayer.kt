package de.tempotap.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Low-latency tap click. The sound is synthesized once into a small WAV file in the
 * cache directory and loaded into SoundPool, so no binary asset is shipped.
 */
class ClickPlayer(context: Context) {

    private val appContext = context.applicationContext

    private val soundPool = SoundPool.Builder()
        .setMaxStreams(3)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private var soundId = 0

    fun preload() {
        if (soundId != 0) return
        soundId = runCatching {
            val file = File(appContext.cacheDir, "tap_click.wav")
            if (!file.exists()) {
                writeWav(file, buildClick(sampleRate = 44_100, frequency = 1_750.0, durationMs = 42))
            }
            soundPool.load(file.absolutePath, 1)
        }.getOrDefault(0)
    }

    fun playTap(enabled: Boolean) {
        if (!enabled) return
        if (soundId == 0) preload()
        if (soundId != 0) {
            soundPool.play(soundId, 0.55f, 0.55f, 1, 0, 1f)
        }
    }

    private fun buildClick(sampleRate: Int, frequency: Double, durationMs: Int): ShortArray {
        val count = sampleRate * durationMs / 1_000
        return ShortArray(count) { index ->
            val seconds = index.toDouble() / sampleRate
            val attack = 1.0 - exp(-seconds * 1_200.0)
            val decay = exp(-seconds * 65.0)
            val tone = sin(2.0 * PI * frequency * seconds) * 0.72 +
                sin(2.0 * PI * frequency * 2.0 * seconds) * 0.18
            (tone * attack * decay * Short.MAX_VALUE * 0.62)
                .toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
        }
    }

    private fun writeWav(file: File, samples: ShortArray, sampleRate: Int = 44_100) {
        val dataSize = samples.size * 2
        val out = ByteArrayOutputStream(44 + dataSize)

        fun writeAscii(value: String) = out.write(value.toByteArray(Charsets.US_ASCII))
        fun writeInt(value: Int) {
            out.write(value and 0xFF)
            out.write((value shr 8) and 0xFF)
            out.write((value shr 16) and 0xFF)
            out.write((value shr 24) and 0xFF)
        }
        fun writeShort(value: Int) {
            out.write(value and 0xFF)
            out.write((value shr 8) and 0xFF)
        }

        writeAscii("RIFF")
        writeInt(36 + dataSize)
        writeAscii("WAVE")
        writeAscii("fmt ")
        writeInt(16)
        writeShort(1)
        writeShort(1)
        writeInt(sampleRate)
        writeInt(sampleRate * 2)
        writeShort(2)
        writeShort(16)
        writeAscii("data")
        writeInt(dataSize)
        samples.forEach { writeShort(it.toInt()) }

        file.writeBytes(out.toByteArray())
    }
}
