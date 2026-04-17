package com.thinh.snaplet.di

import com.thinh.snaplet.data.repository.auth.AuthRepository
import com.thinh.snaplet.data.repository.auth.AuthRepositoryImpl
import com.thinh.snaplet.data.repository.device.DeviceRepository
import com.thinh.snaplet.data.repository.device.DeviceRepositoryImpl
import com.thinh.snaplet.data.repository.MediaRepository
import com.thinh.snaplet.data.repository.MediaRepositoryImpl
import com.thinh.snaplet.data.repository.UserRepository
import com.thinh.snaplet.data.repository.UserRepositoryImpl
import com.thinh.snaplet.data.repository.chat.ChatRepository
import com.thinh.snaplet.data.repository.chat.ChatRepositoryImpl
import com.thinh.snaplet.data.repository.post.PostRepository
import com.thinh.snaplet.data.repository.post.PostRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMediaRepository(
        repository: MediaRepositoryImpl
    ): MediaRepository
    
    @Binds
    @Singleton
    abstract fun bindUserRepository(
        repository: UserRepositoryImpl
    ): UserRepository
    
    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        repository: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindDeviceRepository(
        repository: DeviceRepositoryImpl
    ): DeviceRepository

    @Binds
    @Singleton
    abstract fun bindPostRepository(impl: PostRepositoryImpl): PostRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository
}
