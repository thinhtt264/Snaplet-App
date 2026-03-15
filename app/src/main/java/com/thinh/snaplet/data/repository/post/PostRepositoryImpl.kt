package com.thinh.snaplet.data.repository.post

import com.thinh.snaplet.data.model.post.NewPostUpdate
import com.thinh.snaplet.platform.socket.SocketEvent
import com.thinh.snaplet.platform.socket.SocketManager
import com.thinh.snaplet.utils.Logger
import com.thinh.snaplet.utils.network.GsonHolder.gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapNotNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostRepositoryImpl @Inject constructor(
    private val socketManager: SocketManager
) : PostRepository {

    override val newPostMessages: Flow<NewPostUpdate> =
        socketManager.messages
            .filter { it.event == SocketEvent.NEW_POST }
            .mapNotNull { message ->
                val raw = message.args.firstOrNull() ?: return@mapNotNull null
                val jsonString = when (raw) {
                    is String -> raw
                    else -> raw.toString()
                }
                runCatching {
                    gson.fromJson(jsonString, NewPostUpdate::class.java)
                }.onFailure {
                    Logger.e("PostRepository: parse new_post failed: ${it.message}")
                }.getOrNull()
            }
}
