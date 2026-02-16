package com.shadowcrypt.game

import android.app.Application
import com.shadowcrypt.game.audio.SoundManager
import com.shadowcrypt.game.data.MetaProgressRepository
import com.shadowcrypt.game.data.SettingsRepository
import com.shadowcrypt.game.haptic.HapticManager

class ShadowcryptApp : Application() {

    override fun onCreate() {
        super.onCreate()
        ServiceLocator.initialize(this)
    }
}

object ServiceLocator {

    private var initialized = false

    var appContext: android.content.Context? = null
        private set
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

        appContext = context.applicationContext
        settingsRepository = SettingsRepository(context)
        metaProgressRepository = MetaProgressRepository(context)
        soundManager = SoundManager()
        hapticManager = HapticManager(context)
    }
}
