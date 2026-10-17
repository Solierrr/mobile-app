package com.project.solaria_mobile.di

import com.project.solaria_mobile.BuildConfig
import com.project.solaria_mobile.core.network.RetrofitFactory
import com.project.solaria_mobile.feature.communication.data.MessengerApi
import com.project.solaria_mobile.feature.discovery.data.RegistryApi
import com.project.solaria_mobile.feature.profile.data.ProfileApi
import com.project.solaria_mobile.feature.project.data.ProjectApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private const val DISABLED_REGISTRY_URL = "https://registry.disabled.invalid/"

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {
    @Provides
    @Singleton
    fun provideProjectApi(factory: RetrofitFactory): ProjectApi =
        factory.create(BuildConfig.CORE_BASE_URL).create(ProjectApi::class.java)

    @Provides
    @Singleton
    fun provideProfileApi(factory: RetrofitFactory): ProfileApi =
        factory.create(BuildConfig.CORE_BASE_URL).create(ProfileApi::class.java)

    @Provides
    @Singleton
    fun provideMessengerApi(factory: RetrofitFactory): MessengerApi =
        factory.create(BuildConfig.MESSENGER_BASE_URL).create(MessengerApi::class.java)

    @Provides
    @Singleton
    fun provideRegistryApi(factory: RetrofitFactory): RegistryApi =
        factory.create(BuildConfig.GOOGLE_REGISTRY_BASE_URL.ifBlank { DISABLED_REGISTRY_URL }).create(RegistryApi::class.java)
}
