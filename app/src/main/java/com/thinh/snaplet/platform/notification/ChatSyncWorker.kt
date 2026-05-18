package com.thinh.snaplet.platform.notification

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.thinh.snaplet.data.repository.chat.ChatRepository
import com.thinh.snaplet.utils.network.ApiResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class ChatSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val chatRepository: ChatRepository,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val conversationId = inputData.getString(KEY_CONVERSATION_ID) ?: return Result.failure()
        return try {
            when (val result = chatRepository.syncMessages(conversationId)) {
                is ApiResult.Success -> Result.success()
                is ApiResult.Failure ->
                    if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
            }
        } catch (_: Exception) {
            if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val KEY_CONVERSATION_ID = "conversationId"
        private const val MAX_RETRIES = 3

        fun enqueue(context: Context, conversationId: String) {
            val data = workDataOf(KEY_CONVERSATION_ID to conversationId)

            val request = OneTimeWorkRequestBuilder<ChatSyncWorker>()
                .setInputData(data)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS,
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "chat_sync_$conversationId",
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }
    }
}
