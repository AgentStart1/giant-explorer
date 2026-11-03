package com.storyteller_f.giant_explorer.control

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.net.toUri
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.fragment.NavHostFragment
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.storyteller_f.giant_explorer.R
import com.storyteller_f.giant_explorer.dialog.NewNameDialog
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class FileListMenuTest {
    @Test
    fun directoryTransitionKeepsOneFileMenuContribution() = runBlocking {
        withDirectory { scenario, child ->
            val counts = CompletableDeferred<Pair<Int, Int>>()
            lateinit var callbacks: FragmentManager.FragmentLifecycleCallbacks
            scenario.onActivity { activity ->
                callbacks = object : FragmentManager.FragmentLifecycleCallbacks() {
                    override fun onFragmentViewCreated(fm: FragmentManager, f: Fragment, v: View, state: Bundle?) {
                        if (f is FileListFragment && FileListFragmentArgs.fromBundle(f.requireArguments()).uri == child.toUri()) {
                            v.doOnPreDraw {
                                val menu = activity.findViewById<Toolbar>(R.id.toolbar).menu
                                counts.complete(menu.countItems(R.id.add_file) to menu.countItems(R.id.paste_file))
                            }
                        }
                    }
                }
                activity.supportFragmentManager.registerFragmentLifecycleCallbacks(callbacks, true)
                host(activity).navController.navigate(
                    R.id.action_fileListFragment_self,
                    FileListFragmentArgs(child.toUri()).toBundle()
                )
            }
            try {
                val (addCount, pasteCount) = withTimeout(10_000) { counts.await() }
                assertEquals(1, addCount)
                assertEquals(1, pasteCount)
            } finally {
                scenario.onActivity { it.supportFragmentManager.unregisterFragmentLifecycleCallbacks(callbacks) }
            }
        }
    }

    @Test
    fun addFileActionTargetsTheVisibleChildDirectory() = runBlocking {
        withDirectory { scenario, child ->
            val created = child.resolve("created.txt")
            created.delete()
            scenario.onActivity { activity ->
                host(activity).navController.navigate(
                    R.id.action_fileListFragment_self,
                    FileListFragmentArgs(child.toUri()).toBundle()
                )
            }
            withTimeout(10_000) {
                while (true) {
                    var ready = false
                    scenario.onActivity { activity ->
                        ready = host(activity).childFragmentManager.primaryNavigationFragment?.view
                            ?.descendants()?.filterIsInstance<TextView>()
                            ?.any { it.id == R.id.fileName && it.text.toString() == "nested.txt" && it.isShown } == true
                    }
                    if (ready) break
                    delay(25)
                }
            }
            scenario.onActivity { activity ->
                assertTrue(activity.findViewById<Toolbar>(R.id.toolbar).menu.performIdentifierAction(R.id.add_file, 0))
            }
            withTimeout(10_000) {
                while (true) {
                    var showing = false
                    scenario.onActivity { activity ->
                        showing = host(activity).childFragmentManager.fragments.filterIsInstance<NewNameDialog>()
                            .any { it.dialog?.findViewById<View>(R.id.newName)?.isShown == true }
                    }
                    if (showing) break
                    delay(25)
                }
            }
            scenario.onActivity { activity ->
                val dialog = host(activity).childFragmentManager.fragments.filterIsInstance<NewNameDialog>()
                    .single { it.dialog?.findViewById<View>(R.id.newName)?.isShown == true }
                    .requireDialog()
                dialog.findViewById<TextView>(R.id.newName).text = "created.txt"
                assertTrue(dialog.findViewById<View>(R.id.positive).performClick())
            }
            withTimeout(10_000) {
                while (!created.exists()) delay(25)
            }
            assertTrue(created.isFile)
            assertFalse(child.parentFile!!.resolve("created.txt").exists())
        }
    }

    private suspend fun withDirectory(block: suspend (ActivityScenario<MainActivity>, File) -> Unit) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(context.cacheDir, "file-menu-test").apply { mkdirs() }
        val child = root.resolve("folder").apply { mkdirs() }
        child.resolve("nested.txt").writeText("nested")
        root.resolve("created.txt").delete()
        val intent = Intent(context, MainActivity::class.java)
            .putExtra("start", FileListFragmentArgs(root.toUri()).toBundle())
        try {
            ActivityScenario.launch<MainActivity>(intent).use { block(it, child) }
        } finally {
            root.deleteRecursively()
        }
    }

    private fun host(activity: MainActivity) =
        activity.supportFragmentManager.findFragmentById(R.id.nav_host_fragment_main) as NavHostFragment

    private fun Menu.countItems(id: Int): Int = (0 until size()).sumOf { index ->
        val item = getItem(index)
        (if (item.itemId == id) 1 else 0) + (item.subMenu?.countItems(id) ?: 0)
    }

    private fun View.descendants(): Sequence<View> = sequence {
        yield(this@descendants)
        if (this@descendants is ViewGroup) {
            for (index in 0 until childCount) yieldAll(getChildAt(index).descendants())
        }
    }
}
