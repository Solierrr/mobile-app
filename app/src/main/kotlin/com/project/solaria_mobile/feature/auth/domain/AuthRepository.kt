package com.project.solaria_mobile.feature.auth.domain

import com.project.solaria_mobile.core.common.ApiResult
import kotlinx.coroutines.flow.StateFlow

enum class SessionState { SIGNED_OUT, SIGNED_IN }

interface AuthRepository {
    val session: StateFlow<SessionState>
    suspend fun beginLogin(): ApiResult<LoginDestination>
    suspend fun completeLogin(code: String, state: String): ApiResult<Unit>
    suspend fun signOut(): ApiResult<Unit>
}
