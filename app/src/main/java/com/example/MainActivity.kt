package com.example

import android.Manifest
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.screens.MainScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AutoXTheme

enum class AppScreen {
    MAIN,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        val app = application as AutoXApplication
        MainViewModel.Factory(
            downloadDao = app.database.downloadDao(),
            preferencesRepository = app.preferencesRepository,
            mediaResolver = app.mediaResolver,
            downloadEngine = app.downloadEngine
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle intent if launched via Sharesheet or deep link
        handleIncomingIntent(intent)

        setContent {
            AutoXTheme(darkTheme = true) {
                AutoXApp(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkClipboardForEligibleXLinks()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return

        when (intent.action) {
            Intent.ACTION_SEND -> {
                if (intent.type?.startsWith("text/") == true) {
                    val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                    if (!sharedText.isNullOrBlank()) {
                        viewModel.onUrlSubmitted(sharedText, isFromSharesheet = true)
                    }
                }
            }
            Intent.ACTION_VIEW -> {
                val dataUri = intent.data?.toString()
                if (!dataUri.isNullOrBlank()) {
                    viewModel.onUrlSubmitted(dataUri, isFromSharesheet = false)
                }
            }
        }
    }

    private fun checkClipboardForEligibleXLinks() {
        try {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            if (clipboard != null && clipboard.hasPrimaryClip()) {
                val item = clipboard.primaryClip?.getItemAt(0)
                val text = item?.text?.toString()
                if (!text.isNullOrBlank()) {
                    viewModel.checkClipboard(text)
                }
            }
        } catch (_: SecurityException) {
            // Android 10+ background clip exception safeguard
        }
    }
}

@Composable
fun AutoXApp(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var currentScreen by remember { mutableStateOf(AppScreen.MAIN) }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()

    // Request POST_NOTIFICATIONS on Android 13+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { /* Handled gracefully */ }

        LaunchedEffect(Unit) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Crossfade(
        targetState = currentScreen,
        label = "screen_navigation",
        modifier = modifier
    ) { screen ->
        when (screen) {
            AppScreen.MAIN -> {
                MainScreen(
                    uiState = uiState,
                    preferences = preferences,
                    downloads = downloads,
                    onUrlSubmitted = { viewModel.onUrlSubmitted(it, isFromSharesheet = false) },
                    onConfirmPreviewDownload = { viewModel.startDownloadPreview() },
                    onDismissPreview = { viewModel.dismissPreview() },
                    onDownloadClipboard = { viewModel.onUrlSubmitted(it, isFromSharesheet = false) },
                    onDismissClipboard = { viewModel.dismissClipboardBanner() },
                    onToggleAutoDownload = { viewModel.setAutoDownloadEnabled(it) },
                    onDeleteDownload = { viewModel.deleteDownload(it) },
                    onRetryDownload = { viewModel.retryDownload(it) },
                    onClearMessages = { viewModel.clearMessages() },
                    onNavigateToSettings = { currentScreen = AppScreen.SETTINGS }
                )
            }
            AppScreen.SETTINGS -> {
                SettingsScreen(
                    preferences = preferences,
                    onToggleAutoDownload = { viewModel.setAutoDownloadEnabled(it) },
                    onSetPreferredQuality = { viewModel.setPreferredQuality(it) },
                    onToggleLongVideoPolicy = { viewModel.setLongVideoPolicyEnabled(it) },
                    onToggleWifiOnly = { viewModel.setWifiOnly(it) },
                    onTogglePreventDuplicates = { viewModel.setPreventDuplicates(it) },
                    onToggleNotifications = { viewModel.setNotificationsEnabled(it) },
                    onSetCustomResolver = { viewModel.setCustomResolverUrl(it) },
                    onClearHistory = { viewModel.clearAllHistory() },
                    onBack = { currentScreen = AppScreen.MAIN }
                )
            }
        }
    }
}
