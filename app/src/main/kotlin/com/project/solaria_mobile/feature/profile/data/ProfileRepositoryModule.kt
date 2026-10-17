package com.project.solaria_mobile.feature.profile.data

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.project.solaria_mobile.feature.profile.domain.ProfileRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class ProfileRepositoryModule {
    @Binds abstract fun bindRepository(repository: RemoteProfileRepository): ProfileRepository
}
