package com.project.solaria_mobile.core.network

import javax.inject.Inject
import javax.inject.Singleton

/**
 * De onde a camada HTTP obtém o token de acesso
 *
 * Principais operações:
 * - [accessToken] -> token atual | null
 */
interface CredentialProvider {
    /** devolve o token de auth atual | null */
    fun accessToken(): String?
}

/** Implementação vazia enquanto não tiver definição */
@Singleton
class NoCredentialProvider @Inject constructor() : CredentialProvider {
    override fun accessToken(): String? = null
}
