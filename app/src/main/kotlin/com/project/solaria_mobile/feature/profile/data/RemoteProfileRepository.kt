package com.project.solaria_mobile.feature.profile.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import com.project.solaria_mobile.core.common.ReadResult

import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.map
import kotlinx.coroutines.flow.map
import com.project.solaria_mobile.core.auth.accessTokenEmail
import com.project.solaria_mobile.core.format.initialsOf
import com.project.solaria_mobile.core.network.ApiCallExecutor
import com.project.solaria_mobile.core.offline.OfflineReader
import com.project.solaria_mobile.feature.profile.domain.Connection
import com.project.solaria_mobile.feature.profile.domain.ProfileRepository
import com.project.solaria_mobile.feature.profile.domain.UserProfile
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.MultipartBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import com.project.solaria_mobile.feature.profile.domain.ProfilePhotoType
import com.project.solaria_mobile.feature.profile.domain.CatalogCompany
import com.project.solaria_mobile.feature.profile.domain.CompanyPhotos

@Singleton
class RemoteProfileRepository @Inject constructor(
    private val api: ProfileApi,
    private val executor: ApiCallExecutor,
    private val reader: OfflineReader,
    private val network: com.project.solaria_mobile.core.network.NetworkMonitor,
    private val credentials: com.project.solaria_mobile.core.network.CredentialProvider,
) : ProfileRepository {
    override suspend fun uploadOwnPhoto(userId: String, type: ProfilePhotoType, image: ByteArray, mime: String): ApiResult<Unit> {
        val extension = if (mime == "image/png") "png" else "jpg"
        val part = MultipartBody.Part.createFormData("file", "perfil.$extension", image.toRequestBody(mime.toMediaType()))
        return executor.execute { api.uploadPhoto(userId, type.api, part) }.map { }
    }

    override suspend fun removeOwnPhoto(userId: String, type: ProfilePhotoType): ApiResult<Unit> =
        executor.execute { api.removePhoto(userId, type.api) }

    override suspend fun changeUsername(username: String): ApiResult<Unit> =
        executor.execute { api.changeUsername(UsernameRequest(username.trim())) }.map { }

    override suspend fun companyCatalog(): ApiResult<List<CatalogCompany>> =
        reader.read("catalog:companies", kotlinx.serialization.builtins.ListSerializer(CatalogCompanyDto.serializer())) { api.companyCatalog() }
            .map { items ->
                items.map { CatalogCompany(it.id, it.tradeName?.takeIf(String::isNotBlank) ?: "Empresa", listOfNotNull(it.city?.takeIf(String::isNotBlank), it.state?.takeIf(String::isNotBlank)).joinToString(", "), it.slug.orEmpty()) }
            }

    override suspend fun companyPhotos(companyId: String): ApiResult<CompanyPhotos> =
        executor.execute { api.companyPhotos(companyId) }.map { photos ->
            fun url(type: String) = photos.firstOrNull { it.type == type }?.url?.takeIf { it.isNotBlank() }
            CompanyPhotos(url("PROFILE"), url("BANNER"))
        }

    override suspend fun ownProfile(): ApiResult<UserProfile> =
        reader.read("me", SelfProfileDto.serializer()) { api.me() }.map { it.toProfile().copy(email = sessionEmail()) }

    override suspend fun profile(id: String): ApiResult<UserProfile> =
        reader.read("person:$id", UserDto.serializer()) { api.connection(id) }.map { it.toProfile() }

    override suspend fun connections(): ApiResult<List<Connection>> =
        executor.execute { api.connections() }.map { users -> users.map { it.toConnection() } }

    override suspend fun isConnected(profileId: String): ApiResult<Boolean> =
        profile(profileId).map { it.connected }

    override suspend fun people(query: String, page: Int, connectedOnly: Boolean): ApiResult<com.project.solaria_mobile.feature.profile.domain.PeoplePage> =
        readPeople(query, page, connectedOnly).result

    override suspend fun readPeople(query: String, page: Int, connectedOnly: Boolean): ReadResult<com.project.solaria_mobile.feature.profile.domain.PeoplePage> =
        reader.readResult("people:$connectedOnly:$page:${query.trim()}", PeoplePageDto.serializer()) {
            api.people(query.trim(), page, connectedOnly)
        }.map { com.project.solaria_mobile.feature.profile.domain.PeoplePage(it.items.map { user -> user.toConnection() }, it.hasMore) }

    override suspend fun toggleConnection(profileId: String): ApiResult<Boolean> = when (val result = isConnected(profileId)) {
        is ApiResult.Failure -> result
        is ApiResult.Success -> setConnection(profileId, !result.data)
    }

    override suspend fun setConnection(profileId: String, connected: Boolean): ApiResult<Boolean> {
        if (!network.isOnline.first()) return ApiResult.Failure(com.project.solaria_mobile.core.common.ErrorKind.CONNECTIVITY)
        return executor.execute {
        try {
            if (connected) api.addConnection(ConnectionRequest(profileId))
            else {
                val response = api.removeConnection(profileId)
                if (!response.isSuccessful) throw retrofit2.HttpException(response)
            }
        } catch (error: retrofit2.HttpException) {
            if (!((connected && error.code() == 409) || (!connected && error.code() == 404))) throw error
        }
        reader.invalidate("person:$profileId")
        reader.invalidatePrefix("people:")
        connected
        }
    }
    override fun observeOwnProfile(): Flow<ReadResult<UserProfile>> =
        reader.observe("me", SelfProfileDto.serializer()) { api.me() }.map { read -> read.map { it.toProfile().copy(email = sessionEmail()) } }

    /** O servidor não devolve o e-mail no perfil próprio; ele vem da sessão, que o traz no token. */
    private fun sessionEmail(): String = credentials.accessToken()?.let(::accessTokenEmail).orEmpty()

}

private fun UserDto.displayName(): String = name?.takeIf { it.isNotBlank() } ?: username?.takeIf { it.isNotBlank() } ?: "Usuário"

private fun UserDto.handle(): String = username?.takeIf { it.isNotBlank() }?.let { "@$it" }.orEmpty()

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
        affiliation = affiliationLabel(professions.isNotEmpty()),
        avatarUrl = avatar?.takeIf { it.isNotBlank() },
        bannerUrl = banner?.takeIf { it.isNotBlank() },
        companyId = companyMemberships.firstOrNull { !it.companyId.isNullOrBlank() }?.companyId,
        technicianId = technician?.id?.takeIf { it.isNotBlank() },
        independent = activeAffiliations.any { it.type == "INDEPENDENT" },
        authId = authId,
    )
}

/**
 * Vínculo do profissional: tipo do vínculo técnico com a empresa ou, sem ele, cargo e empresa. O cargo só entra aqui
 * quando a profissão já ocupa a outra linha; sem profissão, ele já aparece como ocupação.
 */
private fun SelfProfileDto.affiliationLabel(hasProfession: Boolean): String {
    activeAffiliations.firstOrNull()?.let { affiliation ->
        val company = affiliation.companyName?.takeIf { it.isNotBlank() }
        return when (affiliation.type) {
            "INDEPENDENT" -> "Autônomo"
            "AFFILIATED" -> listOfNotNull("Afiliado", company).joinToString(" · ")
            "PARTNER" -> listOfNotNull("Parceiro", company).joinToString(" · ")
            else -> company.orEmpty()
        }
    }
    if (!hasProfession) return ""
    return companyMemberships.firstOrNull()?.let { membership ->
        listOfNotNull(membership.position?.takeIf { it.isNotBlank() }, membership.companyName?.takeIf { it.isNotBlank() }).joinToString(" · ")
    }.orEmpty()
}

internal fun UserDto.toProfile(): UserProfile = UserProfile(
    id = id, initials = initialsOf(displayName()), name = displayName(), handle = handle(),
    state = "", occupation = "", phone = "", avatarUrl = avatar, bannerUrl = banner, connected = connected, authId = authId,
)

internal fun UserDto.toConnection(): Connection = Connection(
    id = id,
    initials = initialsOf(displayName()),
    name = displayName(),
    handle = handle(),
    avatarUrl = avatar,
    authId = authId,
)
