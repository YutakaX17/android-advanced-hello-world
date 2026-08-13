package io.github.yutakax17.advancedhelloworld.android

import androidx.work.BackoffPolicy
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.WorkRequest
import io.github.yutakax17.advancedhelloworld.core.SyncResult
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.TimeUnit

class MessageSyncWorkerTest {
    @Test
    fun requestRequiresNetworkAndUsesExponentialBackoff() {
        val request = MessageSyncScheduler.createRequest()

        assertEquals(NetworkType.CONNECTED, request.workSpec.constraints.requiredNetworkType)
        assertEquals(BackoffPolicy.EXPONENTIAL, request.workSpec.backoffPolicy)
        assertEquals(WorkRequest.MIN_BACKOFF_MILLIS, request.workSpec.backoffDelayDuration)
        assertEquals(TimeUnit.HOURS.toMillis(6), request.workSpec.intervalDuration)
    }

    @Test
    fun retryableSynchronizationRequestsSchedulerRetry() {
        assertEquals(
            ListenableWorker.Result.retry().javaClass,
            SyncResult.Retry("offline").toWorkerResult().javaClass,
        )
    }

    @Test
    fun terminalSynchronizationResultsDoNotRetry() {
        assertEquals(
            ListenableWorker.Result.success().javaClass,
            SyncResult.Success.toWorkerResult().javaClass,
        )
        assertEquals(
            ListenableWorker.Result.failure().javaClass,
            SyncResult.PermanentFailure("invalid").toWorkerResult().javaClass,
        )
    }
}
