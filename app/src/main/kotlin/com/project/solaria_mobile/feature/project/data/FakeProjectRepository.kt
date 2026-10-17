package com.project.solaria_mobile.feature.project.data

import com.project.solaria_mobile.core.common.ApiResult
import com.project.solaria_mobile.core.common.ErrorKind
import com.project.solaria_mobile.feature.project.domain.ProjectRepository
import com.project.solaria_mobile.feature.project.domain.ProjectSummary
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeProjectRepository @Inject constructor() : ProjectRepository {
    private val items = listOf(
        ProjectSummary("1", "Fortech Enterprise", "São Paulo, SP", "Em andamento", "12 envolvidos"),
        ProjectSummary("2", "Solar Tech", "Fortaleza, CE", "Em análise", "8 envolvidos"),
        ProjectSummary("3", "Residencial Aurora", "Sorocaba, SP", "Em andamento", "6 envolvidos"),
        ProjectSummary("4", "Unidade Paraíba", "Santana de Parnaíba, SP", "Concluído", "4 envolvidos"),
    )

    override suspend fun projects(): ApiResult<List<ProjectSummary>> = ApiResult.Success(items)

    override suspend fun project(id: String): ApiResult<ProjectSummary> =
        items.firstOrNull { it.id == id }?.let { ApiResult.Success(it) } ?: ApiResult.Failure(ErrorKind.NOT_FOUND)
}
