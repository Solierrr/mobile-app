package com.project.solaria_mobile.feature.profile.data

import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.feature.profile.domain.Connection
import com.project.solaria_mobile.feature.profile.domain.ProfileRepository
import com.project.solaria_mobile.feature.profile.domain.UserProfile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeProfileRepository @Inject constructor() : ProfileRepository {
    private val own = UserProfile("me", "RC", "Ronaldo Chagas", "@ronaldoa02349993", "São Paulo", "013110–200", "55+ (11) 91022–3438")
    private val other = UserProfile("u1", "RN", "Rodrigo Nolabas", "@rodrigaoobatechagaas", "São Paulo", "Engenheiro Elétrico", "55+ (11) 91022–3438")
    private val baseConnections = listOf(
        Connection("u1", "EV", "Everson", "@eozoiogarai"),
        Connection("u2", "BB", "Bruce Banner", "@hulksmash"),
        Connection("u3", "SU", "Sasuke Uchiha", "@konohabush"),
        Connection("u4", "AN", "Ana Silva", "@anasilva"),
        Connection("u5", "ML", "Mariana Lima", "@marianalima"),
        Connection("u6", "FS", "Felipe Santos", "@felipesantos"),
    )
    private val connections = baseConnections + baseConnections.map { it.copy(id = it.id + "-b") }
    private val connected = mutableSetOf<String>()

    override suspend fun ownProfile(): ApiResult<UserProfile> = ApiResult.Success(own)

    override suspend fun profile(id: String): ApiResult<UserProfile> = when (id) {
        own.id -> ApiResult.Success(own)
        else -> ApiResult.Success(other.copy(id = id))
    }

    override suspend fun connections(): ApiResult<List<Connection>> = ApiResult.Success(connections)

    override suspend fun isConnected(profileId: String): ApiResult<Boolean> = ApiResult.Success(profileId in connected)

    override suspend fun toggleConnection(profileId: String): ApiResult<Boolean> {
        if (profileId.isBlank()) return ApiResult.Failure(ErrorKind.VALIDATION, setOf("profileId"))
        if (!connected.add(profileId)) connected.remove(profileId)
        return ApiResult.Success(profileId in connected)
    }
}
