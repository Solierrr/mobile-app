package com.project.solaria_mobile.feature.auth.data

import com.project.solaria_mobile.BuildConfig
import com.project.solaria_mobile.core.auth.AuthApi
import com.project.solaria_mobile.core.auth.AuthTokens
import com.project.solaria_mobile.core.auth.PendingLogin
import com.project.solaria_mobile.core.auth.Pkce
import com.project.solaria_mobile.core.auth.RedeemRequest
import com.project.solaria_mobile.core.auth.StartTransactionRequest
import com.project.solaria_mobile.core.auth.TokenStore
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.network.ApiCallExecutor
import com.project.solaria_mobile.core.offline.OfflineStore
import com.project.solaria_mobile.feature.auth.domain.AuthRepository
import com.project.solaria_mobile.feature.auth.domain.SessionState
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@Singleton
class RemoteAuthRepository @Inject constructor(
    private val api: AuthApi,
    private val executor: ApiCallExecutor,
    private val tokenStore: TokenStore,
    private val offlineStore: OfflineStore,
) : AuthRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val session: StateFlow<SessionState> = tokenStore.tokens
        .map { if (it == null) SessionState.SIGNED_OUT else SessionState.SIGNED_IN }
        .stateIn(
            scope,
            SharingStarted.Eagerly,
            if (tokenStore.currentTokens() == null) SessionState.SIGNED_OUT else SessionState.SIGNED_IN,
        )

    override suspend fun beginLogin(): ApiResult<String> {
        val verifier = Pkce.randomUrlSafe(VERIFIER_BYTES)
        val state = Pkce.randomUrlSafe(STATE_BYTES)
        val request = StartTransactionRequest(
            clientId = CLIENT_ID,
            redirectUri = BuildConfig.AUTH_REDIRECT_URI,
            state = state,
            codeChallenge = Pkce.challengeFor(verifier),
            codeChallengeMethod = CODE_CHALLENGE_METHOD,
        )
        return when (val result = executor.execute { api.startTransaction(request) }) {
            is ApiResult.Failure -> result
            is ApiResult.Success -> {
                tokenStore.savePendingLogin(PendingLogin(state, verifier))
                ApiResult.Success(result.data.authorizationUrl)
            }
        }
    }

    override suspend fun completeLogin(code: String, state: String): ApiResult<Unit> {
        val pending = tokenStore.takePendingLogin()
        if (pending == null || pending.state != state) return ApiResult.Failure(ErrorKind.UNAUTHORIZED)
        val request = RedeemRequest(CLIENT_ID, BuildConfig.AUTH_REDIRECT_URI, code, pending.codeVerifier)
        return when (val result = executor.execute { api.redeem(request) }) {
            is ApiResult.Failure -> result
            is ApiResult.Success -> {
                offlineStore.clearAll()
                tokenStore.saveTokens(AuthTokens(result.data.accessToken, result.data.refreshToken, result.data.accessTokenExpiresAt))
                ApiResult.Success(Unit)
            }
        }
    }

    override suspend fun signOut(): ApiResult<Unit> {
        executor.execute { api.logout() }
        tokenStore.clearTokens()
        offlineStore.clearAll()
        return ApiResult.Success(Unit)
    }

    private companion object {
        const val CLIENT_ID = "solaria-mobile"
        const val CODE_CHALLENGE_METHOD = "S256"
        const val VERIFIER_BYTES = 64
        const val STATE_BYTES = 32
    }
}
