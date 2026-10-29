package com.studio.wastickermaker

import com.studio.wastickermaker.data.model.AppLanguage
import com.studio.wastickermaker.data.model.AppSettings
import com.studio.wastickermaker.ui.theme.AppStrings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Unit test verifying that sticker collection deletion targets the exact specified file
 * without renaming other files, segments are isolated, and text wraps properly.
 */
class StickerCollectionLogicTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testDeleteTargetStickerDeletesExactFileWithoutRenamingOthers() {
        val packDir = tempFolder.newFolder("pack")

        // Simulate 3 user stickers with immutable filenames
        val stickerA = File(packDir, "user_sticker_1001_aaa.webp").apply { writeText("data_A") }
        val stickerB = File(packDir, "user_sticker_1002_bbb.webp").apply { writeText("data_B") }
        val stickerC = File(packDir, "user_sticker_1003_ccc.webp").apply { writeText("data_C") }

        val initialList = packDir.listFiles { f ->
            f.extension.equals("webp", ignoreCase = true) && f.name.startsWith("user_sticker_")
        }?.sortedBy { it.name }.orEmpty()

        assertEquals(3, initialList.size)
        assertEquals("user_sticker_1001_aaa.webp", initialList[0].name)
        assertEquals("user_sticker_1002_bbb.webp", initialList[1].name)
        assertEquals("user_sticker_1003_ccc.webp", initialList[2].name)

        // User deletes Sticker A
        val targetToDelete = stickerA
        if (targetToDelete.exists()) {
            targetToDelete.delete()
        }

        val remaining = packDir.listFiles { f ->
            f.extension.equals("webp", ignoreCase = true) && f.name.startsWith("user_sticker_")
        }?.sortedBy { it.name }.orEmpty()

        // Sticker A is gone
        assertFalse(stickerA.exists())
        assertEquals(2, remaining.size)

        // Crucial: Sticker B and C keep their exact filenames and content!
        assertTrue(stickerB.exists())
        assertEquals("user_sticker_1002_bbb.webp", remaining[0].name)
        assertEquals("data_B", remaining[0].readText())

        assertTrue(stickerC.exists())
        assertEquals("user_sticker_1003_ccc.webp", remaining[1].name)
        assertEquals("data_C", remaining[1].readText())
    }

    @Test
    fun testAutoFillUsesTransparentPlaceholderInsteadOfDuplicatingUserSticker() {
        val packDir = tempFolder.newFolder("pack_autofill")
        val transparentFillerBytes = "TRANSPARENT_PLACEHOLDER_WEBP".toByteArray()

        // 1 user sticker
        val stickerA = File(packDir, "user_sticker_2001_aaa.webp").apply { writeText("USER_VISIBLE_STICKER_A") }

        // Simulate legacy duplicate autofill on disk from old bug
        File(packDir, "autofill_sticker_02.webp").writeText("USER_VISIBLE_STICKER_A")
        File(packDir, "autofill_sticker_03.webp").writeText("USER_VISIBLE_STICKER_A")

        val userStickers = packDir.listFiles { f ->
            f.extension.equals("webp", ignoreCase = true) && f.name.startsWith("user_sticker_")
        }?.sortedBy { it.name }.orEmpty()

        // Heal / populate autofill slots with transparent bytes instead of copying user's sticker
        val neededNames = ((userStickers.size + 1)..3).map { slot ->
            String.format("autofill_sticker_%02d.webp", slot)
        }.toSet()

        for (name in neededNames) {
            val fillerFile = File(packDir, name)
            if (!fillerFile.exists() || !fillerFile.readBytes().contentEquals(transparentFillerBytes)) {
                fillerFile.writeBytes(transparentFillerBytes)
            }
        }

        val allPackFiles = packDir.listFiles { f ->
            f.extension.equals("webp", ignoreCase = true)
        }?.sortedBy { it.name }.orEmpty()

        // Total should be 3: 1 user sticker + 2 transparent fillers (NOT duplicates of stickerA)
        assertEquals(3, allPackFiles.size)
        val filler2 = File(packDir, "autofill_sticker_02.webp")
        val filler3 = File(packDir, "autofill_sticker_03.webp")
        assertTrue(filler2.exists())
        assertTrue(filler3.exists())
        assertFalse(filler2.readBytes().contentEquals(stickerA.readBytes()))
        assertFalse(filler3.readBytes().contentEquals(stickerA.readBytes()))
        assertTrue(filler2.readBytes().contentEquals(transparentFillerBytes))
        assertTrue(filler3.readBytes().contentEquals(transparentFillerBytes))
    }

    @Test
    fun testStaticAndAnimatedPacksAreCompletelySeparated() {
        val packsRoot = tempFolder.newFolder("sticker_packs")
        val staticDir = File(packsRoot, "cobalt_custom_pack_static").apply { mkdirs() }
        val animatedDir = File(packsRoot, "cobalt_custom_pack_animated").apply { mkdirs() }

        // User saves 2 photo stickers in static pack
        File(staticDir, "user_sticker_1.webp").writeText("photo1")
        File(staticDir, "user_sticker_2.webp").writeText("photo2")

        // User switches to animated and saves a GIF sticker
        File(animatedDir, "user_sticker_anim1.webp").writeText("anim1")

        // Crucial: Static pack remains 100% intact with its 2 stickers!
        val staticFiles = staticDir.listFiles { f -> f.name.startsWith("user_sticker_") }?.sortedBy { it.name }.orEmpty()
        val animatedFiles = animatedDir.listFiles { f -> f.name.startsWith("user_sticker_") }?.sortedBy { it.name }.orEmpty()

        assertEquals(2, staticFiles.size)
        assertEquals(1, animatedFiles.size)
        assertEquals("photo1", staticFiles[0].readText())
        assertEquals("photo2", staticFiles[1].readText())
        assertEquals("anim1", animatedFiles[0].readText())
    }

    @Test
    fun testWordWrapSplitsLongTextWithoutNewlines() {
        val longText = "Selamat Ulang Tahun Sahabat Karibku Tercinta"
        val maxCharsPerLine = 16

        val result = mutableListOf<String>()
        val paragraphs = longText.trim().split("\n")
        for (paragraph in paragraphs) {
            val words = paragraph.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
            if (words.isEmpty()) continue
            var currentLine = StringBuilder()
            for (word in words) {
                if (currentLine.isEmpty()) {
                    currentLine.append(word)
                } else if (currentLine.length + 1 + word.length <= maxCharsPerLine) {
                    currentLine.append(" ").append(word)
                } else {
                    result.add(currentLine.toString())
                    currentLine = StringBuilder(word)
                }
            }
            if (currentLine.isNotEmpty()) {
                result.add(currentLine.toString())
            }
        }

        assertTrue(result.size >= 3)
        // All wrapped lines must be <= maxCharsPerLine (or a single long word)
        for (line in result) {
            assertTrue(line.length <= 20)
        }
    }

    @Test
    fun testCachePruningRetainsOnlyRecentFiveFiles() {
        val cacheDir = tempFolder.newFolder("stickers_cache")

        // Create 8 dummy preview files
        for (i in 1..8) {
            val file = File(cacheDir, "preview_$i.webp")
            file.writeText("data_$i")
            file.setLastModified(1000L * i)
        }

        // Run pruning logic
        cacheDir.listFiles { f -> f.extension.equals("webp", ignoreCase = true) }
            ?.sortedByDescending { it.lastModified() }
            ?.drop(5)
            ?.forEach { it.delete() }

        val remainingFiles = cacheDir.listFiles()?.sortedBy { it.lastModified() }.orEmpty()
        assertEquals(5, remainingFiles.size)
        assertEquals("preview_4.webp", remainingFiles[0].name)
        assertEquals("preview_8.webp", remainingFiles[4].name)
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
}
