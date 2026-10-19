package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.PayloadBuilder
import com.example.model.FeatureKey
import com.example.model.GeminiLiveModel
import com.example.model.ReasoningLevel
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Gemini Live Control", appName)
    }

    @Test
    fun `payload builder omits video modality when video toggle is off`() {
        val togglesWithoutVideo = mapOf(
            FeatureKey.REALTIME_VOICE_CHAT to true,
            FeatureKey.LIVE_VIDEO_SCREEN_FEED to false,
            FeatureKey.AFFECTIVE_DIALOGUE to true,
            FeatureKey.ASYNC_TOOL_CALLING to true
        )

        val payload = PayloadBuilder.buildPayloadJson(
            model = GeminiLiveModel.GEMINI_3_8_LIVE,
            toggles = togglesWithoutVideo,
            reasoningLevel = ReasoningLevel.MEDIUM
        )

        assertFalse("Payload should not contain VIDEO_STREAM when video is off", payload.contains("VIDEO_STREAM"))
        assertTrue("Payload should contain voice/text modalities", payload.contains("AUDIO"))
    }

    @Test
    fun `payload builder configures extended thinking budget correctly`() {
        val toggles = mapOf(
            FeatureKey.BACKGROUND_MULTI_STEP_SOLVING to true,
            FeatureKey.EARLY_VERBAL_CUES to true,
            FeatureKey.ALPHANUMERIC_PRECISION to true
        )

        val payloadHigh = PayloadBuilder.buildPayloadJson(
            model = GeminiLiveModel.GEMINI_3_8_EXTENDED_THINKING,
            toggles = toggles,
            reasoningLevel = ReasoningLevel.HIGH
        )

        assertTrue(payloadHigh.contains("\"thinkingBudget\": 16384"))
        assertTrue(payloadHigh.contains("\"earlyVerbalCues\": true"))
    }
}
