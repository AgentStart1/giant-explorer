package com.storyteller_f.common_ui

import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.storyteller_f.giant_explorer.R
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

fun <T : View> T.pp(block: (T) -> Unit) = post { block(this) }

fun <T : View> T.setVisible(visible: Boolean, block: (T) -> Unit = {}) {
    isVisible = visible
    if (visible) block(this)
}

inline fun <T : View, reified V> T.setVisible(obj: Any, visible: (V) -> Boolean, block: (T, V) -> Unit) {
    val value = obj as? V
    isVisible = value != null && visible(value)
    if (isVisible && value != null) block(this, value)
}

fun List<View>.onVisible(view: View) = forEach { it.isVisible = it === view }

fun LifecycleOwner.repeatOnViewResumed(block: suspend CoroutineScope.() -> Unit) = scope.launch {
    cycle.repeatOnLifecycle(Lifecycle.State.RESUMED, block)
}

class WaitingDialog : DialogFragment(R.layout.dialog_waiting) {
    lateinit var deferred: CompletableDeferred<Unit>

    override fun onStart() {
        super.onStart()
        scope.launch {
            deferred.await()
            dismissAllowingStateLoss()
        }
    }
}

fun LifecycleOwner.waitingDialog(): CompletableDeferred<Unit> {
    val completion = CompletableDeferred<Unit>()
    WaitingDialog().also { it.deferred = completion }.show(fm, "waiting")
    return completion
}

fun LifecycleOwner.waitingDialog(block: suspend () -> Unit) {
    scope.launch {
        val waiting = waitingDialog()
        try {
            block()
        } catch (error: Exception) {
            Toast.makeText(ctx, error.localizedMessage ?: error.javaClass.simpleName, Toast.LENGTH_SHORT).show()
            Log.e("WaitingDialog", "Operation failed", error)
        } finally {
            waiting.end()
        }
    }
}

fun CompletableDeferred<Unit>.end() {
    complete(Unit)
}
