package com.project.solaria_mobile.di

import com.project.solaria_mobile.core.network.realtime.KrossbowStompConnector
import com.project.solaria_mobile.core.network.realtime.MessengerRealtimeEndpoint
import com.project.solaria_mobile.core.network.realtime.RealtimeClient
import com.project.solaria_mobile.core.network.realtime.RealtimeEndpoint
import com.project.solaria_mobile.core.network.realtime.StompConnector
import com.project.solaria_mobile.core.network.realtime.StompRealtimeClient
import com.project.solaria_mobile.feature.communication.data.ChatCapabilities
import com.project.solaria_mobile.feature.communication.data.ChatRealtime
import com.project.solaria_mobile.feature.communication.data.MockModeChatCapabilities
import com.project.solaria_mobile.feature.communication.data.SwitchingChatRealtime
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

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
    abstract fun bindRealtimeEndpoint(impl: MessengerRealtimeEndpoint): RealtimeEndpoint

    @Binds
    abstract fun bindChatRealtime(impl: SwitchingChatRealtime): ChatRealtime

    @Binds
    abstract fun bindChatCapabilities(impl: MockModeChatCapabilities): ChatCapabilities

    companion object {
        /** Escopo do processo para a conexão em tempo real, que sobrevive às telas. */
        @Provides
        @Singleton
        fun provideRealtimeScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}
