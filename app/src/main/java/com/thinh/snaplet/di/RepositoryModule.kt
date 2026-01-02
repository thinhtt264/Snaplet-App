package com.thinh.snaplet.di

import com.thinh.snaplet.data.repository.FakeMediaRepository
import com.thinh.snaplet.data.repository.MediaRepository
import com.thinh.snaplet.data.repository.MediaRepositoryImpl
import com.thinh.snaplet.data.repository.UserRepository
import com.thinh.snaplet.data.repository.UserRepositoryImpl
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
        repository: FakeMediaRepository
    ): MediaRepository
    
    @Binds
    @Singleton
    abstract fun bindUserRepository(
        repository: UserRepositoryImpl
    ): UserRepository
}
