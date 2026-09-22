package com.vaizero.uploadready.domain.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min

class ImageProcessor(private val context: Context) {

    suspend fun processImage(
        sourceUri: Uri,
        params: ImageProcessParams
    ): ProcessResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        var sourceBitmap: Bitmap? = null
        var croppedBitmap: Bitmap? = null
        var scaledBitmap: Bitmap? = null
        var processedBitmap: Bitmap? = null

        try {
            // Get original file size
            val originalSizeBytes = getUriFileSize(sourceUri)

            // Step 1: Decode image with safe downsampling
            sourceBitmap = decodeSampledBitmapFromUri(
                sourceUri,
                reqWidth = max(params.targetWidth * 2, 1024),
                reqHeight = max(params.targetHeight * 2, 1024)
            ) ?: return@withContext ProcessResult.Error("Unable to read image. The file may be damaged or unsupported.")

            // Step 2: Handle EXIF orientation
            val orientedBitmap = applyExifOrientation(sourceUri, sourceBitmap)
            if (orientedBitmap != sourceBitmap) {
                sourceBitmap.recycle()
                sourceBitmap = orientedBitmap
            }

            // Step 3: Crop if cropRect provided
            val bmpWidth = sourceBitmap.width
            val bmpHeight = sourceBitmap.height

            croppedBitmap = if (params.cropRect != null) {
                val left = (params.cropRect.left * bmpWidth).toInt().coerceIn(0, bmpWidth - 1)
                val top = (params.cropRect.top * bmpHeight).toInt().coerceIn(0, bmpHeight - 1)
                val right = (params.cropRect.right * bmpWidth).toInt().coerceIn(left + 1, bmpWidth)
                val bottom = (params.cropRect.bottom * bmpHeight).toInt().coerceIn(top + 1, bmpHeight)
                val cropW = right - left
                val cropH = bottom - top
                Bitmap.createBitmap(sourceBitmap, left, top, cropW, cropH)
            } else {
                sourceBitmap
            }

            // Step 4: Whitening / Contrast enhancement (especially for signatures)
            val filteredBitmap = if (params.makeBackgroundWhite) {
                enhanceSignature(croppedBitmap)
            } else {
                croppedBitmap
            }

            // Step 5: Resize to exact target dimensions
            scaledBitmap = Bitmap.createScaledBitmap(
                filteredBitmap,
                params.targetWidth,
                params.targetHeight,
                true
            )

            // Ensure RGB canvas with white background if converting to JPEG (in case of PNG alpha)
            val finalRgbBitmap = Bitmap.createBitmap(
                params.targetWidth,
                params.targetHeight,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(finalRgbBitmap)
            canvas.drawColor(Color.WHITE)
            canvas.drawBitmap(scaledBitmap, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG))
            processedBitmap = finalRgbBitmap

            // Step 6: Iterative compression to satisfy target size
            val maxBytes = params.maxSizeKb.toLong() * 1024L
            val minBytes = params.minSizeKb.toLong() * 1024L

            val compressFormat = when (params.format.uppercase()) {
                "PNG" -> Bitmap.CompressFormat.PNG
                else -> Bitmap.CompressFormat.JPEG
            }

            val (bestData, bestQuality) = findOptimalCompression(
                processedBitmap,
                compressFormat,
                minBytes,
                maxBytes
            )

            if (bestData == null || bestData.size > maxBytes) {
                val actualKb = (bestData?.size ?: 0) / 1024
                return@withContext ProcessResult.Error(
                    "Cannot compress to ${params.maxSizeKb} KB with ${params.targetWidth}x${params.targetHeight} px. Lowest achieved was ${actualKb} KB. Please increase maximum file size."
                )
            }

            // Step 7: Write to output file
            val extension = if (compressFormat == Bitmap.CompressFormat.PNG) "png" else "jpg"
            val outputDir = File(context.filesDir, "UploadReady_Outputs").apply { mkdirs() }
            val timeStamp = System.currentTimeMillis() % 10000
            val fileName = "${params.customFileNamePrefix}_$timeStamp.$extension"
            val outputFile = File(outputDir, fileName)

            FileOutputStream(outputFile).use { fos ->
                fos.write(bestData)
                fos.flush()
            }

            val elapsed = System.currentTimeMillis() - startTime

            ProcessResult.Success(
                file = outputFile,
                width = params.targetWidth,
                height = params.targetHeight,
                fileSizeBytes = outputFile.length(),
                format = params.format.uppercase(),
                qualityUsed = bestQuality,
                originalSizeBytes = originalSizeBytes,
                processingTimeMs = elapsed
            )
        } catch (e: Exception) {
            ProcessResult.Error(
                message = e.localizedMessage ?: "Failed to process image. Please try again.",
                cause = e
            )
        } finally {
            // Clean up memory
            if (sourceBitmap != null && !sourceBitmap.isRecycled) sourceBitmap.recycle()
            if (croppedBitmap != null && croppedBitmap != sourceBitmap && !croppedBitmap.isRecycled) croppedBitmap.recycle()
            if (scaledBitmap != null && !scaledBitmap.isRecycled) scaledBitmap.recycle()
            if (processedBitmap != null && !processedBitmap.isRecycled) processedBitmap.recycle()
        }
    }

    /**
     * Auto-detect signature bounding box to trim excess empty margins
     */
    suspend fun autoDetectSignatureCrop(sourceUri: Uri): RectF? = withContext(Dispatchers.IO) {
        try {
            val bitmap = decodeSampledBitmapFromUri(sourceUri, 800, 800) ?: return@withContext null
            val w = bitmap.width
            val h = bitmap.height

            var minX = w
            var minY = h
            var maxX = 0
            var maxY = 0
            var inkPixelCount = 0

            val pixels = IntArray(w * h)
            bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

            // Look for dark pixels (ink)
            for (y in 0 until h) {
                for (x in 0 until w) {
                    val color = pixels[y * w + x]
                    val r = (color shr 16) and 0xFF
                    val g = (color shr 8) and 0xFF
                    val b = color and 0xFF
                    val lum = 0.299 * r + 0.587 * g + 0.114 * b

                    // Non-white pixel threshold
                    if (lum < 210) {
                        inkPixelCount++
                        if (x < minX) minX = x
                        if (y < minY) minY = y
                        if (x > maxX) maxX = x
                        if (y > maxY) maxY = y
                    }
                }
            }

            bitmap.recycle()

            if (inkPixelCount > 30 && minX < maxX && minY < maxY) {
                // Add 8% padding
                val padX = ((maxX - minX) * 0.08f).toInt()
                val padY = ((maxY - minY) * 0.08f).toInt()
                val left = max(0, minX - padX).toFloat() / w
                val top = max(0, minY - padY).toFloat() / h
                val right = min(w, maxX + padX).toFloat() / w
                val bottom = min(h, maxY + padY).toFloat() / h
                RectF(left, top, right, bottom)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun enhanceSignature(bitmap: Bitmap): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        for (i in pixels.indices) {
            val color = pixels[i]
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF
            val lum = 0.299 * r + 0.587 * g + 0.114 * b

            // Enhanced thresholding:
            // Background paper (lum > 175) -> Pure white
            // Darker ink (lum <= 175) -> Contrast boosted darker
            val newColor = if (lum > 185) {
                Color.WHITE
            } else if (lum > 130) {
                // Smooth transition
                val factor = (lum - 130) / (185 - 130)
                val targetLum = (255 * factor + (lum * 0.7) * (1 - factor)).toInt().coerceIn(0, 255)
                Color.rgb(targetLum, targetLum, targetLum)
            } else {
                val darkLum = (lum * 0.65).toInt().coerceIn(0, 255)
                Color.rgb(darkLum, darkLum, darkLum)
            }
            pixels[i] = newColor
        }

        output.setPixels(pixels, 0, w, 0, 0, w, h)
        return output
    }

    private fun findOptimalCompression(
        bitmap: Bitmap,
        format: Bitmap.CompressFormat,
        minBytes: Long,
        maxBytes: Long
    ): Pair<ByteArray?, Int> {
        if (format == Bitmap.CompressFormat.PNG) {
            val stream = ByteArrayOutputStream()
            bitmap.compress(format, 100, stream)
            val bytes = stream.toByteArray()
            return Pair(bytes, 100)
        }

        var low = 5
        var high = 98
        var bestBytes: ByteArray? = null
        var bestQuality = 90

        // Binary search for highest quality that is <= maxBytes
        while (low <= high) {
            val mid = (low + high) / 2
            val stream = ByteArrayOutputStream()
            bitmap.compress(format, mid, stream)
            val currentBytes = stream.toByteArray()
            val size = currentBytes.size.toLong()

            if (size <= maxBytes) {
                bestBytes = currentBytes
                bestQuality = mid
                // Try higher quality
                low = mid + 1
            } else {
                // Too big, lower quality
                high = mid - 1
            }
        }

        // If minBytes is specified and best is smaller than minBytes,
        // we can slightly pad or keep it since meeting max size is the primary constraint
        return Pair(bestBytes, bestQuality)
    }

    private fun decodeSampledBitmapFromUri(uri: Uri, reqWidth: Int, reqHeight: Int): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, options)
        }

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false
        options.inPreferredConfig = Bitmap.Config.ARGB_8888

        return context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, options)
        }
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun applyExifOrientation(uri: Uri, bitmap: Bitmap): Bitmap {
        var rotationDegrees = 0
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exifInterface = ExifInterface(inputStream)
                val orientation = exifInterface.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
                rotationDegrees = when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            }
        } catch (_: Exception) {
            // Ignore EXIF failure, keep original rotation
        }

        if (rotationDegrees == 0) return bitmap

        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )
    }

    private fun getUriFileSize(uri: Uri): Long {
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                pfd.statSize
            } ?: 0L
        } catch (_: Exception) {
            0L
        }
    }
}
