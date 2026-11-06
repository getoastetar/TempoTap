package de.tempotap.app.tempo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TapTempoEngineTest {

    private val ms = 1_000_000L

    @Test
    fun `steady taps produce 120 bpm`() {
        val engine = TapTempoEngine(resetTimeoutMs = 2_500L)
        var snapshot = engine.tap(0L)
        listOf(500L, 1_000L, 1_500L, 2_000L).forEach { offsetMs ->
            snapshot = engine.tap(offsetMs * ms)
        }

        assertEquals(5, snapshot.tapCount)
        assertEquals(120.0, snapshot.bpm ?: 0.0, 0.001)
        assertEquals(500.0, snapshot.lastIntervalMs ?: 0.0, 0.001)
        assertEquals(Stability.HIGH, snapshot.stability)
    }

    @Test
    fun `long pause starts a new measurement`() {
        val engine = TapTempoEngine(resetTimeoutMs = 2_000L)
        engine.tap(0L)
        engine.tap(500L * ms)
        val snapshot = engine.tap(4_000L * ms)

        assertEquals(1, snapshot.tapCount)
        assertNull(snapshot.bpm)
        assertEquals(Stability.NONE, snapshot.stability)
    }

    @Test
    fun `removeOutliers ignores a single late tap`() {
        val intervals = listOf(500.0, 500.0, 500.0, 500.0, 1_000.0)
        val filtered = BpmMath.removeOutliers(intervals)

        assertEquals(4, filtered.size)
        assertEquals(120.0, 60_000.0 / filtered.average(), 0.001)
    }

    @Test
    fun `variation reflects irregular tapping`() {
        val steady = BpmMath.variationPercent(listOf(500.0, 500.0, 500.0, 500.0))
        val shaky = BpmMath.variationPercent(listOf(400.0, 600.0, 420.0, 580.0))

        assertEquals(0.0, steady ?: -1.0, 0.001)
        assertTrue((shaky ?: 0.0) > (steady ?: 0.0))
        assertEquals(Stability.LOW, BpmMath.stability(shaky))
    }

    @Test
    fun `reset clears the tap buffer`() {
        val engine = TapTempoEngine()
        engine.tap(0L)
        engine.tap(500L * ms)
        engine.reset()

        val snapshot = engine.snapshot()
        assertEquals(0, snapshot.tapCount)
        assertNull(snapshot.bpm)
    }
}
