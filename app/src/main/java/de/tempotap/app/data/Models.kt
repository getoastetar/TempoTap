package de.tempotap.app.data

import java.util.Locale
import kotlinx.serialization.Serializable

@Serializable
data class SessionRecord(
    val id: Long,
    val bpm: Double,
    val taps: Int,
    val durationMs: Long,
    val createdAt: Long,
)

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

enum class MetronomeSound {
    CLASSIC,
    ANALOG,
    DIGITAL,
}

enum class AppLanguage(val languageTag: String) {
    GERMAN("de"),
    ENGLISH("en");

    companion object {
        fun fromTag(tag: String?): AppLanguage? =
            entries.firstOrNull { it.languageTag == tag }

        fun systemDefault(): AppLanguage =
            if (Locale.getDefault().language == "de") GERMAN else ENGLISH
    }
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val amoledMode: Boolean = false,
    val haptics: Boolean = true,
    val keepScreenOn: Boolean = false,
    val resetTimeoutMs: Long = 2_500L,
    val accentFirstBeat: Boolean = true,
    val defaultBeatsPerBar: Int = 4,
    val metronomeSound: MetronomeSound = MetronomeSound.CLASSIC,
    val language: AppLanguage = AppLanguage.systemDefault(),
)
