package com.thinh.snaplet.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.thinh.snaplet.data.local.entity.MessageEntity

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

    @Query("UPDATE messages SET status = :status, id = :serverId, serverCreatedAt = :serverCreatedAt WHERE localId = :localId")
    suspend fun updateStatusAfterSend(localId: String, serverId: String, status: String, serverCreatedAt: Long)

    @Query("UPDATE messages SET status = :status WHERE localId = :localId")
    suspend fun updateStatus(localId: String, status: String)

    @Query("UPDATE messages SET mediaUrl = :mediaUrl, mediaLocalUri = null, status = :status WHERE localId = :localId")
    suspend fun updateAfterUpload(localId: String, mediaUrl: String, status: String)

    @Query("SELECT * FROM messages WHERE status IN ('PENDING', 'UPLOADING', 'FAILED') ORDER BY createdAt ASC")
    suspend fun getAllPending(): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE localId = :localId LIMIT 1")
    suspend fun getByLocalId(localId: String): MessageEntity?

    // Deletes any PENDING row with the given localId — used in onIncomingMessage to prevent
    // PK conflict when socket fires before the HTTP response updates the optimistic row's id.
    @Query("DELETE FROM messages WHERE localId = :localId AND status = 'PENDING'")
    suspend fun deletePendingByLocalId(localId: String)
}
