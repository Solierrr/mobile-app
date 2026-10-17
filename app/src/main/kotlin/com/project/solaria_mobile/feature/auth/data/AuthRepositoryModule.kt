package com.project.solaria_mobile.feature.auth.data

import com.project.solaria_mobile.feature.auth.domain.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthRepositoryModule {
    @Binds abstract fun bindRepository(repository: RemoteAuthRepository): AuthRepository
}
