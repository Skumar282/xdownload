package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.DownloadDao
import com.example.data.local.DownloadEntity
import com.example.data.model.DownloadQualityPreference
import com.example.data.model.DownloadStatus
import com.example.data.model.ResolvedMedia
import com.example.data.preferences.AppPreferencesRepository
import com.example.data.preferences.UserPreferences
import com.example.download.DownloadEngine
import com.example.download.DownloadResult
import com.example.resolver.MediaResolver
import com.example.resolver.ResolutionResult
import com.example.resolver.SmartQualityEngine
import com.example.util.UrlExtractor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UiState(
    val isResolving: Boolean = false,
    val resolvingStatusText: String? = null,
    val detectedClipboardUrl: String? = null,
    val previewMedia: ResolvedMedia? = null,
    val infoBanner: String? = null,
    val errorMessage: String? = null
)

class MainViewModel(
    private val downloadDao: DownloadDao,
    private val preferencesRepository: AppPreferencesRepository,
    private val mediaResolver: MediaResolver,
    private val downloadEngine: DownloadEngine
) : ViewModel() {

    val preferences: StateFlow<UserPreferences> = preferencesRepository.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserPreferences())

    val downloads: StateFlow<List<DownloadEntity>> = downloadDao.getAllDownloads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isDownloading: StateFlow<Boolean> = downloadEngine.isDownloadingFlow

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var lastHandledClipboardText: String? = null

    /**
     * Inspects clipboard content when the app is in foreground and user has granted access
     */
    fun checkClipboard(clipText: String?) {
        if (clipText.isNullOrBlank()) return
        if (clipText == lastHandledClipboardText) return

        val extracted = UrlExtractor.extractXStatusUrl(clipText)
        if (extracted != null) {
            lastHandledClipboardText = clipText
            if (preferences.value.autoDownloadEnabled) {
                // Auto download from clipboard immediately
                processPost(extracted.canonicalUrl, extracted.statusId, autoStart = true)
            } else {
                // Show clipboard prompt banner
                _uiState.value = _uiState.value.copy(
                    detectedClipboardUrl = extracted.canonicalUrl
                )
            }
        }
    }

    fun dismissClipboardBanner() {
        _uiState.value = _uiState.value.copy(detectedClipboardUrl = null)
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(infoBanner = null, errorMessage = null)
    }

    /**
     * Entry point for manual link submission or Sharesheet incoming intent
     */
    fun onUrlSubmitted(input: String, isFromSharesheet: Boolean = false) {
        val extracted = UrlExtractor.extractXStatusUrl(input)
        if (extracted == null) {
            val nonXDomain = UrlExtractor.detectNonXUrlDomain(input)
            val errorMsg = if (nonXDomain != null) {
                "Detected link from '$nonXDomain'. Auto X Downloader only processes posts from x.com or twitter.com."
            } else {
                "Please enter a valid X (Twitter) post URL (e.g. https://x.com/user/status/123456789)."
            }
            _uiState.value = _uiState.value.copy(errorMessage = errorMsg, infoBanner = null)
            return
        }

        val autoStart = preferences.value.autoDownloadEnabled || isFromSharesheet
        processPost(extracted.canonicalUrl, extracted.statusId, autoStart = autoStart)
    }

    private fun processPost(url: String, statusId: String, autoStart: Boolean) {
        viewModelScope.launch {
            // Check duplicate first if enabled
            if (preferences.value.preventDuplicates) {
                val existing = downloadDao.findCompletedByPostId(statusId)
                if (existing != null) {
                    _uiState.value = _uiState.value.copy(
                        infoBanner = "Post #$statusId has already been downloaded (${existing.fileName}).",
                        errorMessage = null,
                        detectedClipboardUrl = null
                    )
                    return@launch
                }
            }

            _uiState.value = _uiState.value.copy(
                isResolving = true,
                resolvingStatusText = "Resolving X post media…",
                errorMessage = null,
                infoBanner = null,
                detectedClipboardUrl = null
            )

            when (val result = mediaResolver.resolve(url, statusId)) {
                is ResolutionResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isResolving = false,
                        resolvingStatusText = null,
                        errorMessage = result.message
                    )
                }
                is ResolutionResult.Success -> {
                    val media = result.media
                    if (autoStart) {
                        _uiState.value = _uiState.value.copy(
                            isResolving = false,
                            resolvingStatusText = null
                        )
                        startDownloadPipeline(media)
                    } else {
                        // Show preview card
                        _uiState.value = _uiState.value.copy(
                            isResolving = false,
                            resolvingStatusText = null,
                            previewMedia = media
                        )
                    }
                }
            }
        }
    }

    fun startDownloadPreview() {
        val preview = _uiState.value.previewMedia ?: return
        _uiState.value = _uiState.value.copy(previewMedia = null)
        startDownloadPipeline(preview)
    }

    fun dismissPreview() {
        _uiState.value = _uiState.value.copy(previewMedia = null)
    }

    private fun startDownloadPipeline(media: ResolvedMedia) {
        viewModelScope.launch {
            val userPrefs = preferences.value

            // Process each item in the post
            for (item in media.items) {
                val selected = try {
                    SmartQualityEngine.selectBestVariant(
                        item = item,
                        durationSeconds = media.durationSeconds,
                        userQualityPref = userPrefs.preferredQuality,
                        enforceLongVideoPolicy = userPrefs.longVideoPolicyEnabled
                    )
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Quality selection failed: ${e.message}"
                    )
                    return@launch
                }

                when (val dlResult = downloadEngine.executeDownload(
                    resolvedMedia = media,
                    selectedVariant = selected.variant,
                    mediaType = item.type,
                    actualResolution = selected.actualResolution,
                    wifiOnly = userPrefs.wifiOnly,
                    preventDuplicates = userPrefs.preventDuplicates,
                    notificationsEnabled = userPrefs.notificationsEnabled
                )) {
                    is DownloadResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            infoBanner = "Downloaded ${dlResult.fileName} (${selected.actualResolution})",
                            errorMessage = null
                        )
                    }
                    is DownloadResult.Duplicate -> {
                        _uiState.value = _uiState.value.copy(
                            infoBanner = "Duplicate ignored: ${dlResult.fileName} already downloaded."
                        )
                    }
                    is DownloadResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            errorMessage = dlResult.message
                        )
                    }
                }
            }
        }
    }

    fun deleteDownload(id: Long) {
        viewModelScope.launch {
            downloadDao.deleteById(id)
        }
    }

    fun retryDownload(id: Long) {
        viewModelScope.launch {
            val record = downloadDao.getDownloadById(id) ?: return@launch
            processPost(record.postUrl, record.postId, autoStart = true)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            downloadDao.clearAll()
        }
    }

    // Preferences mutations
    fun setAutoDownloadEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setAutoDownloadEnabled(enabled) }
    }

    fun setPreferredQuality(quality: DownloadQualityPreference) {
        viewModelScope.launch { preferencesRepository.setPreferredQuality(quality) }
    }

    fun setLongVideoPolicyEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setLongVideoPolicyEnabled(enabled) }
    }

    fun setWifiOnly(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setWifiOnly(enabled) }
    }

    fun setPreventDuplicates(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setPreventDuplicates(enabled) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setNotificationsEnabled(enabled) }
    }

    fun setCustomResolverUrl(url: String) {
        viewModelScope.launch { preferencesRepository.setCustomResolverUrl(url) }
    }

    class Factory(
        private val downloadDao: DownloadDao,
        private val preferencesRepository: AppPreferencesRepository,
        private val mediaResolver: MediaResolver,
        private val downloadEngine: DownloadEngine
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(
                downloadDao,
                preferencesRepository,
                mediaResolver,
                downloadEngine
            ) as T
        }
    }
}
