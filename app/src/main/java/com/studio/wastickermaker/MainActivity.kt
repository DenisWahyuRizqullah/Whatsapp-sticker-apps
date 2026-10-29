package com.studio.wastickermaker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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
            AiryCobaltStickerTheme(themeMode = screenState.settings.themeMode) {
                StickerMakerScreen(viewModel = stickerViewModel)
            }
        }
    }
}
