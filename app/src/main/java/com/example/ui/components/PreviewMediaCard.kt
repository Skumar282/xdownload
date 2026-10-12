package com.example.ui.components

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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.MediaType
import com.example.data.model.ResolvedMedia
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.XBlue

@Composable
fun PreviewMediaCard(
    media: ResolvedMedia,
    onConfirmDownload: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("preview_media_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(XBlue))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Post Media Ready",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = XBlue
                    )
                )
                BadgePill(
                    text = "${media.items.size} media item${if (media.items.size > 1) "s" else ""}",
                    color = XBlue
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!media.thumbnailUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = media.thumbnailUrl,
                        contentDescription = "Thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                } else {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        val isVideo = media.items.any { it.type == MediaType.VIDEO }
                        Icon(
                            imageVector = if (isVideo) Icons.Default.Movie else Icons.Default.Photo,
                            contentDescription = "Media preview",
                            tint = XBlue,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    if (!media.author.isNullOrBlank() || !media.authorUsername.isNullOrBlank()) {
                        Text(
                            text = media.author ?: "@${media.authorUsername}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (!media.text.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = media.text,
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (media.durationSeconds != null && media.durationSeconds > 0) {
                        val mins = media.durationSeconds / 60
                        val secs = media.durationSeconds % 60
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Duration: ${mins}m ${secs}s",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Dismiss")
                }

                Button(
                    onClick = onConfirmDownload,
                    modifier = Modifier
                        .weight(1.5f)
                        .testTag("confirm_download_button"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = XBlue)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Start Download",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Download Media", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
