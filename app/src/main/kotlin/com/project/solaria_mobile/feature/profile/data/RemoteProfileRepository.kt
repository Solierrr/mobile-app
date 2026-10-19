package com.project.solaria_mobile.feature.profile.data

import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.map
import com.project.solaria_mobile.core.format.initialsOf
import com.project.solaria_mobile.core.network.ApiCallExecutor
import com.project.solaria_mobile.feature.profile.domain.Connection
import com.project.solaria_mobile.feature.profile.domain.ProfileRepository
import com.project.solaria_mobile.feature.profile.domain.UserProfile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteProfileRepository @Inject constructor(
    private val api: ProfileApi,
    private val executor: ApiCallExecutor,
) : ProfileRepository {
    override suspend fun ownProfile(): ApiResult<UserProfile> =
        executor.execute { api.me() }.map { it.toProfile() }

    override suspend fun profile(id: String): ApiResult<UserProfile> =
        executor.execute { api.connection(id) }.map { it.toProfile() }

    override suspend fun connections(): ApiResult<List<Connection>> =
        executor.execute { api.connections() }.map { users -> users.map { it.toConnection() } }

    override suspend fun isConnected(profileId: String): ApiResult<Boolean> =
        executor.execute { api.connections() }.map { users -> users.any { it.id == profileId } }

    override suspend fun toggleConnection(profileId: String): ApiResult<Boolean> =
        when (val connected = isConnected(profileId)) {
            is ApiResult.Failure -> connected
            is ApiResult.Success ->
                if (connected.data) {
                    executor.execute { api.removeConnection(profileId) }.map { false }
                } else {
                    executor.execute { api.addConnection(ConnectionRequest(profileId)) }.map { true }
                }
        }
}

private fun UserDto.displayName(): String = username?.takeIf { it.isNotBlank() } ?: "Usuário"

private fun UserDto.handle(): String = "@" + displayName().replace(" ", "").lowercase()

internal fun SelfProfileDto.toProfile(): UserProfile {
    val name = person?.name?.takeIf { it.isNotBlank() } ?: "Usuário"
    val professions = technician?.professions.orEmpty().mapNotNull { it.name?.takeIf { value -> value.isNotBlank() } }
    val occupation = professions.joinToString(", ").ifBlank {
        companyMemberships.firstOrNull()?.let { membership ->
            listOfNotNull(
                membership.position?.takeIf { it.isNotBlank() },
                membership.companyName?.takeIf { it.isNotBlank() },
            ).joinToString(" · ")
        }.orEmpty()
    }
    return UserProfile(
        id = id,
        initials = initialsOf(name),
        name = name,
        handle = "",
        state = "",
        occupation = occupation,
        phone = person?.phone.orEmpty().trim(),
        avatarUrl = avatar?.takeIf { it.isNotBlank() },
        bannerUrl = banner?.takeIf { it.isNotBlank() },
    )
}

internal fun UserDto.toProfile(): UserProfile = UserProfile(
    id = id,
    initials = initialsOf(displayName()),
    name = displayName(),
    handle = handle(),
    state = "",
    occupation = "",
    phone = "",
)

internal fun UserDto.toConnection(): Connection = Connection(
    id = id,
    initials = initialsOf(displayName()),
    name = displayName(),
    handle = handle(),
)
