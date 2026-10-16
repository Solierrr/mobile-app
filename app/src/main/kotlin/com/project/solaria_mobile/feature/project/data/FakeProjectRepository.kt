package com.project.solaria_mobile.feature.project.data

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import com.project.solaria_mobile.feature.project.domain.ProjectRepository
import com.project.solaria_mobile.feature.project.domain.ProjectSummary

@Singleton
class FakeProjectRepository @Inject constructor() : ProjectRepository {
    override val projects = MutableStateFlow(
        listOf(
            ProjectSummary("Fortech Enterprise", "São Paulo, SP", "Em andamento", "12 envolvidos"),
            ProjectSummary("Solar Tech", "Fortaleza, CE", "Em análise", "8 envolvidos"),
            ProjectSummary("Residencial Aurora", "Sorocaba, SP", "Em andamento", "6 envolvidos"),
            ProjectSummary("Unidade Paraíba", "Santana de Parnaíba, SP", "Concluído", "4 envolvidos"),
        )
    )
}
