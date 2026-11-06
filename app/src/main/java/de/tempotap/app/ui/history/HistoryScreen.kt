package de.tempotap.app.ui.history

import android.text.format.DateUtils
import java.text.DateFormat
import java.util.Date
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.tempotap.app.R
import de.tempotap.app.data.SessionRecord
import de.tempotap.app.ui.components.ScreenHeader
import de.tempotap.app.ui.components.SectionTitle
import de.tempotap.app.ui.components.StatTile
import kotlin.math.roundToInt

@Composable
fun HistoryScreen(
    onGoToTempo: () -> Unit,
    viewModel: HistoryViewModel = viewModel(),
) {
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val now = remember { System.currentTimeMillis() }
    var showClearDialog by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 860.dp).fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                ScreenHeader(
                    eyebrow = stringResource(R.string.history_eyebrow),
                    title = stringResource(R.string.history_title),
                    trailing = if (sessions.isNotEmpty()) {
                        {
                            TextButton(onClick = { showClearDialog = true }) {
                                Text(stringResource(R.string.history_clear))
                            }
                        }
                    } else {
                        null
                    },
                )
            }

            if (sessions.isEmpty()) {
                item {
                    EmptyHistory(onGoToTempo = onGoToTempo)
                }
            } else {
                item {
                    SummaryRow(sessions)
                }
                item {
                    ChartCard(sessions)
                }
                item {
                    SectionTitle(stringResource(R.string.history_list_title))
                }
                items(items = sessions, key = { it.id }) { session ->
                    SessionRow(
                        session = session,
                        now = now,
                        onDelete = { viewModel.delete(session.id) },
                    )
                }
                item { Spacer(Modifier.height(8.dp)) }
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
                        viewModel.clear()
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
private fun SummaryRow(sessions: List<SessionRecord>) {
    val average = sessions.map { it.bpm }.average().roundToInt()
    val fastest = sessions.maxOf { it.bpm }.roundToInt()

    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StatTile(
            label = stringResource(R.string.history_sessions),
            value = sessions.size.toString(),
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
        StatTile(
            label = stringResource(R.string.history_average),
            value = average.toString(),
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
        StatTile(
            label = stringResource(R.string.history_fastest),
            value = fastest.toString(),
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
    }
}

@Composable
private fun ChartCard(sessions: List<SessionRecord>) {
    val values = sessions.take(24).reversed().map { it.bpm }
    val minBpm = values.minOrNull()?.roundToInt() ?: 0
    val maxBpm = values.maxOrNull()?.roundToInt() ?: 0
    val chartDescription = stringResource(R.string.history_chart_desc)

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.history_chart_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "$minBpm – $maxBpm BPM",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(16.dp))
            BpmChart(
                values = values,
                description = chartDescription,
                modifier = Modifier.fillMaxWidth().height(140.dp),
            )
        }
    }
}

@Composable
private fun BpmChart(
    values: List<Double>,
    description: String,
    modifier: Modifier = Modifier,
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val fillBrush = Brush.verticalGradient(
        colors = listOf(
            lineColor.copy(alpha = 0.32f),
            Color.Transparent,
        ),
    )
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    Canvas(
        modifier = modifier.semantics { contentDescription = description },
    ) {
        if (values.isEmpty()) return@Canvas

        val inset = 8.dp.toPx()
        val chartHeight = (size.height - inset * 2).coerceAtLeast(1f)
        val min = values.min()
        val max = values.max()
        val range = (max - min).coerceAtLeast(1.0)

        repeat(3) { index ->
            val y = inset + chartHeight * index / 2f
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx(),
            )
        }

        val points = values.mapIndexed { index, value ->
            val x = if (values.size > 1) {
                size.width * index / (values.size - 1).toFloat()
            } else {
                size.width / 2f
            }
            val y = inset + ((max - value) / range * chartHeight).toFloat()
            Offset(x, y)
        }

        if (points.size == 1) {
            drawCircle(color = lineColor, radius = 6.dp.toPx(), center = points.first())
            return@Canvas
        }

        val linePath = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { point -> lineTo(point.x, point.y) }
        }
        val fillPath = Path().apply {
            addPath(linePath)
            lineTo(points.last().x, size.height)
            lineTo(points.first().x, size.height)
            close()
        }

        drawPath(path = fillPath, brush = fillBrush)
        drawPath(
            path = linePath,
            color = lineColor,
            style = Stroke(
                width = 3.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
        drawCircle(color = lineColor, radius = 4.dp.toPx(), center = points.last())
    }
}

@Composable
private fun SessionRow(
    session: SessionRecord,
    now: Long,
    onDelete: () -> Unit,
) {
    val relative = DateUtils.getRelativeTimeSpanString(
        session.createdAt,
        now,
        DateUtils.MINUTE_IN_MILLIS,
    ).toString()
    val absolute = remember(session.createdAt) {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
            .format(Date(session.createdAt))
    }
    val durationSeconds = (session.durationMs / 1_000L).toInt()
    val duration = stringResource(
        R.string.history_duration,
        durationSeconds / 60,
        durationSeconds % 60,
    )

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(start = 18.dp, top = 14.dp, bottom = 14.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = relative,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = absolute,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = pluralStringResource(
                        R.plurals.tap_count,
                        session.taps,
                        session.taps,
                    ) + " · " + duration,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = session.bpm.roundToInt().toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.bpm_unit),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.history_delete_session),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun EmptyHistory(onGoToTempo: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(76.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.ic_timeline),
                    contentDescription = null,
                    modifier = Modifier.size(34.dp),
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.history_empty_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.history_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 340.dp),
        )
        Spacer(Modifier.height(20.dp))
        FilledTonalButton(onClick = onGoToTempo) {
            Text(stringResource(R.string.history_empty_action))
        }
    }
}
