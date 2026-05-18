package com.thinh.snaplet.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.thinh.snaplet.data.local.entity.MessageRemoteKeyEntity

@Dao
interface MessageRemoteKeyDao {

    @Query("SELECT * FROM message_remote_keys WHERE conversationId = :convId LIMIT 1")
    suspend fun getByConvId(convId: String): MessageRemoteKeyEntity?

    @Upsert
    suspend fun upsert(key: MessageRemoteKeyEntity)

    @Query("DELETE FROM message_remote_keys WHERE conversationId = :convId")
    suspend fun deleteByConvId(convId: String)

    @Query("DELETE FROM message_remote_keys")
    suspend fun deleteAll()
}
