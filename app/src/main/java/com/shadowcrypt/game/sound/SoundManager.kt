package com.shadowcrypt.game.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool

/**
 * Manages all game audio: SoundPool for short SFX, MediaPlayer for background music.
 *
 * Currently a placeholder implementation — all sound IDs are unloaded because
 * no audio asset files exist yet. When real .ogg/.wav files are added to res/raw,
 * load them in init and store their SoundPool IDs in the soundIds map.
 *
 * Adding a real sound later:
 *   1. Place file in app/src/main/res/raw/ (e.g., sfx_attack.ogg)
 *   2. In init: soundIds[SfxEvent.ATTACK] = soundPool.load(context, R.raw.sfx_attack, 1)
 *   3. playSfx(SfxEvent.ATTACK) will then play the loaded sound
 */
class SoundManager(private val context: Context) {

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private var mediaPlayer: MediaPlayer? = null

    private val soundIds: MutableMap<SfxEvent, Int> = mutableMapOf()

    var sfxEnabled: Boolean = true
    var musicEnabled: Boolean = true

    fun playSfx(event: SfxEvent) {
        if (!sfxEnabled) return
        val id = soundIds[event] ?: return
        soundPool.play(id, 1f, 1f, 1, 0, 1f)
    }

    fun pauseBackgroundMusic() {
        mediaPlayer?.let {
            if (it.isPlaying) it.pause()
        }
    }

    fun resumeBackgroundMusic() {
        if (!musicEnabled) return
        mediaPlayer?.start()
    }

    fun stopBackgroundMusic() {
        mediaPlayer?.let {
            it.stop()
            it.release()
        }
        mediaPlayer = null
    }

    fun release() {
        soundPool.release()
        stopBackgroundMusic()
    }
}

enum class SfxEvent {
    ATTACK,
    DAMAGE,
    ENEMY_DEATH,
    ITEM_PICKUP,
    LEVEL_UP,
    DESCEND,
    PLAYER_DEATH,
    VICTORY,
    MENU_SELECT,
    EQUIP
}
