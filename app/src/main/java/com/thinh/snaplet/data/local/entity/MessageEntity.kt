package com.thinh.snaplet.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.thinh.snaplet.data.model.chat.Message
import java.util.Date

@Entity(
    tableName = "messages",
    foreignKeys = [ForeignKey(
        entity = ConversationEntity::class,
        parentColumns = ["id"],
        childColumns = ["conversationId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("conversationId")]
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val senderId: String,
    val type: String,
    val text: String?,
    val mediaUrl: String?,
    val mediaLocalUri: String?,
    val mediaType: String?,
    val status: String,
    val isDeleted: Boolean,
    val createdAt: Long,
    val serverCreatedAt: Long?,
    // Stable local UUID — use this as the UI key, never `id`
    val localId: String,
)

fun MessageEntity.toMessage(): Message = Message(
    id = id,
    conversationId = conversationId,
    senderId = senderId,
    clientUuid = localId,
    text = text,
    mediaUrls = null,
    mimeType = mediaType,
    isDeleted = isDeleted,
    replyTo = null,
    pinnedAt = null,
    createdAt = Date(createdAt),
    status = status,
)

object MessageStatus {
    const val PENDING = "PENDING"
    const val UPLOADING = "UPLOADING"
    const val SENT = "SENT"
    const val FAILED = "FAILED"
}
