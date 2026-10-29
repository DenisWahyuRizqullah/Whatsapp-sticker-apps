package com.studio.wastickermaker.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.studio.wastickermaker.data.model.MediaInputType
import com.studio.wastickermaker.data.model.ScaleFitMode
import com.studio.wastickermaker.data.repository.StickerRepository
import com.studio.wastickermaker.ui.state.StickerPackSegment
import com.studio.wastickermaker.ui.state.StickerScreenState
import com.studio.wastickermaker.ui.state.StickerUiState
import com.studio.wastickermaker.util.StickerContentProvider
import com.studio.wastickermaker.util.StickerConversionException
import com.studio.wastickermaker.util.StickerConverter
import java.io.File
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StickerViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = StickerRepository(application.applicationContext)

    private val _screenState = MutableStateFlow(StickerScreenState())
    val screenState: StateFlow<StickerScreenState> = _screenState.asStateFlow()

    private var activeConversionJob: Job? = null
    private var textDebounceJob: Job? = null

    init {
        loadLocalPacks()
    }

    fun loadLocalPacks() {
        viewModelScope.launch {
            try {
                val staticList = repository.getLocalPackStickers(StickerContentProvider.STATIC_PACK_ID)
                val animatedList = repository.getLocalPackStickers(StickerContentProvider.ANIMATED_PACK_ID)
                _screenState.update { state ->
                    state.copy(
                        staticPackStickers = staticList,
                        animatedPackStickers = animatedList
                    )
                }
            } catch (_: Throwable) {}
        }
    }

    fun onSegmentSelected(segment: StickerPackSegment) {
        _screenState.update { state ->
            val preview = state.previewFile
            val targetList = if (segment == StickerPackSegment.STATIC) state.staticPackStickers else state.animatedPackStickers
            val isCurrentSaved = preview != null && targetList.any {
                it.length() == preview.length() && it.readBytes().contentEquals(preview.readBytes())
            }
            state.copy(
                selectedSegment = segment,
                isCurrentStickerSaved = isCurrentSaved
            )
        }
    }

    fun clearSegmentNotice() {
        _screenState.update { it.copy(segmentSwitchNotice = null) }
    }

    fun deleteSticker(file: File) {
        viewModelScope.launch {
            try {
                val currentSegment = _screenState.value.selectedSegment
                val updated = repository.deleteStickerFromLocalPack(file, currentSegment.packId)
                val currentPreview = _screenState.value.previewFile
                val isCurrentStillInPack = currentPreview != null && updated.any {
                    it.length() == currentPreview.length() && it.readBytes().contentEquals(currentPreview.readBytes())
                }
                _screenState.update { state ->
                    if (currentSegment == StickerPackSegment.STATIC) {
                        state.copy(
                            staticPackStickers = updated,
                            isCurrentStickerSaved = if (!isCurrentStillInPack) false else state.isCurrentStickerSaved
                        )
                    } else {
                        state.copy(
                            animatedPackStickers = updated,
                            isCurrentStickerSaved = if (!isCurrentStillInPack) false else state.isCurrentStickerSaved
                        )
                    }
                }
            } catch (t: Throwable) {
                _screenState.update { state ->
                    state.copy(
                        conversionState = StickerUiState.Error(
                            title = "Gagal Menghapus Stiker",
                            message = t.localizedMessage ?: "Tidak dapat menghapus stiker."
                        )
                    )
                }
            }
        }
    }

    fun onMediaUriSelected(uri: Uri?) {
        if (uri == null) return
        activeConversionJob?.cancel()

        viewModelScope.launch {
            _screenState.update {
                it.copy(
                    conversionState = StickerUiState.Processing(
                        progress = 0.1f,
                        stageMessage = "Reading file..."
                    )
                )
            }

            try {
                val mediaInfo = repository.inspectSelectedUri(uri)
                val targetSegment = if (mediaInfo.inputType.isAnimated) StickerPackSegment.ANIMATED else StickerPackSegment.STATIC
                val currentSegment = _screenState.value.selectedSegment
                val notice = if (targetSegment != currentSegment) {
                    if (targetSegment == StickerPackSegment.ANIMATED) {
                        "Beralih ke segmen Stiker Gerak. Stiker foto Anda tetap aman tersimpan!"
                    } else {
                        "Beralih ke segmen Stiker Foto. Stiker gerak Anda tetap aman tersimpan!"
                    }
                } else null

                _screenState.update {
                    it.copy(
                        selectedMedia = mediaInfo,
                        selectedSegment = targetSegment,
                        segmentSwitchNotice = notice,
                        conversionState = StickerUiState.Idle
                    )
                }
                convertSelectedMedia()
            } catch (e: StickerConversionException) {
                _screenState.update {
                    it.copy(
                        conversionState = StickerUiState.Error(
                            title = "Could Not Open File",
                            message = e.message ?: "Please select a valid Photo, GIF, or Video.",
                            isSizeError = e.isSizeLimitError,
                            isDurationError = e.isDurationLimitError
                        )
                    )
                }
            } catch (t: Throwable) {
                _screenState.update {
                    it.copy(
                        conversionState = StickerUiState.Error(
                            title = "Could Not Open File",
                            message = t.localizedMessage ?: "An unexpected error occurred."
                        )
                    )
                }
            }
        }
    }

    fun onStickerTextChanged(text: String) {
        val current = _screenState.value.selectedMedia ?: return
        if (current.stickerText == text) return
        _screenState.update { state ->
            state.copy(
                selectedMedia = current.copy(stickerText = text),
                isCurrentStickerSaved = false
            )
        }
        textDebounceJob?.cancel()
        textDebounceJob = viewModelScope.launch {
            kotlinx.coroutines.delay(350L) // ponytail: debounce typing to eliminate photo flickering
            convertSelectedMedia()
        }
    }

    fun onVideoTrimRangeChanged(startMs: Long, endMs: Long) {
        val current = _screenState.value.selectedMedia ?: return
        if (current.inputType != MediaInputType.VIDEO) return

        val safeStart = startMs.coerceIn(0L, current.totalDurationMs)
        val minEnd = (safeStart + 500L).coerceAtMost(current.totalDurationMs)
        var safeEnd = endMs.coerceIn(minEnd, current.totalDurationMs)

        if (safeEnd - safeStart > StickerConverter.MAX_VIDEO_TRIM_DURATION_MS) {
            safeEnd = (safeStart + StickerConverter.MAX_VIDEO_TRIM_DURATION_MS)
                .coerceAtMost(current.totalDurationMs)
        }

        _screenState.update { state ->
            state.copy(
                selectedMedia = current.copy(
                    trimStartMs = safeStart,
                    trimEndMs = safeEnd
                )
            )
        }
    }

    fun onScaleFitModeChanged(fitMode: ScaleFitMode) {
        val current = _screenState.value.selectedMedia ?: return
        _screenState.update { state ->
            state.copy(
                selectedMedia = current.copy(fitMode = fitMode),
                isCurrentStickerSaved = false
            )
        }
        convertSelectedMedia()
    }

    suspend fun saveCurrentStickerToPack(): Boolean {
        val currentConversion = _screenState.value.conversionState
        if (currentConversion !is StickerUiState.Success) return false
        if (_screenState.value.isCurrentStickerSaved) return true

        return try {
            val isAnimated = currentConversion.stickerOutput.isAnimated
            val targetSegment = if (isAnimated) StickerPackSegment.ANIMATED else StickerPackSegment.STATIC
            val packId = targetSegment.packId

            repository.addStickerToLocalPack(currentConversion.stickerOutput, packId)
            val staticList = repository.getLocalPackStickers(StickerContentProvider.STATIC_PACK_ID)
            val animatedList = repository.getLocalPackStickers(StickerContentProvider.ANIMATED_PACK_ID)

            _screenState.update { state ->
                state.copy(
                    selectedSegment = targetSegment,
                    staticPackStickers = staticList,
                    animatedPackStickers = animatedList,
                    isCurrentStickerSaved = true
                )
            }
            true
        } catch (t: Throwable) {
            _screenState.update { state ->
                state.copy(
                    conversionState = StickerUiState.Error(
                        title = "Gagal Menyimpan",
                        message = t.localizedMessage ?: "Tidak dapat menyimpan stiker."
                    )
                )
            }
            false
        }
    }

    fun onSaveCurrentStickerClicked() {
        viewModelScope.launch {
            saveCurrentStickerToPack()
        }
    }

    fun convertSelectedMedia() {
        val media = _screenState.value.selectedMedia ?: return

        if (media.inputType == MediaInputType.VIDEO &&
            media.trimmedDurationMs > StickerConverter.MAX_VIDEO_TRIM_DURATION_MS
        ) {
            _screenState.update {
                it.copy(
                    conversionState = StickerUiState.Error(
                        title = "Video Too Long",
                        message = "Please trim the video to 6 seconds or less.",
                        isDurationError = true
                    )
                )
            }
            return
        }

        activeConversionJob?.cancel()
        activeConversionJob = viewModelScope.launch {
            _screenState.update {
                it.copy(
                    conversionState = StickerUiState.Processing(
                        progress = 0.1f,
                        stageMessage = "Creating sticker..."
                    )
                )
            }

            try {
                val output = repository.convertMediaToSticker(media) { progress, stage ->
                    _screenState.update { state ->
                        state.copy(
                            conversionState = StickerUiState.Processing(
                                progress = progress.coerceIn(0f, 1f),
                                stageMessage = stage
                            )
                        )
                    }
                }

                _screenState.update { state ->
                    state.copy(
                        conversionState = StickerUiState.Success(
                            stickerOutput = output,
                            packStickerCount = state.packStickers.size
                        ),
                        previewFile = output.file,
                        isCurrentStickerSaved = false
                    )
                }
            } catch (e: StickerConversionException) {
                _screenState.update { state ->
                    state.copy(
                        conversionState = StickerUiState.Error(
                            title = "Could Not Create Sticker",
                            message = e.message ?: "Sticker conversion failed.",
                            isSizeError = e.isSizeLimitError,
                            isDurationError = e.isDurationLimitError
                        )
                    )
                }
            } catch (t: Throwable) {
                _screenState.update { state ->
                    state.copy(
                        conversionState = StickerUiState.Error(
                            title = "Error",
                            message = t.localizedMessage ?: "Failed to create sticker."
                        )
                    )
                }
            }
        }
    }

    fun resetState() {
        activeConversionJob?.cancel()
        textDebounceJob?.cancel()
        _screenState.update {
            it.copy(
                selectedMedia = null,
                conversionState = StickerUiState.Idle,
                previewFile = null,
                isCurrentStickerSaved = false
            )
        }
    }
}
