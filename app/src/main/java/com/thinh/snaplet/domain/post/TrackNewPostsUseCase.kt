package com.thinh.snaplet.domain.post

import com.thinh.snaplet.data.model.post.SseEvent
import com.thinh.snaplet.data.repository.post.PostRepository
import com.thinh.snaplet.domain.model.PostUpdateEvent
import com.thinh.snaplet.utils.Logger
import com.thinh.snaplet.utils.network.GsonHolder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import javax.inject.Inject

class TrackNewPostsUseCase @Inject constructor(
    private val postRepository: PostRepository,
) {

    operator fun invoke(): Flow<PostUpdateEvent> {
        return postRepository.observePostStream()
            .mapNotNull { event ->
                when (event) {
                    is SseEvent.Message -> {
                        if (event.type != "posts_update") {
                            return@mapNotNull null
                        }

                        runCatching {
                            GsonHolder.gson.fromJson(
                                event.data,
                                PostUpdateEvent::class.java,
                            )
                        }.onFailure { throwable ->
                            Logger.e(throwable, "⚠️ Failed to parse SSE posts_update payload: ${event.data}")
                        }.getOrNull()
                    }

                    is SseEvent.Error -> {
                        Logger.e(event.throwable, "⚠️ SSE Error while tracking new posts")
                        null
                    }

                    SseEvent.Closed -> {
                        Logger.d("🛑 SSE Closed while tracking new posts")
                        null
                    }
                }
            }
    }
}
