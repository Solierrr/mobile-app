package com.project.solaria_mobile.core.auth

import com.project.solaria_mobile.BuildConfig
import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.core.mock.MockApiData
import com.project.solaria_mobile.core.mock.MockModeStore
import com.project.solaria_mobile.core.network.ApiCallExecutor
import com.project.solaria_mobile.core.network.RetrofitFactory
import com.project.solaria_mobile.feature.auth.domain.LoginDestination
import javax.inject.Inject
import retrofit2.http.Header
import retrofit2.http.POST

interface QaSessionApi {
    @POST("auth/mobile/qa/session")
    suspend fun signIn(@Header("X-QA-Device-Token") credential: String): TokenResponse
}

class FlavorLogin @Inject constructor(
    factory: RetrofitFactory,
    private val credentials: QaDeviceCredentialStore,
    private val executor: ApiCallExecutor,
    private val tokenStore: TokenStore,
    private val mockMode: MockModeStore,
    private val mockData: MockApiData,
) {
    private val api = factory.create(BuildConfig.AUTH_BASE_URL).create(QaSessionApi::class.java)

    suspend fun beginLogin(): ApiResult<LoginDestination> {
        if (mockMode.isEnabled()) {
            val mock = mockData.mockTokens()
            tokenStore.saveTokens(AuthTokens(mock.accessToken, mock.refreshToken, mock.accessTokenExpiresAt))
            return ApiResult.Success(LoginDestination.Authenticated)
        }
        val credential = credentials.read() ?: return ApiResult.Failure(ErrorKind.UNAUTHORIZED)
        return when (val result = executor.execute { api.signIn(credential) }) {
            is ApiResult.Failure -> result
            is ApiResult.Success -> {
                val response = result.data
                tokenStore.saveTokens(AuthTokens(response.accessToken, response.refreshToken, response.accessTokenExpiresAt))
                ApiResult.Success(LoginDestination.Authenticated)
            }
        }
    }
}
