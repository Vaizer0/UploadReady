package com.vaizero.uploadready.ui.profiles

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vaizero.uploadready.data.db.AppDatabase
import com.vaizero.uploadready.data.model.AppProfile
import com.vaizero.uploadready.data.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileDialogState(
    val isOpen: Boolean = false,
    val editingProfile: AppProfile? = null,
    val name: String = "",
    val photoWidth: String = "200",
    val photoHeight: String = "230",
    val photoMinKb: String = "20",
    val photoMaxKb: String = "50",
    val signatureWidth: String = "140",
    val signatureHeight: String = "60",
    val signatureMinKb: String = "10",
    val signatureMaxKb: String = "20",
    val pdfMaxKb: String = "1024",
    val error: String? = null
)

class ProfilesViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val profileRepository = ProfileRepository(database.profileDao())

    val profiles: StateFlow<List<AppProfile>> = profileRepository.allProfiles
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _dialogState = MutableStateFlow(ProfileDialogState())
    val dialogState: StateFlow<ProfileDialogState> = _dialogState.asStateFlow()

    init {
        viewModelScope.launch {
            profileRepository.ensureInitialPresets()
        }
    }

    fun openCreateDialog() {
        _dialogState.value = ProfileDialogState(isOpen = true)
    }

    fun openEditDialog(profile: AppProfile) {
        _dialogState.value = ProfileDialogState(
            isOpen = true,
            editingProfile = profile,
            name = profile.name,
            photoWidth = profile.photoWidthPx.toString(),
            photoHeight = profile.photoHeightPx.toString(),
            photoMinKb = profile.photoMinKb.toString(),
            photoMaxKb = profile.photoMaxKb.toString(),
            signatureWidth = profile.signatureWidthPx.toString(),
            signatureHeight = profile.signatureHeightPx.toString(),
            signatureMinKb = profile.signatureMinKb.toString(),
            signatureMaxKb = profile.signatureMaxKb.toString(),
            pdfMaxKb = profile.pdfMaxKb.toString()
        )
    }

    fun closeDialog() {
        _dialogState.value = ProfileDialogState(isOpen = false)
    }

    fun updateDialogName(v: String) { _dialogState.value = _dialogState.value.copy(name = v) }
    fun updatePhotoWidth(v: String) { _dialogState.value = _dialogState.value.copy(photoWidth = v.filter { it.isDigit() }) }
    fun updatePhotoHeight(v: String) { _dialogState.value = _dialogState.value.copy(photoHeight = v.filter { it.isDigit() }) }
    fun updatePhotoMinKb(v: String) { _dialogState.value = _dialogState.value.copy(photoMinKb = v.filter { it.isDigit() }) }
    fun updatePhotoMaxKb(v: String) { _dialogState.value = _dialogState.value.copy(photoMaxKb = v.filter { it.isDigit() }) }
    fun updateSignatureWidth(v: String) { _dialogState.value = _dialogState.value.copy(signatureWidth = v.filter { it.isDigit() }) }
    fun updateSignatureHeight(v: String) { _dialogState.value = _dialogState.value.copy(signatureHeight = v.filter { it.isDigit() }) }
    fun updateSignatureMinKb(v: String) { _dialogState.value = _dialogState.value.copy(signatureMinKb = v.filter { it.isDigit() }) }
    fun updateSignatureMaxKb(v: String) { _dialogState.value = _dialogState.value.copy(signatureMaxKb = v.filter { it.isDigit() }) }
    fun updatePdfMaxKb(v: String) { _dialogState.value = _dialogState.value.copy(pdfMaxKb = v.filter { it.isDigit() }) }

    fun saveProfile() {
        val s = _dialogState.value
        if (s.name.isBlank()) {
            _dialogState.value = s.copy(error = "Profile name cannot be blank")
            return
        }

        val profile = AppProfile(
            id = s.editingProfile?.id ?: 0,
            name = s.name.trim(),
            photoWidthPx = s.photoWidth.toIntOrNull() ?: 200,
            photoHeightPx = s.photoHeight.toIntOrNull() ?: 230,
            photoMinKb = s.photoMinKb.toIntOrNull() ?: 20,
            photoMaxKb = s.photoMaxKb.toIntOrNull() ?: 50,
            photoFormat = "JPEG",
            signatureWidthPx = s.signatureWidth.toIntOrNull() ?: 140,
            signatureHeightPx = s.signatureHeight.toIntOrNull() ?: 60,
            signatureMinKb = s.signatureMinKb.toIntOrNull() ?: 10,
            signatureMaxKb = s.signatureMaxKb.toIntOrNull() ?: 20,
            pdfMaxKb = s.pdfMaxKb.toIntOrNull() ?: 1024,
            isPreset = false
        )

        viewModelScope.launch {
            if (s.editingProfile == null) {
                profileRepository.insertProfile(profile)
            } else {
                profileRepository.updateProfile(profile)
            }
            closeDialog()
        }
    }

    fun deleteProfile(profile: AppProfile) {
        viewModelScope.launch {
            profileRepository.deleteProfile(profile)
        }
    }
}
