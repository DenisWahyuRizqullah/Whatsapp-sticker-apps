package com.studio.wastickermaker.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import com.studio.wastickermaker.BuildConfig
import com.studio.wastickermaker.data.model.SelectedMediaInfo
import com.studio.wastickermaker.data.model.StickerOutput
import com.studio.wastickermaker.util.StickerContentProvider
import com.studio.wastickermaker.util.StickerConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Coordinates media inspection, 512x512 WebP conversion, and local WhatsApp Sticker Pack persistence.
 * Ensures WhatsApp's strict pack rules (96x96 tray icon <= 50 KB, minimum 3 stickers, matching static/animated type)
 * are always satisfied so the pack adds to WhatsApp without errors.
 */
class StickerRepository(
    private val context: Context,
    private val converter: StickerConverter = StickerConverter(context)
) {

    suspend fun inspectSelectedUri(uri: Uri): SelectedMediaInfo {
        return converter.inspectMediaUri(uri)
    }

    suspend fun convertMediaToSticker(
        mediaInfo: SelectedMediaInfo,
        onProgress: (Float, String) -> Unit
    ): StickerOutput {
        return converter.convertToStickerOutput(mediaInfo, onProgress)
    }

    suspend fun addStickerToLocalPack(
        stickerOutput: StickerOutput,
        packId: String = StickerContentProvider.DEFAULT_PACK_ID
    ): Int = withContext(Dispatchers.IO) {
        val packDir = File(context.filesDir, "sticker_packs/$packId").apply { mkdirs() }

        // WhatsApp forbids mixing static and animated stickers in the same pack.
        // If switching between static and animated, reset pack files to keep the pack 100% valid.
        val typeMarkerFile = File(packDir, "pack_type.txt")
        val currentType = if (stickerOutput.isAnimated) "ANIMATED" else "STATIC"
        if (typeMarkerFile.exists() && typeMarkerFile.readText().trim() != currentType) {
            packDir.listFiles { f -> f.extension.equals("webp", ignoreCase = true) && f.name != "tray_icon.webp" }
                ?.forEach { it.delete() }
        }
        typeMarkerFile.writeText(currentType)

        updateTrayIconFromSticker(packDir, stickerOutput.file)

        val existingStickers = packDir.listFiles { f ->
            f.extension.equals("webp", ignoreCase = true) &&
                f.name.startsWith("user_sticker_")
        }?.sortedBy { it.name }.orEmpty()

        val nextIdx = (existingStickers.size + 1).coerceAtMost(30)
        val userStickerFile = File(packDir, String.format("user_sticker_%02d.webp", nextIdx))
        stickerOutput.file.copyTo(userStickerFile, overwrite = true)

        // WhatsApp requires at least 3 stickers in a pack. Populate filler slots if user has < 3 stickers.
        val allUserStickers = packDir.listFiles { f ->
            f.extension.equals("webp", ignoreCase = true) && f.name.startsWith("user_sticker_")
        }?.sortedBy { it.name }.orEmpty()

        // Remove old auto-fillers first
        packDir.listFiles { f -> f.name.startsWith("autofill_sticker_") }?.forEach { it.delete() }

        if (allUserStickers.size < 3) {
            for (slot in (allUserStickers.size + 1)..3) {
                val filler = File(packDir, String.format("autofill_sticker_%02d.webp", slot))
                stickerOutput.file.copyTo(filler, overwrite = true)
            }
        }

        // Increment pack image_data_version so WhatsApp refreshes cache
        val versionFile = File(packDir, "version.txt")
        val nextVersion = (versionFile.takeIf { it.exists() }?.readText()?.trim()?.toIntOrNull() ?: 1) + 1
        versionFile.writeText(nextVersion.toString())

        try {
            val authority = BuildConfig.CONTENT_PROVIDER_AUTHORITY
            context.contentResolver.notifyChange(Uri.parse("content://$authority/metadata"), null)
            context.contentResolver.notifyChange(Uri.parse("content://$authority/stickers/$packId"), null)
        } catch (_: Exception) {}

        allUserStickers.size
    }

    suspend fun getLocalPackStickers(
        packId: String = StickerContentProvider.DEFAULT_PACK_ID
    ): List<File> = withContext(Dispatchers.IO) {
        val packDir = File(context.filesDir, "sticker_packs/$packId")
        if (!packDir.exists()) return@withContext emptyList()
        packDir.listFiles { f ->
            f.extension.equals("webp", ignoreCase = true) && f.name.startsWith("user_sticker_")
        }?.sortedBy { it.name }.orEmpty()
    }

    suspend fun deleteStickerFromLocalPack(
        targetFile: File,
        packId: String = StickerContentProvider.DEFAULT_PACK_ID
    ): List<File> = withContext(Dispatchers.IO) {
        val packDir = File(context.filesDir, "sticker_packs/$packId")
        if (!packDir.exists()) return@withContext emptyList()

        if (targetFile.exists()) {
            targetFile.delete()
        }

        // Get remaining user stickers
        val remainingUserStickers = packDir.listFiles { f ->
            f.extension.equals("webp", ignoreCase = true) && f.name.startsWith("user_sticker_")
        }?.sortedBy { it.name }.orEmpty()

        // Temporarily move to avoid naming collisions when reindexing
        val tempFiles = remainingUserStickers.mapIndexed { index, file ->
            val temp = File(packDir, "tmp_reindex_${index}_${System.currentTimeMillis()}.webp")
            file.renameTo(temp)
            temp
        }

        // Reindex user stickers sequentially: user_sticker_01.webp, user_sticker_02.webp, ...
        val reindexed = tempFiles.mapIndexed { index, tempFile ->
            val dest = File(packDir, String.format("user_sticker_%02d.webp", index + 1))
            tempFile.renameTo(dest)
            dest
        }

        // Remove old auto-fillers first
        packDir.listFiles { f -> f.name.startsWith("autofill_sticker_") }?.forEach { it.delete() }

        if (reindexed.isNotEmpty()) {
            updateTrayIconFromSticker(packDir, reindexed.first())
            // WhatsApp requires at least 3 stickers in a pack. Populate filler slots if < 3.
            if (reindexed.size < 3) {
                for (slot in (reindexed.size + 1)..3) {
                    val filler = File(packDir, String.format("autofill_sticker_%02d.webp", slot))
                    reindexed.first().copyTo(filler, overwrite = true)
                }
            }
        } else {
            File(packDir, "tray_icon.webp").delete()
            File(packDir, "pack_type.txt").delete()
        }

        // Increment pack image_data_version so WhatsApp refreshes cache
        val versionFile = File(packDir, "version.txt")
        val nextVersion = (versionFile.takeIf { it.exists() }?.readText()?.trim()?.toIntOrNull() ?: 1) + 1
        versionFile.writeText(nextVersion.toString())

        try {
            val authority = BuildConfig.CONTENT_PROVIDER_AUTHORITY
            context.contentResolver.notifyChange(Uri.parse("content://$authority/metadata"), null)
            context.contentResolver.notifyChange(Uri.parse("content://$authority/stickers/$packId"), null)
        } catch (_: Exception) {}

        reindexed
    }

    private fun updateTrayIconFromSticker(packDir: File, stickerFile: File) {
        val trayFile = File(packDir, "tray_icon.webp")
        val trayBitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(trayBitmap)
        canvas.drawColor(Color.TRANSPARENT)

        val decoded = BitmapFactory.decodeFile(stickerFile.absolutePath)
        if (decoded != null) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(decoded, null, RectF(4f, 4f, 92f, 92f), paint)
            decoded.recycle()
        } else {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#2563EB") }
            canvas.drawRoundRect(RectF(8f, 8f, 88f, 88f), 24f, 24f, paint)
        }

        val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            @Suppress("DEPRECATION")
            Bitmap.CompressFormat.WEBP
        }
        FileOutputStream(trayFile).use { fos ->
            trayBitmap.compress(format, 80, fos)
        }
        trayBitmap.recycle()
    }
}
