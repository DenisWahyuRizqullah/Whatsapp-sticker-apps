package com.studio.wastickermaker.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.studio.wastickermaker.data.model.AppLanguage
import com.studio.wastickermaker.data.model.AppSettings
import com.studio.wastickermaker.data.model.AppThemeMode
import com.studio.wastickermaker.data.model.MediaInputType
import com.studio.wastickermaker.data.model.ScaleFitMode
import com.studio.wastickermaker.data.model.WhatsAppTarget
import com.studio.wastickermaker.data.repository.SettingsRepository
import com.studio.wastickermaker.data.repository.StickerRepository
import com.studio.wastickermaker.ui.state.StickerPackSegment
import com.studio.wastickermaker.ui.state.StickerScreenState
import com.studio.wastickermaker.ui.state.StickerUiState
import com.studio.wastickermaker.ui.theme.AppStrings
import com.studio.wastickermaker.util.StickerContentProvider
import com.studio.wastickermaker.util.StickerConversionException
import com.studio.wastickermaker.util.StickerConverter
import java.io.File
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StickerViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = StickerRepository(application.applicationContext)
    private val settingsRepository = SettingsRepository(application.applicationContext)

    private val _screenState = MutableStateFlow(
        StickerScreenState(settings = settingsRepository.loadSettings())
    )
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

    fun setSettingsOpen(open: Boolean) {
        _screenState.update { it.copy(isSettingsOpen = open) }
    }

    fun onLanguageChanged(language: AppLanguage) {
        updateSettings { it.copy(language = language) }
    }

    fun onThemeModeChanged(themeMode: AppThemeMode) {
        updateSettings { it.copy(themeMode = themeMode) }
    }

    fun onResolutionPercentChanged(resolutionPercent: Int) {
        val clamped = resolutionPercent.coerceIn(50, 100)
        if (_screenState.value.settings.resolutionPercent == clamped) return
        updateSettings { it.copy(resolutionPercent = clamped) }
        if (_screenState.value.selectedMedia != null) {
            _screenState.update { it.copy(isCurrentStickerSaved = false) }
            convertSelectedMedia()
        }
    }

    fun onMaxDurationSecChanged(durationSec: Int) {
        val clamped = durationSec.coerceIn(2, 10)
        if (_screenState.value.settings.maxAnimatedDurationSec == clamped) return
        updateSettings { it.copy(maxAnimatedDurationSec = clamped) }

        val currentMedia = _screenState.value.selectedMedia ?: return
        val maxMs = clamped * 1000L
        if (currentMedia.inputType == MediaInputType.VIDEO) {
            val newEnd = currentMedia.trimEndMs.coerceAtMost(
                (currentMedia.trimStartMs + maxMs).coerceAtMost(currentMedia.totalDurationMs)
            )
            _screenState.update {
                it.copy(
                    selectedMedia = currentMedia.copy(trimEndMs = newEnd),
                    isCurrentStickerSaved = false
                )
            }
            convertSelectedMedia()
        } else if (currentMedia.inputType == MediaInputType.ANIMATED_GIF) {
            _screenState.update { it.copy(isCurrentStickerSaved = false) }
            convertSelectedMedia()
        }
    }

    fun onDefaultFitModeChanged(fitMode: ScaleFitMode) {
        updateSettings { it.copy(defaultFitMode = fitMode) }
    }

    fun onWhatsAppTargetChanged(target: WhatsAppTarget) {
        updateSettings { it.copy(whatsAppTarget = target) }
    }

    fun onCustomPublisherChanged(publisher: String) {
        updateSettings { it.copy(customPublisher = publisher.take(36)) }
    }

    fun clearPreviewCache(onCleared: (String) -> Unit) {
        viewModelScope.launch {
            val freedBytes = repository.clearPreviewCache(keepFile = _screenState.value.previewFile)
            val freedKb = String.format(Locale.US, "%.1f", freedBytes / 1024.0)
            onCleared(freedKb)
        }
    }

    private fun updateSettings(transform: (AppSettings) -> AppSettings) {
        val updated = transform(_screenState.value.settings)
        _screenState.update { it.copy(settings = updated) }
        settingsRepository.saveSettings(updated)
    }

    fun onSegmentSelected(segment: StickerPackSegment) {
        _screenState.update { it.copy(selectedSegment = segment) }
        viewModelScope.launch {
            val isSaved = repository.isStickerInPack(_screenState.value.previewFile, segment.packId)
            _screenState.update { it.copy(isCurrentStickerSaved = isSaved) }
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
                val isCurrentStillInPack = repository.isStickerInPack(_screenState.value.previewFile, currentSegment.packId)
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
                val currentSettings = _screenState.value.settings
                val strings = AppStrings.forLanguage(currentSettings.language)
                val mediaInfo = repository.inspectSelectedUri(uri, currentSettings)
                val targetSegment = if (mediaInfo.inputType.isAnimated) StickerPackSegment.ANIMATED else StickerPackSegment.STATIC
                val currentSegment = _screenState.value.selectedSegment
                val notice = if (targetSegment != currentSegment) {
                    if (targetSegment == StickerPackSegment.ANIMATED) {
                        strings.switchToAnimatedNotice
                    } else {
                        strings.switchToStaticNotice
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
            delay(350L) // ponytail: debounce typing to eliminate photo flickering
            convertSelectedMedia()
        }
    }

    fun onVideoTrimRangeChanged(startMs: Long, endMs: Long) {
        val current = _screenState.value.selectedMedia ?: return
        if (current.inputType != MediaInputType.VIDEO) return
        val maxDurationMs = _screenState.value.settings.maxAnimatedDurationMs

        val (safeStart, safeEnd) = StickerConverter.computeSafeTrimRange(
            totalDurationMs = current.totalDurationMs,
            startMs = startMs,
            endMs = endMs,
            maxDurationMs = maxDurationMs
        )

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
        val currentSettings = _screenState.value.settings

        if (media.inputType == MediaInputType.VIDEO &&
            media.trimmedDurationMs > currentSettings.maxAnimatedDurationMs
        ) {
            _screenState.update {
                it.copy(
                    conversionState = StickerUiState.Error(
                        title = "Video Too Long",
                        message = "Please trim the video to ${currentSettings.maxAnimatedDurationSec} seconds or less.",
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
                val output = repository.convertMediaToSticker(media, currentSettings) { progress, stage ->
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
                        conversionState = StickerUiState.Success(stickerOutput = output),
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
