package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.GeminiLiveModel
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.GeminiLiveViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiLiveControlScreen(
    viewModel: GeminiLiveViewModel,
    hasOverlayPermission: Boolean,
    onActivateOverlayClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedModel by viewModel.selectedModel.collectAsStateWithLifecycle()
    val featureToggles by viewModel.featureToggles.collectAsStateWithLifecycle()
    val reasoningLevel by viewModel.reasoningLevel.collectAsStateWithLifecycle()
    val apiKey by viewModel.apiKey.collectAsStateWithLifecycle()
    val sessionStatus by viewModel.sessionStatus.collectAsStateWithLifecycle()
    val sandboxMessages by viewModel.sandboxMessages.collectAsStateWithLifecycle()
    val currentStreamingText by viewModel.currentStreamingText.collectAsStateWithLifecycle()
    val currentThoughtText by viewModel.currentThoughtText.collectAsStateWithLifecycle()
    val audioAmplitude by viewModel.audioAmplitude.collectAsStateWithLifecycle()
    val isRecordingVoice by viewModel.isRecordingVoice.collectAsStateWithLifecycle()
    val payloadJson by viewModel.payloadJson.collectAsStateWithLifecycle()
    val showPayloadInspector by viewModel.showPayloadInspector.collectAsStateWithLifecycle()
    val isOverlayRunning by viewModel.isOverlayRunning.collectAsStateWithLifecycle()

    // Live Voice Session state
    val showLiveVoiceModal by viewModel.showLiveVoiceModal.collectAsStateWithLifecycle()
    val isScreenShareActive by viewModel.isScreenShareActive.collectAsStateWithLifecycle()
    val liveUserSubtitle by viewModel.liveUserSubtitle.collectAsStateWithLifecycle()
    val liveModelSubtitle by viewModel.liveModelSubtitle.collectAsStateWithLifecycle()
    val isThinking by viewModel.isThinking.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.isSpeaking.collectAsStateWithLifecycle()

    val hasApiKey = viewModel.getEffectiveApiKey().isNotBlank()

    val modelAccentColor = when (selectedModel) {
        GeminiLiveModel.GEMINI_3_5_TRANSLATE -> GeminiCyan
        GeminiLiveModel.GEMINI_3_5_TRANSCRIBE -> GeminiEmerald
        GeminiLiveModel.GEMINI_3_8_LIVE -> GeminiCyan
        GeminiLiveModel.GEMINI_3_8_EXTENDED_THINKING -> GeminiViolet
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(GeminiDarkBg),
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("mission_control_top_bar"),
                color = GeminiSurface,
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(listOf(GeminiBorder, modelAccentColor.copy(alpha = 0.5f)))
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(modelAccentColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sensors,
                                contentDescription = null,
                                tint = modelAccentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "GEMINI LIVE",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = GeminiTextPrimary,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "CONTROL CENTER",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = modelAccentColor,
                                    letterSpacing = 1.sp
                                )
                            }
                            Text(
                                text = "Mission Control & Live Playground",
                                fontSize = 11.sp,
                                color = GeminiTextSecondary
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Quick Live Voice Mode Trigger Button
                        FilledTonalButton(
                            onClick = { viewModel.openLiveVoiceModal() },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = modelAccentColor.copy(alpha = 0.2f),
                                contentColor = modelAccentColor
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("top_quick_live_voice_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = "Live Voice",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "TALK LIVE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        // Payload Quick Trigger Icon
                        IconButton(
                            onClick = { viewModel.togglePayloadInspector() },
                            modifier = Modifier.testTag("top_payload_btn")
                        ) {
                            Badge(
                                containerColor = modelAccentColor,
                                contentColor = Color.Black
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DataObject,
                                    contentDescription = "View Payload",
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = GeminiDarkBg
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // 1. API Configuration Section
            item {
                ApiConfigCard(
                    apiKey = apiKey,
                    onSaveKey = { viewModel.saveApiKey(it) },
                    onClearKey = { viewModel.clearApiKey() }
                )
            }

            // 2. Master Live Voice & Background Floating HUD Card
            item {
                ActivationCard(
                    isServiceRunning = isOverlayRunning,
                    selectedModel = selectedModel,
                    hasOverlayPermission = hasOverlayPermission,
                    onActivateClicked = onActivateOverlayClicked,
                    onStopClicked = { viewModel.stopBackgroundOverlay(context) },
                    onLiveVoiceClicked = { viewModel.openLiveVoiceModal() }
                )
            }

            // 3. Model Selection Dropdown
            item {
                ModelSelectorDropdown(
                    selectedModel = selectedModel,
                    onModelSelected = { viewModel.selectModel(it) }
                )
            }

            // 4. Dynamic Feature Control Panel (Toggles & Permissions)
            item {
                DynamicControlPanel(
                    selectedModel = selectedModel,
                    featureToggles = featureToggles,
                    reasoningLevel = reasoningLevel,
                    onToggleChanged = { key, enabled -> viewModel.setToggle(key, enabled) },
                    onReasoningLevelChanged = { viewModel.setReasoningLevel(it) },
                    onInspectPayloadClicked = { viewModel.setShowPayloadInspector(true) }
                )
            }

            // 5. Interactive Sandbox / Workspace Area
            item {
                InteractiveSandbox(
                    selectedModel = selectedModel,
                    messages = sandboxMessages,
                    currentStreamingText = currentStreamingText,
                    currentThoughtText = currentThoughtText,
                    audioAmplitude = audioAmplitude,
                    isRecordingVoice = isRecordingVoice,
                    sessionStatus = sessionStatus,
                    hasApiKey = hasApiKey,
                    onSendMessage = { viewModel.sendPrompt(it) },
                    onVoiceToggle = { viewModel.openLiveVoiceModal() },
                    onClearMessages = { viewModel.clearSandbox() }
                )
            }
        }
    }

    // Modal Payload Inspector
    if (showPayloadInspector) {
        PayloadInspectorModal(
            payloadJson = payloadJson,
            selectedModel = selectedModel,
            onDismiss = { viewModel.setShowPayloadInspector(false) }
        )
    }

    // Full-Screen ChatGPT / Gemini Live Voice Session Modal
    if (showLiveVoiceModal) {
        LiveVoiceModal(
            selectedModel = selectedModel,
            isListening = isRecordingVoice,
            isThinking = isThinking,
            isSpeaking = isSpeaking,
            audioAmplitude = audioAmplitude,
            userSubtitle = liveUserSubtitle,
            modelSubtitle = liveModelSubtitle,
            onMicToggle = { viewModel.toggleLiveVoiceMic() },
            onScreenShareToggle = { viewModel.toggleScreenShare() },
            isScreenShareActive = isScreenShareActive,
            onDismiss = { viewModel.closeLiveVoiceModal() }
        )
    }
}
