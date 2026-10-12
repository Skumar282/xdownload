package com.example.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

class DownloadNotificationManager(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "x_downloads_channel"
        private const val BASE_NOTIFICATION_ID = 2000
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.notification_channel_name)
            val descriptionText = context.getString(R.string.notification_channel_desc)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showSuccessNotification(
        downloadId: Long,
        fileName: String,
        author: String?,
        mediaUri: Uri?
    ) {
        if (!hasNotificationPermission()) return

        val viewIntent = if (mediaUri != null) {
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(mediaUri, if (fileName.endsWith(".mp4")) "video/*" else "image/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            downloadId.toInt(),
            viewIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val authorText = if (!author.isNullOrBlank()) "from @$author" else ""
        val contentText = if (authorText.isNotBlank()) "$fileName $authorText" else fileName

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("X Media Downloaded ✓")
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(
                (BASE_NOTIFICATION_ID + downloadId % 1000).toInt(),
                notification
            )
        } catch (_: SecurityException) {
            // Ignored if permission was revoked in runtime
        }
    }

    fun showFailureNotification(downloadId: Long, reason: String) {
        if (!hasNotificationPermission()) return

        val appIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            downloadId.toInt(),
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Download Failed")
            .setContentText(reason)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(
                (BASE_NOTIFICATION_ID + downloadId % 1000).toInt(),
                notification
            )
        } catch (_: SecurityException) {
            // Ignored
        }
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}
