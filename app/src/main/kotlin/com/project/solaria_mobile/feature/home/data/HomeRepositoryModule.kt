package com.project.solaria_mobile.feature.home.data

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.project.solaria_mobile.feature.home.domain.HomeRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class HomeRepositoryModule {
    @Binds abstract fun bindRepository(repository: FakeHomeRepository): HomeRepository
}
