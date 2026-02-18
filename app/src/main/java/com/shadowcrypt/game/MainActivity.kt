package com.shadowcrypt.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.shadowcrypt.game.ui.navigation.AppNavigation
import com.shadowcrypt.game.ui.theme.ShadowcryptTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShadowcryptTheme {
                AppNavigation()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        ServiceLocator.soundManager.pauseBackgroundMusic()
    }

    override fun onResume() {
        super.onResume()
        ServiceLocator.soundManager.resumeBackgroundMusic()
    }

    override fun onDestroy() {
        super.onDestroy()
        ServiceLocator.soundManager.release()
    }
}
