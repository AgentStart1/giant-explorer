package com.storyteller_f.giant_explorer.control

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.NavHostFragment
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.storyteller_f.giant_explorer.R
import com.storyteller_f.ui_list.core.AbstractViewHolder
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.hamcrest.Matchers.allOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@RunWith(Parameterized::class)
class FileListNavigationLifecycleTest(private val grid: Boolean) {
    @Test
    fun rowsReleaseTheirLifecycleWhenDirectoryViewIsDestroyed() = runBlocking {
        withDirectory { scenario ->
            lateinit var holder: AbstractViewHolder<*>
            scenario.onActivity { activity ->
                holder = recycler(activity).findViewHolderForAdapterPosition(1) as AbstractViewHolder<*>
            }
            val lifecycle = checkNotNull(holder.holderLifecycleOwnerOrNull).lifecycle
            onView(allOf(withId(R.id.fileName), withText("folder-000"), isDisplayed())).perform(click())
            awaitName(scenario, "nested.txt")
            withTimeout(10_000) {
                while (true) {
                    var destroyed = false
                    scenario.onActivity { destroyed = lifecycle.currentState == Lifecycle.State.DESTROYED }
                    if (destroyed) break
                    delay(25)
                }
            }
            scenario.onActivity { assertNull(holder.holderLifecycleOwnerOrNull) }
        }
    }

    @Test
    fun returningFromChildRestoresTheScrolledRowAndOffset() = runBlocking {
        withDirectory { scenario ->
            scenario.onActivity { recycler(it).scrollToPosition(21) }
            awaitName(scenario, "folder-020")
            var position = 0
            var offset = 0
            scenario.onActivity { activity ->
                val manager = recycler(activity).layoutManager as LinearLayoutManager
                position = manager.findFirstVisibleItemPosition()
                offset = checkNotNull(manager.findViewByPosition(position)).top
            }
            onView(allOf(withId(R.id.fileName), withText("folder-020"), isDisplayed())).perform(click())
            awaitName(scenario, "nested.txt")
            pressBack()
            awaitName(scenario, "folder-020")
            scenario.onActivity { activity ->
                val manager = recycler(activity).layoutManager as LinearLayoutManager
                assertEquals(position, manager.findFirstVisibleItemPosition())
                val row = manager.findViewByPosition(position)
                assertNotNull(row)
                assertEquals(offset, checkNotNull(row).top)
            }
        }
    }

    @Test
    fun returningFromChildDoesNotRepublishUnchangedPages() = runBlocking {
        withDirectory { scenario ->
            lateinit var parentAdapter: PagingDataAdapter<*, *>
            lateinit var parentPage: FileListFragment
            var pageUpdates = 0
            val listener = { pageUpdates += 1 }
            scenario.onActivity {
                parentPage = page(it)
                parentAdapter = recycler(it).adapter as PagingDataAdapter<*, *>
                parentAdapter.addOnPagesUpdatedListener(listener)
            }
            try {
                onView(allOf(withId(R.id.fileName), withText("folder-000"), isDisplayed())).perform(click())
                awaitName(scenario, "nested.txt")
                awaitViewDestroyed(scenario, parentPage)
                pressBack()
                awaitName(scenario, "folder-000")
                scenario.onActivity {
                    assertEquals("Returning with unchanged query must not republish pages", 0, pageUpdates)
                }
            } finally {
                scenario.onActivity { parentAdapter.removeOnPagesUpdatedListener(listener) }
            }
        }
    }

    @Test
    fun sortChangeUpdatesTheParentWhileItsViewIsAbsent() = runBlocking {
        withDirectory { scenario ->
            lateinit var parentAdapter: PagingDataAdapter<FileItemHolder, *>
            lateinit var parentPage: FileListFragment
            lateinit var originalSort: SortFilterConfig
            scenario.onActivity {
                @Suppress("UNCHECKED_CAST")
                val adapter = recycler(it).adapter as PagingDataAdapter<FileItemHolder, *>
                parentAdapter = adapter
                parentPage = page(it)
                originalSort = checkNotNull(currentSortFilterConfig.value)
            }
            try {
                onView(allOf(withId(R.id.fileName), withText("folder-000"), isDisplayed())).perform(click())
                awaitName(scenario, "nested.txt")
                awaitViewDestroyed(scenario, parentPage)
                scenario.onActivity {
                    currentSortFilterConfig.value = originalSort.copy(sortType = SortType.NAME, sortDirection = SortDirection.DESCENDING)
                }
                withTimeout(10_000) {
                    while (true) {
                        var updated = false
                        scenario.onActivity {
                            updated = parentAdapter.snapshot().items.getOrNull(1)?.file?.name == "folder-059"
                        }
                        if (updated) break
                        delay(25)
                    }
                }
                pressBack()
                awaitName(scenario, "folder-059")
            } finally {
                scenario.onActivity { currentSortFilterConfig.value = originalSort }
            }
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun cancellingEdgeBackKeepsTheChildDirectoryAndPath() = runBlocking {
        withDirectory { scenario ->
            lateinit var parent: FileListFragment
            scenario.onActivity { parent = page(it) }
            onView(allOf(withId(R.id.fileName), withText("folder-000"), isDisplayed())).perform(click())
            awaitName(scenario, "nested.txt")
            awaitViewDestroyed(scenario, parent)
            var pathBefore = ""
            lateinit var childUri: Uri
            var y = 0f
            scenario.onActivity {
                pathBefore = it.findViewById<TextView>(R.id.path_edit).text.toString()
                childUri = FileListFragmentArgs.fromBundle(page(it).requireArguments()).uri
                y = it.resources.displayMetrics.heightPixels / 2f
            }
            val downTime = SystemClock.uptimeMillis()
            injectTouch(MotionEvent.ACTION_DOWN, 5f, y, downTime)
            try {
                delay(50)
                injectTouch(MotionEvent.ACTION_MOVE, 180f, y, downTime)
                withTimeout(5_000) {
                    while (true) {
                        var prepared = false
                        scenario.onActivity { prepared = parent.view != null }
                        if (prepared) break
                        delay(25)
                    }
                }
                delay(100)
            } finally {
                injectTouch(MotionEvent.ACTION_CANCEL, 180f, y, downTime)
            }
            withTimeout(10_000) {
                while (true) {
                    var resumed = false
                    scenario.onActivity {
                        val current = page(it)
                        resumed = FileListFragmentArgs.fromBundle(current.requireArguments()).uri == childUri &&
                            current.lifecycle.currentState == Lifecycle.State.RESUMED
                    }
                    if (resumed) break
                    delay(25)
                }
            }
            awaitName(scenario, "nested.txt")
            scenario.onActivity {
                assertEquals(pathBefore, it.findViewById<TextView>(R.id.path_edit).text.toString())
            }
        }
    }

    private fun injectTouch(action: Int, x: Float, y: Float, downTime: Long) {
        val event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action, x, y, 0).apply {
            source = InputDevice.SOURCE_TOUCHSCREEN
        }
        try {
            assertTrue(InstrumentationRegistry.getInstrumentation().uiAutomation.injectInputEvent(event, true))
        } finally {
            event.recycle()
        }
    }

    private suspend fun awaitViewDestroyed(scenario: ActivityScenario<MainActivity>, parent: FileListFragment) {
        withTimeout(10_000) {
            while (true) {
                var destroyed = false
                scenario.onActivity { destroyed = parent.view == null }
                if (destroyed) break
                delay(25)
            }
        }
    }

    private suspend fun withDirectory(block: suspend (ActivityScenario<MainActivity>) -> Unit) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(context.cacheDir, "navigation-lifecycle-$grid").apply { mkdirs() }
        repeat(60) { index ->
            File(root, "folder-%03d".format(index)).apply { mkdirs() }
                .resolve("nested.txt").writeText("nested")
        }
        val intent = Intent(context, MainActivity::class.java)
            .putExtra("start", FileListFragmentArgs(root.toUri()).toBundle())
        try {
            ActivityScenario.launch<MainActivity>(intent).use { scenario ->
                awaitName(scenario, "folder-000")
                scenario.onActivity { activity ->
                    val switch = activity.findViewById<View>(R.id.switchDisplay)
                    if (switch.isActivated != grid) switch.performClick()
                }
                awaitName(scenario, "folder-000")
                block(scenario)
            }
        } finally {
            root.deleteRecursively()
        }
    }

    private suspend fun awaitName(scenario: ActivityScenario<MainActivity>, name: String) {
        withTimeout(10_000) {
            while (true) {
                var found = false
                scenario.onActivity { activity ->
                    val list = recycler(activity)
                    found = !list.isLayoutRequested && !list.isAnimating &&
                        !list.hasPendingAdapterUpdates() && list.descendants().filterIsInstance<TextView>().any {
                        it.id == R.id.fileName && it.text.toString() == name && it.isShown
                    }
                }
                if (found) return@withTimeout
                delay(25)
            }
        }
    }

    private fun page(activity: MainActivity): FileListFragment {
        val host = activity.supportFragmentManager.findFragmentById(R.id.nav_host_fragment_main) as NavHostFragment
        return host.childFragmentManager.primaryNavigationFragment as FileListFragment
    }

    private fun recycler(activity: MainActivity): RecyclerView = page(activity).requireView().findViewById(R.id.list)

    private fun View.descendants(): Sequence<View> = sequence {
        yield(this@descendants)
        if (this@descendants is ViewGroup) {
            for (index in 0 until childCount) yieldAll(getChildAt(index).descendants())
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "grid={0}")
        fun modes() = listOf(arrayOf(false), arrayOf(true))
    }
}
