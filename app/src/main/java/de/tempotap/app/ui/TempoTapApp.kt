package de.tempotap.app.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import de.tempotap.app.R
import de.tempotap.app.audio.MetronomeState
import de.tempotap.app.ui.about.AboutScreen
import de.tempotap.app.ui.history.HistoryScreen
import de.tempotap.app.ui.metronome.MetronomeScreen
import de.tempotap.app.ui.metronome.MetronomeViewModel
import de.tempotap.app.ui.settings.SettingsScreen
import de.tempotap.app.ui.settings.SettingsViewModel
import de.tempotap.app.ui.tempo.TempoScreen

private const val ABOUT_ROUTE = "about"

private enum class AppDestination(
    val route: String,
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
) {
    Tempo("tempo", R.string.nav_tempo, R.drawable.ic_tap),
    Metronome("metronome", R.string.nav_metronome, R.drawable.ic_metronome),
    History("history", R.string.nav_history, R.drawable.ic_timeline),
    Settings("settings", R.string.nav_settings, R.drawable.ic_settings),
}

@Composable
fun TempoTapApp(
    settingsViewModel: SettingsViewModel,
) {
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val metronomeViewModel: MetronomeViewModel = viewModel()
    val metronomeState by metronomeViewModel.state.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: AppDestination.Tempo.route
    val showNavigation = AppDestination.entries.any { it.route == currentRoute }

    val navigateTopLevel: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    androidx.compose.foundation.layout.BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val wide = maxWidth >= 840.dp

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (!wide && showNavigation) {
                    AppBottomBar(
                        currentRoute = currentRoute,
                        metronomeState = metronomeState,
                        onNavigate = navigateTopLevel,
                    )
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (wide && showNavigation) {
                    AppNavigationRail(
                        currentRoute = currentRoute,
                        metronomeState = metronomeState,
                        onNavigate = navigateTopLevel,
                    )
                }

                NavHost(
                    navController = navController,
                    startDestination = AppDestination.Tempo.route,
                    modifier = Modifier.weight(1f),
                ) {
                    composable(AppDestination.Tempo.route) {
                        TempoScreen(
                            settings = settings,
                            metronomeState = metronomeState,
                            onOpenMetronome = {
                                navigateTopLevel(AppDestination.Metronome.route)
                            },
                            onStartMetronome = { bpm ->
                                metronomeViewModel.startWithBpm(bpm)
                                navigateTopLevel(AppDestination.Metronome.route)
                            },
                        )
                    }
                    composable(AppDestination.Metronome.route) {
                        MetronomeScreen(
                            settings = settings,
                            onOpenTempo = {
                                navigateTopLevel(AppDestination.Tempo.route)
                            },
                            viewModel = metronomeViewModel,
                        )
                    }
                    composable(AppDestination.History.route) {
                        HistoryScreen(
                            onGoToTempo = {
                                navigateTopLevel(AppDestination.Tempo.route)
                            },
                        )
                    }
                    composable(AppDestination.Settings.route) {
                        SettingsScreen(
                            settings = settings,
                            viewModel = settingsViewModel,
                            onAbout = { navController.navigate(ABOUT_ROUTE) },
                        )
                    }
                    composable(ABOUT_ROUTE) {
                        AboutScreen(onBack = { navController.popBackStack() })
                    }
                }
            }
        }
    }
}

@Composable
private fun AppBottomBar(
    currentRoute: String,
    metronomeState: MetronomeState,
    onNavigate: (String) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        AppDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = currentRoute == destination.route,
                onClick = {
                    if (currentRoute != destination.route) onNavigate(destination.route)
                },
                icon = {
                    DestinationIcon(
                        destination = destination,
                        metronomeState = metronomeState,
                    )
                },
                label = { Text(stringResource(destination.labelRes)) },
            )
        }
    }
}

@Composable
private fun AppNavigationRail(
    currentRoute: String,
    metronomeState: MetronomeState,
    onNavigate: (String) -> Unit,
) {
    NavigationRail(
        modifier = Modifier.fillMaxHeight(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        windowInsets = WindowInsets(0, 0, 0, 0),
        header = {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.ic_metronome),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        },
    ) {
        Spacer(Modifier.height(16.dp))
        AppDestination.entries.forEach { destination ->
            NavigationRailItem(
                selected = currentRoute == destination.route,
                onClick = {
                    if (currentRoute != destination.route) onNavigate(destination.route)
                },
                icon = {
                    DestinationIcon(
                        destination = destination,
                        metronomeState = metronomeState,
                    )
                },
                label = { Text(stringResource(destination.labelRes)) },
            )
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun DestinationIcon(
    destination: AppDestination,
    metronomeState: MetronomeState,
) {
    if (destination == AppDestination.Metronome && metronomeState.isPlaying) {
        BadgedBox(
            badge = {
                Badge(containerColor = MaterialTheme.colorScheme.primary)
            },
        ) {
            Icon(
                painter = painterResource(destination.iconRes),
                contentDescription = null,
            )
        }
    } else {
        Icon(
            painter = painterResource(destination.iconRes),
            contentDescription = null,
        )
    }
}
