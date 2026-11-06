package de.tempotap.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.sessionsDataStore: DataStore<Preferences> by preferencesDataStore(name = "sessions")

class SessionRepository(context: Context) {

    private val dataStore = context.applicationContext.sessionsDataStore
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val sessions: Flow<List<SessionRecord>> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { prefs -> decode(prefs[Keys.SESSIONS]) }

    suspend fun add(record: SessionRecord) {
        dataStore.edit { prefs ->
            val current = decode(prefs[Keys.SESSIONS])
            val updated = (listOf(record) + current).take(MAX_SESSIONS)
            prefs[Keys.SESSIONS] = json.encodeToString(updated)
        }
    }

    suspend fun remove(id: Long) {
        dataStore.edit { prefs ->
            val updated = decode(prefs[Keys.SESSIONS]).filterNot { it.id == id }
            prefs[Keys.SESSIONS] = json.encodeToString(updated)
        }
    }

    suspend fun clear() {
        dataStore.edit { prefs -> prefs.remove(Keys.SESSIONS) }
    }

    private fun decode(raw: String?): List<SessionRecord> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<SessionRecord>>(raw) }.getOrDefault(emptyList())
    }

    private object Keys {
        val SESSIONS = stringPreferencesKey("sessions_json")
    }

    private companion object {
        const val MAX_SESSIONS = 100
    }
}
