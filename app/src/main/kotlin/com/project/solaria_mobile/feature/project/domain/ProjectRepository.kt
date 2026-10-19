package com.project.solaria_mobile.feature.project.domain

import com.project.solaria_mobile.core.common.ApiResult

interface ProjectRepository {
    suspend fun projects(): ApiResult<List<ProjectSummary>>
    suspend fun project(id: String): ApiResult<ProjectSummary>
    suspend fun create(draft: NewProjectDraft): ApiResult<String>
}
