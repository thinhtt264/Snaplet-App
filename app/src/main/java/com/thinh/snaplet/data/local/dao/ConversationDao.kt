package com.thinh.snaplet.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.thinh.snaplet.data.local.entity.ConversationEntity
import com.thinh.snaplet.data.local.entity.ConversationLastMessageStatusProjection
import com.thinh.snaplet.data.local.entity.ConversationUpdatedAtProjection
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {

    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ConversationEntity>>

    @Query(
        """
        SELECT c.id AS conversationId, m.status AS status
        FROM conversations c
        LEFT JOIN messages m
            ON m.id = c.lastMessageId
    """
    )
    fun observeLastMessageStatuses(): Flow<List<ConversationLastMessageStatusProjection>>

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ConversationEntity?

    @Query("SELECT id, updatedAt, myLastSeenAt, partnerLastSeenAt FROM conversations")
    suspend fun getAllUpdatedAtSnapshot(): List<ConversationUpdatedAtProjection>

    @Upsert
    suspend fun upsertAll(conversations: List<ConversationEntity>)

    @Upsert
    suspend fun upsert(conversation: ConversationEntity)

    @Query(
        """
        UPDATE conversations
        SET myLastSeenAt = CASE
            WHEN COALESCE(myLastSeenAt, 0) >= :seenAt THEN myLastSeenAt
            ELSE :seenAt
        END
        WHERE id = :id
        """
    )
    suspend fun updateMyLastSeenAt(id: String, seenAt: Long)

    // Backend có thể bắn read receipts cho cả room.
    // `participantId` trong conversations đang lưu id của partner, vì vậy:
    // - nếu readerId == participantId  -> partner vừa đọc -> update `partnerLastSeenAt`
    // - ngược lại -> user hiện tại vừa đọc -> update `myLastSeenAt`
    @Query(
        """
        UPDATE conversations
        SET
            myLastSeenAt = CASE
                WHEN :readerId != participantId THEN
                    CASE
                        WHEN COALESCE(myLastSeenAt, 0) >= :seenAt THEN myLastSeenAt
                        ELSE :seenAt
                    END
                ELSE myLastSeenAt
            END,
            partnerLastSeenAt = CASE
                WHEN :readerId = participantId THEN
                    CASE
                        WHEN COALESCE(partnerLastSeenAt, 0) >= :seenAt THEN partnerLastSeenAt
                        ELSE :seenAt
                    END
                ELSE partnerLastSeenAt
            END
        WHERE id = :id
        """
    )
    suspend fun updatePartnerLastSeenAt(id: String, readerId: String, seenAt: Long)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM conversations WHERE participantId = :participantId AND id != :keepId")
    suspend fun deleteByParticipantIdExcept(participantId: String, keepId: String)

    @Query(
        """
        UPDATE conversations
        SET lastMessageId = COALESCE(:lastMessageId, lastMessageId),
            lastMessageAt = :lastMessageAt,
            lastMessageSenderId = :lastMessageSenderId,
            lastMessageText = :lastMessageText,
            lastMessageType = :lastMessageType,
            isLastMessageDeleted = 0
        WHERE id = :convId
    """
    )
    suspend fun updateLastMessage(
        convId: String,
        lastMessageId: String?,
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
