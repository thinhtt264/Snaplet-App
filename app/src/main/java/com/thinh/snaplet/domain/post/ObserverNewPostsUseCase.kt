package com.thinh.snaplet.domain.post

import com.thinh.snaplet.data.model.post.SseEvent
import com.thinh.snaplet.data.repository.post.PostRepository
import com.thinh.snaplet.domain.model.PostUpdateEvent
import com.thinh.snaplet.utils.Logger
import com.thinh.snaplet.utils.network.GsonHolder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import javax.inject.Inject

class ObserverNewPostsUseCase @Inject constructor(
    private val postRepository: PostRepository,
) {

    operator fun invoke(): Flow<PostUpdateEvent> {
        return postRepository.observePostStream().mapNotNull { event ->
                when (event) {
                    is SseEvent.Message -> {
                        val (type, data) = event
                        Logger.d("📨 onEvent — type=$type data=$data")

                        if (type != "posts_update") {
                            return@mapNotNull null
                        }

                        runCatching {
                            GsonHolder.gson.fromJson(
                                event.data,
                                PostUpdateEvent::class.java,
                            )
                        }.onFailure { throwable ->
                            Logger.e(
                                throwable,
                                "⚠️ Failed to parse SSE posts_update payload: ${event.data}"
                            )
                        }.getOrNull()
                    }

                    is SseEvent.Opened -> {
                        Logger.d("✅ SSE onOpen — HTTP ${event.response.code}")
                        null
                    }

                    is SseEvent.Error -> {
                        Logger.e(event.throwable, "⚠️ SSE Error while tracking new posts")
                        null
                    }

                    is SseEvent.Closed -> {
                        Logger.d("🛑 SSE Closed while tracking new posts")
                        null
                    }

                    is SseEvent.MaxRetriesExceeded -> return@mapNotNull null
                }
            }
    }
}