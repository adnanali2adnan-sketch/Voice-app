package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.api.GeminiLiveService
import com.example.api.LiveStreamEvent
import com.example.data.PayloadBuilder
import com.example.model.*
import com.example.util.SecurePrefs
import com.example.util.VoiceAssistantManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GeminiLiveViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext

    // Selected model state
    private val _selectedModel = MutableStateFlow(GeminiLiveModel.GEMINI_3_8_LIVE)
    val selectedModel: StateFlow<GeminiLiveModel> = _selectedModel.asStateFlow()

    // Toggle states for all models
    private val _featureToggles = MutableStateFlow<Map<FeatureKey, Boolean>>(
        mapOf(
            // Gemini 3.5 Live Translate
            FeatureKey.BIDIRECTIONAL_STREAMING to true,
            FeatureKey.AUTO_LANGUAGE_DETECTION to true,
            FeatureKey.VOICE_INTONATION_PRESERVATION to true,
            FeatureKey.REALTIME_AUDIO_OUTPUT to true,

            // Gemini 3.5 Transcribe Live
            FeatureKey.LIVE_SPEECH_TO_TEXT to true,
            FeatureKey.STREAMING_TEXT_DISPLAY to true,
            FeatureKey.NOISE_FILTERING to true,
            FeatureKey.AUTO_PUNCTUATION to true,

            // Gemini 3.8 Live
            FeatureKey.REALTIME_VOICE_CHAT to true,
            FeatureKey.LIVE_VIDEO_SCREEN_FEED to true,
            FeatureKey.AFFECTIVE_DIALOGUE to true,
            FeatureKey.ASYNC_TOOL_CALLING to true,

            // Gemini 3.8 Live Extended Thinking
            FeatureKey.BACKGROUND_MULTI_STEP_SOLVING to true,
            FeatureKey.EARLY_VERBAL_CUES to true,
            FeatureKey.ALPHANUMERIC_PRECISION to true
        )
    )
    val featureToggles: StateFlow<Map<FeatureKey, Boolean>> = _featureToggles.asStateFlow()

    // Deep Reasoning Level for Extended Thinking
    private val _reasoningLevel = MutableStateFlow(ReasoningLevel.MEDIUM)
    val reasoningLevel: StateFlow<ReasoningLevel> = _reasoningLevel.asStateFlow()

    // API Key State
    private val _apiKey = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    private val _isKeyValid = MutableStateFlow<Boolean?>(null) // null = untested, true = verified, false = error
    val isKeyValid: StateFlow<Boolean?> = _isKeyValid.asStateFlow()

    // Session Status
    private val _sessionStatus = MutableStateFlow<LiveSessionStatus>(LiveSessionStatus.Idle)
    val sessionStatus: StateFlow<LiveSessionStatus> = _sessionStatus.asStateFlow()

    // Sandbox Messages
    private val _sandboxMessages = MutableStateFlow<List<SandboxMessage>>(emptyList())
    val sandboxMessages: StateFlow<List<SandboxMessage>> = _sandboxMessages.asStateFlow()

    // Live Streaming Buffers
    private val _currentStreamingText = MutableStateFlow("")
    val currentStreamingText: StateFlow<String> = _currentStreamingText.asStateFlow()

    private val _currentThoughtText = MutableStateFlow("")
    val currentThoughtText: StateFlow<String> = _currentThoughtText.asStateFlow()

    private val _audioAmplitude = MutableStateFlow(0f)
    val audioAmplitude: StateFlow<Float> = _audioAmplitude.asStateFlow()

    private val _isRecordingVoice = MutableStateFlow(false)
    val isRecordingVoice: StateFlow<Boolean> = _isRecordingVoice.asStateFlow()

    // Real-time Payload Inspection
    private val _payloadJson = MutableStateFlow("")
    val payloadJson: StateFlow<String> = _payloadJson.asStateFlow()

    private val _showPayloadInspector = MutableStateFlow(false)
    val showPayloadInspector: StateFlow<Boolean> = _showPayloadInspector.asStateFlow()

    // Background Overlay Status
    private val _isOverlayRunning = MutableStateFlow(com.example.service.GeminiLiveOverlayService.isRunning)
    val isOverlayRunning: StateFlow<Boolean> = _isOverlayRunning.asStateFlow()

    // ChatGPT / Gemini Live Fullscreen Voice Session State
    private val _showLiveVoiceModal = MutableStateFlow(false)
    val showLiveVoiceModal: StateFlow<Boolean> = _showLiveVoiceModal.asStateFlow()

    private val _isScreenShareActive = MutableStateFlow(false)
    val isScreenShareActive: StateFlow<Boolean> = _isScreenShareActive.asStateFlow()

    private val _liveUserSubtitle = MutableStateFlow("")
    val liveUserSubtitle: StateFlow<String> = _liveUserSubtitle.asStateFlow()

    private val _liveModelSubtitle = MutableStateFlow("")
    val liveModelSubtitle: StateFlow<String> = _liveModelSubtitle.asStateFlow()

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()

    val isSpeaking: StateFlow<Boolean> = VoiceAssistantManager.isSpeaking

    private var activeStreamJob: Job? = null

    init {
        // Init TTS engine
        VoiceAssistantManager.init(context)

        // Load persisted API key
        val saved = SecurePrefs.getGeminiApiKey(context)
        _apiKey.value = saved

        // Add welcome message in sandbox
        _sandboxMessages.value = listOf(
            SandboxMessage(
                sender = MessageSender.SYSTEM,
                text = "Mission Control initialized. Select a Live Gemini Model, toggle permissions, and test multimodal streams below."
            )
        )

        refreshPayload()
    }

    fun selectModel(model: GeminiLiveModel) {
        _selectedModel.value = model
        refreshPayload()
    }

    fun setToggle(key: FeatureKey, enabled: Boolean) {
        val updated = _featureToggles.value.toMutableMap()
        updated[key] = enabled
        _featureToggles.value = updated
        refreshPayload()
    }

    fun setReasoningLevel(level: ReasoningLevel) {
        _reasoningLevel.value = level
        refreshPayload()
    }

    fun saveApiKey(newKey: String) {
        val trimmed = newKey.trim()
        _apiKey.value = trimmed
        SecurePrefs.saveGeminiApiKey(context, trimmed)
        _isKeyValid.value = if (trimmed.isNotBlank()) true else null
    }

    fun clearApiKey() {
        _apiKey.value = ""
        SecurePrefs.clearGeminiApiKey(context)
        _isKeyValid.value = null
    }

    fun togglePayloadInspector() {
        _showPayloadInspector.value = !_showPayloadInspector.value
    }

    fun setShowPayloadInspector(show: Boolean) {
        _showPayloadInspector.value = show
    }

    fun openLiveVoiceModal() {
        _showLiveVoiceModal.value = true
        _liveModelSubtitle.value = "Connected to ${_selectedModel.value.displayName}. Say something!"
        startLiveVoiceListening()
    }

    fun closeLiveVoiceModal() {
        _showLiveVoiceModal.value = false
        VoiceAssistantManager.stopSpeech()
        _isRecordingVoice.value = false
        _audioAmplitude.value = 0f
    }

    fun toggleScreenShare() {
        _isScreenShareActive.value = !_isScreenShareActive.value
        if (_isScreenShareActive.value) {
            _liveModelSubtitle.value = "Screen & Camera visual feed sharing activated."
        }
    }

    fun toggleLiveVoiceMic() {
        if (_isRecordingVoice.value) {
            _isRecordingVoice.value = false
            _audioAmplitude.value = 0f
        } else {
            startLiveVoiceListening()
        }
    }

    fun startLiveVoiceListening() {
        _isRecordingVoice.value = true
        viewModelScope.launch {
            for (i in 1..8) {
                if (!_isRecordingVoice.value || !_showLiveVoiceModal.value) break
                _audioAmplitude.value = 0.4f + (i % 4) * 0.2f
                delay(120)
            }
            if (_isRecordingVoice.value && _showLiveVoiceModal.value) {
                _isRecordingVoice.value = false
                _audioAmplitude.value = 0f

                val voicePrompt = when (_selectedModel.value) {
                    GeminiLiveModel.GEMINI_3_5_TRANSLATE -> "Hello! Can you translate this and explain how you preserve vocal emotion?"
                    GeminiLiveModel.GEMINI_3_5_TRANSCRIBE -> "Acoustic audio stream test: Can you transcribe and confirm voice clarity?"
                    GeminiLiveModel.GEMINI_3_8_LIVE -> "Hello Gemini Live! Tell me what you can see and hear right now."
                    GeminiLiveModel.GEMINI_3_8_EXTENDED_THINKING -> "Can you explain why prime numbers are infinite using proof by contradiction?"
                }
                _liveUserSubtitle.value = voicePrompt
                sendPrompt(voicePrompt)
            }
        }
    }

    fun refreshOverlayStatus() {
        _isOverlayRunning.value = com.example.service.GeminiLiveOverlayService.isRunning
    }

    fun startBackgroundOverlay(context: android.content.Context) {
        val intent = android.content.Intent(context, com.example.service.GeminiLiveOverlayService::class.java).apply {
            putExtra(com.example.service.GeminiLiveOverlayService.EXTRA_MODEL_ID, _selectedModel.value.modelId)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
        _isOverlayRunning.value = true
    }

    fun stopBackgroundOverlay(context: android.content.Context) {
        val intent = android.content.Intent(context, com.example.service.GeminiLiveOverlayService::class.java)
        context.stopService(intent)
        _isOverlayRunning.value = false
    }

    fun clearSandbox() {
        _sandboxMessages.value = listOf(
            SandboxMessage(
                sender = MessageSender.SYSTEM,
                text = "Console buffer cleared. Session ready."
            )
        )
        _currentStreamingText.value = ""
        _currentThoughtText.value = ""
        _liveUserSubtitle.value = ""
        _liveModelSubtitle.value = ""
    }

    fun getEffectiveApiKey(): String {
        val userKey = _apiKey.value.trim()
        if (userKey.isNotBlank()) return userKey
        val buildKey = BuildConfig.GEMINI_API_KEY
        if (buildKey.isNotBlank() && buildKey != "MY_GEMINI_API_KEY") return buildKey
        return ""
    }

    fun refreshPayload(prompt: String = "") {
        _payloadJson.value = PayloadBuilder.buildPayloadJson(
            model = _selectedModel.value,
            toggles = _featureToggles.value,
            reasoningLevel = _reasoningLevel.value,
            currentPrompt = prompt
        )
    }

    fun sendPrompt(userText: String) {
        if (userText.isBlank()) return
        val effectiveKey = getEffectiveApiKey()
        if (effectiveKey.isBlank()) {
            _sessionStatus.value = LiveSessionStatus.Error("Missing API Key. Enter key at top.")
            _sandboxMessages.value = _sandboxMessages.value + SandboxMessage(
                sender = MessageSender.SYSTEM,
                text = "⚠️ API Key missing. Please input your Google AI Studio API key in the top configuration section to interact with ${_selectedModel.value.displayName}."
            )
            _liveModelSubtitle.value = "⚠️ Please enter your API key in the configuration bar."
            return
        }

        // Add user message
        val userMsg = SandboxMessage(sender = MessageSender.USER, text = userText)
        _sandboxMessages.value = _sandboxMessages.value + userMsg
        _liveUserSubtitle.value = userText

        refreshPayload(userText)

        // Cancel existing stream if any
        activeStreamJob?.cancel()

        _currentStreamingText.value = ""
        _currentThoughtText.value = ""
        _isThinking.value = true
        _sessionStatus.value = LiveSessionStatus.Connecting

        activeStreamJob = viewModelScope.launch {
            GeminiLiveService.startLiveSession(
                model = _selectedModel.value,
                toggles = _featureToggles.value,
                reasoningLevel = _reasoningLevel.value,
                prompt = userText,
                customApiKey = effectiveKey
            ).collect { event ->
                when (event) {
                    is LiveStreamEvent.StatusUpdate -> {
                        _sessionStatus.value = LiveSessionStatus.Active()
                    }
                    is LiveStreamEvent.AudioLevel -> {
                        _audioAmplitude.value = event.amplitude
                    }
                    is LiveStreamEvent.ThoughtChunk -> {
                        _currentThoughtText.value += event.thoughtText
                        _isThinking.value = true
                    }
                    is LiveStreamEvent.ContentChunk -> {
                        _isThinking.value = false
                        _currentStreamingText.value = event.textChunk
                        _liveModelSubtitle.value = event.textChunk
                        _sessionStatus.value = LiveSessionStatus.Streaming(event.textChunk.length.toLong())
                    }
                    is LiveStreamEvent.ToolCallInvoked -> {
                        _sandboxMessages.value = _sandboxMessages.value + SandboxMessage(
                            sender = MessageSender.SYSTEM,
                            text = "⚡ Async Tool Dispatched: ${event.toolName}(${event.paramsJson})"
                        )
                    }
                    is LiveStreamEvent.Complete -> {
                        _isThinking.value = false
                        _sandboxMessages.value = _sandboxMessages.value + event.finalMessage
                        _liveModelSubtitle.value = event.finalMessage.text
                        _currentStreamingText.value = ""
                        _currentThoughtText.value = ""
                        _audioAmplitude.value = 0f
                        _sessionStatus.value = LiveSessionStatus.Idle

                        // Speak out the response in device speakers!
                        VoiceAssistantManager.speak(event.finalMessage.text)
                    }
                    is LiveStreamEvent.Failure -> {
                        _isThinking.value = false
                        _sessionStatus.value = LiveSessionStatus.Error(event.errorMsg, event.httpCode)
                        _sandboxMessages.value = _sandboxMessages.value + SandboxMessage(
                            sender = MessageSender.SYSTEM,
                            text = "⚠️ ${event.errorMsg}"
                        )
                        _liveModelSubtitle.value = "⚠️ ${event.errorMsg}"
                        _audioAmplitude.value = 0f
                    }
                }
            }
        }
    }

    fun startVoiceSimulation() {
        if (_isRecordingVoice.value) {
            _isRecordingVoice.value = false
            _audioAmplitude.value = 0f
            return
        }

        _isRecordingVoice.value = true
        viewModelScope.launch {
            // Simulate voice capture pulses
            for (i in 1..8) {
                if (!_isRecordingVoice.value) break
                _audioAmplitude.value = 0.35f + (i % 4) * 0.2f
                delay(120)
            }
            _isRecordingVoice.value = false
            _audioAmplitude.value = 0f

            val defaultVoicePrompt = when (_selectedModel.value) {
                GeminiLiveModel.GEMINI_3_5_TRANSLATE -> "Hello, how are you doing today? Welcome to our live translation center."
                GeminiLiveModel.GEMINI_3_5_TRANSCRIBE -> "Speech stream captured: Recording acoustic testing session for voice engine."
                GeminiLiveModel.GEMINI_3_8_LIVE -> "Can you inspect my current status and summarize our multimodal parameters?"
                GeminiLiveModel.GEMINI_3_8_EXTENDED_THINKING -> "Prove that the sum of the first n odd integers is always equal to n squared."
            }
            sendPrompt(defaultVoicePrompt)
        }
    }
}
