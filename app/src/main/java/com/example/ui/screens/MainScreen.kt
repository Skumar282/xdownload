package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.DownloadEntity
import com.example.data.preferences.UserPreferences
import com.example.ui.UiState
import com.example.ui.components.BadgePill
import com.example.ui.components.DetectedClipboardBanner
import com.example.ui.components.DownloadItemCard
import com.example.ui.components.LinkInputCard
import com.example.ui.components.PreviewMediaCard
import com.example.ui.components.PrivacyClipboardInfoCard
import com.example.ui.theme.AccentError
import com.example.ui.theme.AccentSuccess
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.XBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    uiState: UiState,
    preferences: UserPreferences,
    downloads: List<DownloadEntity>,
    onUrlSubmitted: (String) -> Unit,
    onConfirmPreviewDownload: () -> Unit,
    onDismissPreview: () -> Unit,
    onDownloadClipboard: (String) -> Unit,
    onDismissClipboard: () -> Unit,
    onToggleAutoDownload: (Boolean) -> Unit,
    onDeleteDownload: (Long) -> Unit,
    onRetryDownload: (Long) -> Unit,
    onClearMessages: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ObsidianBlack,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurfaceElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "𝕏",
                                color = XBlue,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Auto X Downloader",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Status messages banner
            if (!uiState.errorMessage.isNullOrBlank()) {
                item {
                    NotificationBanner(
                        message = uiState.errorMessage,
                        isError = true,
                        onDismiss = onClearMessages
                    )
                }
            }

            if (!uiState.infoBanner.isNullOrBlank()) {
                item {
                    NotificationBanner(
                        message = uiState.infoBanner,
                        isError = false,
                        onDismiss = onClearMessages
                    )
                }
            }

            // Auto-Download Toggle Banner Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Automatic Download",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = if (preferences.autoDownloadEnabled)
                                    "Starts instantly when links are shared or copied"
                                else
                                    "Shows preview card before downloading",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                        }

                        Switch(
                            checked = preferences.autoDownloadEnabled,
                            onCheckedChange = onToggleAutoDownload,
                            modifier = Modifier.testTag("auto_download_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = androidx.compose.ui.graphics.Color.White,
                                checkedTrackColor = XBlue,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = DarkSurfaceElevated
                            )
                        )
                    }
                }
            }

            // Main Link Input Card
            item {
                LinkInputCard(
                    isResolving = uiState.isResolving,
                    onDownload = onUrlSubmitted
                )
            }

            // Clipboard detection prompt if link found
            if (uiState.detectedClipboardUrl != null) {
                item {
                    DetectedClipboardBanner(
                        detectedUrl = uiState.detectedClipboardUrl,
                        onDownload = { onDownloadClipboard(uiState.detectedClipboardUrl) },
                        onDismiss = onDismissClipboard
                    )
                }
            }

            // Preview card when auto download is disabled
            if (uiState.previewMedia != null) {
                item {
                    PreviewMediaCard(
                        media = uiState.previewMedia,
                        onConfirmDownload = onConfirmPreviewDownload,
                        onDismiss = onDismissPreview
                    )
                }
            }

            // Clipboard privacy guide card
            item {
                PrivacyClipboardInfoCard()
            }

            // Recent Downloads Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Recent Downloads",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "${downloads.size} item${if (downloads.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                    )
                }
            }

            // Empty state if list is empty
            if (downloads.isEmpty()) {
                item {
                    EmptyDownloadsView()
                }
            } else {
                items(
                    items = downloads,
                    key = { it.id }
                ) { download ->
                    DownloadItemCard(
                        download = download,
                        onDelete = onDeleteDownload,
                        onRetry = onRetryDownload
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyDownloadsView() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CloudDownload,
                    contentDescription = null,
                    tint = XBlue,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "No Downloads Yet",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Share any X post to this app, or paste a status URL above to download high quality photos & videos.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            )
        }
    }
}

@Composable
private fun NotificationBanner(
    message: String,
    isError: Boolean,
    onDismiss: () -> Unit
) {
    val bgColor = if (isError) AccentError.copy(alpha = 0.15f) else AccentSuccess.copy(alpha = 0.15f)
    val textColor = if (isError) AccentError else AccentSuccess

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isError) Icons.Default.ErrorOutline else Icons.Default.Info,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium,
                    color = textColor
                ),
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = textColor,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
