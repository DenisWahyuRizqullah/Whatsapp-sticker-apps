package com.studio.wastickermaker.ui.state

import com.studio.wastickermaker.data.model.SelectedMediaInfo
import com.studio.wastickermaker.data.model.StickerOutput
import java.io.File

sealed interface StickerUiState {
    data object Idle : StickerUiState

    data class Processing(
        val progress: Float,
        val stageMessage: String
    ) : StickerUiState

    data class Success(
        val stickerOutput: StickerOutput,
        val packStickerCount: Int = 0
    ) : StickerUiState

    data class Error(
        val title: String,
        val message: String,
        val isSizeError: Boolean = false,
        val isDurationError: Boolean = false
    ) : StickerUiState
}

enum class StickerPackSegment(val label: String, val packId: String, val packName: String) {
    STATIC("Stiker Foto", "cobalt_custom_pack_static", "Koleksi Stiker Foto"),
    ANIMATED("Stiker Gerak", "cobalt_custom_pack_animated", "Koleksi Stiker Gerak")
}

data class StickerScreenState(
    val selectedMedia: SelectedMediaInfo? = null,
    val conversionState: StickerUiState = StickerUiState.Idle,
    val selectedSegment: StickerPackSegment = StickerPackSegment.STATIC,
    val staticPackStickers: List<File> = emptyList(),
    val animatedPackStickers: List<File> = emptyList(),
    val segmentSwitchNotice: String? = null,
    val previewFile: File? = null,
    val isCurrentStickerSaved: Boolean = false
) {
    val packStickers: List<File>
        get() = if (selectedSegment == StickerPackSegment.STATIC) staticPackStickers else animatedPackStickers

    val savedPackCount: Int
        get() = packStickers.size
}
