package com.example.model

/**
 * The exact 4 Live Gemini Models specified in requirements:
 * a) Gemini 3.5 Live Translate
 * b) Gemini 3.5 Transcribe Live
 * c) Gemini 3.8 Live
 * d) Gemini 3.8 Live Extended Thinking
 */
enum class GeminiLiveModel(
    val displayName: String,
    val modelId: String,
    val tag: String,
    val description: String,
    val defaultEndpoint: String
) {
    GEMINI_3_5_TRANSLATE(
        displayName = "Gemini 3.5 Live Translate",
        modelId = "gemini-3.5-live-translate",
        tag = "TRANSLATE",
        description = "Low-latency bidirectional speech & text translation with voice intonation preservation.",
        defaultEndpoint = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiStream"
    ),
    GEMINI_3_5_TRANSCRIBE(
        displayName = "Gemini 3.5 Transcribe Live",
        modelId = "gemini-3.5-transcribe-live",
        tag = "SPEECH-TO-TEXT",
        description = "Continuous live audio transcription with acoustic noise filtration and punctuation intelligence.",
        defaultEndpoint = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiStream"
    ),
    GEMINI_3_8_LIVE(
        displayName = "Gemini 3.8 Live",
        modelId = "gemini-3.8-live",
        tag = "MULTIMODAL LIVE",
        description = "Flagship real-time voice, vision & screen feed dialogue with emotional affective tone and async tools.",
        defaultEndpoint = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiStream"
    ),
    GEMINI_3_8_EXTENDED_THINKING(
        displayName = "Gemini 3.8 Live Extended Thinking",
        modelId = "gemini-3.8-live-extended-thinking",
        tag = "DEEP REASONING",
        description = "Advanced multi-step reasoning engine with verbal cueing, background solving, and alphanumeric precision.",
        defaultEndpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-thinking-exp:generateContent"
    );

    companion object {
        fun fromModelId(id: String): GeminiLiveModel =
            entries.firstOrNull { it.modelId == id } ?: GEMINI_3_8_LIVE
    }
}

/**
 * Feature keys for dynamic toggle controls per model
 */
enum class FeatureKey {
    // Gemini 3.5 Live Translate
    BIDIRECTIONAL_STREAMING,
    AUTO_LANGUAGE_DETECTION,
    VOICE_INTONATION_PRESERVATION,
    REALTIME_AUDIO_OUTPUT,

    // Gemini 3.5 Transcribe Live
    LIVE_SPEECH_TO_TEXT,
    STREAMING_TEXT_DISPLAY,
    NOISE_FILTERING,
    AUTO_PUNCTUATION,

    // Gemini 3.8 Live
    REALTIME_VOICE_CHAT,
    LIVE_VIDEO_SCREEN_FEED,
    AFFECTIVE_DIALOGUE,
    ASYNC_TOOL_CALLING,

    // Gemini 3.8 Live Extended Thinking
    BACKGROUND_MULTI_STEP_SOLVING,
    EARLY_VERBAL_CUES,
    ALPHANUMERIC_PRECISION
}

/**
 * Deep Reasoning Level for Gemini 3.8 Live Extended Thinking
 */
enum class ReasoningLevel(
    val label: String,
    val tokenBudget: Int,
    val description: String
) {
    LOW("Low", 1024, "Quick structured deliberation (~1K tokens)"),
    MEDIUM("Med", 4096, "Balanced multi-hop analytical reasoning (~4K tokens)"),
    HIGH("High", 16384, "Exhaustive mathematical verification & deep proof (~16K tokens)")
}

/**
 * Metadata definition for each toggle switch
 */
data class FeatureToggleSpec(
    val key: FeatureKey,
    val title: String,
    val description: String,
    val technicalParam: String,
    val defaultEnabled: Boolean = true
)

/**
 * Interactive Sandbox Message item
 */
data class SandboxMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val thoughts: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isStreaming: Boolean = false,
    val latencyMs: Long? = null,
    val audioDurationSeconds: Float? = null
)

enum class MessageSender {
    USER,
    MODEL,
    SYSTEM
}

/**
 * Live Session State
 */
sealed interface LiveSessionStatus {
    data object Idle : LiveSessionStatus
    data object Connecting : LiveSessionStatus
    data class Active(val connectedAt: Long = System.currentTimeMillis()) : LiveSessionStatus
    data class Streaming(val bytesStreamed: Long) : LiveSessionStatus
    data class Error(val message: String, val code: Int? = null) : LiveSessionStatus
}
