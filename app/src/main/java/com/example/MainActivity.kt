package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.ui.GeminiLiveControlScreen
import com.example.ui.theme.GeminiDarkBg
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.GeminiLiveViewModel

class MainActivity : ComponentActivity() {

    private val liveViewModel: GeminiLiveViewModel by viewModels()
    private var hasOverlayPermissionState by mutableStateOf(false)

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        checkOverlayPermission()
        if (hasOverlayPermissionState) {
            Toast.makeText(this, "Overlay permission granted! Activating Background Assistant...", Toast.LENGTH_SHORT).show()
            liveViewModel.startBackgroundOverlay(this)
        } else {
            Toast.makeText(this, "Overlay permission required to display floating subtitles.", Toast.LENGTH_LONG).show()
        }
    }

    private val audioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            checkAndStartOverlay()
        } else {
            Toast.makeText(this, "Microphone permission required for continuous hands-free listening.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkOverlayPermission()
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = GeminiDarkBg
                ) {
                    GeminiLiveControlScreen(
                        viewModel = liveViewModel,
                        hasOverlayPermission = hasOverlayPermissionState,
                        onActivateOverlayClicked = { handleActivateRequest() }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkOverlayPermission()
        liveViewModel.refreshOverlayStatus()
    }

    private fun checkOverlayPermission() {
        hasOverlayPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    private fun handleActivateRequest() {
        // First check microphone permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        checkAndStartOverlay()
    }

    private fun checkAndStartOverlay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayPermissionLauncher.launch(intent)
        } else {
            liveViewModel.startBackgroundOverlay(this)
            Toast.makeText(this, "Gemini Live Background Assistant & Floating Subtitles Activated!", Toast.LENGTH_SHORT).show()
        }
    }
}
