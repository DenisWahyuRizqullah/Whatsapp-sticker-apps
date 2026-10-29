package com.studio.wastickermaker.data.model

enum class AppLanguage(val code: String, val label: String) {
    INDONESIAN("id", "Bahasa Indonesia"),
    ENGLISH("en", "English")
}

enum class AppThemeMode(val labelId: String, val labelEn: String) {
    LIGHT("Terang", "Light"),
    DARK("Gelap", "Dark"),
    SYSTEM("Sistem", "System");

    fun label(language: AppLanguage): String =
        if (language == AppLanguage.INDONESIAN) labelId else labelEn
}

enum class WhatsAppTarget(val labelId: String, val labelEn: String) {
    AUTO("Otomatis", "Auto"),
    CONSUMER("WhatsApp", "WhatsApp"),
    BUSINESS("WA Business", "WA Business");

    fun label(language: AppLanguage): String =
        if (language == AppLanguage.INDONESIAN) labelId else labelEn
}

data class AppSettings(
    val language: AppLanguage = AppLanguage.INDONESIAN,
    val resolutionPercent: Int = 100,
    val maxAnimatedDurationSec: Int = 6,
    val themeMode: AppThemeMode = AppThemeMode.LIGHT,
    val defaultFitMode: ScaleFitMode = ScaleFitMode.CENTER_CROP,
    val whatsAppTarget: WhatsAppTarget = WhatsAppTarget.AUTO,
    val customPublisher: String = "Sticker Maker"
) {
    val maxAnimatedDurationMs: Long
        get() = maxAnimatedDurationSec.coerceIn(2, 10) * 1000L

    val internalResolutionPx: Int
        get() = computeInternalResolutionPx(resolutionPercent)

    val effectivePublisher: String
        get() = customPublisher.trim().ifEmpty { "Sticker Maker" }

    companion object {
        val RESOLUTION_OPTIONS = listOf(50, 75, 100)
        val DURATION_OPTIONS_SEC = listOf(3, 6, 8, 10)

        fun computeInternalResolutionPx(resolutionPercent: Int): Int {
            val clamped = resolutionPercent.coerceIn(50, 100)
            val rawPx = (512 * clamped) / 100
            // Keep dimension even for yuva420p FFmpeg filter compatibility
            return (if (rawPx % 2 == 1) rawPx + 1 else rawPx).coerceIn(256, 512)
        }
    }
}
