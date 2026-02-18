package com.shadowcrypt.game

import android.app.Application
import com.shadowcrypt.game.data.MetaProgressDataStore
import com.shadowcrypt.game.data.MetaProgressRepository
import com.shadowcrypt.game.data.SettingsDataStore
import com.shadowcrypt.game.data.SettingsRepository
import com.shadowcrypt.game.haptic.HapticManager
import com.shadowcrypt.game.sound.SoundManager
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ShadowcryptApp : Application() {

    override fun onCreate() {
        super.onCreate()
        ServiceLocator.initialize(this)
    }
}

object ServiceLocator {

    private var initialized = false

    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var metaProgressRepository: MetaProgressRepository
        private set
    lateinit var soundManager: SoundManager
        private set
    lateinit var hapticManager: HapticManager
        private set

    fun initialize(context: Application) {
        if (initialized) return
        initialized = true

        val settingsDataStore = SettingsDataStore(context)
        val metaProgressDataStore = MetaProgressDataStore(context)

        settingsRepository = SettingsRepository(settingsDataStore)
        metaProgressRepository = MetaProgressRepository(metaProgressDataStore)
        soundManager = SoundManager(context)
        hapticManager = HapticManager(context)

        @Suppress("OPT_IN_USAGE")
        GlobalScope.launch {
            settingsRepository.settings.collect { settings ->
                soundManager.sfxEnabled = settings.soundEnabled
                soundManager.musicEnabled = settings.musicEnabled
                hapticManager.enabled = settings.hapticsEnabled

                if (!settings.musicEnabled) {
                    soundManager.stopBackgroundMusic()
                }
            }
        }
    }
}
