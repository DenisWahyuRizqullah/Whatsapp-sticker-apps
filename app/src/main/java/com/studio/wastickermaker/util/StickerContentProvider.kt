package com.studio.wastickermaker.util

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.studio.wastickermaker.BuildConfig
import com.studio.wastickermaker.data.repository.SettingsRepository
import java.io.File
import java.io.FileNotFoundException
import java.nio.charset.StandardCharsets

/**
 * Official WhatsApp Sticker Pack ContentProvider serving pack metadata, sticker list cursors,
 * and 512x512 .webp + 96x96 tray_icon.webp assets to WhatsApp.
 */
class StickerContentProvider : ContentProvider() {

    companion object {
        const val STATIC_PACK_ID = "cobalt_custom_pack_static"
        const val STATIC_PACK_NAME = "Koleksi Stiker Foto"
        const val ANIMATED_PACK_ID = "cobalt_custom_pack_animated"
        const val ANIMATED_PACK_NAME = "Koleksi Stiker Gerak"
        const val DEFAULT_PACK_PUBLISHER = "Sticker Maker"

        const val STICKER_PACK_IDENTIFIER_IN_QUERY = "sticker_pack_identifier"
        const val STICKER_PACK_NAME_IN_QUERY = "sticker_pack_name"
        const val STICKER_PACK_PUBLISHER_IN_QUERY = "sticker_pack_publisher"
        const val STICKER_PACK_ICON_IN_QUERY = "sticker_pack_icon"
        const val ANDROID_APP_DOWNLOAD_LINK_IN_QUERY = "android_play_store_link"
        const val IOS_APP_DOWNLOAD_LINK_IN_QUERY = "ios_app_download_link"
        const val PUBLISHER_EMAIL = "sticker_pack_publisher_email"
        const val PUBLISHER_WEBSITE = "sticker_pack_publisher_website"
        const val PRIVACY_POLICY_WEBSITE = "sticker_pack_privacy_policy_website"
        const val LICENSE_AGREEMENT_WEBSITE = "sticker_pack_license_agreement_website"
        const val IMAGE_DATA_VERSION = "image_data_version"
        const val AVOID_CACHE = "whatsapp_will_not_cache_stickers"
        const val ANIMATED_STICKER_PACK = "animated_sticker_pack"

        const val STICKER_FILE_NAME_IN_QUERY = "sticker_file_name"
        const val STICKER_EMOJI_IN_QUERY = "sticker_emoji"
        const val STICKER_ACCESSIBILITY_TEXT_IN_QUERY = "sticker_accessibility_text"

        private const val METADATA = "metadata"
        private const val STICKERS = "stickers"
        private const val STICKERS_ASSET = "stickers_asset"

        private const val METADATA_CODE = 1
        private const val METADATA_CODE_FOR_SINGLE_PACK = 2
        private const val STICKERS_CODE = 3
        private const val STICKERS_ASSET_CODE = 4

        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH)
    }

    override fun onCreate(): Boolean {
        val authority = BuildConfig.CONTENT_PROVIDER_AUTHORITY
        uriMatcher.addURI(authority, METADATA, METADATA_CODE)
        uriMatcher.addURI(authority, "$METADATA/*", METADATA_CODE_FOR_SINGLE_PACK)
        uriMatcher.addURI(authority, "$STICKERS/*", STICKERS_CODE)
        uriMatcher.addURI(authority, "$STICKERS_ASSET/*/*", STICKERS_ASSET_CODE)
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        return when (uriMatcher.match(uri)) {
            METADATA_CODE, METADATA_CODE_FOR_SINGLE_PACK -> buildPackMetadataCursor(uri)
            STICKERS_CODE -> buildStickersListCursor(uri)
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }

    private fun buildPackMetadataCursor(uri: Uri): Cursor {
        val columns = arrayOf(
            STICKER_PACK_IDENTIFIER_IN_QUERY,
            STICKER_PACK_NAME_IN_QUERY,
            STICKER_PACK_PUBLISHER_IN_QUERY,
            STICKER_PACK_ICON_IN_QUERY,
            ANDROID_APP_DOWNLOAD_LINK_IN_QUERY,
            IOS_APP_DOWNLOAD_LINK_IN_QUERY,
            PUBLISHER_EMAIL,
            PUBLISHER_WEBSITE,
            PRIVACY_POLICY_WEBSITE,
            LICENSE_AGREEMENT_WEBSITE,
            IMAGE_DATA_VERSION,
            AVOID_CACHE,
            ANIMATED_STICKER_PACK
        )
        val cursor = MatrixCursor(columns)

        val requestedPackId = if (uriMatcher.match(uri) == METADATA_CODE_FOR_SINGLE_PACK) {
            uri.lastPathSegment
        } else {
            null
        }

        val packsToQuery = if (requestedPackId != null) {
            listOf(requestedPackId)
        } else {
            listOf(STATIC_PACK_ID, ANIMATED_PACK_ID)
        }

        for (packId in packsToQuery) {
            val packDir = File(context?.filesDir, "sticker_packs/$packId")
            val stickerFiles = packDir.listFiles { f ->
                f.isFile && f.extension.equals("webp", ignoreCase = true) &&
                    (f.name.startsWith("user_sticker_") || f.name.startsWith("autofill_sticker_"))
            }.orEmpty()

            // Only expose non-empty packs in the all-packs query to prevent WhatsApp empty pack errors
            if (stickerFiles.isEmpty() && requestedPackId == null) {
                continue
            }

            val versionFile = File(packDir, "version.txt")
            val dataVersion = if (versionFile.exists()) versionFile.readText().trim().ifEmpty { "1" } else "1"
            val isAnimated = packId == ANIMATED_PACK_ID || stickerFiles.any { isAnimatedWebpFile(it) }
            val packName = if (isAnimated) ANIMATED_PACK_NAME else STATIC_PACK_NAME
            val publisher = SettingsRepository.getCustomPublisher(context)

            cursor.addRow(
                arrayOf<Any>(
                    packId,
                    packName,
                    publisher,
                    "tray_icon.webp",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    dataVersion,
                    0,
                    if (isAnimated) 1 else 0
                )
            )
        }

        context?.contentResolver?.let { cursor.setNotificationUri(it, uri) }
        return cursor
    }

    private fun buildStickersListCursor(uri: Uri): Cursor {
        val packId = uri.lastPathSegment ?: STATIC_PACK_ID
        val packDir = File(context?.filesDir, "sticker_packs/$packId")
        val webpFiles = packDir.listFiles { file ->
            file.isFile && file.extension.equals("webp", ignoreCase = true) &&
                (file.name.startsWith("user_sticker_") || file.name.startsWith("autofill_sticker_"))
        }?.sortedWith(compareBy({ !it.name.startsWith("user_sticker_") }, { it.lastModified() }, { it.name })).orEmpty()

        val cursor = MatrixCursor(
            arrayOf(
                STICKER_FILE_NAME_IN_QUERY,
                STICKER_EMOJI_IN_QUERY,
                STICKER_ACCESSIBILITY_TEXT_IN_QUERY
            )
        )
        for (file in webpFiles) {
            cursor.addRow(arrayOf(file.name, "😀,✨", "Custom Sticker"))
        }
        context?.contentResolver?.let { cursor.setNotificationUri(it, uri) }
        return cursor
    }

    private fun isAnimatedWebpFile(file: File): Boolean {
        return try {
            val header = file.inputStream().use { input ->
                val buf = ByteArray(64)
                val read = input.read(buf)
                if (read > 0) String(buf, 0, read, StandardCharsets.US_ASCII) else ""
            }
            header.contains("ANIM") || header.contains("ANMF")
        } catch (_: Exception) {
            false
        }
    }

    override fun openAssetFile(uri: Uri, mode: String): AssetFileDescriptor? {
        if (uriMatcher.match(uri) != STICKERS_ASSET_CODE) {
            throw IllegalArgumentException("Unsupported asset URI: $uri")
        }
        val segments = uri.pathSegments
        if (segments.size < 3) return null
        val packId = segments[segments.size - 2]
        val fileName = segments[segments.size - 1]

        val packsRoot = File(context?.filesDir, "sticker_packs").canonicalFile
        val targetFile = File(packsRoot, "$packId/$fileName").canonicalFile
        if (!targetFile.path.startsWith(packsRoot.path + File.separator) ||
            !targetFile.extension.equals("webp", ignoreCase = true) ||
            !targetFile.exists()
        ) {
            throw FileNotFoundException("Sticker asset not found: $fileName")
        }
        val pfd = ParcelFileDescriptor.open(targetFile, ParcelFileDescriptor.MODE_READ_ONLY)
        return AssetFileDescriptor(pfd, 0, AssetFileDescriptor.UNKNOWN_LENGTH)
    }

    override fun getType(uri: Uri): String {
        return when (uriMatcher.match(uri)) {
            METADATA_CODE -> "vnd.android.cursor.dir/vnd.${BuildConfig.CONTENT_PROVIDER_AUTHORITY}.$METADATA"
            METADATA_CODE_FOR_SINGLE_PACK -> "vnd.android.cursor.item/vnd.${BuildConfig.CONTENT_PROVIDER_AUTHORITY}.$METADATA"
            STICKERS_CODE -> "vnd.android.cursor.dir/vnd.${BuildConfig.CONTENT_PROVIDER_AUTHORITY}.$STICKERS"
            STICKERS_ASSET_CODE -> "image/webp"
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri =
        throw UnsupportedOperationException("Read-only StickerContentProvider")

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int =
        throw UnsupportedOperationException("Read-only StickerContentProvider")

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = throw UnsupportedOperationException("Read-only StickerContentProvider")
}
