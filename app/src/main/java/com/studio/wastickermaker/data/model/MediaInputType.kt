package com.studio.wastickermaker.data.model

import android.net.Uri

enum class MediaInputType(
    val displayLabel: String,
    val isAnimated: Boolean,
    val maxAllowedBytes: Long
) {
    STATIC_IMAGE(
        displayLabel = "Photo",
        isAnimated = false,
        maxAllowedBytes = 100 * 1024L // 100 KB max for static WhatsApp sticker
    ),
    VIDEO(
        displayLabel = "Video",
        isAnimated = true,
        maxAllowedBytes = 500 * 1024L // 500 KB max for animated WhatsApp sticker
    ),
    ANIMATED_GIF(
        displayLabel = "GIF",
        isAnimated = true,
        maxAllowedBytes = 500 * 1024L // 500 KB max for animated WhatsApp sticker
    )
}

enum class ScaleFitMode(val label: String) {
    CENTER_CROP("Fill Square"),
    FIT_CENTER("Fit Whole Image")
}

data class SelectedMediaInfo(
    val uri: Uri,
    val mimeType: String,
    val inputType: MediaInputType,
    val displayName: String,
    val sourceSizeBytes: Long,
    val totalDurationMs: Long = 0L,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = 0L,
    val fitMode: ScaleFitMode = ScaleFitMode.CENTER_CROP,
    val stickerText: String = ""
) {
    val trimmedDurationMs: Long
        get() = (trimEndMs - trimStartMs).coerceAtLeast(0L)
}
