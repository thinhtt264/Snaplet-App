package com.thinh.snaplet.data.repository.chat

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.thinh.snaplet.data.datasource.remote.ApiService
import com.thinh.snaplet.data.local.db.AppDatabase
import com.thinh.snaplet.data.local.entity.MessageEntity
import com.thinh.snaplet.data.local.entity.MessageRemoteKeyEntity
import com.thinh.snaplet.data.model.chat.toEntity

@OptIn(ExperimentalPagingApi::class)
class MessageRemoteMediator(
    private val convId: String,
    private val apiService: ApiService,
    private val db: AppDatabase,
) : RemoteMediator<Int, MessageEntity>() {

    override suspend fun initialize(): InitializeAction = InitializeAction.SKIP_INITIAL_REFRESH

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, MessageEntity>,
    ): MediatorResult {
        val cursor: String? = when (loadType) {
            LoadType.REFRESH -> null
            // New messages arrive via socket → Room, not via mediator
            LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
            LoadType.APPEND -> {
                val key = db.messageRemoteKeyDao().getByConvId(convId)
                    ?: return MediatorResult.Success(endOfPaginationReached = true)
                key.nextCursor
                    ?: return MediatorResult.Success(endOfPaginationReached = true)
            }
        }

        return try {
            val response = apiService.getMessages(
                conversationId = convId,
                limit = state.config.pageSize,
                cursor = cursor,
            )
            if (!response.isSuccessful) {
                return MediatorResult.Error(Exception("HTTP ${response.code()}"))
            }
            val body = response.body()?.data
                ?: return MediatorResult.Error(Exception("Empty response body"))
            val nextCursor = body.pagination.nextCursor

            db.withTransaction {
                db.messageDao().upsertAll(body.data.map { it.toEntity() })
                db.messageRemoteKeyDao().upsert(
                    MessageRemoteKeyEntity(
                        conversationId = convId,
                        nextCursor = nextCursor,
                    )
                )
            }

            MediatorResult.Success(endOfPaginationReached = nextCursor == null)
        } catch (e: Exception) {
            MediatorResult.Error(e)
        }
    }
}
