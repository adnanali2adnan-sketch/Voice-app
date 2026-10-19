package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun InteractiveSandbox(
    selectedModel: GeminiLiveModel,
    messages: List<SandboxMessage>,
    currentStreamingText: String,
    currentThoughtText: String,
    audioAmplitude: Float,
    isRecordingVoice: Boolean,
    sessionStatus: LiveSessionStatus,
    hasApiKey: Boolean,
    onSendMessage: (String) -> Unit,
    onVoiceToggle: () -> Unit,
    onClearMessages: () -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Scroll to bottom when new messages arrive or text streams
    LaunchedEffect(messages.size, currentStreamingText, currentThoughtText) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size)
        }
    }

    val modelColor = when (selectedModel) {
        GeminiLiveModel.GEMINI_3_5_TRANSLATE -> GeminiCyan
        GeminiLiveModel.GEMINI_3_5_TRANSCRIBE -> GeminiEmerald
        GeminiLiveModel.GEMINI_3_8_LIVE -> GeminiCyan
        GeminiLiveModel.GEMINI_3_8_EXTENDED_THINKING -> GeminiViolet
    }

    // Quick test prompt chips based on selected model
    val samplePrompts = remember(selectedModel) {
        when (selectedModel) {
            GeminiLiveModel.GEMINI_3_5_TRANSLATE -> listOf(
                "Hello, how are you? Welcome to our live conference.",
                "The quantum compute architecture is functioning optimally.",
                "Where can I find the nearest transport station?"
            )
            GeminiLiveModel.GEMINI_3_5_TRANSCRIBE -> listOf(
                "Live microphone acoustic check one two three.",
                "Testing real-time transcription with complex punctuation and noise suppression.",
                "Alpha seven nine zero status confirmed."
            )
            GeminiLiveModel.GEMINI_3_8_LIVE -> listOf(
                "Hello! Can you introduce your multimodal live capabilities?",
                "Analyze my current visual and audio stream with high warmth.",
                "Can you calculate the environmental metrics asynchronously?"
            )
            GeminiLiveModel.GEMINI_3_8_EXTENDED_THINKING -> listOf(
                "Prove by induction that sum(1 to n) = n(n+1)/2.",
                "If a train leaves Station A at 60km/h and Station B at 90km/h 300km apart, when do they meet?",
                "Provide an exhaustive proof of why sqrt(2) is irrational."
            )
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("interactive_sandbox"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = GeminiSurface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(listOf(GeminiBorder, modelColor.copy(alpha = 0.4f)))
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                when (sessionStatus) {
                                    is LiveSessionStatus.Streaming -> GeminiEmerald
                                    is LiveSessionStatus.Active -> GeminiCyan
                                    is LiveSessionStatus.Connecting -> GeminiAmber
                                    is LiveSessionStatus.Error -> GeminiRed
                                    else -> GeminiTextMuted
                                }
                            )
                    )
                    Text(
                        text = "LIVE SANDBOX & TESTING AREA",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GeminiTextMuted,
                        letterSpacing = 1.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = onClearMessages,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("clear_sandbox_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Clear",
                            tint = GeminiTextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Audio Streaming Waveform Visualizer
            AudioVisualizerWaveform(
                amplitude = audioAmplitude,
                isActive = audioAmplitude > 0.05f || isRecordingVoice || sessionStatus is LiveSessionStatus.Streaming,
                accentColor = modelColor
            )

            // Missing API Key Warning Banner if applicable
            if (!hasApiKey) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = GeminiRed.copy(alpha = 0.15f),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(GeminiRed.copy(alpha = 0.5f)))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = GeminiRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "API Key required to initiate live stream. Enter your key in the API Credentials card above.",
                            fontSize = 11.sp,
                            color = GeminiRed,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Chat / Stream Message Window
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 220.dp, max = 340.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, GeminiBorder, RoundedCornerShape(12.dp)),
                color = GeminiDarkBg
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        SandboxMessageItem(msg = msg, accentColor = modelColor)
                    }

                    // Live thoughts in progress (Extended Thinking)
                    if (currentThoughtText.isNotBlank()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = GeminiViolet.copy(alpha = 0.12f),
                                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(GeminiViolet.copy(alpha = 0.4f)))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Psychology,
                                            contentDescription = null,
                                            tint = GeminiViolet,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "LIVE REASONING IN PROGRESS...",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GeminiViolet
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = currentThoughtText,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = GeminiViolet.copy(alpha = 0.9f),
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }

                    // Live Streaming Content in progress
                    if (currentStreamingText.isNotBlank()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = GeminiSurfaceVariant,
                                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(modelColor.copy(alpha = 0.4f)))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(modelColor)
                                        )
                                        Text(
                                            text = "${selectedModel.displayName} (Streaming...)",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = modelColor
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = currentStreamingText + " ▌",
                                        fontSize = 13.sp,
                                        color = GeminiTextPrimary,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Quick Prompt Chips
            Text(
                text = "Quick Test Prompts:",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = GeminiTextMuted
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                samplePrompts.take(2).forEach { prompt ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = GeminiSurfaceVariant,
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(GeminiBorder)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                inputText = prompt
                                onSendMessage(prompt)
                            }
                    ) {
                        Text(
                            text = prompt,
                            fontSize = 10.sp,
                            maxLines = 1,
                            color = GeminiTextSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Input Bar & Voice Mic Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Mic Button with Pulse Wave Animation
                FilledIconButton(
                    onClick = onVoiceToggle,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isRecordingVoice) GeminiRed else modelColor.copy(alpha = 0.2f),
                        contentColor = if (isRecordingVoice) Color.White else modelColor
                    ),
                    modifier = Modifier
                        .size(46.dp)
                        .testTag("voice_mic_btn")
                ) {
                    Icon(
                        imageVector = if (isRecordingVoice) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Voice Input",
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Text Input Field
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("sandbox_text_input"),
                    placeholder = { Text("Send prompt or audio packet...", color = GeminiTextMuted, fontSize = 12.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (inputText.isNotBlank()) {
                                onSendMessage(inputText)
                                inputText = ""
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = modelColor,
                        unfocusedBorderColor = GeminiBorder,
                        focusedTextColor = GeminiTextPrimary,
                        unfocusedTextColor = GeminiTextPrimary,
                        cursorColor = modelColor
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                // Send Button
                FilledIconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            onSendMessage(inputText)
                            inputText = ""
                        }
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = modelColor,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier
                        .size(46.dp)
                        .testTag("send_prompt_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AudioVisualizerWaveform(
    amplitude: Float,
    isActive: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val barCount = 24
    val infiniteTransition = rememberInfiniteTransition(label = "audio_bars")

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, GeminiBorder, RoundedCornerShape(10.dp)),
        color = GeminiCodeBg
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            for (i in 0 until barCount) {
                val phaseOffset = (i * 0.25f)
                val animatedScale by infiniteTransition.animateFloat(
                    initialValue = 0.15f,
                    targetValue = 0.95f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 350 + (i % 5) * 50, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "bar_$i"
                )

                val effectiveHeight = if (isActive) {
                    val base = (amplitude * animatedScale).coerceIn(0.1f, 1.0f)
                    (base * 32).dp
                } else {
                    4.dp
                }

                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(effectiveHeight)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (isActive) accentColor else GeminiTextMuted.copy(alpha = 0.3f))
                )
            }
        }
    }
}

@Composable
fun SandboxMessageItem(
    msg: SandboxMessage,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    when (msg.sender) {
        MessageSender.USER -> {
            Row(
                modifier = modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp).copy(bottomEnd = androidx.compose.foundation.shape.CornerSize(2.dp)),
                    color = GeminiSurfaceVariant,
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(GeminiCyan.copy(alpha = 0.3f))),
                    modifier = Modifier.widthIn(max = 280.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "USER INPUT",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = GeminiCyan
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = msg.text,
                            fontSize = 13.sp,
                            color = GeminiTextPrimary
                        )
                    }
                }
            }
        }

        MessageSender.MODEL -> {
            var showThoughts by remember { mutableStateOf(false) }

            Row(
                modifier = modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp).copy(bottomStart = androidx.compose.foundation.shape.CornerSize(2.dp)),
                    color = GeminiSurfaceVariant.copy(alpha = 0.8f),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(accentColor.copy(alpha = 0.35f))),
                    modifier = Modifier.fillMaxWidth(0.92f)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "MODEL RESPONSE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                            if (msg.latencyMs != null) {
                                Text(
                                    text = "${msg.latencyMs}ms",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = GeminiTextMuted
                                )
                            }
                        }

                        // Collapsible thoughts accordion (if model provided thoughts)
                        if (msg.thoughts != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = GeminiDarkBg,
                                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(GeminiViolet.copy(alpha = 0.3f))),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showThoughts = !showThoughts }
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Psychology,
                                                contentDescription = null,
                                                tint = GeminiViolet,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = "Extended Thinking Steps",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = GeminiViolet
                                            )
                                        }
                                        Icon(
                                            imageVector = if (showThoughts) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            tint = GeminiViolet,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    AnimatedVisibility(visible = showThoughts) {
                                        Text(
                                            text = msg.thoughts,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = GeminiTextSecondary,
                                            lineHeight = 15.sp,
                                            modifier = Modifier.padding(top = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = msg.text,
                            fontSize = 13.sp,
                            color = GeminiTextPrimary,
                            lineHeight = 18.sp
                        )

                        if (msg.audioDurationSeconds != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    tint = GeminiEmerald,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Audio stream synthesized (${String.format("%.1f", msg.audioDurationSeconds)}s)",
                                    fontSize = 10.sp,
                                    color = GeminiEmerald
                                )
                            }
                        }
                    }
                }
            }
        }

        MessageSender.SYSTEM -> {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = GeminiSurfaceVariant.copy(alpha = 0.4f),
                modifier = modifier.fillMaxWidth()
            ) {
                Text(
                    text = msg.text,
                    fontSize = 11.sp,
                    color = GeminiTextMuted,
                    modifier = Modifier.padding(8.dp),
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
