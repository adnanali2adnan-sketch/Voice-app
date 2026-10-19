package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.GeminiLiveModel
import com.example.ui.theme.*

@Composable
fun PayloadInspectorModal(
    payloadJson: String,
    selectedModel: GeminiLiveModel,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val accentColor = when (selectedModel) {
        GeminiLiveModel.GEMINI_3_5_TRANSLATE -> GeminiCyan
        GeminiLiveModel.GEMINI_3_5_TRANSCRIBE -> GeminiEmerald
        GeminiLiveModel.GEMINI_3_8_LIVE -> GeminiCyan
        GeminiLiveModel.GEMINI_3_8_EXTENDED_THINKING -> GeminiViolet
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                .testTag("payload_inspector_modal"),
            color = GeminiDarkBg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Modal Top Bar
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
                            imageVector = Icons.Default.DataObject,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = "LIVE PAYLOAD INSPECTOR",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GeminiTextMuted,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = selectedModel.displayName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = GeminiTextPrimary
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(payloadJson))
                                Toast.makeText(context, "Payload JSON copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.testTag("copy_payload_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy JSON",
                                tint = accentColor
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("close_payload_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = GeminiTextMuted
                            )
                        }
                    }
                }

                // Info banner explaining the access & control logic
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = GeminiSurfaceVariant.copy(alpha = 0.6f),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(GeminiBorder))
                ) {
                    Text(
                        text = "🔒 Access Control Verified: Parameters and tool objects matching disabled toggles are automatically omitted or blocked from this live request envelope.",
                        modifier = Modifier.padding(10.dp),
                        fontSize = 11.sp,
                        color = GeminiTextSecondary,
                        lineHeight = 15.sp
                    )
                }

                // Monospace JSON Code Viewer
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, GeminiBorder, RoundedCornerShape(12.dp)),
                    color = GeminiCodeBg
                ) {
                    val verticalScroll = rememberScrollState()
                    val horizontalScroll = rememberScrollState()

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(verticalScroll)
                            .horizontalScroll(horizontalScroll)
                            .padding(14.dp)
                    ) {
                        Text(
                            text = payloadJson.ifBlank { "{\n  // Initializing payload...\n}" },
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = GeminiCyan,
                            lineHeight = 18.sp
                        )
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        "DONE",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
