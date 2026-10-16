package com.project.solaria_mobile.feature.project.data

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.project.solaria_mobile.feature.project.domain.ProjectRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class ProjectRepositoryModule {
    @Binds abstract fun bindRepository(repository: RemoteProjectRepository): ProjectRepository
}
