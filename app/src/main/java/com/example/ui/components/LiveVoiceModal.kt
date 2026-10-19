package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.GeminiLiveModel
import com.example.ui.theme.*

@Composable
fun LiveVoiceModal(
    selectedModel: GeminiLiveModel,
    isListening: Boolean,
    isThinking: Boolean,
    isSpeaking: Boolean,
    audioAmplitude: Float,
    userSubtitle: String,
    modelSubtitle: String,
    onMicToggle: () -> Unit,
    onScreenShareToggle: () -> Unit,
    isScreenShareActive: Boolean,
    onDismiss: () -> Unit
) {
    val modelColor = when (selectedModel) {
        GeminiLiveModel.GEMINI_3_5_TRANSLATE -> GeminiCyan
        GeminiLiveModel.GEMINI_3_5_TRANSCRIBE -> GeminiEmerald
        GeminiLiveModel.GEMINI_3_8_LIVE -> GeminiCyan
        GeminiLiveModel.GEMINI_3_8_EXTENDED_THINKING -> GeminiViolet
    }

    val infiniteTransition = rememberInfiniteTransition(label = "orb_pulse")

    // Breathing scale animation
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Dynamic scale driven by audio volume when speaking or listening
    val activeScale = if (isSpeaking || isListening) {
        pulseScale + (audioAmplitude * 0.35f).coerceIn(0f, 0.45f)
    } else {
        pulseScale
    }

    val stateText = when {
        isSpeaking -> "GEMINI IS SPEAKING..."
        isThinking -> "THINKING & PROCESSING PROOF..."
        isListening -> "LISTENING TO YOUR VOICE..."
        else -> "STANDBY // TAP MIC TO SPEAK"
    }

    val stateColor = when {
        isSpeaking -> GeminiEmerald
        isThinking -> GeminiViolet
        isListening -> GeminiCyan
        else -> GeminiTextMuted
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("live_voice_full_screen_modal"),
            color = GeminiDarkBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Row: Model title & Close Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(stateColor)
                            )
                            Text(
                                text = "LIVE VOICE SESSION",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GeminiTextMuted,
                                letterSpacing = 1.sp
                            )
                        }
                        Text(
                            text = selectedModel.displayName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = GeminiTextPrimary
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(GeminiSurfaceVariant)
                            .testTag("close_live_voice_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = GeminiTextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Screen Share / Camera Viewfinder HUD Simulation
                AnimatedVisibility(visible = isScreenShareActive) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, GeminiCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                        color = GeminiCodeBg
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Videocam,
                                        contentDescription = null,
                                        tint = GeminiCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "LIVE SCREEN / CAMERA STREAM ACTIVE (720p 30fps)",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = GeminiCyan,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "Gemini 3.8 is analyzing live screen contents and visual context in real time.",
                                    fontSize = 11.sp,
                                    color = GeminiTextSecondary
                                )
                            }

                            // Viewfinder grid marks
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(40.dp)
                                    .border(1.dp, GeminiCyan.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                            )
                        }
                    }
                }

                // Center Animated Glowing AI Orb (Like ChatGPT / Gemini Live)
                Box(
                    modifier = Modifier
                        .size(230.dp)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer diffuse glow ring
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .scale(activeScale * 1.15f)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(stateColor.copy(alpha = 0.35f), Color.Transparent)
                                )
                            )
                    )

                    // Secondary pulsating ripple
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .scale(activeScale * 1.05f)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(stateColor.copy(alpha = 0.5f), modelColor.copy(alpha = 0.1f))
                                )
                            )
                    )

                    // Core Fluid Sphere
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .scale(activeScale)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        stateColor,
                                        if (isThinking) GeminiAmber else modelColor,
                                        Color(0xFF001F29)
                                    )
                                )
                            )
                            .border(2.dp, Color.White.copy(alpha = 0.7f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSpeaking) Icons.Default.GraphicEq else if (isListening) Icons.Default.Mic else Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }

                // Status text indicator
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stateText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = stateColor,
                        letterSpacing = 1.sp
                    )

                    Text(
                        text = "Speak in English, Urdu, Hindi, Spanish, or any language",
                        fontSize = 11.sp,
                        color = GeminiTextMuted
                    )
                }

                // Dual Subtitles Display (User Speech + Gemini Live Response)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, GeminiBorder, RoundedCornerShape(14.dp)),
                    color = GeminiSurface
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (userSubtitle.isNotBlank()) {
                            Text(
                                text = "👤 User: $userSubtitle",
                                fontSize = 13.sp,
                                color = GeminiTextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Text(
                            text = "🤖 Live: ${modelSubtitle.ifBlank { "Listening for your voice input..." }}",
                            fontSize = 13.sp,
                            color = modelColor,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 18.sp
                        )
                    }
                }

                // Bottom Call Controls (ChatGPT / Gemini Live Bar)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Screen Share / Camera toggle
                    IconButton(
                        onClick = onScreenShareToggle,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(if (isScreenShareActive) GeminiCyan else GeminiSurfaceVariant)
                            .testTag("toggle_screen_share_live_btn")
                    ) {
                        Icon(
                            imageVector = if (isScreenShareActive) Icons.Default.ScreenShare else Icons.Default.StopScreenShare,
                            contentDescription = "Screen Share",
                            tint = if (isScreenShareActive) Color.Black else GeminiTextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Main Mic Toggle Button
                    IconButton(
                        onClick = onMicToggle,
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(if (isListening) GeminiEmerald else GeminiSurfaceVariant)
                            .border(2.dp, if (isListening) Color.White else GeminiBorder, CircleShape)
                            .testTag("main_live_mic_btn")
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                            contentDescription = "Mic Toggle",
                            tint = if (isListening) Color.Black else GeminiTextMuted,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // End Session (Red Button)
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(GeminiRed)
                            .testTag("end_live_voice_session_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "End Call",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}
