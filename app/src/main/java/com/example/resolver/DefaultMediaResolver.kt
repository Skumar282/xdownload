package com.example.resolver

import com.example.data.model.MediaItem
import com.example.data.model.MediaType
import com.example.data.model.MediaVariant
import com.example.data.model.ResolvedMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Default media resolver implementation for X (Twitter) status posts.
 *
 * PRIVACY & NETWORK DISCLOSURE:
 * To resolve media from public X posts without scraping or requiring personal login tokens,
 * this resolver sends the post status ID to a syndication gateway:
 * - Default gateway: https://api.vxtwitter.com/Twitter/status/{statusId}
 * - Custom gateway: Configurable by the user in Settings (e.g. self-hosted instance).
 *
 * Only the post ID / URL is sent to the configured resolver. No personal user data,
 * credentials, or device telemetry are ever transmitted.
 */
class DefaultMediaResolver(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build(),
    private val customResolverProvider: () -> String? = { null }
) : MediaResolver {

    override suspend fun resolve(postUrl: String, statusId: String): ResolutionResult = withContext(Dispatchers.IO) {
        if (statusId.isBlank()) {
            return@withContext ResolutionResult.Error(
                ResolutionError.INVALID_URL,
                ResolutionError.INVALID_URL.userMessage
            )
        }

        val customEndpoint = customResolverProvider()?.trim()
        val targetEndpoint = if (!customEndpoint.isNullOrBlank()) {
            // User configured custom resolver
            val base = customEndpoint.removeSuffix("/")
            if (base.contains("{id}")) {
                base.replace("{id}", statusId)
            } else {
                "$base/status/$statusId"
            }
        } else {
            // Standard compliant public syndication gateway
            "https://api.vxtwitter.com/Twitter/status/$statusId"
        }

        val request = Request.Builder()
            .url(targetEndpoint)
            .header("User-Agent", "AutoXDownloader/1.0 (Android Native)")
            .header("Accept", "application/json")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.code == 404) {
                    return@withContext ResolutionResult.Error(
                        ResolutionError.POST_NOT_FOUND,
                        "Post #$statusId could not be found or has been deleted."
                    )
                }

                if (response.code == 401 || response.code == 403) {
                    return@withContext ResolutionResult.Error(
                        ResolutionError.LOGIN_REQUIRED_OR_PRIVATE,
                        "This post is private or requires authentication to view."
                    )
                }

                if (!response.isSuccessful) {
                    return@withContext ResolutionResult.Error(
                        ResolutionError.NETWORK_FAILURE,
                        "Media resolver returned HTTP status ${response.code}."
                    )
                }

                val bodyString = response.body?.string()
                if (bodyString.isNullOrBlank()) {
                    return@withContext ResolutionResult.Error(
                        ResolutionError.NO_MEDIA_FOUND,
                        "Empty response received from resolver."
                    )
                }

                parseResolverJson(postUrl, statusId, bodyString)
            }
        } catch (e: IOException) {
            ResolutionResult.Error(
                ResolutionError.NETWORK_FAILURE,
                "Network connection error: ${e.localizedMessage ?: "Unable to reach server"}"
            )
        } catch (e: Exception) {
            ResolutionResult.Error(
                ResolutionError.UNSUPPORTED_CONTENT,
                "Failed to process post media: ${e.localizedMessage ?: "Unknown parsing error"}"
            )
        }
    }

    private fun parseResolverJson(
        postUrl: String,
        statusId: String,
        jsonString: String
    ): ResolutionResult {
        return try {
            val json = JSONObject(jsonString)

            val author = json.optString("user_name").ifBlank {
                json.optString("author_name").ifBlank { null }
            }
            val authorScreenName = json.optString("user_screen_name").ifBlank {
                json.optString("author").ifBlank { null }
            }
            val text = json.optString("text").ifBlank { null }

            val mediaItems = mutableListOf<MediaItem>()

            // 1. Try media_extended if available
            val mediaExtended = json.optJSONArray("media_extended")
            if (mediaExtended != null && mediaExtended.length() > 0) {
                for (i in 0 until mediaExtended.length()) {
                    val m = mediaExtended.getJSONObject(i)
                    val typeStr = m.optString("type").lowercase()
                    val mediaUrl = m.optString("url")
                    if (mediaUrl.isBlank()) continue

                    val sizeObj = m.optJSONObject("size")
                    val width = sizeObj?.optInt("width", 0)?.takeIf { it > 0 }
                    val height = sizeObj?.optInt("height", 0)?.takeIf { it > 0 }
                    val durationMillis = m.optLong("duration_millis", 0L).takeIf { it > 0 }
                    val durationSeconds = durationMillis?.let { (it / 1000).toInt() }

                    val type = when (typeStr) {
                        "video" -> MediaType.VIDEO
                        "gif" -> MediaType.GIF
                        else -> MediaType.IMAGE
                    }

                    val variants = mutableListOf<MediaVariant>()

                    // Check if multiple variants or formats exist
                    val variantsJson = m.optJSONArray("variants")
                    if (variantsJson != null && variantsJson.length() > 0) {
                        for (v in 0 until variantsJson.length()) {
                            val vObj = variantsJson.getJSONObject(v)
                            val vUrl = vObj.optString("url")
                            if (vUrl.isBlank()) continue
                            val vMime = vObj.optString("content_type", if (type == MediaType.VIDEO) "video/mp4" else "image/jpeg")
                            val vHeight = vObj.optInt("height", height ?: 0).takeIf { it > 0 }
                            val vWidth = vObj.optInt("width", width ?: 0).takeIf { it > 0 }
                            val vBitrate = vObj.optLong("bitrate", 0L).takeIf { it > 0 }

                            variants.add(
                                MediaVariant(
                                    url = vUrl,
                                    mimeType = vMime,
                                    width = vWidth,
                                    height = vHeight,
                                    bitrate = vBitrate,
                                    isOriginal = false
                                )
                            )
                        }
                    }

                    if (variants.isEmpty()) {
                        val mime = if (type == MediaType.VIDEO) "video/mp4" else "image/jpeg"
                        if (type == MediaType.IMAGE) {
                            val origUrl = toOriginalImageUrl(mediaUrl)
                            variants.add(
                                MediaVariant(
                                    url = origUrl,
                                    mimeType = mime,
                                    width = width,
                                    height = height,
                                    bitrate = null,
                                    isOriginal = true
                                )
                            )
                        } else {
                            variants.add(
                                MediaVariant(
                                    url = mediaUrl,
                                    mimeType = mime,
                                    width = width,
                                    height = height,
                                    bitrate = null,
                                    isOriginal = false
                                )
                            )
                        }
                    }

                    mediaItems.add(
                        MediaItem(
                            id = "${statusId}_$i",
                            type = type,
                            variants = variants
                        )
                    )
                }
            }

            // 2. Fallback to mediaURLs if media_extended was empty
            if (mediaItems.isEmpty()) {
                val mediaUrls = json.optJSONArray("mediaURLs")
                if (mediaUrls != null && mediaUrls.length() > 0) {
                    for (i in 0 until mediaUrls.length()) {
                        val mUrl = mediaUrls.getString(i)
                        if (mUrl.isNotBlank()) {
                            val isVideo = mUrl.contains(".mp4") || mUrl.contains("video.twimg.com")
                            val type = if (isVideo) MediaType.VIDEO else MediaType.IMAGE
                            val mime = if (isVideo) "video/mp4" else "image/jpeg"
                            val finalUrl = if (type == MediaType.IMAGE) toOriginalImageUrl(mUrl) else mUrl
                            mediaItems.add(
                                MediaItem(
                                    id = "${statusId}_$i",
                                    type = type,
                                    variants = listOf(
                                        MediaVariant(
                                            url = finalUrl,
                                            mimeType = mime,
                                            isOriginal = !isVideo
                                        )
                                    )
                                )
                            )
                        }
                    }
                }
            }

            // 3. Fallback to single video_url or image_url
            if (mediaItems.isEmpty()) {
                val singleVideo = json.optString("video_url").ifBlank { null }
                val singleImage = json.optString("image_url").ifBlank { null }

                if (singleVideo != null) {
                    mediaItems.add(
                        MediaItem(
                            id = "${statusId}_0",
                            type = MediaType.VIDEO,
                            variants = listOf(
                                MediaVariant(
                                    url = singleVideo,
                                    mimeType = "video/mp4"
                                )
                            )
                        )
                    )
                } else if (singleImage != null) {
                    val origUrl = toOriginalImageUrl(singleImage)
                    mediaItems.add(
                        MediaItem(
                            id = "${statusId}_0",
                            type = MediaType.IMAGE,
                            variants = listOf(
                                MediaVariant(
                                    url = origUrl,
                                    mimeType = "image/jpeg",
                                    isOriginal = true
                                )
                            )
                        )
                    )
                }
            }

            if (mediaItems.isEmpty()) {
                return ResolutionResult.Error(
                    ResolutionError.NO_MEDIA_FOUND,
                    "No downloadable photo or video found in this post."
                )
            }

            // Determine thumbnail
            val thumb = json.optString("thumbnail_url").ifBlank {
                mediaItems.firstOrNull { it.type == MediaType.IMAGE }?.variants?.firstOrNull()?.url
            }

            // Determine overall duration if present
            val durationSeconds = mediaExtended?.optJSONObject(0)?.optLong("duration_millis", 0L)
                ?.takeIf { it > 0 }
                ?.let { (it / 1000).toInt() }

            ResolutionResult.Success(
                ResolvedMedia(
                    postId = statusId,
                    originalUrl = postUrl,
                    author = author,
                    authorUsername = authorScreenName,
                    text = text,
                    thumbnailUrl = thumb,
                    durationSeconds = durationSeconds,
                    items = mediaItems
                )
            )
        } catch (e: Exception) {
            ResolutionResult.Error(
                ResolutionError.UNSUPPORTED_CONTENT,
                "Unable to parse media content: ${e.message}"
            )
        }
    }

    private fun toOriginalImageUrl(url: String): String {
        if (!url.contains("twimg.com")) return url
        return if (url.contains("name=")) {
            url.replace(Regex("name=[a-zA-Z0-9_]+"), "name=orig")
        } else if (url.contains("?")) {
            "$url&name=orig"
        } else {
            "$url?name=orig"
        }
    }
}
