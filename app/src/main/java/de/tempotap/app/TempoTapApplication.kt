package de.tempotap.app

import android.app.Application
import de.tempotap.app.audio.ClickPlayer
import de.tempotap.app.audio.MetronomeController
import de.tempotap.app.data.SessionRepository
import de.tempotap.app.data.SettingsRepository

class AppContainer(application: Application) {
    val settingsRepository = SettingsRepository(application)
    val sessionRepository = SessionRepository(application)
    val clickPlayer = ClickPlayer(application)
    val metronomeController = MetronomeController()
}

class TempoTapApplication : Application() {

    val container by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        container.clickPlayer.preload()
    }
}
