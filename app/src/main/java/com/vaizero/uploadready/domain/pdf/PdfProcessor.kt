package com.vaizero.uploadready.domain.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

data class PdfCreationParams(
    val maxKb: Int = 1024,
    val pageSize: PdfPageFormat = PdfPageFormat.A4,
    val customFileNamePrefix: String = "document"
)

enum class PdfPageFormat(val widthPoints: Int, val heightPoints: Int) {
    A4(595, 842),
    LETTER(612, 792),
    FIT_IMAGE(0, 0)
}

data class PdfMetadata(
    val fileName: String,
    val pageCount: Int,
    val fileSizeBytes: Long,
    val file: File
)

sealed class PdfResult {
    data class Success(
        val file: File,
        val pageCount: Int,
        val fileSizeBytes: Long,
        val metadata: PdfMetadata
    ) : PdfResult()

    data class Error(
        val message: String,
        val cause: Throwable? = null
    ) : PdfResult()
}

class PdfProcessor(private val context: Context) {

    suspend fun createPdfFromImages(
        imageUris: List<Uri>,
        params: PdfCreationParams
    ): PdfResult = withContext(Dispatchers.IO) {
        if (imageUris.isEmpty()) {
            return@withContext PdfResult.Error("Please select at least one image to create a PDF.")
        }

        try {
            val outputDir = File(context.filesDir, "UploadReady_Outputs").apply { mkdirs() }
            val timeStamp = System.currentTimeMillis() % 10000
            val outputFile = File(outputDir, "${params.customFileNamePrefix}_$timeStamp.pdf")

            // Determine image compression quality per page to target maxKb
            val maxBytes = params.maxKb * 1024L
            val targetBytesPerPage = (maxBytes * 0.90 / imageUris.size).toLong().coerceAtLeast(10 * 1024L)

            val document = PdfDocument()

            try {
                for ((index, uri) in imageUris.withIndex()) {
                    val rawBitmap = decodeBitmapFromUri(uri)
                        ?: return@withContext PdfResult.Error("Could not read image page ${index + 1}.")

                    // Determine page dimensions (standard A4 is 595 x 842 points)
                    val pageWidth = if (params.pageSize == PdfPageFormat.FIT_IMAGE) {
                        rawBitmap.width.coerceIn(400, 1200)
                    } else {
                        params.pageSize.widthPoints
                    }
                    val pageHeight = if (params.pageSize == PdfPageFormat.FIT_IMAGE) {
                        rawBitmap.height.coerceIn(400, 1600)
                    } else {
                        params.pageSize.heightPoints
                    }

                    // Compress page bitmap if needed to stay within target size
                    val compressedBitmap = optimizeBitmapForPdf(rawBitmap, targetBytesPerPage)

                    val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
                    val page = document.startPage(pageInfo)
                    val canvas: Canvas = page.canvas

                    // Clean white page background
                    canvas.drawColor(Color.WHITE)

                    // Fit image into page with 20pt margin
                    val margin = 20f
                    val availableW = pageWidth - (margin * 2)
                    val availableH = pageHeight - (margin * 2)

                    val scale = min(availableW / compressedBitmap.width, availableH / compressedBitmap.height)
                    val drawW = compressedBitmap.width * scale
                    val drawH = compressedBitmap.height * scale
                    val left = margin + (availableW - drawW) / 2f
                    val top = margin + (availableH - drawH) / 2f

                    val destRect = Rect(
                        left.toInt(),
                        top.toInt(),
                        (left + drawW).toInt(),
                        (top + drawH).toInt()
                    )
                    canvas.drawBitmap(compressedBitmap, null, destRect, Paint(Paint.FILTER_BITMAP_FLAG))

                    document.finishPage(page)

                    if (compressedBitmap != rawBitmap) {
                        compressedBitmap.recycle()
                    }
                    rawBitmap.recycle()
                }

                FileOutputStream(outputFile).use { fos ->
                    document.writeTo(fos)
                }
            } finally {
                document.close()
            }

            val finalSize = outputFile.length()
            val meta = PdfMetadata(
                fileName = outputFile.name,
                pageCount = imageUris.size,
                fileSizeBytes = finalSize,
                file = outputFile
            )

            PdfResult.Success(
                file = outputFile,
                pageCount = imageUris.size,
                fileSizeBytes = finalSize,
                metadata = meta
            )
        } catch (e: Exception) {
            PdfResult.Error(
                message = e.localizedMessage ?: "Failed to generate PDF. Please try again.",
                cause = e
            )
        }
    }

    suspend fun mergePdfs(
        pdfUris: List<Uri>,
        outputPrefix: String = "merged_doc"
    ): PdfResult = withContext(Dispatchers.IO) {
        if (pdfUris.size < 2) {
            return@withContext PdfResult.Error("Please select at least two PDF documents to merge.")
        }

        try {
            val outputDir = File(context.filesDir, "UploadReady_Outputs").apply { mkdirs() }
            val timeStamp = System.currentTimeMillis() % 10000
            val outputFile = File(outputDir, "${outputPrefix}_$timeStamp.pdf")

            val document = PdfDocument()
            var totalPages = 0

            try {
                for (uri in pdfUris) {
                    val pfd: ParcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
                        ?: continue

                    val renderer = PdfRenderer(pfd)
                    for (i in 0 until renderer.pageCount) {
                        val rendererPage = renderer.openPage(i)
                        totalPages++

                        val w = rendererPage.width
                        val h = rendererPage.height
                        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                        val pageCanvas = Canvas(bitmap)
                        pageCanvas.drawColor(Color.WHITE)

                        rendererPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)

                        val pageInfo = PdfDocument.PageInfo.Builder(w, h, totalPages).create()
                        val pdfPage = document.startPage(pageInfo)
                        pdfPage.canvas.drawBitmap(bitmap, 0f, 0f, null)
                        document.finishPage(pdfPage)

                        bitmap.recycle()
                        rendererPage.close()
                    }
                    renderer.close()
                    pfd.close()
                }

                if (totalPages == 0) {
                    return@withContext PdfResult.Error("Could not read any pages from the selected PDF files.")
                }

                FileOutputStream(outputFile).use { fos ->
                    document.writeTo(fos)
                }
            } finally {
                document.close()
            }

            val finalSize = outputFile.length()
            val meta = PdfMetadata(
                fileName = outputFile.name,
                pageCount = totalPages,
                fileSizeBytes = finalSize,
                file = outputFile
            )

            PdfResult.Success(
                file = outputFile,
                pageCount = totalPages,
                fileSizeBytes = finalSize,
                metadata = meta
            )
        } catch (e: Exception) {
            PdfResult.Error(
                message = e.localizedMessage ?: "Failed to merge PDF files.",
                cause = e
            )
        }
    }

    private fun decodeBitmapFromUri(uri: Uri): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream, null, options)
        }
    }

    private fun optimizeBitmapForPdf(original: Bitmap, targetBytes: Long): Bitmap {
        // Estimate size if compressed
        val stream = ByteArrayOutputStream()
        original.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        val currentBytes = stream.size().toLong()

        if (currentBytes <= targetBytes) {
            return original
        }

        // Downsample dimensions or compress with lower quality
        val scale = min(1.0f, kotlin.math.sqrt(targetBytes.toDouble() / currentBytes.toDouble()).toFloat())
        val newW = (original.width * scale).toInt().coerceAtLeast(300)
        val newH = (original.height * scale).toInt().coerceAtLeast(400)

        val scaled = Bitmap.createScaledBitmap(original, newW, newH, true)
        return scaled
    }
}
