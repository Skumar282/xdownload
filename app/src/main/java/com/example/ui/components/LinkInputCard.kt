package com.example.ui.components

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ui.theme.BorderMedium
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.XBlue

@Composable
fun LinkInputCard(
    isResolving: Boolean,
    onDownload: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var urlText by remember { mutableStateOf("") }
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(BorderMedium))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Download from X",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )
            Text(
                text = "Paste any X / Twitter post URL to extract & save media",
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
            )

            OutlinedTextField(
                value = urlText,
                onValueChange = { urlText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("url_input_field"),
                placeholder = {
                    Text(
                        "https://x.com/username/status/…",
                        color = TextMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Go
                ),
                keyboardActions = KeyboardActions(
                    onGo = {
                        if (urlText.isNotBlank()) {
                            keyboardController?.hide()
                            onDownload(urlText)
                        }
                    }
                ),
                trailingIcon = {
                    if (urlText.isNotEmpty()) {
                        IconButton(onClick = { urlText = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear input",
                                tint = TextSecondary
                            )
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = XBlue,
                    unfocusedBorderColor = BorderMedium,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = XBlue
                ),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        val clip = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()
                        if (!clip.isNullOrBlank()) {
                            urlText = clip
                        }
                    },
                    modifier = Modifier.testTag("paste_button"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = "Paste from clipboard",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Paste", style = MaterialTheme.typography.labelLarge)
                }

                Button(
                    onClick = {
                        if (urlText.isNotBlank()) {
                            keyboardController?.hide()
                            onDownload(urlText)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("download_submit_button"),
                    enabled = urlText.isNotBlank() && !isResolving,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = XBlue,
                        contentColor = androidx.compose.ui.graphics.Color.White
                    )
                ) {
                    if (isResolving) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            color = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Resolving…", fontWeight = FontWeight.SemiBold)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download media",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Download", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
