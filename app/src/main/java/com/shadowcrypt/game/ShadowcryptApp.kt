package com.shadowcrypt.game

import android.app.Application

/**
 * Application class for Shadowcrypt Depths.
 *
 * This is the first thing that runs when the app starts.
 * We use it to initialize our services (repositories, sound manager, etc.)
 * using a simple ServiceLocator pattern instead of a dependency injection framework.
 *
 * Services will be added incrementally as the game features are built:
 * - Phase 2: (none needed yet — engine runs in-memory)
 * - Phase 5: MetaProgressRepository, SettingsRepository
 * - Phase 6: SoundManager, HapticManager
 */
class ShadowcryptApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialize the ServiceLocator so all parts of the app
        // can access shared services
        ServiceLocator.initialize(this)
    }
}

/**
 * Simple service locator that holds references to shared services.
 *
 * This is a beginner-friendly alternative to dependency injection frameworks
 * like Hilt or Dagger. It provides the same benefit (shared instances) without
 * the complexity of annotations and modules.
 *
 * Services are added as each phase is implemented:
 * - Phase 5: metaProgressRepository, settingsRepository
 * - Phase 6: soundManager, hapticManager
 */
object ServiceLocator {

    private var initialized = false

    fun initialize(context: Application) {
        if (initialized) return
        initialized = true

        // Services will be initialized here as they are implemented.
        // For now, the game engine runs entirely in-memory via ViewModels.
        //
        // Future additions:
        // metaProgressRepository = MetaProgressRepository(MetaProgressDataStore(context))
        // settingsRepository = SettingsRepository(SettingsDataStore(context))
        // soundManager = SoundManager(context)
        // hapticManager = HapticManager(context)
    }
}
