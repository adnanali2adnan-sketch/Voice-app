package com.example.util

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Universal Multi-Language Text-To-Speech & Voice Commentary Engine for Gemini Live.
 * Supports dynamic language switching: English, Urdu, Hindi, Spanish, and device default.
 */
object VoiceAssistantManager {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isVoiceEnabled = MutableStateFlow(true)
    val isVoiceEnabled: StateFlow<Boolean> = _isVoiceEnabled.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _lastSpokenText = MutableStateFlow("")
    val lastSpokenText: StateFlow<String> = _lastSpokenText.asStateFlow()

    fun init(context: Context) {
        if (isInitialized) return

        try {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    isInitialized = true
                    tts?.language = Locale.getDefault()
                    setupUtteranceListener()
                } else {
                    Log.w("VoiceAssistantManager", "TTS initialization returned status: $status")
                }
            }
        } catch (e: Exception) {
            Log.e("VoiceAssistantManager", "Failed to init TextToSpeech: ${e.message}")
        }
    }

    private fun setupUtteranceListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isSpeaking.value = true
            }

            override fun onDone(utteranceId: String?) {
                _isSpeaking.value = false
            }

            override fun onError(utteranceId: String?) {
                _isSpeaking.value = false
            }
        })
    }

    fun setVoiceEnabled(enabled: Boolean) {
        _isVoiceEnabled.value = enabled
        if (!enabled) {
            stopSpeech()
        }
    }

    fun stopSpeech() {
        try {
            tts?.stop()
            _isSpeaking.value = false
        } catch (_: Exception) {}
    }

    /**
     * Speaks the given text in appropriate language.
     * Cleans markdown formatting, brackets, and code blocks before vocalizing.
     */
    fun speak(text: String, languageHint: String? = null) {
        if (!_isVoiceEnabled.value || !isInitialized || text.isBlank()) return

        val cleanText = text
            .replace(Regex("\\[.*?\\]"), "")
            .replace(Regex("`.*?`"), "")
            .replace(Regex("[*#_~]"), "")
            .trim()

        if (cleanText.isBlank()) return

        _lastSpokenText.value = cleanText

        // Set locale based on languageHint or auto-detection
        val targetLocale = when {
            languageHint.equals("es", ignoreCase = true) || cleanText.contains("Hola", ignoreCase = true) -> Locale("es", "ES")
            languageHint.equals("ur", ignoreCase = true) || isUrduScript(cleanText) -> Locale("ur", "PK")
            languageHint.equals("hi", ignoreCase = true) || isHindiScript(cleanText) -> Locale("hi", "IN")
            else -> Locale.ENGLISH
        }

        try {
            val result = tts?.setLanguage(targetLocale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.language = Locale.ENGLISH
            }
            tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, "gemini_live_speech_${System.currentTimeMillis()}")
        } catch (e: Exception) {
            Log.e("VoiceAssistantManager", "TTS speak failed: ${e.message}")
        }
    }

    private fun isUrduScript(text: String): Boolean {
        return text.any { it in '\u0600'..'\u06FF' || it in '\uFB50'..'\uFDFF' || it in '\uFE70'..'\uFEFF' }
    }

    private fun isHindiScript(text: String): Boolean {
        return text.any { it in '\u0900'..'\u097F' }
    }
}
