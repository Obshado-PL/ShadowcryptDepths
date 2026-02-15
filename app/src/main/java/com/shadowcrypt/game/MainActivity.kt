package com.shadowcrypt.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.shadowcrypt.game.ui.navigation.AppNavigation
import com.shadowcrypt.game.ui.theme.ShadowcryptTheme

/**
 * The single Activity for the entire app.
 *
 * In modern Android development with Jetpack Compose, we use a single Activity
 * and handle all screen navigation within Compose using Navigation Compose.
 * This Activity just sets up the Compose content and applies our custom theme.
 *
 * Sound lifecycle management will be added in Phase 6:
 * - onPause: pause background music
 * - onResume: resume background music
 * - onDestroy: release audio resources
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Enable edge-to-edge display (content extends behind status bar)
        enableEdgeToEdge()
        // Set the Compose UI content
        setContent {
            ShadowcryptTheme {
                AppNavigation()
            }
        }
    }

    // Sound lifecycle hooks will be added in Phase 6:
    // override fun onPause() { ... ServiceLocator.soundManager.pauseBackgroundMusic() }
    // override fun onResume() { ... ServiceLocator.soundManager.resumeBackgroundMusic() }
    // override fun onDestroy() { ... ServiceLocator.soundManager.release() }
}
