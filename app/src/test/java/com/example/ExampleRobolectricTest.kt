package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.vaizero.uploadready.domain.image.ImageProcessParams
import com.vaizero.uploadready.domain.storage.FileManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("UploadReady", appName)
  }

  @Test
  fun `fileManager formats file sizes accurately`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val fileManager = FileManager(context)

    assertEquals("0 B", fileManager.formatFileSize(0))
    assertEquals("500 B", fileManager.formatFileSize(500))
    assertEquals("35.0 KB", fileManager.formatFileSize(35 * 1024))
    assertEquals("1.00 MB", fileManager.formatFileSize(1024 * 1024))
    assertEquals("2.50 MB", fileManager.formatFileSize((2.5 * 1024 * 1024).toLong()))
  }

  @Test
  fun `imageProcessParams defaults and constraints`() {
    val params = ImageProcessParams(
      targetWidth = 200,
      targetHeight = 230,
      minSizeKb = 20,
      maxSizeKb = 50,
      format = "JPEG"
    )
    assertEquals(200, params.targetWidth)
    assertEquals(230, params.targetHeight)
    assertEquals(20, params.minSizeKb)
    assertEquals(50, params.maxSizeKb)
    assertEquals("JPEG", params.format)
    assertTrue(params.targetWidth > 0)
    assertTrue(params.targetHeight > 0)
  }
}
