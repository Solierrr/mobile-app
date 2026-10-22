package com.project.solaria_mobile.feature.project.domain

import kotlinx.coroutines.flow.Flow
import com.project.solaria_mobile.core.common.ReadResult
import com.project.solaria_mobile.core.common.readOnce

import com.project.solaria_mobile.core.common.ApiResult

interface ProjectRepository {
    suspend fun resolveLocation(id: String): ApiResult<ProjectSummary> = project(id)
    suspend fun projects(): ApiResult<List<ProjectSummary>>
    suspend fun project(id: String): ApiResult<ProjectSummary>
    suspend fun create(draft: NewProjectDraft): ApiResult<String>
    fun observeProjects(): Flow<ReadResult<List<ProjectSummary>>> =
        readOnce { projects() }
    fun observeProject(id: String): Flow<ReadResult<ProjectSummary>> =
        readOnce { project(id) }
}
