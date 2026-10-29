package com.studio.wastickermaker.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.studio.wastickermaker.data.model.AppThemeMode

/**
 * Airy Cobalt (Light) & Midnight Cobalt (Dark) Frosted Bento UI Design System.
 */
object AiryCobaltTokens {
    var isDark by mutableStateOf(false)

    val CobaltPrimary: Color
        get() = if (isDark) Color(0xFF3B82F6) else Color(0xFF2563EB)
    val CobaltDeep: Color
        get() = if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8)
    val PeriwinkleSoft: Color
        get() = if (isDark) Color(0xFF1E3A8A) else Color(0xFFBFDBFE)
    val IceBlueAccent: Color
        get() = if (isDark) Color(0xFF60A5FA) else Color(0xFF93C5FD)

    val FrostedCardSurface: Color
        get() = if (isDark) Color(0xCC1E293B) else Color(0x94FFFFFF)
    val FrostedCardBorder: Color
        get() = if (isDark) Color(0xFF334155) else Color(0xD9FFFFFF)

    val DeepSlateHeading: Color
        get() = if (isDark) Color(0xFFF8FAFC) else Color(0xFF1E293B)
    val SlateCardTitle: Color
        get() = if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155)
    val MutedCoolGrey: Color
        get() = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val LightAxisGrey: Color
        get() = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)

    val PreviewBoxSurface: Color
        get() = if (isDark) Color(0xB30F172A) else Color(0xBFFFFFFF)

    val EmeraldPositive = Color(0xFF10B981)
    val CoralAlert = Color(0xFFEF4444)

    val SkyCanvasBrush: Brush
        get() = if (isDark) {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF0B1120),
                    Color(0xFF0F172A),
                    Color(0xFF1E293B)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFFFFFFF),
                    Color(0xFFEBF5FF),
                    Color(0xFFD8EEFE)
                )
            )
        }
}

private val AiryCobaltLightColorScheme = lightColorScheme(
    primary = Color(0xFF2563EB),
    onPrimary = Color.White,
    secondary = Color(0xFFBFDBFE),
    onSecondary = Color(0xFF1E293B),
    background = Color(0xFFEBF5FF),
    surface = Color(0x94FFFFFF),
    onSurface = Color(0xFF1E293B)
)

private val MidnightCobaltDarkColorScheme = darkColorScheme(
    primary = Color(0xFF3B82F6),
    onPrimary = Color.White,
    secondary = Color(0xFF1E3A8A),
    onSecondary = Color(0xFFF8FAFC),
    background = Color(0xFF0F172A),
    surface = Color(0xCC1E293B),
    onSurface = Color(0xFFF8FAFC)
)

@Composable
fun AiryCobaltStickerTheme(
    themeMode: AppThemeMode = AppThemeMode.LIGHT,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (themeMode) {
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
        AppThemeMode.SYSTEM -> systemDark
    }
    AiryCobaltTokens.isDark = useDark

    MaterialTheme(
        colorScheme = if (useDark) MidnightCobaltDarkColorScheme else AiryCobaltLightColorScheme,
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
