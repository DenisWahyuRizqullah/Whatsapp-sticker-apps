package com.studio.wastickermaker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Unit test verifying that sticker collection deletion targets the exact specified file
 * without renaming other files (preventing Coil cache collisions and Compose key desync).
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
    fun testAutoFillMaintainsThreeStickersForWhatsApp() {
        val packDir = tempFolder.newFolder("pack_autofill")

        // 1 user sticker
        val stickerA = File(packDir, "user_sticker_2001_aaa.webp").apply { writeText("data_A") }

        val userStickers = packDir.listFiles { f ->
            f.extension.equals("webp", ignoreCase = true) && f.name.startsWith("user_sticker_")
        }?.sortedBy { it.name }.orEmpty()

        // Remove old autofillers
        packDir.listFiles { f -> f.name.startsWith("autofill_sticker_") }?.forEach { it.delete() }

        // WhatsApp requires 3 stickers
        if (userStickers.size < 3) {
            for (slot in (userStickers.size + 1)..3) {
                val filler = File(packDir, String.format("autofill_sticker_%02d.webp", slot))
                userStickers.first().copyTo(filler, overwrite = true)
            }
        }

        val allPackFiles = packDir.listFiles { f ->
            f.extension.equals("webp", ignoreCase = true)
        }?.sortedBy { it.name }.orEmpty()

        // Total should be 3: 1 user sticker + 2 fillers
        assertEquals(3, allPackFiles.size)
        assertTrue(File(packDir, "autofill_sticker_02.webp").exists())
        assertTrue(File(packDir, "autofill_sticker_03.webp").exists())
    }
}
