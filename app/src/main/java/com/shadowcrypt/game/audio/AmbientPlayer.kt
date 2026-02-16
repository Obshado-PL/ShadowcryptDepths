package com.shadowcrypt.game.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.shadowcrypt.game.model.FloorTheme
import kotlin.math.PI
import kotlin.math.sin

/**
 * Generates continuous procedural ambient audio per floor theme.
 * Uses AudioTrack in streaming mode on a dedicated thread.
 * Each theme gets a distinct low drone with unique harmonics.
 */
class AmbientPlayer {

    private val sampleRate = 22050
    private val bufferSamples = 2048
    @Volatile var volume: Float = 0.4f
    @Volatile var enabled: Boolean = true

    @Volatile private var currentTheme: FloorTheme? = null
    @Volatile private var targetTheme: FloorTheme? = null
    @Volatile private var crossfadeProgress: Float = 1.0f

    private var track: AudioTrack? = null
    private var playbackThread: Thread? = null
    @Volatile private var running = false
    private var phase: Double = 0.0
    private var targetPhase: Double = 0.0

    fun setTheme(theme: FloorTheme) {
        if (theme == currentTheme && crossfadeProgress >= 1.0f) return
        targetTheme = theme
        crossfadeProgress = 0f
        targetPhase = 0.0
    }

    fun start() {
        if (running) return
        running = true

        val minBufSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = maxOf(minBufSize, bufferSamples * 2)

        track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        track?.play()

        playbackThread = Thread({
            val buffer = ShortArray(bufferSamples)
            while (running) {
                if (!enabled || volume <= 0f) {
                    buffer.fill(0)
                    track?.write(buffer, 0, buffer.size)
                    continue
                }

                fillBuffer(buffer)
                track?.write(buffer, 0, buffer.size)
            }
        }, "AmbientAudio").apply {
            isDaemon = true
            start()
        }
    }

    fun stop() {
        running = false
        playbackThread?.join(500)
        playbackThread = null
        try {
            track?.stop()
            track?.release()
        } catch (_: Exception) {}
        track = null
    }

    fun pause() {
        try { track?.pause() } catch (_: Exception) {}
    }

    fun resume() {
        try { track?.play() } catch (_: Exception) {}
    }

    private fun fillBuffer(buffer: ShortArray) {
        val current = currentTheme
        val target = targetTheme

        // Advance crossfade
        if (target != null && crossfadeProgress < 1.0f) {
            crossfadeProgress = (crossfadeProgress + bufferSamples.toFloat() / sampleRate / 2.0f)
                .coerceAtMost(1.0f)
            if (crossfadeProgress >= 1.0f) {
                currentTheme = target
                targetTheme = null
                phase = targetPhase
            }
        }

        for (i in buffer.indices) {
            var sample = 0.0

            if (current != null && crossfadeProgress < 1.0f) {
                // Crossfading: blend old and new
                val oldSample = generateSample(current, phase)
                val newSample = if (target != null) generateSample(target, targetPhase) else 0.0
                sample = oldSample * (1.0 - crossfadeProgress) + newSample * crossfadeProgress
                phase += 1.0 / sampleRate
                targetPhase += 1.0 / sampleRate
            } else if (currentTheme != null) {
                // Steady state
                sample = generateSample(currentTheme!!, phase)
                phase += 1.0 / sampleRate
            } else if (target != null) {
                // Initial fade in
                sample = generateSample(target, targetPhase) * crossfadeProgress
                targetPhase += 1.0 / sampleRate
            }

            buffer[i] = (sample * volume * 16000).toInt().coerceIn(-32768, 32767).toShort()
        }
    }

    private fun generateSample(theme: FloorTheme, t: Double): Double {
        return when (theme) {
            FloorTheme.Crypt -> {
                // Low drone 55Hz + minor third harmonic (65.4Hz) + subtle reverb echo
                val base = sin(2 * PI * 55.0 * t)
                val minor = sin(2 * PI * 65.4 * t) * 0.4
                val sub = sin(2 * PI * 27.5 * t) * 0.3
                val tremolo = 0.85 + 0.15 * sin(2 * PI * 0.3 * t)
                (base + minor + sub) * 0.3 * tremolo
            }
            FloorTheme.Sewers -> {
                // 65Hz with dripping rhythm modulation
                val base = sin(2 * PI * 65.0 * t)
                val chorus = sin(2 * PI * 65.5 * t) * 0.5
                val drip = sin(2 * PI * 2.0 * t).let { if (it > 0.8) it * 0.3 else 0.0 }
                val sub = sin(2 * PI * 32.5 * t) * 0.25
                (base + chorus + sub) * 0.25 + drip
            }
            FloorTheme.Caverns -> {
                // 50Hz resonant hum with detuned octave
                val base = sin(2 * PI * 50.0 * t)
                val octave = sin(2 * PI * 100.3 * t) * 0.35
                val detune = sin(2 * PI * 49.5 * t) * 0.4
                val slow = 0.8 + 0.2 * sin(2 * PI * 0.15 * t)
                (base + octave + detune) * 0.28 * slow
            }
            FloorTheme.Inferno -> {
                // 45Hz rumble with crackling noise texture
                val base = sin(2 * PI * 45.0 * t)
                val rumble = sin(2 * PI * 22.5 * t) * 0.5
                // Pseudo-noise using multiple inharmonic frequencies
                val crackle = (sin(2 * PI * 137.0 * t) * sin(2 * PI * 3.7 * t)) * 0.15
                val tremolo = 0.7 + 0.3 * sin(2 * PI * 1.5 * t)
                (base + rumble) * 0.3 * tremolo + crackle
            }
            FloorTheme.Void -> {
                // 60Hz ethereal pad with fifth harmony + slow LFO sweep
                val base = sin(2 * PI * 60.0 * t)
                val fifth = sin(2 * PI * 90.0 * t) * 0.45
                val octaveUp = sin(2 * PI * 120.0 * t) * 0.2
                val lfo = 0.7 + 0.3 * sin(2 * PI * 0.08 * t)
                val shimmer = sin(2 * PI * 180.0 * t) * sin(2 * PI * 0.5 * t) * 0.1
                (base + fifth + octaveUp) * 0.25 * lfo + shimmer
            }
        }
    }
}
