package com.thinh.snaplet.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.thinh.snaplet.data.local.dao.ConversationDao
import com.thinh.snaplet.data.local.dao.MessageDao
import com.thinh.snaplet.data.local.dao.MessageRemoteKeyDao
import com.thinh.snaplet.data.local.entity.ConversationEntity
import com.thinh.snaplet.data.local.entity.MessageEntity
import com.thinh.snaplet.data.local.entity.MessageRemoteKeyEntity

@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
        MessageRemoteKeyEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun messageRemoteKeyDao(): MessageRemoteKeyDao
}
