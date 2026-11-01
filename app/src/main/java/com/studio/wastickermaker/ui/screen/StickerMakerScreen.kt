package com.studio.wastickermaker.ui.screen

import android.app.Activity
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LibraryAdd
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import com.studio.wastickermaker.R
import com.studio.wastickermaker.data.model.AppLanguage
import com.studio.wastickermaker.data.model.AppSettings
import com.studio.wastickermaker.data.model.AppThemeMode
import com.studio.wastickermaker.data.model.MediaInputType
import com.studio.wastickermaker.data.model.ScaleFitMode
import com.studio.wastickermaker.data.model.WhatsAppTarget
import com.studio.wastickermaker.ui.state.AppNotice
import com.studio.wastickermaker.ui.state.AppNoticeTone
import com.studio.wastickermaker.ui.state.StickerPackSegment
import com.studio.wastickermaker.ui.state.StickerUiState
import com.studio.wastickermaker.ui.theme.AiryCobaltTokens
import com.studio.wastickermaker.ui.theme.AppStrings
import com.studio.wastickermaker.ui.theme.StickerStrings
import com.studio.wastickermaker.ui.theme.frostedBentoCard
import com.studio.wastickermaker.ui.viewmodel.StickerViewModel
import com.studio.wastickermaker.util.WhatsAppShareHelper
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StickerMakerScreen(
    viewModel: StickerViewModel = viewModel()
) {
    val context = LocalContext.current
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()
    val settings = screenState.settings
    val strings = AppStrings.forLanguage(settings.language)
    val scrollState = rememberScrollState()

    val coilImageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
                add(VideoFrameDecoder.Factory())
            }
            .build()
    }

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = PickVisualMedia()
    ) { uri ->
        viewModel.onMediaUriSelected(uri)
    }

    val addStickerPackLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.showNotice(strings.packAddedToast, AppNoticeTone.SUCCESS)
        }
    }

    val currentSegmentLabel = strings.segmentLabel(screenState.selectedSegment)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AiryCobaltTokens.SkyCanvasBrush)
    ) {
        if (screenState.isSettingsOpen) {
            BackHandler { viewModel.setSettingsOpen(false) }
            SettingsScreenContent(
                settings = settings,
                strings = strings,
                onBack = { viewModel.setSettingsOpen(false) },
                onLanguageSelected = viewModel::onLanguageChanged,
                onThemeSelected = viewModel::onThemeModeChanged,
                onResolutionSelected = viewModel::onResolutionPercentChanged,
                onMaxDurationSelected = viewModel::onMaxDurationSecChanged,
                onDefaultFitModeSelected = viewModel::onDefaultFitModeChanged,
                onWhatsAppTargetSelected = viewModel::onWhatsAppTargetChanged,
                onCustomPublisherChanged = viewModel::onCustomPublisherChanged,
                onClearCacheClicked = viewModel::clearPreviewCache
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 28.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
            // Top Header with Brand Logo Mark & Minimalist Right-Corner Settings Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .border(1.dp, AiryCobaltTokens.FrostedCardBorder, RoundedCornerShape(13.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher_background),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground),
                            contentDescription = strings.appTitle,
                            modifier = Modifier.size(56.dp),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.appTitle,
                            color = AiryCobaltTokens.DeepSlateHeading,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = strings.appSubtitle,
                            color = AiryCobaltTokens.MutedCoolGrey,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (screenState.selectedMedia != null) {
                        OutlinedButton(
                            onClick = { viewModel.resetState() },
                            shape = CircleShape,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = strings.newButton,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.newButton, fontSize = 12.sp, maxLines = 1)
                        }
                    }

                    IconButton(
                        onClick = { viewModel.setSettingsOpen(true) },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(AiryCobaltTokens.FrostedCardSurface)
                            .border(1.dp, AiryCobaltTokens.FrostedCardBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = strings.settingsButtonDesc,
                            tint = AiryCobaltTokens.DeepSlateHeading,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Step 1: Simple Upload Button Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .frostedBentoCard(24.dp)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        mediaPickerLauncher.launch(
                            PickVisualMediaRequest(PickVisualMedia.ImageAndVideo)
                        )
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AiryCobaltTokens.CobaltPrimary,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AddPhotoAlternate,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = strings.chooseMediaButton,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }

                // Selected File Indicator & Simple Fit Toggle
                screenState.selectedMedia?.let { media ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = media.displayName,
                            color = AiryCobaltTokens.DeepSlateHeading,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        )
                        Surface(
                            shape = CircleShape,
                            color = AiryCobaltTokens.PeriwinkleSoft
                        ) {
                            Text(
                                text = strings.mediaTypeLabel(media.inputType, settings.language),
                                color = AiryCobaltTokens.CobaltDeep,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ScaleFitMode.entries.forEach { mode ->
                            FilterChip(
                                selected = media.fitMode == mode,
                                onClick = { viewModel.onScaleFitModeChanged(mode) },
                                label = { Text(strings.fitModeLabel(mode, settings.language), fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AiryCobaltTokens.CobaltPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    if (media.inputType == MediaInputType.STATIC_IMAGE) {
                        OutlinedTextField(
                            value = media.stickerText,
                            onValueChange = { newText -> viewModel.onStickerTextChanged(newText) },
                            label = { Text(strings.stickerTextLabel, fontSize = 12.sp) },
                            placeholder = { Text(strings.stickerTextPlaceholder, fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(Icons.Outlined.TextFields, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            trailingIcon = {
                                if (media.stickerText.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onStickerTextChanged("") }) {
                                        Icon(Icons.Outlined.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            singleLine = false,
                            maxLines = 2,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Step 2: Video Trim Slider (Shown ONLY when a Video is selected)
            val currentMedia = screenState.selectedMedia
            AnimatedVisibility(visible = currentMedia?.inputType == MediaInputType.VIDEO) {
                if (currentMedia != null && currentMedia.inputType == MediaInputType.VIDEO) {
                    val maxDurationSec = (currentMedia.totalDurationMs.coerceAtLeast(1000L)) / 1000f
                    val rawStartSec = currentMedia.trimStartMs / 1000f
                    val rawEndSec = currentMedia.trimEndMs / 1000f
                    val startSec = rawStartSec.coerceIn(0f, maxDurationSec)
                    val endSec = rawEndSec.coerceIn(startSec, maxDurationSec)
                    val clipSec = (endSec - startSec)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .frostedBentoCard(24.dp)
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.trimVideoTitle(settings.maxAnimatedDurationSec),
                                color = AiryCobaltTokens.SlateCardTitle,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = String.format(Locale.US, "%.1fs %s", clipSec, strings.trimSelectedSuffix),
                                color = AiryCobaltTokens.CobaltPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        RangeSlider(
                            value = startSec..endSec,
                            onValueChange = { range ->
                                viewModel.onVideoTrimRangeChanged(
                                    startMs = (range.start * 1000f).toLong(),
                                    endMs = (range.endInclusive * 1000f).toLong()
                                )
                            },
                            onValueChangeFinished = {
                                viewModel.convertSelectedMedia()
                            },
                            valueRange = 0f..maxDurationSec,
                            colors = SliderDefaults.colors(
                                thumbColor = AiryCobaltTokens.CobaltPrimary,
                                activeTrackColor = AiryCobaltTokens.CobaltPrimary,
                                inactiveTrackColor = AiryCobaltTokens.PeriwinkleSoft
                            )
                        )
                    }
                }
            }

            // Step 3: 1:1 Sticker Preview & Save to Collection Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .frostedBentoCard(24.dp)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.previewTitle,
                        color = AiryCobaltTokens.SlateCardTitle,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    if (screenState.conversionState is StickerUiState.Success) {
                        val out = (screenState.conversionState as StickerUiState.Success).stickerOutput
                        Text(
                            text = "${out.sizeKbFormatted} • ${settings.resolutionPercent}% • ${strings.readyBadgeSuffix}",
                            color = AiryCobaltTokens.EmeraldPositive,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }
                }

                // 1:1 Square Preview Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.82f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(AiryCobaltTokens.PreviewBoxSurface)
                        .border(
                            width = 1.dp,
                            color = AiryCobaltTokens.PeriwinkleSoft,
                            shape = RoundedCornerShape(24.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val previewFile = screenState.previewFile
                    if (previewFile != null && previewFile.exists()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(previewFile)
                                .build(),
                            imageLoader = coilImageLoader,
                            contentDescription = strings.previewTitle,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )

                        if (screenState.conversionState is StickerUiState.Processing) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = AiryCobaltTokens.CobaltPrimary,
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    } else {
                        when (val uiState = screenState.conversionState) {
                            is StickerUiState.Idle -> {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.AddPhotoAlternate,
                                        contentDescription = null,
                                        tint = AiryCobaltTokens.LightAxisGrey,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Text(
                                        text = strings.emptyPreviewPlaceholder,
                                        color = AiryCobaltTokens.MutedCoolGrey,
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            is StickerUiState.Processing -> {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = AiryCobaltTokens.CobaltPrimary,
                                        strokeWidth = 3.dp
                                    )
                                    LinearProgressIndicator(
                                        progress = { uiState.progress },
                                        color = AiryCobaltTokens.CobaltPrimary,
                                        trackColor = AiryCobaltTokens.PeriwinkleSoft,
                                        modifier = Modifier
                                            .fillMaxWidth(0.75f)
                                            .height(6.dp)
                                            .clip(CircleShape)
                                    )
                                    Text(
                                        text = uiState.stageMessage,
                                        color = AiryCobaltTokens.DeepSlateHeading,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            is StickerUiState.Success -> {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(uiState.stickerOutput.file)
                                        .build(),
                                    imageLoader = coilImageLoader,
                                    contentDescription = strings.previewTitle,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            is StickerUiState.Error -> {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ErrorOutline,
                                        contentDescription = null,
                                        tint = AiryCobaltTokens.CoralAlert,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Text(
                                        text = uiState.message,
                                        color = AiryCobaltTokens.DeepSlateHeading,
                                        fontSize = 13.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                // Save to Sticker Collection Action
                if (screenState.conversionState is StickerUiState.Success) {
                    if (!screenState.isCurrentStickerSaved) {
                        Button(
                            onClick = { viewModel.onSaveCurrentStickerClicked() },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AiryCobaltTokens.CobaltPrimary,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                        ) {
                            Icon(Icons.Outlined.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.saveToSegmentButton(currentSegmentLabel),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        OutlinedButton(
                            onClick = { },
                            enabled = false,
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = AiryCobaltTokens.EmeraldPositive,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.savedInSegmentButton(currentSegmentLabel),
                                color = AiryCobaltTokens.EmeraldPositive,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Step 4: My Sticker Collection & Management Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .frostedBentoCard(24.dp)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Collections,
                            contentDescription = null,
                            tint = AiryCobaltTokens.CobaltPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = strings.collectionTitle,
                            color = AiryCobaltTokens.SlateCardTitle,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = AiryCobaltTokens.PeriwinkleSoft
                    ) {
                        Text(
                            text = strings.stickerCountBadge(screenState.packStickers.size),
                            color = AiryCobaltTokens.CobaltDeep,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                // Segments Tab: Stiker Foto vs Stiker Gerak
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = screenState.selectedSegment == StickerPackSegment.STATIC,
                        onClick = { viewModel.onSegmentSelected(StickerPackSegment.STATIC) },
                        label = { Text("${strings.staticSegmentLabel} (${screenState.staticPackStickers.size})", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AiryCobaltTokens.CobaltPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = screenState.selectedSegment == StickerPackSegment.ANIMATED,
                        onClick = { viewModel.onSegmentSelected(StickerPackSegment.ANIMATED) },
                        label = { Text("${strings.animatedSegmentLabel} (${screenState.animatedPackStickers.size})", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AiryCobaltTokens.CobaltPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }

                Text(
                    text = strings.collectionDesc(currentSegmentLabel),
                    color = AiryCobaltTokens.MutedCoolGrey,
                    fontSize = 12.sp
                )

                if (screenState.packStickers.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(screenState.packStickers, key = { it.name }) { file ->
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(AiryCobaltTokens.PreviewBoxSurface)
                                    .border(
                                        width = 1.dp,
                                        color = AiryCobaltTokens.PeriwinkleSoft,
                                        shape = RoundedCornerShape(16.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(file)
                                        .memoryCacheKey("${file.absolutePath}_${file.lastModified()}")
                                        .build(),
                                    imageLoader = coilImageLoader,
                                    contentDescription = "Collection Sticker",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(6.dp)
                                )

                                IconButton(
                                    onClick = { viewModel.deleteSticker(file) },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(22.dp)
                                        .background(AiryCobaltTokens.CoralAlert, CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = "Delete",
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            try {
                                val segment = screenState.selectedSegment
                                val intent = WhatsAppShareHelper.createAddStickerPackIntent(
                                    context = context,
                                    packIdentifier = segment.packId,
                                    packName = segment.packName,
                                    whatsAppTarget = settings.whatsAppTarget
                                )
                                addStickerPackLauncher.launch(intent)
                            } catch (_: Exception) {
                                viewModel.showNotice(strings.whatsAppErrorToast, AppNoticeTone.ERROR)
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AiryCobaltTokens.CobaltPrimary,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 50.dp)
                    ) {
                        Icon(Icons.Outlined.LibraryAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.addPackToWhatsAppButton(currentSegmentLabel, screenState.packStickers.size),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(AiryCobaltTokens.PreviewBoxSurface)
                            .padding(vertical = 24.dp, horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = strings.emptyCollectionPlaceholder(currentSegmentLabel),
                            color = AiryCobaltTokens.MutedCoolGrey,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
        }

        FloatingNoticePill(
            notice = screenState.activeNotice,
            onDismiss = viewModel::dismissNotice,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp, start = 24.dp, end = 24.dp)
        )
    }
}

@Composable
private fun FloatingNoticePill(
    notice: AppNotice?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var lastNotice by remember { mutableStateOf<AppNotice?>(null) }
    if (notice != null) {
        lastNotice = notice
    }

    AnimatedVisibility(
        visible = notice != null,
        enter = slideInVertically(initialOffsetY = { -it - 24 }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it - 24 }) + fadeOut(),
        modifier = modifier
    ) {
        val current = lastNotice ?: return@AnimatedVisibility
        val accentColor = when (current.tone) {
            AppNoticeTone.SUCCESS -> AiryCobaltTokens.EmeraldPositive
            AppNoticeTone.ERROR -> AiryCobaltTokens.CoralAlert
            AppNoticeTone.INFO -> AiryCobaltTokens.CobaltPrimary
        }
        val icon = when (current.tone) {
            AppNoticeTone.SUCCESS -> Icons.Outlined.CheckCircle
            AppNoticeTone.ERROR -> Icons.Outlined.ErrorOutline
            AppNoticeTone.INFO -> Icons.Outlined.Info
        }

        Surface(
            shape = CircleShape,
            color = AiryCobaltTokens.FrostedCardSurface,
            shadowElevation = 6.dp,
            modifier = Modifier
                .clip(CircleShape)
                .border(1.dp, accentColor.copy(alpha = 0.35f), CircleShape)
                .clickable(onClick = onDismiss)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = current.message,
                    color = AiryCobaltTokens.DeepSlateHeading,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SettingsScreenContent(
    settings: AppSettings,
    strings: StickerStrings,
    onBack: () -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit,
    onThemeSelected: (AppThemeMode) -> Unit,
    onResolutionSelected: (Int) -> Unit,
    onMaxDurationSelected: (Int) -> Unit,
    onDefaultFitModeSelected: (ScaleFitMode) -> Unit,
    onWhatsAppTargetSelected: (WhatsAppTarget) -> Unit,
    onCustomPublisherChanged: (String) -> Unit,
    onClearCacheClicked: () -> Unit
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AiryCobaltTokens.SkyCanvasBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Settings Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(AiryCobaltTokens.FrostedCardSurface)
                        .border(1.dp, AiryCobaltTokens.FrostedCardBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = strings.backButton,
                        tint = AiryCobaltTokens.DeepSlateHeading,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.settingsTitle,
                        color = AiryCobaltTokens.DeepSlateHeading,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = strings.settingsSubtitle,
                        color = AiryCobaltTokens.MutedCoolGrey,
                        fontSize = 12.sp
                    )
                }
            }

            // 1. Language Setting Card
            SettingsSectionCard(
                title = strings.languageSettingTitle,
                description = strings.languageSettingDesc
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppLanguage.entries.forEach { lang ->
                        FilterChip(
                            selected = settings.language == lang,
                            onClick = { onLanguageSelected(lang) },
                            label = { Text(lang.label, fontSize = 13.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AiryCobaltTokens.CobaltPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // 2. Sticker Resolution % Setting Card
            SettingsSectionCard(
                title = strings.resolutionSettingTitle,
                description = strings.resolutionSettingDesc
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppSettings.RESOLUTION_OPTIONS.forEach { percent ->
                        val px = AppSettings.computeInternalResolutionPx(percent)
                        FilterChip(
                            selected = settings.resolutionPercent == percent,
                            onClick = { onResolutionSelected(percent) },
                            label = { Text("$percent% (${px}px)", fontSize = 13.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AiryCobaltTokens.CobaltPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // 3. GIF & Video Duration Limit Card
            SettingsSectionCard(
                title = strings.durationSettingTitle,
                description = strings.durationSettingDesc
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppSettings.DURATION_OPTIONS_SEC.forEach { sec ->
                        FilterChip(
                            selected = settings.maxAnimatedDurationSec == sec,
                            onClick = { onMaxDurationSelected(sec) },
                            label = { Text("$sec ${strings.secondsUnit}", fontSize = 13.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AiryCobaltTokens.CobaltPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // 4. App Theme Card
            SettingsSectionCard(
                title = strings.themeSettingTitle,
                description = strings.themeSettingDesc
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = settings.themeMode == mode,
                            onClick = { onThemeSelected(mode) },
                            label = { Text(mode.label(settings.language), fontSize = 13.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AiryCobaltTokens.CobaltPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // 5a. Default Fit Mode Card
            SettingsSectionCard(
                title = strings.defaultFitSettingTitle,
                description = strings.defaultFitSettingDesc
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ScaleFitMode.entries.forEach { fitMode ->
                        FilterChip(
                            selected = settings.defaultFitMode == fitMode,
                            onClick = { onDefaultFitModeSelected(fitMode) },
                            label = { Text(strings.fitModeLabel(fitMode, settings.language), fontSize = 13.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AiryCobaltTokens.CobaltPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // 5b. Target WhatsApp App Card
            SettingsSectionCard(
                title = strings.whatsAppTargetTitle,
                description = strings.whatsAppTargetDesc
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WhatsAppTarget.entries.forEach { target ->
                        FilterChip(
                            selected = settings.whatsAppTarget == target,
                            onClick = { onWhatsAppTargetSelected(target) },
                            label = { Text(target.label(settings.language), fontSize = 13.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AiryCobaltTokens.CobaltPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // 5c. Custom Sticker Publisher / Author Card
            SettingsSectionCard(
                title = strings.publisherSettingTitle,
                description = strings.publisherSettingDesc
            ) {
                OutlinedTextField(
                    value = settings.customPublisher,
                    onValueChange = onCustomPublisherChanged,
                    placeholder = { Text(strings.publisherPlaceholder, fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(Icons.Outlined.PersonOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 5d. Storage & Preview Cache Card
            SettingsSectionCard(
                title = strings.storageSettingTitle,
                description = ""
            ) {
                OutlinedButton(
                    onClick = onClearCacheClicked,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 46.dp)
                ) {
                    Icon(Icons.Outlined.CleaningServices, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = strings.clearCacheButton,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    description: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .frostedBentoCard(20.dp)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            color = AiryCobaltTokens.SlateCardTitle,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp
        )
        if (description.isNotBlank()) {
            Text(
                text = description,
                color = AiryCobaltTokens.MutedCoolGrey,
                fontSize = 12.sp
            )
        }
        content()
    }
}
