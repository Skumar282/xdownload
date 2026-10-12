package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.DownloadStatus
import com.example.data.model.MediaType

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val postId: String,
    val postUrl: String,
    val status: DownloadStatus,
    val mediaType: MediaType,
    val resolution: String,
    val fileSizeBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val progress: Int = 0,
    val localUri: String? = null,
    val fileName: String,
    val thumbnailUrl: String? = null,
    val author: String? = null,
    val postText: String? = null,
    val errorMessage: String? = null,
    val durationSeconds: Int? = null,
    val createdAt: Long = System.currentTimeMillis()
)
