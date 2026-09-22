package com.vaizero.uploadready.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class AppProfile(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    // Photo specifications
    val photoWidthPx: Int = 200,
    val photoHeightPx: Int = 230,
    val photoMinKb: Int = 20,
    val photoMaxKb: Int = 50,
    val photoFormat: String = "JPEG",
    // Signature specifications
    val signatureWidthPx: Int = 140,
    val signatureHeightPx: Int = 60,
    val signatureMinKb: Int = 10,
    val signatureMaxKb: Int = 20,
    val signatureFormat: String = "JPEG",
    // PDF specifications
    val pdfMaxKb: Int = 1024,
    val isPreset: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
