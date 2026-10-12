package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.DownloadQualityPreference
import com.example.data.preferences.UserPreferences
import com.example.ui.theme.AccentError
import com.example.ui.theme.BorderMedium
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
fun SettingsScreen(
    preferences: UserPreferences,
    onToggleAutoDownload: (Boolean) -> Unit,
    onSetPreferredQuality: (DownloadQualityPreference) -> Unit,
    onToggleLongVideoPolicy: (Boolean) -> Unit,
    onToggleWifiOnly: (Boolean) -> Unit,
    onTogglePreventDuplicates: (Boolean) -> Unit,
    onToggleNotifications: (Boolean) -> Unit,
    onSetCustomResolver: (String) -> Unit,
    onClearHistory: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var customResolverInput by remember(preferences.customResolverUrl) {
        mutableStateOf(preferences.customResolverUrl)
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Clear Download History?") },
            text = { Text("This will delete all download records from the database. Files already saved in your device storage will remain untouched.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearHistory()
                        showClearHistoryDialog = false
                    }
                ) {
                    Text("Clear All", color = AccentError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = DarkSurfaceElevated
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ObsidianBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Quality Engine Section
            item {
                SettingsSectionCard(title = "Smart Quality Engine", icon = Icons.Default.HighQuality) {
                    Text(
                        text = "Preferred Video Resolution",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "Selects the highest available variant at or below your target without upscaling.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    DownloadQualityPreference.entries.forEach { quality ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSetPreferredQuality(quality) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = preferences.preferredQuality == quality,
                                onClick = { onSetPreferredQuality(quality) },
                                colors = RadioButtonDefaults.colors(selectedColor = XBlue)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = quality.label,
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary)
                            )
                        }
                    }

                    HorizontalDivider(
                        color = BorderSubtle,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    SettingsToggleRow(
                        title = "Long-Video Policy (140s threshold)",
                        subtitle = "Caps videos longer than 2m 20s to 720p by default to preserve storage and bandwidth.",
                        checked = preferences.longVideoPolicyEnabled,
                        onCheckedChange = onToggleLongVideoPolicy,
                        testTag = "long_video_policy_toggle"
                    )
                }
            }

            // Automation & Duplicates Section
            item {
                SettingsSectionCard(title = "Automation & Prevention", icon = Icons.Default.Tune) {
                    SettingsToggleRow(
                        title = "Automatic Downloads",
                        subtitle = "Begin downloading immediately when post link is received from Sharesheet or copied.",
                        checked = preferences.autoDownloadEnabled,
                        onCheckedChange = onToggleAutoDownload,
                        testTag = "auto_download_toggle"
                    )

                    HorizontalDivider(
                        color = BorderSubtle,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    SettingsToggleRow(
                        title = "Duplicate Download Prevention",
                        subtitle = "Ignores re-downloading posts that have already been saved to your library.",
                        checked = preferences.preventDuplicates,
                        onCheckedChange = onTogglePreventDuplicates,
                        testTag = "prevent_duplicates_toggle"
                    )
                }
            }

            // Network & Notifications Section
            item {
                SettingsSectionCard(title = "Network & Notifications", icon = Icons.Default.Wifi) {
                    SettingsToggleRow(
                        title = "Download on Wi-Fi Only",
                        subtitle = "Prevents downloading large video streams while on cellular data.",
                        checked = preferences.wifiOnly,
                        onCheckedChange = onToggleWifiOnly,
                        testTag = "wifi_only_toggle"
                    )

                    HorizontalDivider(
                        color = BorderSubtle,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    SettingsToggleRow(
                        title = "Download Completion Notifications",
                        subtitle = "Displays 'X Media Downloaded ✓' when file is completely saved and verified.",
                        checked = preferences.notificationsEnabled,
                        onCheckedChange = onToggleNotifications,
                        testTag = "notifications_toggle"
                    )
                }
            }

            // Storage & Destination Section
            item {
                SettingsSectionCard(title = "Storage Destination", icon = Icons.Default.Storage) {
                    Text(
                        text = "Native MediaStore Integration",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "Videos are saved to 'Movies/AutoXDownloader' and photos to 'Pictures/AutoXDownloader'. They appear directly in Google Photos and your device Gallery without requiring broad storage permissions.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Media Resolver Configuration
            item {
                SettingsSectionCard(title = "Media Resolver Service", icon = Icons.Default.Security) {
                    Text(
                        text = "Syndication Gateway Adapter",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "Default compliant resolver queries public X syndication endpoints. You can optionally supply your own self-hosted resolver endpoint below.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                        modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                    )

                    OutlinedTextField(
                        value = customResolverInput,
                        onValueChange = {
                            customResolverInput = it
                            onSetCustomResolver(it)
                        },
                        placeholder = { Text("https://my-custom-resolver.example.com", color = TextMuted) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = XBlue,
                            unfocusedBorderColor = BorderMedium,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            // Download History Management
            item {
                SettingsSectionCard(title = "History & Storage", icon = Icons.Default.DeleteSweep) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Clear History",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = "Clear recorded list of past downloads",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                        }

                        TextButton(
                            onClick = { showClearHistoryDialog = true },
                            modifier = Modifier.testTag("clear_history_button")
                        ) {
                            Text("Clear", color = AccentError, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Privacy & App Version Section
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Auto X Downloader v1.0.0",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "100% On-Device Privacy • No Ads • No Trackers",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = XBlue,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
            }
            content()
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = androidx.compose.ui.graphics.Color.White,
                checkedTrackColor = XBlue,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = DarkSurfaceElevated
            )
        )
    }
}
