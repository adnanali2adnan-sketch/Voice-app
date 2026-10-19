package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GeminiLiveModel
import com.example.ui.theme.*

@Composable
fun ActivationCard(
    isServiceRunning: Boolean,
    selectedModel: GeminiLiveModel,
    hasOverlayPermission: Boolean,
    onActivateClicked: () -> Unit,
    onStopClicked: () -> Unit,
    onLiveVoiceClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val modelColor = when (selectedModel) {
        GeminiLiveModel.GEMINI_3_5_TRANSLATE -> GeminiCyan
        GeminiLiveModel.GEMINI_3_5_TRANSCRIBE -> GeminiEmerald
        GeminiLiveModel.GEMINI_3_8_LIVE -> GeminiCyan
        GeminiLiveModel.GEMINI_3_8_EXTENDED_THINKING -> GeminiViolet
    }

    val statusColor = if (isServiceRunning) GeminiEmerald else GeminiAmber

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("activation_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = GeminiSurface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(
                if (isServiceRunning) listOf(GeminiEmerald, GeminiCyan) else listOf(GeminiBorder, modelColor.copy(alpha = 0.5f))
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with status indicator
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
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isServiceRunning) GeminiEmerald.copy(alpha = 0.2f) else GeminiSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isServiceRunning) Icons.Default.Sensors else Icons.Default.PowerSettingsNew,
                            contentDescription = null,
                            tint = if (isServiceRunning) GeminiEmerald else modelColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "LIVE VOICE & BACKGROUND ASSISTANT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = GeminiTextMuted,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = if (isServiceRunning) "Floating Cockpit Live" else "AI Live Modes Ready",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = GeminiTextPrimary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusColor.copy(alpha = 0.15f),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.linearGradient(listOf(statusColor.copy(alpha = 0.5f), statusColor))
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Text(
                            text = if (isServiceRunning) "HUD ACTIVE" else "READY",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }
            }

            // Description info box
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = GeminiSurfaceVariant.copy(alpha = 0.5f),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(GeminiBorder))
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = modelColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Active Engine: ${selectedModel.displayName}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = GeminiTextPrimary
                        )
                    }
                    Text(
                        text = "• Interactive Voice: Speak in any language (English, Urdu, Hindi, Spanish).\n• Background Floating HUD: Draggable pill with live dual subtitles.\n• Screen/Camera Stream: Analyze visuals live like Gemini / ChatGPT Live.",
                        fontSize = 11.sp,
                        color = GeminiTextSecondary,
                        lineHeight = 15.sp
                    )
                }
            }

            // Action Buttons Row:
            // 1. TALK LIVE NOW (ChatGPT/Gemini Style)
            Button(
                onClick = onLiveVoiceClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("open_live_voice_mode_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = modelColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = "Live Voice",
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "START LIVE VOICE CALL 🎙️ (GEMINI / CHATGPT STYLE)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Black
                )
            }

            // 2. Master Background Floating HUD Button
            if (isServiceRunning) {
                OutlinedButton(
                    onClick = onStopClicked,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("stop_background_hud_btn"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GeminiRed),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(GeminiRed)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop",
                        tint = GeminiRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "DISMISS FLOATING SUBTITLES HUD",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GeminiRed
                    )
                }
            } else {
                OutlinedButton(
                    onClick = onActivateClicked,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("activate_background_hud_btn"),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (hasOverlayPermission) GeminiCyan else GeminiAmber
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(
                            if (hasOverlayPermission) GeminiCyan.copy(alpha = 0.8f) else GeminiAmber
                        )
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureInPicture,
                        contentDescription = "Activate Overlay",
                        tint = if (hasOverlayPermission) GeminiCyan else GeminiAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (hasOverlayPermission) "ACTIVATE FLOATING SUBTITLES OVER OTHER APPS ⚡" else "GRANT OVERLAY PERMISSION & ACTIVATE HUD",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
