package com.vaizero.uploadready.data.repository

import com.vaizero.uploadready.data.db.RecentFileDao
import com.vaizero.uploadready.data.model.RecentFile
import kotlinx.coroutines.flow.Flow
import java.io.File

class RecentFileRepository(private val recentFileDao: RecentFileDao) {
    val allRecentFiles: Flow<List<RecentFile>> = recentFileDao.getAllRecentFiles()

    fun getRecentFiles(limit: Int): Flow<List<RecentFile>> = recentFileDao.getRecentFiles(limit)

    suspend fun insertRecentFile(recentFile: RecentFile): Long = recentFileDao.insertRecentFile(recentFile)

    suspend fun deleteRecentFile(recentFile: RecentFile) {
        // Also delete physical file if exists
        try {
            val file = File(recentFile.filePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {
            // Ignore file system delete errors
        }
        recentFileDao.deleteRecentFile(recentFile)
    }

    suspend fun clearAll() {
        recentFileDao.deleteAll()
    }
}
