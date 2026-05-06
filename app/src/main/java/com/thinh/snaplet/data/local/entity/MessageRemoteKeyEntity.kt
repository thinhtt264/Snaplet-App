package com.thinh.snaplet.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "message_remote_keys")
data class MessageRemoteKeyEntity(
    @PrimaryKey val conversationId: String,
    val nextCursor: String?,
)
