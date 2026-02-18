package com.shadowcrypt.game.haptic

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class HapticManager(context: Context) {

    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        manager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    var enabled: Boolean = true

    fun trigger(event: HapticEvent) {
        if (!enabled) return
        if (!vibrator.hasVibrator()) return

        val effect = VibrationEffect.createWaveform(event.timings, event.amplitudes, -1)
        vibrator.vibrate(effect)
    }

    fun cancel() {
        vibrator.cancel()
    }
}
