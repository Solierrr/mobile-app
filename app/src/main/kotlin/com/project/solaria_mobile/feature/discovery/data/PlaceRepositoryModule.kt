package com.project.solaria_mobile.feature.discovery.data

import com.project.solaria_mobile.feature.discovery.domain.PlaceRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class PlaceRepositoryModule {
    @Binds abstract fun bindRepository(repository: RemotePlaceRepository): PlaceRepository
}
