package com.dynatech2012.kamleonuserapp.provider

import android.content.Context
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FileProviderPathsTest {
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun profileImagePath_isShareable() {
        val pictures = requireNotNull(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES))
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            File(pictures, "my_images/profile.jpg")
        )

        assertEquals("content", uri.scheme)
        assertEquals("${context.packageName}.fileprovider", uri.authority)
        assertEquals("/profile_images/profile.jpg", uri.path)
    }

    @Test
    fun privateFile_isRejected() {
        assertRejected(File(context.filesDir, "private.jpg"))
    }

    @Test
    fun siblingPictureDirectory_isRejected() {
        val pictures = requireNotNull(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES))
        assertRejected(File(pictures, "other_images/private.jpg"))
    }

    @Test
    fun traversalOutsideProfileImages_isRejected() {
        val pictures = requireNotNull(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES))
        assertRejected(File(pictures, "my_images/../private.jpg"))
    }

    private fun assertRejected(file: File) {
        assertThrows(IllegalArgumentException::class.java) {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }
    }
}
