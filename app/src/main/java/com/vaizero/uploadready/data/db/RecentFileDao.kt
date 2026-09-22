package com.vaizero.uploadready.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vaizero.uploadready.data.model.RecentFile
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentFileDao {
    @Query("SELECT * FROM recent_files ORDER BY createdAt DESC")
    fun getAllRecentFiles(): Flow<List<RecentFile>>

    @Query("SELECT * FROM recent_files ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentFiles(limit: Int): Flow<List<RecentFile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentFile(file: RecentFile): Long

    @Delete
    suspend fun deleteRecentFile(file: RecentFile)

    @Query("DELETE FROM recent_files WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM recent_files")
    suspend fun deleteAll()
}
