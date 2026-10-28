package com.studio.wastickermaker.ui.screen

import android.app.Activity
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.LibraryAdd
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Send
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.studio.wastickermaker.data.model.MediaInputType
import com.studio.wastickermaker.data.model.ScaleFitMode
import com.studio.wastickermaker.ui.state.StickerUiState
import com.studio.wastickermaker.ui.theme.AiryCobaltTokens
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
            Toast.makeText(context, "Sticker Pack added to WhatsApp!", Toast.LENGTH_SHORT).show()
        }
    }

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
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Simple Top Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Sticker Maker",
                        color = AiryCobaltTokens.DeepSlateHeading,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Turn any photo, GIF, or video into a WhatsApp sticker",
                        color = AiryCobaltTokens.MutedCoolGrey,
                        fontSize = 13.sp
                    )
                }

                if (screenState.selectedMedia != null) {
                    OutlinedButton(
                        onClick = { viewModel.resetState() },
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Reset",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("New", fontSize = 12.sp)
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AddPhotoAlternate,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Choose Photo, GIF, or Video",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
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
                            fontWeight = FontWeight.Medium
                        )
                        Surface(
                            shape = CircleShape,
                            color = AiryCobaltTokens.PeriwinkleSoft
                        ) {
                            Text(
                                text = media.inputType.displayLabel,
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
                                label = { Text(mode.label, fontSize = 12.sp) },
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
                            label = { Text("Teks Stiker (Meme / Caption)", fontSize = 12.sp) },
                            placeholder = { Text("Ketik teks untuk stiker...", fontSize = 13.sp) },
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
                                text = "Trim Video (Max 6 seconds)",
                                color = AiryCobaltTokens.SlateCardTitle,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = String.format(Locale.US, "%.1fs selected", clipSec),
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

            // Step 3: 1:1 Sticker Preview & Send to WhatsApp Card
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
                        text = "Sticker Preview",
                        color = AiryCobaltTokens.SlateCardTitle,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                    if (screenState.conversionState is StickerUiState.Success) {
                        val out = (screenState.conversionState as StickerUiState.Success).stickerOutput
                        Text(
                            text = "${out.sizeKbFormatted} • Ready",
                            color = AiryCobaltTokens.EmeraldPositive,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }
                }

                // 1:1 Square Preview Box (Empty until user selects media)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.82f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.75f))
                        .border(
                            width = 1.dp,
                            color = AiryCobaltTokens.PeriwinkleSoft,
                            shape = RoundedCornerShape(24.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val previewFile = screenState.previewFile
                    if (previewFile != null && previewFile.exists()) {
                        // Always keep preview image rendered to prevent blinking/flickering
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(previewFile)
                                .build(),
                            imageLoader = coilImageLoader,
                            contentDescription = "Sticker Preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )

                        // If background debounce conversion is actively processing, show a subtle progress indicator overlay
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
                                        text = "Your sticker will appear here",
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
                                    contentDescription = "Sticker Preview",
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

                // Send to WhatsApp Actions
                if (screenState.conversionState is StickerUiState.Success) {
                    val output = (screenState.conversionState as StickerUiState.Success).stickerOutput

                    // Primary Button: Simpan ke Koleksi Stiker
                    if (!screenState.isCurrentStickerSaved) {
                        Button(
                            onClick = {
                                viewModel.saveCurrentStickerToPack()
                                Toast.makeText(context, "Stiker berhasil disimpan ke koleksi!", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AiryCobaltTokens.CobaltPrimary,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Icon(Icons.Outlined.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "+ Simpan ke Koleksi Stiker",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        OutlinedButton(
                            onClick = { },
                            enabled = false,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = AiryCobaltTokens.EmeraldPositive,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Tersimpan di Koleksi ✓",
                                color = AiryCobaltTokens.EmeraldPositive,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Secondary Button: Tambahkan Koleksi ke WhatsApp
                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = WhatsAppShareHelper.createAddStickerPackIntent(context)
                                addStickerPackLauncher.launch(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open WhatsApp.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.Outlined.LibraryAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tambahkan Koleksi (${screenState.packStickers.size}) ke WhatsApp",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Collections,
                            contentDescription = null,
                            tint = AiryCobaltTokens.CobaltPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Koleksi Stiker Saya",
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
                            text = "${screenState.packStickers.size} Stiker",
                            color = AiryCobaltTokens.CobaltDeep,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Text(
                    text = "Kelola stiker yang telah dibuat. Stiker di koleksi ini otomatis masuk ke keyboard WhatsApp.",
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
                                    .background(Color.White.copy(alpha = 0.8f))
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
                                    onClick = {
                                        viewModel.deleteSticker(file)
                                        Toast.makeText(context, "Stiker dihapus dari koleksi.", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(22.dp)
                                        .background(AiryCobaltTokens.CoralAlert, CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = "Hapus",
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
                                val intent = WhatsAppShareHelper.createAddStickerPackIntent(context)
                                addStickerPackLauncher.launch(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open WhatsApp.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AiryCobaltTokens.CobaltPrimary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(Icons.Outlined.LibraryAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tambahkan Koleksi (${screenState.packStickers.size}) ke WhatsApp",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.5f))
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Belum ada stiker di koleksi.\nBuat stiker pertamamu di atas!",
                            color = AiryCobaltTokens.MutedCoolGrey,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
