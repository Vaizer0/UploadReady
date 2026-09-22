package com.vaizero.uploadready.ui.tools.pdf

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vaizero.uploadready.data.db.AppDatabase
import com.vaizero.uploadready.data.model.AppProfile
import com.vaizero.uploadready.data.model.FileType
import com.vaizero.uploadready.data.model.RecentFile
import com.vaizero.uploadready.data.repository.ProfileRepository
import com.vaizero.uploadready.data.repository.RecentFileRepository
import com.vaizero.uploadready.domain.pdf.PdfCreationParams
import com.vaizero.uploadready.domain.pdf.PdfPageFormat
import com.vaizero.uploadready.domain.pdf.PdfProcessor
import com.vaizero.uploadready.domain.pdf.PdfResult
import com.vaizero.uploadready.domain.storage.FileManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

enum class PdfMode {
    IMAGES_TO_PDF,
    MERGE_PDFS
}

data class PdfToolUiState(
    val mode: PdfMode = PdfMode.IMAGES_TO_PDF,
    val selectedImageUris: List<Uri> = emptyList(),
    val selectedPdfUris: List<Uri> = emptyList(),
    val maxKbText: String = "1024",
    val pageFormat: PdfPageFormat = PdfPageFormat.A4,
    val isProcessing: Boolean = false,
    val processResult: PdfResult.Success? = null,
    val errorMessage: String? = null,
    val savedNotification: String? = null
)

class PdfToolViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val profileRepository = ProfileRepository(database.profileDao())
    private val recentFileRepository = RecentFileRepository(database.recentFileDao())
    private val pdfProcessor = PdfProcessor(application)
    val fileManager = FileManager(application)

    private val _uiState = MutableStateFlow(PdfToolUiState())
    val uiState: StateFlow<PdfToolUiState> = _uiState.asStateFlow()

    val profiles: StateFlow<List<AppProfile>> = profileRepository.allProfiles
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun setMode(mode: PdfMode) {
        _uiState.value = _uiState.value.copy(
            mode = mode,
            processResult = null,
            errorMessage = null,
            savedNotification = null
        )
    }

    fun onImagesSelected(uris: List<Uri>) {
        val current = _uiState.value.selectedImageUris.toMutableList()
        current.addAll(uris)
        _uiState.value = _uiState.value.copy(
            selectedImageUris = current,
            processResult = null,
            errorMessage = null
        )
    }

    fun onPdfsSelected(uris: List<Uri>) {
        val current = _uiState.value.selectedPdfUris.toMutableList()
        current.addAll(uris)
        _uiState.value = _uiState.value.copy(
            selectedPdfUris = current,
            processResult = null,
            errorMessage = null
        )
    }

    fun moveImage(fromIndex: Int, toIndex: Int) {
        val list = _uiState.value.selectedImageUris.toMutableList()
        if (fromIndex in list.indices && toIndex in list.indices) {
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            _uiState.value = _uiState.value.copy(selectedImageUris = list)
        }
    }

    fun removeImage(index: Int) {
        val list = _uiState.value.selectedImageUris.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            _uiState.value = _uiState.value.copy(selectedImageUris = list)
        }
    }

    fun removePdf(index: Int) {
        val list = _uiState.value.selectedPdfUris.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            _uiState.value = _uiState.value.copy(selectedPdfUris = list)
        }
    }

    fun clearSelections() {
        _uiState.value = _uiState.value.copy(
            selectedImageUris = emptyList(),
            selectedPdfUris = emptyList(),
            processResult = null,
            errorMessage = null
        )
    }

    fun updateMaxKb(maxKb: String) {
        val filtered = maxKb.filter { it.isDigit() }
        _uiState.value = _uiState.value.copy(maxKbText = filtered)
    }

    fun applyProfile(profile: AppProfile) {
        _uiState.value = _uiState.value.copy(
            maxKbText = profile.pdfMaxKb.toString()
        )
    }

    fun createOrMergePdf() {
        val state = _uiState.value
        val maxKb = state.maxKbText.toIntOrNull() ?: 1024

        if (state.mode == PdfMode.IMAGES_TO_PDF) {
            if (state.selectedImageUris.isEmpty()) {
                _uiState.value = state.copy(errorMessage = "Please select at least one page image.")
                return
            }

            _uiState.value = state.copy(isProcessing = true, errorMessage = null, processResult = null)

            viewModelScope.launch {
                val params = PdfCreationParams(
                    maxKb = maxKb,
                    pageSize = state.pageFormat,
                    customFileNamePrefix = "document"
                )
                when (val result = pdfProcessor.createPdfFromImages(state.selectedImageUris, params)) {
                    is PdfResult.Success -> {
                        recentFileRepository.insertRecentFile(
                            RecentFile(
                                fileName = result.file.name,
                                fileType = FileType.PDF,
                                filePath = result.file.absolutePath,
                                mimeType = "application/pdf",
                                fileSizeBytes = result.fileSizeBytes,
                                pageCount = result.pageCount
                            )
                        )
                        _uiState.value = _uiState.value.copy(
                            isProcessing = false,
                            processResult = result,
                            errorMessage = null
                        )
                    }
                    is PdfResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isProcessing = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
        } else {
            // MERGE_PDFS
            if (state.selectedPdfUris.size < 2) {
                _uiState.value = state.copy(errorMessage = "Please select at least two PDF files to merge.")
                return
            }

            _uiState.value = state.copy(isProcessing = true, errorMessage = null, processResult = null)

            viewModelScope.launch {
                when (val result = pdfProcessor.mergePdfs(state.selectedPdfUris, "merged_doc")) {
                    is PdfResult.Success -> {
                        recentFileRepository.insertRecentFile(
                            RecentFile(
                                fileName = result.file.name,
                                fileType = FileType.PDF,
                                filePath = result.file.absolutePath,
                                mimeType = "application/pdf",
                                fileSizeBytes = result.fileSizeBytes,
                                pageCount = result.pageCount
                            )
                        )
                        _uiState.value = _uiState.value.copy(
                            isProcessing = false,
                            processResult = result,
                            errorMessage = null
                        )
                    }
                    is PdfResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isProcessing = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    fun saveToDevice() {
        val result = _uiState.value.processResult ?: return
        val exportResult = fileManager.exportToPublicStorage(result.file, "application/pdf", result.file.name)
        if (exportResult.isSuccess) {
            _uiState.value = _uiState.value.copy(
                savedNotification = "PDF saved to Downloads/UploadReady!"
            )
        } else {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Could not export PDF: ${exportResult.exceptionOrNull()?.localizedMessage}"
            )
        }
    }

    fun shareResult() {
        val result = _uiState.value.processResult ?: return
        fileManager.shareFile(result.file, "application/pdf", "Share Generated PDF")
    }
}
