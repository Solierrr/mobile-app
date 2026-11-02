package com.project.solaria_mobile.feature.profile.domain

import kotlinx.coroutines.flow.Flow
import com.project.solaria_mobile.core.common.ReadResult
import com.project.solaria_mobile.core.common.readOnce

import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.map
import com.project.solaria_mobile.core.common.ErrorKind

interface ProfileRepository {
    suspend fun readPeople(query: String, page: Int, connectedOnly: Boolean): ReadResult<PeoplePage> =
        ReadResult(people(query, page, connectedOnly))
    suspend fun people(query: String, page: Int, connectedOnly: Boolean): ApiResult<PeoplePage> =
        connections().map { PeoplePage(it.filter { person -> person.name.contains(query, true) }) }
    suspend fun setConnection(profileId: String, connected: Boolean): ApiResult<Boolean> = toggleConnection(profileId)

    suspend fun ownProfile(): ApiResult<UserProfile>
    suspend fun companyCatalog(): ApiResult<List<CatalogCompany>> = ApiResult.Failure(ErrorKind.NOT_SUPPORTED)
    suspend fun companyPhotos(companyId: String): ApiResult<CompanyPhotos> = ApiResult.Failure(ErrorKind.NOT_SUPPORTED)
    suspend fun uploadOwnPhoto(userId: String, type: ProfilePhotoType, image: ByteArray, mime: String): ApiResult<Unit> = ApiResult.Failure(ErrorKind.NOT_SUPPORTED)
    suspend fun removeOwnPhoto(userId: String, type: ProfilePhotoType): ApiResult<Unit> = ApiResult.Failure(ErrorKind.NOT_SUPPORTED)
    suspend fun changeUsername(username: String): ApiResult<Unit> = ApiResult.Failure(ErrorKind.NOT_SUPPORTED)
    suspend fun profile(id: String): ApiResult<UserProfile>
    suspend fun connections(): ApiResult<List<Connection>>
    suspend fun isConnected(profileId: String): ApiResult<Boolean>
    suspend fun toggleConnection(profileId: String): ApiResult<Boolean>
    fun observeOwnProfile(): Flow<ReadResult<UserProfile>> =
        readOnce { ownProfile() }
}
