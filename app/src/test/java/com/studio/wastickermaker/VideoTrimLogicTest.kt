package com.studio.wastickermaker

import com.studio.wastickermaker.util.StickerConverter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression test guarding the production video trim calculation against empty range exceptions.
 */
class VideoTrimLogicTest {

    @Test
    fun testNormalVideoRange() {
        val (start, end) = StickerConverter.computeSafeTrimRange(
            totalDurationMs = 10000L,
            startMs = 1000L,
            endMs = 4000L
        )
        assertEquals(1000L, start)
        assertEquals(4000L, end)
        assertTrue(end - start <= 6000L)
    }

    @Test
    fun testShortVideoDoesNotThrowEmptyRangeException() {
        val (start, end) = StickerConverter.computeSafeTrimRange(
            totalDurationMs = 300L,
            startMs = 0L,
            endMs = 300L
        )
        assertEquals(0L, start)
        assertEquals(300L, end)
    }

    @Test
    fun testStartNearEndOfVideoDoesNotInvertRange() {
        val (start, end) = StickerConverter.computeSafeTrimRange(
            totalDurationMs = 5000L,
            startMs = 4800L,
            endMs = 4900L
        )
        assertEquals(4800L, start)
        assertEquals(5000L, end)
    }

    @Test
    fun testTrimExceedingConfiguredMaxDurationIsCapped() {
        val (start, end) = StickerConverter.computeSafeTrimRange(
            totalDurationMs = 20000L,
            startMs = 2000L,
            endMs = 15000L,
            maxDurationMs = 3000L
        )
        assertEquals(2000L, start)
        assertEquals(5000L, end)
    }
}
