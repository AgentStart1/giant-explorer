package com.storyteller_f.giant_explorer.control

import android.view.ContextThemeWrapper
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.platform.app.InstrumentationRegistry
import com.storyteller_f.giant_explorer.R
import com.storyteller_f.giant_explorer.view.PathMan
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class PathManNavigationTest(
    private val initialPath: String,
    private val currentPath: String,
    private val clickedIndex: Int,
    private val expectedPath: String,
    private val expectedLabels: List<String>,
) {
    @Test
    fun ancestorClickUsesTheCurrentDirectoryAfterPathChanges() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val context = ContextThemeWrapper(instrumentation.targetContext, R.style.Theme_Giant_Explorer)
            val view = PathMan(context)
            view.drawPath(initialPath)
            view.drawPath(currentPath)
            var selected: String? = null
            view.pathChangeListener = PathMan.PathChangeListener { selected = it }
            val cells = view.getChildAt(0) as ViewGroup
            cells.getChildAt(clickedIndex).performClick()
            assertEquals(expectedPath, selected)
            assertEquals(expectedLabels, (0 until cells.childCount).map { (cells.getChildAt(it) as TextView).text.toString() })
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0} -> {1}")
        fun paths(): List<Array<Any>> = listOf(
            arrayOf("/a/b/c", "/a/b", 2, "/a/b", listOf("/", "a", "b")),
            arrayOf("/a/b", "/x/y/z", 2, "/x/y", listOf("/", "x", "y")),
            arrayOf("/a/b", "/", 0, "/", listOf("/")),
            arrayOf("/", "/资料/子目录", 1, "/资料", listOf("/", "资料")),
            arrayOf("/a/b", "/a/b", 1, "/a", listOf("/", "a")),
        )
    }
}
