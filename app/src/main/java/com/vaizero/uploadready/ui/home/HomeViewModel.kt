package com.vaizero.uploadready.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vaizero.uploadready.data.db.AppDatabase
import com.vaizero.uploadready.data.model.AppProfile
import com.vaizero.uploadready.data.model.RecentFile
import com.vaizero.uploadready.data.repository.ProfileRepository
import com.vaizero.uploadready.data.repository.RecentFileRepository
import com.vaizero.uploadready.domain.storage.FileManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val profileRepository = ProfileRepository(database.profileDao())
    private val recentFileRepository = RecentFileRepository(database.recentFileDao())
    val fileManager = FileManager(application)

    val profiles: StateFlow<List<AppProfile>> = profileRepository.allProfiles
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val recentFiles: StateFlow<List<RecentFile>> = recentFileRepository.getRecentFiles(10)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            profileRepository.ensureInitialPresets()
        }
    }

    fun deleteRecentFile(file: RecentFile) {
        viewModelScope.launch {
            recentFileRepository.deleteRecentFile(file)
        }
    }

    fun shareRecentFile(file: RecentFile) {
        val physicalFile = File(file.filePath)
        if (physicalFile.exists()) {
            fileManager.shareFile(physicalFile, file.mimeType, "Share ${file.fileName}")
        }
    }
}
