package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FeatureKey
import com.example.model.GeminiLiveModel
import com.example.model.ReasoningLevel
import com.example.ui.theme.*

@Composable
fun DynamicControlPanel(
    selectedModel: GeminiLiveModel,
    featureToggles: Map<FeatureKey, Boolean>,
    reasoningLevel: ReasoningLevel,
    onToggleChanged: (FeatureKey, Boolean) -> Unit,
    onReasoningLevelChanged: (ReasoningLevel) -> Unit,
    onInspectPayloadClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val modelColor = when (selectedModel) {
        GeminiLiveModel.GEMINI_3_5_TRANSLATE -> GeminiCyan
        GeminiLiveModel.GEMINI_3_5_TRANSCRIBE -> GeminiEmerald
        GeminiLiveModel.GEMINI_3_8_LIVE -> GeminiCyan
        GeminiLiveModel.GEMINI_3_8_EXTENDED_THINKING -> GeminiViolet
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("dynamic_control_panel"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = GeminiSurface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(listOf(modelColor.copy(alpha = 0.4f), GeminiBorder))
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header with Inspect Payload Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "DYNAMIC FEATURE CONTROLS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GeminiTextMuted,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Active Capability Permissions",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = GeminiTextPrimary
                    )
                }

                FilledTonalButton(
                    onClick = onInspectPayloadClicked,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = modelColor.copy(alpha = 0.15f),
                        contentColor = modelColor
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("inspect_payload_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = "Inspect",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "INSPECT PAYLOAD",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = "Toggles actively alter parameters in the model payload. Disabled items are strictly omitted or blocked.",
                fontSize = 11.sp,
                color = GeminiTextSecondary
            )

            // Dynamic Toggles List based on active model
            when (selectedModel) {
                GeminiLiveModel.GEMINI_3_5_TRANSLATE -> {
                    ToggleItemRow(
                        title = "Bidirectional Streaming",
                        description = "Enables simultaneous audio/text streaming pipe in both directions",
                        technicalTag = "translationConfig.bidirectionalStreaming",
                        enabled = featureToggles[FeatureKey.BIDIRECTIONAL_STREAMING] ?: true,
                        activeColor = GeminiCyan,
                        icon = Icons.Default.SyncAlt,
                        onToggle = { onToggleChanged(FeatureKey.BIDIRECTIONAL_STREAMING, it) }
                    )
                    ToggleItemRow(
                        title = "Auto-Language Detection",
                        description = "Automatically detects source language instead of locking to default",
                        technicalTag = "sourceLanguage: \"auto_detect\"",
                        enabled = featureToggles[FeatureKey.AUTO_LANGUAGE_DETECTION] ?: true,
                        activeColor = GeminiCyan,
                        icon = Icons.Default.Language,
                        onToggle = { onToggleChanged(FeatureKey.AUTO_LANGUAGE_DETECTION, it) }
                    )
                    ToggleItemRow(
                        title = "Voice Intonation Preservation",
                        description = "Maintains speaker's emotional inflection and emphasis in translation",
                        technicalTag = "voiceConfig.preserveIntonation",
                        enabled = featureToggles[FeatureKey.VOICE_INTONATION_PRESERVATION] ?: true,
                        activeColor = GeminiCyan,
                        icon = Icons.Default.GraphicEq,
                        onToggle = { onToggleChanged(FeatureKey.VOICE_INTONATION_PRESERVATION, it) }
                    )
                    ToggleItemRow(
                        title = "Real-time Audio Output",
                        description = "Synthesizes low-latency live audio stream packets back to client",
                        technicalTag = "responseModalities: [\"AUDIO\", \"TEXT\"]",
                        enabled = featureToggles[FeatureKey.REALTIME_AUDIO_OUTPUT] ?: true,
                        activeColor = GeminiCyan,
                        icon = Icons.Default.VolumeUp,
                        onToggle = { onToggleChanged(FeatureKey.REALTIME_AUDIO_OUTPUT, it) }
                    )
                }

                GeminiLiveModel.GEMINI_3_5_TRANSCRIBE -> {
                    ToggleItemRow(
                        title = "Live Speech-to-Text",
                        description = "Continuous live acoustic intake and real-time speech tokenization",
                        technicalTag = "transcriptionConfig.liveSpeechToText",
                        enabled = featureToggles[FeatureKey.LIVE_SPEECH_TO_TEXT] ?: true,
                        activeColor = GeminiEmerald,
                        icon = Icons.Default.Mic,
                        onToggle = { onToggleChanged(FeatureKey.LIVE_SPEECH_TO_TEXT, it) }
                    )
                    ToggleItemRow(
                        title = "Streaming Text Display",
                        description = "Emits partial interim transcript tokens as words are spoken",
                        technicalTag = "transcriptionConfig.incrementalStreaming",
                        enabled = featureToggles[FeatureKey.STREAMING_TEXT_DISPLAY] ?: true,
                        activeColor = GeminiEmerald,
                        icon = Icons.Default.TextFields,
                        onToggle = { onToggleChanged(FeatureKey.STREAMING_TEXT_DISPLAY, it) }
                    )
                    ToggleItemRow(
                        title = "Noise Filtering",
                        description = "Suppresses background hum, echo, and environmental acoustic noise",
                        technicalTag = "transcriptionConfig.noiseSuppressionFilter",
                        enabled = featureToggles[FeatureKey.NOISE_FILTERING] ?: true,
                        activeColor = GeminiEmerald,
                        icon = Icons.Default.FilterAlt,
                        onToggle = { onToggleChanged(FeatureKey.NOISE_FILTERING, it) }
                    )
                    ToggleItemRow(
                        title = "Auto-Punctuation",
                        description = "Applies sentence boundary detection, commas, and capitalization",
                        technicalTag = "transcriptionConfig.autoPunctuation",
                        enabled = featureToggles[FeatureKey.AUTO_PUNCTUATION] ?: true,
                        activeColor = GeminiEmerald,
                        icon = Icons.Default.FormatQuote,
                        onToggle = { onToggleChanged(FeatureKey.AUTO_PUNCTUATION, it) }
                    )
                }

                GeminiLiveModel.GEMINI_3_8_LIVE -> {
                    ToggleItemRow(
                        title = "Real-time Voice Chat",
                        description = "Bidirectional PCM audio conversation over WebSocket stream",
                        technicalTag = "modalities: [\"AUDIO\", \"TEXT\"]",
                        enabled = featureToggles[FeatureKey.REALTIME_VOICE_CHAT] ?: true,
                        activeColor = GeminiCyan,
                        icon = Icons.Default.RecordVoiceOver,
                        onToggle = { onToggleChanged(FeatureKey.REALTIME_VOICE_CHAT, it) }
                    )
                    ToggleItemRow(
                        title = "Live Video / Screen Feed",
                        description = "Streams camera frames or screen capture to model. If OFF, video is blocked",
                        technicalTag = "inputModalities: [\"IMAGE\", \"VIDEO_STREAM\"]",
                        enabled = featureToggles[FeatureKey.LIVE_VIDEO_SCREEN_FEED] ?: true,
                        activeColor = GeminiCyan,
                        icon = Icons.Default.Videocam,
                        onToggle = { onToggleChanged(FeatureKey.LIVE_VIDEO_SCREEN_FEED, it) }
                    )
                    ToggleItemRow(
                        title = "Affective Dialogue",
                        description = "Expressive vocal prosody, warmth, and dynamic emotional tone",
                        technicalTag = "dialogueConfig.emotionalProsody",
                        enabled = featureToggles[FeatureKey.AFFECTIVE_DIALOGUE] ?: true,
                        activeColor = GeminiCyan,
                        icon = Icons.Default.SentimentSatisfiedAlt,
                        onToggle = { onToggleChanged(FeatureKey.AFFECTIVE_DIALOGUE, it) }
                    )
                    ToggleItemRow(
                        title = "Asynchronous Tool Calling",
                        description = "Allows live model to invoke external tools & queries. If OFF, tools are omitted",
                        technicalTag = "tools: [functionDeclarations]",
                        enabled = featureToggles[FeatureKey.ASYNC_TOOL_CALLING] ?: true,
                        activeColor = GeminiCyan,
                        icon = Icons.Default.Build,
                        onToggle = { onToggleChanged(FeatureKey.ASYNC_TOOL_CALLING, it) }
                    )
                }

                GeminiLiveModel.GEMINI_3_8_EXTENDED_THINKING -> {
                    // Deep Reasoning Level Selector (Low / Med / High)
                    ReasoningLevelSelector(
                        currentLevel = reasoningLevel,
                        onLevelSelected = onReasoningLevelChanged
                    )

                    ToggleItemRow(
                        title = "Background Multi-step Problem Solving",
                        description = "Enables multi-branch proof expansion and intermediate theorem checking",
                        technicalTag = "thinkingConfig.multiStepPlanning: true",
                        enabled = featureToggles[FeatureKey.BACKGROUND_MULTI_STEP_SOLVING] ?: true,
                        activeColor = GeminiViolet,
                        icon = Icons.Default.AccountTree,
                        onToggle = { onToggleChanged(FeatureKey.BACKGROUND_MULTI_STEP_SOLVING, it) }
                    )
                    ToggleItemRow(
                        title = "Early Verbal Cues",
                        description = "Emits preliminary vocalized reasoning signals while computing proofs",
                        technicalTag = "thinkingConfig.earlyVerbalCues: true",
                        enabled = featureToggles[FeatureKey.EARLY_VERBAL_CUES] ?: true,
                        activeColor = GeminiViolet,
                        icon = Icons.Default.Hearing,
                        onToggle = { onToggleChanged(FeatureKey.EARLY_VERBAL_CUES, it) }
                    )
                    ToggleItemRow(
                        title = "Alphanumeric Precision",
                        description = "Forces exact numerical formatting, equation bounds, and symbol constraints",
                        technicalTag = "thinkingConfig.strictAlphanumericPrecision",
                        enabled = featureToggles[FeatureKey.ALPHANUMERIC_PRECISION] ?: true,
                        activeColor = GeminiViolet,
                        icon = Icons.Default.Calculate,
                        onToggle = { onToggleChanged(FeatureKey.ALPHANUMERIC_PRECISION, it) }
                    )
                }
            }
        }
    }
}

@Composable
fun ToggleItemRow(
    title: String,
    description: String,
    technicalTag: String,
    enabled: Boolean,
    activeColor: Color,
    icon: ImageVector,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 1.dp,
                color = if (enabled) activeColor.copy(alpha = 0.35f) else GeminiBorder.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onToggle(!enabled) }
            .testTag("toggle_${title.replace(" ", "_").lowercase()}"),
        color = if (enabled) GeminiSurfaceVariant.copy(alpha = 0.7f) else GeminiSurfaceVariant.copy(alpha = 0.25f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (enabled) activeColor.copy(alpha = 0.2f) else GeminiBorder.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (enabled) activeColor else GeminiTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (enabled) GeminiTextPrimary else GeminiTextMuted
                    )
                    Text(
                        text = description,
                        fontSize = 11.sp,
                        color = GeminiTextSecondary,
                        lineHeight = 15.sp
                    )
                    Text(
                        text = technicalTag,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (enabled) activeColor.copy(alpha = 0.8f) else GeminiRed.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = activeColor,
                    uncheckedThumbColor = GeminiTextMuted,
                    uncheckedTrackColor = GeminiBorder
                ),
                modifier = Modifier.testTag("switch_${title.replace(" ", "_").lowercase()}")
            )
        }
    }
}

@Composable
fun ReasoningLevelSelector(
    currentLevel: ReasoningLevel,
    onLevelSelected: (ReasoningLevel) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(GeminiSurfaceVariant.copy(alpha = 0.5f))
            .border(1.dp, GeminiViolet.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Memory,
                    contentDescription = null,
                    tint = GeminiViolet,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Deep Reasoning Level",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = GeminiTextPrimary
                )
            }
            Text(
                text = "${currentLevel.tokenBudget} Tokens",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = GeminiViolet
            )
        }

        Text(
            text = currentLevel.description,
            fontSize = 11.sp,
            color = GeminiTextSecondary
        )

        // 3-way Segmented Buttons (Low, Med, High)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(GeminiDarkBg)
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ReasoningLevel.entries.forEach { level ->
                val isSelected = level == currentLevel
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onLevelSelected(level) }
                        .testTag("reasoning_level_${level.name.lowercase()}"),
                    color = if (isSelected) GeminiViolet else Color.Transparent
                ) {
                    Text(
                        text = level.label.uppercase(),
                        modifier = Modifier.padding(vertical = 8.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.Black else GeminiTextSecondary
                    )
                }
            }
        }
    }
}
