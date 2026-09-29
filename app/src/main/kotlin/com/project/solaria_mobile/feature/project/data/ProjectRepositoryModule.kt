package com.project.solaria_mobile.feature.project.data

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ProjectRepositoryModule {
    @Binds abstract fun bindRepository(repository: FakeProjectRepository): ProjectRepository
}
