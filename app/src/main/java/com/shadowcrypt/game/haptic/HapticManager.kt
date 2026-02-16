package com.shadowcrypt.game.haptic

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.shadowcrypt.game.model.GameEvent

class HapticManager(context: Context) {

    private val vibrator: Vibrator = if (android.os.Build.VERSION.SDK_INT >= 31) {
        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vm.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    var enabled: Boolean = true

    fun play(event: GameEvent) {
        if (!enabled || !vibrator.hasVibrator()) return

        val effect = when (event) {
            GameEvent.PlayerMove -> oneShot(10, 40)
            GameEvent.PlayerAttack -> oneShot(20, 120)
            GameEvent.PlayerHit -> oneShot(40, 200)
            GameEvent.EnemyKilled -> waveform(
                timings = longArrayOf(0, 15, 50, 15),
                amplitudes = intArrayOf(0, 150, 0, 150)
            )
            GameEvent.BossKilled -> waveform(
                timings = longArrayOf(0, 20, 40, 20, 40, 20),
                amplitudes = intArrayOf(0, 100, 0, 150, 0, 220)
            )
            GameEvent.ItemPickup -> oneShot(10, 50)
            GameEvent.ItemEquip -> oneShot(15, 80)
            GameEvent.ItemUse -> oneShot(15, 60)
            GameEvent.LevelUp -> waveform(
                timings = longArrayOf(0, 20, 30, 25, 30, 30),
                amplitudes = intArrayOf(0, 80, 0, 140, 0, 220)
            )
            GameEvent.FloorDescend -> oneShot(30, 100)
            GameEvent.TrapTriggered -> oneShot(30, 180)
            GameEvent.PlayerDeath -> oneShot(100, 255)
            GameEvent.Victory -> waveform(
                timings = longArrayOf(0, 25, 40, 25, 40, 25, 40, 30),
                amplitudes = intArrayOf(0, 100, 0, 140, 0, 180, 0, 255)
            )
        }

        try {
            vibrator.vibrate(effect)
        } catch (_: Exception) {
            // Silently ignore — haptics should never crash the game
        }
    }

    private fun oneShot(durationMs: Long, amplitude: Int): VibrationEffect =
        VibrationEffect.createOneShot(durationMs, amplitude.coerceIn(1, 255))

    private fun waveform(timings: LongArray, amplitudes: IntArray): VibrationEffect =
        VibrationEffect.createWaveform(timings, amplitudes, -1)
}
