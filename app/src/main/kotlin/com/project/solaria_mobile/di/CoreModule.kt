package com.project.solaria_mobile.di

import com.project.solaria_mobile.core.network.CredentialProvider
import com.project.solaria_mobile.core.network.NoCredentialProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Ligações de interface para implementação do pacote `core`
 *
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CoreModule {

    /** sem definição de implementação ainda */
    @Binds
    abstract fun bindCredentialProvider(impl: NoCredentialProvider): CredentialProvider
}
