package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.DownloadStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {

    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun getAllDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getDownloadById(id: Long): DownloadEntity?

    @Query("SELECT * FROM downloads WHERE id = :id")
    fun getDownloadByIdFlow(id: Long): Flow<DownloadEntity?>

    @Query("SELECT * FROM downloads WHERE postId = :postId AND status = 'COMPLETED' LIMIT 1")
    suspend fun findCompletedByPostId(postId: String): DownloadEntity?

    @Query("SELECT * FROM downloads WHERE postId = :postId ORDER BY createdAt DESC LIMIT 1")
    suspend fun findLatestByPostId(postId: String): DownloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(download: DownloadEntity): Long

    @Update
    suspend fun update(download: DownloadEntity)

    @Query("UPDATE downloads SET downloadedBytes = :downloadedBytes, fileSizeBytes = :totalBytes, progress = :progress, status = :status WHERE id = :id")
    suspend fun updateProgress(
        id: Long,
        downloadedBytes: Long,
        totalBytes: Long,
        progress: Int,
        status: DownloadStatus
    )

    @Query("UPDATE downloads SET localUri = :localUri, fileSizeBytes = :fileSizeBytes, downloadedBytes = :fileSizeBytes, progress = 100, status = 'COMPLETED', errorMessage = null WHERE id = :id")
    suspend fun markCompleted(id: Long, localUri: String, fileSizeBytes: Long)

    @Query("UPDATE downloads SET status = 'FAILED', errorMessage = :errorMessage WHERE id = :id")
    suspend fun markFailed(id: Long, errorMessage: String)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM downloads")
    suspend fun clearAll()
}
