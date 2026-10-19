package com.example.service

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.*
import android.widget.*
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.api.GeminiLiveService
import com.example.api.LiveStreamEvent
import com.example.data.PayloadBuilder
import com.example.model.FeatureKey
import com.example.model.GeminiLiveModel
import com.example.model.ReasoningLevel
import com.example.util.SecurePrefs
import kotlinx.coroutines.*
import java.util.Locale

class GeminiLiveOverlayService : Service(), TextToSpeech.OnInitListener {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var params: WindowManager.LayoutParams? = null

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var activeStreamJob: Job? = null

    private var activeModel: GeminiLiveModel = GeminiLiveModel.GEMINI_3_8_LIVE
    private var isListening = false
    private var isMinimized = false

    // Views inside floating HUD
    private var layoutExpanded: LinearLayout? = null
    private var layoutMinimized: FrameLayout? = null
    private var tvModelTitle: TextView? = null
    private var tvUserSubtitle: TextView? = null
    private var tvModelSubtitle: TextView? = null
    private var tvStatusText: TextView? = null
    private var audioBars: List<View> = emptyList()
    private var btnMicToggle: ImageView? = null

    companion object {
        const val CHANNEL_ID = "gemini_live_overlay_channel"
        const val NOTIFICATION_ID = 2026
        const val EXTRA_MODEL_ID = "EXTRA_MODEL_ID"
        const val ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE"

        var isRunning = false
            private set
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Initializing Gemini Live Assistant..."))

        textToSpeech = TextToSpeech(this, this)
        initSpeechRecognizer()
        initOverlayWindow()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            stopSelf()
            return START_NOT_STICKY
        }

        val modelId = intent?.getStringExtra(EXTRA_MODEL_ID) ?: GeminiLiveModel.GEMINI_3_8_LIVE.modelId
        activeModel = GeminiLiveModel.fromModelId(modelId)

        tvModelTitle?.text = activeModel.displayName.uppercase()
        updateNotification("Active: ${activeModel.displayName}")

        // Start continuous hands-free listening
        startListening()

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Gemini Live Overlay Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps Gemini Live Assistant & Floating Subtitles active in background"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(statusText: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, GeminiLiveOverlayService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Gemini Live Assistant")
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(statusText: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(statusText))
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initOverlayWindow() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 24
            y = 140
        }

        overlayView = createProgrammaticOverlayView()

        try {
            windowManager?.addView(overlayView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    private fun createProgrammaticOverlayView(): View {
        val root = FrameLayout(this)

        // 1. Minimized Bubble View
        layoutMinimized = FrameLayout(this).apply {
            val bg = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(AndroidColor.parseColor("#111726"))
                setStroke(3, AndroidColor.parseColor("#00E5FF"))
            }
            background = bg
            val sizePx = (48 * resources.displayMetrics.density).toInt()
            layoutParams = FrameLayout.LayoutParams(sizePx, sizePx)
            visibility = View.GONE

            val icon = ImageView(context).apply {
                setImageResource(android.R.drawable.ic_btn_speak_now)
                setColorFilter(AndroidColor.parseColor("#00E5FF"))
                val iconSize = (24 * resources.displayMetrics.density).toInt()
                layoutParams = FrameLayout.LayoutParams(iconSize, iconSize).apply {
                    gravity = Gravity.CENTER
                }
            }
            addView(icon)

            setOnClickListener {
                isMinimized = false
                layoutMinimized?.visibility = View.GONE
                layoutExpanded?.visibility = View.VISIBLE
            }
        }

        // 2. Expanded Floating HUD Pill View
        layoutExpanded = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val density = resources.displayMetrics.density
            val widthPx = (resources.displayMetrics.widthPixels * 0.92f).toInt().coerceAtMost((420 * density).toInt())
            layoutParams = FrameLayout.LayoutParams(widthPx, FrameLayout.LayoutParams.WRAP_CONTENT)

            val bg = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = 16 * density
                setColor(AndroidColor.parseColor("#E60D121E")) // Semi-transparent dark obsidian
                setStroke(2, AndroidColor.parseColor("#00E5FF"))
            }
            background = bg
            setPadding((12 * density).toInt(), (10 * density).toInt(), (12 * density).toInt(), (10 * density).toInt())
        }

        val density = resources.displayMetrics.density

        // --- Header Row (Drag Bar + Title + Controls) ---
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        // Status indicator dot
        val statusDot = View(this).apply {
            val dotBg = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(AndroidColor.parseColor("#00F5A0"))
            }
            background = dotBg
            val dotSize = (8 * density).toInt()
            layoutParams = LinearLayout.LayoutParams(dotSize, dotSize).apply {
                rightMargin = (8 * density).toInt()
            }
        }
        headerRow.addView(statusDot)

        // Title TextView
        tvModelTitle = TextView(this).apply {
            text = activeModel.displayName.uppercase()
            setTextColor(AndroidColor.parseColor("#00E5FF"))
            textSize = 11f
            paint.isFakeBoldText = true
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        headerRow.addView(tvModelTitle)

        // Mic Status button
        btnMicToggle = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_btn_speak_now)
            setColorFilter(AndroidColor.parseColor("#00F5A0"))
            val btnSize = (24 * density).toInt()
            layoutParams = LinearLayout.LayoutParams(btnSize, btnSize).apply {
                rightMargin = (8 * density).toInt()
            }
            setOnClickListener {
                if (isListening) stopListening() else startListening()
            }
        }
        headerRow.addView(btnMicToggle)

        // Minimize button
        val btnMinimize = TextView(this).apply {
            text = "─"
            setTextColor(AndroidColor.parseColor("#94A3B8"))
            textSize = 14f
            paint.isFakeBoldText = true
            setPadding((6 * density).toInt(), 0, (8 * density).toInt(), 0)
            setOnClickListener {
                isMinimized = true
                layoutExpanded?.visibility = View.GONE
                layoutMinimized?.visibility = View.VISIBLE
            }
        }
        headerRow.addView(btnMinimize)

        // Close button
        val btnClose = TextView(this).apply {
            text = "✕"
            setTextColor(AndroidColor.parseColor("#FF4D6D"))
            textSize = 13f
            paint.isFakeBoldText = true
            setPadding((6 * density).toInt(), 0, (4 * density).toInt(), 0)
            setOnClickListener {
                stopSelf()
            }
        }
        headerRow.addView(btnClose)

        layoutExpanded?.addView(headerRow)

        // --- Audio Visualizer Equalizer Row ---
        val audioEqualizerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (14 * density).toInt()).apply {
                topMargin = (6 * density).toInt()
                bottomMargin = (6 * density).toInt()
            }
        }
        val barsList = mutableListOf<View>()
        for (i in 0 until 18) {
            val bar = View(this).apply {
                val barBg = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = 2 * density
                    setColor(AndroidColor.parseColor("#00E5FF"))
                }
                background = barBg
                val barWidth = (3 * density).toInt()
                layoutParams = LinearLayout.LayoutParams(barWidth, (3 * density).toInt()).apply {
                    leftMargin = (2 * density).toInt()
                    rightMargin = (2 * density).toInt()
                }
            }
            barsList.add(bar)
            audioEqualizerRow.addView(bar)
        }
        audioBars = barsList
        layoutExpanded?.addView(audioEqualizerRow)

        // --- Status Subtitle Bar ---
        tvStatusText = TextView(this).apply {
            text = "🎙️ Listening continuously (Speak anytime)..."
            setTextColor(AndroidColor.parseColor("#64748B"))
            textSize = 10f
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        layoutExpanded?.addView(tvStatusText)

        // --- Dual Subtitles Container ---
        val subtitleContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val subBg = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = 8 * density
                setColor(AndroidColor.parseColor("#151D2C"))
                setStroke(1, AndroidColor.parseColor("#22304E"))
            }
            background = subBg
            setPadding((8 * density).toInt(), (6 * density).toInt(), (8 * density).toInt(), (6 * density).toInt())
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = (6 * density).toInt()
            }
        }

        // 1. User Subtitle Line
        tvUserSubtitle = TextView(this).apply {
            text = "👤 User: (Say something...)"
            setTextColor(AndroidColor.parseColor("#F8FAFC"))
            textSize = 12f
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        subtitleContainer.addView(tvUserSubtitle)

        // 2. Model Subtitle Line (Dual subtitle)
        tvModelSubtitle = TextView(this).apply {
            text = "🤖 Live: Waiting for speech..."
            setTextColor(AndroidColor.parseColor("#00E5FF"))
            textSize = 12f
            paint.isFakeBoldText = true
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = (4 * density).toInt()
            }
        }
        subtitleContainer.addView(tvModelSubtitle)

        layoutExpanded?.addView(subtitleContainer)

        // Drag-and-drop listener on the root overlay view
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        root.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params?.x ?: 0
                    initialY = params?.y ?: 0
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params?.x = initialX + (event.rawX - initialTouchX).toInt()
                    params?.y = initialY + (event.rawY - initialTouchY).toInt()
                    params?.let { windowManager?.updateViewLayout(root, it) }
                    true
                }
                else -> false
            }
        }

        root.addView(layoutExpanded)
        root.addView(layoutMinimized)

        return root
    }

    private fun initSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) return

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    tvStatusText?.text = "🎙️ Listening... Speak naturally"
                    btnMicToggle?.setColorFilter(AndroidColor.parseColor("#00F5A0"))
                }

                override fun onBeginningOfSpeech() {
                    tvStatusText?.text = "⚡ Voice input detected..."
                }

                override fun onRmsChanged(rmsdB: Float) {
                    val normalized = (rmsdB / 10f).coerceIn(0.1f, 1.0f)
                    updateAudioBars(normalized)
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    tvStatusText?.text = "⏳ Processing with ${activeModel.displayName}..."
                    updateAudioBars(0.1f)
                }

                override fun onError(error: Int) {
                    // Automatically restart listening on silence / timeout
                    if (isRunning && isListening) {
                        serviceScope.launch {
                            delay(500)
                            startListening()
                        }
                    }
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val spokenText = matches?.firstOrNull() ?: ""
                    if (spokenText.isNotBlank()) {
                        handleSpokenInput(spokenText)
                    } else if (isRunning && isListening) {
                        startListening()
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    if (!partial.isNullOrBlank()) {
                        tvUserSubtitle?.text = "👤 User: \"$partial\""
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }
    }

    private fun updateAudioBars(amplitude: Float) {
        val density = resources.displayMetrics.density
        audioBars.forEachIndexed { i, bar ->
            val scale = (amplitude * (0.5f + (i % 4) * 0.15f)).coerceIn(0.15f, 1.0f)
            val heightPx = ((scale * 14) * density).toInt().coerceAtLeast((3 * density).toInt())
            val lp = bar.layoutParams
            lp.height = heightPx
            bar.layoutParams = lp
        }
    }

    private fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            tvStatusText?.text = "⚠️ Speech Recognition unavailable on device"
            return
        }
        isListening = true
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopListening() {
        isListening = false
        btnMicToggle?.setColorFilter(AndroidColor.parseColor("#94A3B8"))
        tvStatusText?.text = "⏸️ Mic paused (Tap mic to resume)"
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {}
    }

    private fun handleSpokenInput(userSpeech: String) {
        val timeNow = java.text.SimpleDateFormat("HH:mm", Locale.getDefault()).format(java.util.Date())
        tvUserSubtitle?.text = "👤 User ($timeNow): \"$userSpeech\""
        tvModelSubtitle?.text = "🤖 ${activeModel.tag}: Translating & Thinking..."

        activeStreamJob?.cancel()
        activeStreamJob = serviceScope.launch {
            val key = SecurePrefs.getGeminiApiKey(this@GeminiLiveOverlayService)

            // Default model-specific toggles
            val toggles = mapOf(
                FeatureKey.BIDIRECTIONAL_STREAMING to true,
                FeatureKey.AUTO_LANGUAGE_DETECTION to true,
                FeatureKey.VOICE_INTONATION_PRESERVATION to true,
                FeatureKey.REALTIME_AUDIO_OUTPUT to true,
                FeatureKey.LIVE_SPEECH_TO_TEXT to true,
                FeatureKey.STREAMING_TEXT_DISPLAY to true,
                FeatureKey.NOISE_FILTERING to true,
                FeatureKey.AUTO_PUNCTUATION to true,
                FeatureKey.REALTIME_VOICE_CHAT to true,
                FeatureKey.LIVE_VIDEO_SCREEN_FEED to false,
                FeatureKey.AFFECTIVE_DIALOGUE to true,
                FeatureKey.ASYNC_TOOL_CALLING to true,
                FeatureKey.BACKGROUND_MULTI_STEP_SOLVING to true,
                FeatureKey.EARLY_VERBAL_CUES to true,
                FeatureKey.ALPHANUMERIC_PRECISION to true
            )

            var accumulatedText = ""

            GeminiLiveService.startLiveSession(
                model = activeModel,
                toggles = toggles,
                reasoningLevel = ReasoningLevel.MEDIUM,
                prompt = userSpeech,
                customApiKey = key
            ).collect { event ->
                when (event) {
                    is LiveStreamEvent.ContentChunk -> {
                        accumulatedText = event.textChunk
                        tvModelSubtitle?.text = "🤖 ${activeModel.tag}: $accumulatedText"
                    }
                    is LiveStreamEvent.Complete -> {
                        val finalText = event.finalMessage.text
                        tvModelSubtitle?.text = "🤖 ${activeModel.tag}: $finalText"

                        // Speak out the response if TTS ready
                        if (isTtsReady && finalText.isNotBlank()) {
                            speakOut(finalText)
                        } else {
                            // Resume listening
                            delay(1000)
                            if (isRunning && isListening) startListening()
                        }
                    }
                    is LiveStreamEvent.Failure -> {
                        tvModelSubtitle?.text = "⚠️ Error: ${event.errorMsg}"
                        delay(2000)
                        if (isRunning && isListening) startListening()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun speakOut(text: String) {
        val cleanSpeech = text.replace(Regex("\\[.*?\\]"), "").trim()
        if (activeModel == GeminiLiveModel.GEMINI_3_5_TRANSLATE) {
            textToSpeech?.language = Locale("es", "ES")
        } else {
            textToSpeech?.language = Locale.getDefault()
        }

        val params = Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "live_speech_utterance")
        textToSpeech?.speak(cleanSpeech, TextToSpeech.QUEUE_FLUSH, params, "live_speech_utterance")

        textToSpeech?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                tvStatusText?.post { tvStatusText?.text = "🔊 Speaking live response..." }
            }

            override fun onDone(utteranceId: String?) {
                tvStatusText?.post {
                    tvStatusText?.text = "🎙️ Listening continuously..."
                    if (isRunning && isListening) startListening()
                }
            }

            override fun onError(utteranceId: String?) {
                tvStatusText?.post {
                    if (isRunning && isListening) startListening()
                }
            }
        })
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsReady = true
            textToSpeech?.language = Locale.getDefault()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        activeStreamJob?.cancel()
        serviceScope.cancel()

        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) {}

        try {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        } catch (_: Exception) {}

        if (overlayView != null) {
            try {
                windowManager?.removeView(overlayView)
            } catch (_: Exception) {}
        }
    }
}
