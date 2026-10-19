package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import com.example.model.GeminiLiveModel
import com.example.ui.theme.*

@Composable
fun ModelSelectorDropdown(
    selectedModel: GeminiLiveModel,
    onModelSelected: (GeminiLiveModel) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val modelAccentColor = when (selectedModel) {
        GeminiLiveModel.GEMINI_3_5_TRANSLATE -> GeminiCyan
        GeminiLiveModel.GEMINI_3_5_TRANSCRIBE -> GeminiEmerald
        GeminiLiveModel.GEMINI_3_8_LIVE -> GeminiCyan
        GeminiLiveModel.GEMINI_3_8_EXTENDED_THINKING -> GeminiViolet
    }

    val modelIcon = when (selectedModel) {
        GeminiLiveModel.GEMINI_3_5_TRANSLATE -> Icons.Default.Translate
        GeminiLiveModel.GEMINI_3_5_TRANSCRIBE -> Icons.Default.Mic
        GeminiLiveModel.GEMINI_3_8_LIVE -> Icons.Default.Sensors
        GeminiLiveModel.GEMINI_3_8_EXTENDED_THINKING -> Icons.Default.Psychology
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("model_selector_dropdown"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = GeminiSurface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(listOf(modelAccentColor.copy(alpha = 0.5f), GeminiBorder))
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TARGET LIVE MODEL",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = GeminiTextMuted,
                    letterSpacing = 1.sp
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = modelAccentColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = selectedModel.tag,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = modelAccentColor
                    )
                }
            }

            // Clickable Dropdown Trigger Box
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { expanded = !expanded }
                    .border(1.dp, GeminiBorder, RoundedCornerShape(12.dp))
                    .testTag("model_dropdown_trigger"),
                color = GeminiSurfaceVariant
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(modelAccentColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = modelIcon,
                                contentDescription = null,
                                tint = modelAccentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = selectedModel.displayName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = GeminiTextPrimary
                            )
                            Text(
                                text = "ID: ${selectedModel.modelId}",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = GeminiTextMuted
                            )
                        }
                    }

                    Icon(
                        imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                        contentDescription = "Expand",
                        tint = GeminiCyan,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Description of active model
            Text(
                text = selectedModel.description,
                fontSize = 12.sp,
                color = GeminiTextSecondary,
                lineHeight = 16.sp
            )

            // Dropdown List Items
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(GeminiDarkBg)
                        .border(1.dp, GeminiBorder, RoundedCornerShape(12.dp))
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    GeminiLiveModel.entries.forEach { model ->
                        val isSelected = model == selectedModel
                        val itemColor = when (model) {
                            GeminiLiveModel.GEMINI_3_5_TRANSLATE -> GeminiCyan
                            GeminiLiveModel.GEMINI_3_5_TRANSCRIBE -> GeminiEmerald
                            GeminiLiveModel.GEMINI_3_8_LIVE -> GeminiCyan
                            GeminiLiveModel.GEMINI_3_8_EXTENDED_THINKING -> GeminiViolet
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onModelSelected(model)
                                    expanded = false
                                }
                                .testTag("select_model_${model.modelId}"),
                            color = if (isSelected) itemColor.copy(alpha = 0.15f) else Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) itemColor else GeminiTextMuted)
                                    )
                                    Column {
                                        Text(
                                            text = model.displayName,
                                            fontSize = 14.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) GeminiTextPrimary else GeminiTextSecondary
                                        )
                                        Text(
                                            text = model.tag,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = itemColor
                                        )
                                    }
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = itemColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
