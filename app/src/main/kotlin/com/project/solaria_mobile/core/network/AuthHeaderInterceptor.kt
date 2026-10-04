package com.project.solaria_mobile.core.network

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * intercepta todda requisição HTTP e adiciona o header de authorization com o token, caso ele exista
 *
 * Nunca sobrescreve um header que a chamada já definiu
 *
 * Depende de:
 * - [CredentialProvider] -> fornece o token
 *
 * Principais operações:
 * - [intercept] -> coloca o header de authorization com o token, caso ele exista
 */
class AuthHeaderInterceptor @Inject constructor(
    private val credentials: CredentialProvider,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val token = credentials.accessToken()

        if (token.isNullOrBlank() || request.header(AUTHORIZATION) != null) {
            return chain.proceed(request)
        }

        return chain.proceed(request.newBuilder().header(AUTHORIZATION, "Bearer $token").build())
    }

    private companion object {
        const val AUTHORIZATION = "Authorization"
    }
}
