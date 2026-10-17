package com.project.solaria_mobile.feature.profile.domain

import com.project.solaria_mobile.core.common.ApiResult

interface ProfileRepository {
    suspend fun ownProfile(): ApiResult<UserProfile>
    suspend fun profile(id: String): ApiResult<UserProfile>
    suspend fun connections(): ApiResult<List<Connection>>
    suspend fun isConnected(profileId: String): ApiResult<Boolean>
    suspend fun toggleConnection(profileId: String): ApiResult<Boolean>
}
