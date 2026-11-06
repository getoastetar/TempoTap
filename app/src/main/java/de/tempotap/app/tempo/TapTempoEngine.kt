package de.tempotap.app.tempo

import kotlin.math.sqrt

enum class Stability {
    NONE,
    LOW,
    MEDIUM,
    HIGH,
}

data class TapSnapshot(
    val bpm: Double?,
    val tapCount: Int,
    val lastIntervalMs: Double?,
    val variationPercent: Double?,
    val stability: Stability,
    val durationMs: Long,
)

/**
 * Pure, testable tap-tempo logic. Timestamps are monotonic nanoseconds.
 */
class TapTempoEngine(
    resetTimeoutMs: Long = 2_500L,
    private val maxTaps: Int = 12,
) {
    private val timestamps = ArrayDeque<Long>()

    var resetTimeoutMs: Long = resetTimeoutMs
        set(value) {
            field = value.coerceIn(500L, 10_000L)
        }

    fun tap(nowNanos: Long): TapSnapshot {
        val last = timestamps.lastOrNull()
        if (last != null && nowNanos - last > resetTimeoutMs * 1_000_000L) {
            timestamps.clear()
        }
        timestamps.addLast(nowNanos)
        while (timestamps.size > maxTaps) {
            timestamps.removeFirst()
        }
        return snapshot()
    }

    fun reset() {
        timestamps.clear()
    }

    fun snapshot(): TapSnapshot {
        if (timestamps.size < 2) {
            return TapSnapshot(
                bpm = null,
                tapCount = timestamps.size,
                lastIntervalMs = null,
                variationPercent = null,
                stability = Stability.NONE,
                durationMs = 0L,
            )
        }

        val intervals = buildList(timestamps.size - 1) {
            var previous = timestamps.first()
            timestamps.drop(1).forEach { current ->
                add((current - previous) / 1_000_000.0)
                previous = current
            }
        }

        val filtered = BpmMath.removeOutliers(intervals)
        val mean = filtered.average()
        val bpm = if (mean > 0.0) 60_000.0 / mean else null
        val variation = BpmMath.variationPercent(filtered)
        val durationMs = (timestamps.last() - timestamps.first()) / 1_000_000L

        return TapSnapshot(
            bpm = bpm,
            tapCount = timestamps.size,
            lastIntervalMs = intervals.lastOrNull(),
            variationPercent = variation,
            stability = BpmMath.stability(variation),
            durationMs = durationMs,
        )
    }
}

object BpmMath {

    fun removeOutliers(intervals: List<Double>): List<Double> {
        if (intervals.size < 4) return intervals
        val median = intervals.sorted()[intervals.size / 2]
        if (median <= 0.0) return intervals
        val lower = median * 0.55
        val upper = median * 1.75
        val filtered = intervals.filter { it in lower..upper }
        return if (filtered.size >= 2) filtered else intervals
    }

    fun variationPercent(intervals: List<Double>): Double? {
        if (intervals.size < 2) return null
        val mean = intervals.average()
        if (mean <= 0.0) return null
        val variance = intervals.sumOf { (it - mean) * (it - mean) } / intervals.size
        return sqrt(variance) / mean * 100.0
    }

    fun stability(variationPercent: Double?): Stability = when {
        variationPercent == null -> Stability.NONE
        variationPercent <= 2.5 -> Stability.HIGH
        variationPercent <= 6.0 -> Stability.MEDIUM
        else -> Stability.LOW
    }
}
