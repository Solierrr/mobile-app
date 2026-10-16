package com.project.solaria_mobile.core.auth

import com.project.solaria_mobile.core.network.CredentialProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionCredentialProvider @Inject constructor(
    private val tokenStore: TokenStore,
) : CredentialProvider {
    override fun accessToken(): String? = tokenStore.currentTokens()?.accessToken
}
