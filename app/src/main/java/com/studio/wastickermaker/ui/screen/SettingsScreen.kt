package com.studio.wastickermaker.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studio.wastickermaker.data.model.AppLanguage
import com.studio.wastickermaker.data.model.AppSettings
import com.studio.wastickermaker.data.model.AppThemeMode
import com.studio.wastickermaker.data.model.ScaleFitMode
import com.studio.wastickermaker.data.model.WhatsAppTarget
import com.studio.wastickermaker.ui.theme.AiryCobaltTokens
import com.studio.wastickermaker.ui.theme.StickerStrings
import com.studio.wastickermaker.ui.theme.frostedBentoCard

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreenContent(
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
fun SettingsSectionCard(
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
