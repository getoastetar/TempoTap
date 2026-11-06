package de.tempotap.app.ui.settings

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.tempotap.app.BuildConfig
import de.tempotap.app.R
import de.tempotap.app.data.AppLanguage
import de.tempotap.app.data.AppSettings
import de.tempotap.app.data.MetronomeSound
import de.tempotap.app.data.ThemeMode
import de.tempotap.app.ui.components.IconBadge
import de.tempotap.app.ui.components.ScreenHeader
import de.tempotap.app.ui.components.SectionTitle
import de.tempotap.app.ui.components.SettingsCard
import de.tempotap.app.ui.components.SettingsDivider
import de.tempotap.app.ui.components.SettingsSwitchRow
import java.util.Locale
import kotlin.math.roundToLong

@Composable
fun SettingsScreen(
    settings: AppSettings,
    viewModel: SettingsViewModel,
    onAbout: () -> Unit,
) {
    val sessionCount by viewModel.sessionCount.collectAsStateWithLifecycle()
    var showClearDialog by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 760.dp).fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                ScreenHeader(
                    eyebrow = stringResource(R.string.settings_eyebrow),
                    title = stringResource(R.string.settings_title),
                    subtitle = stringResource(R.string.settings_subtitle),
                )
            }

            item { SectionTitle(stringResource(R.string.settings_appearance)) }
            item {
                SettingsCard {
                    ThemeRow(
                        selected = settings.themeMode,
                        onSelect = viewModel::setThemeMode,
                    )
                    SettingsDivider()
                    SettingsSwitchRow(
                        title = stringResource(R.string.settings_dynamic),
                        description = stringResource(R.string.settings_dynamic_desc),
                        checked = settings.dynamicColor,
                        onCheckedChange = viewModel::setDynamicColor,
                        enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                    )
                    SettingsDivider()
                    SettingsSwitchRow(
                        title = stringResource(R.string.settings_amoled),
                        description = stringResource(R.string.settings_amoled_desc),
                        checked = settings.amoledMode,
                        onCheckedChange = viewModel::setAmoledMode,
                    )
                }
            }

            item { SectionTitle(stringResource(R.string.settings_language_section)) }
            item {
                SettingsCard {
                    LanguageRow(
                        selected = settings.language,
                        onSelect = { language ->
                            viewModel.setLanguage(language)
                            context.findActivity()?.recreate()
                        },
                    )
                }
            }

            item { SectionTitle(stringResource(R.string.settings_behavior)) }
            item {
                SettingsCard {
                    SettingsSwitchRow(
                        title = stringResource(R.string.settings_haptics),
                        description = stringResource(R.string.settings_haptics_desc),
                        checked = settings.haptics,
                        onCheckedChange = viewModel::setHaptics,
                    )
                    SettingsDivider()
                    SettingsSwitchRow(
                        title = stringResource(R.string.settings_keep_awake),
                        description = stringResource(R.string.settings_keep_awake_desc),
                        checked = settings.keepScreenOn,
                        onCheckedChange = viewModel::setKeepScreenOn,
                    )
                    SettingsDivider()
                    TimeoutRow(
                        millis = settings.resetTimeoutMs,
                        onChange = viewModel::setResetTimeout,
                    )
                }
            }

            item { SectionTitle(stringResource(R.string.settings_metronome)) }
            item {
                SettingsCard {
                    MetronomeSoundRow(
                        selected = settings.metronomeSound,
                        onSelect = viewModel::setMetronomeSound,
                    )
                }
            }

            item { SectionTitle(stringResource(R.string.settings_data)) }
            item {
                SettingsCard {
                    HistoryRow(
                        sessionCount = sessionCount,
                        onClear = { showClearDialog = true },
                    )
                    SettingsDivider()
                    AboutRow(
                        version = BuildConfig.VERSION_NAME,
                        onClick = onAbout,
                    )
                }
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.about_made_with),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.history_clear_title)) },
            text = { Text(stringResource(R.string.history_clear_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearHistory()
                        showClearDialog = false
                    },
                ) {
                    Text(
                        text = stringResource(R.string.action_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun LanguageRow(selected: AppLanguage, onSelect: (AppLanguage) -> Unit) {
    Column(modifier = Modifier.padding(20.dp)) {
        Text(
            text = stringResource(R.string.settings_language),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.settings_language_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppLanguage.entries.forEach { language ->
                FilterChip(
                    selected = selected == language,
                    onClick = {
                        if (selected != language) onSelect(language)
                    },
                    label = {
                        Text(
                            stringResource(
                                when (language) {
                                    AppLanguage.GERMAN -> R.string.settings_language_german
                                    AppLanguage.ENGLISH -> R.string.settings_language_english
                                },
                            ),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun ThemeRow(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    Column(modifier = Modifier.padding(20.dp)) {
        Text(
            text = stringResource(R.string.settings_theme),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ThemeMode.entries.forEach { mode ->
                FilterChip(
                    selected = selected == mode,
                    onClick = { onSelect(mode) },
                    label = {
                        Text(
                            stringResource(
                                when (mode) {
                                    ThemeMode.SYSTEM -> R.string.settings_theme_system
                                    ThemeMode.LIGHT -> R.string.settings_theme_light
                                    ThemeMode.DARK -> R.string.settings_theme_dark
                                },
                            ),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun MetronomeSoundRow(selected: MetronomeSound, onSelect: (MetronomeSound) -> Unit) {
    Column(modifier = Modifier.padding(20.dp)) {
        Text(
            text = stringResource(R.string.settings_metronome_sound),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.settings_metronome_sound_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetronomeSound.entries.forEach { sound ->
                FilterChip(
                    selected = selected == sound,
                    onClick = { onSelect(sound) },
                    label = {
                        Text(
                            stringResource(
                                when (sound) {
                                    MetronomeSound.CLASSIC -> R.string.settings_sound_classic
                                    MetronomeSound.ANALOG -> R.string.settings_sound_analog
                                    MetronomeSound.DIGITAL -> R.string.settings_sound_digital
                                },
                            ),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun TimeoutRow(millis: Long, onChange: (Long) -> Unit) {
    var value by remember(millis) { mutableFloatStateOf(millis / 1_000f) }
    val formatted = String.format(Locale.getDefault(), "%.1f", value)

    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.settings_reset_timeout),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.settings_reset_timeout_value, formatted),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = stringResource(R.string.tempo_tip_3),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Slider(
            value = value,
            onValueChange = { value = it },
            onValueChangeFinished = { onChange((value * 1_000f).roundToLong()) },
            valueRange = 1f..5f,
            steps = 7,
        )
    }
}

@Composable
private fun HistoryRow(sessionCount: Int, onClear: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(
            iconRes = R.drawable.ic_timeline,
            modifier = Modifier.size(42.dp),
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.settings_history),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(
                    R.string.settings_history_desc,
                    pluralStringResource(
                        R.plurals.session_count,
                        sessionCount,
                        sessionCount,
                    ),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onClear, enabled = sessionCount > 0) {
            Text(stringResource(R.string.action_delete))
        }
    }
}

@Composable
private fun AboutRow(version: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(
            iconRes = R.drawable.ic_info,
            modifier = Modifier.size(42.dp),
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.settings_about_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.settings_version, version),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
    }
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
