package com.project.solaria_mobile.di

import com.project.solaria_mobile.core.network.realtime.KrossbowStompConnector
import com.project.solaria_mobile.core.network.realtime.NoRealtimeEndpoint
import com.project.solaria_mobile.core.network.realtime.RealtimeClient
import com.project.solaria_mobile.core.network.realtime.RealtimeEndpoint
import com.project.solaria_mobile.core.network.realtime.StompConnector
import com.project.solaria_mobile.core.network.realtime.StompRealtimeClient
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Ligações de interface para implementação do STOMP
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RealtimeModule {

    @Binds
    abstract fun bindRealtimeClient(impl: StompRealtimeClient): RealtimeClient

    @Binds
    abstract fun bindStompConnector(impl: KrossbowStompConnector): StompConnector

    @Binds
    abstract fun bindRealtimeEndpoint(impl: NoRealtimeEndpoint): RealtimeEndpoint
}
