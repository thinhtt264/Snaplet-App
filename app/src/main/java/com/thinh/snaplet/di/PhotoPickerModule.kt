package com.thinh.snaplet.di

import com.thinh.snaplet.platform.photo_picker.PhotoPickerManager
import com.thinh.snaplet.platform.photo_picker.PhotoPickerManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class PhotoPickerModule {

    @Binds
    abstract fun bindPhotoPickerManager(
        impl: PhotoPickerManagerImpl
    ): PhotoPickerManager
}
