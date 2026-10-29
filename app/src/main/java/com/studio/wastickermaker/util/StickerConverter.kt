package com.studio.wastickermaker.util

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.ReturnCode
import com.studio.wastickermaker.data.model.MediaInputType
import com.studio.wastickermaker.data.model.ScaleFitMode
import com.studio.wastickermaker.data.model.SelectedMediaInfo
import com.studio.wastickermaker.data.model.StickerOutput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max
import kotlin.math.min

class StickerConversionException(
    message: String,
    val isSizeLimitError: Boolean = false,
    val isDurationLimitError: Boolean = false,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Converts Static Images (JPEG/PNG), Videos (MP4/MKV/3GP), and Animated GIFs (.gif)
 * into official 512x512 WhatsApp Stickers (.webp) with embedded WhatsApp EXIF metadata.
 */
class StickerConverter(private val context: Context) {

    companion object {
        const val STICKER_DIMENSION_PX = 512
        const val MAX_STATIC_SIZE_BYTES = 100 * 1024L      // 100 KB
        const val MAX_ANIMATED_SIZE_BYTES = 500 * 1024L    // 500 KB
        const val MAX_VIDEO_TRIM_DURATION_MS = 6_000L      // 6 seconds max trim
        const val MIN_VIDEO_TRIM_DURATION_MS = 500L        // 0.5 seconds min trim
        private const val STICKER_CACHE_FOLDER = "stickers"
    }

    private data class AnimatedEncodingProfile(
        val fps: Int,
        val quality: Int,
        val compressionLevel: Int
    )

    private val animatedProfiles = listOf(
        AnimatedEncodingProfile(fps = 15, quality = 75, compressionLevel = 4),
        AnimatedEncodingProfile(fps = 12, quality = 60, compressionLevel = 5),
        AnimatedEncodingProfile(fps = 10, quality = 45, compressionLevel = 6),
        AnimatedEncodingProfile(fps = 8,  quality = 32, compressionLevel = 6),
        AnimatedEncodingProfile(fps = 6,  quality = 22, compressionLevel = 6)
    )

    suspend fun inspectMediaUri(uri: Uri): SelectedMediaInfo = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val mimeType = detectMimeType(uri)
            ?: throw StickerConversionException("Could not detect file format.")

        val inputType = classifyMimeType(mimeType)
        val (displayName, fileSize) = queryFileDisplayNameAndSize(resolver, uri)

        val durationMs = if (inputType == MediaInputType.VIDEO) {
            extractVideoDurationMs(uri)
        } else {
            0L
        }

        val defaultEndMs = if (inputType == MediaInputType.VIDEO) {
            min(durationMs, MAX_VIDEO_TRIM_DURATION_MS)
        } else {
            0L
        }

        SelectedMediaInfo(
            uri = uri,
            mimeType = mimeType,
            inputType = inputType,
            displayName = displayName,
            sourceSizeBytes = fileSize,
            totalDurationMs = durationMs,
            trimStartMs = 0L,
            trimEndMs = defaultEndMs
        )
    }

    fun detectMimeType(uri: Uri): String? {
        val resolver = context.contentResolver
        val resolvedType = resolver.getType(uri)?.lowercase(Locale.US)
        if (!resolvedType.isNullOrBlank()) return resolvedType
        val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            ?.lowercase(Locale.US)
            ?: return null
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    }

    fun classifyMimeType(mimeType: String): MediaInputType {
        val normalized = mimeType.lowercase(Locale.US)
        return when {
            normalized == "image/gif" -> MediaInputType.ANIMATED_GIF
            normalized.startsWith("image/") -> MediaInputType.STATIC_IMAGE
            normalized.startsWith("video/") -> MediaInputType.VIDEO
            else -> throw StickerConversionException(
                "Unsupported file format. Please choose a Photo, GIF, or Video."
            )
        }
    }

    /**
     * Converts a Static Image (JPEG, PNG) into an exact 512x512 Static WebP (< 100 KB)
     * with alpha channel preserved and WhatsApp EXIF sticker metadata injected.
     */
    suspend fun convertStaticImageToWebp(
        uri: Uri,
        fitMode: ScaleFitMode = ScaleFitMode.CENTER_CROP,
        stickerText: String = "",
        onProgress: (Float, String) -> Unit = { _, _ -> }
    ): File = withContext(Dispatchers.IO) {
        onProgress(0.20f, "Loading photo...")
        val sourceBitmap = decodeBitmapFromUri(uri)

        onProgress(0.50f, "Formatting 512x512 sticker canvas...")
        val canvasBitmap = transformBitmapTo512x512(sourceBitmap, fitMode, stickerText)
        if (sourceBitmap !== canvasBitmap) {
            sourceBitmap.recycle()
        }

        onProgress(0.75f, "Optimizing WebP sticker size...")
        val outputFile = createOutputWebpFile("sticker_static")
        var currentQuality = 90
        var finalWebpBytes: ByteArray

        val compressFormat = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            @Suppress("DEPRECATION")
            Bitmap.CompressFormat.WEBP
        }

        do {
            val baos = ByteArrayOutputStream()
            canvasBitmap.compress(compressFormat, currentQuality, baos)
            val rawWebp = baos.toByteArray()
            finalWebpBytes = injectWhatsAppExifChunk(rawWebp)
            if (finalWebpBytes.size <= MAX_STATIC_SIZE_BYTES) {
                break
            }
            currentQuality -= 8
        } while (currentQuality >= 16)

        canvasBitmap.recycle()

        if (finalWebpBytes.size > MAX_STATIC_SIZE_BYTES) {
            throw StickerConversionException(
                message = "Sticker file size exceeds WhatsApp's 100 KB limit.",
                isSizeLimitError = true
            )
        }

        FileOutputStream(outputFile).use { fos ->
            fos.write(finalWebpBytes)
            fos.flush()
        }

        onProgress(1.0f, "Sticker ready!")
        outputFile
    }

    /**
     * Converts a Video (MP4, MKV, 3GP) trimmed to <= 6 seconds into a 512x512 Animated WebP (<= 500 KB).
     */
    suspend fun convertVideoToAnimatedWebp(
        uri: Uri,
        startMs: Long,
        endMs: Long,
        fitMode: ScaleFitMode = ScaleFitMode.CENTER_CROP,
        onProgress: (Float, String) -> Unit = { _, _ -> }
    ): File = withContext(Dispatchers.IO) {
        if (endMs <= startMs) {
            throw StickerConversionException(
                message = "End time must be greater than start time.",
                isDurationLimitError = true
            )
        }

        val clipDurationMs = endMs - startMs
        if (clipDurationMs > MAX_VIDEO_TRIM_DURATION_MS) {
            throw StickerConversionException(
                message = "Please trim the video to 6 seconds or less.",
                isDurationLimitError = true
            )
        }
        if (clipDurationMs < MIN_VIDEO_TRIM_DURATION_MS) {
            throw StickerConversionException(
                message = "Video clip must be at least 0.5 seconds long.",
                isDurationLimitError = true
            )
        }

        onProgress(0.10f, "Preparing video clip...")
        val tempInputFile = copyUriToTempFile(uri, "input_video.mp4")

        try {
            val outputFile = createOutputWebpFile("sticker_video")
            val startSeconds = String.format(Locale.US, "%.3f", startMs / 1000.0)
            val durationSeconds = String.format(Locale.US, "%.3f", clipDurationMs / 1000.0)

            for ((index, profile) in animatedProfiles.withIndex()) {
                onProgress(
                    0.20f + (index * 0.15f),
                    "Creating animated sticker (${profile.fps} FPS)..."
                )

                val scaleFilter = build512ScaleFilter(profile.fps, fitMode)
                val command = buildString {
                    append("-y ")
                    append("-ss $startSeconds ")
                    append("-t $durationSeconds ")
                    append("-i \"${tempInputFile.absolutePath}\" ")
                    append("-vf \"$scaleFilter\" ")
                    append("-vcodec libwebp ")
                    append("-lossless 0 ")
                    append("-compression_level ${profile.compressionLevel} ")
                    append("-q:v ${profile.quality} ")
                    append("-loop 0 ")
                    append("-preset default ")
                    append("-an -vsync 0 ")
                    append("\"${outputFile.absolutePath}\"")
                }

                executeFfmpegCommand(
                    command = command,
                    totalDurationMs = clipDurationMs,
                    onProgress = { passProgress ->
                        val base = 0.20f + (index * 0.15f)
                        val mapped = (base + passProgress * 0.65f).coerceAtMost(0.95f)
                        onProgress(mapped, "Converting video to sticker...")
                    }
                )

                if (outputFile.exists() && outputFile.length() in 1..(MAX_ANIMATED_SIZE_BYTES - 512)) {
                    val withExif = injectWhatsAppExifChunk(outputFile.readBytes())
                    if (withExif.size <= MAX_ANIMATED_SIZE_BYTES) {
                        outputFile.writeBytes(withExif)
                        onProgress(1.0f, "Animated sticker ready!")
                        return@withContext outputFile
                    }
                }
            }

            if (outputFile.exists()) outputFile.delete()
            throw StickerConversionException(
                message = "Video is too complex to fit under 500 KB. Try trimming a shorter clip (2–4 seconds).",
                isSizeLimitError = true
            )
        } finally {
            if (tempInputFile.exists()) tempInputFile.delete()
        }
    }

    /**
     * Converts an Animated GIF (.gif) into a 512x512 Animated WebP (loop = 0, <= 500 KB).
     */
    suspend fun convertGifToAnimatedWebp(
        uri: Uri,
        fitMode: ScaleFitMode = ScaleFitMode.FIT_CENTER,
        onProgress: (Float, String) -> Unit = { _, _ -> }
    ): File = withContext(Dispatchers.IO) {
        onProgress(0.15f, "Reading GIF frames...")
        val tempInputFile = copyUriToTempFile(uri, "input_anim.gif")

        try {
            val outputFile = createOutputWebpFile("sticker_gif")

            for ((index, profile) in animatedProfiles.withIndex()) {
                onProgress(
                    0.25f + (index * 0.15f),
                    "Optimizing animated GIF (${profile.fps} FPS)..."
                )

                val scaleFilter = build512ScaleFilter(profile.fps, fitMode)
                val command = buildString {
                    append("-y ")
                    append("-t 10.0 ")
                    append("-i \"${tempInputFile.absolutePath}\" ")
                    append("-vf \"$scaleFilter\" ")
                    append("-vcodec libwebp ")
                    append("-lossless 0 ")
                    append("-compression_level ${profile.compressionLevel} ")
                    append("-q:v ${profile.quality} ")
                    append("-loop 0 ")
                    append("-preset default ")
                    append("-an -vsync 0 ")
                    append("\"${outputFile.absolutePath}\"")
                }

                executeFfmpegCommand(
                    command = command,
                    totalDurationMs = 3_000L,
                    onProgress = { passProgress ->
                        val mapped = (0.25f + passProgress * 0.65f).coerceAtMost(0.95f)
                        onProgress(mapped, "Converting GIF to sticker...")
                    }
                )

                if (outputFile.exists() && outputFile.length() in 1..(MAX_ANIMATED_SIZE_BYTES - 512)) {
                    val withExif = injectWhatsAppExifChunk(outputFile.readBytes())
                    if (withExif.size <= MAX_ANIMATED_SIZE_BYTES) {
                        outputFile.writeBytes(withExif)
                        onProgress(1.0f, "Animated sticker ready!")
                        return@withContext outputFile
                    }
                }
            }

            if (outputFile.exists()) outputFile.delete()
            throw StickerConversionException(
                message = "GIF exceeds WhatsApp's 500 KB animated sticker limit.",
                isSizeLimitError = true
            )
        } finally {
            if (tempInputFile.exists()) tempInputFile.delete()
        }
    }

    suspend fun convertToStickerOutput(
        mediaInfo: SelectedMediaInfo,
        onProgress: (Float, String) -> Unit
    ): StickerOutput = withContext(Dispatchers.IO) {
        val outputFile = when (mediaInfo.inputType) {
            MediaInputType.STATIC_IMAGE -> convertStaticImageToWebp(
                uri = mediaInfo.uri,
                fitMode = mediaInfo.fitMode,
                stickerText = mediaInfo.stickerText,
                onProgress = onProgress
            )
            MediaInputType.VIDEO -> convertVideoToAnimatedWebp(
                uri = mediaInfo.uri,
                startMs = mediaInfo.trimStartMs,
                endMs = mediaInfo.trimEndMs,
                fitMode = mediaInfo.fitMode,
                onProgress = onProgress
            )
            MediaInputType.ANIMATED_GIF -> convertGifToAnimatedWebp(
                uri = mediaInfo.uri,
                fitMode = mediaInfo.fitMode,
                onProgress = onProgress
            )
        }

        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            outputFile
        )

        StickerOutput(
            file = outputFile,
            contentUri = contentUri,
            inputType = mediaInfo.inputType,
            widthPx = STICKER_DIMENSION_PX,
            heightPx = STICKER_DIMENSION_PX,
            sizeBytes = outputFile.length(),
            durationMs = if (mediaInfo.inputType == MediaInputType.VIDEO) mediaInfo.trimmedDurationMs else 0L,
            encodedFps = if (mediaInfo.inputType.isAnimated) 15 else 0,
            encodedQuality = if (mediaInfo.inputType.isAnimated) 75 else 88,
            isAnimated = mediaInfo.inputType.isAnimated,
            loopCount = 0
        )
    }

    /**
     * Injects WhatsApp's official Sticker Pack EXIF JSON metadata chunk into a RIFF WebP container
     * and sets the VP8X Extended Format EXIF bit (0x08) so WhatsApp recognizes single shared .webp
     * files and pack assets as native WhatsApp Stickers.
     */
    fun injectWhatsAppExifChunk(
        webpBytes: ByteArray,
        packId: String = StickerContentProvider.DEFAULT_PACK_ID,
        packName: String = StickerContentProvider.DEFAULT_PACK_NAME,
        publisher: String = StickerContentProvider.DEFAULT_PACK_PUBLISHER
    ): ByteArray {
        if (webpBytes.size < 16) return webpBytes
        val riffHeader = String(webpBytes, 0, 4, StandardCharsets.US_ASCII)
        val webpHeader = String(webpBytes, 8, 4, StandardCharsets.US_ASCII)
        if (riffHeader != "RIFF" || webpHeader != "WEBP") return webpBytes

        val jsonPayload = "{\"sticker-pack-id\":\"$packId\",\"sticker-pack-name\":\"$packName\",\"sticker-pack-publisher\":\"$publisher\",\"emojis\":[\"😀\"]}"
        val jsonBytes = jsonPayload.toByteArray(StandardCharsets.UTF_8)

        // Minimal TIFF/EXIF header wrapping the WhatsApp JSON string in tag 0x5741 ("WA")
        val exifPrefix = byteArrayOf(
            0x49, 0x49, 0x2A, 0x00, 0x08, 0x00, 0x00, 0x00,
            0x01, 0x00, 0x41, 0x57, 0x07, 0x00,
            (jsonBytes.size and 0xFF).toByte(),
            ((jsonBytes.size shr 8) and 0xFF).toByte(),
            ((jsonBytes.size shr 16) and 0xFF).toByte(),
            ((jsonBytes.size shr 24) and 0xFF).toByte(),
            0x1A, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
        )
        val exifData = exifPrefix + jsonBytes
        val paddedExifLen = if (exifData.size % 2 == 1) exifData.size + 1 else exifData.size

        val firstChunkFourCC = String(webpBytes, 12, 4, StandardCharsets.US_ASCII)
        val bodyStream = ByteArrayOutputStream()

        if (firstChunkFourCC == "VP8X") {
            // Already has VP8X header; set EXIF bit (bit 3 = 0x08) in flags byte at offset 20
            val modified = webpBytes.copyOf()
            modified[20] = (modified[20].toInt() or 0x08).toByte()
            bodyStream.write(modified, 12, modified.size - 12)
        } else {
            // Prepend a 18-byte VP8X chunk for 512x512 canvas (511 = 0x0001FF in 24-bit LE) with Alpha + EXIF flags (0x18)
            val vp8xChunk = byteArrayOf(
                'V'.code.toByte(), 'P'.code.toByte(), '8'.code.toByte(), 'X'.code.toByte(),
                10, 0, 0, 0,
                0x18, 0, 0, 0,
                0xFF.toByte(), 0x01, 0x00, // Width - 1 = 511
                0xFF.toByte(), 0x01, 0x00  // Height - 1 = 511
            )
            bodyStream.write(vp8xChunk)
            bodyStream.write(webpBytes, 12, webpBytes.size - 12)
        }

        // Append EXIF chunk
        bodyStream.write("EXIF".toByteArray(StandardCharsets.US_ASCII))
        val lenBuf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(exifData.size).array()
        bodyStream.write(lenBuf)
        bodyStream.write(exifData)
        if (paddedExifLen > exifData.size) {
            bodyStream.write(0)
        }

        val webpBody = bodyStream.toByteArray()
        val riffSize = 4 + webpBody.size
        val out = ByteArrayOutputStream(8 + riffSize)
        out.write("RIFF".toByteArray(StandardCharsets.US_ASCII))
        out.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(riffSize).array())
        out.write("WEBP".toByteArray(StandardCharsets.US_ASCII))
        out.write(webpBody)
        return out.toByteArray()
    }

    private fun build512ScaleFilter(fps: Int, fitMode: ScaleFitMode): String {
        return when (fitMode) {
            ScaleFitMode.CENTER_CROP -> {
                "fps=$fps,scale=512:512:force_original_aspect_ratio=increase,crop=512:512,format=yuva420p"
            }
            ScaleFitMode.FIT_CENTER -> {
                "fps=$fps,scale=512:512:force_original_aspect_ratio=decrease,pad=512:512:(ow-iw)/2:(oh-ih)/2:color=0x00000000,format=yuva420p"
            }
        }
    }

    private suspend fun executeFfmpegCommand(
        command: String,
        totalDurationMs: Long,
        onProgress: (Float) -> Unit
    ) = suspendCancellableCoroutine<Unit> { continuation ->
        FFmpegKitConfig.enableStatisticsCallback { stats ->
            if (totalDurationMs > 0) {
                val fraction = (stats.time.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
                onProgress(fraction)
            }
        }

        val session = FFmpegKit.executeAsync(command) { completedSession ->
            val returnCode = completedSession.returnCode
            if (ReturnCode.isSuccess(returnCode)) {
                if (continuation.isActive) continuation.resume(Unit)
            } else if (ReturnCode.isCancel(returnCode)) {
                if (continuation.isActive) {
                    continuation.resumeWithException(StickerConversionException("Conversion cancelled."))
                }
            } else {
                if (continuation.isActive) {
                    continuation.resumeWithException(StickerConversionException("Video conversion failed."))
                }
            }
        }

        continuation.invokeOnCancellation {
            FFmpegKit.cancel(session.sessionId)
        }
    }

    private fun decodeBitmapFromUri(uri: Uri): Bitmap {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = true
                }
            } else {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)
                } ?: throw StickerConversionException("Unable to read selected image.")
            }
        } catch (e: Exception) {
            throw StickerConversionException("Could not open image file.", cause = e)
        }
    }

    /**
     * Resizes and normalizes any Bitmap onto an exact 512x512 ARGB_8888 canvas,
     * preserving an alpha channel so WhatsApp recognizes the output as a sticker.
     */
    private fun transformBitmapTo512x512(
        source: Bitmap,
        fitMode: ScaleFitMode,
        stickerText: String = ""
    ): Bitmap {
        val targetSize = STICKER_DIMENSION_PX
        val output = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        output.setHasAlpha(true)
        val canvas = Canvas(output)
        canvas.drawColor(Color.TRANSPARENT)

        val srcWidth = source.width.toFloat()
        val srcHeight = source.height.toFloat()
        if (srcWidth <= 0f || srcHeight <= 0f) return output

        val scale = when (fitMode) {
            ScaleFitMode.CENTER_CROP -> max(targetSize / srcWidth, targetSize / srcHeight)
            ScaleFitMode.FIT_CENTER -> min(targetSize / srcWidth, targetSize / srcHeight)
        }

        val scaledWidth = srcWidth * scale
        val scaledHeight = srcHeight * scale
        val left = (targetSize - scaledWidth) / 2f
        val top = (targetSize - scaledHeight) / 2f

        val destRect = RectF(left, top, left + scaledWidth, top + scaledHeight)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
        canvas.drawBitmap(source, null, destRect, paint)

        // ponytail: native Canvas drawText with stroke + fill meme styling; no heavy text engine needed
        drawTextOverlayOnCanvas(canvas, stickerText)

        // Ensure corner pixel (0,0) has alpha < 255 so Android's WebP encoder always emits an ALPH chunk
        val cornerPixel = output.getPixel(0, 0)
        if (Color.alpha(cornerPixel) == 255) {
            output.setPixel(0, 0, Color.argb(254, Color.red(cornerPixel), Color.green(cornerPixel), Color.blue(cornerPixel)))
        }
        return output
    }

    private fun drawTextOverlayOnCanvas(canvas: Canvas, text: String) {
        if (text.isBlank()) return

        val lines = wrapText(text, maxCharsPerLine = 16)
        if (lines.isEmpty()) return

        val baseSize = when {
            lines.size >= 4 -> 24f
            lines.size == 3 -> 30f
            lines.any { it.length > 14 } -> 34f
            else -> 42f
        }

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 7f
            color = Color.BLACK
            textSize = baseSize
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.WHITE
            textSize = baseSize
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val lineHeight = baseSize * 1.25f
        val startY = 512f - 24f - ((lines.size - 1) * lineHeight)
        val centerX = 256f

        lines.forEachIndexed { i, line ->
            val y = startY + (i * lineHeight)
            canvas.drawText(line, centerX, y, strokePaint)
            canvas.drawText(line, centerX, y, fillPaint)
        }
    }

    private fun wrapText(text: String, maxCharsPerLine: Int): List<String> {
        val result = mutableListOf<String>()
        val paragraphs = text.trim().split("\n")
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
        return result.take(5)
    }

    private fun extractVideoDurationMs(uri: Uri): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            durationStr?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            throw StickerConversionException("Could not read video duration.", cause = e)
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }

    private fun queryFileDisplayNameAndSize(resolver: ContentResolver, uri: Uri): Pair<String, Long> {
        var name = "sticker_media"
        var size = 0L
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIndex >= 0 && !cursor.isNull(nameIndex)) name = cursor.getString(nameIndex)
                    if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
                }
            }
        return name to size
    }

    private fun copyUriToTempFile(uri: Uri, suffixName: String): File {
        val tempDir = File(context.cacheDir, STICKER_CACHE_FOLDER).apply { mkdirs() }
        val tempFile = File(tempDir, "tmp_${UUID.randomUUID()}_$suffixName")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output -> input.copyTo(output) }
        } ?: throw IOException("Cannot read selected media.")
        return tempFile
    }

    private fun createOutputWebpFile(prefix: String): File {
        val stickerDir = File(context.cacheDir, STICKER_CACHE_FOLDER).apply { mkdirs() }
        // ponytail: prune draft previews keeping only the 5 most recent files to prevent cache buildup
        stickerDir.listFiles { f -> f.extension.equals("webp", ignoreCase = true) }
            ?.sortedByDescending { it.lastModified() }
            ?.drop(5)
            ?.forEach { it.delete() }
        return File(stickerDir, "${prefix}_${System.currentTimeMillis()}.webp")
    }
}
