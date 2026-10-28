package com.studio.wastickermaker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression test guarding the video trim calculation against empty range exceptions.
 */
class VideoTrimLogicTest {

    private fun computeSafeTrimRange(
        totalDurationMs: Long,
        startMs: Long,
        endMs: Long
    ): Pair<Long, Long> {
        val safeStart = startMs.coerceIn(0L, totalDurationMs)
        val minEnd = (safeStart + 500L).coerceAtMost(totalDurationMs)
        var safeEnd = endMs.coerceIn(minEnd, totalDurationMs)
        val maxDurationMs = 6000L

        if (safeEnd - safeStart > maxDurationMs) {
            safeEnd = (safeStart + maxDurationMs).coerceAtMost(totalDurationMs)
        }
        return safeStart to safeEnd
    }

    @Test
    fun testNormalVideoRange() {
        val (start, end) = computeSafeTrimRange(totalDurationMs = 10000L, startMs = 1000L, endMs = 4000L)
        assertEquals(1000L, start)
        assertEquals(4000L, end)
        assertTrue(end - start <= 6000L)
    }

    @Test
    fun testShortVideoDoesNotThrowEmptyRangeException() {
        // Video is only 300ms (shorter than the 500ms preferred buffer)
        val (start, end) = computeSafeTrimRange(totalDurationMs = 300L, startMs = 0L, endMs = 300L)
        assertEquals(0L, start)
        assertEquals(300L, end)
        assertTrue(start <= end)
    }

    @Test
    fun testZeroDurationVideoDoesNotThrow() {
        val (start, end) = computeSafeTrimRange(totalDurationMs = 0L, startMs = 0L, endMs = 1000L)
        assertEquals(0L, start)
        assertEquals(0L, end)
        assertTrue(start <= end)
    }

    @Test
    fun testExcessiveDurationClampsToSixSeconds() {
        val (start, end) = computeSafeTrimRange(totalDurationMs = 30000L, startMs = 2000L, endMs = 20000L)
        assertEquals(2000L, start)
        assertEquals(8000L, end)
        assertEquals(6000L, end - start)
    }
}
