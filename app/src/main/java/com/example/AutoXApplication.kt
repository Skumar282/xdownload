package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.preferences.AppPreferencesRepository
import com.example.download.DownloadEngine
import com.example.notification.DownloadNotificationManager
import com.example.resolver.DefaultMediaResolver
import com.example.resolver.MediaResolver

class AutoXApplication : Application() {

    lateinit var database: AppDatabase
        private set
    lateinit var preferencesRepository: AppPreferencesRepository
        private set
    lateinit var notificationManager: DownloadNotificationManager
        private set
    lateinit var mediaResolver: MediaResolver
        private set
    lateinit var downloadEngine: DownloadEngine
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        preferencesRepository = AppPreferencesRepository(this)
        notificationManager = DownloadNotificationManager(this)

        var customResolverCache = ""
        mediaResolver = DefaultMediaResolver { customResolverCache }

        downloadEngine = DownloadEngine(
            context = this,
            downloadDao = database.downloadDao(),
            notificationManager = notificationManager
        )
    }
}
