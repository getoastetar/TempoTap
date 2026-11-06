package de.tempotap.app.ui.metronome

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.tempotap.app.TempoTapApplication
import de.tempotap.app.audio.MetronomeState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MetronomeViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as TempoTapApplication).container
    private val controller = container.metronomeController

    val state: StateFlow<MetronomeState> = controller.state

    init {
        viewModelScope.launch {
            container.settingsRepository.settings.collect { settings ->
                controller.setSound(settings.metronomeSound)
            }
        }
    }

    fun toggle() = controller.toggle()

    fun start() = controller.start()

    fun stop() = controller.stop()

    fun setBpm(bpm: Int) = controller.setBpm(bpm)

    fun adjustBpm(delta: Int) = controller.adjustBpm(delta)

    fun startWithBpm(bpm: Int) {
        controller.setBpm(bpm)
        controller.start()
    }
}
