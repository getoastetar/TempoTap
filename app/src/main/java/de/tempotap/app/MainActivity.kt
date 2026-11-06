package de.tempotap.app

import android.content.Context
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import de.tempotap.app.ui.TempoTapApp
import de.tempotap.app.ui.settings.SettingsViewModel
import de.tempotap.app.ui.theme.TempoTapTheme
import de.tempotap.app.ui.theme.resolveDarkTheme
import de.tempotap.app.util.AppLocale
import androidx.compose.ui.platform.LocalView

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocale.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val settings by settingsViewModel.settings.collectAsState()
            val darkTheme = resolveDarkTheme(settings.themeMode, isSystemInDarkTheme())
            val view = LocalView.current

            LaunchedEffect(darkTheme) {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }

            DisposableEffect(settings.keepScreenOn) {
                if (settings.keepScreenOn) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
                onDispose {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            TempoTapTheme(
                themeMode = settings.themeMode,
                dynamicColor = settings.dynamicColor,
                amoledMode = settings.amoledMode,
            ) {
                TempoTapApp(
                    settingsViewModel = settingsViewModel,
                )
            }
        }
    }
}
