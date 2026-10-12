package com.example.util

import java.net.URI
import java.util.regex.Pattern

object UrlExtractor {

    private val URL_REGEX = Pattern.compile(
        "https?://(?:(?:mobile|www|m)\\.)?(?:twitter\\.com|x\\.com)/[a-zA-Z0-9_]+/status/(\\d+)(?:[/?#]\\S*)?",
        Pattern.CASE_INSENSITIVE
    )

    private val GENERIC_URL_REGEX = Pattern.compile(
        "https?://\\S+",
        Pattern.CASE_INSENSITIVE
    )

    /**
     * Extracts a normalized X post URL and the status ID from raw input string.
     * The input may be a standalone URL or text shared via the Android Sharesheet.
     *
     * Returns null if no valid X/Twitter status URL is detected.
     */
    fun extractXStatusUrl(input: String?): ExtractedUrlInfo? {
        if (input.isNullOrBlank()) return null

        val matcher = URL_REGEX.matcher(input)
        if (matcher.find()) {
            val statusId = matcher.group(1) ?: return null
            val fullMatch = matcher.group(0) ?: return null

            // Clean query parameters and fragments to produce canonical URL
            val canonicalUrl = try {
                val uri = URI(fullMatch)
                "https://x.com${uri.path}"
            } catch (e: Exception) {
                // Fallback to regex strip if URI parsing fails
                fullMatch.substringBefore('?').substringBefore('#')
            }

            return ExtractedUrlInfo(
                statusId = statusId,
                canonicalUrl = canonicalUrl,
                rawMatch = fullMatch
            )
        }
        return null
    }

    /**
     * Checks if the text contains any URL that is NOT an X/Twitter URL.
     * Helpful to provide specific user feedback: "This is a YouTube link, please provide an X (Twitter) post link."
     */
    fun detectNonXUrlDomain(input: String?): String? {
        if (input.isNullOrBlank()) return null
        val matcher = GENERIC_URL_REGEX.matcher(input)
        while (matcher.find()) {
            val url = matcher.group(0)
            try {
                val uri = URI(url)
                val host = uri.host?.lowercase() ?: ""
                if (!host.contains("x.com") && !host.contains("twitter.com")) {
                    return host
                }
            } catch (_: Exception) {
                // Ignore parse error
            }
        }
        return null
    }

    data class ExtractedUrlInfo(
        val statusId: String,
        val canonicalUrl: String,
        val rawMatch: String
    )
}
