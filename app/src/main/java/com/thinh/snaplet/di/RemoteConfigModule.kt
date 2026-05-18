package com.thinh.snaplet.di

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.thinh.snaplet.R
import com.thinh.snaplet.data.repository.remoteconfig.RemoteConfigRepository
import com.thinh.snaplet.data.repository.remoteconfig.RemoteConfigRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RemoteConfigProviderModule {

    @Provides
    @Singleton
    fun provideFirebaseRemoteConfig(): FirebaseRemoteConfig {
        return FirebaseRemoteConfig.getInstance().apply {
            setConfigSettingsAsync(
                FirebaseRemoteConfigSettings.Builder()
                    .setMinimumFetchIntervalInSeconds(3600L)
                    .build()
            )
            setDefaultsAsync(R.xml.remote_config_defaults)
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RemoteConfigModule {

    @Binds
    @Singleton
    abstract fun bindRemoteConfigRepository(
        impl: RemoteConfigRepositoryImpl,
    ): RemoteConfigRepository
}
