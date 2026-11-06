package de.tempotap.app.ui.metronome

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.tempotap.app.R
import de.tempotap.app.audio.MetronomeController
import de.tempotap.app.audio.MetronomeState
import de.tempotap.app.data.AppSettings
import de.tempotap.app.ui.components.SectionTitle
import kotlin.math.roundToInt

@Composable
fun MetronomeScreen(
    settings: AppSettings,
    onOpenTempo: () -> Unit,
    viewModel: MetronomeViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val view = LocalView.current

    fun tick() {
        if (settings.haptics) {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 760.dp).fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                TempoPanel(
                    state = state,
                    onBpmChange = viewModel::setBpm,
                    onAdjust = {
                        viewModel.adjustBpm(it)
                        tick()
                    },
                )
            }
            item {
                Button(
                    onClick = {
                        viewModel.toggle()
                        tick()
                    },
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Icon(
                        painter = painterResource(
                            if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(26.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = stringResource(
                            if (state.isPlaying) R.string.metronome_stop
                            else R.string.metronome_start,
                        ),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
            item {
                PresetRow(
                    currentBpm = state.bpm,
                    onSelect = viewModel::setBpm,
                )
            }
            item {
                HintCard(onOpenTempo = onOpenTempo)
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun TempoPanel(
    state: MetronomeState,
    onBpmChange: (Int) -> Unit,
    onAdjust: (Int) -> Unit,
) {
    var sliderValue by remember { mutableFloatStateOf(state.bpm.toFloat()) }
    var dragging by remember { mutableStateOf(false) }

    LaunchedEffect(state.bpm, dragging) {
        if (!dragging) sliderValue = state.bpm.toFloat()
    }

    val displayedBpm = if (dragging) sliderValue.roundToInt() else state.bpm

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = stringResource(R.string.metronome_tempo),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = displayedBpm.toString(),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 76.sp,
                        lineHeight = 82.sp,
                        letterSpacing = (-3).sp,
                        fontFeatureSettings = "tnum",
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.bpm_unit),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }

            Spacer(Modifier.height(18.dp))
            BeatDots(state)
            Spacer(Modifier.height(22.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                FilledTonalIconButton(
                    onClick = { onAdjust(-1) },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_remove),
                        contentDescription = stringResource(R.string.metronome_decrease),
                        modifier = Modifier.size(20.dp),
                    )
                }
                Slider(
                    value = sliderValue,
                    onValueChange = {
                        dragging = true
                        sliderValue = it
                    },
                    onValueChangeFinished = {
                        dragging = false
                        onBpmChange(sliderValue.roundToInt())
                    },
                    valueRange = MetronomeController.MIN_BPM.toFloat()..
                        MetronomeController.MAX_BPM.toFloat(),
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                )
                FilledTonalIconButton(
                    onClick = { onAdjust(1) },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_add),
                        contentDescription = stringResource(R.string.metronome_increase),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun BeatDots(state: MetronomeState) {
    val activeColor = MaterialTheme.colorScheme.primary
    val accentColor = MaterialTheme.colorScheme.tertiary
    val inactiveColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val activeContent = MaterialTheme.colorScheme.onPrimary
    val accentContent = MaterialTheme.colorScheme.onTertiary
    val inactiveContent = MaterialTheme.colorScheme.onSurfaceVariant
    val beatLabel = stringResource(
        R.string.metronome_beat_indicator,
        (state.currentBeat + 1).coerceAtLeast(0),
        state.beatsPerBar,
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = beatLabel },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(state.beatsPerBar) { index ->
            val active = state.isPlaying && state.currentBeat == index
            val accent = index == 0
            val color by animateColorAsState(
                targetValue = when {
                    active && accent -> accentColor
                    active -> activeColor
                    else -> inactiveColor
                },
                label = "beatColor",
            )
            val contentColor by animateColorAsState(
                targetValue = when {
                    active && accent -> accentContent
                    active -> activeContent
                    else -> inactiveContent
                },
                label = "beatContentColor",
            )
            val scale by animateFloatAsState(
                targetValue = if (active) 1.05f else 1f,
                label = "beatScale",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(RoundedCornerShape(18.dp))
                    .background(color),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = (index + 1).toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    color = contentColor,
                )
            }
        }
    }
}

@Composable
private fun PresetRow(currentBpm: Int, onSelect: (Int) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionTitle(stringResource(R.string.metronome_presets))
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(60, 90, 120, 140, 175).forEach { preset ->
                FilterChip(
                    selected = currentBpm == preset,
                    onClick = { onSelect(preset) },
                    label = { Text(preset.toString()) },
                )
            }
        }
    }
}

@Composable
private fun HintCard(onOpenTempo: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_tap),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = stringResource(R.string.metronome_tap_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onOpenTempo) {
                Text(stringResource(R.string.metronome_tap_hint_action))
            }
        }
    }
}
