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
import com.studio.wastickermaker.data.model.AppSettings
import com.studio.wastickermaker.data.model.SelectedMediaInfo
import com.studio.wastickermaker.data.model.StickerOutput
import com.studio.wastickermaker.util.StickerContentProvider
import com.studio.wastickermaker.util.StickerConverter
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Coordinates media inspection, 512x512 WebP conversion, and local WhatsApp Sticker Pack persistence.
 * Ensures WhatsApp's strict pack rules (96x96 tray icon <= 50 KB, minimum 3 stickers, matching static/animated type)
 * are always satisfied so the pack adds to WhatsApp without errors.
 */
class StickerRepository(
    private val context: Context,
    private val converter: StickerConverter = StickerConverter(context)
) {

    private val packMutex = Mutex()
    @Volatile
    private var legacyMigrationChecked = false

    private val staticFillerBytes: ByteArray by lazy {
        converter.createTransparentFillerWebp(isAnimated = false)
    }

    private val animatedFillerBytes: ByteArray by lazy {
        converter.createTransparentFillerWebp(isAnimated = true)
    }

    private fun ensureLegacyMigrated() {
        if (legacyMigrationChecked) return
        legacyMigrationChecked = true
        try {
            val legacyDir = File(context.filesDir, "sticker_packs/cobalt_custom_pack_01")
            if (!legacyDir.exists()) return

            val typeMarkerFile = File(legacyDir, "pack_type.txt")
            val isAnimated = typeMarkerFile.exists() && typeMarkerFile.readText().trim() == "ANIMATED"
            val targetPackId = if (isAnimated) StickerContentProvider.ANIMATED_PACK_ID else StickerContentProvider.STATIC_PACK_ID
            val targetDir = File(context.filesDir, "sticker_packs/$targetPackId").apply { mkdirs() }

            legacyDir.listFiles()?.forEach { file ->
                file.copyTo(File(targetDir, file.name), overwrite = true)
            }
            legacyDir.deleteRecursively()
        } catch (_: Exception) {}
    }

    suspend fun inspectSelectedUri(
        uri: Uri,
        settings: AppSettings = AppSettings()
    ): SelectedMediaInfo {
        return converter.inspectMediaUri(uri, settings)
    }

    suspend fun convertMediaToSticker(
        mediaInfo: SelectedMediaInfo,
        settings: AppSettings = AppSettings(),
        onProgress: (Float, String) -> Unit
    ): StickerOutput {
        return converter.convertToStickerOutput(mediaInfo, settings, onProgress)
    }

    suspend fun clearPreviewCache(keepFile: File? = null): Long = withContext(Dispatchers.IO) {
        converter.clearPreviewCache(keepFile)
    }

    suspend fun isStickerInPack(
        previewFile: File?,
        packId: String
    ): Boolean = withContext(Dispatchers.IO) {
        if (previewFile == null || !previewFile.exists()) return@withContext false
        val packDir = File(context.filesDir, "sticker_packs/$packId")
        if (!packDir.exists()) return@withContext false
        val previewLen = previewFile.length()
        val candidates = packDir.listFiles { f ->
            f.isFile && f.name.startsWith("user_sticker_") && f.length() == previewLen
        }.orEmpty()
        if (candidates.isEmpty()) return@withContext false
        val previewBytes = previewFile.readBytes()
        candidates.any { it.readBytes().contentEquals(previewBytes) }
    }

    suspend fun addStickerToLocalPack(
        stickerOutput: StickerOutput,
        packId: String = if (stickerOutput.isAnimated) StickerContentProvider.ANIMATED_PACK_ID else StickerContentProvider.STATIC_PACK_ID
    ): Int = withContext(Dispatchers.IO) {
        packMutex.withLock {
            ensureLegacyMigrated()
            val packDir = File(context.filesDir, "sticker_packs/$packId").apply { mkdirs() }

            updateTrayIconFromSticker(packDir, stickerOutput.file)

            val existingStickers = listUserStickers(packDir)
            val newBytes = stickerOutput.file.readBytes()
            val isDuplicate = existingStickers.any { existing ->
                existing.length() == newBytes.size.toLong() && existing.readBytes().contentEquals(newBytes)
            }
            if (isDuplicate || existingStickers.size >= 30) {
                return@withLock existingStickers.size
            }

            val stickerFileName = "user_sticker_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.webp"
            val userStickerFile = File(packDir, stickerFileName)
            userStickerFile.writeBytes(newBytes)

            syncPackFillers(packDir, packId, forceVersionBump = true).size
        }
    }

    suspend fun getLocalPackStickers(
        packId: String = StickerContentProvider.STATIC_PACK_ID
    ): List<File> = withContext(Dispatchers.IO) {
        packMutex.withLock {
            ensureLegacyMigrated()
            val packDir = File(context.filesDir, "sticker_packs/$packId")
            if (!packDir.exists()) return@withLock emptyList()
            syncPackFillers(packDir, packId, forceVersionBump = false)
        }
    }

    suspend fun deleteStickerFromLocalPack(
        targetFile: File,
        packId: String = StickerContentProvider.STATIC_PACK_ID
    ): List<File> = withContext(Dispatchers.IO) {
        packMutex.withLock {
            ensureLegacyMigrated()
            val packDir = File(context.filesDir, "sticker_packs/$packId")
            if (!packDir.exists()) return@withLock emptyList()

            if (targetFile.exists()) {
                targetFile.delete()
            }

            val remainingUserStickers = syncPackFillers(packDir, packId, forceVersionBump = true)
            if (remainingUserStickers.isNotEmpty()) {
                updateTrayIconFromSticker(packDir, remainingUserStickers.first())
            } else {
                File(packDir, "tray_icon.webp").delete()
                File(packDir, "pack_type.txt").delete()
            }
            remainingUserStickers
        }
    }

    private fun syncPackFillers(
        packDir: File,
        packId: String,
        forceVersionBump: Boolean
    ): List<File> {
        val isAnimated = packId == StickerContentProvider.ANIMATED_PACK_ID
        val transparentBytes = if (isAnimated) animatedFillerBytes else staticFillerBytes
        val (userStickers, fillersChanged) = syncPackFillersInDirectory(packDir, transparentBytes)

        if (forceVersionBump || fillersChanged) {
            val versionFile = File(packDir, "version.txt")
            val nextVersion = (versionFile.takeIf { it.exists() }?.readText()?.trim()?.toIntOrNull() ?: 1) + 1
            versionFile.writeText(nextVersion.toString())

            try {
                val authority = BuildConfig.CONTENT_PROVIDER_AUTHORITY
                context.contentResolver.notifyChange(Uri.parse("content://$authority/metadata"), null)
                context.contentResolver.notifyChange(Uri.parse("content://$authority/stickers/$packId"), null)
            } catch (_: Exception) {}
        }

        return userStickers
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

    companion object {
        internal fun listUserStickers(packDir: File): List<File> {
            return packDir.listFiles { f ->
                f.isFile && f.extension.equals("webp", ignoreCase = true) && f.name.startsWith("user_sticker_")
            }?.sortedWith(compareBy({ it.lastModified() }, { it.name })).orEmpty()
        }

        /**
         * Ensures packs with 1 or 2 user stickers are padded to WhatsApp's 3-sticker minimum using
         * 100% transparent blank WebP placeholders (never duplicating the user's sticker), and heals
         * any legacy duplicate autofill files already on disk.
         */
        internal fun syncPackFillersInDirectory(
            packDir: File,
            transparentBytes: ByteArray
        ): Pair<List<File>, Boolean> {
            val userStickers = listUserStickers(packDir)
            var fillersChanged = false
            val existingFillers = packDir.listFiles { f ->
                f.isFile && f.name.startsWith("autofill_sticker_")
            }.orEmpty()

            if (userStickers.isEmpty() || userStickers.size >= 3) {
                existingFillers.forEach {
                    if (it.delete()) fillersChanged = true
                }
            } else {
                val neededNames = ((userStickers.size + 1)..3).map { slot ->
                    String.format("autofill_sticker_%02d.webp", slot)
                }.toSet()

                existingFillers.filter { it.name !in neededNames }.forEach {
                    if (it.delete()) fillersChanged = true
                }

                for (name in neededNames) {
                    val fillerFile = File(packDir, name)
                    val matchesTransparent = fillerFile.exists() &&
                        fillerFile.length() == transparentBytes.size.toLong() &&
                        fillerFile.readBytes().contentEquals(transparentBytes)
                    if (!matchesTransparent) {
                        fillerFile.writeBytes(transparentBytes)
                        fillersChanged = true
                    }
                }
            }
            return userStickers to fillersChanged
        }
    }
}
