package com.project.solaria_mobile.feature.discovery.data

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.project.solaria_mobile.feature.discovery.domain.DiscoveryRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class DiscoveryRepositoryModule {
    @Binds abstract fun bindRepository(repository: UnavailableDiscoveryRepository): DiscoveryRepository
}
