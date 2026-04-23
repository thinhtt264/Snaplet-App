package com.thinh.snaplet.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.thinh.snaplet.data.local.dao.ConversationDao
import com.thinh.snaplet.data.local.entity.ConversationEntity

@Database(
    entities = [ConversationEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
}
