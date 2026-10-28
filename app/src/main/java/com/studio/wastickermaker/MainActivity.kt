package com.studio.wastickermaker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.studio.wastickermaker.ui.screen.StickerMakerScreen
import com.studio.wastickermaker.ui.theme.AiryCobaltStickerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AiryCobaltStickerTheme {
                StickerMakerScreen()
            }
        }
    }
}
