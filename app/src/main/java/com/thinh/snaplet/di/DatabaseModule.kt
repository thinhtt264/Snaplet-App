package com.thinh.snaplet.di

import android.content.Context
import androidx.room.Room
import com.thinh.snaplet.data.local.dao.ConversationDao
import com.thinh.snaplet.data.local.dao.MessageDao
import com.thinh.snaplet.data.local.dao.MessageRemoteKeyDao
import com.thinh.snaplet.data.local.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "snaplet.db",
        )
            .fallbackToDestructiveMigration(true)
            .build()
    }

    @Provides
    @Singleton
    fun provideConversationDao(db: AppDatabase): ConversationDao = db.conversationDao()

    @Provides
    @Singleton
    fun provideMessageDao(db: AppDatabase): MessageDao = db.messageDao()

    @Provides
    @Singleton
    fun provideMessageRemoteKeyDao(db: AppDatabase): MessageRemoteKeyDao = db.messageRemoteKeyDao()
}
