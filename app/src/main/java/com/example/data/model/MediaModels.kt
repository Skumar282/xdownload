package com.example.data.model

enum class MediaType {
    VIDEO,
    IMAGE,
    GIF
}

enum class DownloadStatus {
    IDLE,
    RESOLVING,
    DOWNLOADING,
    COMPLETED,
    FAILED,
    CANCELLED
}

enum class DownloadQualityPreference(val label: String, val maxVerticalPixels: Int) {
    MAX("Maximum Available", Int.MAX_VALUE),
    RES_1080P("1080p (Full HD)", 1080),
    RES_720P("720p (HD)", 720),
    RES_480P("480p (SD)", 480);

    companion object {
        fun fromName(name: String?): DownloadQualityPreference {
            return entries.find { it.name == name } ?: MAX
        }
    }
}

data class MediaVariant(
    val url: String,
    val mimeType: String,
    val width: Int? = null,
    val height: Int? = null,
    val bitrate: Long? = null,
    val isOriginal: Boolean = false
) {
    val resolutionLabel: String
        get() = when {
            width != null && height != null -> "${width}x${height}"
            height != null -> "${height}p"
            isOriginal -> "Original"
            else -> "Standard"
        }
}

data class MediaItem(
    val id: String,
    val type: MediaType,
    val variants: List<MediaVariant>
)

data class ResolvedMedia(
    val postId: String,
    val originalUrl: String,
    val author: String?,
    val authorUsername: String?,
    val text: String?,
    val thumbnailUrl: String?,
    val durationSeconds: Int?,
    val items: List<MediaItem>
)
