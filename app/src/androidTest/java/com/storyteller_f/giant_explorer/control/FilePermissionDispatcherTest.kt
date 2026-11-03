package com.storyteller_f.giant_explorer.control

import android.os.Build
import android.os.Environment
import androidx.core.net.toUri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.storyteller_f.file_system_local.permission.checkFilePermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
class FilePermissionDispatcherTest {
    @Test
    fun ioPermissionCheckMatchesThePlatformStoragePermission() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val uri = Environment.getExternalStorageDirectory().toUri()
        val expected = Environment.isExternalStorageManager()

        val granted = withContext(Dispatchers.IO) { context.checkFilePermission(uri) }

        assertEquals(expected, granted)
    }
}
