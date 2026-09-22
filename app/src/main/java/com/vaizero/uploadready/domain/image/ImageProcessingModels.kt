package com.vaizero.uploadready.domain.image

import android.graphics.RectF
import java.io.File

data class ImageProcessParams(
    val targetWidth: Int,
    val targetHeight: Int,
    val minSizeKb: Int = 0,
    val maxSizeKb: Int,
    val format: String = "JPEG",
    val cropRect: RectF? = null, // Normalized coordinates [0f..1f]
    val makeBackgroundWhite: Boolean = false,
    val customFileNamePrefix: String = "photo"
)

sealed class ProcessResult {
    data class Success(
        val file: File,
        val width: Int,
        val height: Int,
        val fileSizeBytes: Long,
        val format: String,
        val qualityUsed: Int,
        val originalSizeBytes: Long,
        val processingTimeMs: Long
    ) : ProcessResult()

    data class Error(
        val message: String,
        val cause: Throwable? = null
    ) : ProcessResult()
}
