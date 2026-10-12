package com.example.resolver

import com.example.data.model.DownloadQualityPreference
import com.example.data.model.MediaItem
import com.example.data.model.MediaType
import com.example.data.model.MediaVariant

object SmartQualityEngine {

    const val LONG_VIDEO_THRESHOLD_SECONDS = 140 // 2 minutes 20 seconds

    /**
     * Selects the most appropriate media variant for an item according to media type,
     * duration, user quality preference, and long-video policy.
     */
    fun selectBestVariant(
        item: MediaItem,
        durationSeconds: Int?,
        userQualityPref: DownloadQualityPreference,
        enforceLongVideoPolicy: Boolean
    ): SelectedVariantResult {
        if (item.variants.isEmpty()) {
            throw IllegalArgumentException("MediaItem contains no available variants")
        }

        return when (item.type) {
            MediaType.IMAGE -> selectImageVariant(item.variants)
            MediaType.VIDEO, MediaType.GIF -> selectVideoVariant(
                variants = item.variants,
                durationSeconds = durationSeconds,
                userQualityPref = userQualityPref,
                enforceLongVideoPolicy = enforceLongVideoPolicy
            )
        }
    }

    private fun selectImageVariant(variants: List<MediaVariant>): SelectedVariantResult {
        // Prefer original variant if available
        val original = variants.firstOrNull { it.isOriginal }
        if (original != null) {
            return SelectedVariantResult(
                variant = original,
                actualResolution = original.resolutionLabel,
                isFallback = false,
                reason = "Original quality image"
            )
        }

        // Otherwise pick the variant with highest resolution (width * height)
        val best = variants.maxByOrNull { (it.width ?: 0) * (it.height ?: 0) } ?: variants.first()
        return SelectedVariantResult(
            variant = best,
            actualResolution = best.resolutionLabel,
            isFallback = false,
            reason = "Highest resolution image available"
        )
    }

    private fun getShortDimension(variant: MediaVariant): Int {
        val w = variant.width ?: 0
        val h = variant.height ?: 0
        return when {
            w > 0 && h > 0 -> minOf(w, h)
            h > 0 -> h
            w > 0 -> w
            else -> 0
        }
    }

    private fun selectVideoVariant(
        variants: List<MediaVariant>,
        durationSeconds: Int?,
        userQualityPref: DownloadQualityPreference,
        enforceLongVideoPolicy: Boolean
    ): SelectedVariantResult {
        val isLongVideo = durationSeconds != null && durationSeconds > LONG_VIDEO_THRESHOLD_SECONDS
        val effectivePref = if (enforceLongVideoPolicy && isLongVideo && userQualityPref == DownloadQualityPreference.MAX) {
            // Default policy: if > 140s and preference is MAX, cap to 720p to preserve mobile data and storage
            DownloadQualityPreference.RES_720P
        } else {
            userQualityPref
        }

        // Sort video variants: prefer higher resolution (short dimension), then higher bitrate
        val sorted = variants.sortedWith(
            compareByDescending<MediaVariant> { getShortDimension(it) }
                .thenByDescending { it.bitrate ?: 0L }
        )

        // Find variants at or below requested target dimension
        val candidate = sorted.firstOrNull { variant ->
            val dim = getShortDimension(variant)
            dim == 0 || dim <= effectivePref.maxVerticalPixels
        }

        val chosen = candidate ?: sorted.lastOrNull() ?: variants.first()
        val chosenDim = getShortDimension(chosen)
        val isFallback = effectivePref != DownloadQualityPreference.MAX &&
                (candidate == null || (chosenDim > 0 && chosenDim < effectivePref.maxVerticalPixels))

        val reason = when {
            isLongVideo && enforceLongVideoPolicy && effectivePref == DownloadQualityPreference.RES_720P ->
                "Applied long-video policy (>140s: capped to 720p)"
            isFallback ->
                "Requested ${effectivePref.label} unavailable; selected best alternative (${chosen.resolutionLabel})"
            else ->
                "Selected based on ${effectivePref.label}"
        }

        return SelectedVariantResult(
            variant = chosen,
            actualResolution = chosen.resolutionLabel,
            isFallback = isFallback,
            reason = reason
        )
    }

    data class SelectedVariantResult(
        val variant: MediaVariant,
        val actualResolution: String,
        val isFallback: Boolean,
        val reason: String
    )
}
