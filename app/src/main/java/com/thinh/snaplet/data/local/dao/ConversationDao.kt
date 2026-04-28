package com.thinh.snaplet.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.thinh.snaplet.data.local.entity.ConversationEntity
import com.thinh.snaplet.data.local.entity.ConversationUpdatedAtProjection
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {

    @Query("SELECT * FROM conversations ORDER BY lastMessageAt DESC")
    fun observeAll(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ConversationEntity?

    @Query("SELECT id, updatedAt FROM conversations")
    suspend fun getAllUpdatedAtSnapshot(): List<ConversationUpdatedAtProjection>

    @Upsert
    suspend fun upsertAll(conversations: List<ConversationEntity>)

    @Upsert
    suspend fun upsert(conversation: ConversationEntity)

    @Query("UPDATE conversations SET myLastSeenAt = :seenAt WHERE id = :id")
    suspend fun updateMyLastSeenAt(id: String, seenAt: Long)

    @Query("UPDATE conversations SET partnerLastSeenAt = :seenAt WHERE id = :id")
    suspend fun updatePartnerLastSeenAt(id: String, seenAt: Long)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query(
        """
        UPDATE conversations
        SET lastMessageAt = :lastMessageAt,
            lastMessageSenderId = :lastMessageSenderId,
            lastMessageText = :lastMessageText,
            lastMessageType = :lastMessageType,
            isLastMessageDeleted = 0
        WHERE id = :convId
    """
    )
    suspend fun updateLastMessage(
        convId: String,
        lastMessageAt: Long,
        lastMessageSenderId: String,
        lastMessageText: String?,
        lastMessageType: String?,
    )

    @Query(
        """
    SELECT COUNT(*) FROM conversations 
    WHERE lastMessageSenderId != :myUserId 
    AND lastMessageAt > COALESCE(myLastSeenAt, 0) 
    """
    )
    fun observeUnreadCount(myUserId: String): Flow<Int>

    @Query("DELETE FROM conversations")
    suspend fun deleteAll()
}
