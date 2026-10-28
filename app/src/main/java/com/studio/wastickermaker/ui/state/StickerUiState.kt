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

data class StickerScreenState(
    val selectedMedia: SelectedMediaInfo? = null,
    val conversionState: StickerUiState = StickerUiState.Idle,
    val savedPackCount: Int = 0,
    val packStickers: List<File> = emptyList()
)
