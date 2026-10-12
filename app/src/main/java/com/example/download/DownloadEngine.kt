package com.example.download

import android.content.ContentValues
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.data.local.DownloadDao
import com.example.data.local.DownloadEntity
import com.example.data.model.DownloadStatus
import com.example.data.model.MediaType
import com.example.data.model.MediaVariant
import com.example.data.model.ResolvedMedia
import com.example.notification.DownloadNotificationManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

sealed class DownloadResult {
    data class Success(val downloadId: Long, val uri: Uri, val fileName: String) : DownloadResult()
    data class Duplicate(val existingDownloadId: Long, val fileName: String) : DownloadResult()
    data class Error(val message: String, val isNetworkError: Boolean = false) : DownloadResult()
}

class DownloadEngine(
    private val context: Context,
    private val downloadDao: DownloadDao,
    private val notificationManager: DownloadNotificationManager,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) {

    private val activeJobs = ConcurrentHashMap<Long, Job>()
    private val mutex = Mutex()

    private val _isDownloadingFlow = MutableStateFlow(false)
    val isDownloadingFlow = _isDownloadingFlow.asStateFlow()

    /**
     * Checks if current connection matches Wi-Fi constraints
     */
    fun isConnectedToWifi(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    fun hasNetworkConnection(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Executes download of a selected variant.
     */
    suspend fun executeDownload(
        resolvedMedia: ResolvedMedia,
        selectedVariant: MediaVariant,
        mediaType: MediaType,
        actualResolution: String,
        wifiOnly: Boolean,
        preventDuplicates: Boolean,
        notificationsEnabled: Boolean
    ): DownloadResult = withContext(Dispatchers.IO) {
        // 1. Connection check
        if (!hasNetworkConnection()) {
            return@withContext DownloadResult.Error(
                "No active internet connection.",
                isNetworkError = true
            )
        }

        if (wifiOnly && !isConnectedToWifi()) {
            return@withContext DownloadResult.Error(
                "Wi-Fi only mode is enabled, but device is on mobile data.",
                isNetworkError = true
            )
        }

        // 2. Duplicate detection
        if (preventDuplicates) {
            val existing = downloadDao.findCompletedByPostId(resolvedMedia.postId)
            if (existing != null) {
                return@withContext DownloadResult.Duplicate(
                    existingDownloadId = existing.id,
                    fileName = existing.fileName
                )
            }
        }

        // 3. Prepare File name
        val extension = when (mediaType) {
            MediaType.VIDEO -> "mp4"
            MediaType.GIF -> "mp4"
            MediaType.IMAGE -> if (selectedVariant.mimeType.contains("png")) "png" else "jpg"
        }
        val authorTag = resolvedMedia.authorUsername?.takeIf { it.isNotBlank() } ?: "post"
        val fileName = "x_${authorTag}_${resolvedMedia.postId}.$extension"

        // 4. Create record in Room Database
        val entity = DownloadEntity(
            postId = resolvedMedia.postId,
            postUrl = resolvedMedia.originalUrl,
            status = DownloadStatus.DOWNLOADING,
            mediaType = mediaType,
            resolution = actualResolution,
            fileName = fileName,
            thumbnailUrl = resolvedMedia.thumbnailUrl,
            author = resolvedMedia.author ?: resolvedMedia.authorUsername,
            postText = resolvedMedia.text,
            durationSeconds = resolvedMedia.durationSeconds
        )
        val downloadId = downloadDao.insert(entity)

        var tempFile: File? = null

        try {
            _isDownloadingFlow.value = true
            tempFile = File(context.cacheDir, "download_${downloadId}.tmp")

            val request = Request.Builder()
                .url(selectedVariant.url)
                .header("User-Agent", "AutoXDownloader/1.0 (Android Native)")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errMsg = "HTTP error ${response.code} downloading media."
                    downloadDao.markFailed(downloadId, errMsg)
                    if (notificationsEnabled) {
                        notificationManager.showFailureNotification(downloadId, errMsg)
                    }
                    return@withContext DownloadResult.Error(errMsg, isNetworkError = response.code >= 500)
                }

                val body = response.body ?: throw IOException("Empty server response body")
                val totalLength = body.contentLength()
                val byteStream = body.byteStream()

                var downloadedBytes = 0L
                val buffer = ByteArray(32 * 1024) // 32KB buffer

                FileOutputStream(tempFile).use { fos ->
                    var lastProgressUpdate = 0L
                    var read: Int

                    while (byteStream.read(buffer).also { read = it } != -1) {
                        fos.write(buffer, 0, read)
                        downloadedBytes += read

                        val now = System.currentTimeMillis()
                        if (now - lastProgressUpdate > 250 || (totalLength > 0 && downloadedBytes == totalLength)) {
                            lastProgressUpdate = now
                            val progress = if (totalLength > 0) {
                                ((downloadedBytes * 100) / totalLength).toInt().coerceIn(0, 100)
                            } else {
                                0
                            }
                            downloadDao.updateProgress(
                                id = downloadId,
                                downloadedBytes = downloadedBytes,
                                totalBytes = if (totalLength > 0) totalLength else downloadedBytes,
                                progress = progress,
                                status = DownloadStatus.DOWNLOADING
                            )
                        }
                    }
                    fos.flush()
                }

                // Verify temp file
                if (!tempFile.exists() || tempFile.length() == 0L) {
                    throw IOException("Downloaded file is empty or corrupted.")
                }

                val finalSize = tempFile.length()

                // 5. Transfer to MediaStore
                val finalUri = saveToMediaStore(
                    tempFile = tempFile,
                    fileName = fileName,
                    mediaType = mediaType,
                    mimeType = selectedVariant.mimeType
                )

                // 6. Mark completed in Room
                downloadDao.markCompleted(
                    id = downloadId,
                    localUri = finalUri.toString(),
                    fileSizeBytes = finalSize
                )

                // 7. Show success notification
                if (notificationsEnabled) {
                    notificationManager.showSuccessNotification(
                        downloadId = downloadId,
                        fileName = fileName,
                        author = resolvedMedia.authorUsername ?: resolvedMedia.author,
                        mediaUri = finalUri
                    )
                }

                DownloadResult.Success(
                    downloadId = downloadId,
                    uri = finalUri,
                    fileName = fileName
                )
            }
        } catch (e: CancellationException) {
            downloadDao.updateProgress(downloadId, 0, 0, 0, DownloadStatus.CANCELLED)
            DownloadResult.Error("Download cancelled by user.")
        } catch (e: IOException) {
            val isSpaceError = e.message?.contains("ENOSPC", ignoreCase = true) == true ||
                    e.message?.contains("No space left", ignoreCase = true) == true
            val errorMsg = if (isSpaceError) {
                "Storage is full. Please free up space on your device."
            } else {
                "Network or storage error: ${e.localizedMessage ?: "Unknown I/O error"}"
            }
            downloadDao.markFailed(downloadId, errorMsg)
            if (notificationsEnabled) {
                notificationManager.showFailureNotification(downloadId, errorMsg)
            }
            DownloadResult.Error(errorMsg, isNetworkError = !isSpaceError)
        } catch (e: Exception) {
            val errorMsg = "Download failed: ${e.localizedMessage ?: "Unknown error"}"
            downloadDao.markFailed(downloadId, errorMsg)
            if (notificationsEnabled) {
                notificationManager.showFailureNotification(downloadId, errorMsg)
            }
            DownloadResult.Error(errorMsg)
        } finally {
            // Partial file cleanup
            tempFile?.let {
                if (it.exists()) it.delete()
            }
            _isDownloadingFlow.value = false
        }
    }

    private fun saveToMediaStore(
        tempFile: File,
        fileName: String,
        mediaType: MediaType,
        mimeType: String
    ): Uri {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.IS_PENDING, 1)
                val relativePath = when (mediaType) {
                    MediaType.VIDEO, MediaType.GIF -> "${Environment.DIRECTORY_MOVIES}/AutoXDownloader"
                    MediaType.IMAGE -> "${Environment.DIRECTORY_PICTURES}/AutoXDownloader"
                }
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            }
        }

        val collectionUri = when (mediaType) {
            MediaType.VIDEO, MediaType.GIF -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            MediaType.IMAGE -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val itemUri = resolver.insert(collectionUri, contentValues)
            ?: throw IOException("Failed to create MediaStore entry.")

        try {
            resolver.openOutputStream(itemUri).use { outStream ->
                if (outStream == null) throw IOException("Failed to open output stream to MediaStore URI.")
                tempFile.inputStream().use { inStream ->
                    inStream.copyTo(outStream, bufferSize = 32 * 1024)
                }
                outStream.flush()
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(itemUri, contentValues, null, null)
            }

            return itemUri
        } catch (e: Exception) {
            // Clean up incomplete MediaStore row if writing failed
            resolver.delete(itemUri, null, null)
            throw e
        }
    }

    suspend fun cancelDownload(downloadId: Long) {
        activeJobs[downloadId]?.cancel()
        downloadDao.updateProgress(downloadId, 0, 0, 0, DownloadStatus.CANCELLED)
    }

    suspend fun retryDownload(
        downloadId: Long,
        wifiOnly: Boolean,
        notificationsEnabled: Boolean
    ): DownloadResult {
        val record = downloadDao.getDownloadById(downloadId)
            ?: return DownloadResult.Error("Download record not found.")

        // Mark back to DOWNLOADING
        downloadDao.updateProgress(downloadId, 0, 0, 0, DownloadStatus.DOWNLOADING)

        // Run download
        return executeDownload(
            resolvedMedia = ResolvedMedia(
                postId = record.postId,
                originalUrl = record.postUrl,
                author = record.author,
                authorUsername = record.author,
                text = record.postText,
                thumbnailUrl = record.thumbnailUrl,
                durationSeconds = record.durationSeconds,
                items = emptyList() // Will use reconstructed variant
            ),
            selectedVariant = MediaVariant(
                url = record.postUrl, // Resolver or direct
                mimeType = if (record.mediaType == MediaType.VIDEO) "video/mp4" else "image/jpeg"
            ),
            mediaType = record.mediaType,
            actualResolution = record.resolution,
            wifiOnly = wifiOnly,
            preventDuplicates = false,
            notificationsEnabled = notificationsEnabled
        )
    }
}
