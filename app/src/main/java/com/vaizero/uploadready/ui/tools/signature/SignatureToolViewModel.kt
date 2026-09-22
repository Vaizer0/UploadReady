package com.vaizero.uploadready.ui.tools.signature

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

data class SignatureToolUiState(
    val selectedUri: Uri? = null,
    val targetWidthText: String = "140",
    val targetHeightText: String = "60",
    val minSizeKbText: String = "10",
    val maxSizeKbText: String = "20",
    val makeBackgroundWhite: Boolean = true,
    val autoTrimMargins: Boolean = true,
    val selectedProfileId: Long? = null,
    val isProcessing: Boolean = false,
    val processResult: ProcessResult.Success? = null,
    val errorMessage: String? = null,
    val savedNotification: String? = null
)

class SignatureToolViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val profileRepository = ProfileRepository(database.profileDao())
    private val recentFileRepository = RecentFileRepository(database.recentFileDao())
    private val imageProcessor = ImageProcessor(application)
    val fileManager = FileManager(application)

    private val _uiState = MutableStateFlow(SignatureToolUiState())
    val uiState: StateFlow<SignatureToolUiState> = _uiState.asStateFlow()

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
            targetWidthText = profile.signatureWidthPx.toString(),
            targetHeightText = profile.signatureHeightPx.toString(),
            minSizeKbText = profile.signatureMinKb.toString(),
            maxSizeKbText = profile.signatureMaxKb.toString(),
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

    fun toggleBackgroundWhite(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(makeBackgroundWhite = enabled)
    }

    fun toggleAutoTrim(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(autoTrimMargins = enabled)
    }

    fun processSignature() {
        val state = _uiState.value
        val uri = state.selectedUri ?: run {
            _uiState.value = state.copy(errorMessage = "Please select a signature image first.")
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
            // Auto detect crop if auto-trim is enabled
            val detectedCrop: RectF? = if (state.autoTrimMargins) {
                imageProcessor.autoDetectSignatureCrop(uri)
            } else {
                null
            }

            val params = ImageProcessParams(
                targetWidth = width,
                targetHeight = height,
                minSizeKb = minKb,
                maxSizeKb = maxKb,
                format = "JPEG",
                cropRect = detectedCrop,
                makeBackgroundWhite = state.makeBackgroundWhite,
                customFileNamePrefix = "signature"
            )

            when (val result = imageProcessor.processImage(uri, params)) {
                is ProcessResult.Success -> {
                    recentFileRepository.insertRecentFile(
                        RecentFile(
                            fileName = result.file.name,
                            fileType = FileType.SIGNATURE,
                            filePath = result.file.absolutePath,
                            mimeType = "image/jpeg",
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
        val exportResult = fileManager.exportToPublicStorage(result.file, "image/jpeg", result.file.name)
        if (exportResult.isSuccess) {
            _uiState.value = _uiState.value.copy(
                savedNotification = "Signature saved to Pictures/UploadReady!"
            )
        } else {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Could not export: ${exportResult.exceptionOrNull()?.localizedMessage}"
            )
        }
    }

    fun shareResult() {
        val result = _uiState.value.processResult ?: return
        fileManager.shareFile(result.file, "image/jpeg", "Share Ready Signature")
    }
}
