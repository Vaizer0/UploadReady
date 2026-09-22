package com.vaizero.uploadready.ui.tools.photo

import android.app.Application
import android.graphics.RectF
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vaizero.uploadready.data.db.AppDatabase
import com.vaizero.uploadready.data.model.AppProfile
import com.vaizero.uploadready.data.model.FileType
import com.vaizero.uploadready.data.model.RecentFile
import com.vaizero.uploadready.data.repository.ProfileRepository
import com.vaizero.uploadready.data.repository.RecentFileRepository
import com.vaizero.uploadready.domain.image.ImageProcessParams
import com.vaizero.uploadready.domain.image.ImageProcessor
import com.vaizero.uploadready.domain.image.ProcessResult
import com.vaizero.uploadready.domain.storage.FileManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PhotoToolUiState(
    val selectedUri: Uri? = null,
    val targetWidthText: String = "200",
    val targetHeightText: String = "230",
    val minSizeKbText: String = "20",
    val maxSizeKbText: String = "50",
    val selectedFormat: String = "JPEG",
    val cropAspectRatio: Float? = 200f / 230f,
    val selectedProfileId: Long? = null,
    val isProcessing: Boolean = false,
    val processResult: ProcessResult.Success? = null,
    val errorMessage: String? = null,
    val savedNotification: String? = null
)

class PhotoToolViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val profileRepository = ProfileRepository(database.profileDao())
    private val recentFileRepository = RecentFileRepository(database.recentFileDao())
    private val imageProcessor = ImageProcessor(application)
    val fileManager = FileManager(application)

    private val _uiState = MutableStateFlow(PhotoToolUiState())
    val uiState: StateFlow<PhotoToolUiState> = _uiState.asStateFlow()

    val profiles: StateFlow<List<AppProfile>> = profileRepository.allProfiles
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onImageSelected(uri: Uri?) {
        _uiState.value = _uiState.value.copy(
            selectedUri = uri,
            processResult = null,
            errorMessage = null,
            savedNotification = null
        )
    }

    fun applyProfile(profile: AppProfile) {
        _uiState.value = _uiState.value.copy(
            targetWidthText = profile.photoWidthPx.toString(),
            targetHeightText = profile.photoHeightPx.toString(),
            minSizeKbText = profile.photoMinKb.toString(),
            maxSizeKbText = profile.photoMaxKb.toString(),
            selectedFormat = profile.photoFormat,
            cropAspectRatio = profile.photoWidthPx.toFloat() / profile.photoHeightPx.toFloat(),
            selectedProfileId = profile.id
        )
    }

    fun updateWidth(width: String) {
        val filtered = width.filter { it.isDigit() }
        _uiState.value = _uiState.value.copy(targetWidthText = filtered)
    }

    fun updateHeight(height: String) {
        val filtered = height.filter { it.isDigit() }
        _uiState.value = _uiState.value.copy(targetHeightText = filtered)
    }

    fun updateMinKb(minKb: String) {
        val filtered = minKb.filter { it.isDigit() }
        _uiState.value = _uiState.value.copy(minSizeKbText = filtered)
    }

    fun updateMaxKb(maxKb: String) {
        val filtered = maxKb.filter { it.isDigit() }
        _uiState.value = _uiState.value.copy(maxSizeKbText = filtered)
    }

    fun updateFormat(format: String) {
        _uiState.value = _uiState.value.copy(selectedFormat = format)
    }

    fun setAspectRatio(ratio: Float?) {
        _uiState.value = _uiState.value.copy(cropAspectRatio = ratio)
    }

    fun processPhoto() {
        val state = _uiState.value
        val uri = state.selectedUri ?: run {
            _uiState.value = state.copy(errorMessage = "Please select a photo first.")
            return
        }

        val width = state.targetWidthText.toIntOrNull() ?: 0
        val height = state.targetHeightText.toIntOrNull() ?: 0
        val minKb = state.minSizeKbText.toIntOrNull() ?: 0
        val maxKb = state.maxSizeKbText.toIntOrNull() ?: 0

        if (width <= 10 || height <= 10) {
            _uiState.value = state.copy(errorMessage = "Please enter valid pixel dimensions (minimum 10px).")
            return
        }
        if (maxKb <= 0) {
            _uiState.value = state.copy(errorMessage = "Please enter a valid maximum file size (KB).")
            return
        }

        _uiState.value = state.copy(isProcessing = true, errorMessage = null, processResult = null)

        viewModelScope.launch {
            val params = ImageProcessParams(
                targetWidth = width,
                targetHeight = height,
                minSizeKb = minKb,
                maxSizeKb = maxKb,
                format = state.selectedFormat,
                customFileNamePrefix = "photo"
            )

            when (val result = imageProcessor.processImage(uri, params)) {
                is ProcessResult.Success -> {
                    // Record in Room recent files
                    recentFileRepository.insertRecentFile(
                        RecentFile(
                            fileName = result.file.name,
                            fileType = FileType.PHOTO,
                            filePath = result.file.absolutePath,
                            mimeType = if (result.format == "PNG") "image/png" else "image/jpeg",
                            fileSizeBytes = result.fileSizeBytes,
                            width = result.width,
                            height = result.height
                        )
                    )

                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        processResult = result,
                        errorMessage = null
                    )
                }
                is ProcessResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isProcessing = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }

    fun saveToDevice() {
        val result = _uiState.value.processResult ?: return
        val mime = if (result.format == "PNG") "image/png" else "image/jpeg"
        val exportResult = fileManager.exportToPublicStorage(result.file, mime, result.file.name)
        if (exportResult.isSuccess) {
            _uiState.value = _uiState.value.copy(
                savedNotification = "Saved successfully to Pictures/UploadReady!"
            )
        } else {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Could not export to Pictures: ${exportResult.exceptionOrNull()?.localizedMessage}"
            )
        }
    }

    fun shareResult() {
        val result = _uiState.value.processResult ?: return
        val mime = if (result.format == "PNG") "image/png" else "image/jpeg"
        fileManager.shareFile(result.file, mime, "Share Ready Photo")
    }

    fun clearNotification() {
        _uiState.value = _uiState.value.copy(savedNotification = null, errorMessage = null)
    }
}
