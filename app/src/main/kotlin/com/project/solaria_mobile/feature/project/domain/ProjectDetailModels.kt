package com.project.solaria_mobile.feature.project.domain

import com.project.solaria_mobile.core.common.ApiResult

data class ProjectLocationInfo(val description: String, val addressLines: List<String>) {
    val copyText: String get() = addressLines.joinToString(", ")
}

data class ProjectMember(val id: String, val userId: String, val name: String, val role: String, val initials: String)

data class ProjectActivity(val id: String, val title: String, val subtitle: String, val time: String)

data class ProjectHistoryEntry(val id: String, val title: String, val detail: String)

data class ProjectPhoto(val id: String, val cover: Boolean)

interface ProjectDetailRepository {
    suspend fun location(projectId: String): ApiResult<ProjectLocationInfo>
    suspend fun members(projectId: String): ApiResult<List<ProjectMember>>
    suspend fun activities(projectId: String): ApiResult<List<ProjectActivity>>
    suspend fun history(projectId: String): ApiResult<List<ProjectHistoryEntry>>
    suspend fun photos(projectId: String): ApiResult<List<ProjectPhoto>>
    suspend fun photoContent(projectId: String, photoId: String): ApiResult<ByteArray>
}
