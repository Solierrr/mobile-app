package com.project.solaria_mobile.feature.project.domain

import kotlinx.coroutines.flow.Flow
import com.project.solaria_mobile.core.common.ReadResult
import com.project.solaria_mobile.core.common.readOnce

import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind

interface ProjectRepository {
    suspend fun resolveLocation(id: String): ApiResult<ProjectSummary> = project(id)
    suspend fun projects(): ApiResult<List<ProjectSummary>>
    suspend fun project(id: String): ApiResult<ProjectSummary>
    suspend fun create(draft: NewProjectDraft): ApiResult<String>
    suspend fun changeStatus(id: String, status: ProjectStatusOption): ApiResult<ProjectSummary> = ApiResult.Failure(ErrorKind.NOT_SUPPORTED)
    suspend fun archive(id: String): ApiResult<Unit> = ApiResult.Failure(ErrorKind.NOT_SUPPORTED)
    suspend fun draft(id: String): ApiResult<NewProjectDraft> = ApiResult.Failure(ErrorKind.NOT_SUPPORTED)
    suspend fun myInvitations(): ApiResult<List<ProjectInvitation>> = ApiResult.Failure(ErrorKind.NOT_SUPPORTED)
    suspend fun respondInvitation(id: String, accept: Boolean): ApiResult<Unit> = ApiResult.Failure(ErrorKind.NOT_SUPPORTED)
    suspend fun update(id: String, draft: NewProjectDraft): ApiResult<String> = ApiResult.Failure(ErrorKind.NOT_SUPPORTED)
    fun observeProjects(): Flow<ReadResult<List<ProjectSummary>>> =
        readOnce { projects() }
    fun observeProject(id: String): Flow<ReadResult<ProjectSummary>> =
        readOnce { project(id) }
}
