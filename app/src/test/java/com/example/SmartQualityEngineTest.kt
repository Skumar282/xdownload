package com.example

import com.example.data.model.DownloadQualityPreference
import com.example.data.model.MediaItem
import com.example.data.model.MediaType
import com.example.data.model.MediaVariant
import com.example.resolver.SmartQualityEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartQualityEngineTest {

    private val videoVariants = listOf(
        MediaVariant(
            url = "https://video.twimg.com/1080p.mp4",
            mimeType = "video/mp4",
            width = 1080,
            height = 1920,
            bitrate = 2_170_000L
        ),
        MediaVariant(
            url = "https://video.twimg.com/720p.mp4",
            mimeType = "video/mp4",
            width = 720,
            height = 1280,
            bitrate = 832_000L
        ),
        MediaVariant(
            url = "https://video.twimg.com/480p.mp4",
            mimeType = "video/mp4",
            width = 480,
            height = 854,
            bitrate = 256_000L
        )
    )

    @Test
    fun testShortVideoMaxQualitySelection() {
        // Duration <= 140 seconds with MAX preference selects 1080p
        val item = MediaItem(id = "1", type = MediaType.VIDEO, variants = videoVariants)
        val result = SmartQualityEngine.selectBestVariant(
            item = item,
            durationSeconds = 60,
            userQualityPref = DownloadQualityPreference.MAX,
            enforceLongVideoPolicy = true
        )

        assertEquals("https://video.twimg.com/1080p.mp4", result.variant.url)
        assertEquals("1080x1920", result.actualResolution)
        assertFalse(result.isFallback)
    }

    @Test
    fun testLongVideoPolicyEnforced() {
        // Duration > 140s (180s) caps to 720p when policy is enabled
        val item = MediaItem(id = "2", type = MediaType.VIDEO, variants = videoVariants)
        val result = SmartQualityEngine.selectBestVariant(
            item = item,
            durationSeconds = 180,
            userQualityPref = DownloadQualityPreference.MAX,
            enforceLongVideoPolicy = true
        )

        assertEquals("https://video.twimg.com/720p.mp4", result.variant.url)
        assertEquals("720x1280", result.actualResolution)
    }

    @Test
    fun testLongVideoPolicyDisabledByUser() {
        // Duration > 140s but user disabled the policy, so MAX gives 1080p
        val item = MediaItem(id = "3", type = MediaType.VIDEO, variants = videoVariants)
        val result = SmartQualityEngine.selectBestVariant(
            item = item,
            durationSeconds = 200,
            userQualityPref = DownloadQualityPreference.MAX,
            enforceLongVideoPolicy = false
        )

        assertEquals("https://video.twimg.com/1080p.mp4", result.variant.url)
    }

    @Test
    fun testExplicitResolutionPreference() {
        val item = MediaItem(id = "4", type = MediaType.VIDEO, variants = videoVariants)
        val result = SmartQualityEngine.selectBestVariant(
            item = item,
            durationSeconds = 30,
            userQualityPref = DownloadQualityPreference.RES_480P,
            enforceLongVideoPolicy = true
        )

        assertEquals("https://video.twimg.com/480p.mp4", result.variant.url)
        assertEquals("480x854", result.actualResolution)
    }

    @Test
    fun testFallbackWhenPreferredResolutionMissing() {
        // Only 480p variant available, but user wants 1080p -> selects 480p and marks fallback
        val lowResOnly = listOf(
            MediaVariant(
                url = "https://video.twimg.com/480p.mp4",
                mimeType = "video/mp4",
                width = 480,
                height = 854,
                bitrate = 256_000L
            )
        )
        val item = MediaItem(id = "5", type = MediaType.VIDEO, variants = lowResOnly)
        val result = SmartQualityEngine.selectBestVariant(
            item = item,
            durationSeconds = 20,
            userQualityPref = DownloadQualityPreference.RES_1080P,
            enforceLongVideoPolicy = true
        )

        assertEquals("https://video.twimg.com/480p.mp4", result.variant.url)
        assertTrue(result.isFallback)
    }

    @Test
    fun testImageQualitySelectionPrefersOriginal() {
        val imageVariants = listOf(
            MediaVariant(
                url = "https://pbs.twimg.com/media/small.jpg",
                mimeType = "image/jpeg",
                width = 680,
                height = 400
            ),
            MediaVariant(
                url = "https://pbs.twimg.com/media/large.jpg",
                mimeType = "image/jpeg",
                width = 2048,
                height = 1536
            ),
            MediaVariant(
                url = "https://pbs.twimg.com/media/orig.jpg",
                mimeType = "image/jpeg",
                isOriginal = true
            )
        )

        val item = MediaItem(id = "6", type = MediaType.IMAGE, variants = imageVariants)
        val result = SmartQualityEngine.selectBestVariant(
            item = item,
            durationSeconds = null,
            userQualityPref = DownloadQualityPreference.MAX,
            enforceLongVideoPolicy = true
        )

        assertEquals("https://pbs.twimg.com/media/orig.jpg", result.variant.url)
        assertEquals("Original", result.actualResolution)
    }

    @Test
    fun testDurationBoundaryAt140Seconds() {
        // Exact boundary: 140 seconds is <= 140, so it remains uncapped at MAX quality
        val item = MediaItem(id = "7", type = MediaType.VIDEO, variants = videoVariants)
        val result = SmartQualityEngine.selectBestVariant(
            item = item,
            durationSeconds = 140,
            userQualityPref = DownloadQualityPreference.MAX,
            enforceLongVideoPolicy = true
        )

        assertEquals("https://video.twimg.com/1080p.mp4", result.variant.url)
        assertEquals("1080x1920", result.actualResolution)
        assertFalse(result.isFallback)
    }

    @Test
    fun testDurationBoundaryAt141Seconds() {
        // 141 seconds exceeds 140s threshold, so it is capped to 720p
        val item = MediaItem(id = "8", type = MediaType.VIDEO, variants = videoVariants)
        val result = SmartQualityEngine.selectBestVariant(
            item = item,
            durationSeconds = 141,
            userQualityPref = DownloadQualityPreference.MAX,
            enforceLongVideoPolicy = true
        )

        assertEquals("https://video.twimg.com/720p.mp4", result.variant.url)
        assertEquals("720x1280", result.actualResolution)
    }

    @Test
    fun testMissingResolutionMetadataFallback() {
        // Variant without width/height metadata
        val unknownResVariants = listOf(
            MediaVariant(
                url = "https://video.twimg.com/unknown.mp4",
                mimeType = "video/mp4",
                width = null,
                height = null,
                bitrate = 500_000L
            )
        )
        val item = MediaItem(id = "9", type = MediaType.VIDEO, variants = unknownResVariants)
        val result = SmartQualityEngine.selectBestVariant(
            item = item,
            durationSeconds = 60,
            userQualityPref = DownloadQualityPreference.RES_720P,
            enforceLongVideoPolicy = true
        )

        assertEquals("https://video.twimg.com/unknown.mp4", result.variant.url)
        assertEquals("Standard", result.actualResolution)
    }
}
