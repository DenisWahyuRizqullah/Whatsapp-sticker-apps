package com.studio.wastickermaker.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Airy Cobalt & Frosted Bento UI Design System (@ui-design-airy-cobalt-glass).
 */
object AiryCobaltTokens {
    val CobaltPrimary = Color(0xFF2563EB)
    val CobaltDeep = Color(0xFF1D4ED8)
    val PeriwinkleSoft = Color(0xFFBFDBFE)
    val IceBlueAccent = Color(0xFF93C5FD)

    val FrostedCardSurface = Color(0x94FFFFFF)   // rgba(255, 255, 255, 0.58)
    val FrostedCardBorder = Color(0xD9FFFFFF)    // rgba(255, 255, 255, 0.85)

    val DeepSlateHeading = Color(0xFF1E293B)
    val SlateCardTitle = Color(0xFF334155)
    val MutedCoolGrey = Color(0xFF64748B)
    val LightAxisGrey = Color(0xFF94A3B8)

    val EmeraldPositive = Color(0xFF10B981)
    val CoralAlert = Color(0xFFEF4444)

    val SkyCanvasBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFFFFF),
            Color(0xFFEBF5FF),
            Color(0xFFD8EEFE)
        )
    )
}

private val AiryCobaltColorScheme = lightColorScheme(
    primary = AiryCobaltTokens.CobaltPrimary,
    onPrimary = Color.White,
    secondary = AiryCobaltTokens.PeriwinkleSoft,
    onSecondary = AiryCobaltTokens.DeepSlateHeading,
    background = Color(0xFFEBF5FF),
    surface = AiryCobaltTokens.FrostedCardSurface,
    onSurface = AiryCobaltTokens.DeepSlateHeading
)

@Composable
fun AiryCobaltStickerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AiryCobaltColorScheme,
        content = content
    )
}

fun Modifier.frostedBentoCard(cornerRadius: Dp = 24.dp): Modifier {
    val shape = RoundedCornerShape(cornerRadius)
    return this
        .clip(shape)
        .background(AiryCobaltTokens.FrostedCardSurface)
        .border(width = 1.dp, color = AiryCobaltTokens.FrostedCardBorder, shape = shape)
}
