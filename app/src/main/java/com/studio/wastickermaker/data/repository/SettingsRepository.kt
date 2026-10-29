package com.studio.wastickermaker.data.repository

import android.content.Context
import com.studio.wastickermaker.data.model.AppLanguage
import com.studio.wastickermaker.data.model.AppSettings
import com.studio.wastickermaker.data.model.AppThemeMode
import com.studio.wastickermaker.data.model.ScaleFitMode
import com.studio.wastickermaker.data.model.WhatsAppTarget

class SettingsRepository(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadSettings(): AppSettings {
        val langName = prefs.getString(KEY_LANGUAGE, AppLanguage.INDONESIAN.name)
        val themeName = prefs.getString(KEY_THEME_MODE, AppThemeMode.LIGHT.name)
        val fitName = prefs.getString(KEY_DEFAULT_FIT_MODE, ScaleFitMode.CENTER_CROP.name)
        val waTargetName = prefs.getString(KEY_WA_TARGET, WhatsAppTarget.AUTO.name)

        return AppSettings(
            language = runCatching { AppLanguage.valueOf(langName!!) }.getOrDefault(AppLanguage.INDONESIAN),
            resolutionPercent = prefs.getInt(KEY_RESOLUTION_PERCENT, 100).coerceIn(50, 100),
            maxAnimatedDurationSec = prefs.getInt(KEY_MAX_DURATION_SEC, 6).coerceIn(2, 10),
            themeMode = runCatching { AppThemeMode.valueOf(themeName!!) }.getOrDefault(AppThemeMode.LIGHT),
            defaultFitMode = runCatching { ScaleFitMode.valueOf(fitName!!) }.getOrDefault(ScaleFitMode.CENTER_CROP),
            whatsAppTarget = runCatching { WhatsAppTarget.valueOf(waTargetName!!) }.getOrDefault(WhatsAppTarget.AUTO),
            customPublisher = prefs.getString(KEY_CUSTOM_PUBLISHER, "Sticker Maker") ?: "Sticker Maker"
        )
    }

    fun saveSettings(settings: AppSettings) {
        prefs.edit()
            .putString(KEY_LANGUAGE, settings.language.name)
            .putInt(KEY_RESOLUTION_PERCENT, settings.resolutionPercent.coerceIn(50, 100))
            .putInt(KEY_MAX_DURATION_SEC, settings.maxAnimatedDurationSec.coerceIn(2, 10))
            .putString(KEY_THEME_MODE, settings.themeMode.name)
            .putString(KEY_DEFAULT_FIT_MODE, settings.defaultFitMode.name)
            .putString(KEY_WA_TARGET, settings.whatsAppTarget.name)
            .putString(KEY_CUSTOM_PUBLISHER, settings.customPublisher.trim().ifEmpty { "Sticker Maker" })
            .apply()
    }

    companion object {
        const val PREFS_NAME = "sticker_maker_settings"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_RESOLUTION_PERCENT = "resolution_percent"
        private const val KEY_MAX_DURATION_SEC = "max_duration_sec"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_DEFAULT_FIT_MODE = "default_fit_mode"
        private const val KEY_WA_TARGET = "wa_target"
        const val KEY_CUSTOM_PUBLISHER = "custom_publisher"

        fun getCustomPublisher(context: Context?): String {
            if (context == null) return "Sticker Maker"
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getString(KEY_CUSTOM_PUBLISHER, "Sticker Maker")
                ?.trim()
                ?.ifEmpty { "Sticker Maker" }
                ?: "Sticker Maker"
        }
    }
}
