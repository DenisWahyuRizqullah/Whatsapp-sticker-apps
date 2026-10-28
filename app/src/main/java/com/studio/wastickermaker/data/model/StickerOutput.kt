package com.studio.wastickermaker.data.model

import android.net.Uri
import java.io.File

data class StickerOutput(
    val file: File,
    val contentUri: Uri,
    val inputType: MediaInputType,
    val widthPx: Int = 512,
    val heightPx: Int = 512,
    val sizeBytes: Long,
    val durationMs: Long,
    val encodedFps: Int,
    val encodedQuality: Int,
    val isAnimated: Boolean,
    val loopCount: Int = 0
) {
    val sizeKbFormatted: String
        get() = String.format("%.1f KB", sizeBytes / 1024.0)

    val maxBudgetKb: Int
        get() = if (isAnimated) 500 else 100
}
