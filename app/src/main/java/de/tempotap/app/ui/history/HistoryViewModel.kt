package de.tempotap.app.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.tempotap.app.TempoTapApplication
import de.tempotap.app.data.SessionRecord
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as TempoTapApplication).container

    val sessions: StateFlow<List<SessionRecord>> = container.sessionRepository.sessions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun delete(id: Long) {
        viewModelScope.launch { container.sessionRepository.remove(id) }
    }

    fun clear() {
        viewModelScope.launch { container.sessionRepository.clear() }
    }
}
