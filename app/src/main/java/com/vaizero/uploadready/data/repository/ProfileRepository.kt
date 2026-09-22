package com.vaizero.uploadready.data.repository

import com.vaizero.uploadready.data.db.ProfileDao
import com.vaizero.uploadready.data.model.AppProfile
import kotlinx.coroutines.flow.Flow

class ProfileRepository(private val profileDao: ProfileDao) {
    val allProfiles: Flow<List<AppProfile>> = profileDao.getAllProfiles()

    suspend fun getProfileById(id: Long): AppProfile? = profileDao.getProfileById(id)

    suspend fun insertProfile(profile: AppProfile): Long = profileDao.insertProfile(profile)

    suspend fun updateProfile(profile: AppProfile) = profileDao.updateProfile(profile)

    suspend fun deleteProfile(profile: AppProfile) = profileDao.deleteProfile(profile)

    suspend fun ensureInitialPresets() {
        if (profileDao.getCount() == 0) {
            val presets = listOf(
                AppProfile(
                    name = "SSC Exams / Govt Jobs",
                    description = "Standard specifications for SSC CGL, CHSL, MTS & State Staff Selection",
                    photoWidthPx = 200,
                    photoHeightPx = 230,
                    photoMinKb = 20,
                    photoMaxKb = 50,
                    photoFormat = "JPEG",
                    signatureWidthPx = 140,
                    signatureHeightPx = 60,
                    signatureMinKb = 10,
                    signatureMaxKb = 20,
                    signatureFormat = "JPEG",
                    pdfMaxKb = 1024,
                    isPreset = true
                ),
                AppProfile(
                    name = "Passport & Visa Application",
                    description = "Standard 2x2 inch square photo specifications with crisp clarity",
                    photoWidthPx = 600,
                    photoHeightPx = 600,
                    photoMinKb = 50,
                    photoMaxKb = 200,
                    photoFormat = "JPEG",
                    signatureWidthPx = 300,
                    signatureHeightPx = 150,
                    signatureMinKb = 20,
                    signatureMaxKb = 50,
                    signatureFormat = "JPEG",
                    pdfMaxKb = 2048,
                    isPreset = true
                ),
                AppProfile(
                    name = "UPSC Civil Services",
                    description = "Civil Services examination portal specifications",
                    photoWidthPx = 350,
                    photoHeightPx = 350,
                    photoMinKb = 20,
                    photoMaxKb = 300,
                    photoFormat = "JPEG",
                    signatureWidthPx = 350,
                    signatureHeightPx = 350,
                    signatureMinKb = 20,
                    signatureMaxKb = 300,
                    signatureFormat = "JPEG",
                    pdfMaxKb = 1024,
                    isPreset = true
                ),
                AppProfile(
                    name = "College Admissions",
                    description = "General academic admissions and scholarship portals",
                    photoWidthPx = 300,
                    photoHeightPx = 400,
                    photoMinKb = 30,
                    photoMaxKb = 100,
                    photoFormat = "JPEG",
                    signatureWidthPx = 200,
                    signatureHeightPx = 80,
                    signatureMinKb = 10,
                    signatureMaxKb = 30,
                    signatureFormat = "JPEG",
                    pdfMaxKb = 2048,
                    isPreset = true
                )
            )
            profileDao.insertAll(presets)
        }
    }
}
