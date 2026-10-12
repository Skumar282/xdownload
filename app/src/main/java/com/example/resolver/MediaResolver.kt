package com.example.resolver

import com.example.data.model.ResolvedMedia

sealed class ResolutionResult {
    data class Success(val media: ResolvedMedia) : ResolutionResult()
    data class Error(val errorType: ResolutionError, val message: String) : ResolutionResult()
}

enum class ResolutionError(val userMessage: String) {
    INVALID_URL("The provided link is not a valid X or Twitter status URL."),
    POST_NOT_FOUND("The X post could not be found. It may have been deleted."),
    NO_MEDIA_FOUND("This X post does not contain downloadable images or videos."),
    LOGIN_REQUIRED_OR_PRIVATE("This post is private or requires authentication to view."),
    NETWORK_FAILURE("Network failure while connecting to the media resolver. Please check your internet connection."),
    RESOLVER_NOT_CONFIGURED("Media resolver service is not configured. Please check Settings."),
    UNSUPPORTED_CONTENT("The content format of this post is not currently supported for download.")
}

interface MediaResolver {
    /**
     * Resolves an X/Twitter status URL into structured media items and metadata.
     */
    suspend fun resolve(postUrl: String, statusId: String): ResolutionResult
}
