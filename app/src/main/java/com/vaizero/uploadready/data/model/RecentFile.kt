package com.vaizero.uploadready.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class FileType {
    PHOTO,
    SIGNATURE,
    PDF
}

@Entity(tableName = "recent_files")
data class RecentFile(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileName: String,
    val fileType: FileType,
    val filePath: String,
    val mimeType: String,
    val fileSizeBytes: Long,
    val width: Int = 0,
    val height: Int = 0,
    val pageCount: Int = 1,
    val createdAt: Long = System.currentTimeMillis()
)
