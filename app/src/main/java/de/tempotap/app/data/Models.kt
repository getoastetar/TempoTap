package de.tempotap.app.data

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

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val haptics: Boolean = true,
    val tapSound: Boolean = false,
    val keepScreenOn: Boolean = false,
    val resetTimeoutMs: Long = 2_500L,
    val accentFirstBeat: Boolean = true,
    val defaultBeatsPerBar: Int = 4,
)
