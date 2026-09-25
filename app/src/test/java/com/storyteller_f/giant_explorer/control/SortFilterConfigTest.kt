package com.storyteller_f.giant_explorer.control

import android.net.TestUri
import com.storyteller_f.file_system.instance.FileKind
import com.storyteller_f.file_system.instance.FilePermissions
import com.storyteller_f.file_system.instance.FileTime
import com.storyteller_f.file_system.model.FileInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class SortFilterConfigTest {
    @Test
    fun sortByNameAscending() {
        val files = listOf(
            file("zeta.txt"),
            file("alpha.txt"),
            file("middle.txt")
        )

        val sorted = SortFilterConfig(sortType = SortType.NAME).sort(files)

        assertEquals(listOf("alpha.txt", "middle.txt", "zeta.txt"), sorted.map { it.name })
    }

    @Test
    fun sortBySizeDescendingTreatsDirectoriesAsZeroSize() {
        val files = listOf(
            file("small.txt", size = 10),
            directory("folder"),
            file("large.txt", size = 200)
        )

        val sorted = SortFilterConfig(
            sortType = SortType.SIZE,
            sortDirection = SortDirection.DESCENDING
        ).sort(files)

        assertEquals(listOf("large.txt", "small.txt", "folder"), sorted.map { it.name })
    }

    @Test
    fun applyFiltersDotFilesBeforeSorting() {
        val files = listOf(
            file("visible-b.txt"),
            file(".hidden-a.txt"),
            file("visible-a.txt")
        )

        val result = SortFilterConfig(filterHidden = true).apply(files)

        assertEquals(listOf("visible-a.txt", "visible-b.txt"), result.map { it.name })
    }

    @Test
    fun sortByTimeUsesZeroForMissingModifiedTime() {
        val files = listOf(
            file("new.txt", lastModified = 200),
            file("unknown.txt", lastModified = null),
            file("old.txt", lastModified = 100)
        )

        val sorted = SortFilterConfig(sortType = SortType.TIME).sort(files)

        assertEquals(listOf("unknown.txt", "old.txt", "new.txt"), sorted.map { it.name })
    }

    private fun file(
        name: String,
        size: Long = 0,
        lastModified: Long? = null,
        hidden: Boolean = name.startsWith(".")
    ) = FileInfo(
        name,
        TestUri("/test/$name"),
        FileTime(lastModified = lastModified),
        FileKind.File(null, hidden, size, name.substringAfterLast('.', "")),
        FilePermissions.USER_READABLE
    )

    private fun directory(
        name: String,
        lastModified: Long? = null,
        hidden: Boolean = name.startsWith(".")
    ) = FileInfo(
        name,
        TestUri("/test/$name"),
        FileTime(lastModified = lastModified),
        FileKind.Directory(null, hidden),
        FilePermissions.USER_READABLE
    )
}
