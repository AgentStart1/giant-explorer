package com.storyteller_f.common_ui

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Serial, UI-independent ownership of concurrent waiting operations. */
internal class WaitingHost(dispatcher: CoroutineDispatcher) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val operations = mutableMapOf<String, CompletableDeferred<Unit>>()

    suspend fun begin(id: String): CompletableDeferred<Unit> = try {
        scope.async {
            check(id !in operations)
            CompletableDeferred<Unit>().also { completion ->
                operations[id] = completion
                completion.invokeOnCompletion {
                    scope.launch { operations.remove(id) }
                }
            }
        }.await()
    } catch (error: CancellationException) {
        scope.launch { operations.remove(id)?.cancel() }
        throw error
    }

    suspend fun await(id: String) {
        // Missing operations have already finished, or were lost with the process.
        val completion = scope.async { operations[id] }.await() ?: return
        completion.join()
    }

    fun close() {
        scope.launch {
            operations.values.toList().forEach { it.cancel() }
            operations.clear()
            scope.cancel()
        }
    }
}
