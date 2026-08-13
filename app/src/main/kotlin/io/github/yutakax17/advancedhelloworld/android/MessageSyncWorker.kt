package io.github.yutakax17.advancedhelloworld.android

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import io.github.yutakax17.advancedhelloworld.core.SyncResult
import java.util.concurrent.TimeUnit

internal class MessageSyncWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        val application = applicationContext as AdvancedHelloWorldApplication
        return application.messagesSyncContributor.synchronize().toWorkerResult()
    }
}

internal object MessageSyncScheduler {
    const val UNIQUE_WORK_NAME: String = "message-backend-synchronization"

    fun enqueue(context: Context) {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            createRequest(),
        )
    }

    fun createRequest(): PeriodicWorkRequest {
        val requestBuilder =
            PeriodicWorkRequestBuilder<MessageSyncWorker>(
                SYNC_INTERVAL_HOURS,
                TimeUnit.HOURS,
            )
        return requestBuilder.setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build(),
        ).setBackoffCriteria(
            BackoffPolicy.EXPONENTIAL,
            WorkRequest.MIN_BACKOFF_MILLIS,
            TimeUnit.MILLISECONDS,
        ).build()
    }

    private const val SYNC_INTERVAL_HOURS: Long = 6
}

internal fun SyncResult.toWorkerResult(): ListenableWorker.Result = when (this) {
    SyncResult.Success -> ListenableWorker.Result.success()
    is SyncResult.Retry -> ListenableWorker.Result.retry()
    is SyncResult.PermanentFailure -> ListenableWorker.Result.failure()
}
