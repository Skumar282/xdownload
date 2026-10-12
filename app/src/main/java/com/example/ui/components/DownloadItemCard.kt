package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.DownloadEntity
import com.example.data.model.DownloadStatus
import com.example.data.model.MediaType
import com.example.ui.theme.AccentError
import com.example.ui.theme.AccentSuccess
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.XBlue

@Composable
fun DownloadItemCard(
    download: DownloadEntity,
    onDelete: (Long) -> Unit,
    onRetry: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("download_card_${download.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderSubtle))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Thumbnail or Type Icon
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (!download.thumbnailUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = download.thumbnailUrl,
                            contentDescription = "Media thumbnail",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(68.dp)
                        )
                    } else {
                        Icon(
                            imageVector = if (download.mediaType == MediaType.VIDEO) Icons.Default.Movie else Icons.Default.Image,
                            contentDescription = download.mediaType.name,
                            tint = XBlue,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Metadata Column
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Media type pill
                        BadgePill(text = download.mediaType.name, color = XBlue)

                        // Resolution pill
                        if (download.resolution.isNotBlank()) {
                            BadgePill(text = download.resolution, color = TextSecondary)
                        }

                        // Duration pill if video
                        if (download.durationSeconds != null && download.durationSeconds > 0) {
                            val mins = download.durationSeconds / 60
                            val secs = download.durationSeconds % 60
                            BadgePill(
                                text = String.format("%d:%02d", mins, secs),
                                color = TextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = download.author?.let { "@$it" } ?: download.fileName,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (!download.postText.isNullOrBlank()) {
                        Text(
                            text = download.postText,
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Size and Status
                    Row(
                        modifier = Modifier.padding(top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (download.fileSizeBytes > 0) {
                            Text(
                                text = formatFileSize(download.fileSizeBytes),
                                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        StatusIndicator(status = download.status)
                    }
                }
            }

            // Progress bar if downloading
            if (download.status == DownloadStatus.DOWNLOADING) {
                Spacer(modifier = Modifier.height(8.dp))
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Downloading…",
                            style = MaterialTheme.typography.labelSmall.copy(color = XBlue)
                        )
                        Text(
                            text = "${download.progress}%",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    if (download.progress > 0) {
                        LinearProgressIndicator(
                            progress = { download.progress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = XBlue
                        )
                    } else {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = XBlue
                        )
                    }
                }
            }

            // Error message if failed
            if (download.status == DownloadStatus.FAILED && !download.errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = download.errorMessage,
                    style = MaterialTheme.typography.bodySmall.copy(color = AccentError),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Action row
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (download.status == DownloadStatus.COMPLETED && !download.localUri.isNullOrBlank()) {
                    IconButton(
                        onClick = { openDownloadedFile(context, download.localUri, download.fileName) },
                        modifier = Modifier.testTag("open_file_button_${download.id}")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Open file",
                            tint = XBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { shareDownloadedFile(context, download.localUri, download.fileName) },
                        modifier = Modifier.testTag("share_file_button_${download.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share file",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (download.status == DownloadStatus.FAILED) {
                    IconButton(
                        onClick = { onRetry(download.id) },
                        modifier = Modifier.testTag("retry_button_${download.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Retry download",
                            tint = XBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                IconButton(
                    onClick = { onDelete(download.id) },
                    modifier = Modifier.testTag("delete_button_${download.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete record",
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusIndicator(status: DownloadStatus) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        when (status) {
            DownloadStatus.COMPLETED -> {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Completed",
                    tint = AccentSuccess,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Saved",
                    style = MaterialTheme.typography.labelSmall.copy(color = AccentSuccess)
                )
            }
            DownloadStatus.DOWNLOADING -> {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    color = XBlue,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "In Progress",
                    style = MaterialTheme.typography.labelSmall.copy(color = XBlue)
                )
            }
            DownloadStatus.FAILED -> {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "Failed",
                    tint = AccentError,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Failed",
                    style = MaterialTheme.typography.labelSmall.copy(color = AccentError)
                )
            }
            DownloadStatus.RESOLVING -> {
                Text(
                    text = "Resolving…",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                )
            }
            DownloadStatus.CANCELLED -> {
                Text(
                    text = "Cancelled",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                )
            }
            DownloadStatus.IDLE -> {}
        }
    }
}

@Composable
fun BadgePill(text: String, color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = color
            )
        )
    }
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format("%.2f GB", gb)
        mb >= 1.0 -> String.format("%.1f MB", mb)
        kb >= 1.0 -> String.format("%.0f KB", kb)
        else -> "$bytes B"
    }
}

private fun openDownloadedFile(context: Context, uriString: String, fileName: String) {
    try {
        val uri = Uri.parse(uriString)
        val mimeType = if (fileName.endsWith(".mp4", ignoreCase = true)) "video/*" else "image/*"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Open media"))
    } catch (_: Exception) {
        // Fallback to gallery
    }
}

private fun shareDownloadedFile(context: Context, uriString: String, fileName: String) {
    try {
        val uri = Uri.parse(uriString)
        val mimeType = if (fileName.endsWith(".mp4", ignoreCase = true)) "video/*" else "image/*"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share media"))
    } catch (_: Exception) {
        // Fallback
    }
}
