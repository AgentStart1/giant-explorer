package com.storyteller_f.common_ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WaitingHostTest {
    @Test(timeout = 10000) fun recreatedObserverSharesOperation() = runBlocking {
        val host = WaitingHost(Dispatchers.Default.limitedParallelism(1))
        try {
            val completion = host.begin("operation")
            val oldObserver = async(start = CoroutineStart.UNDISPATCHED) { host.await("operation") }
            oldObserver.cancelAndJoin()
            assertFalse(completion.isCompleted)
            val restoredObserver = async { host.await("operation") }
            completion.complete(Unit)
            restoredObserver.await()
            host.await("operation")
        } finally { host.close() }
    }

    @Test(timeout = 10000) fun missingOperationDoesNotWaitAfterProcessDeath() = runBlocking {
        val host = WaitingHost(Dispatchers.Default.limitedParallelism(1))
        try { host.await("lost-operation") } finally { host.close() }
    }

    @Test(timeout = 10000) fun concurrentOperationsAndCancellationAreIndependent() = runBlocking {
        val host = WaitingHost(Dispatchers.Default.limitedParallelism(1))
        try {
            val first = host.begin("first")
            val second = host.begin("second")
            first.cancel()
            host.await("first")
            assertFalse(second.isCompleted)
            host.close()
            second.join()
            assertTrue(second.isCancelled)
        } finally { host.close() }
    }
}
