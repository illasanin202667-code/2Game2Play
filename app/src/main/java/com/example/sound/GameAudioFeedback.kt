package com.example.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

/**
 * Lightweight real-time sound and haptics generator.
 * Creates clean game audio tones directly using AudioTrack without any external assets.
 */
object GameAudioFeedback {
    private val scope = CoroutineScope(Dispatchers.Default)

    fun vibrate(context: Context, durationMs: Long = 50) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.vibrate(
                    CombinedVibration.createParallel(
                        VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(durationMs)
                }
            }
        } catch (_: Exception) {
            // Ignore if vibration unsupported
        }
    }

    fun vibratePattern(context: Context, timings: LongArray) {
        try {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(timings, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(timings, -1)
            }
        } catch (_: Exception) {
            // Ignore
        }
    }

    /**
     * Plays a synthesized tone of given frequency (Hz) and duration (ms).
     */
    fun playTone(freqHz: Double, durationMs: Int) {
        scope.launch {
            try {
                val sampleRate = 22050
                val numSamples = (durationMs * sampleRate) / 1000
                val samples = ShortArray(numSamples)
                val angularFreq = 2.0 * Math.PI * freqHz / sampleRate

                for (i in 0 until numSamples) {
                    // Apply smooth envelope to prevent clicking
                    val envelope = when {
                        i < 200 -> i / 200.0
                        i > numSamples - 200 -> (numSamples - i) / 200.0
                        else -> 1.0
                    }
                    samples[i] = (sin(angularFreq * i) * 28000 * envelope).toInt().toShort()
                }

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(samples.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(samples, 0, samples.size)
                audioTrack.play()
                // Let it play out then release
                kotlinx.coroutines.delay(durationMs.toLong() + 50)
                audioTrack.stop()
                audioTrack.release()
            } catch (_: Exception) {
                // AudioTrack fallback
            }
        }
    }

    fun playClick() {
        playTone(600.0, 35)
    }

    fun playTick() {
        playTone(900.0, 25)
    }

    fun playSuccess() {
        scope.launch {
            playTone(523.25, 80) // C5
            kotlinx.coroutines.delay(85)
            playTone(659.25, 80) // E5
            kotlinx.coroutines.delay(85)
            playTone(783.99, 150) // G5
        }
    }

    fun playPenalty() {
        scope.launch {
            playTone(220.0, 100)
            kotlinx.coroutines.delay(100)
            playTone(160.0, 180)
        }
    }

    fun playExplosion() {
        scope.launch {
            playTone(110.0, 250)
            kotlinx.coroutines.delay(50)
            playTone(85.0, 300)
        }
    }

    fun playFlash() {
        playTone(880.0, 100)
    }
}
