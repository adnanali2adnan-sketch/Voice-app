package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.ui.theme.*

@Composable
fun ApiConfigCard(
    apiKey: String,
    onSaveKey: (String) -> Unit,
    onClearKey: () -> Unit,
    modifier: Modifier = Modifier
) {
    var inputKey by remember(apiKey) { mutableStateOf(apiKey) }
    var passwordVisible by remember { mutableStateOf(false) }
    var isExpanded by remember { mutableStateOf(apiKey.isBlank()) }

    val hasCustomKey = apiKey.isNotBlank()
    val hasEnvKey = BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY"

    val statusText = when {
        hasCustomKey -> "CUSTOM KEY SAVED"
        hasEnvKey -> "APP ENVIRONMENT KEY ACTIVE"
        else -> "API KEY REQUIRED"
    }

    val statusColor = when {
        hasCustomKey -> GeminiEmerald
        hasEnvKey -> GeminiCyan
        else -> GeminiAmber
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("api_config_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = GeminiSurface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(listOf(GeminiBorder, GeminiBorder.copy(alpha = 0.5f)))
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header row with status badge
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
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(GeminiSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = "API Key",
                            tint = GeminiCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "API CREDENTIALS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = GeminiTextMuted,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Google AI Studio Key",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GeminiTextPrimary
                        )
                    }
                }

                // Status Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusColor.copy(alpha = 0.15f),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.linearGradient(listOf(statusColor.copy(alpha = 0.5f), statusColor))
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Text(
                            text = statusText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            // Key Input & Buttons
            OutlinedTextField(
                value = inputKey,
                onValueChange = { inputKey = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("api_key_input"),
                label = { Text("AI Studio API Key (AIzaSy...)") },
                placeholder = { Text("Paste your API key here", color = GeminiTextMuted) },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { onSaveKey(inputKey) }),
                trailingIcon = {
                    IconButton(
                        onClick = { passwordVisible = !passwordVisible },
                        modifier = Modifier.testTag("toggle_key_visibility")
                    ) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "Hide key" else "Show key",
                            tint = GeminiTextMuted
                        )
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GeminiCyan,
                    unfocusedBorderColor = GeminiBorder,
                    focusedLabelColor = GeminiCyan,
                    unfocusedLabelColor = GeminiTextMuted,
                    focusedTextColor = GeminiTextPrimary,
                    unfocusedTextColor = GeminiTextPrimary,
                    cursorColor = GeminiCyan
                ),
                shape = RoundedCornerShape(12.dp)
            )

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { onSaveKey(inputKey) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("save_api_key_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = GeminiCyan),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Save",
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "SAVE CREDENTIAL",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                if (hasCustomKey) {
                    OutlinedButton(
                        onClick = {
                            inputKey = ""
                            onClearKey()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GeminiRed),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = Brush.linearGradient(listOf(GeminiRed.copy(alpha = 0.5f), GeminiRed))
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("clear_api_key_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear",
                            tint = GeminiRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("CLEAR", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Guidance Note
            if (!hasCustomKey && !hasEnvKey) {
                Text(
                    text = "💡 Enter your key from Google AI Studio. Stored locally on device.",
                    fontSize = 11.sp,
                    color = GeminiTextMuted
                )
            }
        }
    }
}
