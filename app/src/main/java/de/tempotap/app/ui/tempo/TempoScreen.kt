package de.tempotap.app.ui.tempo

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.tempotap.app.R
import de.tempotap.app.audio.MetronomeState
import de.tempotap.app.data.AppSettings
import de.tempotap.app.tempo.Stability
import de.tempotap.app.ui.components.IconBadge
import de.tempotap.app.ui.components.StatTile
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
fun TempoScreen(
    settings: AppSettings,
    metronomeState: MetronomeState,
    onOpenMetronome: () -> Unit,
    onStartMetronome: (Int) -> Unit,
    viewModel: TempoViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val view = LocalView.current

    val onTap: () -> Unit = {
        viewModel.tap()
        if (settings.haptics) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceContainerLow,
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            ),
    ) {
        val wide = maxWidth >= 780.dp && maxHeight >= 520.dp
        val compactTapHeight = (maxHeight - 220.dp).coerceAtLeast(260.dp)
        if (wide) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Column(modifier = Modifier.weight(1.15f).fillMaxHeight()) {
                    TapHero(
                        state = state,
                        onTap = onTap,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(0.85f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    StatsRow(state)
                    MetronomeCard(
                        state = state,
                        metronomeState = metronomeState,
                        onOpen = onOpenMetronome,
                        onStart = onStartMetronome,
                    )
                    TipsCard()
                    Spacer(Modifier.height(8.dp))
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                TapHero(
                    state = state,
                    onTap = onTap,
                    modifier = Modifier.fillMaxWidth().height(compactTapHeight),
                )
                StatsRow(state)
                MetronomeCard(
                    state = state,
                    metronomeState = metronomeState,
                    onOpen = onOpenMetronome,
                    onStart = onStartMetronome,
                )
                TipsCard()
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun TapHero(
    state: TempoUiState,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.extraLarge
    val bpmValue = state.bpm?.roundToInt()?.coerceAtLeast(0) ?: 0
    val currentOnTap by rememberUpdatedState(onTap)
    val sessionEnded = state.saved
    val blockStart = if (sessionEnded) Color(0xFF737373) else colors.primary
    val blockEnd = if (sessionEnded) Color(0xFF4A4A4A) else colors.tertiary
    val contentColor = if (sessionEnded) Color.White else colors.onPrimary

    val ripple = remember { androidx.compose.animation.core.Animatable(1f) }
    val scope = rememberCoroutineScope()
    var hasRipple by remember { mutableStateOf(false) }
    var rippleCenter by remember { mutableStateOf(Offset.Zero) }
    var pressed by remember { mutableStateOf(false) }
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = 0.6f,
            stiffness = 600f,
        ),
        label = "tapScale",
    )

    val statusText = when {
        state.bpm == null -> stringResource(R.string.tempo_idle_hint)
        else -> stringResource(R.string.tempo_stable_hint)
    }
    val stabilityLabel = when (state.stability) {
        Stability.NONE -> stringResource(R.string.tempo_stability_none)
        Stability.LOW -> stringResource(R.string.tempo_stability_low)
        Stability.MEDIUM -> stringResource(R.string.tempo_stability_medium)
        Stability.HIGH -> stringResource(R.string.tempo_stability_high)
    }
    val accessibilityDescription = stringResource(R.string.a11y_tap_pad) + ". " +
        stringResource(R.string.a11y_bpm, bpmValue)
    val tapLabel = stringResource(R.string.tempo_tap_action)

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(blockStart, blockEnd),
                ),
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    },
                    onTap = { offset ->
                        rippleCenter = offset
                        hasRipple = true
                        scope.launch {
                            ripple.stop()
                            ripple.snapTo(0f)
                            ripple.animateTo(
                                targetValue = 1f,
                                animationSpec = androidx.compose.animation.core.tween(
                                    durationMillis = 520,
                                    easing = androidx.compose.animation.core.LinearOutSlowInEasing,
                                ),
                            )
                        }
                        currentOnTap()
                    },
                )
            }
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = accessibilityDescription
                onClick(label = tapLabel) {
                    currentOnTap()
                    true
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (hasRipple && ripple.value < 1f) {
                val progress = ripple.value
                drawCircle(
                    color = contentColor.copy(alpha = (1f - progress) * 0.20f),
                    radius = size.minDimension * 0.14f + progress * size.maxDimension * 0.72f,
                    center = rippleCenter,
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            contentColor.copy(
                                alpha = if (state.stability == Stability.NONE) 0.35f else 1f,
                            ),
                        ),
                )
                Text(
                    text = stringResource(R.string.bpm_unit),
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.85f),
                )
            }
            Text(
                text = bpmValue.toString(),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 108.sp,
                    lineHeight = 112.sp,
                    letterSpacing = (-4).sp,
                    fontFeatureSettings = "tnum",
                ),
                color = contentColor,
            )
            Text(
                text = statusText,
                style = MaterialTheme.typography.titleMedium,
                color = contentColor.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = stabilityLabel,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor.copy(alpha = 0.7f),
            )
            Spacer(Modifier.height(20.dp))
            Surface(
                shape = CircleShape,
                color = contentColor.copy(alpha = 0.14f),
                contentColor = contentColor,
                modifier = Modifier.size(64.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.ic_tap),
                        contentDescription = null,
                        modifier = Modifier.size(30.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsRow(state: TempoUiState) {
    val interval = state.lastIntervalMs?.let { millis ->
        if (millis < 1_000.0) {
            stringResource(R.string.value_ms, millis.roundToInt())
        } else {
            stringResource(
                R.string.value_seconds,
                String.format(Locale.getDefault(), "%.2f", millis / 1_000.0),
            )
        }
    } ?: stringResource(R.string.value_none)
    val variation = state.variationPercent?.let {
        stringResource(
            R.string.value_variation,
            String.format(Locale.getDefault(), "%.1f", it),
        )
    } ?: stringResource(R.string.value_none)

    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StatTile(
            label = stringResource(R.string.tempo_stat_interval),
            value = interval,
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
        StatTile(
            label = stringResource(R.string.tempo_stat_taps),
            value = state.tapCount.toString(),
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
        StatTile(
            label = stringResource(R.string.tempo_stat_variation),
            value = variation,
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
    }
}

@Composable
private fun MetronomeCard(
    state: TempoUiState,
    metronomeState: MetronomeState,
    onOpen: () -> Unit,
    onStart: (Int) -> Unit,
) {
    val tapBpm = state.bpm?.roundToInt()
    val subtitle = when {
        metronomeState.isPlaying -> stringResource(
            R.string.tempo_metronome_running,
            metronomeState.bpm,
        )

        tapBpm != null -> stringResource(R.string.tempo_metronome_with_bpm, tapBpm)
        else -> stringResource(R.string.tempo_metronome_idle)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onOpen),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBadge(iconRes = R.drawable.ic_metronome)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.tempo_metronome_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FilledIconButton(
                onClick = {
                    if (metronomeState.isPlaying) onOpen()
                    else onStart(tapBpm ?: metronomeState.bpm)
                },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Icon(
                    painter = painterResource(
                        if (metronomeState.isPlaying) R.drawable.ic_chevron_right
                        else R.drawable.ic_play,
                    ),
                    contentDescription = stringResource(
                        if (metronomeState.isPlaying) R.string.action_open
                        else R.string.tempo_metronome_action,
                    ),
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Composable
private fun TipsCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.tempo_tips_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            TipRow(1, stringResource(R.string.tempo_tip_1))
            TipRow(2, stringResource(R.string.tempo_tip_2))
            TipRow(3, stringResource(R.string.tempo_tip_3))
        }
    }
}

@Composable
private fun TipRow(number: Int, text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(24.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = number.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
