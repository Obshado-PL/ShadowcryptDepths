package com.shadowcrypt.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.shadowcrypt.game.ui.navigation.AppNavigation
import com.shadowcrypt.game.ui.theme.ShadowcryptTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Observe settings and forward to managers
        lifecycleScope.launch {
            ServiceLocator.settingsRepository.settings.collect { settings ->
                ServiceLocator.soundManager.enabled = settings.soundEnabled
                ServiceLocator.soundManager.volume = settings.soundVolume
                ServiceLocator.hapticManager.enabled = settings.hapticEnabled
            }
        }

        setContent {
            ShadowcryptTheme {
                AppNavigation()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ServiceLocator.soundManager.release()
    }
}
