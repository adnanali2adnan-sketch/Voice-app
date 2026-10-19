package com.example.data

import com.example.model.FeatureKey
import com.example.model.GeminiLiveModel
import com.example.model.ReasoningLevel
import org.json.JSONArray
import org.json.JSONObject

/**
 * Builds real-time API request payloads and Live WebSocket configuration frames.
 * Actively includes or strictly blocks/omits parameters based on user toggle states.
 */
object PayloadBuilder {

    /**
     * Builds the complete, formatted JSON string reflecting current model and active toggles.
     */
    fun buildPayloadJson(
        model: GeminiLiveModel,
        toggles: Map<FeatureKey, Boolean>,
        reasoningLevel: ReasoningLevel,
        currentPrompt: String = ""
    ): String {
        val root = JSONObject()

        // 1. Model & Setup Header
        val setup = JSONObject()
        setup.put("model", "models/${model.modelId}")

        // 2. Generation Config
        val genConfig = JSONObject()
        genConfig.put("temperature", if (model == GeminiLiveModel.GEMINI_3_8_EXTENDED_THINKING) 0.2 else 0.7)
        genConfig.put("topP", 0.95)

        // 3. Modalities List (Actively conditioned on toggles)
        val responseModalities = JSONArray()
        val inputModalities = JSONArray()
        inputModalities.put("TEXT")
        responseModalities.put("TEXT")

        when (model) {
            GeminiLiveModel.GEMINI_3_5_TRANSLATE -> {
                val bidiStreaming = toggles[FeatureKey.BIDIRECTIONAL_STREAMING] ?: true
                val autoDetect = toggles[FeatureKey.AUTO_LANGUAGE_DETECTION] ?: true
                val intonation = toggles[FeatureKey.VOICE_INTONATION_PRESERVATION] ?: true
                val audioOutput = toggles[FeatureKey.REALTIME_AUDIO_OUTPUT] ?: true

                inputModalities.put("AUDIO")
                if (audioOutput) {
                    responseModalities.put("AUDIO")
                }

                val speechConfig = JSONObject()
                val voiceConfig = JSONObject()
                voiceConfig.put("preserveIntonation", intonation)
                voiceConfig.put("prosodyRate", 1.0)
                speechConfig.put("voiceConfig", voiceConfig)
                genConfig.put("speechConfig", speechConfig)

                val translationConfig = JSONObject()
                translationConfig.put("sourceLanguage", if (autoDetect) "auto_detect" else "en-US")
                translationConfig.put("targetLanguage", "es-ES") // dynamic translation target
                translationConfig.put("bidirectionalStreaming", bidiStreaming)
                setup.put("translationConfig", translationConfig)
            }

            GeminiLiveModel.GEMINI_3_5_TRANSCRIBE -> {
                val liveStt = toggles[FeatureKey.LIVE_SPEECH_TO_TEXT] ?: true
                val streamingDisplay = toggles[FeatureKey.STREAMING_TEXT_DISPLAY] ?: true
                val noiseFiltering = toggles[FeatureKey.NOISE_FILTERING] ?: true
                val autoPunctuation = toggles[FeatureKey.AUTO_PUNCTUATION] ?: true

                inputModalities.put("AUDIO")

                val transcriptionConfig = JSONObject()
                transcriptionConfig.put("liveSpeechToText", liveStt)
                transcriptionConfig.put("incrementalStreaming", streamingDisplay)
                transcriptionConfig.put("noiseSuppressionFilter", noiseFiltering)
                transcriptionConfig.put("autoPunctuation", autoPunctuation)
                transcriptionConfig.put("sampleRateHertz", 16000)
                setup.put("transcriptionConfig", transcriptionConfig)
            }

            GeminiLiveModel.GEMINI_3_8_LIVE -> {
                val voiceChat = toggles[FeatureKey.REALTIME_VOICE_CHAT] ?: true
                val videoFeed = toggles[FeatureKey.LIVE_VIDEO_SCREEN_FEED] ?: true
                val affective = toggles[FeatureKey.AFFECTIVE_DIALOGUE] ?: true
                val toolCalling = toggles[FeatureKey.ASYNC_TOOL_CALLING] ?: true

                if (voiceChat) {
                    inputModalities.put("AUDIO")
                    responseModalities.put("AUDIO")
                }

                // Explicit Access Control: Only add VIDEO modality if videoFeed toggle is active
                if (videoFeed) {
                    inputModalities.put("IMAGE")
                    inputModalities.put("VIDEO_STREAM")
                    val visionConfig = JSONObject()
                    visionConfig.put("maxFps", 1.0)
                    visionConfig.put("resolution", "720p")
                    setup.put("visionFeedConfig", visionConfig)
                }

                if (affective) {
                    val dialogueConfig = JSONObject()
                    dialogueConfig.put("emotionalProsody", "adaptive_affective")
                    dialogueConfig.put("toneExpressiveness", "high")
                    genConfig.put("dialogueConfig", dialogueConfig)
                }

                // Explicit Access Control: Only attach tools if asyncToolCalling is enabled
                if (toolCalling) {
                    val toolsArray = JSONArray()
                    val toolItem = JSONObject()
                    val funcList = JSONArray()

                    val weatherFunc = JSONObject()
                    weatherFunc.put("name", "queryEnvironmentalMetrics")
                    weatherFunc.put("description", "Query live environmental sensors and status")
                    funcList.put(weatherFunc)

                    val codeExecFunc = JSONObject()
                    codeExecFunc.put("name", "executeAsyncCalculation")
                    codeExecFunc.put("description", "Execute asynchronous background computing tasks")
                    funcList.put(codeExecFunc)

                    toolItem.put("functionDeclarations", funcList)
                    toolsArray.put(toolItem)
                    setup.put("tools", toolsArray)
                }
            }

            GeminiLiveModel.GEMINI_3_8_EXTENDED_THINKING -> {
                val multiStep = toggles[FeatureKey.BACKGROUND_MULTI_STEP_SOLVING] ?: true
                val earlyCues = toggles[FeatureKey.EARLY_VERBAL_CUES] ?: true
                val alphanumeric = toggles[FeatureKey.ALPHANUMERIC_PRECISION] ?: true

                val thinkingConfig = JSONObject()
                thinkingConfig.put("thinkingBudget", reasoningLevel.tokenBudget)
                thinkingConfig.put("includeThoughts", true)
                thinkingConfig.put("earlyVerbalCues", earlyCues)
                thinkingConfig.put("multiStepPlanning", multiStep)
                thinkingConfig.put("strictAlphanumericPrecision", alphanumeric)
                genConfig.put("thinkingConfig", thinkingConfig)

                val sysInstruction = JSONObject()
                val parts = JSONArray()
                val textPart = JSONObject()
                textPart.put(
                    "text",
                    "Deep reasoning mode active (${reasoningLevel.label} budget: ${reasoningLevel.tokenBudget} tokens). " +
                            "Break down problems into rigorous intermediate verification steps."
                )
                parts.put(textPart)
                sysInstruction.put("parts", parts)
                setup.put("systemInstruction", sysInstruction)
            }
        }

        genConfig.put("responseModalities", responseModalities)
        setup.put("inputModalities", inputModalities)
        setup.put("generationConfig", genConfig)

        root.put("setup", setup)

        // If a prompt is present, encapsulate the live user turn
        if (currentPrompt.isNotBlank()) {
            val clientContent = JSONObject()
            val turns = JSONArray()
            val turn = JSONObject()
            turn.put("role", "user")
            val parts = JSONArray()
            val part = JSONObject()
            part.put("text", currentPrompt)
            parts.put(part)
            turn.put("parts", parts)
            turns.put(turn)
            clientContent.put("turns", turns)
            clientContent.put("turnComplete", true)
            root.put("clientContent", clientContent)
        }

        return root.toString(2)
    }
}
