package com.studio.wastickermaker

import com.studio.wastickermaker.data.model.AppLanguage
import com.studio.wastickermaker.data.model.AppSettings
import com.studio.wastickermaker.data.repository.StickerRepository
import com.studio.wastickermaker.ui.theme.AppStrings
import com.studio.wastickermaker.util.StickerConverter
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Unit tests verifying production pack filler synchronization, deletion, word-wrapping,
 * animated WebP RIFF container construction, and EXIF JSON escaping.
 */
class StickerCollectionLogicTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testDeleteTargetStickerDeletesExactFileAndUpdatesFillersViaRepository() {
        val packDir = tempFolder.newFolder("pack")
        val transparentBytes = "TRANSPARENT_FILLER".toByteArray()

        val stickerA = File(packDir, "user_sticker_1001_aaa.webp").apply { writeText("data_A") }
        val stickerB = File(packDir, "user_sticker_1002_bbb.webp").apply { writeText("data_B") }
        val stickerC = File(packDir, "user_sticker_1003_ccc.webp").apply { writeText("data_C") }

        val (initialUsers, _) = StickerRepository.syncPackFillersInDirectory(packDir, transparentBytes)
        assertEquals(3, initialUsers.size)
        assertFalse(File(packDir, "autofill_sticker_03.webp").exists())

        // Delete Sticker A and run production syncPackFillersInDirectory
        assertTrue(stickerA.delete())
        val (remaining, changed) = StickerRepository.syncPackFillersInDirectory(packDir, transparentBytes)

        assertTrue(changed)
        assertEquals(2, remaining.size)
        assertEquals("user_sticker_1002_bbb.webp", remaining[0].name)
        assertEquals("user_sticker_1003_ccc.webp", remaining[1].name)
        // With 2 user stickers, slot 3 filler must exist and slots 1-2 must not
        assertFalse(File(packDir, "autofill_sticker_02.webp").exists())
        assertTrue(File(packDir, "autofill_sticker_03.webp").exists())
    }

    @Test
    fun testSyncPackFillersReplacesLegacyDuplicateWithTransparentPlaceholder() {
        val packDir = tempFolder.newFolder("pack_autofill")
        val transparentFillerBytes = "TRANSPARENT_PLACEHOLDER_WEBP".toByteArray()

        val stickerA = File(packDir, "user_sticker_2001_aaa.webp").apply { writeText("USER_VISIBLE_STICKER_A") }
        File(packDir, "autofill_sticker_02.webp").writeText("USER_VISIBLE_STICKER_A")
        File(packDir, "autofill_sticker_03.webp").writeText("USER_VISIBLE_STICKER_A")

        val (userStickers, changed) = StickerRepository.syncPackFillersInDirectory(packDir, transparentFillerBytes)
        assertEquals(1, userStickers.size)
        assertTrue(changed)

        val filler2 = File(packDir, "autofill_sticker_02.webp")
        val filler3 = File(packDir, "autofill_sticker_03.webp")
        assertTrue(filler2.exists())
        assertTrue(filler3.exists())
        assertFalse(filler2.readBytes().contentEquals(stickerA.readBytes()))
        assertTrue(filler2.readBytes().contentEquals(transparentFillerBytes))
        assertTrue(filler3.readBytes().contentEquals(transparentFillerBytes))

        // Delete the last user sticker: all fillers must be removed
        assertTrue(stickerA.delete())
        val (emptyUsers, changedAfterEmpty) = StickerRepository.syncPackFillersInDirectory(packDir, transparentFillerBytes)
        assertTrue(emptyUsers.isEmpty())
        assertTrue(changedAfterEmpty)
        assertFalse(filler2.exists())
        assertFalse(filler3.exists())
    }

    @Test
    fun testWordWrapSplitsLongTextWithoutNewlines() {
        val longText = "Selamat Ulang Tahun Sahabat Karibku Tercinta"
        val result = StickerConverter.wrapText(longText, maxCharsPerLine = 16)

        assertTrue(result.size >= 3)
        for (line in result) {
            assertTrue(line.length <= 20)
        }
    }

    @Test
    fun testAnimatedWebpContainerFromSingleFrameHasValidRiffAndTwoAnmfFrames() {
        // Minimal single-frame RIFF WEBP with a 4-byte dummy VP8L chunk
        val vp8lChunk = byteArrayOf(
            'V'.code.toByte(), 'P'.code.toByte(), '8'.code.toByte(), 'L'.code.toByte(),
            4, 0, 0, 0,
            0x2F, 0x00, 0x00, 0x00
        )
        val riffSize = 4 + vp8lChunk.size
        val singleFrame = ByteBuffer.allocate(8 + riffSize).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(StandardCharsets.US_ASCII))
            putInt(riffSize)
            put("WEBP".toByteArray(StandardCharsets.US_ASCII))
            put(vp8lChunk)
        }.array()

        val animated = StickerConverter.buildAnimatedWebpFromSingleFrame(singleFrame, frameDurationMs = 100, frameCount = 2)
        assertEquals("RIFF", String(animated, 0, 4, StandardCharsets.US_ASCII))
        assertEquals("WEBP", String(animated, 8, 4, StandardCharsets.US_ASCII))
        val declaredRiffSize = ByteBuffer.wrap(animated, 4, 4).order(ByteOrder.LITTLE_ENDIAN).int
        assertEquals(animated.size - 8, declaredRiffSize)
        assertEquals("VP8X", String(animated, 12, 4, StandardCharsets.US_ASCII))
        assertEquals("ANIM", String(animated, 30, 4, StandardCharsets.US_ASCII))
        assertEquals("ANMF", String(animated, 44, 4, StandardCharsets.US_ASCII))
    }

    @Test
    fun testSettingsResolutionAndDurationCalculations() {
        assertEquals(256, AppSettings.computeInternalResolutionPx(50))
        assertEquals(384, AppSettings.computeInternalResolutionPx(75))
        assertEquals(512, AppSettings.computeInternalResolutionPx(100))

        val customSettings = AppSettings(
            language = AppLanguage.ENGLISH,
            resolutionPercent = 75,
            maxAnimatedDurationSec = 8,
            customPublisher = "   "
        )
        assertEquals(384, customSettings.internalResolutionPx)
        assertEquals(8_000L, customSettings.maxAnimatedDurationMs)
        assertEquals("Sticker Maker", customSettings.effectivePublisher)

        val idStrings = AppStrings.forLanguage(AppLanguage.INDONESIAN)
        val enStrings = AppStrings.forLanguage(AppLanguage.ENGLISH)
        assertEquals("Pengaturan", idStrings.settingsTitle)
        assertEquals("Settings", enStrings.settingsTitle)
    }

    @Test
    fun testExifJsonPayloadEscapesQuotesAndBackslashes() {
        val json = StickerConverter.buildExifJsonPayload(
            packId = "pack_01",
            packName = "My \"Pack\"",
            publisher = "Denis \"Dev\" \\ Studio\nLine2"
        )
        assertTrue(json.contains("\"sticker-pack-publisher\":\"Denis \\\"Dev\\\" \\\\ Studio\\nLine2\""))
        assertTrue(json.contains("\"sticker-pack-name\":\"My \\\"Pack\\\"\""))
    }
}
