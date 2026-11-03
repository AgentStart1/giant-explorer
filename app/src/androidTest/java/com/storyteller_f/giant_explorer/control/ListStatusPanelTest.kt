package com.storyteller_f.giant_explorer.control

import android.content.Intent
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.net.toUri
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.storyteller_f.giant_explorer.R
import com.storyteller_f.ui_list.ui.ListWithState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ListStatusPanelTest {
    @Test
    fun stateTransitionsShowOnlyTheRequestedPanel() = withList { _, list ->
        list.flash(ListWithState.UIState.loading)
        assertTrue(list.findViewById<View>(R.id.progress_bar).isShown)
        assertFalse(list.recyclerView.isShown)

        list.flash(ListWithState.UIState(false, false, true, false, null, false))
        assertTrue(list.findViewById<View>(R.id.emptyList).isShown)
        assertFalse(list.findViewById<View>(R.id.progress_bar).isShown)

        list.flash(ListWithState.UIState(false, true, false, false, null, false))
        assertTrue(list.recyclerView.isShown)
        assertFalse(list.findViewById<View>(R.id.emptyList).isShown)
        assertFalse(list.findViewById<View>(R.id.progress_bar).isShown)
    }

    @Test
    fun retryRemainsConnectedAcrossErrorAndContentStates() = withList { activity, list ->
        var retries = 0
        list.sourceUp(fileListAdapter(), activity, refresh = { retries += 1 })
        val error = ListWithState.UIState(true, false, false, false, "offline", false)
        list.flash(error)
        assertEquals("offline", list.findViewById<TextView>(R.id.error_msg).text.toString())
        assertTrue(list.findViewById<View>(R.id.retry_button).isShown)
        list.findViewById<View>(R.id.retry_button).performClick()
        assertEquals(1, retries)

        list.flash(ListWithState.UIState(false, true, false, false, null, false))
        assertFalse(list.findViewById<View>(R.id.error_page).isShown)
        list.flash(error.copy(error = "retry failed"))
        assertEquals("retry failed", list.findViewById<TextView>(R.id.error_msg).text.toString())
        list.findViewById<View>(R.id.retry_button).performClick()
        assertEquals(2, retries)
    }

    private fun withList(block: (MainActivity, ListWithState) -> Unit) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val root = File(context.cacheDir, "list-status-panel-test").apply { mkdirs() }
        val intent = Intent(context, MainActivity::class.java)
            .putExtra("start", FileListFragmentArgs(root.toUri()).toBundle())
        try {
            ActivityScenario.launch<MainActivity>(intent).use { scenario ->
                scenario.onActivity { activity ->
                    val list = ListWithState(activity)
                    activity.addContentView(
                        list,
                        FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        )
                    )
                    block(activity, list)
                }
            }
        } finally {
            root.deleteRecursively()
        }
    }
}
