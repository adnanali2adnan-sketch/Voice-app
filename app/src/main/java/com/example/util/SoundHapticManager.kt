package com.example.util

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
import kotlin.math.PI
import kotlin.math.sin

/**
 * Utility for synthesized futuristic sci-fi sound ticks and tactile haptic feedback.
 * Does not require external audio assets - generates clean synth sine-waves programmatically.
 */
object SoundHapticManager {

    private val audioScope = CoroutineScope(Dispatchers.Default)

    /**
     * Trigger tactile haptic pulse
     */
    fun triggerHaptic(context: Context, type: HapticType = HapticType.LIGHT_TICK) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                val effect = when (type) {
                    HapticType.LIGHT_TICK -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                    HapticType.PREDICTION_PULSE -> VibrationEffect.createWaveform(longArrayOf(0, 30, 40, 45), intArrayOf(0, 180, 0, 240), -1)
                    HapticType.STREAK_CHANGE -> VibrationEffect.createWaveform(longArrayOf(0, 50, 60, 80), intArrayOf(0, 200, 0, 255), -1)
                    HapticType.SUCCESS_WIN -> VibrationEffect.createWaveform(longArrayOf(0, 40, 50, 40, 50, 60), intArrayOf(0, 150, 0, 200, 0, 255), -1)
                    HapticType.DANGER_ALERT -> VibrationEffect.createWaveform(longArrayOf(0, 80, 50, 80), intArrayOf(0, 255, 0, 255), -1)
                }
                vibrator?.vibrate(effect)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                val effect = when (type) {
                    HapticType.LIGHT_TICK -> VibrationEffect.createOneShot(18, 120)
                    HapticType.PREDICTION_PULSE -> VibrationEffect.createWaveform(longArrayOf(0, 30, 40, 45), -1)
                    HapticType.STREAK_CHANGE -> VibrationEffect.createWaveform(longArrayOf(0, 50, 60, 80), -1)
                    HapticType.SUCCESS_WIN -> VibrationEffect.createWaveform(longArrayOf(0, 40, 50, 40, 50, 60), -1)
                    HapticType.DANGER_ALERT -> VibrationEffect.createWaveform(longArrayOf(0, 80, 50, 80), -1)
                }
                vibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(35)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Synthesize clean sci-fi chime / futuristic tick without requiring mp3 assets
     */
    fun playFuturisticSound(soundType: SoundType = SoundType.SOFT_TICK) {
        audioScope.launch {
            try {
                when (soundType) {
                    SoundType.SOFT_TICK -> playTone(frequency = 880.0, durationMs = 35, startVolume = 0.4f, endVolume = 0.05f)
                    SoundType.PREDICTION_CHIME -> {
                        playTone(frequency = 1046.5, durationMs = 50, startVolume = 0.5f, endVolume = 0.1f)
                        playTone(frequency = 1318.5, durationMs = 90, startVolume = 0.6f, endVolume = 0.02f)
                    }
                    SoundType.STREAK_ALERT -> {
                        playTone(frequency = 659.25, durationMs = 50, startVolume = 0.5f, endVolume = 0.2f)
                        playTone(frequency = 987.77, durationMs = 120, startVolume = 0.6f, endVolume = 0.02f)
                    }
                    SoundType.WIN_CHIME -> {
                        playTone(frequency = 784.0, durationMs = 60, startVolume = 0.5f, endVolume = 0.3f)
                        playTone(frequency = 1046.5, durationMs = 70, startVolume = 0.6f, endVolume = 0.3f)
                        playTone(frequency = 1567.98, durationMs = 150, startVolume = 0.7f, endVolume = 0.02f)
                    }
                    SoundType.DANGER_ALERT -> {
                        playTone(frequency = 440.0, durationMs = 80, startVolume = 0.6f, endVolume = 0.1f)
                        playTone(frequency = 370.0, durationMs = 120, startVolume = 0.7f, endVolume = 0.02f)
                    }
                }
            } catch (e: Exception) {
                // Ignore audio synthesis errors on headless/restricted environments
            }
        }
    }

    private fun playTone(
        frequency: Double,
        durationMs: Int,
        startVolume: Float = 0.6f,
        endVolume: Float = 0.05f
    ) {
        val sampleRate = 44100
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt().coerceAtLeast(1)
        val generatedSnd = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val fraction = i.toDouble() / numSamples
            val currentVolume = (startVolume + (endVolume - startVolume) * fraction).coerceIn(0.0, 1.0)
            val angle = 2.0 * PI * i * (frequency / sampleRate)
            // Apply subtle sine wave with smooth linear decay envelope
            val sampleVal = (sin(angle) * currentVolume * Short.MAX_VALUE).toInt().toShort()
            generatedSnd[i] = sampleVal
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
            .setBufferSizeInBytes(generatedSnd.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        audioTrack.write(generatedSnd, 0, generatedSnd.size)
        audioTrack.play()

        // Auto release after sound completes
        audioTrack.setNotificationMarkerPosition(numSamples)
        audioTrack.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
            override fun onMarkerReached(track: AudioTrack?) {
                try {
                    track?.stop()
                    track?.release()
                } catch (e: Exception) {}
            }
            override fun onPeriodicNotification(track: AudioTrack?) {}
        })
    }

    enum class HapticType {
        LIGHT_TICK,
        PREDICTION_PULSE,
        STREAK_CHANGE,
        SUCCESS_WIN,
        DANGER_ALERT
    }

    enum class SoundType {
        SOFT_TICK,
        PREDICTION_CHIME,
        STREAK_ALERT,
        WIN_CHIME,
        DANGER_ALERT
    }
}
