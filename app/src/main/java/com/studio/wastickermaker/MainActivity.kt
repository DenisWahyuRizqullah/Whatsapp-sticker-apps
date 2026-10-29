package com.studio.wastickermaker

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studio.wastickermaker.data.model.AppThemeMode
import com.studio.wastickermaker.ui.screen.StickerMakerScreen
import com.studio.wastickermaker.ui.theme.AiryCobaltStickerTheme
import com.studio.wastickermaker.ui.viewmodel.StickerViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val stickerViewModel: StickerViewModel = viewModel()
            val screenState by stickerViewModel.screenState.collectAsStateWithLifecycle()
            val themeMode = screenState.settings.themeMode
            val systemDark = isSystemInDarkTheme()
            val isDark = when (themeMode) {
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
                AppThemeMode.SYSTEM -> systemDark
            }
            LaunchedEffect(isDark) {
                val barStyle = if (isDark) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
            }
            AiryCobaltStickerTheme(themeMode = themeMode) {
                StickerMakerScreen(viewModel = stickerViewModel)
            }
        }
    }
}
