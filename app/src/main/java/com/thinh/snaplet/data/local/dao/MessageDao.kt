package com.thinh.snaplet.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.thinh.snaplet.data.local.entity.MessageEntity
import com.thinh.snaplet.data.model.chat.MessageMediaStatus
import com.thinh.snaplet.data.model.chat.MessageReaction
import java.util.Date

@Dao
interface MessageDao {

    @Query("SELECT * FROM messages WHERE conversationId = :convId AND isDeleted = 0 ORDER BY createdAt DESC")
    fun pagingSource(convId: String): PagingSource<Int, MessageEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(message: MessageEntity)

    @Upsert
    suspend fun upsertAll(messages: List<MessageEntity>)

    @Upsert
    suspend fun upsert(message: MessageEntity)

    @Query("UPDATE messages SET status = :status, id = :serverId, createdAt = :serverCreatedAt, serverCreatedAt = :serverCreatedAt WHERE localId = :localId")
    suspend fun updateStatusAfterSend(localId: String, serverId: String, status: String, serverCreatedAt: Date)

    @Query("UPDATE messages SET status = :status WHERE localId = :localId")
    suspend fun updateStatus(localId: String, status: String)

    @Query("UPDATE messages SET mediaUrl = :mediaUrl, mediaLocalUri = null, status = :status WHERE localId = :localId")
    suspend fun updateAfterUpload(localId: String, mediaUrl: String, status: String)

    @Query("SELECT * FROM messages WHERE status IN ('PENDING', 'UPLOADING', 'FAILED') ORDER BY createdAt ASC")
    suspend fun getAllPending(): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE conversationId = :convId AND status IN ('PENDING', 'UPLOADING', 'FAILED') ORDER BY createdAt ASC")
    suspend fun getPendingByConvId(convId: String): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE localId = :localId LIMIT 1")
    suspend fun getByLocalId(localId: String): MessageEntity?

    @Query(
        """
        INSERT INTO messages(
            id,
            conversationId,
            senderId,
            type,
            text,
            mediaUrl,
            mediaLocalUri,
            mediaType,
            mediaStatus,
            status,
            isDeleted,
            createdAt,
            serverCreatedAt,
            localId,
            mediaWidth,
            mediaHeight,
            reactions
        ) VALUES (
            :id,
            :conversationId,
            :senderId,
            :type,
            :text,
            :mediaUrl,
            :mediaLocalUri,
            :mediaType,
            :mediaStatus,
            :status,
            :isDeleted,
            :createdAt,
            :serverCreatedAt,
            :localId,
            :mediaWidth,
            :mediaHeight,
            :reactions
        )
        ON CONFLICT(localId) DO UPDATE SET
            id = excluded.id,
            conversationId = excluded.conversationId,
            senderId = excluded.senderId,
            type = excluded.type,
            text = excluded.text,
            mediaUrl = excluded.mediaUrl,
            mediaLocalUri = excluded.mediaLocalUri,
            mediaType = excluded.mediaType,
            mediaStatus = excluded.mediaStatus,
            status = excluded.status,
            isDeleted = excluded.isDeleted,
            createdAt = excluded.createdAt,
            serverCreatedAt = excluded.serverCreatedAt,
            mediaWidth = excluded.mediaWidth,
            mediaHeight = excluded.mediaHeight,
            reactions = excluded.reactions
        """
    )
    suspend fun upsertByLocalId(
        id: String,
        conversationId: String,
        senderId: String,
        type: String,
        text: String?,
        mediaUrl: String?,
        mediaLocalUri: String?,
        mediaType: String?,
        mediaStatus: MessageMediaStatus,
        status: String,
        isDeleted: Boolean,
        createdAt: Date,
        serverCreatedAt: Date?,
        localId: String,
        mediaWidth: Int,
        mediaHeight: Int,
        reactions: List<MessageReaction>,
    )

    @Transaction
    suspend fun upsertAllByLocalId(messages: List<MessageEntity>) {
        messages.forEach { message ->
            upsertByLocalId(
                id = message.id,
                conversationId = message.conversationId,
                senderId = message.senderId,
                type = message.type,
                text = message.text,
                mediaUrl = message.mediaUrl,
                mediaLocalUri = message.mediaLocalUri,
                mediaType = message.mediaType,
                mediaStatus = message.mediaStatus,
                status = message.status,
                isDeleted = message.isDeleted,
                createdAt = message.createdAt,
                serverCreatedAt = message.serverCreatedAt,
                localId = message.localId,
                mediaWidth = message.mediaWidth,
                mediaHeight = message.mediaHeight,
                reactions = message.reactions,
            )
        }
    }

    @Query("UPDATE messages SET reactions = :reactions WHERE id = :messageId")
    suspend fun updateReactions(messageId: String, reactions: List<MessageReaction>)
}
