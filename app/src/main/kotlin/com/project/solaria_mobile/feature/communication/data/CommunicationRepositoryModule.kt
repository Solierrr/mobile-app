package com.project.solaria_mobile.feature.communication.data

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class CommunicationRepositoryModule {
    @Binds abstract fun bindRepository(repository: FakeCommunicationRepository): CommunicationRepository
}
