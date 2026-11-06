package de.tempotap.app

import android.app.Application
import android.content.Context
import de.tempotap.app.audio.MetronomeController
import de.tempotap.app.data.SessionRepository
import de.tempotap.app.data.SettingsRepository
import de.tempotap.app.util.AppLocale

class AppContainer(application: Application) {
    val settingsRepository = SettingsRepository(application)
    val sessionRepository = SessionRepository(application)
    val metronomeController = MetronomeController()
}

class TempoTapApplication : Application() {

    val container by lazy { AppContainer(this) }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(AppLocale.wrap(base))
    }
}
