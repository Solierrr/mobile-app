package com.project.solaria_mobile.core.network.realtime

import com.project.solaria_mobile.core.auth.TokenAuthenticator
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun interface RealtimeTokenRefresher {
    suspend fun refresh(rejectedToken: String): String?
}

class SessionRealtimeTokenRefresher @Inject constructor(private val authenticator: TokenAuthenticator) : RealtimeTokenRefresher {
    override suspend fun refresh(rejectedToken: String): String? = withContext(Dispatchers.IO) { authenticator.renewAccessToken(rejectedToken) }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RealtimeTokenModule {
    @Binds abstract fun refresher(implementation: SessionRealtimeTokenRefresher): RealtimeTokenRefresher
}
