package com.storyteller_f.giant_explorer

import android.content.ContentResolver
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.storyteller_f.file_system_local.permission.checkFilePermission
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Instrumented tests for file permission checking on Android 16 (API 36) and above.
 *
 * On Android 11+ (API 30+), including Android 16 (API 36), access to external storage
 * requires the MANAGE_EXTERNAL_STORAGE permission, checked via
 * [Environment.isExternalStorageManager].
 */
@RunWith(AndroidJUnit4::class)
class PermissionTest {

    /**
     * Content-scheme URIs do not require file system permissions.
     * [checkFilePermission] should always return true for content:// URIs.
     */
    @Test
    fun testContentUriAlwaysHasPermission() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val contentUri = Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority("com.storyteller_f.giant_explorer")
            .path("/test")
            .build()

        runBlocking {
            assertTrue(
                "Content-scheme URIs should always have permission",
                context.checkFilePermission(contentUri)
            )
        }
    }

    /**
     * On Android 11+ (API 30+), including Android 16 (API 36), access to emulated
     * external storage (/storage/emulated/0/) requires MANAGE_EXTERNAL_STORAGE.
     * [checkFilePermission] must reflect the state of [Environment.isExternalStorageManager].
     */
    @Test
    fun testEmulatedStoragePermissionReflectsManageExternalStorage() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val emulatedStorageUri = Uri.fromFile(File("/storage/emulated/0/test.txt"))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val expectedPermission = Environment.isExternalStorageManager()
            runBlocking {
                assertEquals(
                    "On Android 11+ (API 30+), checkFilePermission for emulated storage " +
                        "should match Environment.isExternalStorageManager()",
                    expectedPermission,
                    context.checkFilePermission(emulatedStorageUri)
                )
            }
        }
    }

    /**
     * The app's own data directory does not require special permissions.
     * [checkFilePermission] should return true for app-private directories.
     */
    @Test
    fun testAppDataDirectoryAlwaysHasPermission() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val appDataUri = Uri.fromFile(context.filesDir)

        runBlocking {
            assertTrue(
                "App's own data directory should always have permission",
                context.checkFilePermission(appDataUri)
            )
        }
    }
}
