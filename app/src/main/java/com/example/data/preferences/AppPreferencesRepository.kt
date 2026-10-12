package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.DownloadQualityPreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "autox_preferences")

data class UserPreferences(
    val autoDownloadEnabled: Boolean = true,
    val preferredQuality: DownloadQualityPreference = DownloadQualityPreference.MAX,
    val longVideoPolicyEnabled: Boolean = true,
    val wifiOnly: Boolean = false,
    val preventDuplicates: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val customResolverUrl: String = ""
)

class AppPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val AUTO_DOWNLOAD_ENABLED = booleanPreferencesKey("auto_download_enabled")
        val PREFERRED_QUALITY = stringPreferencesKey("preferred_quality")
        val LONG_VIDEO_POLICY = booleanPreferencesKey("long_video_policy")
        val WIFI_ONLY = booleanPreferencesKey("wifi_only")
        val PREVENT_DUPLICATES = booleanPreferencesKey("prevent_duplicates")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val CUSTOM_RESOLVER_URL = stringPreferencesKey("custom_resolver_url")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { preferences ->
        UserPreferences(
            autoDownloadEnabled = preferences[PreferencesKeys.AUTO_DOWNLOAD_ENABLED] ?: true,
            preferredQuality = DownloadQualityPreference.fromName(
                preferences[PreferencesKeys.PREFERRED_QUALITY]
            ),
            longVideoPolicyEnabled = preferences[PreferencesKeys.LONG_VIDEO_POLICY] ?: true,
            wifiOnly = preferences[PreferencesKeys.WIFI_ONLY] ?: false,
            preventDuplicates = preferences[PreferencesKeys.PREVENT_DUPLICATES] ?: true,
            notificationsEnabled = preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true,
            customResolverUrl = preferences[PreferencesKeys.CUSTOM_RESOLVER_URL] ?: ""
        )
    }

    suspend fun setAutoDownloadEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.AUTO_DOWNLOAD_ENABLED] = enabled }
    }

    suspend fun setPreferredQuality(quality: DownloadQualityPreference) {
        context.dataStore.edit { it[PreferencesKeys.PREFERRED_QUALITY] = quality.name }
    }

    suspend fun setLongVideoPolicyEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.LONG_VIDEO_POLICY] = enabled }
    }

    suspend fun setWifiOnly(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.WIFI_ONLY] = enabled }
    }

    suspend fun setPreventDuplicates(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.PREVENT_DUPLICATES] = enabled }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setCustomResolverUrl(url: String) {
        context.dataStore.edit { it[PreferencesKeys.CUSTOM_RESOLVER_URL] = url.trim() }
    }
}
