package com.thinh.snaplet.di

import com.thinh.snaplet.data.repository.FakeUserRepository
import com.thinh.snaplet.data.repository.MediaRepository
import com.thinh.snaplet.data.repository.MediaRepositoryImpl
import com.thinh.snaplet.data.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /**
     * PRODUCTION MODE: Using MediaRepositoryImpl (real API)
     */
    @Binds
    @Singleton
    abstract fun bindMediaRepository(
        realRepository: MediaRepositoryImpl  // ← Now using REAL API
    ): MediaRepository
    
    /**
     * DEVELOPMENT MODE: Switch back to fake data if needed
     * Uncomment this and comment out the above:
     */

    
    /**
     * DEVELOPMENT MODE: Using FakeUserRepository (mock data)
     * TODO: Replace with real API implementation when ready
     */
    @Binds
    @Singleton
    abstract fun bindUserRepository(
        fakeRepository: FakeUserRepository
    ): UserRepository
}
