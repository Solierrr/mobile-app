package com.project.solaria_mobile.di

import com.project.solaria_mobile.core.network.ConnectivityManagerNetworkMonitor
import com.project.solaria_mobile.core.network.NetworkMonitor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Liga o monitor de conectividade do Android à interface usada pelo app. */
@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkMonitorModule {
    @Binds
    @Singleton
    abstract fun bindNetworkMonitor(impl: ConnectivityManagerNetworkMonitor): NetworkMonitor
}
