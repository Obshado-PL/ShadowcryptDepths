package com.shadowcrypt.game.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.shadowcrypt.game.model.GameEvent
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

class SoundManager {

    private val sampleRate = 22050
    private val executor = Executors.newSingleThreadExecutor()
    private val waveforms = mutableMapOf<GameEvent, ShortArray>()

    var volume: Float = 0.8f
    var enabled: Boolean = true

    init {
        // Pre-generate all waveforms on construction
        for (event in GameEvent.entries) {
            waveforms[event] = generateWaveform(event)
        }
    }

    fun play(event: GameEvent) {
        if (!enabled) return
        val waveform = waveforms[event] ?: return

        executor.execute {
            var track: AudioTrack? = null
            try {
                val scaled = applyVolume(waveform, volume)
                val bufferSize = scaled.size * 2 // 16-bit = 2 bytes per sample
                track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
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
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(scaled, 0, scaled.size)
                track.play()

                // Wait for playback to finish
                val durationMs = (scaled.size * 1000L) / sampleRate
                Thread.sleep(durationMs + 50)
            } catch (_: Exception) {
                // Silently ignore audio errors — game should never crash for sound
            } finally {
                try {
                    track?.stop()
                    track?.release()
                } catch (_: Exception) {
                    // Ignore cleanup errors
                }
            }
        }
    }

    fun release() {
        executor.shutdownNow()
    }

    // ===== Waveform Generation =====

    private fun generateWaveform(event: GameEvent): ShortArray = when (event) {
        GameEvent.PlayerMove -> generateClick(durationMs = 30, freq = 800.0)
        GameEvent.PlayerAttack -> generateSweep(durationMs = 80, startFreq = 600.0, endFreq = 200.0)
        GameEvent.PlayerHit -> generateThud(durationMs = 100, freq = 120.0)
        GameEvent.EnemyKilled -> generateSweep(durationMs = 120, startFreq = 400.0, endFreq = 900.0)
        GameEvent.BossKilled -> generateChord(durationMs = 300, baseFreq = 300.0)
        GameEvent.ItemPickup -> generateSweep(durationMs = 80, startFreq = 600.0, endFreq = 1200.0)
        GameEvent.ItemEquip -> generateClick(durationMs = 50, freq = 500.0)
        GameEvent.ItemUse -> generateSweep(durationMs = 100, startFreq = 400.0, endFreq = 800.0)
        GameEvent.LevelUp -> generateArpeggio(durationMs = 250)
        GameEvent.FloorDescend -> generateSweep(durationMs = 200, startFreq = 800.0, endFreq = 300.0)
        GameEvent.PlayerDeath -> generateSweep(durationMs = 300, startFreq = 400.0, endFreq = 80.0)
        GameEvent.Victory -> generateFanfare(durationMs = 400)
        GameEvent.TrapTriggered -> generateThud(durationMs = 80, freq = 180.0)
    }

    /** Short click/tick at a fixed frequency */
    private fun generateClick(durationMs: Int, freq: Double): ShortArray {
        val samples = (sampleRate * durationMs) / 1000
        val data = ShortArray(samples)
        for (i in 0 until samples) {
            val t = i.toDouble() / sampleRate
            val envelope = fadeEnvelope(i, samples)
            val value = sin(2.0 * PI * freq * t) * envelope * 0.6
            data[i] = (value * Short.MAX_VALUE).toInt().toShort()
        }
        return data
    }

    /** Frequency sweep from startFreq to endFreq */
    private fun generateSweep(durationMs: Int, startFreq: Double, endFreq: Double): ShortArray {
        val samples = (sampleRate * durationMs) / 1000
        val data = ShortArray(samples)
        var phase = 0.0
        for (i in 0 until samples) {
            val progress = i.toDouble() / samples
            val freq = startFreq + (endFreq - startFreq) * progress
            val envelope = fadeEnvelope(i, samples)
            phase += 2.0 * PI * freq / sampleRate
            val value = sin(phase) * envelope * 0.7
            data[i] = (value * Short.MAX_VALUE).toInt().toShort()
        }
        return data
    }

    /** Low bass thud */
    private fun generateThud(durationMs: Int, freq: Double): ShortArray {
        val samples = (sampleRate * durationMs) / 1000
        val data = ShortArray(samples)
        for (i in 0 until samples) {
            val t = i.toDouble() / sampleRate
            val progress = i.toDouble() / samples
            // Quick attack, slow decay
            val envelope = if (progress < 0.1) progress / 0.1 else (1.0 - progress) * 1.1
            // Mix sine + square for punch
            val sine = sin(2.0 * PI * freq * t)
            val square = if (sine > 0) 0.3 else -0.3
            val value = (sine * 0.7 + square) * envelope.coerceIn(0.0, 1.0)
            data[i] = (value * Short.MAX_VALUE).toInt().toShort()
        }
        return data
    }

    /** Triumphant chord (root + major third + fifth) */
    private fun generateChord(durationMs: Int, baseFreq: Double): ShortArray {
        val samples = (sampleRate * durationMs) / 1000
        val data = ShortArray(samples)
        val freqs = doubleArrayOf(baseFreq, baseFreq * 1.25, baseFreq * 1.5) // major chord
        for (i in 0 until samples) {
            val t = i.toDouble() / sampleRate
            val envelope = fadeEnvelope(i, samples)
            var value = 0.0
            for (f in freqs) {
                value += sin(2.0 * PI * f * t)
            }
            value = value / freqs.size * envelope * 0.7
            data[i] = (value * Short.MAX_VALUE).toInt().toShort()
        }
        return data
    }

    /** Ascending arpeggio (C-E-G-C') for level up */
    private fun generateArpeggio(durationMs: Int): ShortArray {
        val noteFreqs = doubleArrayOf(523.25, 659.25, 783.99, 1046.50) // C5-E5-G5-C6
        val totalSamples = (sampleRate * durationMs) / 1000
        val data = ShortArray(totalSamples)
        val samplesPerNote = totalSamples / noteFreqs.size

        for (noteIdx in noteFreqs.indices) {
            val startSample = noteIdx * samplesPerNote
            for (i in 0 until samplesPerNote) {
                val idx = startSample + i
                if (idx >= totalSamples) break
                val t = i.toDouble() / sampleRate
                val envelope = fadeEnvelope(i, samplesPerNote)
                val value = sin(2.0 * PI * noteFreqs[noteIdx] * t) * envelope * 0.6
                data[idx] = (value * Short.MAX_VALUE).toInt().toShort()
            }
        }
        return data
    }

    /** Victory fanfare — ascending chord sequence */
    private fun generateFanfare(durationMs: Int): ShortArray {
        // Two chords: C major then G major, ascending
        val chord1 = doubleArrayOf(523.25, 659.25, 783.99) // C5 major
        val chord2 = doubleArrayOf(783.99, 987.77, 1174.66) // G5 major
        val totalSamples = (sampleRate * durationMs) / 1000
        val halfSamples = totalSamples / 2
        val data = ShortArray(totalSamples)

        fun writeChord(freqs: DoubleArray, offset: Int, count: Int) {
            for (i in 0 until count) {
                val idx = offset + i
                if (idx >= totalSamples) break
                val t = i.toDouble() / sampleRate
                val envelope = fadeEnvelope(i, count)
                var value = 0.0
                for (f in freqs) {
                    value += sin(2.0 * PI * f * t)
                }
                value = value / freqs.size * envelope * 0.6
                data[idx] = (value * Short.MAX_VALUE).toInt().toShort()
            }
        }

        writeChord(chord1, 0, halfSamples)
        writeChord(chord2, halfSamples, halfSamples)
        return data
    }

    /** Smooth fade-in/fade-out envelope to avoid clicks */
    private fun fadeEnvelope(sample: Int, totalSamples: Int): Double {
        val fadeLen = min(totalSamples / 5, sampleRate / 100) // ~10ms fade
        return when {
            sample < fadeLen -> sample.toDouble() / fadeLen
            sample > totalSamples - fadeLen -> (totalSamples - sample).toDouble() / fadeLen
            else -> 1.0
        }
    }

    /** Scale waveform by volume */
    private fun applyVolume(waveform: ShortArray, vol: Float): ShortArray {
        if (vol >= 0.99f) return waveform
        val scaled = ShortArray(waveform.size)
        for (i in waveform.indices) {
            scaled[i] = (waveform[i] * vol).toInt().toShort()
        }
        return scaled
    }
}
