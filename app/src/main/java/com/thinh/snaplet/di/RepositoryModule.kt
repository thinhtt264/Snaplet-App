package com.thinh.snaplet.di

import com.thinh.snaplet.data.repository.FakeMediaRepository
import com.thinh.snaplet.data.repository.MediaRepository
import com.thinh.snaplet.data.repository.MediaRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency Injection module for repositories
 * 
 * To switch between implementations, simply comment/uncomment the binding:
 * - FakeMediaRepository: Mock data for development
 * - MediaRepositoryImpl: Real API calls
 * 
 * NO changes needed in ViewModel or UI!
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    /**
     * Provide MediaRepository implementation
     * 
     * DEVELOPMENT MODE: Using FakeMediaRepository (mock data)
     * Uncomment the line below to use real API
     */
    @Binds
    @Singleton
    abstract fun bindMediaRepository(
        fakeRepository: FakeMediaRepository  // ← Currently using FAKE
        // realRepository: MediaRepositoryImpl  // ← Uncomment for REAL API
    ): MediaRepository
    
    /**
     * PRODUCTION MODE: Using MediaRepositoryImpl (real API)
     * Comment out FakeMediaRepository and uncomment this:
     */
    // @Binds
    // @Singleton
    // abstract fun bindMediaRepository(
    //     realRepository: MediaRepositoryImpl
    // ): MediaRepository
}
